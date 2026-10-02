# MF650 8081 API 清单

2026-10-02。由当前保存的页面源码逐项恢复，功能列是客户端代码用途，不按 URL 名猜测。**36 条路径、44 个方法/路径组合**；15 条路径有查询用途，29 条路径有写入或执行能力，其中 8 条共享读写。查询用途不自动等于后端无副作用。

本轮实测 18 个只读 GET 变体、19 次 API 请求（一次补采样），覆盖 13 条 API 路径，全部 200；POST/PUT/DELETE 均为 0。没有访问源码未引用的 `/api/status` 或猜测维护 URL。

表中证据是原 HTML 的行号，所有路径均相对于 `http://192.168.100.1:8081`。表单 POST 使用 application/x-www-form-urlencoded（cell 的 URLSearchParams 由浏览器编码）；壁纸上传使用 multipart/form-data。写接口只记录，风险按动作评估；AT/IMEI/SIM/频段/升级/EDL 等均可能高风险，未调用。

| API | 方法 | 参数 | 功能（代码用途） | 是否读 | 是否写 | 代码证据 |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/device/info` | `GET` | 无 | 设备/CPU/RAM百分比/温度/网络/流量；updateDeviceInfo读取对应字段 | YES | NO | `mf650.html:747,818` |
| `/api/device/status` | `GET` | 无 | 充电、电池、USB、云控和版本状态；updateInterfaceStatus消费字段 | YES | NO | `mf650.html:789,819` |
| `/api/device/clear_cache` | `POST` | 无 | clearCache()点击RAM圆环清缓存 | NO | YES | `mf650.html:395,831` |
| `/api/device-status` | `GET` | 无 | 电源管理页加载电池/充电/自动充电/校准/低压状态 | YES | NO | `html/charge.html:761` |
| `/api/control-charge` | `POST` | mode=0或1 | controlCharge()启停充电 | NO | YES | `html/charge.html:955` |
| `/api/set-auto-charge` | `POST` | status=0或1 | controlAutoCharge()切换自动充电 | NO | YES | `html/charge.html:998` |
| `/api/battery-calibration` | `POST` | status=0或1 | 电池校准开关 | NO | YES | `html/charge.html:1041` |
| `/api/low-voltage-shutdown` | `POST` | status=0或1 | 低压关机功能开关 | NO | YES | `html/charge.html:1084` |
| `/api/set-charge-levels` | `POST` | min,max：硬件档位 | 保存自动充电阈值 | NO | YES | `html/charge.html:1133` |
| `/api/cell` | `GET` | ACTION=get_status / verify_activation；另有unlock_lte / unlock_nr | 前两项读状态/激活状态；后两项解除锁定，是写动作 | YES | YES | `html/cell.html:759,839,1100,1155` |
| `/api/cell` | `POST` | ACTION=activate,activation_code；lock_lte,LTE_ARFCN,LTE_PCI；lock_nr,NR_ARFCN,NR_PCI；get_nr_neighbors | 激活、锁LTE/NR；get_nr_neighbors为主动邻区扫描，副作用UNKNOWN，仍禁止POST | 查询意图 | YES | `html/cell.html:800,1070,1125,1199` |
| `/api/nr5g-band` | `GET` | 无 | loadBands()读取SA/NSA频段数组 | YES | NO | `html/nr5g_band.html:365` |
| `/api/nr5g-band` | `POST` | type=sa或nsa；sa_bands,nsa_bands=JSON数组 | applyBands()修改频段；前端处理可能断网 | NO | YES | `html/nr5g_band.html:406,434` |
| `/api/imei` | `GET` | query标志get-imei-info | fetchImeiInfo()读取当前/首个/历史IMEI | YES | NO | `html/imei.html:451` |
| `/api/imei` | `POST` | query标志reboot-device；clear-history+type；modify-imei+imei | 重启、清历史记录、改IMEI | NO | YES | `html/imei.html:495,535,567` |
| `/api/liuliang` | `GET` | query标志traffic-usage / liuliang-cx / feixing-cx | 读取流量统计、插件运行状态、飞行模式状态 | YES | NO | `html/liuliang.html:684,746,789` |
| `/api/liuliang` | `POST` | liuliang-kg；feixing-kg；set-traffic-limit+limit；set-traffic+type,value | 切换插件/飞行模式、设置限额、修改流量值 | NO | YES | `html/liuliang.html:773,821,838,868` |
| `/api/cron` | `GET` | ACTION=get | loadTaskList()加载定时任务列表 | YES | NO | `html/cron.html:955,957` |
| `/api/cron` | `POST` | ACTION=save,MODE=shutdown/reboot/traffic,TIME,DAYS；ACTION=delete,COMMAND | 添加定时关机/重启/流量上报；删除已有任务 | NO | YES | `html/cron.html:1068,1159,1296` |
| `/api/traffic-config` | `GET` | 无 | 读取流量上报token2/qudao2配置 | YES | NO | `html/cron.html:1211` |
| `/api/traffic-config` | `POST` | action=save,token2,qudao2 | 保存流量推送配置 | NO | YES | `html/cron.html:1265,1270` |
| `/api/forward` | `GET` | action=load / status / log | 读取短信转发配置、服务运行状态、转发日志 | YES | NO | `html/forward.html:292,308,469` |
| `/api/forward` | `POST` | action=start/stop/save/clear/clearlog；save含token,qudao | 启停短信转发服务、保存/清配置、清日志 | NO | YES | `html/forward.html:364,404,436,491` |
| `/api/photos/upload` | `POST` | multipart file | 上传高级屏幕壁纸 | NO | YES | `html/photos.html:714,737` |
| `/api/photos/upload/quanping` | `POST` | multipart file | 上传后台横屏背景 | NO | YES | `html/photos.html:720,737` |
| `/api/photos/upload/asus_logo` | `POST` | multipart file | 上传后台Logo | NO | YES | `html/photos.html:726,737` |
| `/api/photos/upload/shuping` | `POST` | multipart file | 上传后台竖排背景 | NO | YES | `html/photos.html:732,737` |
| `/api/photos/device-model` | `GET` | 无 | 读取deviceModel以选择壁纸预览数量/路径 | YES | NO | `html/photos.html:767` |
| `/api/photos/change-wallpaper` | `POST` | choice | 切换当前壁纸 | NO | YES | `html/photos.html:881` |
| `/api/system/info` | `GET` | 无 | 读取高级后台版本、SIM/屏幕/功能开关/激活状态 | YES | NO | `html/system.html:1616` |
| `/api/system/bigscreen` | `GET` | 无 | loadLCDStatus()读取激活、lcd_status、serial | YES | NO | `html/system.html:1746,2200` |
| `/api/system/bigscreen` | `POST` | action=turn_on或turn_off | toggleLCD()切换大屏；处理reboot_required | NO | YES | `html/system.html:1793,1796` |
| `/api/system/check-update` | `GET` | 无 | checkUpdate()检查更新；可能外部访问/缓存行为未知，未调用 | 查询意图 | UNKNOWN | `html/system.html:1837` |
| `/api/system/apply-update` | `POST` | 无 | 执行高级后台更新 | NO | YES | `html/system.html:1874` |
| `/api/system/switch-sim` | `POST` | type=simType | 切换SIM卡槽 | NO | YES | `html/system.html:1933` |
| `/api/system/toggle-lcd-screen` | `POST` | 无 | 切换LCD屏幕类型 | NO | YES | `html/system.html:1965` |
| `/api/system/toggle-sim` | `POST` | sim_mode=simMode | 切换开机SIM选择模式 | NO | YES | `html/system.html:1992` |
| `/api/system/toggle` | `POST` | feature=upgrade,enable,password；reverse_charge/reconnect/cloud/self_screen | 自动更新、反充、断网重连、云控、自研屏幕开关；部分处理reboot_required | NO | YES | `html/system.html:2038,2052,2065,2082,2098` |
| `/api/system/action` | `POST` | action=reboot/shutdown/restore_wallpaper/reboot_edl；EDL含password | 整机重启/关机、还原壁纸、9008入口；只记录，禁止调用 | NO | YES | `html/system.html:2114,2127,2144,2247` |
| `/api/system/activate` | `POST` | key | 高级后台激活 | NO | YES | `html/system.html:2163` |
| `/api/system/activate-lcd` | `POST` | lcd_key | 高级屏幕激活 | NO | YES | `html/system.html:2189` |
| `/api/system/bigscreen-activate` | `POST` | activation_code | 大屏幕激活 | NO | YES | `html/system.html:2222` |
| `/api/system/update-log` | `GET` | 无 | 读取升级变更日志log_content；本轮未调用 | YES | NO | `html/system.html:2266` |
| `/api/at-debug` | `POST` | atcmd | AT调试发送指令并显示output；浏览器拦截usbcfg | 按指令而定 | 可能 | `html/at_debug.html:361,408,448` |

## GET 不一定只读

`/api/cell?ACTION=unlock_lte` 与 `unlock_nr` 明确由 unlockLTE/unlockNR 使用，反馈文本为“锁定已解除”。**这是两个写动作，虽然 method=GET；本轮都没有请求。** `/api/system/check-update` 可能涉及外部查询/临时状态，按本轮不触发更新的边界排除；update-log 仅保留静态证据。POST get_nr_neighbors 是主动扫描，不因名称 get 就改成 GET 或调用。

## 覆盖范围

当前 12 个成功 HTML URL（首页两个别名内容相同）的 JS/CSS 均为内联：12 个 script 和 12 个 style 块，无外部 JS/CSS/JSON 文件引用。提取脚本先索引 50 个 fetch 调用点（包含首页别名重复）、3 个 xhr.open 和 wrapper/上传调用，再人工解析 API_URL、endpoint、url、模板字符串、query 标志及 action/feature 分支。`/api/photos` 只是前缀常量，不作为独立可调用路由；4 个 upload 子路径、device-model、change-wallpaper 均已列出。

源码和内联资源仅保存在本地忽略目录 `analysis/web8081/{html,js,css}`。公开索引见 [endpoint-inventory.json](../analysis/web8081/reports/endpoint-inventory.json)、[source-index.json](../analysis/web8081/reports/source-index.json)，只读采集见 [HTTP_GET_MANIFEST.csv](../analysis/web8081/reports/HTTP_GET_MANIFEST.csv) 和 [api-schemas.json](../analysis/web8081/reports/api-schemas.json)。词法索引不自动证明路由存在或完整后端注册表；动态分支已人工核对。

没有发现 `/api/service/restart`、restart_adbd、enable debug、文件系统或任意 shell API 的实际调用。AT debug 是已知 AT 功能；cron 的 COMMAND 用于删除已有任务，不能当作任意即时系统命令接口。完整结论见 [8081_ADVANCED_WEB_AUDIT_REPORT.md](8081_ADVANCED_WEB_AUDIT_REPORT.md)。
