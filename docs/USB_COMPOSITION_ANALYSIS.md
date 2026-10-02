# USB Composition 与持久覆盖分析

日期：2026-10-02。当前在线 USB = **VID 05C6 / PID 9057**。没有切换、重枚举测试或写启动配置。

## 完整矩阵

[USB_COMPOSITION_MATRIX.csv](../fota-analysis/reports/USB_COMPOSITION_MATRIX.csv) 覆盖 compositions 目录全部 **66 个四位十六进制 PID 脚本**，每行包含 description、sdxprairie 适用性、configfs 引用、目标函数、ADB/RNDIS/ECM/DIAG 标志、pkill 和脚本 hash。

统计：52 个有显式 sdxprairie 或适用通用分支，13 个在该 target 落入不支持提示，1 个保留 UNKNOWN（90CA 进入 legacy run_8009，在线节点适用性未证明）。901D 是通用分支，legacy/configfs 取决于 enable 节点，两种分支均 DIAG+ADB。

标志针对目标可选功能；NO 支持行的功能标志是 N/A，UNKNOWN 行不借用其他平台的功能。configfs references 列保留静态引用供人工检查。函数清单是多个 USB configuration 和条件分支的并集，不表示全部接口在同一 configuration 同时可用。52 个定义适用不等于 52 个都经过设备实测。

另有四个辅助文件，未冒充 PID 行：empty（无 composition 占位）、hsic_next（0）、hsusb_next（9025）、__emptyfile__。它们同样在完整文件清单中。

## 三个重点 PID

| PID | sdxprairie configfs 定义 | ADB | RNDIS | ECM | DIAG |
| --- | --- | --- | --- | --- | --- |
| 9057 | c.1 `gsi.rndis`；c.2 `gsi.ecm` | NO | YES | YES | NO |
| 9059 | c.1 `gsi.rndis` + `ffs.diag` 或 `diag.diag` + `ffs.adb`；c.2 `gsi.ecm` | YES | YES | YES | YES |
| 90DB | DIAG + DUN + RMNET + DPL + QDSS + ADB | YES | NO | NO | YES |

9057 的 run_configfs 先 pkill adbd，清理旧链接、写 idVendor/idProduct，建立 RNDIS/ECM 两个配置并绑定 UDC，没有 ffs.adb。**9057 本身不能提供 USB ADB。** 这不排除独立的 TCP adbd。

9059 的 run_configfs 是主要 USB ADB 候选。FROM_ADBD 非 y 时可 pkill adbd 并 sleep 1；写 `/data/usb_bind_in_progress`、重建配置、后台绑定 UDC。此 configfs body 没有其他平台分支中的显式 `/etc/launch_adbd start`；可能依赖 supervisor 重启，但本轮未验证实际启动、驱动或网络恢复。函数定义能保留 RNDIS 不等于 Windows 实测必成功。

90DB 的 run_configfs 有 ffs.adb，但没有 gsi.rndis/gsi.ecm。RMNET 不能当作当前 Windows RNDIS 管理网卡的替代保证。直接切入可能失去管理网络，因此没有执行。

三份脚本 hash 均列在 CSV，可与完整 SHA256SUMS 对照。

## FunctionFS 与服务启动

`/etc/initscripts/usb init` 创建 functions/ffs.adb、gsi.rndis、gsi.ecm 等，mount FunctionFS 到 `/dev/usb-ffs/adb`。创建 function 实例、mount ep0、将 function 链接进某个 USB configuration、启动 daemon、Windows 识别接口，是不同条件；只满足其中一个不能声称 USB ADB 已开启。

USB target 脚本读 target（或 machine fallback）后小写化，sdxprairie/sa515m 明确映射到 sdxprairie。9057/9059/90DB 的 target case 都明确调用 run_configfs。其他平台的 launch_adbd 调用不能移植到这个分支作为在线保证。

## boot_hsusb_comp 与 systemrw

FOTA `/etc/usb/boot_hsusb_comp` 是 **90DB**。但以下 units 都有覆盖链：systemrw-boot_hsusb_comp.service、usb.service、pcie.service 使用 `/sbin/mount-copybind /systemrw/boot_hsusb_comp /etc/usb/boot_hsusb_comp`。

mount-copybind 对单文件先检查：若 systemrw source 不存在则复制默认文件；若 source 已存在则不覆盖，随后 bind 到 `/etc`；已绑定则跳过。因此已有 `/systemrw/boot_hsusb_comp` 可以覆盖 FOTA 默认值。不能把 ZIP 的 90DB 当作当前运行值。

usb init 还可能根据 AT 读取值、target、devicetree 或平台分支选择/更新 PID；start_pcie 会根据模式写 `/data/adb.conf` 和 boot composition。它们均只阅读分析，未执行，也未调用 AT 指令。**当前 descriptor 9057、当前 boot 文件值、包内默认 90DB 三者不能混为一谈。**

## 持久化与恢复风险

usb_composition 的参数区分非持久和持久。PERSISTENT=y 不仅写 boot 文件，若发现 usb_qti MTD，还会 flash_erase/nandwrite。这不是本轮允许的操作。

PERSISTENT=n / IMMEDIATE=y 仍会修改 configfs、可能初始化 gadget 并短暂断网；脚本没有保证成功的 9057 自动恢复计时器。当前 shell/SYNC 不可用，没有已验证的独立恢复渠道。不能把“存在 9057 脚本”当作“断网后能执行恢复脚本”。

故 adb usb 和直接 9059 切换都 **NOT EXECUTED**。本地 PRE_USB_ADB_STATE 记录了实际 PnP、IP、网关；公开版见 [PRE_USB_ADB_STATE.md](PRE_USB_ADB_STATE.md)。
