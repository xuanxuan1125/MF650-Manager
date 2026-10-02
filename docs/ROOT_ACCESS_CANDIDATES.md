# MF650 Root 入口候选实证

日期：2026-10-02。依据前阶段完整 TCP 扫描、7 个端口的 3/3 复测与实际协议输出，并补充本轮标准 SYNC 结果；本轮未重复扫描或 shell 测试，不按端口号推测服务。

**已验证可用 root shell：NONE。SSH/Telnet 候选：未发现真实 Banner。没有取得 uid=0。**

## Port 7628：协议线索，身份验证失败

- Service：ADB-compatible transport；NOT usable ADB shell。
- Evidence：有效 CNXN response；adb connect 成功；adb devices -l 中显示 device。
- Confidence：HIGH（上述握手事实）；root 身份 NOT VERIFIED。
- 唯一身份验证：adb -s 192.168.100.1:7628 shell id。
- 完整命令输出：error: closed。
- 失败后 adb devices 仍显示 device，不能据此证明 shell 可执行。
- 后续标准 SYNC 审计：stock pull closed，原始 OPEN sync: 返回 CLSE，Sync Read=NO；未取得文件或目录，见 ADB7628_SYNC_AUDIT.md。
- 下一安全步骤：核对厂商公开协议或已有定制服务源码，解释可识别握手与 shell/SYNC 关闭的差异。没有依据继续猜服务名、RPC 方法或命令。

## Port 7689：ShellCrash 菜单终端

- Service：ttyd + ShellCrash menu。
- Direct root shell：NOT VERIFIED。
- Ctrl+C：closes session（此前完整实测；本轮禁止并未重复）。
- 当前 HTTP：ttyd/1.7.7-40e79c7 (libwebsockets/4.3.3-unknown)，标题 ttyd - Terminal。
- 当前 WebSocket：/ws，HTTP 101，Subprotocol tty；只握手，无初始化或键盘输入。
- 已知启动链路：ttyd :7689 → sh -c sh /usr/bin/feiliu.sh → ShellCrash。
- Confidence：HIGH（ttyd/菜单终端）；root 身份 NOT VERIFIED。
- 下一安全步骤：下一阶段取得 feiliu.sh 实际内容及启动参数后再分析；本轮不进入菜单、不尝试逃逸、不改服务。

## 不列为 Root 候选的开放端口

2358 仅确认 TCP OPEN 3/3，没有 SSH/Telnet/HTTP/TLS/有效 ADB/JSON 证据。7777 确认 OPEN 3/3 并返回二进制数据，没有 shell 提示或已识别的 console 协议。它们属于未识别服务，不能把开放连接本身当作 root 证据。

53 为 DNS；80 和 8081 为管理 HTTP 前端，目前没有可用 shell 证据。只有一个已识别 ttyd 实例（7689）。

当前网络探测没有发现可直接进入并通过 id 验证的 root shell。这个结论限定于本轮网络暴露与安全探测；不涵盖 localhost-only 服务、本地 Unix socket 或未知二进制协议的功能。

RAM 部署保持暂停。证据和矩阵见 PORT_REDISCOVERY_REPORT.md 与 test-results/port-rediscovery/SERVICE_MATRIX.csv。
