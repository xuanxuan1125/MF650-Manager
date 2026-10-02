# MF650 厂商 adbd 三方逆向

2026-10-03（本轮分析始于2026-10-02）。基线提交 `71ae8bd01bd71e29e0b118043be42e1fbf121b99`。**厂商版修改了 service_to_fd 的许可写入条件：一条 BNE 被替换成 MOVS，正常服务分派会无条件置 flag=1。它不是 90DB→9057 的 literal patch。** 没有运行任何 adbd、附件或安装脚本，没有设备请求，没有生成新安装 ZIP。

## 样本身份与工作区

仅从已存在的 FOTA ZIP 读取 `system/sbin/adbd` 单一成员；没有重新扫描 FOTA。厂商来源为已解压 `ALKMF650.zip!sbin/adbd`；自行补丁来源为既有 `patch-output/patched-adbd`。各原件不变，本机工作副本分别在 `analysis/adbd-threeway/original/`、`vendor/`、`our-patch/`；完整字符串、GNU 输出和反汇编位于忽略的 `diff/`、`disasm/`。只公开少量必要片段和文本元数据。

| 样本 | 字节数 | SHA256 | MD5 | GNU build-id |
| --- | ---: | --- | --- | --- |
| A：FOTA 2.3 原版 | 30,380 | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` | `48eb92938b75958bc725bbd8c0fc1018` | `e01e6d6a66474ce1f546f2e5f7f345ebc0199f6e` |
| B：高级后台包厂商版 | 30,380 | `e545cde0feedeb1deb2aa97ffe062b54caf522d4996e0db404b4fb0e7552f1c8` | `6008dbb3a301ec76c9e60dae0c002b33` | `3c76f15602425e3878923352638238e182173acf` |
| C：既有自行 literal patch | 30,380 | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` | `b05555bf4a5ade59bb2d87e0e9fcaff0` | `e01e6d6a66474ce1f546f2e5f7f345ebc0199f6e` |

三份均为 stripped ARM32 little-endian、EABI5、ET_DYN，入口 `0x2261`，解释器 `/lib/ld-linux-armhf.so.3`。ELF header、program headers、section headers、DT_NEEDED、动态符号表、PLT、GOT、重定位表全部相同；后几项相关 section 的内容也相同。12 项依赖为 libadbd.so.0、libselinux.so.1、libbase.so.0、libfs_mgr.so.0、libpthread.so.0、libglib-2.0.so.0、libcutils.so.0、liblog.so.0、libstdc++.so.6、libgcc_s.so.1、libc.so.6 和 ARM loader。

精确表：[ADBD_VARIANTS.csv](../analysis/adbd-threeway/reports/ADBD_VARIANTS.csv)；布局：[ELF_LAYOUT_COMPARISON.json](../analysis/adbd-threeway/reports/ELF_LAYOUT_COMPARISON.json)。build-id 的差别不能代替全文件 hash；C 保留 A 的旧 build-id 并不表示文件相同。

## 完整差异：FACT

| 比较方向 | 不同 bytes | 连续区块 |
| --- | ---: | ---: |
| A → B | **37** | **5** |
| A → C | 2 | 1 |
| B → C | 39 | 6 |

连续区块按不同字节相邻分组，`offset_end` 为包含的最后一个字节。A→B 全部 5 区块如下，没有其他差异：

| 文件 offset 起止 | 长度 | ELF section | 改动 |
| --- | ---: | --- | --- |
| `0x1c0–0x1d3` | 20 | .note.gnu.build-id | 替换 GNU build-id |
| `0x3580–0x3581` | 2 | .text | `03 d1 → 01 20`，BNE→MOVS |
| `0x5754–0x5757` | 4 | .rodata | revision 展示字符串的部分字节 |
| `0x5759–0x575f` | 7 | .rodata | 同一 revision 字符串的另一连续差异块 |
| `0x7094–0x7097` | 4 | .gnu_debuglink | debug 文件 CRC32：小端值 `0x42f5b976 → 0xc5dc1479` |

完整 before/after hex、section 和 VA 见 [ADBD_BINARY_DIFF.csv](../analysis/adbd-threeway/reports/ADBD_BINARY_DIFF.csv)。**功能代码差异只有 2 bytes，其他 35 bytes 是构建/展示/调试元数据。** `.data/.dynamic/.got/.plt` 无变化。revision 从 `5bcd4abf1128-android` 改为 `42e64f3c96ba-android`，在 `0x216a` 取地址，`0x2172` 作为栈参数传入 `0x2176` 的 `__printf_chk`，对应 daemon version 的 `%s` 展示，不是新增授权 token。

修改形态分类为 **MIXED：branch code patch + build metadata**。高度相同的布局、符号与 30,343 个相同字节支持“局部修改既有产物”的形态判断。build-id、revision 和 debuglink CRC 同时变化也可能来自保留布局的重编译/链接或事后修改；没有源码、构建日志或匹配 debug 文件，**具体制作工艺 UNKNOWN，不宣称已证明重新编译或已证明未重新编译**。

字符串三方 diff 见 [ADBD_STRING_DIFF.json](../analysis/adbd-threeway/reports/ADBD_STRING_DIFF.json)。除 revision、build-id 中偶然形成的可打印短串，A→B 无新服务/属性/序列号字符串。B 保留 `90DB @0x5e18`、`9059 @0x5e20`，没有 9057 比较字符串。7628 是指令立即数，不能用 strings 未命中来否定端口。C 唯一字符串变化是 90DB→9057；原授权常量、凭据生成算法和实际序列号均未公开或生成。

## service_to_fd 与 gate：FACT

三份动态符号均为 `_Z13service_to_fdPKc`，Thumb symbol `0x3561`，实际指令起点 `0x3560`，size 1460。所有 VA 是链接地址，不是在线进程地址。此处第一 PT_LOAD 的 VA/file offset 相同，经 program headers 核对。

```text
VA        A / C                          B
357a      BL   34d0                      BL   34d0
357e      CMP  r0,#1                     CMP  r0,#1
3580      03 d1: BNE 358a                01 20: MOVS r0,#1
3582      LDR.W r1,[PC,#4d4]             相同
3586      ADD r1,PC                      相同
3588      STR r0,[r1]                    相同
358a      继续服务名分派                 相同
```

原指令是 Thumb 16-bit `0xd103`：条件 NE，即 Z=0 时分支到 `0x358a`，跳过许可写入；目标为 `0x3580+4+3*2`。新指令是 `0x2001`：无条件执行 `MOVS r0,#1`，更新 N/Z 后**顺序落入**地址计算和 STR；它不是 NOP、条件反转或把分支目标换到别处。

`0x3582` 的 literal 在 `0x3a58`，值 `0x4aba`；加 `0x3586+4` 得到 BSS flag **VA `0x8044`**。`0x3588` 因而写入 1。前面的 `0x34d0` boot helper 仍读取 `/etc/usb/boot_hsusb_comp`、用 strcasecmp 比较完整 90DB/9059 token；其结果现在被 MOVS 覆盖。文件未匹配或读取失败而正常返回 0 时也会置位。异常退出/进程未启动不在“正常到达该指令”的范围内。

| 初始 flag=0；boot helper 正常返回 | A 自动许可 | B 自动许可 | C 自动许可 |
| --- | --- | --- | --- |
| 90DB | YES | YES | NO |
| 9057 | NO | **YES** | YES |
| 9059 | YES | YES | YES |
| FFFF、90570、空 token/缺失文件 | NO | **YES** | NO |

A/C 是不区分 ASCII 大小写的完整 token 比较；B 的 setter 不再依赖 token。A 的完整许可来源仍是 token 为 90DB/9059 **或之前合法厂商授权已置位**；C 替换为 9057/9059 加原授权路径；B 在正常 service_to_fd 前置阶段就令 flag=1。

**B 直接比较 9057：NO；有效允许 9057：YES（静态）。** B 保留并有效允许 9059/90DB，但“保留”不代表仍靠它们控制服务许可。B 不是只扩大一个 USB token 的白名单。flag 装载初值仍为 0，不是把 BSS 默认值改成 1；在每次正常服务 OPEN 分派时强制写 1。CNXN/transport 建立本身不等于已经执行该 setter。

## shell/SYNC 与序列号授权：FACT

create_service_thread `0x2c50` 的检查完全保留：`0x2c8c/0x2c8e` 读同一 flag，`0x2c92 BEQ 0x2d46` 在零值时失败；shell handler 内 `0x2e9c/0x2e9e` 读 flag，`0x2ea2 BEQ.W 0x3126` 也保留。**厂商改的是提供 flag 的前置路径，没有删掉两个消费者检查。**

SYNC 名称在 `0x3730`，`0x3732` strncmp，匹配从 `0x3738` 转 `0x382c`。`0x382c–0x3832` 从 GOT VA `0x7df8`（文件 offset `0x6df8`）取得 Thumb callback `0x3d85`，对应本 binary 内 `0x3d84` 的 SYNC callback；`0x3836` 调 create_service_thread。与 A/C 相同，B 的前置 setter 使许可检查通过。因此 B **从 adbd 代码层解除 shell/SYNC 的此项 gate**；不是设备已实测成功，也不保证库、transport、路径权限或运行时启动条件满足。

原 NVRAM serialno → 辅助混合/外部 MD5 → shell 参数比较路径和 `/proc/cmdline` 回退仍在。辅助窗口 `0x2a84–0x2b87`、授权代码窗口 `0x3864–0x398f` 和相关字符串三份逐字节相同；未实现算法、生成 token、暴力序列号或重放凭据。

shell 授权选择在 `0x364a` 读 flag，`0x3650 BEQ.W 0x3864` 仅在 flag=0 时进入授权。B 的前置写入令该正常路径不进入授权，直接走 `0x3654 → 0x3658` 的 shell handler。授权成功写入点 `0x38f8` 保留，但正常 shell 服务不需要到达它。

分类必须分开：**授权代码 UNCHANGED；授权正常可达性 BYPASSED；没有 REMOVED。** 按最终摘要的“整体授权行为”选项记为 MODIFIED（BYPASSED）。A/C 未匹配 boot 条件时仍保留授权路径；已经设置的 flag 不会因不匹配 token或客户端断开而清零。B 未新增正常清零指令，进程退出才丢失该 BSS 状态。

## TCP、tcpip、USB、root 与标准 auth：FACT / 条件限制

| 对象 | 厂商版指令证据 | 变化与限制 |
| --- | --- | --- |
| 默认 TCP | `0x258c: 41 f6 cc 50 MOVW r0,#0x1dcc`，即 **7628**；`0x2590: ff f7 68 eb BLX 0x1c64 local_init` | 三份相同；没有改成 5555。main 仍优先读 service/persist 端口属性，有效正数分支 `0x2632` 可调用指定端口；这里只确认代码默认值，不确认在线 listener |
| tcpip handler `0x277c` | 正数校验，格式化端口，`0x27ae BLX property_set@0x1d30`，随后回复/close | 代码/属性字符串不变；未新增直接 listener 重建/daemon 重启。厂商包不含匹配 libcutils，不能断言当前实际 property_set 行为 |
| usb handler `0x274c` | `0x2756` 取 service.adb.tcp.port，`0x2758` 调 property_set，值仍为 `"0"`；回复后关闭 fd | 没有新增 usb_composition、AT、system 或脚本调用；不是 adbd 内新增 USB 切换 |
| main 标准 auth | `0x24b8` 取 ro.adb.secure，r1 默认 0；`0x24ba` 调 property_get_bool；false 时 `0x24c4` 写 auth_required；`0x24c6` 调 adbd_auth_init | 三份相同；没有新增“关闭 RSA auth”改动。FOTA 配套空属性库返回默认值，厂商/在线实际匹配库 UNKNOWN；service flag 与 transport RSA auth 不同 |
| root handler `0x298c` | `0x299e` getuid；UID=0 时 already root；否则 `0x29d0` 读 ro.debuggable，非 `"1"` 仍 production deny；满足时 `0x29e4` property_set | 三份相同；没有把 UID 改成 0 的新指令。root 服务入口可达性扩大，不等于新增强制 root 或确认在线 UID |
| unroot handler `0x2a34` | `0x2a38` getuid，`0x2a5a` property_set(service.adb.root, `"0"`) | 三份相同；不推断空属性库能真正降权/重启 |

动态符号表完全相同，没有新增 setuid/setgid/RSA/USB/NV/AT 导入；原已有 setgroups/getuid、popen/system/文件写入等代码没有删除。已知参考 libcutils 的 property_set 是空实现、property_get 只取默认值，仍只作为**配套 FOTA 库条件**，不是厂商包或在线库证据。本轮没有读取/替换在线库。

没有新 system/popen/shell-script/NVRAM/AT/USB 配置调用点；唯一变化指令不含调用。但前置 gate 放宽会令已有 shell、SYNC、root、remount 等服务更易到达，**新增危险代码 NO，已有危险能力可达性扩大 YES**。厂商序列号授权路径本来包含临时文件/外部 MD5 命令；代码仍在，不能称原授权算法已经删除。

## 安装脚本 MD5：FACT

内层 install.sh SHA256 `d18204bdd7d4da3233b6630fef3e91753191c3bd321adae04757ed9412f015b7`。第 47 行计算已有 `/sbin/adbd` 的 MD5，第 48 行比较 `6008dbb3a301ec76c9e60dae0c002b33`，**第 49 行在相等时跳过升级，第 52 行在不等时复制厂商版**。

这个常量等于 **B 自身 MD5**，不等于 A 的 `48eb92938b75958bc725bbd8c0fc1018`，也不等于 C。它是“已经安装 B 则不再替换”的标识，**不是旧原版白名单**。故 `VENDOR_PATCH_TARGET_MATCH=NO`（不存在 FOTA MD5 精确目标门槛）；A 会满足不等分支而被复制覆盖，但不能据此宣称安装器只针对该 FOTA。

第 51 行横幅“移除无线adb密钥验证”与正常服务跳过序列号授权相符，这是设计目的推断，不能据横幅声称 RSA transport 认证代码被删除。第 95–105 行还可能执行 902D/9059 的 USB AT 切换和持久 marker：**adbd 自身 USB 代码未变，安装流程另有 USB 副作用。** 全部只静态阅读，未执行。

## 推断、最小改动与备份工具

**INFERENCE：**厂商意图很可能是解除无线 ADB 服务许可，配合安装器 USB 开启，使普通 shell/SYNC 不必先走序列号授权。它可以解释 `adb pull /www` 工具为何没有授权步骤；已有可用 transport 和 root/读权限可能来自此前安装状态。但原 BAT 的缺少 connect/root 不能证明这种前提，原 A 在允许 token/已授权 flag 条件下也可提供读取。当前实际是否运行 B、连接方式及完整后台导出覆盖仍 UNKNOWN，备份工具整体安全结论不升级。

[VENDOR_ADBD_PATCH_MAP.json](../analysis/adbd-threeway/reports/VENDOR_ADBD_PATCH_MAP.json) 将 `0x3580` 归为厂商机制的 **REQUIRED_FOR_ADB** 功能改动，并同时标注 **RISKY_UNCONDITIONAL_GATE**；其余 build-id/revision/debuglink 改动归为 UNRELATED。这里的 REQUIRED 指复现厂商无条件 setter 所需，不表示适合本项目或已获准安装。

最小等效厂商逻辑只涉及这条 2-byte 指令，不需要复制全部 B 或修改三种元数据；本阶段**不生成其载荷或安装 ZIP**。对于当前限定 9057 的目标，推荐保留已存在的 **90DB→9057、保留9059** 设计，接受它移除90DB自动许可的已知取舍，不采用更宽的厂商 setter。比较分类为 **C：厂商更宽松**；`VENDOR_CONFIRMS_9057_GATE_PATCH=NO`；对现有两字节思路为 **PARTIAL**（gate 位置/作用验证，不是 literal 或许可范围等价）。详见 [ADB_PATCH_STRATEGY_COMPARISON.md](ADB_PATCH_STRATEGY_COMPARISON.md)。

## UNKNOWN 与部署门槛

当前在线 `/sbin/adbd` hash、实际 boot token、匹配运行库、UID、服务状态、恢复入口和安装方式未确认；USB 枚举 PID=9057 仍不等于 boot_hsusb_comp 内容。不能从本轮结果推断在线已能 shell/SYNC/备份，也不能把参考 B 当作当前 8081 后端来源。

**当前能否部署：NO。** 保留既有 PATCH_READY_OFFLINE 产物未安装；无新 Enable ZIP，无设备替换、root/remount、USB/NV、重启或9008操作。部署仍须满足 [ADB_PATCH_ONLINE_PREFLIGHT.md](ADB_PATCH_ONLINE_PREFLIGHT.md) 的在线身份、boot、兼容与可恢复门槛及后续明确授权。

## 交叉验证与复现

[ADBD_OFFLINE_VERIFICATION.json](../analysis/adbd-threeway/reports/ADBD_OFFLINE_VERIFICATION.json)：Python 逐字节 diff 与 GNU cmp -l 三组完全一致；Python ELF 与 GNU readelf -hW/-lW/-dW/-sW/-SW/-nW 一致；ASCII diff 与 GNU strings -a 一致；Capstone 与 GNU objdump 都确认唯一 .text 差异及 TCP/root/auth/USB 调用。只用 strings 得不到本报告核心结论。

自身工具顺序为 [analyze_mf650_adbd_threeway.py](../tools/analyze_mf650_adbd_threeway.py) → [inspect_mf650_adbd_gnu.py](../tools/inspect_mf650_adbd_gnu.py) → [verify_mf650_adbd_threeway.py](../tools/verify_mf650_adbd_threeway.py)。依赖此前本机 pyelftools/Capstone、WSL 和既有 ARM binutils；不自动下载或运行目标。GNU工具命令中 adbd 始终是文件参数，没有作为可执行命令使用。完整反汇编保留本机，公开 [KEY_THUMB_INSTRUCTIONS.csv](../analysis/adbd-threeway/reports/KEY_THUMB_INSTRUCTIONS.csv) 与 [UNCHANGED_CODE_REGIONS.csv](../analysis/adbd-threeway/reports/UNCHANGED_CODE_REGIONS.csv) 的少量证据/hash。
