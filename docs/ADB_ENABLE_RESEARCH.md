# ADB 启用可行性与停止条件

日期：2026-10-02。**尚未找到可靠、可重复、可回滚的 ADB 启用路径。** 完成了 FOTA 静态研究和唯一一次符合授权范围的 TCPIP 请求；设备文件读取仍不可用。

## 三条路线的具体结论

| 路线 | 离线证据 | 在线行动 | 结果/停止理由 |
| --- | --- | --- | --- |
| 7628 → tcpip:5555 | adbd 有标准服务分派；但受许可限制，配套 property_set 为空实现 | get-state、features 后仅发送一次 tcpip 5555；等待 3 秒只检查 5555 | FAIL：error: closed；5555 timeout；7628 仍 device |
| restart_adbd.asp | httpd xref 确认 systemctl restart adbd.service | NOT EXECUTED | 间接启动脚本写 USB serial / 可能写持久 adb_devid；不满足“无其他副作用” |
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

这些是继续条件，不是本轮验证成功的方案。没有为了完成报告而执行不满足用户条件的服务重启或 USB 切换。

## 持久化和安全判断

没有 adb root/unroot、persist.adb.tcp.port、boot/system/modem/NV 写入、FOTA 安装或设备文件 push。没有开放永久 LAN ADB。

FOTA 的默认 UID/认证组合有风险，但在线 UID 未验证。若未来实际出现无 RSA authorization 且 uid=0 的 TCP ADB，必须记录 **UNSAFE FOR PERMANENT LAN EXPOSURE**；仅作临时验证，不能自动持久化。当前 5555 没有连接成功，不生成虚构的认证/UID 结果。

RAM 修复仍受阻；没有 `/proc/meminfo`，没有当前 Web 原文件权限/属主和可执行 rollback，因此没有 RAM patch 或设备部署。
