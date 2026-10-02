# MF650 FOTA 离线分析

日期：2026-10-02。参考包仅在 PC 解包；没有运行包内脚本/ELF、刷机、升级、执行 update-binary 或写入设备分区。设备文件访问仍未恢复，RAM 修复未部署。

## 参考包与完整性

| 项目 | 结果 |
| --- | --- |
| 文件 | MF650_2.3_Fota.zip |
| 字节数 | 58,614,865 |
| SHA-256 | `ef46382fb3b1126c42780e141b60c0a638f7b68d3125659bfaa1c42a14ae8e8e` |
| ZIP entries | 5,006：455 个目录、4,551 个文件 |
| 文件解压总大小 | 116,633,721 字节 |
| 解包复核 | 4,551/4,551 文件大小及 SHA-256 匹配；读取 ZIP 时校验 CRC |
| 大小写冲突 | 9 个 Linux 文件名在普通 NTFS 下冲突，以 `.__zipcase__<hash8>` 保存第二个文件 |
| ZIP symlink mode entries | 0；不能据此推断原系统没有符号链接 |

目录按要求建立为 `fota-analysis/{extracted,adb,usb,web,binaries,diff,reports,hashes}`。
清单保留原路径、PC 路径、ZIP mode、时间、大小、SHA-256。ZIP 没有 UID/GID，不能重建属主；PC 文件权限/修改时间也不代表设备原元数据。

安装脚本另有声明式元数据：updater-script 会创建 libadbd.so.0 / libcutils.so.0 指向版本文件的 symlink，并对 adbd、launch_adbd、实际库设置 UID/GID 0:0、mode 0755；initscripts/adbd 为 0750。这些是安装意图，未在 PC 执行或应用，也不是在线文件元数据。解包目录是 ZIP 内容的保真副本，并非执行安装步骤后的完整根文件系统。脚本还含格式化 system、写 boot/uefi 的操作，均未执行。metadata 中 pre-device/post-build 为 None，不能用它保证当前设备升级兼容性。

公开产物：[FILE_MANIFEST.csv](../fota-analysis/hashes/FILE_MANIFEST.csv)、[SHA256SUMS.txt](../fota-analysis/hashes/SHA256SUMS.txt)、[ARCHIVE_SUMMARY.json](../fota-analysis/hashes/ARCHIVE_SUMMARY.json)、[BOOT_IMAGE_SUMMARY.json](../fota-analysis/hashes/BOOT_IMAGE_SUMMARY.json)。固件、解压内容、分析依赖与原始设备日志均不提交。

## 平台证据

| 项目 | 文件内证据 | 结论 |
| --- | --- | --- |
| Target | `system/target` 内容 `sdxprairie`；build.prop 的产品名 `sdxprairie-mdm` | sdxprairie |
| SDX55 | boot.img 内解压内核的编译路径包含 `SDX55_apps` / `sdx55-1.4`，与 target 一致 | SDX55 构建来源；不是从 ZIP 名猜测 |
| Kernel | boot.img 内核版本字符串及 kernel opkg control | Linux 4.14.206-perf；内核编译时间 2024-01-30 |
| USB target | `/sbin/usb/target` 将 sdxprairie / sa515m 映射为 sdxprairie | 选用各 composition 的 sdxprairie 分支 |
| 初始化 | `/lib/systemd/system/adbd.service`、usb.service、systemrw 相关 units | systemd 服务链 |
| ADB | ARM ELF `/sbin/adbd`、libadbd 与完整服务分派实现 | 真正的厂商定制 adbd |
| Web | `/www/restart_adbd.asp`、`/usr/bin/httpd`；另有 feiliu ELF | FOTA 包含 Web 服务资料；不能等同当前 8081 程序 |
| QMI | 两份 qmi_ip_cfg.xml 的 tcp_server_port 为 7777；qmi_ip_multiclientd.service 指定程序和配置 | 7777 的 QMI IP 线索明确 |

boot.img 为 Android boot magic，页大小 4096、kernel_size 10,842,713、ramdisk_size 0。内核 gzip 内容只解压为 PC 数据，未启动。
build.prop 的 `ro.build.version.release=202402021114` 是此包的版本字段，不当作 Android 主版本或 userdebug 证据。

## 分析方法与可复核性

使用 GNU `strings -a -tx`、`readelf -aW`、ARM GNU objdump 2.42、Capstone 5.0.9、pyelftools 0.33。
ELF 为 little-endian ARM32 EABI5 hard-float，目标函数为 Thumb；无完整 `.symtab`。GNU objdump 必须对 `.text` 使用 `-M force-thumb`，默认 ARM 反汇编会误解码这些代码。PLT 使用 ARM 模式并结合 `.rel.plt` 标注。

关键结论来自完整函数控制流、PC-relative literal/string、ELF relocations、动态符号大小和调用点；不把 strings 邻近位置当作调用关系。自编辅助工具的 string candidate 仅辅助人工复核，线性输出中的 literal pool 不是可执行指令。关键指令又以 GNU Thumb 输出交叉检查。

本地复核命令（只读固件数据）：

```text
python tools/fota_inventory.py <reference.zip> fota-analysis
python tools/fota_usb_matrix.py fota-analysis/extracted/system/sbin/usb/compositions fota-analysis/reports/USB_COMPOSITION_MATRIX.csv
strings -a -tx <ELF>
readelf -aW <ELF>
arm-linux-gnueabihf-objdump -d -M force-thumb -j .text <ELF>
python tools/fota_elf_disasm.py <ELF> <local-output.txt>
```

Python 分析依赖和 GNU ARM binutils 的本地包放在被忽略的 `fota-analysis/binaries/`。这些工具不会加载执行目标 ELF，也不连接设备。

## 与在线设备的差异

FOTA 默认 boot_hsusb_comp 是 90DB；当前 Windows 实测是 05C6:9057。当前 `/etc/usb/boot_hsusb_comp` 尚未读到，不能以枚举 PID 代替该文件内容，也不能用 FOTA 默认覆盖在线配置。

包内清单没有名为 setprop/getprop 的文件，也没有当前定制 `feiliu.sh` 或 `mf650.html`。这不是对所有运行时命令或内嵌资源的否定；只说明 ZIP 文件不能直接当作当前设备文件备份。feiliu ELF 内未找到当前 REST 的 `/api/device/info` 或 `memory_usage` 字面量，不能据此确定当前 8081 实现。

在线 adbd/httpd/tcpserver 的 SHA-256 未取得。对在线服务身份及拒绝原因的解释保留 LIKELY；离线函数行为只对表中 FOTA hash 成立。

## 结果与报告入口

- [FOTA_ADB_ARCHITECTURE.md](FOTA_ADB_ARCHITECTURE.md)：默认 TCP 7628、USB 端点、服务许可检查、空属性实现及 root 证据边界。
- [TCP7628_REVERSE.md](TCP7628_REVERSE.md)：tcpserver 的 2358 bind 常量和在线 7628 分类。
- [ADB_USB_SERVICE_REVERSE.md](ADB_USB_SERVICE_REVERSE.md)：`usb:` 没有切换到 9059 的实现。
- [RESTART_ADBD_ANALYSIS.md](RESTART_ADBD_ANALYSIS.md)：直接 handler 与启动链间接副作用。
- [USB_COMPOSITION_ANALYSIS.md](USB_COMPOSITION_ANALYSIS.md)：66 个 PID、9057/9059/90DB、systemrw 覆盖。
- [ADB_ENABLE_RESEARCH.md](ADB_ENABLE_RESEARCH.md)、[ADB_ENABLE_TEST_REPORT.md](ADB_ENABLE_TEST_REPORT.md)：执行条件与唯一一次 TCPIP 测试结果。

已完成参考包分析和符合条件的测试；尚未找到经过验证、可重复、可回滚的 ADB 开启方式。没有实时 meminfo 或可用 shell/SYNC，不能标记 RAM 修复完成。
