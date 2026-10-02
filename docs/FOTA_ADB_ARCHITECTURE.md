# FOTA ADB 架构与服务限制

日期：2026-10-02。地址均为下表对应 ELF 的静态虚拟地址，非在线进程地址；Thumb function pointer 的最低位为 1。

## 核心文件

| ZIP 内 system 下路径 | 大小 | SHA-256 |
| --- | --- | --- |
| sbin/adbd | 30,380 | `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185` |
| usr/lib/libadbd.so.0.0.0 | 67,992 | `ac87b0000529413d2c2c2c274ef96584c9046208b241935e7c13a8250eb1cc84` |
| usr/lib/libcutils.so.0.0.0 | 42,680 | `ac067fd04f97719575a7a9d34697845240d8b5fed0a66064ce28b44e4140c984` |

adbd 为 stripped ARM32 ET_DYN，入口 0x2261；动态导出 `_Z13service_to_fdPKc`（0x3561，size 1460）。libadbd 包含 transport 注册、消息处理、local/USB transport 与 SYNC 实现，回调 executable 的 service_to_fd。它不是只返回 CNXN 的空壳。

adbd 的 DT_NEEDED 包括 libadbd.so.0、libcutils.so.0、libbase、libfs_mgr、libselinux、libpthread、libglib、liblog、libstdc++、libgcc_s、libc 与 ARM loader。libadbd 也依赖 libcutils；两者的 property_get 是未定义导入，property_set 是 adbd 未定义导入。包内 libcutils 提供下述符号，启动 unit 未指定 LD_PRELOAD。在线库是否相同尚未核实。

对包内 usr/lib 与 lib 顶层 ELF DSO 的动态符号检查，只发现 libcutils.so.0.0.0 定义 property_get/property_set；包内无 ld.so.preload。updater-script 786/818 行声明创建相应 SONAME symlink（仅阅读，没有执行）。opkg 的 system-core-adbd / system-core-usb 为 git-r19、armv7at2hf-neon，不能据此称为标准 Android 用户构建。

## 启动与 transport

```mermaid
flowchart TD
  A[systemd adbd.service] --> B[Requires usb.service]
  A --> C[/etc/launch_adbd start]
  C --> D[/sbin/adbd]
  D --> E[检测 android_adb 或 usb-ffs/adb/ep0]
  E --> F[端点存在则 usb_init]
  D --> G[读取 service / persist adb tcp port]
  G --> H[有效正数则 local_init 指定端口]
  G --> I[空值或无效则 local_init 7628]
  D --> J[服务分派及厂商许可检查]
```

adbd.service 是 Type=forking、Restart=always、KillMode=process，ExecStart 为 `/etc/launch_adbd start`，没有 User= 降权设置。launch_adbd 调 start-stop-daemon 启动 `/sbin/adbd`，并处理 USB 序列号，详见 [重启分析](RESTART_ADBD_ANALYSIS.md)。旧 `/etc/initscripts/adbd` 也有 start/stop/restart、序列号写入，但不是这个 unit 的 ExecStart。

main 0x2480 的明确调用链：

| 地址 | 行为 |
| --- | --- |
| 0x254e / 0x25ce | access `/dev/android_adb` / `/dev/usb-ffs/adb/ep0` |
| 0x2558 | 任一端点存在则 usb_init |
| 0x2566 → 0x256a | property_get `service.adb.tcp.port` |
| 0x266a → 0x266c | service 值为空时读 `persist.adb.tcp.port` |
| 0x257e / 0x2588 | sscanf 数值并要求 >0 |
| 0x2632 | 正数路径 local_init(port) |
| 0x258c → 0x2590 | `movw r0,#7628` → local_init |

USB 初始化成功与否不会跳过该 TCP 默认路径。FOTA 明确支持 TCP ADB，默认端口是 **7628**；不能沿用上游的常见 5555 默认值。

## 最关键的属性实现

libcutils 动态符号及完整函数确认：

```text
property_get: symbol 0x5d89, size 44
  0x5d88: default 为 NULL 则返回 0
  否则把第三参数 default 复制到第二参数 value，限制为 91 字节
  不查询 property name，不访问 Android property service

property_set: symbol 0x5db5, size 4
  0x5db4  00 20    movs r0, #0
  0x5db6  70 47    bx lr
```

因此 property_get 只返回调用者默认值，property_set 是 **返回成功的空实现**。property_get_bool 基于同一 get 函数，返回默认布尔值。不能把 build.prop 中的键当作此库已经读取的运行时属性，也不能把 handler 的成功文字当作真正修改属性、重启或换端口的证据。

tcpip handler 0x277c 只校验正数、格式化端口、property_set(service.adb.tcp.port, port)、写响应、close；没有显式重启 daemon。usb handler 0x274c 类似，仅 property_set(service.adb.tcp.port, "0")。二者在此库配套情况下都不能完成宣称的模式切换。

## 9059 / 90DB 是服务许可检查

函数 0x34d0 在 0x34ee 使用 `/etc/usb/boot_hsusb_comp`，fopen/fscanf 读取字符串，分别在 0x3516、0x3522 与 `90DB`、`9059` 作 strcasecmp，匹配返回 1。

service_to_fd 在 0x357a 调它；返回 1 时在 0x3588 将 BSS flag 0x8044 置 1。没有匹配时，此处不重置已经置位的 flag；初始 BSS 为 0。检查依据是 **boot 文件内容**，不是当前 USB descriptor PID。

create_service_thread 0x2c50 中，0x2c8a–0x2c92 读取 flag；为 0 则跳到 0x2d46 返回 -1，不启动处理线程。shell handler 的 0x2e9c 也检查此 flag。后续服务许可逆向已确认另一写入指令 `0x38f8`：首选 NVRAM serialno、回退 cmdline serialno 的派生摘要与 shell 参数比较，成功后共用该写入点。整个参考 ELF 确认 2 条 STR、3 条逻辑启用路径；没有推导/复现凭据或绕过。完整控制流和生命周期见 [ADB_VENDOR_AUTH_REVERSE.md](ADB_VENDOR_AUTH_REVERSE.md)。

| 服务字符串 | 分派/目标 | 证据含义 |
| --- | --- | --- |
| shell: / exec: | 实际进程/PTY 路径；shell 内含 flag 检查 | 代码存在不表示在线已允许 |
| sync: | 0x3730 → 0x382c → create_service_thread | 可解释 OPEN 后关闭的候选原因 |
| root: | 0x3760 → 0x38fc，handler 0x298c | 仍受服务线程许可限制 |
| unroot: | 分派至对应属性 handler | 仍受许可与空属性实现限制 |
| tcpip: | 0x37a8 → 0x39d2，handler 0x277c | 标准服务名称；FOTA 属性更改无效 |
| usb: | 0x37bc → 0x39fe，handler 0x274c | 不调用 composition |
| remount: | strings 和真实分派存在 | 仅离线审计，未调用 |

在线 9057、CNXN=device、shell/SYNC/tcpip closed 与这类限制相容；但当前 boot 文件、flag、daemon hash 都未知，不能宣布已证实在线拒绝的唯一原因。

## root、userdebug 与认证

main 的 ro.adb.secure 读取在 0x24b8/0x24ba，默认值 0；为空属性库配套时关闭标准 auth_required。unit 无 User=，main 有 setgroups(9)，未发现 setuid/setgid/setresuid/setresgid/capset 导入或 main 的标准降权调用。因此 **FOTA 正常由此 unit 启动时默认 root 很可能成立**；不是在线 UID 验证。

root handler 先 getuid：已为 0 时返回 already root；非 root 时读取 ro.debuggable，默认空字符串，该库不能返回 "1"，会走 production deny。unroot handler 的属性写同样为空操作。ro.secure 的引用在 verity 相关处理路径，不能把它误说成此 main 的降权分支。

build.prop 未声明 ro.secure、ro.debuggable、ro.adb.secure 或 build.type；没有证据确认 userdebug。未执行 adb root/unroot；当前 shell UID **UNKNOWN**。若后续证实匹配此无认证 root 行为，应标记 **UNSAFE FOR PERMANENT LAN EXPOSURE**，不能持久开放 LAN。

作为对照，[AOSP 同时期 adb_main.cpp](https://android.googlesource.com/platform/system/core/+/b0b4946/adb/adb_main.cpp) 有属性/降权/transport 分支；这里只用于识别差异，不能以该源码替代厂商 ELF。
