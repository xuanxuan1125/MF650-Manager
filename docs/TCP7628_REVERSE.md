# 7628 与 tcpserver 离线交叉分析

日期：2026-10-02。在线服务 hash 尚未取得。结论为：**7628 很可能是厂商定制的真正 adbd，但当前 Shell/SYNC/control service 不可用；不能称“假 ADB”或“完整可用 ADB”。**

## tcpserver 身份和 bind 常量

FOTA `/usr/bin/tcpserver`：47,008 字节，SHA-256 `cb36787c93d83b4accf54d5477b216dae891aaa178034be598fef04f784baaf5`。stripped little-endian ARM32 ELF，依赖 cJSON；strings 有 cJSON_Parse、`<Client_Call> Failed to cJSON_Parse`、alktcpserver。

主监听路径的 Thumb 指令：

```text
0x17a6: movs r2,#2
0x17aa: mov r8,r2
0x17b4: movt r8,#0x3609
0x17b8: str.w r8,[sockaddr]
0x17bc: 零初始化地址部分
0x17ce: bind(fd, sockaddr, 16)
0x17dc: listen(fd, 10)
```

写入字为 `0x36090002`，小端内存是 `02 00 09 36`：AF_INET=2，network-order port=0x0936=**2358**，地址 0.0.0.0。socket 参数为 AF_INET/SOCK_STREAM。这个结论来自 sockaddr 和 bind 的数据流，不依赖可见十进制端口字符串。

另一个 File_Update 路径中，0x1ca0 建 SOCK_STREAM socket，0x1cc0 构造 `0xb5160002`，0x1cd6 bind，关联路径 0x1c48 listen；字节 `02 00 16 b5` 表示 **5813**。这是离线更新服务候选，没有新增在线扫描或连接，更未调用更新功能。

接收路径在 0x1eb2 调 cJSON_Parse，随后按 JSON 请求分派 Client_Call；无效 JSON 有错误日志。没有向设备发送该服务的方法、动作或命令。

完整 binary 字面量检查没有 adbd、/sbin/adbd、ADB、CNXN、shell:、sync:、tcpip:、7628。缺少字面量本身不排除所有间接代码；与明确 2358 bind 和 JSON 路径结合，**此 FOTA tcpserver 没有支持“它就是 7628 ADB compatibility server”的证据**。

## adbd 的直接 7628 证据

FOTA adbd 在 0x258c 执行 `movw r0,#0x1dcc`（7628），0x2590 调 libadbd local_init。它同时包含真实 service_to_fd 和 shell/exec/SYNC/control 分派。属性库只返回默认值，因此默认 TCP 7628 是可达正常启动路径，详见 [架构分析](FOTA_ADB_ARCHITECTURE.md)。

在线前阶段标准 CNXN 有效，adb devices=device，产品属性为空，shell id 与 SYNC OPEN 后关闭；本轮 tcpip:5555 也 error: closed。这与定制 adbd 的属性空实现和厂商服务许可限制相容，不能证实在线二进制相同或具体哪个条件拒绝了服务。

## 四种分类的证据权重

| 分类 | 当前结论 | 理由 |
| --- | --- | --- |
| A 真正 adbd（厂商定制） | **LIKELY，最强候选** | FOTA 明确默认 7628、真 ADB 服务分派；在线标准握手和受限响应相容 |
| B 新版 tcpserver 加 ADB compatibility | 未证实 | FOTA tcpserver 明确为 2358 JSON；在线新版文件未知 |
| C ADB proxy / bridge | 未证实、不能完全排除 | 没有在线 executable/hash 或进程 socket 归属 |
| D 其他 vendor transport | 泛称不足以进一步分类 | 已证明 ADB 协议握手，具体 executable 未读到 |

确认 A 需要实际 daemon 文件 hash、启动参数或进程/监听归属；当前没有安全读取渠道。无需再次全端口扫描，也不能通过猜服务名或厂商授权字段来绕过限制。

## 7777 与旧端口报告

FOTA 两份 `/etc/qmi_ip_cfg.xml`、`/etc/data/qmi_ip_cfg.xml` 均配置 tcp_server_port 7777。qmi_ip_multiclientd.service 指向 `/usr/bin/qmi_ip_multiclient /etc/data/qmi_ip_cfg.xml`。

结合前阶段二进制响应，在线 7777 **高度可能是 QMI IP binary service**；在线 socket 归属仍未证明。2358 同理为 FOTA tcpserver 对应的强候选。旧报告的 unknown 是当时网络证据结论，新离线证据没有变成新的在线身份验证。没有再向 2358/7777 发包，二者均不列为 shell 入口。
