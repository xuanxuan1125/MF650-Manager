# boot_hsusb_comp 全局读写、复制与覆盖链

日期：2026-10-02。扫描全部 4,551 个解包文件的字节内容，字面路径命中 20 个文件；继续追踪脚本变量、ELF 引用和 `usb_composition_switch`，没有运行这些程序。逐文件 hash/命中次数：[BOOT_USB_COMP_REFERENCES.csv](../fota-analysis/reports/BOOT_USB_COMP_REFERENCES.csv)。此清单覆盖参考 FOTA 的可见正常路径；在线覆盖文件及额外安装组件尚未取得。

## 内容读取和写入

| 文件 / 地址 | 角色与条件 | 内容 / 下游影响 |
| --- | --- | --- |
| `/sbin/adbd`，`0x34d0` | 只读；每次 service_to_fd 前调用 | 90DB/9059 匹配开启内存许可，自己不改 USB |
| `/etc/start_pcie` | `/dev/mhi_ctrl` 存在时写；模式取 `/data/debug_transport.conf` 或首次平台默认 | pcie→`none`，usb→`901F`；同时写 `/data/adb.conf`；首次还写 debug_transport.conf |
| `/etc/initscripts/usb`，207–289 行 | 平台/硬件条件写 | 8916→9091；9650/sdx20 的 PCIe 条件→901F；sdxpoorwills/sdxprairie 的 ADP subtype 分支→90DB/9104/9102/9106/901F；8909 等→901D。不能把每个平台分支当作当前 MF650 必经路径 |
| 同脚本，383–389 行 | legacy android_usb 存在时读 `AT+USBCFG?`；非空且不同于文件值时写 | 查询本身为读取，但脚本会将返回 PID 写回 boot 文件，再执行 composition；这是初始化流程的真实写入 |
| `/sbin/usb_composition` | 读 hsusb/hsic；`persistent=y, hsic=n` 时写 | 写新 PID 到 boot 文件；若有 usb_qti 分区，flash_erase/nandwrite 同时持久写 MTD；`immediate=y` 可禁用 gadget、执行 USB 初始化和 composition |
| `/usr/bin/usb_composition_switch`，main `0x594` | 没有字面 boot 路径，间接调用上述脚本 | argv 前三个参数转为 `usb_composition %s %s %s y y`：强制 immediate=y、from_adbd=y；额外参数并未进入格式串。fork 后父、子路径均有 system 调用，不能假定只执行一次 |
| `/usr/bin/mbimd`，`0x3c2a8` | open flags=0，read 4 字节，close | 只读 next PID，作为 DIAG composition 配置输入 |
| mbimd，`0x3e950` / `0x3ea18` / `0x3ea28` | DIAG config 变化时格式化并 system 调用 usb_composition_switch | 传入 PID、hsic=n、persistent=y；经 wrapper 强制 immediate=y，具有 boot/MTD/USB 副作用，不是纯运行时 ADB 许可 API |
| `/usr/bin/network_mgr`，`0x2d78`、`0x987e` | get_file_content 读 8 字节，strncmp 与 9057 比较 | SIM/拨号策略的条件输入；这两处不直接写文件 |
| network_mgr，`0x4ad0` | system 执行 `usb_composition $(cat /etc/usb/boot_hsusb_comp) n y y n` | 读已有 PID后再次持久应用，并操作 bridge0 的 rndis0/ecm0；可能写 usb_qti MTD、改变网络；不是安全的 ADB 服务开关 |
| `/usr/bin/feiliu`，`0x1662` | get_file_content 读 8 字节、与 9057 比较 | SIM/飞流策略；没有在此引用附近写 boot 文件 |
| `/usr/lib/libshared.so`，`switch_sim` `0x91dc`、引用 `0x9296` | 读并与 9057 比较 | SIM 切换策略，随后可有 SIM/NV/ubus 操作；boot 文件在此仅作为条件，不是写目标 |

上述写入者不直接指向 adbd 的隐藏 BSS 地址。只有 boot 文件内容后来被 adbd 读取匹配，才能间接影响许可；runtime PID、本文件值、modem 保存的 USB PID 可能不同。

## systemrw、复制和 bind-mount

`/sbin/mount-copybind` 的文件分支：先确认目标存在；若 `/proc/mounts` 已绑定则返回；若 source 不存在，mkdir 后把原目标用 `cp -P --preserve=all` 复制到 source；最后 bind source 到目标。它通常保留既有内容，但会创建持久 source；不直接选择新 PID。

| 调用者 | source → target | 条件 / 生命周期 |
| --- | --- | --- |
| systemrw-boot_hsusb_comp.service | `/systemrw/boot_hsusb_comp` → `/etc/usb/boot_hsusb_comp` | ConditionPathIsReadWrite=/systemrw，目标存在且不是可写；oneshot/RemainAfterExit；ExecStop unbind |
| usb.service 的 ExecStartPre | 同上 | Requires=systemrw.mount，USB 服务启动时执行；已 active 的 RemainAfterExit USB 服务不会因 adbd restart 自动重新执行 |
| pcie.service 的 ExecStartPre | 同上 | PCIe 服务启动时先 bind，随后 start_pcie 有上述内容写入条件 |
| systemrw-adb_devid.service / usb.service | `/systemrw/adb_devid` → `/etc/adb_devid` | 解释 serial 元数据的持久性；与选择 USB PID不同 |
| systemrw-data-usb.service / usb.service.d/usb_data_service.conf | `/systemrw/data/usb` → `/etc/data/usb` | 覆盖 USB 配置目录；不是 boot 文件的另一套原始内容 |

`volatile-binds.postinst` 安装脚本 enable/restart bind units；prerm stop/disable。META-INF updater-script 声明 systemd wants symlink 和 boot 文件权限；没有把这些安装脚本或权限指令执行到设备。ZIP 中的普通文件/无 symlink mode 不代表安装后没有 symlink，安装脚本会重建它们。

## 其他字面命中与相关线索

根 file_contexts、两份 SELinux 文本 file_contexts 和 file_contexts.bin 属于标签规则；system-core-usb.list、volatile-binds.list 属于包清单；均不据此判为内容写入者。20 个命中文件的全部路径在 CSV，没有遗漏 ELF 四个额外使用者。

另查 `/oemdata/alk_nv/net_adb`：libshared 的导出 `nvram_restore_default` (`0xb238`) 在 nvram_commit 后于 `0xb358` unlink 这个文件。**这只证明恢复出厂会删除它**，没有找到创建/读取它即可设置 `0x8044` 的数据流。不创建它、不改 NV，也不把文件名当成官方开关。

当前优先路线仍是厂商合法序列号授权的进程内 flag；composition 的 persistent=n 虽能避免脚本的直接持久分支，运行时 USB 切换依然可能断开 RNDIS，且后续服务可能重新持久应用配置。它不满足本轮“不修改设备”的范围，也不作为下一步推荐。
