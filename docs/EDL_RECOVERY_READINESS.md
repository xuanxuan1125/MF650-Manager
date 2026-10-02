# MF650 EDL / 9008 离线恢复准备度

2026-10-02。结论：**完整 EDL 恢复 NOT READY**。用户报告高级后台可请求 Qualcomm 9008，是恢复入口候选；本轮没有调用、进入或测试这个模式，不能把入口存在等同于恢复链可用。

检查范围为 `C:\Users\fqxku\Desktop\MF650` 项目树（包含忽略文件，以大小写无关 glob 搜索）及原 MF650_2.3_Fota.zip 全部 5,006 entries。

| 所需素材 | 项目/FOTA 结果 |
| --- | --- |
| prog_firehose*.mbn | 未找到 |
| prog_emmc_firehose* | 未找到 |
| prog_ufs_firehose* | 未找到 |
| rawprogram*.xml | 未找到 |
| patch*.xml | 未找到 |
| partition.xml | 未找到 |

还以 `*firehose*` 扩展检索，未发现其他 programmer。FOTA 中的 boot.img、uefi.elf、文件式 system 和 recovery-from-boot.p 不构成完整 EDL 刷机包；updater 的 UBI/MTD 支持也不能代替匹配 programmer、分区描述及完整恢复镜像。不能为 SDX55 臆造通用 eMMC/UFS 分区布局。

尚未验证：匹配本板/存储的已授权 programmer，Sahara/firehose 接受条件，实际分区及镜像对应，Windows 9008 驱动与独立连接，完整备份，以及失去 RNDIS 后可执行的恢复流程。当前 Rollback ZIP 只处理精确版本的 adbd，不能替代 EDL；部分写入/未知 hash 会触发它的版本保护并 ABORT。

本轮未运行 QFIL、QSaharaServer、emmcdl、firehose write；未刷任何分区，也未测试切换 9008。后续只在用户另行明确授权和素材匹配复核后评估恢复，不能声称“能切 9008，所以绝对不会变砖”。
