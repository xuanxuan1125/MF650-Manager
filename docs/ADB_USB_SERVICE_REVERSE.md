# ADB usb: 服务离线逆向

日期：2026-10-02。目标 hash 见 [FOTA_ADB_ARCHITECTURE.md](FOTA_ADB_ARCHITECTURE.md)。**未执行 adb usb。离线证据没有证明它能切入 9059。**

## 真实调用链

adbd service_to_fd：0x37bc 比较 `usb:`，匹配后 0x39fe 准备 Thumb handler pointer 0x274d，进入 create_service_thread 0x2c50。该线程创建函数仍受 flag 0x8044 许可检查限制。

完整 handler 的有效指令范围 0x274c–0x276c：

```text
0x2754: r1 指向字符串 "0"（VA 0x57c4）
0x2756: r0 指向 "service.adb.tcp.port"（VA 0x564c）
0x2758: BLX property_set@PLT
0x2760: 响应字符串 "restarting in USB mode\n"
0x2762: BLX WriteFdExactly
0x276c: 关闭服务 FD 的尾调用
```

这段没有传入 9059/90DB、调用 usb_composition/usb_composition_switch、system/systemctl、写 configfs/sysfs、启动 composition 脚本或显式退出 daemon。配套 libcutils 的 property_set 又是四字节空实现，所以该 FOTA 中连 TCP 属性也没有真正改变。

adbd 和 libadbd 的字节/strings 检查均无 `usb_composition` 引用。否定结论主要来自 **完整 usb handler 的调用链**，不是仅凭字符串缺失。

## 9059 / 90DB 的实际用途

它们在 0x34d0 的函数中作为 `/etc/usb/boot_hsusb_comp` 的允许值，匹配可设置服务许可 flag。两个比较点为 0x3516 / 0x3522；该函数由 service_to_fd 0x357a 调用。

这是读文件后判断是否开放服务的路径，不是收到 `usb:` 后选择 composition 的路径。不能用 strings 相邻推导“adb usb 会运行 9059”。当前枚举 9057 也不能直接证明 boot 文件必为 9057。

## composition 是另一个控制机制

`/sbin/usb_composition` 接受 PID、HSIC、PERSISTENT、IMMEDIATE、FROM_ADBD。它最终调用 `/sbin/usb/compositions/<PID>`，可能先初始化 qcom gadget；非持久参数也会断开/重建 USB 功能。持久参数还可能 flash_erase/nandwrite usb_qti，本轮均禁止且未调用。

包内另有 ARM ELF `/usr/bin/usb_composition_switch`，其 strings 有格式 `usb_composition %s %s %s y y`。它是另一个程序，不能证明 adbd usb handler 调用它；未执行、未把它当作已验证的安全接口。usbd、USB debug 脚本、udev 节点规则、FunctionFS 初始化均只作离线资料检查。

## 执行条件检查

| 条件 | 结果 |
| --- | --- |
| USB 直连、记录当前 05C6:9057/PnP/IP/网关 | PC 本地记录已完成 |
| usb: 明确进入 9059 | **不满足**：只调用空 property_set |
| 9059 在 sdxprairie 定义 RNDIS+ADB | 离线 YES；未验证在线驱动、端点与枚举 |
| 可靠恢复 9057 | **不满足**：没有独立可用 shell/SYNC/恢复控制渠道 |
| 禁止 90DB 网络丢失风险 | 已遵守，没有切换 |

结论：**adb usb = NOT EXECUTED**。未调用 PID 脚本，未通过其他协议替代执行 USB 切换。下一步条件及当前停止点见 [ADB_ENABLE_RESEARCH.md](ADB_ENABLE_RESEARCH.md)。
