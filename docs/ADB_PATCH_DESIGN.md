# MF650 ADB gate patch 设计

2026-10-02。本轮用户授权制作本地补丁，明确不安装。策略 **Literal / A**：仅将厂商 adbd 的 gate 专用 `90DB` 改为 `9057`，保留 `9059`；不改 USB composition、启动脚本、端口、NV 或其他分区。构建状态是 **PATCH_READY_OFFLINE，设备安装兼容性 UNVERIFIED**。

## 固定输入与最小差异

| 项目 | 固定值 |
| --- | --- |
| FOTA ZIP SHA256 | `ef46382fb3b1126c42780e141b60c0a638f7b68d3125659bfaa1c42a14ae8e8e` |
| ZIP 内文件 | `system/sbin/adbd`，30,380 bytes |
| 原 adbd SHA256 | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` |
| 补丁后 SHA256 | `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae` |
| literal VA / 文件 offset | `0x5E18` / `0x5E18`（24,088）；经 PT_LOAD 映射确定，未假定 VA 总等于 offset |
| 覆盖区域 | 4 bytes：`39 30 44 42` / `90DB` → `39 30 35 37` / `9057` |
| 实际不同字节 | **2**：`0x5E1A: 44→35`，`0x5E1B: 42→37`；结尾 NUL 不变 |

原 ELF 中 `90DB` 出现 **1** 次，`9059` 出现 1 次，`9057` 出现 0 次。完整 Thumb 反汇编、PC-relative literal、绝对指针/重定位和旧函数 CFG 复核确认：`90DB` 只有 gate 的 `0x3516` 引用；gate `0x34D0` 的直接调用者只有 service_to_fd 的 `0x357A`。未发现 composition 切换共享该 literal；没有全局替换其他固件文件。

Builder 的 PC 引用扫描是保守候选检查，不是通用反编译器，也不证明任意未知版本的计算指针。其适用性建立在整个 ZIP、整个 adbd 和关键指令窗口均锁定原 SHA256，以及 GNU Thumb 人工控制流复核上。任何 hash、出现次数、映射 offset、指令 fingerprint、引用或调用者变化均 ABORT，不寻找近似位置。

## 完整许可链及静态验证

gate `0x34D0`：fopen64(`/etc/usb/boot_hsusb_comp`, `r`) → fscanf `%s` → fclose → 两次 **strcasecmp 完整 token 比较**。不是当前 descriptor PID 检查，也不是四字节前缀比较。

| 输入 token；初始 flag=0、无厂商授权 | 原 gate | patched gate | patched 控制流 |
| --- | --- | --- | --- |
| 9057 | 不置位 | PASS | `3518 strcasecmp=0 → 351C CBZ → 353A r0=1 → 352E → 3588 STR` |
| 9059 | PASS | PASS | 首次不匹配 → `3524 strcasecmp=0 → 3528 CLZ=32 → 352C LSR5=1 → 3588 STR` |
| 90DB | PASS | 不置位 | 两次不匹配 → CLZ/LSR 结果 0 → caller `3580 BNE → 358A` |
| 90570 / FFFF / 空 token | 不置位 | 不置位 | 未匹配，不写 flag |

`0x3558` 的 literal `0x28FE + (0x3516+4)` 解析为 `0x5E18`；`0x355C` 的 `0x28FA + (0x3522+4)` 仍为 `0x5E20` / `9059`。两个 BLX 仍指向 `0x1EF8 strcasecmp@PLT`。匹配返回 1 后，service_to_fd 在 `0x3586` 解析 flag 地址，在 `0x3588` 写 1。验证的是分支和返回值，不是仅检查 strings 出现 9057。

32 位许可 flag 位于 BSS **VA `0x8044`**。全部正常读写见 [完整授权逆向](ADB_VENDOR_AUTH_REVERSE.md)：

| 类别 | 指令地址 |
| --- | --- |
| 读 | `0x2C8E` 服务线程；`0x2E9E` shell；`0x364A` 授权选择；`0x3910` 回退选择 |
| 写 | `0x3588` boot gate；`0x38F8` 两个序列号来源共用的厂商授权成功路径 |

厂商授权辅助函数及其凭据逻辑全部保留。flag 为进程全局：未匹配不清除已有 flag，断开客户端不撤销许可；新进程 BSS 清零。Literal A 的明确取舍是**移除 90DB 的自动许可，保留 9059**；没有扩展为无条件允许，未采用更复杂的第三条件或 branch patch。

USB 枚举 9057 **不能证明在线 boot 文件 token 为 9057**。线上 daemon/library hash、boot 覆盖和 UID 仍未知；静态 PASS 不等于 shell/SYNC 在线成功。默认 TCP `0x258C movw r0,#7628 → 0x2590 local_init` 不变；不开放 5555，不提供 USB ADB。

## 原 FOTA 格式与单文件候选包

原 updater-script 共 **3,092** 行：第 3 行格式化 UBIFS system，第 4 行挂载，第 5–6 行全量提取；第 2,629 行写 boot，第 2,635 行写 uefi，最后 unmount。它不是可直接运行的小补丁安装脚本。第 1,532 行定义 adbd 为 `0:0 / 0755`。两个新脚本不含 format、delete_recursive、write_raw_image、service restart 或 reboot。

原 `update-binary` 是 145,312 bytes 的 ARM ELF，SHA256 `852c8ba869bc4bcbb1afb690e44f7e7af780015207380f8bb0504e084e731dd1`，仅作为包内数据复用，未执行。注册函数不是只凭 strings 推断：

| 注册名称 | name PC 引用 | handler Thumb pointer；RegisterFunction 调用 |
| --- | --- | --- |
| mount | `0x6A24` | `0x5341`；`0x6A28 → 0x12448` |
| package_extract_file | `0x6A98` | `0x43A1`；`0x6A9A → 0x12448` |
| read_file | `0x6B2C` | `0x3F55`；`0x6B2E → 0x12448` |
| sha1_check | `0x6B38` | `0x6CAD`；`0x6B3A → 0x12448` |
| run_program | `0x6B74` | `0x48DD`；`0x6B76 → 0x12448` |

is_mounted、unmount、set_perm 也有实际注册；内建 assert 的 name 引用在 `0x12556`。read_file 返回文件 blob；sha1_check handler 调 SHA1 并比较预期 digest。run_program 使用 execv/waitpid 返回 status 字符串，比较 `"0"` 只接受正常成功退出。

原 ZIP 没有 JAR CERT/SF/RSA 条目，EOCD comment 长度 0；ZIP 正文后有 **33 bytes**：32 位 ASCII MD5 加 LF。FOTA libshared.so 的 `is_update_file_valid 0x8F84` 保存尾值，在 `0x9014–0x903A` 用 `.data VA 0x15000` 的 1,982 bytes 校验输入覆盖尾部，计算 MD5，`0x907C` 比较前 32 字节，成功 `0x9086` truncate 至 ZIP 正文长度。这个函数会修改待验文件；Builder 仅在 PC 内存中复核等效摘要，没有执行它。

校验输入 SHA256 `eb4c413cf6861cac0ae801fd53fa965c3b18b4052da6dc984e650abf239d087c`，从锁定 hash 的原 libshared.so 按 PT_LOAD 映射提取，不公开完整内容。`MD5(original ZIP正文 + 校验输入)` = 原尾值 `277479e5f3e191ad4859bf6567f58738`，精确匹配。候选包使用相同格式；它是厂商完整性校验，**不是已验证的 recovery 数字签名**。

原 metadata 是 `ota-type=FILE / ota-required-cache=0 / pre-device=None / post-build=None / post-timestamp=None`，不提供可靠型号/版本保护。候选包保留该 metadata，版本保护由 adbd 的精确 SHA256 实现。未发现原包支持 native SHA256 Edify 函数；不会假设 recovery PATH 中有 sha256sum。

## 安装与回滚的包内逻辑

两个 ZIP 各 **9 个条目**：原 update-binary、metadata、file_contexts；新 updater-script；唯一持久文件 `system/sbin/adbd`；原 FOTA BusyBox；before/after/backup 三份 SHA256 校验清单。BusyBox 来自 `system/bin/busybox.nosuid`，870,400 bytes，SHA256 `bf85d416c2109464cd533596d493054bd200fc8c2938ca97f3a92140dd1c85ab`，不是下载或重编译的工具。

脚本要求 `/system` 尚未挂载，然后按原五参数 `mount("ubifs","UBI","system","/system","")` 挂载；先用内建 SHA1 检查源文件/载荷，再提取并验证 BusyBox 到 `/tmp/busybox.mf650-adb-patch`。其 basename 保留 busybox 前缀供 multi-call 分派。工具、校验清单和备份都只留在 recovery `/tmp`。

**写入前必须通过真正的 SHA256 校验**，没有 SHA1 降级放行：Enable 接受原 SHA256；Rollback 接受本 Builder 的 patched SHA256。Rollback 检查 patched 值是必要的，否则成功安装后的文件无法回滚；它不接受未知固件。还检查当前文件不是 symlink、owner/group/mode 正好 `0:0:755`，复制 `/tmp/mf650-adbd-before` 并校验备份 SHA256，然后才允许 adbd 提取。辅助程序/动态 loader/库/applet 不可用或任一返回非零就 ABORT，不继续写 adbd。

单文件 extract handler 在 `0x4420–0x443E` 使用原路径 open flags `0x101241`（含 O_WRONLY/O_CREAT/O_TRUNC/O_SYNC）、mode 0600；该 mode 只适用于新建。前验保证既有文件存在，handler 没有 unlink/rename，故原位写入保留 inode 和既有 SELinux label。`0x4456` 进入 fsync wrapper，随后 close；set_perm 保持 `0:0 / 0755`，再校验完整目标 SHA256、sync、unmount。没有设置 selabel 或 chmod 777。参考系统配置为 SELINUX=disabled，但不据此假定线上也禁用 SELinux。

Rollback 先构建并验证 ZIP CRC、尾校验和恢复载荷 SHA256 等于原版，然后才构建 Enable。这个**离线恢复验证不等于在设备执行过恢复**。原位写入不是断电原子操作；严格 Rollback 会拒绝已损坏/部分写入/未知 hash，不能充当任意分区修复工具。临时备份在 recovery 退出后不保证保留，独立 EDL 恢复尚未就绪。

两个 ZIP 是沿原 updater 机制制作的**离线候选**。当前 recovery 的签名/上传接受策略、分区映射、挂载状态、BusyBox 依赖仍未验证；原 BusyBox 需要 libm.so.6、libresolv.so.2、libc.so.6 和 ARM loader。原包没有这些在线证明，不绕过签名策略，也不执行模拟 updater。通常 FOTA recovery 流程需要重启进入 recovery；即使新脚本无 reboot，也不能承诺整体免重启。

## 复现与下一阶段边界

依赖：Python，`capstone==5.0.9`、`pyelftools==0.33`。从仓库根目录运行：

```text
python tools/build_mf650_adb_patch.py ../MF650_2.3_Fota.zip --output patch-output
python -m unittest discover -s tools/tests -p test_build_mf650_adb_patch.py -v
```

缺少参考包时相关 tests 显式 SKIP，不能称 PASS；可用 MF650_FOTA 指定本地包。Builder 拒绝非空输出目录，避免覆盖用户文件。ZIP 时间/条目顺序固定，同一压缩环境可复现；不同 zlib 版本的 ZIP 字节/hash可能不同，patched adbd hash 必须相同。

本轮不安装、不激活、不进入 EDL、不测试服务；二进制/ZIP 全部本地且 gitignored。公开 Builder、tests、偏移/hash/字节元数据和文档。下一阶段须先核实在线版本/boot token及实际恢复与安装兼容条件，并取得用户明确安装授权；当前不以离线 PASS 宣告 root、shell/SYNC 或 RAM 修复成功。实测无认证 root TCP ADB 时仍须按既有架构报告记录 LAN 暴露风险。
