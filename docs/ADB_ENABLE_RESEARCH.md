# ADB 启用可行性与停止条件

日期：2026-10-02，服务许可逆向阶段更新。**找到参考 ELF 的非持久厂商授权分支，但尚未找到配套合法凭据/客户端和已经验证的在线回滚流程。** 当前没有可立即执行的启用方案；设备文件读取仍不可用，RAM 修复暂停。

本轮确认 BSS `0x8044` 的两处写入：boot 匹配的 `0x3588`，以及两个序列号来源共用的授权成功写入 `0x38f8`。授权辅助函数做状态混合、临时文件和 MD5 摘要；不写 boot/systemrw/MTD/NV。没有生成授权码、提取固定状态、发送授权请求或 patch。见 [授权完整分析](ADB_VENDOR_AUTH_REVERSE.md)。

全量覆盖 63 ASP、52 handler（3 NULL）、35 个注册 RPC 和 1 个容量不足的描述项、20 个 boot 路径命中文件；本轮仅刷新已知页面/静态资源 24 次 GET，22 个 200、两个未跟随的 302。未发现专用 Web/RPC ADB 开关。见 [Web](WEB_ADB_INTERFACE_AUDIT.md)、[2358](TCP2358_RPC_REVERSE.md)、[boot 写入者](BOOT_USB_COMP_WRITERS.md)。

当前风险/路径判定和明确下一步方案统一见 [ADB_ENABLE_PATHS.md](ADB_ENABLE_PATHS.md)：优先取得厂商已有客户端、合法发放凭据及撤销说明；不把序列号摘要逆向当成自己制造凭据的授权。

## 前阶段三条路线与本轮重评

| 路线 | 离线证据 | 在线行动 | 结果/停止理由 |
| --- | --- | --- | --- |
| 7628 → tcpip:5555 | adbd 有标准服务分派；但受许可限制，配套 property_set 为空实现 | get-state、features 后仅发送一次 tcpip 5555；等待 3 秒只检查 5555 | FAIL：error: closed；5555 timeout；7628 仍 device |
| restart_adbd.asp | httpd xref 确认 systemctl restart adbd.service | NOT EXECUTED | 正常稳定 serial bookkeeping 在已核实条件下 R1；在线依赖/override 未核实；解锁 UNLIKELY；本轮明确禁止执行 |
| adb usb / 9059 | usb: 只改 TCP 属性，没有 composition 调用；9059 静态包含 RNDIS+ADB | NOT EXECUTED | 不能证明 usb: 进入 9059，也无可靠断网恢复 9057 的渠道 |

详见 [架构](FOTA_ADB_ARCHITECTURE.md)、[USB handler](ADB_USB_SERVICE_REVERSE.md)、[重启分析](RESTART_ADBD_ANALYSIS.md)、[测试完整输出](ADB_ENABLE_TEST_REPORT.md)。没有反复发控制请求，没有转向未知 RPC、厂商授权破解或改二进制。

## 已建立和仍缺失的证据

已建立：参考包完整 hash/清单、adbd 默认 TCP 7628、许可 flag 控制流、libcutils 空属性函数、tcpserver 主监听 2358、9057/9059/90DB 目标分支、systemrw 覆盖机制、当前 PnP 9057 与 TCPIP 失败结果。

仍缺失：在线 daemon/library/启动脚本 hash，在线 boot_hsusb_comp、systemrw 覆盖、unit 状态，独立的 USB composition 恢复入口，实际 USB ADB 驱动/网络恢复测试，shell UID 与实时 meminfo。

枚举 PID 不是 boot 文件值；FOTA root 倾向不是在线 root 身份；端口超时不是确认 CLOSED；transport=device 不是 Shell/SYNC 服务许可。这些区别保留在所有后续判断中。

## 后续可以推进的实际方向

1. 从已有审计备份或厂商明确支持的读取/维护方式取得当前 adbd、libcutils、launch_adbd、boot composition 与 feiliu.sh，先核实版本和服务限制；不通过猜凭据/替代服务名绕过。
2. 若考虑 9059，必须先有实际可用的独立恢复 9057 方法，再验证在线 composition 脚本、端点/驱动和 RNDIS 恢复。仅知道脚本命令或 FOTA 功能定义还不够。
3. 只有真实 shell 或 SYNC read 可用后，先读取 `/proc/meminfo`、`/proc/swaps`、pwd、实际 Web/后台文件和元数据；限定查找 /www、/usr、/home、/tmp、/data，不运行 find /。
4. 对读取到的 meminfo 用 `(MemTotal - MemAvailable) / MemTotal * 100` 与旧 `(Total - Free)` 比较，再核验 8081 `/api/device/info`；本轮仍不部署。

这些是继续条件，不是在线验证成功的方案。厂商临时授权的执行前检查、一次 shell/SYNC 读取及撤销方案见 ADB_ENABLE_PATHS；本轮未执行服务重启或 USB 切换，也未重复前阶段 shell/SYNC/5555/ttyd 测试。

## 持久化和安全判断

没有 adb root/unroot、persist.adb.tcp.port、boot/system/modem/NV 写入、FOTA 安装或设备文件 push。没有开放永久 LAN ADB。

FOTA 的默认 UID/认证组合有风险，但在线 UID 未验证。若未来实际出现无 RSA authorization 且 uid=0 的 TCP ADB，必须记录 **UNSAFE FOR PERMANENT LAN EXPOSURE**；仅作临时验证，不能自动持久化。当前 5555 没有连接成功，不生成虚构的认证/UID 结果。

RAM 修复仍受阻；没有 `/proc/meminfo`，没有当前 Web 原文件权限/属主和可执行 rollback，因此没有 RAM patch 或设备部署。
