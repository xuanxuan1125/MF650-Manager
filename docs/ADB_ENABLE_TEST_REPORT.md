# ADB 临时启用测试报告

日期：2026-10-02（Asia/Shanghai）。顺序为先离线确认，再只执行一次用户授权的 TCPIP 服务请求。**结果：未恢复 ADB 文件访问；RAM 部署保持暂停。**

## 基线与完整命令响应

PC ADB 为 platform-tools 35.0.1。测试前已记录 USB 05C6:9057、Windows PnP、RNDIS 地址与网关；7628 TCP 可连接，5555 连接超时。完整网络数据留在忽略目录，公开报告只保留默认管理地址。

```text
> adb -s 192.168.100.1:7628 get-state
stdout: device
stderr: <empty>
exit: 0

> adb -s 192.168.100.1:7628 features
stdout: <empty>
stderr: <empty>
exit: 0

> adb -s 192.168.100.1:7628 tcpip 5555
stdout: <empty>
stderr: error: closed
exit: 1

[等待 3 秒；仅检查 TCP 5555]
state: NOT CONNECTABLE
error: timed out

> adb devices -l
List of devices attached
192.168.100.1:7628     device transport_id:2

stderr: <empty>
exit: 0

> adb -s 192.168.100.1:7628 get-state
device
exit: 0
```

tcpip 命令没有重复发送。5555 未 TCP OPEN，因此未 adb connect 5555、未在该端口发 id 或 meminfo。`error: closed` 不能单凭文字判为切换成功；等待后端口和 transport 复核没有出现成功证据。

5555 的正确状态是 **NOT OPEN / timeout（连接未成功）**。本次没有 Connection refused 或 RST 的证据，不能严谨地填“确认 CLOSED”。

## 设备状态与条件性未执行项目

测试后同一 PnP instance 仍为 05C6:9057、Net、OK；RNDIS Up，IP 和网关与记录的基线一致。未观察到 USB ADB Interface。

restart_adbd.asp 的直接调用已反汇编，但完整 service 启动链会处理 USB serial/可能持久 adb_devid，且当前 unit 状态未知；**未满足无其他副作用条件，GET 没有执行**。

usb: handler 未调用 9059 composition；没有可靠恢复 9057 的独立入口；**未满足用户条件，adb usb 和直接 composition 切换都没有执行**。

未执行 adb root/unroot、remount、push、reboot、kill、改服务、防火墙、持久属性、启动脚本、FOTA/分区/NV 操作。本轮唯一设备控制服务尝试是上述已授权的 tcpip:5555；没有报告为“全程纯只读”，也没有可见状态变化或设备文件部署。

## 明确结论表

| 要求项 | 结论 |
| --- | --- |
| FOTA 包含真正 adbd | **YES**，完整服务/transport 实现 |
| TCP ADB 支持 | **YES（FOTA）**：默认 7628；标准 tcpip 动态切换受限且配套属性为空实现 |
| USB ADB 支持 | **YES（FOTA 定义）**：端点/transport 与 9059/90DB 脚本；在线开启未验证 |
| 当前 9057 | RNDIS : ECM，无 USB ADB/DIAG |
| 9059 | c.1 RNDIS+DIAG+ADB；c.2 ECM，离线候选 |
| 90DB | DIAG+DUN+RMNET+DPL+QDSS+ADB，无 RNDIS/ECM |
| 在线 7628 分类 | **LIKELY genuine vendor-custom adbd**；hash/进程归属未知，Shell/SYNC 不可用 |
| restart_adbd.asp | 请求 systemctl restart adbd.service；间接副作用见专篇；**NOT EXECUTED** |
| adb tcpip 5555 | **FAIL**，一次，error: closed |
| ADB 5555 | **NOT OPEN**，timeout；不能确认 CLOSED |
| adb usb | **NOT EXECUTED** |
| 当前 USB ADB | **NO（未观察到可用 USB transport）** |
| ADB shell | **NO**；前阶段 id error: closed，本轮未重试 shell |
| UID | **UNKNOWN**；未得到 uid=0 或 shell UID |
| /proc/meminfo | **FAIL（未取得）**；前阶段 SYNC closed，本轮未恢复读取 |
| 新 RAM 百分比/误差检查 | NOT EXECUTED，无实时 MemAvailable |
| RAM 修复部署 | NOT EXECUTED |

此前标准 SYNC 的完整输出见 [ADB7628_SYNC_AUDIT.md](ADB7628_SYNC_AUDIT.md)，ttyD/ShellCrash 停止点见 [TTYD_CONNECTION_CHECK.md](TTYD_CONNECTION_CHECK.md)。它们没有在本轮反复测试。

## 离线验证与产物

- 4,551 个解压文件大小/hash 全部匹配，9 个大小写别名都有原路径映射。
- USB 矩阵 66 个唯一 PID 覆盖全部四位十六进制脚本；9057/9059/90DB 分支人工复核。
- GNU Thumb objdump 与 Capstone 对默认 7628、属性四字节空函数、服务 gate、tcpserver bind、httpd system 调用作交叉检查。
- 没有 Android App/服务代码更改，不生成 APK，不声称 Android 或部署后页面已测通过。

新增源码只做本地 ZIP/ELF/脚本文本分析，无设备连接或固件执行能力。完整 FOTA、解包内容、依赖二进制和 PnP/网络原始日志均保持本地。提交仅含脱敏报告、清单/hash、矩阵和分析代码。

本阶段未形成已验证开启路径，不使用成功的 feature commit 标题。继续条件见 [ADB_ENABLE_RESEARCH.md](ADB_ENABLE_RESEARCH.md)。
