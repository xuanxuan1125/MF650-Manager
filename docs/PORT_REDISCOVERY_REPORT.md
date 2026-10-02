# MF650 当前端口重新发现报告

日期：2026-10-02（Asia/Shanghai）。目标为 MF650 默认管理地址。RAM 部署已暂停；本报告记录前阶段端口发现结果，后续 SYNC 结论见 ADB7628_SYNC_AUDIT.md。

后续 FOTA 离线证据：adbd 默认 TCP 7628；tcpserver 主 bind 为 2358；qmi_ip_cfg 指定 7777。因此在线 7628 最强候选为真正的厂商定制 adbd，2358/7777 分别很可能对应 JSON tcpserver / QMI IP。在线 executable/hash/归属未读到，这些是对以下历史网络分类的补充推断，详见 [TCP7628_REVERSE.md](TCP7628_REVERSE.md)。没有重复全端口扫描；一次 tcpip 5555 失败，见 [ADB_ENABLE_TEST_REPORT.md](ADB_ENABLE_TEST_REPORT.md)。

## 结论

完整 TCP 1–65535 已逐一尝试，覆盖检查通过（65535 行、65535 个唯一端口）。确认开放 7 个端口，全部复测 3/3 成功。**没有取得可用系统 shell 或 uid=0 的证据。** 未识别服务仍保留未知，不能推导为隐藏 root 入口，也不能排除其存在其他需要协议或凭据的功能。

| TCP 端口 | 前阶段网络实测分类 | 置信度 | 主要证据 |
| --- | --- | --- | --- |
| 53 | DNS | HIGH | 有效 TCP DNS 响应，UDP DNS 两次响应 |
| 80 | Padavan HTTP 管理前端 | HIGH | HTTP 200，Server: httpd，标题 5G MIFI，Padavan 资源/ASP 页面 |
| 2358 | OPEN but unknown | LOW | TCP 3/3；无 SSH/Telnet Banner；CRLF、HTTP、TLS、CNXN、空 JSON 没有可识别服务响应 |
| 7628 | ADB 兼容受限传输；Shell/SYNC 均不可用 | HIGH（传输层） | 有效 CNXN、devices=device；前阶段 id closed；后续 OPEN sync: 返回 CLSE |
| 7689 | ttyd / ShellCrash Frontend | HIGH | ttyd 1.7.7，/ws 101，子协议 tty；ShellCrash 菜单见此前完整终端实测 |
| 7777 | Unknown Binary | LOW（服务身份） | TCP 3/3；空 JSON 对象探测收到 20 字节非 JSON 二进制响应 |
| 8081 | Advanced REST / HTTP 管理前端 | HIGH | / → 302 /mf650.html；目标页 200；/api/system/info 返回 200 application/json |

未发现 SSH Banner、Telnet/登录/BusyBox/裸 shell 提示。未发现第二个 HTTP/xterm/ttyd 实例。所有开放 TCP 端口都尝试了 HTTP HEAD/GET、HTTPS HEAD/GET 与 TLS 握手，没有确认 HTTPS 服务。

## 网络与目标核实

- 已核对设备接口、本机地址、该接口网关与 DHCP、目标 ARP 和路由，扫描前后目标一致且可达。
- Windows 存在多个网络出口；自编 socket 与 curl 绑定已核实的设备接口地址，curl 禁用代理。没有更改路由或代理配置。
- 本机 IP、MAC、目标 MAC、接口标识及完整网络拓扑已从公开报告移除，只保留在 PC 本地忽略目录。
- 前后 ping 均 4/4 成功；扫描后最大 RTT 1 ms。HTTP 基线为 80:200、8081:302。

原始 ipconfig、route print、arp、Get-NetIPConfiguration/Get-NetRoute/Get-NetNeighbor、网卡信息与前后 ping 均保存在 test-results/port-rediscovery。

## 完整 TCP 与复核

本机无 nmap，使用 Python asyncio TCP Connect Scan；不使用漏洞、暴力认证或 DoS 脚本。用时 1022.685 秒（包含开放端口复测）。最大并发 128，超时 500 ms，速率上限 200/s；实际平均约 64 个端口/s。

原始结果：7 OPEN、65525 TIMEOUT_UNCONFIRMED、3 ERROR_UNCONFIRMED。**超时表示本次没有连接成功，不能直接等同于已证明 CLOSED。**

25、110、143 的 Windows asyncio 返回 WinError 1214，Windows 同步 socket 重测各 3 次返回 WinError 10049。没有将这些本地错误当成设备关闭。另用已有 WSL Linux socket 经既有 NAT 路由重测，三个端口均 3/3 Connection refused（errno 111）；这是不同客户端路径的补充证据，没有修改设备、Windows 网络或防火墙。

每个首次 OPEN 都在完整扫描后独立连接 3 次，7 个端口全部 3/3。65535 行 CSV 的连续范围、数量与唯一性已程序校验。扫描期间 HTTP 监测 204 次、失败 0 次、最大响应时间 8.364 ms，没有触发降速。

## HTTP、资源与 WebSocket

80 的 HEAD/GET 为 200，Server=httpd、Content-Type=text/html；8081 根路径为 302，Location=/mf650.html，无 Server Header；7689 为 200，Server=ttyd/1.7.7-40e79c7 (libwebsockets/4.3.3-unknown)、Content-Type=text/html。这些根路径未出现 WWW-Authenticate；不能据此保证所有功能均无认证。

下载并静态分析了 26 个当前 HTML/JS/CSS 资源，共约 2.30 MB；没有执行 JavaScript、提交表单或调用写 API。跟随同设备静态资源和已知门户的 iframe 路径，跳过动作、查询参数及非静态接口；外部 CDN 仅记录，未抓取。

明确设备网络引用为 8081、7689、9999。冒号数字正则另产生 JS/CSS 常量，共 82 个候选，全部追加 TCP 连接检查；没有发现新的开放端口。9999 未连接成功。资源闭包只涵盖上述安全静态边界，不代表下载所有后台动态接口。

7689 源码包含 xterm、相对 /ws 路径、new WebSocket(...,["tty"])。本轮仅握手：HTTP 101 Switching Protocols，Sec-WebSocket-Protocol: tty；未发送初始化 JSON、Ctrl+C、终端输入或任何菜单选项。没有尝试通过 URL 参数逃逸或修改 ttyd。

## 7628 与旧资料冲突

旧资料称 7628 为 tcpservice JSON-RPC，用户之前说明其不是可用 ADB。本轮不能继承该服务分类：

1. 发送标准 ADB CNXN（无 OPEN、WRTE 或命令），收到有效 CNXN，magic、长度和校验和匹配；身份 payload 为 device::ro.product.name=;ro.product.model=;ro.product.device=;。
2. adb connect 192.168.100.1:7628 输出 connected to 192.168.100.1:7628。
3. adb devices -l 列出 192.168.100.1:7628 device transport_id:1。
4. 仅执行一次 adb -s 192.168.100.1:7628 shell id，完整输出为 error: closed，无 UID；后续 adb devices 仍列为 device。

因此记录为 **ADB 兼容握手端点，NOT usable ADB shell，Direct root NOT VERIFIED**。不能因 connected/device 就称其可用 ADB，更不能称 root。也不能因 shell 关闭而抹掉有效 CNXN 的实测证据。是否为厂商兼容桥接或定制服务，当前无法确定；本轮没有通过 JSON/RPC、替代 shell 服务名或其他命令尝试绕过。

CNXN 构造参考 [AOSP ADB 协议](https://android.googlesource.com/platform/system/core/%2B/refs/tags/android-11.0.0_r20/adb/protocol.txt)，只用于协议识别，不用于认证绕过。

7777 的唯一可识别应用层证据来自空对象 {} 加 CRLF，无方法、动作、路径或命令；返回 20 字节二进制，原始内容仅保存在 PC。它不是有效 JSON，也不是已验证 SSH、Telnet 或 ADB；不能凭此命名为 QMI、串口或调试 shell。

## 有限 UDP

只探测要求的 53、67、68、123、161、500、1900、4500、5353，每个 2 次。53 返回有效 DNS。其余均无响应，标记 OPEN_OR_FILTERED_NO_RESPONSE，不能当作 CLOSED 或确认具体服务。使用 DNS/NTP/SSDP/mDNS 只读查询；对其余端口仅发零长度 datagram，没有 DHCP 租约请求、SNMP community 猜测或 IKE 配置。

## Root 候选与本轮停止点

见 ROOT_ACCESS_CANDIDATES.md。7689 仍是菜单终端；其 Ctrl+C 关闭来自此前已记录的实测，本轮未重复。7628 的 id 失败。2358、7777 没有真实 shell 证据。当前最可能的**已验证可用** root 入口：NONE。

未取得 uid=0，未执行 /proc/meminfo 或其他设备文件命令。未重启、启停服务、开放端口、改防火墙/iptables、改 ttyd/feiliu.sh、端口转发、写设备文件或上传二进制。RAM 部署继续暂停，等待用户下一阶段指令。

## 输出文件

- test-results/port-rediscovery/SERVICE_MATRIX.csv：确认开放 TCP、有限 UDP、重点非开放候选及错误复核的矩阵。
- port_rediscovery_tcp.csv、scan_summary.json、tcp_repeated_checks.json、scan_error_rechecks*.json：全范围及复测。
- service_fingerprint.txt、fingerprint_evidence.json、逐端口 *.curl.txt/*.headers/*.first2kb：原始协议及 HTTP/HTTPS 输出。
- protocol_evidence.json、adb-id.txt、adb-devices-after-id.txt、empty_json_probes.json：协议握手、身份失败与二进制证据。
- resource_manifest.json、referenced_ports.txt/json、resources/：资源及端口引用；udp_evidence.json：有限 UDP。

原始结果仅保存在 PC，不提交公共 GitHub。此脱敏报告随受限 ADB SYNC 审计提交；未恢复 RAM 部署。
