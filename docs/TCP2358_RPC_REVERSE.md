# 2358 tcpserver 离线 JSON 分派表

日期：2026-10-02。**2358 在线请求数：0**。样本 SHA-256 `cb36787c93d83b4accf54d5477b216dae891aaa178034be598fef04f784baaf5`，47,008 字节。在线监听端口来自前阶段记录；本轮没有重扫端口或用任意 JSON 探测。

完整索引：[2358_RPC_METHODS.csv](../fota-analysis/reports/2358_RPC_METHODS.csv)。包括 method、参数、读写、功能、风险、handler VA、描述项 VA、response func、实际注册状态和副作用调用。**method 列是实际数字请求 id，response func 不是请求方法名。**

## 分派与容量

Client_Call：recv (`0x1e94`) 上限 512 字节，cJSON_Parse (`0x1eb2`)，cJSON_GetObjectItem (`0x1ec0`) 的 key 为 VA `0x8344` 字符串 `id`。读取 cJSON 的 valueint（结构偏移 0x14），按数字 id 查找注册表，匹配后 `0x1f1a BLX r3`，参数为连接 fd 和已解析 JSON 对象。

`0x1fcc` 注册函数扫描 **35 个** id/function 槽；`0x1f00`、`0x1fdc`、`0x201e` 的上限均为 0x23。初始化 `0x17f6` 从 `.data` VA `0xc008` 复制 0x120 字节（36 个 8 字节描述项），`0x17fa..0x1804` 逐项调用注册。第 36 项 id=1999/echo 因无空槽返回 -1，初始化未处理这个结果。CSV 保留该描述项，标注 **NO / 未注册**，不把它当作当前可调用 RPC。

id=1001/1009 还可能走已有缓存响应路径；“设备信息方法”不保证每次触发相同外部读取。网络在线版本和缓存状态未知。

## 已恢复的功能

35 个成功注册项：设备/SIM/WAN 信息、网络制式、APN、LAN/DHCP、Wi-Fi、重启、恢复出厂、终端列表、MAC 过滤、LTE/NR5G 频段锁定、升级文件接收、充电、SIM 切换策略、错误/诊断、临时关闭 Wi-Fi、日志和关机。

关键参数均从各 handler 的 cJSON_GetObjectItem 调用关联，例：网络 mode、APN apn、LAN 五个字段、Wi-Fi mode/ssid/maxSta/encrypt/wpaOption/key、MAC idmac/mac、频段 lock/lockBands、升级 filename/md5/filelen。CSV 的 id 是请求编号，不能误用通常为 2xxx 的响应编号。

没有在这 35 项中恢复出 ADB、ADBD、USB composition、任意 shell、debug unlock 或 service permit 专用 method；未见直接连接 adbd、写 boot_hsusb_comp 或调用 restart_adbd 的处理链。**2358 含 ADB 专用控制：NO（这份 FOTA 的已恢复表）。在线二进制未取 hash，不能扩大为所有版本都没有。**

READ 是功能分类，不是新增在线授权：读取信息也可能调用 AT、更新临时缓存或生成终端列表。Wi-Fi 信息/日志还可含敏感内容；本轮没有读取实际密码、SN、日志。设置频段/APN、重启 Wi-Fi/LAN、恢复出厂、刷写、关机等均有真实修改或中断，绝不盲发。id=1033 不只是查询升级状态，而是接收文件并触发 flash_firmware；id=1018 是恢复出厂，不能作为“启用 ADB”试探。

AT 线索只记语义：设备信息 handler 查询 OEMNAME/SWVER，频段读取 handler 使用 BANDCFG 查询当前列表；写 handler 能发送 BANDCFG/BANDCFGRESTORE 并断开/重连 WAN。FOTA USB 初始化脚本另有 `AT+USBCFG?` 读取，但这些证据没有给出安全的 ADB 许可 AT 接口。本轮 AT 执行数为 0。
