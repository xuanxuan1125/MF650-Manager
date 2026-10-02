# MF650 ADB Patch 在线预检

2026-10-02，Asia/Shanghai。目标 `192.168.100.1`；参考工程基线 `260cf6eb00f6175dc0c2f46e7f8c0281f8c20104`。本轮仅进行线上页面 GET、Windows USB 枚举和本地文件分析。

**结论：WEB_COMMAND_CONSOLE_UNAVAILABLE / PATCH_PRECHECK=FAIL / Recovery Installability=UNVERIFIED。Patch 未安装，未提交设备持久修改操作，RAM 部署继续暂停。**

FAIL 表示安装前置条件不齐，不表示已经测得在线 adbd hash 不匹配。正常 Web 命令提交链未找到，按用户停止条件没有执行 `id` 或后续系统命令。完整在线事实、请求清单及证据边界见 [ONLINE_SYSTEM_FACTS.md](ONLINE_SYSTEM_FACTS.md)。

## FACT：执行范围与停止点

已读取 WEB_ADB_INTERFACE_AUDIT、FOTA_ANALYSIS、FOTA_ADB_ARCHITECTURE、ADB_PATCH_DESIGN、ADB_PATCH_BUILD_REPORT、ADB_ENABLE_PATHS、BOOT_USB_COMP_WRITERS。

本轮共 **9 次 GET，全部 HTTP 200；POST 0，命令提交 0**。检查用户指定的系统页、升级页，以及系统页实际引用的相关 JS 和导航中的系统信息、设置、日志页。发现的正常表单用于设置、清日志或上传固件，没有发现系统命令输入及其真实提交链。没有将浏览器调试用 `console.log` 当作系统控制台。

停止后没有调用 `console_response.asp`、猜测 CGI/参数、提交 `apply.cgi`、上传 ZIP 或继续寻找命令入口。没有端口扫描、ADB shell/SYNC/tcpip/usb、ttyd Ctrl+C、服务重启、reboot、USB/NV/分区修改或 remount。记录的是本轮未提交修改操作，不据此断言服务器处理 GET 时不会产生访问日志。

没有 `id` 请求或输出可保存：状态为 **NOT EXECUTED**，UID/GID/groups 全部 UNKNOWN。HTTP 页面原文和响应头保存在本地忽略目录 `test-results/adb-patch-online-preflight/`；管理凭据字段不进入本文或公开文档。

## 安装前八项硬门槛

| 条件 | 结果 | 证据及限制 |
| --- | --- | --- |
| 在线 `/sbin/adbd` SHA256 与 FOTA 完全一致 | UNKNOWN | 未取得在线文件或 hash；ONLINE_ADBD_MATCH=UNKNOWN |
| 当前 `/etc/usb/boot_hsusb_comp` token 为 9057 | UNKNOWN | 未读取；USB PID 不代替文件 token |
| `/systemrw/boot_hsusb_comp` override 已确认 | UNKNOWN | 未读取，不能标 NOT PRESENT |
| 当前实际 USB PID 为 9057 | PASS | 16:24:05 本机 `Get-PnpDevice -PresentOnly`：05C6:9057，Net/OK |
| Patch binary diff 只有预期 2 bytes | PASS | 本轮逐字节复核，只有 `0x5E1A`、`0x5E1B` 不同 |
| Rollback 恢复载荷为原 hash | PASS（离线） | ZIP 内载荷与原 adbd 逐字节一致；未在 recovery 执行恢复 |
| 当前升级入口/recovery 接受规则已确认 | UNKNOWN | 只有当前前端和旧 FOTA 的后端静态证据 |
| 当前 `/system` 实际挂载/更新机制已确认 | UNKNOWN | 在线挂载与 recovery 分区映射未取得 |

**3 项通过（其中回滚仅为离线载荷验证），5 项 UNKNOWN。任一 UNKNOWN 即不安装；当前不能进入真正安装讨论阶段。**

## FACT：本地产物复核

未重建或修改上一阶段产物。原 FOTA ZIP 的 `system/sbin/adbd` 与本地 patched-adbd 均为 30,380 bytes。覆盖 literal 为 `0x5E18: 90DB → 9057`，实际差异只有 `0x5E1A: 44→35` 和 `0x5E1B: 42→37`；其余 30,378 bytes 相同。

| 对象 | SHA256 |
| --- | --- |
| 原 FOTA adbd | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` |
| patched adbd | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` |
| Enable ZIP，766,095 bytes | `c25d750436fcb013092b3c4a69254e938afd4c157c3f774af468a93af52cc0d5` |
| Rollback ZIP，766,097 bytes | `7fb8665438fdbb3179010072aeb2c9c550a5c9beef5cfe603f4aea72d7512c24` |

本轮两个 ZIP 的 CRC 检查通过；Enable 载荷等于本地 patched-adbd，Rollback 载荷等于原 FOTA adbd。包内保护、厂商尾校验、9 项既有离线测试和 ELF 验证见 [构建报告](ADB_PATCH_BUILD_REPORT.md) 及 [设计报告](ADB_PATCH_DESIGN.md)。本轮没有运行 ARM 固件、updater 或包内 BusyBox；这些 PASS 不代表设备恢复路径通过。

## FACT / UNKNOWN：升级入口与 recovery 接受规则

当前 `Advanced_FirmwareUpgrade_Content.asp` 正常上传表单为 `POST upgrade.cgi`、`multipart/form-data`、文件字段 `file`。页面调用 `checkSERIALNO()`，并在 `login_safe()` 返回 false 时禁用上传；`fwUpload()` 检查文件字段非空后提交。未提交该表单，也未调用 upgrader。

以下后端事实仅属于 **MF650_2.3_Fota.zip**：

- `system/usr/bin/httpd` SHA256 为 `a0e3f3c7929cddb06ef5b8a8e7f194f2a81d9d263fd26edf7a93b03594adb885`。上传处理函数 `0x10844` 经 multipart parser `0x104B0` 保存 `/data/update_ota.zip`，在 `0x108AE` 调 `is_update_file_valid`。失败分支 unlink；这些函数未在线调用。
- 同一函数还有 `rf_hwid==500`、包尺寸大于 100 MiB、`get_modem_int_ver()<=13` 的组合拒绝条件。这是该旧后端的硬件/调制解调器保护，不证明它校验任意 ZIP 的软件版本。
- `0xA5C0` 后处理检查候选文件及状态条件后，以 `notify_rc("flash_firmware")` 交给后续服务；服务到当前 recovery 的完整接受链仍缺证据。
- 旧 `libshared.so` 的 `is_update_file_valid 0x8F84` 验证厂商 33 字节包尾：32 位 ASCII MD5 加 LF，摘要输入为 ZIP 正文加固定 1,982 bytes 校验输入。验证过程会改写/截断待验文件，本轮只分析其代码和 PC 内存等效计算。详见 [设计报告](ADB_PATCH_DESIGN.md)。
- 原包不含 JAR CERT/SF/RSA 条目，EOCD comment 长度为 0。这不是当前 recovery 无签名要求的证明。原包有 recovery-from-boot.p 等恢复相关数据，但没有提供可直接核实当前接受策略的完整 recovery 可执行文件、信任 keys 和分区配置。
- `update-binary` 注册了 mount、package_extract_file、run_program 等所需 Edify 函数；原脚本以 UBIFS/UBI 挂载 system。其 GPT/sparse CRC 字符串不证明 Web 检查 ZIP CRC 或 recovery 接受候选包。

| 接受规则 | 当前前端/旧 FOTA 的已知事实 | 当前实机判定 |
| --- | --- | --- |
| RSA / ZIP 数字签名 | 当前前端未见验证；原包无 JAR 签名条目；缺 recovery 信任配置 | UNKNOWN |
| 厂商尾校验 / 自定义 checksum | 旧 FOTA libshared 的 MD5+固定校验输入流程已确认；候选包沿用格式 | UNKNOWN：当前库身份未核实 |
| 版本号 | 当前 UI 标注 V3.4；旧后端有上述 modem version 组合保护 | UNKNOWN：完整在线接受条件未取得 |
| manifest / metadata | 原 metadata 为 FILE OTA，型号/构建字段为 None | UNKNOWN：不证明当前 recovery 忽略 manifest |
| CRC | 两个本地 ZIP CRC 通过；旧 updater 有 GPT/sparse CRC 相关代码 | UNKNOWN：在线上传/recovery 的包校验策略未确认 |
| `/system` 挂载/写入 | 原 FOTA 挂载 UBIFS/UBI system；候选包沿用原参数 | UNKNOWN：在线/recovery 状态及工具依赖未验证 |

**Recovery Installability=UNVERIFIED。** 不将旧 FOTA 静态兼容性、客户端未见检查或本地 CRC 通过当作当前设备接受证明。完整 EDL 恢复仍 [NOT READY](EDL_RECOVERY_READINESS.md)。

## INFERENCE：Patch 逻辑适配

当前页面固件标签为 `MF650_V3.4`，参考包为 2.3，提示在线组件可能与参考版本不同；没有在线文件 hash，不能将 ONLINE_ADBD_MATCH 标 NO 或 YES。

当前枚举 PID=9057 只证明 USB descriptor。boot 文件和 override 均未读取，因此落在用户 Case D：**PATCH_LOGIC_MATCH=UNKNOWN**。不能据 PID 推断 boot token，也不能据以解释此前 shell/SYNC closed。

## UNKNOWN：RAM 与下一步

未取得完整 `/proc/meminfo`、`/proc/swaps` 或 `free`。MemTotal、MemFree、MemAvailable 及其他 RAM/Swap 字段全部 UNKNOWN，当前旧 RAM/新 RAM 均 UNKNOWN；没有复用历史 89.35% 作为本轮值，也没有从缓存字段猜 MemAvailable。取得实际数据后才可计算：

```text
legacy_used = MemTotal - MemFree
legacy_usage = legacy_used / MemTotal * 100
effective_used = MemTotal - MemAvailable
usage = effective_used / MemTotal * 100
```

用户要求的 RAM_FIX_REPORT 更新以取得真实 meminfo 为条件，本轮未满足，故未修改该报告或部署 RAM 修复。

下一步应先获得厂商支持的只读维护/导出方式或现有在线备份，核实 adbd、boot token、override、挂载信息与当前升级/recovery 规则。未发现这样的入口前，不猜 CGI、root 端口或绕过权限。**现有证据不足以支持安装；Enable/Rollback 保持本地离线候选状态。**
