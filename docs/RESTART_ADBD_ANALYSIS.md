# restart_adbd.asp 的真实行为

日期：2026-10-02，服务许可逆向阶段更新。**GET = NOT EXECUTED**。稳定 serial/adb_devid 的正常启动 bookkeeping 本身可归为 **R1 / 可恢复**；不会仅因写文件就判为高风险。R1 需要 USB 服务已 active、在线脚本及 override 已核实等条件，目前未满足在线证据要求。本轮用户明确要求只报告，不执行重启。

## ASP 与 httpd xref

FOTA `system/www/restart_adbd.asp` 内容为 `<% restart_adbd(); %>`。
`system/usr/bin/httpd` 为 122,936 字节，SHA-256 `a0e3f3c7929cddb06ef5b8a8e7f194f2a81d9d263fd26edf7a93b03594adb885`。

ELF `.data` 注册表 VA 0x1a2a8 存 name pointer 0x15018（`restart_adbd`）与 Thumb function pointer 0x8bd9。这将 ASP 名称明确连接到 handler 0x8bd8，不是只看到相邻字符串。

```text
0x8bdc: r0 = VA 0x12458, "systemctl restart adbd.service"
0x8be0: BLX system@PLT (0x5494)
0x8bee: fwrite("ok", 1, 2, response)
0x8bf4: fflush(response)
0x8bf8: return 0
```

handler 没有直接调用 composition、property_set、重启设备或修改防火墙。它忽略 system() 返回值，因此 HTTP 的 `ok` **不能证明服务重启成功、5555 出现或 USB ADB 可用**。

## 间接执行链及副作用

adbd.service 的 ExecStart 是 `/etc/launch_adbd start`，Requires=usb.service，After=usb.service pcie.service，Restart=always。

launch_adbd 在启动 `/sbin/adbd` 后必经序列号处理：

- 若 `/etc/adb_devid` 非空，复制其内容到 `/sys/class/android_usb/android0/iSerial`。
- 否则从 cmdline 或 random/boot_id 生成序列号，写 iSerial，并写 `/etc/adb_devid`，随后 sync。
- 可 source `/etc/default/adbd`；该文件不在此 ZIP，在线是否有该文件未知。

其中 UDC 条件是 `[ -z /sys/kernel/config/usb_gadget/g1/UDC ]`，测试的是非空字面路径，通常为假；不能把这行误说成检查 UDC 文件为空后重新绑定。序列号写入则不依赖这个错误条件。

usb.service 是 RemainAfterExit 的 oneshot。Requires 不会单凭 adbd 重启就重启已 active 的 usb.service；但若它未 active，依赖启动会进入 `/etc/initscripts/usb init`，建立 gadget、FunctionFS、序列号并选择 composition。ExecStartPre 还会 copy/bind：

```text
/systemrw/adb_devid       -> /etc/adb_devid
/systemrw/boot_hsusb_comp -> /etc/usb/boot_hsusb_comp
/systemrw/data/usb        -> /etc/data/usb  (drop-in)
```

如果 adb_devid 已被 bind 到 systemrw，上述写入可能成为持久数据。在线 unit 状态、覆盖文件和启动脚本版本均未取得；不能保证序列号写失败，也不能保证全部无副作用。

## 逐项写入与风险

| 路径 / 动作 | 写入条件 / 来源 | 幂等性 | composition / RNDIS / modem |
| --- | --- | --- | --- |
| legacy iSerial | launch_adbd 每次启动；优先已有 adb_devid，其次 cmdline，最后 boot_id 尾段 | 既有稳定 ID 时重复写相同值；节点不支持时可能失败，不能假定必然失败 | 只改 serial 字符串，不选择 PID/网络/基带；驱动具体效果须核实 |
| `/etc/adb_devid` | 仅空/不存在时写上述新 ID | 首次生成后复用；boot_id 值在同次 boot 稳定，若未保存而跨 boot 重建则可能变化 | 正常元数据；若 bind 到 systemrw 会持久，但本身不改 PID/NV/SIM |
| `/systemrw/adb_devid` | copybind source 缺失时复制原文件；后续通过 bind 写入 | 已存在/已绑定则复用 | serial 元数据；不把所有 systemrw 写入等同 composition 写入 |
| `/dev/kmsg`、sync | 启动提示和落盘 | 日志每次追加，sync 不选择配置 | 无直接 PID/网络/基带设置 |
| configfs UDC | 原脚本条件测试非空字面路径，正常为假 | 没有正常必经 UDC 写入 | 不把错误条件当作确定重新绑定 |
| `/systemrw/boot_hsusb_comp` / bind target | USB 依赖需要启动且 source 不存在时，copybind 创建原内容；USB init 随后可能写 PID | copybind 本身复用；init 的平台/AT 分支不保证无变更 | 若触发 USB init，可切换 composition、RNDIS，并持久保存 boot 值；另见全局写入者 |
| 可选 `/etc/default/adbd` | 文件存在则 source；本 ZIP 未包含 | 在线内容未知 | 内容未核实前不能承诺只有正常启动 bookkeeping |

**R1 适用条件**：在线 unit/launch_adbd/library 与已审计链一致，usb.service 已 active 且 RemainAfterExit 有效，systemrw 覆盖不要求重新初始化 USB，adb_devid 稳定，无未知 default/override/自动授权操作。此条件下 adbd restart 短暂中断 ADB transport，重放 serial 元数据，不必重启整机；未见该路径修改 PID、boot composition、网络、modem、SIM 或 NV。

若 USB 依赖 inactive/失败，可能走 copybind 和完整 USB init；平台/AT 返回值可能修改 boot PID和网络。该场景须单独按 R2 或持久硬件路径更高等级评估，不能套用 R1。当前没有 unit/override/boot 文件读取证据，因此 **在线整体风险尚不能确认只为 R1**。这些未知条件不是“任何文件写入都是高风险”的结论。

## 是否能开启 ADB

当前 USB 枚举 9057；FOTA 9057 没有链接 ffs.adb。仅启动 adbd 不会自动把它变成 9059。FOTA 自带 property_set 为空实现，也没有依据认为一次失败 tcpip 后留下了可供重启采用的 5555 属性。

重启本身不设置 `0x8044`，新进程 BSS 清零；只有后续 boot 文件匹配或厂商合法授权会重新置位。因而对当前服务拒绝模式，解锁结论为 **UNLIKELY（参考 FOTA）**；当前 boot 文件/在线版本未知，不能说绝对不可能。详见 [厂商授权](ADB_VENDOR_AUTH_REVERSE.md)。

即便另一个在线版本已设置 TCP 属性，只有真实连接和 shell/SYNC 读取才可确认；仅 `ok` 不够。此处不因未测试而声称接口不存在，也不把 FOTA handler 直接等同在线实现。

## 两阶段执行边界

上阶段用户要求“没有其他副作用”才可 GET，因此没有调用。当前阶段用户要求重新评估正常 bookkeeping，但同时明确“本轮仍不要执行”；本轮未调用 endpoint、未 systemctl、未做新的端口测试。唯一一次 TCPIP 的历史记录见 [ADB_ENABLE_TEST_REPORT.md](ADB_ENABLE_TEST_REPORT.md)。下一步优先合法临时授权，而非没有解锁证据的重启试探，见 [条件方案](ADB_ENABLE_PATHS.md)。
