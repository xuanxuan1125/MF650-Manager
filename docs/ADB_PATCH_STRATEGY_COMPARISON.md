# MF650 ADB 三方策略与风险比较

2026-10-03（本轮分析始于2026-10-02）。**厂商 B 比现有 C 更宽松，不验证 90DB→9057 literal 的等价性。推荐继续保留已存在的限定 token 设计；本轮不生成或安装新包。** 完整字节、指令、安装条件及未知项见 [ADB_VENDOR_PATCH_REVERSE.md](ADB_VENDOR_PATCH_REVERSE.md)。

表中允许指 flag=0 且 boot helper 正常返回时的自动服务许可，不代表在线 UID、transport 认证或服务成功。“改变 root/auth/7628/USB”专指 adbd 本身，不包括安装器其他操作。

| 策略 | 修改 bytes | 修改逻辑 | 90DB | 9057 | 9059 | 序列号授权 | root 代码 | 标准 RSA auth | TCP 7628 | USB 代码/副作用 | 风险 |
| --- | ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A 原 FOTA | 0 | boot token=90DB/9059 或已有授权 flag | 自动允许 | 不自动允许 | 自动允许 | 保留；非允许 token时仍可进入 | 基线 | 基线，依赖匹配库 | 基线默认 | 基线 | 不解决 9057 自动许可；在线身份仍未知 |
| C 既有 literal patch | **2** | 90DB→9057，保留9059；CMP/BNE及全部 flag 消费者不变 | 不再自动允许 | 自动允许 | 自动允许 | 代码保留；非允许 token时仍可进入 | 不变 | 不变 | 不变 | 不变；不改 boot/USB 文件 | 范围较窄，但移除90DB自动许可；安装/恢复风险仍未验证 |
| B 厂商版 | **37总计，2功能＋35元数据** | BNE→MOVS r0,#1，正常服务前置无条件写 flag=1 | 自动允许 | 自动允许 | 自动允许 | 代码保留，正常 shell 路径 **BYPASSED** | 不变；root服务可达性扩大 | 不变；service授权放宽不等于RSA关闭 | 不变 | adbd代码不变；原安装器另有902D/9059 AT切换 | 放行任意正常boot token，扩大已有shell/SYNC/写服务能力；不推荐永久无条件许可 |

三份仍保留消费者检查：create_service_thread 的 flag=0 失败分支和 shell handler 的 flag=0 失败分支。B 让这些检查收到 1，C 仍只在指定 token/合法授权路径提供许可。已有 flag 不因客户端断开或不匹配 token自动清零；进程退出丢失 BSS 许可。表中的 root/auth“不变”不表示安全性相同，B 的服务访问范围确实更广。

## 最小改动分类

| 厂商差异 | 分类 | 是否需要复制到现有 C |
| --- | --- | --- |
| 0x3580 BNE→MOVS | REQUIRED_FOR_ADB（仅针对复现B的无条件机制）；同时 RISKY | **NO**；当前目标只需9057条件许可，已有C更窄 |
| GNU build-id | UNRELATED 元数据 | NO；不使用 build-id冒充新文件身份 |
| revision字符串 | UNRELATED 展示元数据 | NO |
| .gnu_debuglink CRC | UNRELATED 调试元数据，原debug文件未取得 | NO |

没有发现必须新增 USB 切换、root/auth 修改、TCP改端口或删除授权算法才能复现 B 的机制。B 的单条 setter 指令足以解释正常 service_to_fd 中的许可变化，但源码制作工艺和在线适用性不能由此确认。

**对现有2-byte思路的验证：PARTIAL。** 核实了同一 gate/flag 是厂商改动的焦点。`VENDOR_CONFIRMS_9057_GATE_PATCH=NO`，因为 B 没有新增9057 literal/比较，而是绕过结果限制。策略比较分类 **C_MORE_PERMISSIVE**。

## 设计决定与执行限制

保留当前 C 的设计和既有产物，不新增 branch patch、第三条件或安装 ZIP，也不把 B 复制为补丁。若以后需求变成必须同时保留90DB自动许可，那是不同的设计范围，需要单独评估；本轮没有实现它。

当前部署结论 **NO**：在线adbd SHA256、boot token、匹配运行库、可靠恢复入口与安装方式未确认。厂商安装器的 MD5 是“已安装B则跳过”的识别值，**不是FOTA原件的精确版本保护**，不能替代现有补丁的hash门槛。静态允许9057不等于当前设备可部署、可只读备份或可修RAM。

本轮仅离线数据分析。设备修改 NO、adbd执行 NO、新Enable ZIP NO、厂商安装 NO、USB切换 NO、9008 NO。原候选维持 PATCH_READY_OFFLINE / NOT INSTALLED，在线门槛保持不变。
