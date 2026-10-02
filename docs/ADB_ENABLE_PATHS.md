# ADB 服务许可启用路径与下一步条件方案

日期：2026-10-02。本轮未执行任何设备配置修改、授权请求、重启、USB 切换、AT 或 2358 RPC。仅刷新已知页面 GET；RAM 修复仍暂停。结论依据参考 FOTA、前阶段协议结果及本轮页面源码，不把在线 PID 等同 boot 文件值或在线 binary hash。

## 路线分类

| 路线 | 是否找到 | 许可作用 / 风险 / 建议 |
| --- | --- | --- |
| A：厂商 Web ADB 开关 | NO | 63 ASP/52 handler 和当前 80/8081 页面未找到；通用 console 不是已验证的专用授权开关 |
| B：厂商 2358 ADB RPC | NO（参考表） | 恢复 35 注册项和 1 未注册描述项；没有 ADB/USB/许可专用 method；不盲发 |
| C：restart_adbd | YES（FOTA） | 已有 handler→systemctl→launch_adbd；满足 USB 已 active、脚本/override 已核实、serial 稳定时 **R1/可恢复**；只是清空/重启进程，解锁 **UNLIKELY**；本轮不执行 |
| D：运行时 USB composition | YES（脚本能力） | 即使 persistent=n 也会改 gadget，可能断 RNDIS；高风险，恢复渠道未建立；不推荐 |
| E：修改 boot_hsusb_comp | YES（静态逻辑） | 9059/90DB 可影响 flag，但可写 systemrw，部分调用还写 usb_qti MTD；持久高风险；禁止本轮执行 |
| F：patch adbd / 绕过检查 | 禁止 | 不修改二进制、branch、凭据，也不探索异常读取失败绕过 |
| 厂商 ADB 序列号授权分支 | YES（ELF 控制流） | 两个序列号来源共用 `0x38f8`，只设置进程内许可并写临时文件；**非持久路线优先**；厂商合法凭据、客户端和撤销流程未找到，尚不是可立即执行方案 |

最安全的推进方向是取得厂商已有维护客户端/正式操作说明及其合法凭据，验证这条临时授权路线。不是自行计算授权码，也不是仅为了试验调用 restart_adbd。详情：[授权机制](ADB_VENDOR_AUTH_REVERSE.md)、[Web](WEB_ADB_INTERFACE_AUDIT.md)、[RPC](TCP2358_RPC_REVERSE.md)、[boot 写入者](BOOT_USB_COMP_WRITERS.md)。

## 当前拒绝服务原因的证据强度

| 候选 | 证据强度 | 能确认和不能确认的部分 |
| --- | --- | --- |
| A：boot 文件不等于 9059/90DB | 中等、间接 | runtime PID=9057 与参考拒绝模式一致，但没有实际读取 boot 文件；PID=9057 不证明文件=9057 |
| B：厂商许可 flag 未设置 | 最符合现有模式；在线条件推断 | 参考 FOTA 的服务创建/shell 均查 flag；CNXN/device 成功而 shell/SYNC/tcpip OPEN 被关，与 flag=0 相符；未读进程内存，不能宣称在线已证实 |
| C：在线 adbd/library/启动配置与 FOTA 不同 | 未排除 | 尚无在线 hash；当前 Web 有后加资源，不能从页面外观推定 daemon 相同 |
| D：SELinux、权限、服务错误或其他条件 | 较弱、未排除 | 缺少实际 errno/daemon 日志；transport 可用不代表这些条件全成立 |

前阶段 tcpip 5555 仅测试一次且返回 error: closed，5555 超时。配套 FOTA property_set 是空实现；重启不能凭空产生 5555 配置。当前没有理由重试 5555、adb usb、shell id、SYNC 或 ttyd Ctrl+C。

## 一个明确、可回滚的后续方案：厂商临时授权验证

**本轮只生成方案，不执行。下面的前置条件尚未满足；缺任一项都保持停止，不把“可能有用”当作许可。**

1. 执行前，取得厂商维护客户端/说明和合法发放的当前设备授权，不记录或公开实际凭据。由既有备份、厂商受支持的只读导出或正式材料确认在线 adbd/libcutils/launch_adbd/unit/override 的版本，并确认当前 boot 文件不是 9059/90DB。不能先 GET restart_adbd 来验证页面存在。
2. 确认撤销方式：优先使用厂商提供的 revoke；若只能 restart adbd，则须确认 USB 已 active、启动链只重放稳定 serial 元数据、无未知 override/自动授权来源，并有同一已验证管理入口恢复 daemon。没有这些证据，不开始临时授权。记录 RNDIS PID/接口/IP/网关及可用管理连接作为基线。
3. 通过**厂商原有客户端和合法流程**只授权一次。预期是现有 transport 上 shell/SYNC 获得服务许可，USB PID 和 RNDIS 配置保持原状；第一条授权服务未必返回交互 shell。若工具实际要求改 NV/boot、切 USB 或整机重启，则本方案不适用，停止。
4. 授权明确成功后，单次只读确认 shell 的 `id`；root 身份必须真实输出，不从 FOTA 推断。明确 uid=0 后读取 `/proc/meminfo`、`/proc/swaps`、free、pwd，保存完整输出。用 SYNC 读取一个已有小型常规文件（如 `/etc/launch_adbd`），确认 DATA/DONE 和本地 hash；不 push、root、remount、exec 写命令。失败即停止，不枚举其他服务名。
5. 回滚：使用预先确认的厂商撤销功能，或在已核实条件下重启 **adbd 进程**以清掉 BSS 许可；设备整机重启不作为回滚步骤。单纯 disconnect 不清 flag。确认授权已撤销、daemon transport 仍可用、RNDIS/PID/IP/网关与基线一致；必要的服务拒绝验证只作一次，留完整结果。

参考正常序列号授权本身不要求 USB 重枚举或整机重启；进程重启可能短暂断开 ADB transport。在线环境未核实前不能保证这些效果或回滚。取得真实 meminfo 后另开 RAM 修复评估，仍需实际 Web/后台备份与元数据；不会因本轮出现线索而部署 RAM patch。

## 本轮动作与交付

设备控制请求 0；5555/USB/root/remount/push/NV/boot/systemrw/MTD/ttyd/整机 reboot/kill 均未执行。没有授权生成器、密钥材料、固件二进制或原始设备页面上传到仓库。样本 hash、两处写入、ASP/RPC 覆盖及只读页面请求清单保存在 [审计 manifest](../fota-analysis/reports/ADB_AUTH_AUDIT_MANIFEST.json) 和相关 CSV。
