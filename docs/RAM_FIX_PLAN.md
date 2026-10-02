# MF650 RAM 修复准备方案

2026-10-02。**方案状态：数据条件未满足，未修改、未部署。** 本轮已找到真实 RAM JSON，但只有 `system_status.memory_usage=89.97`，没有 MemTotal/MemFree/MemAvailable。详见 [RAM_WEB_DISPLAY_FLOW.md](RAM_WEB_DISPLAY_FLOW.md)。

## 当前算法与问题

可确认的前端算法：每 2 秒 GET `/api/device/info`，`parseFloat(memory_usage) || 0`，限制到 0..100 后显示两位百分比。首页不计算内存数量；缺失值显示 0% 的行为会掩盖数据缺失。

此前证据支持后端 `Total-Free` 的旧口径，本轮没有当前后端源码或原始 meminfo，所以不能宣称已重新验证其公式。若确为旧口径，会把可回收内存计入使用量；只有 percentage 不能推导 MemAvailable。本轮也不能凭 89.97% 判断实际压力。

## 推荐修改（下一阶段条件方案）

优先利用当前 8081 已有设备信息服务取得原始内存数据；无需先刷 ADB Patch。先取得该服务的**官方源代码/维护说明或支持的只读内存导出**，确认数据读取方式、实际程序与 Web 路径、文件 hash/权限及部署恢复方式。现有源码没有额外内存查询路由，不猜新 URL，不用 AT、cron 或其他写接口代替文件读取。

取得这些条件后，最小方案是扩展现有 `/api/device/info` 的只读内存字段，并只调整 RAM 显示链：

| 层 | 建议 | 验证条件 |
| --- | --- | --- |
| 后台 | 解析真实 `/proc/meminfo`，明确输出 MemTotal、MemFree、MemAvailable 及 kB 单位；可另带缓存/Swap字段 | 与同次完整原文逐项相符，非从百分比或缓存猜算 |
| 公式 | effective_used=MemTotal-MemAvailable；usage=effective_used/MemTotal*100 | MemTotal>0，0<=MemAvailable<=MemTotal；数值及舍入有明确验证 |
| 对照 | 同时计算 legacy_used=MemTotal-MemFree 和 legacy_usage | 同一时刻数据，保留两个口径供验证 |
| 前端 | 有效数据才显示科学 RAM；缺失/异常显示 `--` | 不把缺失字段当作 0%，不通过 clamp 掩盖错误 |
| 交互 | RAM 圆环改为只读详情，移除清缓存写动作 | 点击不产生 POST，不改变设备内存管理状态 |

上表描述的是待实现字段和行为，**不是现有 API 已经具备的能力**。没有当前后端源码时，不修改 ELF、不替换未经核实的 daemon、不添加猜测服务。

## 可能修改的文件

- Web 来源：`http://192.168.100.1:8081/mf650.html` 与 `/html/mf650.html`；当前 body 完全相同。本地原文已保存于 `analysis/web8081/html/`。两个 URL 是否对应同一实际文件/嵌入资源仍 UNKNOWN，不能猜设备部署目录。
- 前端函数：updateDeviceInfo 的 RAM 分支、RAM 圆环及其只读详情。CPU/温度/网络等相邻逻辑没有本轮修改需求。
- 后台：当前处理 `/api/device/info` 的模块，实际文件路径、语言和可扩展性 UNKNOWN，先取得支持材料。不得直接把 FOTA feiliu ELF 当作当前 8081 服务。
- Android App：本轮未修改；获得真实字段合同后再评估需要的模型/展示改动，当前不生成 APK。

## 回滚方法与验收

部署前必须先获得原 Web/后台文件或官方可恢复版本，保存完整文件、SHA256、权限/属主及服务配置，确定恢复入口。回滚仅恢复本次修改的 RAM 页面/数据解析模块和原配置，并复核 hash、版本及原页面读取。不存在可靠备份/恢复入口时保持不部署。

正式验收应保存同次完整 meminfo 与 API JSON，验证旧/新两种百分比；测试缺失或异常字段显示 `--`，确认 RAM 点击只读、页面其他数据正常。只做获准的 Web/后台修改，不涉及 ADB gate、boot、NV、USB、systemrw 或分区。

本轮没有可部署补丁、设备 rollback 脚本或新 RAM 百分比。下一步是补齐受支持的 MemAvailable 数据来源，之后再准备最小改动及可审查回滚。
