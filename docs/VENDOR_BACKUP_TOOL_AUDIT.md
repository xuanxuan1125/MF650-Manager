# MF650 Web 备份工具静态安全审计

2026-10-02。**工具包安全性 UNKNOWN；BAT 的远端动作 R0；完整导出当前 8081 后端的能力 UNKNOWN。** 原工具未执行，没有任何设备连接。本报告穷尽可见 BAT 的全部活动命令，但不把原生 ADB 的功能字符串等同于完整实现审计。

## 组成与身份

ZIP SHA256：`64606f0aa44f73b7443f5f3c12fb79342178f2686f2a86500781f2cb37804ee2`，2,813,276 字节，ZIP 成员 CRC PASS。包内 4 个文件，无后台文件、服务配置或额外备份脚本：

| 文件 | 字节数 | SHA256 |
| --- | ---: | --- |
| 双击备份www.bat | 446 | `780497ef629e45107d3b3c5eeaaec7cef7ae78928d8f8d260bddad13713223a4` |
| adb.exe | 5,992,960 | `bce1f6f6f6532266837b66d719bfbd177d406d6d7d8d5adfd5e7c59fcde651a5` |
| AdbWinApi.dll | 97,792 | `d60103a5e99bc9888f786ee916f5d6e45493c3247972cb053833803de7e95cf9` |
| AdbWinUsbApi.dll | 62,976 | `25207c506d29c4e8dceb61b4bd50e8669ba26012988a43fbf26a890b1e60fc97` |

路径前缀为 `web后台备份工具/`。这三个 PC 二进制与另外两个 ZIP 中对应文件逐字节相同，不能由此证明官方来源或可信性。PE 证书表为空；没有执行 `adb version`，没有在线查询签名/官方 hash，不能报告已验证官方版本或认证结果。

## BAT 的完整活动行为

BAT 为 GB18030 可解码文本，共 33 行。第 1 行关闭命令回显，第 3 行设置窗口标题，第 12/32 行等待按键，其余为显示文本/空行，第 33 行退出。唯一设备/客户端命令是第 18 行：

`adb pull /www .\ && (ECHO.备份web成功) || ECHO.备份web失败`

这是静态引用，未执行。读取对象是设备 `/www`，输出到 Windows 当前工作目录；原 BAT 没有固定输出目录、完成性/hash 检查或后台定位。

| 动作 | 风险 | 已证明范围 |
| --- | --- | --- |
| ADB SYNC pull `/www` | R0：远端读取 | BAT 明确请求此读操作；目标 UID、权限和成功与否未验证 |
| PC 输出目录/文件创建 | 本机写入 | 属于备份输出，不是设备 R1；原脚本可覆盖同名本机输出，未隔离目录 |
| 设备临时打包/文件创建 | R1 | BAT 无此命令，`REQUIRES_TEMP_WRITE=NO`（仅针对本脚本方案） |
| 改服务/权限 | R2 | BAT 无此命令 |
| 重启/关机 | R3 | BAT 无此命令 |
| remount、系统/NV/分区写入 | R4 | BAT 无此命令 |

ADB 主机端可能启动自己的 server、处理授权密钥或缓存；本 BAT 没有控制这些行为，随包客户端实现未完全验证。不能把“BAT 不含写命令”扩大为“所有主机和设备状态绝对无副作用”，也不能因为 ADB 支持写操作就认定本 BAT 实际调用了写操作。

## 连接方式与全部命令检查

| 入口/命令 | 本 BAT 结果 |
| --- | --- |
| ADB | YES，仅 `pull`；要求已有可用、授权且可读取的 transport |
| 指定 IP、`adb connect`、host/port | NO；没有 `192.168.100.1` 或显式端口 |
| 80 / 8081 / 7628 / 7689 / 22 / 23 / 5555 | 未显式指定或调用；不能据此选择新入口 |
| HTTP GET/POST、curl、wget、requests、urllib、WebSocket | NO |
| SSH/Telnet/SCP | NO |
| shell、AT、RPC | NO |
| tar、cp、cat、find、ps、netstat、ss、sha256sum | NO；不会定位监听进程，也不会生成远端 manifest |
| chmod、mount、dd、rm、mv、kill、reboot、systemctl、nvram、USB 切换 | NO |
| push、root、remount、install | NO |

`adb` 是未限定路径的命令名，实际解析依赖 CWD/PATH；未用 `-s/-d/-e` 指定单一设备，环境变量和已有 server 状态也可影响选择。不能保证执行时一定使用随包 exe 或目标 MF650。原 BAT 不知道 ADB 网络端口，**不能用它证明 7628 是 ADB，或 7689 是普通 root shell**。

静态能力线索（adb.exe 文件 offset，均未调用）：`Android Debug Bridge version %d.%d.%d` 在 `0x467eb0`、`ADB_SERVER_SOCKET` 在 `0x45da72`、`ANDROID_ADB_SERVER_ADDRESS` 在 `0x45da84`、`ANDROID_ADB_SERVER_PORT` 在 `0x45da9f`、`ANDROID_SERIAL` 在 `0x45db86`、`sync:` 在 `0x4617cb`、`adbkey` 在 `0x46bebc`。可见 USB/WinUSB、网络连接和一般 ADB 命令帮助，不能恢复实际运行时地址或认定其全部代码可信。`8081 @0x4ca8d8` 只是连续数字表的一部分，不是监听/连接配置。

## 能否完整导出当前后台：UNKNOWN

1. 当前只读 transport 未确认；前期 SYNC 会话曾关闭，本阶段不重试。
2. `/www` 可能只包含旧 Web UI。当前 8081 PID、程序路径、工作目录、路由实现、依赖与配置均 UNKNOWN；参考包已出现 `/usr/bin/webservers`、`/home/root/main` 和 `/data/kasb/www` 等 `/www` 外对象，说明只复制 `/www` 不能自动证明覆盖完整后端。
3. ADB pull 本身不要求执行 `adb root`；是否需要 root 取决于实际文件读取权限，当前为 UNKNOWN。不提升权限，不替换 adbd，不切 USB。
4. BAT 无临时 tar、设备清理或重启命令。若以后发现导出依赖临时 tar，标记 `REQUIRES_TEMP_WRITE` 并停止，另行取得用户明确确认，不能在本方案中自动创建或清理。
5. 未收集设备上的可恢复原件、属主/权限/软链接、当前服务与配置，不能作为已验证的部署回滚材料。

## 自身重实现与后续门槛

[tools/mf650_backend_backup_readonly.py](../tools/mf650_backend_backup_readonly.py) 从 BAT 提取 `/www` 的读取意图，默认且仅支持 **DRY_RUN**：输出协议、路径、覆盖未知项、前提与停止条件。没有 subprocess、socket、HTTP、ADB 调用或 `--execute` 入口；不运行原 exe/BAT。它是供审阅的计划器，**尚不是可导出后台的执行工具**。

原工具完整安全性仍 UNKNOWN，当前不满足 SAFE_READONLY。条件方案见 [BACKEND_BACKUP_EXECUTION_PLAN.md](BACKEND_BACKUP_EXECUTION_PLAN.md)。只有先确认可信主机客户端、既有安全读取入口、显式目标选择、当前后端路径与完整备份范围，才可提交独立执行阶段给用户确认。本轮没有请求或取得设备备份执行批准。
