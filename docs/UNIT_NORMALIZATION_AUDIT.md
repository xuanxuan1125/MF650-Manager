# MF650 数据单位与字段语义规范化审计 (UNIT_NORMALIZATION_AUDIT.md)

## 1. 审计背景与目标

在 v0.1.0 中，由于部分 DTO 类型是基于假设定义，导致反序列化不严谨（如将字符串 `"6/6"` 定义为 `Int`，将字符串 `"42.95 GB"` 假设为字节 `Long`）。
本审计基于真实设备（ALECA MF650 @ `192.168.100.1`）抓取的实时 JSON 报文进行逐字段校准，彻底消除类型转换异常与虚假数字。

---

## 2. 字段语义与单位矩阵

### 2.1 电池与供电 (Port 8081 `/api/device-status`)
| 字段路径 | 设备返回示例 | 原始类型 | 真实单位 / 语义 | 规范化规则 |
| :--- | :--- | :--- | :--- | :--- |
| `battery.level` | `"6/6"`, `"4/6"` | String | 0..6 档库仑计档位 | 正则匹配 `(\d+)/6`，查表映射为 `[0, 10, 25, 50, 75, 90, 100]%` |
| `battery.voltage`| `"3.98V"` | String | 伏特 (V) | 提取浮点数 `3.98` |
| `charging.enabled`| `0` 或 `1` | Int | 充电芯片通路使能 | `1 -> true`, `0 -> false` |
| `charging.status`| `"未充电"`, `"充电中"` | String | 文本状态 | 保留用于直接显示 |
| `auto_charge.enabled`| `1` | Int | 自动充电保护开关 | `1 -> true`, `0 -> false` |
| `auto_charge.min_level`| `2` | Int | 自动充电触发下限 (档位 2=25%) | 保持档位整数 |
| `auto_charge.max_level`| `5` | Int | 自动充电停充上限 (档位 5=90%) | 保持档位整数 |
| `usb_connection`| `"已连接"` | String | USB 供电状态 | 判断非空且包含连接 |

### 2.2 蜂窝基站与信号 (Port 8081 `/api/cell?ACTION=get_status`)
| 字段路径 | 设备返回示例 | 原始类型 | 真实单位 / 语义 | 规范化规则 |
| :--- | :--- | :--- | :--- | :--- |
| `status` | `"success"` | String | 请求状态 | `equalsIgnoreCase("success")` |
| `lte_lock` | `"unlocked"`, `"locked"`| String | LTE 小区锁定状态 | `equals("locked")` |
| `nr_lock` | `"unlocked"`, `"locked"`| String | 5G NR 锁定状态 | `equals("locked")` |
| `network` | `"LTE"`, `"NR5G-SA"` | String | 当前附着制式 | 规范化为大写制式标签 |
| `lte.pci` | `"153"` | String | 物理小区标识 (0..503) | `toIntOrNull()` |
| `lte.earfcn` | `"1300"` | String | 载波绝对频点号 | `toIntOrNull()` |
| `lte.tac` | `"37256"` | String | 跟踪区编码 | 字符串保留 |
| `lte.rsrp` | `"-90.5"` | String | 参考信号接收功率 (dBm) | `toDoubleOrNull()?.roundToInt()` 或保留 1 位小数 |
| `lte.rsrq` | `"-12.1"` | String | 参考信号接收质量 (dB) | `toDoubleOrNull()` |
| `lte.rssi` | `"-59.5"` | String | 接收信号强度指示 (dBm) | `toDoubleOrNull()` |

### 2.3 流量与速率 (Port 8081 `/api/device/info` & `/api/liuliang`)
| 字段路径 | 设备返回示例 | 原始类型 | 真实单位 / 语义 | 规范化规则 |
| :--- | :--- | :--- | :--- | :--- |
| `traffic_stats.download` | `"42.94"` 或 `"42.95 GB"`| String | 累计下载量 (GB) | 去除空格与单位后解析为 `Double` (GB) |
| `traffic_stats.upload` | `"2.69"` 或 `"2.70 GB"`| String | 累计上传量 (GB) | 解析为 `Double` (GB) |
| `traffic_stats.total` | `"45.64"` 或 `"45.65 GB"`| String | 累计总流量 (GB) | 解析为 `Double` (GB) |
| `current_speed.download`| `"0.26"` | String | 实时下载速率 (**MB/s**) | 乘以 1024 转换为 KB/s，驱动折线图 |
| `current_speed.upload` | `"0.01"` | String | 实时上传速率 (**MB/s**) | 乘以 1024 转换为 KB/s，驱动折线图 |

### 2.4 系统健康与遥测 (Port 8081 `/api/device/info`)
| 字段路径 | 设备返回示例 | 原始类型 | 真实单位 / 语义 | 规范化规则 |
| :--- | :--- | :--- | :--- | :--- |
| `system_status.cpu_usage` | `33.84` | Double | CPU 利用率百分比 (%) | 保留 1 位小数，直接显示为 `33.8%` |
| `system_status.memory_usage`| `88.91` | Double | 内存占用百分比 (%) | 直接显示为 `88.9%` |
| `system_status.cpu_temperature`| `"45.1"` | String | CPU 核心温度 (°C) | 浮点数解析，保留 `45.1°C` |
| `system_status.sdx_temperature`| `"43.9"` | String | SDX55 基带温度 (°C) | 浮点数解析，保留 `43.9°C` |
| `system_status.uptime` | `"0天14小时1分"` | String | 系统连续运行时间 | 原生展示或格式化 |

---

## 3. 错误预防与降级防线
1. **禁止通用乘除猜测**：所有解析严格根据来源（Port 80 vs Port 8081）采用专用 Normalizer。
2. **非法数据呈现 `--`**：当解析结果为 null 或超出物理常识范围（如温度 < -40°C 或 > 120°C，电量 < 0% 或 > 100%），一律回显 `--`，并输出警告日志。
