# MF650 ADB Patch 构建报告

2026-10-02；工作目录 `C:\Users\fqxku\Desktop\MF650\MF650-Manager`。**PATCH_READY_OFFLINE / INSTALLABILITY UNVERIFIED**；本轮设备访问 0、安装 NO、设备修改 NO、服务重启 NO、RAM 部署 NO。

| 项目 | 结果 |
| --- | --- |
| 策略 | Literal A；gate 专用 `90DB → 9057`，保留 `9059` |
| 原 adbd SHA256 | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` |
| patched adbd SHA256 | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` |
| 修改 offset | `0x5E18`，覆盖 4 bytes；实际 2 bytes 不同，`0x5E1A` / `0x5E1B` |
| ELF size | 原/后均 30,380 bytes |
| gate 9057 / 9059 | PASS / PASS，静态分支路径验证 |
| 90DB 自动许可 | 移除；未改厂商其他授权路径 |
| USB composition | UNCHANGED；包中没有 USB/boot_hsusb_comp/launch_adbd 文件 |
| TCP 默认端口 | 7628；原指令窗口 SHA256 不变 |
| EDL / 9008 完整恢复 | **NOT READY**；[缺失条件](EDL_RECOVERY_READINESS.md) |

二进制 diff：

```text
OFFSET 0x5E18 = 24088
BEFORE 39 30 44 42   ASCII 90DB
AFTER  39 30 35 37   ASCII 9057
0x5E1A: 44 -> 35
0x5E1B: 42 -> 37
所有其余 30378 bytes 相同；NUL、代码、ELF 元数据不变。
```

gate、flag、XREF、更新机制、包尾校验和版本保护的完整证据见 [设计报告](ADB_PATCH_DESIGN.md)。当前枚举 PID=9057 不代替 boot token=9057；设备 shell/SYNC、UID 和实际固件 hash 未取得，本报告不填在线 PASS。

## 本地产物

目录：`C:\Users\fqxku\Desktop\MF650\MF650-Manager\patch-output`。

| 文件 | 字节数 | SHA256 |
| --- | --- | --- |
| MF650_ADB_Enable_9057_v0.1.zip | 766,095 | `c25d750436fcb013092b3c4a69254e938afd4c157c3f774af468a93af52cc0d5` |
| MF650_ADB_Rollback_v0.1.zip | 766,097 | `7fb8665438fdbb3179010072aeb2c9c550a5c9beef5cfe603f4aea72d7512c24` |
| patched-adbd | 30,380 | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` |

另有 original-adbd.sha256、patched-adbd.sha256、adb-gate.patch.json、binary-diff.txt、BUILD_REPORT.md；GNU file/readelf/objdump 原/后输出及 ELF_VERIFICATION.json 留在同一忽略目录。ZIP 不上传 GitHub 或 Release。

两个 ZIP 都是原 updater 的 recovery 候选，使用原 FOTA BusyBox 在 recovery `/tmp` 提供**强制 SHA256**，不假定环境命令存在。Enable 仅接受原 hash；Rollback 仅接受此 patched hash，原文件 owner/group/mode 必须为 0:0/0755 且不是 symlink。先建立并验证 `/tmp` 备份，唯一持久目标是 `/system/sbin/adbd`；原位 extraction 保留 inode label，成功后验证目标 hash 并 sync。临时 backup 不是独立持久恢复链。

## 已执行验证

- 原 ZIP、adbd、updater、原脚本、file_contexts、BusyBox 和 libshared 精确 hash 全部匹配；原厂 33 字节尾校验匹配。
- 9/9 本地回归测试通过：精确 diff/分支、adbd 改动拒绝、独立指令 fingerprint、重复 literal、错误 offset、共享 XREF/变更 caller、错误 ZIP 不产出、成对包版本保护/回滚/尾损坏/同环境复现、完整产物与拒绝覆盖。
- GNU file：ARM32 little-endian EABI5、ET_DYN、hard-float loader、stripped，有效 ELF。
- GNU `readelf -hW / -lW / -dW / -sW`：原与 patched 输出逐字节相同，program headers、dynamic table、12 项 DT_NEEDED、动态符号不变。
- ARM GNU `objdump -d -M force-thumb -j .text --start-address=0x34d0 --stop-address=0x3544`：gate 指令完全相同；实际 literal 池地址结合 rodata 重新解析为 9057/9059 并验证返回路径。其余 ELF section（除 rodata 的两字节）逐一相同。
- 两个 ZIP CRC、9 条精确 entry、权限声明、固件载荷 hash、厂商尾校验均通过；Rollback 恢复载荷与原 adbd 字节完全相同。

GNU build-id 保留原值 `e01e6d6a66474ce1f546f2e5f7f345ebc0199f6e`，不能作为 patched 文件身份判据；应使用新的全文件 SHA256。没有运行 ARM 固件、update-binary 或包内 BusyBox；这些验证不是 recovery 安装测试。App 代码未修改，不构建 APK，不报告 Android 测试结果。

## 停止点

本轮按用户要求停在离线构建。下一步等待明确安装意向，同时仍需补齐**当前 recovery 包接受/签名策略、分区挂载、辅助工具依赖、在线 adbd hash及 boot token**。原 FOTA 的 recovery 工作流可能需要重启；不保证零重启。Rollback 是严格版本文件恢复候选，不能恢复被损坏的 system、未知 hash 或分区；完整 EDL 未验证。因此不能把 PATCH_READY_OFFLINE 写成“设备已确认可部署”或“不会变砖”。

若之后获准安装，还应先记录 80/8081/7628、PID、RNDIS/IP/Ping，再执行约定安装/激活和只读 id/meminfo/swaps/pwd/SYNC 验证。当前没有做任何这些在线操作，也不执行 adb push。
