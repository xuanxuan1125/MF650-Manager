# PRE_USB_ADB_STATE（公开脱敏版）

日期：2026-10-02。本轮没有 USB 切换；此记录用于评估 USB 前置条件及核对一次 TCPIP 测试前后状态。

| 项目 | 测试前 | 测试后 |
| --- | --- | --- |
| 实际 VID / PID | 05C6 / 9057 | 05C6 / 9057 |
| 对应 PnP | Net / Remote NDIS Compatible Device / OK | 同一 instance，状态 OK |
| RNDIS IPv4 | PC 私有记录 | 与测试前一致 |
| 网关 | PC 私有记录 | 与测试前一致 |
| RNDIS 状态 | 管理连接可用 | Up |
| USB ADB 接口 | 未观察到 | 未观察到 |
| 9059 / 90DB 切换 | 未执行 | 未执行 |

完整 VID/PID、PnP instance ID、地址、网关保存在 PC `test-results/fota-adb-enable/PRE_USB_ADB_STATE.md` 及 pnp/network JSON，未提交 GitHub。初始网络快照的 PowerShell CIM JSON 深度提示已处理：从实际保留的 CimInstanceProperties Name/Value 取出 IPAddress/NextHop，保存 normalized 快照，与后置结构化字段比较，二者一致。

恢复能力：当前无可用 shell/SYNC，没有已验证的独立控制渠道在 USB 网络失效后恢复 9057。没有执行任何 USB composition 命令，未把重新插线/重启当作经过验证的恢复方案。
