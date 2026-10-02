# MF650 ADB 服务许可与厂商授权分支

日期：2026-10-02。参考版本为 MF650_2.3_Fota.zip；基线提交 `8f536c878f1061efb5cd84efad805e0b0ebb2788`。本轮只做静态分析及已知页面 GET，没有发送授权请求、生成授权码、提取授权常量、patch 或执行固件程序。**运行时授权分支已经恢复；厂商配套客户端、合法凭据发放和撤销流程尚未找到。**

## 样本、工具与证据范围

`system/sbin/adbd`：30,380 字节，SHA-256 `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185`。ARM32 little-endian ELF，主要代码为 Thumb。地址均为这个 ELF 的链接虚拟地址，不能直接当作在线进程地址；在线文件 hash 未取得。

使用 GNU ARM objdump/readelf、Capstone 5.0.9、pyelftools 0.33；本机 Ghidra/radare2 不可用。完整 `.text` 反汇编、PC-relative literal、GOT/重定位、符号和相邻 BSS 引用均已检查。线性解码中的 literal pool 不按可执行代码计数；关键分支与两条 STR 用 GNU objdump 复核。httpd 的 Thumb→ARM veneer 另行解析，不能把 veneer 内 ARM 字误解为 Thumb 调用。

本地完整反汇编在被忽略的 `fota-analysis/binaries/`，未上传其中的授权常量或固件二进制。公开索引：[写入点 CSV](../fota-analysis/reports/ADB_GATE_WRITE_SITES.csv)、[样本与覆盖清单](../fota-analysis/reports/ADB_AUTH_AUDIT_MANIFEST.json)。

## Flag 及所有已确认写入点

服务许可 flag 是 `.bss` 内 **VA `0x8044` 的 32 位值**，没有独立导出符号；不是标准 Android property。`_Z13service_to_fdPKc` 的 Thumb symbol 为 `0x3561`，实际指令起点 `0x3560`。正常代码中确认 **2 条写入指令、3 条逻辑启用路径**。未找到第三条正常写入或清零指令。ELF 装载时 BSS 初始化为零属于生命周期初始化，不算第三个代码写入点。

| 写入点 | 调用者与条件 | 输入、文件/字符串依赖 | 网络 / socket / ioctl / PID / 凭据 | 副作用 |
| --- | --- | --- | --- | --- |
| 1：`0x3588 STR r0,[r1]` | service_to_fd → `0x34d0` 返回 1 | fopen64/fscanf 读取 `/etc/usb/boot_hsusb_comp`；strcasecmp 等于 `90DB` 或 `9059` | 每次 ADB 服务 OPEN 前检查；沿既有 TCP/USB transport；写 flag 不依赖新的 socket/ioctl、实际枚举 PID 或凭据 | flag=1；这一步不改文件、USB 或 NV |
| 2：`0x38f8 STR r0,[r4]` | shell 分支中序列号派生结果比较成功；两个来源共用此 STR | 首选 `nvram get serialno 2>&1`；回退 `/proc/cmdline`；辅助函数 `0x2a84`；临时文件、md5sum、awk | 输入来自 ADB shell 服务参数；未见 Web/2358 专用请求；无该分支直接 ioctl、USB PID 比较；需要合法厂商凭据 | flag=1；临时文件写入/删除；原参数继续交给 shell handler |

完整引用核对：`0x2c8c/0x2c8e` 读 flag（服务线程创建），`0x2e9c/0x2e9e` 读 flag（shell handler），`0x3586/0x3588` 写，`0x3648/0x364a` 读（shell 授权选择），`0x38f6/0x38f8` 写，`0x390e/0x3910` 读（回退选择）。相邻 `0x8038` 为初始化 bookkeeping，`0x803c+4=0x8040` 和 `0x8048` 的写入不触及该 4 字节 flag；未把相邻变量误计为许可写入。

`0x34d0` 用 fscanf 的 `%s` 取文本 token，再用 **strcasecmp** 做完整字符串比较；不是按运行时 USB PID 或四字节前缀判断。service_to_fd 每次调用都先尝试 boot 匹配，未匹配只跳过写入，**不会清掉已经成功设置的 flag**。

## 厂商分支的完整正常控制流

1. service_to_fd 识别 shell 服务，`0x3646` 取得前缀后的参数；flag 非零直接到 `0x3654`。flag 为零跳到 `0x3864`。
2. 清空序列号/输出缓冲区，`0x38ae` popen 读取 `nvram get serialno 2>&1`，`0x38bc` 用 `%32s` 读取，随后 pclose。`0x38c8` 求长度，`0x38d2` 调用 `0x2a84`，参数是序列号指针、长度、输出缓冲区。
3. 检查服务字符串长度大于前缀长度 6；`0x38ec` 用 strncmp 比较服务参数与辅助输出，比较长度取辅助输出 strlen。正常摘要为 32 个十六进制字符。相等到 `0x38f2`，设置 r0=1，最终在 `0x38f8` 写 flag。
4. 首选比较未通过，且 flag 仍为零时，进入 `0x391a` 回退：读取 `/proc/cmdline`，fgets 上限 512；`0x3950` strstr 查找 `androidboot.serialno=`。随后 `+0x15`，正好是该标记的 **21 字节长度**；`0x395e` 用 `%8s` 读取。
5. `0x3968` 再次调用同一辅助函数，输入长度固定为 8；`0x3984` 作同样的 strncmp，成功仍跳到同一 `0x38f8`。这是第二个序列号来源，不是第三条 STR。
6. 未成功设置 flag 的路径最终仍进入 shell handler；其内部 `0x2e9e` 检查 flag，为零则失败。授权成功后原来的参数也交给 shell handler，没有看到独立的“enable 后剥离凭据”接口。不能假定第一次授权会返回普通交互 shell。

该分支不使用 IMEI/MAC 比较、RSA AUTH packet、SHA/Base64/CRC、签名验证、在线 challenge 或许可服务器。所检查的正常数据流中未见有效期、按连接绑定或撤销接口。这里是 **daemon 进程全局的服务许可**，不等同 CNXN 成功、RSA transport 认证或在线 root 身份。

## 辅助函数 `0x2a84`

辅助函数把固定 32 字节状态与输入序列号做有限长度混合，再通过外部 md5sum 取得文本摘要。只记录流程，不公布固定状态、逐字节算法、可运行授权生成器或请求样例。

| 地址 | 行为 |
| --- | --- |
| `0x2aca..0x2afe` | 对最多 32 字节输入进行状态混合 |
| `0x2b08` | 打开 `/tmp/test-123456-test.txt` 用于写入 |
| `0x2b18` / `0x2b1e` | fwrite 32 字节状态并 fclose |
| `0x2b2a` | popen `md5sum /tmp/test-123456-test.txt \| awk '{print $1}'` |
| `0x2b38` / `0x2b3e` | 用 `%32[^\n]` 接收摘要并 pclose |
| `0x2b46` | system 清理同一临时路径 |

它不写 boot composition、systemrw、MTD 或 NV；`nvram get` 为读取，不能与 `nvram set` 混淆。但创建/覆盖/删除固定临时文件及启动外部命令是真实副作用，因此授权操作不是 R0。调用者没有严格处理所有读取和辅助函数失败结果；本轮没有探索异常输入或利用失败路径。

## 生命周期、正式入口与尚缺证据

boot 匹配及合法序列号授权都能在这个参考二进制中设置许可。序列号授权是**非持久启用线索：YES（静态）**；它不要求切换成带 USB ADB 的 PID，可沿已有 TCP transport 到达。在线版本是否实现同一逻辑、厂商工具怎样提供合法凭据、何时发出授权服务请求，均未实测。

FOTA Web/ASP、2358 全分派表和当前 80/8081 页面中未找到配套 ADB 授权界面或调用代码，见 [Web 审计](WEB_ADB_INTERFACE_AUDIT.md)、[2358 审计](TCP2358_RPC_REVERSE.md)。普通 Web 控制台能执行管理员命令，但不是这个 flag 的专用授权接口，本轮没有使用它代替授权。

daemon 退出会丢失 BSS flag；**断开一个客户端不会清 flag**。重启后的 service_to_fd 若再读到 9059/90DB 会重新开启，因此“重启 adbd 就撤销”只在实际 boot 文件不匹配且没有其他自动授权来源时成立。不能因当前 PID=9057 就假定该回滚条件已满足。下一步只能按 [启用路径与条件方案](ADB_ENABLE_PATHS.md) 先核实厂商工具、在线版本及撤销方式。
