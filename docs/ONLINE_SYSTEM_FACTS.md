# MF650 当前在线系统事实

采集时间：2026-10-02 16:21:52–16:24:05，Asia/Shanghai；目标 `http://192.168.100.1/`。本文件区分 FACT（实测/源码可见）、INFERENCE（推断）和 UNKNOWN（未取得）。当前系统文件未读到，不以旧 FOTA 内容填入在线值。

## FACT：本轮在线结果

| 项目 | 本轮结果 | 来源/边界 |
| --- | --- | --- |
| 正常 Web 系统命令功能 | UNAVAILABLE | 在已取得系统/设置/日志页面、相关 JS 和导航范围内未发现真实提交链 |
| Web 固件标签 | MF650_V3.4 | 升级页面文本，不能替代二进制 hash |
| Web 构建标签 | MF650_V3.4,2025-07-26,16:41:00 | 升级页面文本，不能替代 uname |
| Web 型号标签 | MF650 | 页面文本 |
| 当前 USB VID / PID | 05C6 / 9057 | Windows `Get-PnpDevice -PresentOnly`，16:24:05 |
| USB 类别 / 状态 | Net / OK | Remote NDIS Compatible Device #11；未公开完整 InstanceId |
| HTTP 请求 | 9 GET / 0 POST | 全部 200；无命令/上传/配置请求 |
| `id` | NOT EXECUTED | 正常命令入口未确认，停止 |
| Patch 安装 / 持久修改操作 | NO / NO | 未提交安装或写入/配置操作 |

UNAVAILABLE 的范围是已检查的正常页面功能，不声称证明服务器所有未公开路由均不存在。没有尝试隐藏接口、猜测参数或继续扫描。

## FACT：实际页面表单

| 页面 | 源码中的表单 | 功能判断 |
| --- | --- | --- |
| Advanced_System_Content.asp | POST `/start_apply.htm`；name=form，id=ruleForm，target=hidden_frame | 管理设置，不是命令提交 |
| Advanced_Settings_Content.asp | POST `/start_apply.htm`；action_script=sys_settings | 拨号/网络模式/SIM/APN 等设置，不是命令提交 |
| Main_LogStatus_Content.asp | POST `apply.cgi`；clearLog 设置 action_mode=` ClearLog ` | 日志操作，不是命令提交 |
| Advanced_System_Info.asp | 无 form | CPU/RAM 图表，不是命令提交 |
| Advanced_FirmwareUpgrade_Content.asp | POST `upgrade.cgi`；multipart/form-data；name=form，target=hidden_frame；file 字段名=file | 固件上传入口，未提交 |

系统页真实 hidden fields：`current_page=Advanced_System_Content.asp`、`next_page=""`、`next_host=""`、`sid_list=LANHostConfig;General;Storage;`、`group_id=""`、`action_mode=""`、`action_script=""`、`preferred_lang=CN`。相关设置动作是 ` Apply `、` ApplyApn `、` ApplyLang `，未提交。管理凭据字段值已从报告排除。

设置页包含 `current_page=Advanced_Settings_Content.asp`、`sid_list=LANHostConfig;General;Storage;`、`action_script=sys_settings`、`sys_settings_mode=""`。日志页包含 `current_page=Main_LogStatus_Content.asp`、空 `next_page/next_host/sid_list/group_id/action_mode/action_script`、`preferred_lang=CN`；日志 textarea 为 readonly，本次返回内容为空（0 字符），不能当作执行命令的输出。

升级页 hidden fields 为 `current_page=Advanced_FirmwareUpgrade_Content.asp`、`next_page=""`、`action_mode=""`、`preferred_lang=CN`；**没有 sid_list**。`fwUpload()` 检查 file 非空后提交，`initial()` 调用 checkSERIALNO 并检查 login_safe。本轮只读取源码，未填写文件或提交表单。

`state.js` 的当前系统导航列出 SystemContent、FirmwareUpgrade、SettingBackup、SystemAutoSleep，相关导航另含本轮检查的系统信息、设置、日志页。已检查相关 JS 未出现正常 SystemCmd/syscmd/console_response 命令链；`console.log` 是浏览器调试输出。

系统信息页使用 `ram.total / ram.used / ram.buffers / ram.cached`，没有返回完整 meminfo 或 MemAvailable。本轮页面 GET 没有取得 RAM 样本；停止后没有继续请求状态数据接口。

## UNKNOWN：系统身份、文件与挂载

| 项目 | 结果 |
| --- | --- |
| Web 命令 UID / GID / groups | UNKNOWN / UNKNOWN / UNKNOWN |
| kernel / uname / 运行系统身份 | UNKNOWN |
| 在线 `/sbin/adbd` SHA256 / MD5 | UNKNOWN / UNKNOWN |
| ONLINE_ADBD_MATCH | UNKNOWN |
| adbd 大小 / owner / group / mode / stat | UNKNOWN |
| adbd symlink / readlink / 实际路径 | UNKNOWN |
| adbd 进程 PID / 命令行 | UNKNOWN / UNKNOWN |
| `/lib/systemd/system/adbd.service` | UNKNOWN，未读取，不能标 NOT PRESENT |
| `/etc/launch_adbd` | UNKNOWN，未读取，不能标 NOT PRESENT |
| `/etc/usb/boot_hsusb_comp` 原始内容 / 元数据 | UNKNOWN / UNKNOWN |
| `/systemrw/boot_hsusb_comp` 存在性 / 原始内容 | UNKNOWN / UNKNOWN，不能标 NOT PRESENT |
| `/` mount source / filesystem / RO-RW | UNKNOWN / UNKNOWN / UNKNOWN |
| `/system` mount source / filesystem / RO-RW | UNKNOWN / UNKNOWN / UNKNOWN |
| `/systemrw` mount source / filesystem / RO-RW | UNKNOWN / UNKNOWN / UNKNOWN |
| 当前 recovery 版本 / 签名策略 / 分区映射 / 工具依赖 | UNKNOWN |

以上未知项不是在线读取失败后的文件不存在结论；这些系统命令根本没有执行。FOTA adbd 的参考 SHA256 是 `323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185`，不填作在线值。

## UNKNOWN：RAM / Swap

完整 `/proc/meminfo`、`/proc/swaps`、`free` 均 **NOT OBTAINED**。

| 字段/指标 | 当前值 |
| --- | --- |
| MemTotal / MemFree / MemAvailable | UNKNOWN / UNKNOWN / UNKNOWN |
| Buffers / Cached / SReclaimable | UNKNOWN / UNKNOWN / UNKNOWN |
| Slab / Shmem | UNKNOWN / UNKNOWN |
| SwapTotal / SwapFree / swap 列表 | UNKNOWN / UNKNOWN / UNKNOWN |
| 旧 Web RAM（Total-Free） | UNKNOWN |
| 新 RAM（Total-MemAvailable） | UNKNOWN |

历史 RAM 89.35% 不代表本轮采样。缺少 MemAvailable 时不按缓存字段猜算，也不声称已完成 RAM 口径验证。

## INFERENCE：版本及补丁匹配

页面标注 V3.4，而参考包名为 MF650_2.3_Fota.zip，提示在线组件可能不同，实际是否不同仍需 hash 确认。USB PID=9057 不证明 boot token=9057。故 **PATCH_LOGIC_MATCH=UNKNOWN（Case D）**、Recovery Installability=UNVERIFIED、Patch Precheck=FAIL。详见 [在线预检报告](ADB_PATCH_ONLINE_PREFLIGHT.md)。

## FACT：HTTP GET 证据清单

以下路径均相对于 `http://192.168.100.1/`，无 query 参数，method=GET、status=200。请求使用 `User-Agent: MF650-readonly-preflight`，未附 Authorization 或 Cookie，未跟随重定向。时间来自采集端，SHA256 是保存的完整响应 body 的摘要。

| 路径 | 时间（+08:00） | bytes | body SHA256 |
| --- | --- | --- | --- |
| Advanced_System_Content.asp | 16:21:52.751134 | 15595 | `863c59559c6c595f317cbe61cb644e4350b63cb98066b540911419e1a00ab5ce` |
| Advanced_FirmwareUpgrade_Content.asp | 16:21:52.819252 | 9879 | `ccbe64ac2fc78a40f85b4a2d6441117bf6cc2218d0606a2601ff37baeab13df6` |
| state.js | 16:22:48.516052 | 63949 | `2f82d118dc4e06cffaddd337a33f8d3e22eaee308793cd86556f68db89af008b` |
| general.js | 16:22:48.645872 | 42376 | `d404a5debbd2207ede77646dc51817d33acc8fc6766ea76d09dbe589fc136a20` |
| help.js | 16:22:48.739818 | 16796 | `5fcbdf8c800f00baf3fe2040d19da6fd20ba48af9d00b281d047aac39ad4dd2b` |
| popup.js | 16:22:48.811559 | 3321 | `11b8e718e1d8d6ac95e0761b4440ca4d54e81e94f7d9c9d10a102183dc15886a` |
| Advanced_System_Info.asp | 16:24:05.280152 | 11511 | `367c6141bf324c8d250be9c92dff3d283b2705dae7569ae971c15b077d864064` |
| Advanced_Settings_Content.asp | 16:24:05.355364 | 16922 | `523d2fd31e2394fd82a25602d96e26da27bb6c52f4670cb96ddadba4f5743b19` |
| Main_LogStatus_Content.asp | 16:24:05.441713 | 7529 | `ab4ba607c94dd7c5bc7ac82860013c37173b61769758e5fc5840784b06ff85c7` |

本地原始证据目录：`test-results/adb-patch-online-preflight/`，由 .gitignore 排除。含响应 body/headers、脱敏解析元数据、HTTP_GET_MANIFEST.csv、usb-pnp.json、PREFLIGHT_RESULT.json。系统页原文含管理凭据，不公开原文或完整 USB InstanceId；本文仅保留来源、表单结构和文件摘要。未生成冒充在线读取的 meminfo、boot token 或 id 输出文件。
