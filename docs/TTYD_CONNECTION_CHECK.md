# ttyd root 入口复核记录

日期：2026-10-02。根据用户最新纠正，仅检查 `192.168.100.1:7689`，没有再尝试 SSH、Telnet、ADB 或 7628，也没有寻找另一个 root 端口。

本节描述前阶段 ttyd 复核。后续端口审计及标准 SYNC 审计已获用户单独授权；本轮没有重试 ttyd、Ctrl+C 或 ShellCrash 菜单。7628 的标准 sync: 也在 OPEN 阶段返回 CLSE，未取得 feiliu.sh，详见 ADB7628_SYNC_AUDIT.md。

用户旧审计已确认启动链路：`ttyd :7689 → sh -c sh /usr/bin/feiliu.sh → ShellCrash 菜单`。当前终端标题与此一致；没有证据表明启动方式发生变化。

## 浏览器观察

Chrome 能打开 `http://192.168.100.1:7689/`。
终端标题为 `sh -c sh /usr/bin/feiliu.sh (sdxprairie)`，显示 ShellCrash 1.9.2beta4 菜单。
尝试在 Terminal input 发送 Ctrl+C、id 和 Enter，但未观察到身份输出。浏览器 WebSocket 事件中只观察到尺寸调整帧，未确认自动按键产生了正确控制帧，因此没有将浏览器尝试当作有效 root 测试。

## 独立协议复核

使用已安装的 Python `wsproto` 和直接 TCP socket，不手写 WebSocket 协议，不经过 HTTP 代理。

1. 连接 `192.168.100.1:7689`，握手路径 `/ws`，请求子协议 `tty`。
2. 服务端返回 `AcceptConnection`，接受的子协议是 `tty`。
3. 第一条应用消息使用 Text Frame，数据严格为 `{"AuthToken":"","columns":120,"rows":40}`。
4. 等待并读取完整初始菜单后，才发送 Ctrl+C。
5. Ctrl+C 使用 Binary Frame，字节严格为 `30 03`，即 `b'0\x03'`。
6. 此后等待 1 秒；连接在等待期间关闭，没有可继续发送 `b'0id\r'` 的会话。
7. 前次两次独立测试及用户最新纠正后的本次最小复核，均得到相同结果；不再重复发送 Ctrl+C。

这次复核未发送文件读取命令或任何写命令，因为尚未确认 `uid=0(root)`。

## 完整初始输出（移除 ANSI 颜色）

```text
-----------------------------------------------
欢迎使用ShellCrash！        版本：1.9.2beta4
服务没有运行（Redir模式），未设置开机启动！
TG频道：https://t.me/ShellClash
-----------------------------------------------
 1 启动/重启服务
 2 内核功能设置
 3 停止内核服务
 4 内核启动设置
 5 配置自动任务
 6 导入配置文件
 7 内核进阶设置
 8 其他工具
 9 更新/卸载
*----------------------------------------------
请输入对应数字 >
```

另外收到两条 ttyd 控制消息：

```text
1sh -c sh /usr/bin/feiliu.sh (sdxprairie)
2{ }
```

## Ctrl+C 后的关闭证据

原始日志解析出关闭码 1006。该码不应作为线上 Close Frame 发送，因此 `wsproto` 把它报告为 `1002 remote CLOSE with local-only reason`；这不是本次初始化帧被拒绝的证据。原始帧仅保留在 PC，不提交公共 GitHub。
初始化已成功产生完整终端输出。关闭发生在 Ctrl+C 输入之后。

Ctrl+C 后没有新增终端文字输出，没有 `id` 输出，没有 root 提示符；会话已经关闭，因而未发送 id。`cat /proc/meminfo`、`cat /proc/swaps`、`free`、`pwd` 均未执行。

当前结果只能确认该 ttyd 会话在 Ctrl+C 后结束，不能据此断言设备不存在 root 能力，也不能断言启动方式与旧审计不同。

## 菜单与启动逻辑的只读分析

为避免执行配置功能，先读取上游 ShellCrash 的历史源码，再仅查看设备主菜单的 `8 其他工具`，停在该层提示符，没有选择任何工具。

新连接仍使用相同 `/ws`、`tty`、Text 初始化；完整初始菜单与上文一致。本次唯一菜单输入是 Binary Frame `b'08\r'`（前缀 `0`，键盘输入 `8` 和回车）。以下为此后全部终端输出，移除 ANSI 颜色，保留内容：

```text
8
-----------------------------------------------
欢迎使用其他工具菜单：
本页工具可能无法兼容全部Linux设备，请酌情使用！
磁盘占用/所在目录：
496.0K	/home/root/ShellCrash
-----------------------------------------------
 1 ShellCrash测试菜单
 2 ShellCrash新手引导
 3 日志及推送工具
-----------------------------------------------
 0 返回上级菜单
-----------------------------------------------
请输入对应数字 >
```

此后关闭客户端连接，没有发送 Ctrl+C、0 或其他输入，没有执行测试、新手引导或日志/推送操作。菜单显示的路径仅作为 ShellCrash 目录线索，不能用它证明当前 UID 为 root。

上游参考固定到提交 `6d71c3faf541cc3b821ee8194b1d6114e622823f`（2025-07-11），[bin/version](https://github.com/juewuy/ShellCrash/blob/6d71c3faf541cc3b821ee8194b1d6114e622823f/bin/version) 明确标记 `versionsh=1.9.2beta4`。没有在设备执行任何上游脚本。

- [menu.sh](https://github.com/juewuy/ShellCrash/blob/6d71c3faf541cc3b821ee8194b1d6114e622823f/scripts/menu.sh#L1886)：`tools()` 先读取状态、显示磁盘占用和菜单，再等待选择；这里没有通用 shell 入口。
- [main_menu()](https://github.com/juewuy/ShellCrash/blob/6d71c3faf541cc3b821ee8194b1d6114e622823f/scripts/menu.sh#L2070)：上游 0 是 `exit`，不是创建交互 shell。设备主菜单没有显示这个 0，不能把上游行为直接当作设备定制脚本的行为；未尝试隐藏选项。
- [webget.sh 的 testcommand()](https://github.com/juewuy/ShellCrash/blob/6d71c3faf541cc3b821ee8194b1d6114e622823f/scripts/webget.sh#L2468)：上游测试菜单包括内核 Debug、DNS 端口、加密跑分、路由规则、内核配置和代理连通性，没有通用 shell。设备上未进入该菜单，更未执行这些功能。
- [ttyd 1.7.7 protocol.c](https://github.com/tsl0922/ttyd/blob/1.7.7/src/protocol.c#L95)：子进程退出回调会关闭 WebSocket，非零退出使用 1006。观察到的 Ctrl+C 后关闭与该逻辑一致。这是结合上游的推断，不是设备进程退出码或 feiliu.sh 内容的实测。

**尚未读到设备 `/usr/bin/feiliu.sh` 的内容。** 当前仓库和提供的交接文件没有此脚本，公共 GitHub 代码搜索也未找到它。相同 ShellCrash 版本不能证明定制包装脚本完全相同。

**结论：目前没有已核实的安全系统 shell 入口。** 已排除本次 Ctrl+C 可用的说法，并确认“其他工具”这一层未提供 shell。需要设备脚本内容或旧审计中的完整脚本输出，才能核对是否有明确 shell 分支、调用后返回逻辑或自定义信号处理；不会以猜测代替实际方式。

完整带 ANSI 的收发日志已保存到 PC `backup/20261002-predeployment/ttyd-probe-transcript.txt` 与 `ttyd-menu-transcript.txt`。两份日志均保留 ttyd 控制消息、每个终端数据帧和实际发送的输入。
PC 还保存了 HTTP 拉取的 `mf650.html.original` 与 manifest，内容 SHA-256 为 `bc4a01ccbb817ee801c7a16c32e18385b9aa0dc34e75e8f53182c6361b2fbbc8`。设备真实路径、owner、group、mode 仍未读取，这个 HTTP 副本不能替代完整设备备份。

## 继续条件

需要取得设备 feiliu.sh 的实际内容后再核实入口。当前阶段仅允许获授权的只读访问分析，不继续尝试菜单逃逸或部署；即使未来恢复读取能力，写操作也需遵守后续阶段指令、原文件备份与 rollback 要求。
当前未修改设备文件、ttyd 启动方式或 ShellCrash 配置，未启动/停止任何设备服务，未清理缓存或重启。
