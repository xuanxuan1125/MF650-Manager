# 【MF650 在线 adbd 身份报告】

设备：192.168.100.1。日期：2026-10-03，Asia/Shanghai。采集时间：00:23:07–00:23:23（PC 时钟）。

**在线 adbd 文件 NOT OBTAINED，当前运行版本 UNKNOWN。** 已完成本阶段允许的连接与本机 USB 取证；没有新的合法文件入口，不能从 transport、USB PID 或页面版本确认 FOTA/Vendor/Patch。此结论不是认定设备没有 adbd，也不是确认“其他版本”。

## 身份结果

| 项目 | 结果 |
| --- | --- |
| 当前 ADB transport | TCP 192.168.100.1:7628，ADB 列表 state=device，transport_id=1 |
| 在线 adbd | **NOT FOUND / NOT OBTAINED**（未取得样本，不表示文件不存在） |
| 在线文件路径 / 实际执行路径 | UNKNOWN / UNKNOWN |
| SHA256 / 大小 / ELF 身份 | UNKNOWN / UNKNOWN / UNKNOWN |
| FOTA / Vendor / Patch 匹配 | UNKNOWN / UNKNOWN / UNKNOWN；没有样本，不能填 NO |
| 当前运行版本 | UNKNOWN；未满足任一 ONLINE_*_ADBD_CONFIRMED 条件 |
| USB VID:PID | 05C6:9057 |
| 当前可见 USB 模式 | Windows Net / Remote NDIS Compatible Device #11 / OK；未观察到 USB ADB transport |
| 7628 | TCP_OPEN；adb connect 成功 |
| shell | NOT TESTED 本轮 / FAILED HISTORY；当前 UID、服务权限 UNKNOWN |
| Vendor 维护版 / 完整维护环境确认 | UNKNOWN / UNKNOWN（未确认） |
| 是否需要 Patch | UNKNOWN |
| 当前部署 / 设备修改 | NO / NO |
| ADB Patch | PATCH_READY_OFFLINE，保留未安装 |

USB 枚举只证明当前可见网络设备，不证明 boot_hsusb_comp 内容或全部 gadget 功能。transport_id 是 PC ADB server 分配的标识，不是设备 PID、UID 或文件身份。

## 本轮完整连接输出

仅对用户列出的六个端口各进行一次 TCP connect/close，不发送应用数据，不扩展端口范围：

| 端口 | 时间（+08:00） | 结果 |
| ---: | --- | --- |
| 80 | 00:23:07.516842 | TCP_OPEN |
| 8081 | 00:23:07.537762 | TCP_OPEN |
| 7628 | 00:23:07.540964 | TCP_OPEN |
| 7689 | 00:23:07.545577 | TCP_OPEN |
| 7777 | 00:23:07.567824 | TCP_OPEN |
| 2358 | 00:23:07.583891 | TCP_OPEN |

TCP_OPEN 不确认监听程序/PID。没有向 ttyd、QMI 或 RPC 发送菜单、AT 或服务请求。

使用之前已使用的本机 `ADB & Fastboot++/adb.exe`，历史版本记录为 platform-tools 35.0.1-11580240；本轮客户端 SHA256 为 `e1657ca239bcf53f60dd622a8476d51b8df3c2a3169f7b6082142942560627ed`。它不是厂商 ZIP 中 SHA256 为 `bce1f6f6f6532266837b66d719bfbd177d406d6d7d8d5adfd5e7c59fcde651a5` 的客户端。没有运行厂商 BAT/EXE，也未重新验证本机客户端上游发布签名。

```text
> adb connect 192.168.100.1:7628
stdout: connected to 192.168.100.1:7628
stderr: <empty>
exit: 0

> adb devices -l
List of devices attached
192.168.100.1:7628     device transport_id:1

stderr: <empty>
exit: 0
```

本轮只记录 stock 客户端连接结果，未抓取或重新手写 CNXN 握手。历史直接 CNXN 成功证据见 [ADB7628_SYNC_AUDIT.md](ADB7628_SYNC_AUDIT.md)。本机 PnP 枚举时间 00:23:23，唯一匹配 USB VID_05C6 项为上述 Net 设备。原始连接 JSON 和含 InstanceId 的 USB 数据保存在忽略目录 `test-results/online-adbd-identity/`，不提交标识原文。

## 已有文件入口与停止依据

已先阅读用户指定的九份报告：ADB_VENDOR_PATCH_REVERSE、ADB_PATCH_STRATEGY_COMPARISON、ADB_PATCH_BUILD_REPORT、ADB_ENABLE_PATHS、ADB_ENABLE_TEST_REPORT、FOTA_ANALYSIS、BOOT_USB_COMP_WRITERS、ONLINE_SYSTEM_FACTS 和 HANDOFF。复用其结论，不重新逆向 FOTA、三方 diff、前端或更新通道。

| 入口 | 已有证据 | 本轮决定 |
| --- | --- | --- |
| 厂商备份工具 | [静态审计](VENDOR_BACKUP_TOOL_AUDIT.md)：BAT 第18行仅 `adb pull /www .\`，没有 connect、授权、root 或 shell | 未运行；它不是新增文件通道 |
| 8081 已知 API | [已有清单](8081_API_ENDPOINTS.md)：36 路径/44 方法组合中未发现文件读取、adbd hash 或通用 shell API；device/info 只有业务状态 | 无相关接口可请求；HTTP 请求0，不猜路径，不重做页面提取 |
| ADB SYNC | 历史 OPEN sync: 收到 CLSE，未进入 STAT/LIST/RECV；本轮 device 状态没有证明服务条件变化 | 未重试 SYNC/pull/shell；候选 `/sbin/adbd`、`/system/bin/adbd` 均未读取 |
| ttyd/其他端口 | 已有 ShellCrash 菜单而非已验证 shell；无新明确入口 | 仅 TCP 状态，不操作菜单、不猜服务 |

备份 BAT 使用普通 ADB SYNC 读取语义，不含厂商专有前置命令。它依赖可用 transport、服务许可及 `/www` 读取权限；**不能证明只适用于 Vendor adbd**。参考 FOTA 在许可条件满足时也具有 SYNC；Vendor 无条件 setter 只是提供许可的一种机制。BAT 存在或网页存在，均不证明厂商维护包已安装。

建立 `analysis/online-adbd/{original,current,reports}/`；前两者为空且忽略，未制造在线样本。比较表 [ONLINE_ADBD_COMPARE.csv](../analysis/online-adbd/reports/ONLINE_ADBD_COMPARE.csv) 的 NOT_OBTAINED 行不是实测文件，空 hash/size 和 UNKNOWN 表示未比较。无样本，不执行 file/readelf/strings/byte diff。

参考 SHA256（来自上一阶段，非本轮在线采样）：

| 参考 | SHA256 |
| --- | --- |
| FOTA | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` |
| Vendor | `e545cde0feedeb1deb2aa97ffe062b54caf522d4996e0db404b4fb0e7552f1c8` |
| Our patch | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` |

未来即使取得路径文件并匹配，也先只能确认该路径采样时的文件身份；实际执行文件、覆盖挂载及进程关联仍需证据。FOTA 匹配本身也不能证明厂商包从未安装过或其他组件不存在。

## 系统属性与启动信息

没有合法系统文件/命令读取入口，因此下列值均 UNKNOWN，不用旧 FOTA 默认值补齐：

| 对象 | 在线结果 |
| --- | --- |
| ro.debuggable | UNKNOWN |
| ro.secure | UNKNOWN |
| ro.adb.secure | UNKNOWN |
| ro.build.version | UNKNOWN |
| ro.product.model | UNKNOWN |
| ro.product.device | UNKNOWN |
| /proc/cmdline；androidboot/adb/usb/debug 参数 | NOT OBTAINED / UNKNOWN |
| boot_hsusb_comp、systemrw override | UNKNOWN |
| 进程 PID、UID、路径、匹配库、SELinux 状态/日志 | UNKNOWN |

## 历史 CNXN 成功而 shell closed 的候选解释

连接/transport 层成功与具体服务 OPEN 成功是不同阶段。此前 shell id 返回 error: closed；SYNC 在 OPEN 阶段关闭。这不能唯一定位某一条失败分支。本轮没有再次测试 shell，因此也不能断言当前仍会失败。

| 候选 | 证据 | 强度 | 尚未知 / 不能下的结论 |
| --- | --- | --- | --- |
| A：adbd 版本不同 | 三份参考 hash 和 gate 行为不同，在线无 hash | 参考差异强；在线归属无证据 | 不能从7628/9057选 FOTA 或 Vendor |
| B：permission flag=0 | 参考 FOTA 的 flag 控制服务创建；历史拒绝模式相容 | 静态强；在线间接 | flag、boot token、合法授权状态未知；不得写“已证实flag=0” |
| C：shell service gate | 参考代码包含 shell/线程许可检查；Vendor 在正常分派前置1 | 静态强；在线间接 | 在线是否执行此代码、失败位置及 shell 环境未知；若确为Vendor且正常到达setter，不能仍用未匹配token直接解释 |
| D：SELinux / 系统权限 | 参考含 SELinux 依赖，运行时错误可能阻断服务 | 在线弱、未验证 | 无 enforcing 状态、拒绝日志、errno；不能判为SELinux拒绝 |
| E：transport 类型/实现 | 本轮TCP device；历史CNXN成功、服务CLSE；未见USB ADB | TCP存在强；因果弱 | TCP可用不保证服务可用；没有对照证明TCP类型造成拒绝 |
| F：授权状态 | 参考区分标准RSA transport auth与厂商序列号服务许可 | 静态强；在线许可未知 | device不证明厂商服务授权或root；Vendor正常分派绕过序列号路径不代表RSA代码已删除 |

结论仍是 **原因未唯一定位**。当前没有依据确认 Vendor 维护环境，也没有依据决定需要安装哪种 Patch。旧报告中的“likely vendor-custom”是定制 adbd 类别推断，**不等于本次具体 Vendor 样本已匹配**。

## 动作边界与后续所需证据

本轮仅六个已知 TCP 状态检查、一次 adb connect、一次 adb devices -l、本机只读 PnP 枚举。HTTP、shell、SYNC、pull、root、tcpip、usb、push、install、remount、授权尝试、AT/RPC、菜单、服务修改、NV/USB/boot修改、reboot、9008/EDL、安装包执行均0。设备修改 **NO** 指没有发起设备配置/文件写入或控制动作，不声称连接过程中设备内部不会产生常规日志。

需要一个已验证、获授权的文件只读导出入口，才能取得当前 adbd 的来源、路径、原件及 SHA256；再将文件与实际进程关联。当前没有该入口，按本阶段停止条件结束，不重复失败测试或安装补丁。
