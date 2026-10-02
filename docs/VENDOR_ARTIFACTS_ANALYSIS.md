# MF650 厂商附件离线取证结果

2026-10-02。基线提交：`1248459c96e8d69716e39a0d283e72a94eb6a073`。

**未找到可归属于当前 v5.2.4 的 `/api/device/info` 后端。RAM 修复为 NOT_READY。** 三个附件已完整解压、核对 CRC 与 hash；包内另有原生 ARM 后台、安装脚本和历史 Web 资源。它们只能作为不同实现的参考，不能替代当前设备的进程与程序身份。

本阶段仅读取用户提供的本机 ZIP 和以前缓存的页面。设备请求、设备命令、设备修改均为 0。未执行任何附件内程序或脚本；没有扫描端口、ADB/SYNC/ttyd 访问、更新接口调用或重复 FOTA 全量搜索。ADB Patch 保留 `PATCH_READY_OFFLINE / NOT INSTALLED`，9008 未使用。

## 原件与完整清单

原件位于本机 `analysis/vendor-artifacts/original/`，与 `vendor-artifacts/` 逐字节一致，复制时保留文件修改时间；已有不同内容的原件会触发停止，不覆盖。三个工作目录分别为 `backup-tool/`、`advanced-backend/`、`network-fix/`；高级后台的嵌套 `ALKMF650.zip` 解压到 `extracted/advanced-backend/ALKMF650/`。

| ZIP | 字节数 | SHA256 | MD5 | 整包 CRC32 | ZIP 成员 CRC |
| --- | ---: | --- | --- | --- | --- |
| web后台备份工具.zip | 2,813,276 | `64606f0aa44f73b7443f5f3c12fb79342178f2686f2a86500781f2cb37804ee2` | `9ce4a556a7afbc4f434d71a1cef46121` | `095523cf` | PASS |
| 阿乐卡MF650高级后台.zip | 4,008,852 | `4d51e0682c8585fc4fe18e6f79f134753948ec1a38131303db965f7463b9b91b` | `e56a94b77cf2a8e9020f73d0214a00a5` | `4a81f32f` | PASS |
| 高级后台断网修复包-1.zip | 4,530,501 | `a18d097fb331565543b4a61385b55711623678d93d19e402db4f0465bc8925ea` | `757797ea4778d68f8e11b16ca26076bd` | `190552c6` | PASS |

文件系统修改时间以 UTC 记录在 [VENDOR_ARTIFACTS.csv](../analysis/vendor-artifacts/reports/VENDOR_ARTIFACTS.csv)，不是厂商发布时间。ZIP 内时间为未注明时区的原始元组。整包 CRC32 与各成员 CRC32 分开记录。

[VENDOR_PACKAGE_MANIFEST.csv](../analysis/vendor-artifacts/reports/VENDOR_PACKAGE_MANIFEST.csv) 共 **372 个非目录文件**：备份工具 4、高级后台 104（外层 5、内层 99）、断网修复包 264。包含文件路径、字节数、SHA256、类型、扩展名、可执行格式/脚本标记、文本/二进制分类、成员 CRC 和时间。`executable` 指格式或脚本识别，不能表示已执行或已授予权限；DLL/无后缀数据也保留在清单内。

[PACKAGE_OVERLAP.csv](../analysis/vendor-artifacts/reports/PACKAGE_OVERLAP.csv) 有 **76 组跨顶层 ZIP 的相同 SHA256**，包括同一套 PC ADB 组件和旧 UI 资源；不是 76 个已验证当前后端。高级后台的 `webservers` 与修复包的 `main` hash、启动参数和 API 均不同。

## 当前页面、版本与来源关系

只比较已有缓存，没有再次请求设备。三个指定文件的比较为 9 行，全部 `NOT_PRESENT / same=False`：

| 当前缓存路径 | 当前 SHA256 | 包内同名文件 |
| --- | --- | --- |
| mf650.html | `bc4a01ccbb817ee801c7a16c32e18385b9aa0dc34e75e8f53182c6361b2fbbc8` | 三包均无 |
| system.html | `8b3721397ad8682f2483fef7129097e54b5fb59c969442fa753516892c3ae204` | 三包均无 |
| at_debug.html | `7b7f1d4253f857f099d210e0b2f72b6b00bd287e8eff180d5b3e2616b2eaf600` | 三包均无 |

全部缓存页面内容 hash 与全部 372 个文件比较也为 **0 个相同内容**。缺失不是“同名文件内容不同”；见 [CURRENT_VS_PACKAGE_WEB.csv](../analysis/vendor-artifacts/reports/CURRENT_VS_PACKAGE_WEB.csv)。本轮没有重新分析 HTML/JS/CSS 调用链。

| 对象 | 分类 | 解释 |
| --- | --- | --- |
| 阿乐卡高级后台包 | **UNRELATED**（当前 REST 实现的分类）；交付摘要 **REFERENCE** | 包含另一套 `/api/sysinfo`、`mem_usage` 和旧 ASP UI。与现机来源关系仍 UNKNOWN，不能判定同版本或同构建 |
| 断网修复包 | **UNRELATED**（当前 REST 实现的分类） | `main` 默认 8152，AT/锁频接口；旧 ASP UI 不匹配当前页面 |
| 备份工具 | 后台版本 N/A | 只有 PC ADB 组件与读取 `/www` 的 BAT，不含后台制品 |

原始文件及恢复的 ELF 装载段均未命中 `v5.2.4` 或 `5.2.4`。高级后台安装提示 `20250830` 是安装横幅，不是后台语义版本；修复包 `www/webuibd` 的 `webuibb=v3.0 / jiaobenbb=v3.0` 是包内 UI/脚本标签。高级后台 `kasb_poweroff.sh` 第 19–23 行还主动写入 `MF650_V3.5` 等 NVRAM 版本字符串，不能把这种展示值用于构建身份判断。没有足够依据划为 REFERENCE_OLDER_VERSION 或 REFERENCE_NEWER_VERSION。

## 8081 与 RAM 结论

| 问题 | 本轮结果 |
| --- | --- |
| 当前 8081 可执行文件、SHA256、PID、安装路径、语言 | UNKNOWN；未取得当前文件或进程导出 |
| 包内 8081 证据 | `kasb_cgi.service` 声明 `busybox httpd -p 8081 -h /data/kasb/www`；没有所需 BusyBox 后端 binary，也没有当前路由源码；服务启动是否成功 UNKNOWN |
| 包内原生 Web 服务 | 高级后台 `bin/webservers` 默认 6391；修复包 `main` 默认 8152；均为静态证据，不是当前在线监听状态 |
| `/api/device/info`、`memory_usage` | 原始文件与恢复装载段均 NOT_FOUND；不能证明当前二进制中绝对不存在经过动态拼接的字符串 |
| `system_status`、`cpu_usage`、`mem_usage` | 旧 UI/参考实现有命中；不能单靠字段相似确认当前 handler |
| 当前公式及 MemAvailable 是否使用 | UNKNOWN；没有 CURRENT_TOTAL_FREE_CONFIRMED |
| RAM 修复 | NOT_READY；未生成当前 handler 报告、binary diff 或部署包，未修改 RAM_FIX_PLAN |

完整记录见 [VENDOR_BACKEND_REVERSE.md](VENDOR_BACKEND_REVERSE.md)。旧 FOTA 的 Total-Free 证据仍只适用于旧路径；未因为参考包出现 `mem_usage` 就推断当前 `system_status.memory_usage` 的来源。

## 备份工具和修复包

备份 BAT 全部设备动作只有第 18 行 `adb pull /www .\`，远端命令风险为 **R0**，不创建临时 tar。整个工具包安全分类为 **UNKNOWN**：随包原生 adb.exe 没有完成来源可信度及所有内部行为验证；现机读取入口、权限与 `/www` 对当前后台的覆盖也没有确认。能否完整导出当前后台为 **UNKNOWN**。见 [VENDOR_BACKUP_TOOL_AUDIT.md](VENDOR_BACKUP_TOOL_AUDIT.md)。

修复包 BAT 会 remount、push、chmod、启动及启用服务；`main` 包含 AT 和锁频写动作。高级后台内层安装脚本还包含替换 `/sbin/adbd`、USB 切换、服务变更，以及随服务脚本的 NVRAM/重启/关机动作。不是本阶段可执行的只读工具。没有发现经过验证的当前设备原件或完整回滚材料。见 [VENDOR_NETWORK_FIX_AUDIT.md](VENDOR_NETWORK_FIX_AUDIT.md)。

## 分析方法、复现与限制

自身编写的 [analyze_mf650_vendor_artifacts.py](../tools/analyze_mf650_vendor_artifacts.py) 仅做 ZIP 安全路径检查、解压、hash、CRC、格式识别和 UTF-8/UTF-16LE/UTF-16BE 字面量搜索，不加载目标代码。共 10,868 条原始命中保留在本机忽略的 `test-results/vendor-artifacts/ALL_KEYWORD_OFFSETS.csv`；公开 [KEYWORD_OFFSETS.csv](../analysis/vendor-artifacts/reports/KEYWORD_OFFSETS.csv) 是 752 条核心命中，不附原始页面、脚本或凭据上下文。计数包括外层嵌套 ZIP 字节和内层文件，不是去重后的代码引用数。原始二进制偶然数字也算命中，不能当端口证据：ADB 中 `8081 @0x4ca8d8` 实际位于连续数字格式化表。

[decode_mf650_vendor_blocks.py](../tools/decode_mf650_vendor_blocks.py) 以固定输入 SHA256 为前提，纯字节解码 `forwards/webservers` 中 6 个 NRV2B 数据块，恢复 4 个 PT_LOAD 装载段。未使用目标解压桩、模拟器、动态库加载或目标程序执行。缺失非装载段/section table，派生镜像只供静态阅读；原件不变，不是可安装的完整解壳 binary。记录见 [PACKED_ELF_RECOVERY.json](../analysis/vendor-artifacts/reports/PACKED_ELF_RECOVERY.json)。

[summarize_mf650_vendor_artifacts.py](../tools/summarize_mf650_vendor_artifacts.py) 用已有本机 pyelftools 读取 ELF 元数据、比较当前页面 hash、验证字节 offset 并生成结果。按 inventory → decode → summarize 顺序可复现；依赖前期保存的页面/source-index 以及本机 `fota-analysis/binaries/python-deps`，不自动下载依赖。GNU ARM objdump/readelf 仅读取目标文件，不运行目标文件。完整原始文件、派生 ELF、资源块与反汇编只保留本机忽略目录。

外层高级后台安装器是 UPX 压缩 PE64。已静态记录 PE 头、资源 hash、导入能力及压缩标记，但未完全恢复其程序代码。因此**不能宣称外层安装器的所有执行/网络行为已经穷尽**；本轮“未找到当前后端”是已取得证据范围内的结论。PC ADB 的能力字符串也不能证明 BAT 调用了这些能力。公开 [PE_METADATA.json](../analysis/vendor-artifacts/reports/PE_METADATA.json) 仅含元数据，不含资源内容。

下一阶段的 [BACKEND_BACKUP_EXECUTION_PLAN.md](BACKEND_BACKUP_EXECUTION_PLAN.md) 是 **CONDITIONAL_DRAFT / NOT_READY / NOT_EXECUTED**。自身重实现目前仅打印 DRY_RUN 计划，无执行入口。必须先证明安全只读入口与真实后端路径/覆盖，再由用户确认单独执行阶段；本阶段不尝试修复入口、修改设备或运行原工具。

## 离线验证

[OFFLINE_VERIFICATION.json](../analysis/vendor-artifacts/reports/OFFLINE_VERIFICATION.json) 记录 PASS：3 个原件及时间/CRC/hash、372 个源 ZIP 成员与解压文件、6 个压缩块、12 个精确字节证据、9 行页面比较、ZIP 越界/Windows 保留名/大小写冲突拒绝、截断压缩数据拒绝、计划器默认/显式 DRY_RUN 及 `--execute` 拒绝。公开结果仅为 UTF-8 文本，报告链接可定位。本阶段没有修改 Android 应用，不运行无关构建或设备测试。
