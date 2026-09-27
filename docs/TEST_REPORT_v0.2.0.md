# 单元测试与质量验证报告 (MF650 Manager v0.2.0)

## 1. 测试概览

- **测试框架**：JUnit 4 + Android Gradle Plugin Unit Test Runner
- **执行时间**：2026-09-27
- **测试环境**：Windows 11 x64, OpenJDK 21.0.3 (JBR), Gradle 8.13
- **测试结果**：**40/40 单元测试全部通过（100% PASS，0 失败，0 错误，0 跳过）**

---

## 2. 测试用例清单与分类明细

### 2.1 电量与单位解析核心引擎 (新增)
| 测试套件 | 测试用例方法 | 测试目的与断言点 | 结果 |
| :--- | :--- | :--- | :--- |
| `BatteryNormalizerTest` | `testDiscreteScale0to6Mapping` | 验证 0..6 离散挡位到 `[0, 10, 25, 50, 75, 90, 100]%` 映射 | ✅ PASS |
| `BatteryNormalizerTest` | `testStringFractionFromPort8081`| 验证 Port 8081 返回 `"6/6"`, `"3/6"`, `"0/6"` 等分数字符串提取与电压 | ✅ PASS |
| `BatteryNormalizerTest` | `testDirectPercentageStrings` | 验证 `"85%"` 直接百分比字符串有效提取 | ✅ PASS |
| `BatteryNormalizerTest` | `testVoltageParsing` | 验证 `"4.15V"`, `"3850mV"` 毫伏到伏的精准换算与异常兜底 | ✅ PASS |
| `BatteryNormalizerTest` | `testOutOfRangeInputsDoNotClampSilently` | 验证超出 0..6 挡位的非法输入（如 7 或 -1）严禁盲目 clamp，必须判为无效 | ✅ PASS |
| `BatteryNormalizerTest` | `testNullOrEmptyFallsBackGracefully` | 验证空数据或无有效载荷时安全回退，不发生 NPE | ✅ PASS |
| `UnitNormalizerTest` | `testFormatSpeed` | 验证 0 KB/s、512 KB/s、1.00 MB/s、1.00 GB/s 阶梯格式化 | ✅ PASS |
| `UnitNormalizerTest` | `testParseSpeedToKb` | 验证 8081 无量纲浮点数 `"0.26"` (MB/s) 自动乘以 1024 还原为 KB/s | ✅ PASS |
| `UnitNormalizerTest` | `testFormatTrafficGb` | 验证字节/MB/GB/TB 流量单位阶梯转换 | ✅ PASS |
| `UnitNormalizerTest` | `testParseTrafficStringToGb` | 验证从混合字符串中提取数值转为标准 GB 标量 | ✅ PASS |
| `UnitNormalizerTest` | `testFormatTemperature` | 验证温度有效区间校验 (-30..120°C) 与格式化 | ✅ PASS |
| `UnitNormalizerTest` | `testFormatSignalMetrics` | 验证 RSRP (-140..-40 dBm)、SINR、RSRQ 格式化 | ✅ PASS |
| `UnitNormalizerTest` | `testFormatCpuUsageAndRam` | 验证 CPU 百分比与内存已用/总容量 MB 显示 | ✅ PASS |

### 2.2 协议序列化与数据模型 (`AdvancedApiSerializationTest`)
| 测试用例方法 | 测试内容 | 结果 |
| :--- | :--- | :--- |
| `testDeviceInfoSerialization` | 验证 Port 8081 `/api/device/info` 多层嵌套结构反序列化 | ✅ PASS |
| `testCellularStatusSerialization`| 验证 `/api/cell` 返回的 5G NR / LTE 双模参数与数字/字符混合 status | ✅ PASS |
| `testBatteryStatusSerialization` | 验证 `/api/device-status` 嵌套对象与充电状态布尔属性 | ✅ PASS |
| `testTrafficStatusSerialization` | 验证 `/api/liuliang` 流量统计字节流解析与限额浮点数 | ✅ PASS |
| `testBandConfigSerialization` | 验证 `/api/nr5g-band` 5G SA/NSA 频段列表解析 | ✅ PASS |

### 2.3 安全拦截器与脱敏测试
| 测试套件 | 测试用例方法 | 测试内容 | 结果 |
| :--- | :--- | :--- | :--- |
| `HostAllowlistInterceptorTest` | `testAllowlistedHostPasses` | 验证向 `192.168.100.1` 发送请求正常放行 | ✅ PASS |
| `HostAllowlistInterceptorTest` | `testNonAllowlistedHostBlocked` | 验证向恶意或非局域网 IP 发送请求被直接安全拦截阻断 | ✅ PASS |
| `PadavanAuthInterceptorTest` | `testAuthHeaderAddedForPort80` | 验证向 Port 80 发起请求时自动注入 HTTP Basic 认证头 | ✅ PASS |
| `PadavanAuthInterceptorTest` | `testAuthHeaderNotAddedForPort8081`| 验证向 Port 8081 免密接口请求时不泄露 Port 80 凭据 | ✅ PASS |
| `SensitiveDataRedactorTest` | `testRedactPassword` | 验证日志与异常打印中密码自动打码为 `******` | ✅ PASS |
| `SensitiveDataRedactorTest` | `testRedactImei` | 验证 IMEI 敏感串号仅展示前 6 位与后 4 位，中间星号脱敏 | ✅ PASS |
| `SensitiveDataRedactorTest` | `testRedactIccid` | 验证 SIM 卡 ICCID 自动掩码保护用户隐私 | ✅ PASS |
| `SensitiveDataRedactorTest` | `testRedactPhoneNumber`| 验证手机号第 4-7 位星号脱敏 | ✅ PASS |

### 2.4 表单构建与解析引擎
| 测试套件 | 测试数量 | 验证范围 | 结果 |
| :--- | :--- | :--- | :--- |
| `PadavanFormBuilderTest` | 5 | 验证 2.4G/5G Wi-Fi、短信、DHCP、防火墙表单完整字段与隐藏参数 | ✅ PASS |
| `PadavanParserTest` | 3 | 验证 ASP 页面 HTML 注释与内联 JavaScript 数据提取 | ✅ PASS |
| `SignalQualityEvaluatorTest` | 5 | 验证 5G SA/NSA 各种 RSRP 与 SINR 组合的优/良/中/差评级 | ✅ PASS |
| `RepositoryFailoverTest` | 1 | 验证 8081 离线时自动无缝降级到 Port 80 基础数据 | ✅ PASS |

---

## 3. 质量指标评估

- **测试总数**：40 项
- **通过率**：100%
- **核心数据流无 Clamp 认证**：通过
- **多线程 Coroutine 调度安全性**：通过
- **构建产物字节完整度**：Release APK 编译零告警/零错误
