# RAM 计算公式证据

2026-10-02。**确认的是 2.3 FOTA 的旧公式；当前 8081 `system_status.memory_usage` 的公式仍 UNKNOWN。** 所有 ELF 均作为 PC 数据读取，未执行；关键 Thumb 指令以 GNU ARM objdump 与 Capstone 交叉复核。

## 三条路径的证据边界

| 路径 | 已确认输入/输出 | 公式 | 读取 MemAvailable |
| --- | --- | --- | --- |
| FOTA `/usr/bin/httpd` 的 `json_system_status` | meminfo → `ram.total/used/free/buffers/cached` | `ram.used = MemTotal - MemFree`，输出 kB 数量 | **NO**：该 RAM 解析路径未读取 |
| FOTA `/usr/bin/mobile_svr` → BusyBox free | 第二行 total/used → NVRAM 字符串 `mem_usage` | `used_column × 100 / total_column`，`%.2f%%` | **NO**：这条已核对路径未读取 |
| 当前 8081 `/api/device/info` | 上阶段观测只有 `system_status.memory_usage=89.97` | **UNKNOWN** | **UNKNOWN**：没有当前实现 |

当前返回没有 MemAvailable 不能证明后台未读取它；仅有一个百分比也不能区分 Total-Free、Total-Available 或其他算法，更不能反推出 MemAvailable。

## FOTA httpd 的原始数量路径

文件 SHA-256：`a0e3f3c7929cddb06ef5b8a8e7f194f2a81d9d263fd26edf7a93b03594adb885`。

`0xefd0` 的解析函数打开 `/proc/meminfo`（字符串 offset `0x14988`），按前四行依次 `fgets` + `sscanf`，匹配：

| Offset | 格式字符串 | 写入结构 offset |
| --- | --- | --- |
| `0x14998` | `MemTotal: %lu %*s` | 0 |
| `0x149ac` | `MemFree: %lu %*s` | 4 |
| `0x149c0` | `Buffers: %lu %*s` | 8 |
| `0x149d4` | `Cached: %lu %*s` | 12 |

它没有遍历所有键，也没有 MemAvailable 格式；后续行匹配结果未逐项检查。若 meminfo 第三行是 MemAvailable，这种固定行顺序还会使 Buffers/Cached 解析错位；这是静态代码缺陷，**当前设备实际输出顺序及影响未验证**，不能把其具体缓存值当作正确结果。

调用方 `0xf090` 在 `0xf0b6` 调用此函数；`0xf194` 加载 Total/Free，`0xf1a4` 的 `subs r3, r3, r2` 计算差值，再输出 `ram.used`。格式字符串 offset `0x14a54` 中明确包含 `ram: {total: %lu, used: %lu, free: %lu, buffers: %lu, cached: %lu}`。

分派表 `.data` 的文件 offset `0x19170` / ELF VA `0x1a170` 保存 `{0x14d9c, 0xf091}`：名称 `json_system_status` → Thumb 函数 `0xf090`。它是旧 ASP 模板 helper，**不是已定位的 `/api/device/info` REST handler**。函数地址与 byte offset 分开记录；该段 `.text/.rodata` 地址恰好与文件 offset 相同，不推广到所有 ELF section。

## FOTA mobile_svr 的百分比路径

文件 SHA-256：`18ce836184e70b077df550561665aa7c562d0d3082618eb5e40c2e6fa641892a`。

匿名函数 `0x1350` 的有效代码到 `0x1438` 前，执行以下静态链；下列命令字符串只被读取，**没有在设备或 PC 执行**：

```text
popen("free | awk 'NR==2' | awk '{print $2,$3}'", "r")
sscanf(line, "%d %d", &total_column, &used_column)
if total_column > 0:
    value = used_column * 100.0 / total_column
    text = format("%.2f%%", value)
    nvram_set_temp("mem_usage", text)
```

关键位置：命令字符串 `0x168c`；`0x13bc` sscanf；`0x13f8` 乘法；`0x1404` 除法；`0x1438` 的 double literal 为 100.0；格式 `0x1684`；字段 `0x1524`；`0x142e` 调用 nvram_set_temp。运算包含 int→float→double 及回到 float 的转换，上述伪代码表达数学口径，不声称是恢复出的原 C 源码。定时回调 `0xe38` 在 `0xe6a` 调用此函数，因此确有后台生产路径；不是仅凭字符串推测。

FOTA updater 声明把 `/usr/bin/free` 链接到 `/usr/lib/busybox/usr/bin/free`，该成员是 `#!/bin/busybox.nosuid` applet 入口；安装声明没有执行。

BusyBox SHA-256：`bf85d416c2109464cd533596d493054bd200fc8c2938ca97f3a92140dd1c85ab`，字符串版本 v1.29.3。free 代码入口 `0x3c64c`，`0x3c6a0` 调用 sysinfo；`0x3c6c4–0x3c6ca` 加载 totalram/freeram 并计算差，第二行列头为 total、used、free、shared、buffers、cached。随后单独输出 `-/+ buffers/cache:`，mobile_svr 的 `NR==2` 没有选取这个缓存扣除行。

更精确地，默认 free 的列为：

```text
total_column = floor(sysinfo.totalram * mem_unit / 1024)
used_column  = floor((sysinfo.totalram - sysinfo.freeram) * mem_unit / 1024)
mem_usage    = used_column * 100 / total_column，格式化为两位小数加 %
```

所以其口径是 **Total-Free**，并非 **Total-Available**。这确定了旧 NVRAM 生产者的行为，但当前 PATH、free、mobile_svr 的在线版本/hash 和 8081 对 `mem_usage` 的消费关系均未取得，不能跨过这些条件确认当前 89.97 的来源。

## MemAvailable 是否存在

原 ZIP 成员字面搜索无 MemAvailable，但进一步解压 **内核** 后，在派生文件 offset `0xed36ba` 找到 `MemAvailable:`，邻近 `MemTotal:`、`MemFree:`、`Buffers:`、`Cached:`。

boot.img kernel 起始 offset 4096，gzip 起始 offset 21868（kernel 内 17772），解压 19,680,276 bytes；SHA-256 `e287e54737b374bd17a129406283b8d7f9bc43166348e02803fb85a2776c391a`。本轮重新从 ZIP 解压并与既有派生文件逐字节匹配，来源可复核。该输出字符串是参考内核含此字段的静态线索，不能替代当前 `/proc/meminfo` 原文。

MemAvailable 表示不发生 swap 时可供新增应用使用的估计容量，会考虑可回收缓存和保留水位；不能直接用 Free+Buffers+Cached 当作等效值。[Linux 内核文档](https://docs.kernel.org/filesystems/proc.html#meminfo)

## 最小修改条件方案：未实现、未部署

优先方案：取得当前 route handler 后，在**现有 8081 RAM 分支**按键名解析同一份 `/proc/meminfo`，读取 MemTotal 和 MemAvailable，保持 `memory_usage` 的数字类型及百分比单位，只改变该字段的计算：

```text
memory_usage = round((MemTotal - MemAvailable) / MemTotal * 100, 2)
```

要求 MemTotal>0、0<=MemAvailable<=MemTotal；缺失/异常时明确报告数据不可用，不能伪造 0%、用 clamp 掩盖问题或把旧 free 当作新口径。按键读取，不能复制旧 httpd 的固定行序解析。

如果只需要诊断扩展，可在 system_status 增加真实 `MemAvailable` 并约定 kB 单位。**只增加它不会改变既有 memory_usage；当前 API 没有 MemTotal，客户端也不能仅靠这一新字段独立计算正确百分比。** 若要求客户端计算，还需实际 Total 数据，这是另一项合同变更。

若 handler 只消费 NVRAM 百分比，需要先确认其能否只读 meminfo 或取得受支持的原始数据；不能从 `mem_usage` 推导 Available，也不先修改共享的 mobile_svr/NVRAM 生产者。能否增加字段在逻辑上可设计，当前后台的代码、权限和部署适配性仍 **UNVERIFIED**。

未来实现后才进行离线样本验算（乱序键、缺失/非法值、有效 Total/Available）及同次 meminfo/API 对照。没有当前可修改文件，所以本轮没有后端代码改动、补丁或部署操作。
