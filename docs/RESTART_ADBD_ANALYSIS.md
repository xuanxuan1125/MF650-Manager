# restart_adbd.asp 的真实行为

日期：2026-10-02。**本轮 GET = NOT EXECUTED**。直接 Web handler 只请求重启 adbd，但间接启动链有 USB 序列号/可能持久文件写入，不能满足用户要求的“没有其他副作用”。

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

## 是否能开启 ADB

当前 USB 枚举 9057；FOTA 9057 没有链接 ffs.adb。仅启动 adbd 不会自动把它变成 9059。FOTA 自带 property_set 为空实现，也没有依据认为一次失败 tcpip 后留下了可供重启采用的 5555 属性。

即便另一个在线版本已设置 TCP 属性，只有真实连接和 shell/SYNC 读取才可确认；仅 `ok` 不够。此处不因未测试而声称接口不存在，也不把 FOTA handler 直接等同在线实现。

## 本轮停止理由

用户只允许在反汇编确认“没有其他副作用”时做一次 GET。直接 handler 已确认，**完整执行链不满足这一条件**。因此没有调用 endpoint、没有 systemctl 操作，也没有为它做新的端口前后测试；前后记录属于唯一一次已授权的 TCPIP 测试，见 [ADB_ENABLE_TEST_REPORT.md](ADB_ENABLE_TEST_REPORT.md)。
