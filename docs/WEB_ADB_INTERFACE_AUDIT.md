# FOTA ASP 与当前 80/8081 ADB 接口静态审计

日期：2026-10-02。没有访问 restart_adbd.asp、console_response.asp、serialno.asp 或提交表单、POST/AT/API 操作。当前 Web 只刷新此前资源清单中已经观察到的页面和静态资源；没有猜路径。

## FOTA 全量资源和 handler

`/www` 的 63 个 ASP 全部列于 [FOTA_ASP_FILES.csv](../fota-analysis/reports/FOTA_ASP_FILES.csv)。[HTTPD_ASP_FUNCTION_MAP.csv](../fota-analysis/reports/HTTPD_ASP_FUNCTION_MAP.csv) 有 160 行，覆盖每个 ASP→handler，列出 ASP 参数、offset、system、脚本、配置、写入、service restart、USB/ADB 操作及路径引用；无 ASP 调用的页面及仅注册未使用的 handler 也保留。

httpd `.data` VA `0x1a118..0x1a2b0` 为 **52 个** 8 字节注册项，`0x1a2b8` 为 0/0 终止项；不能把附近 MIME 表或指令数据当作额外 handler。52 项逐一在 [HTTPD_ASP_HANDLERS.csv](../fota-analysis/reports/HTTPD_ASP_HANDLERS.csv) 关联直达/内部调用、PLT 导入；49 项非空，wl_auth_list、wl_bssid_2g、wl_scan_2g 为 **NULL**，没有函数指针重定位提供实现。不能编造这些项的可执行功能。

反汇编调用图排除返回后的 literal pool 和 __stack_chk_fail/exit 等不返回路径后的伪指令。混合模式 veneer `0x10cf0/0x10d10/0x10d20` 对应 nvram_set/doSystem/nvram_set_int，已解析，不当作未知“隐藏 ADB”函数。动态参数和导入库的进一步服务行为有条件性；CSV 中 NO identified 表示未识别专用操作，不表示任意参数都无副作用。

全量 Web 搜索覆盖 `/www` 94 个 ASP/HTML/HTM/JS/dict/config/JSON 文件，以及 `/WEBSERVER` 19 个此类文本文件；[关键词清单](../fota-analysis/reports/FOTA_WEB_KEYWORDS.csv) 保留全部文件。宽匹配命中 11 个 `/www` 文件，WEBSERVER 未命中，逐项语义区分如下：

- restart_adbd.asp 的 `restart_adbd()` 对应 `0x8bd8`；仅 systemctl 重启 daemon，忽略退出状态后返回 ok，不设置许可。完整启动条件见 [重启分析](RESTART_ADBD_ANALYSIS.md)。
- serialno.asp 的 `get_serialno()` 对应 `0x9558`：只读 `/proc/cmdline`，匹配 androidboot.serialno= 后 +21 字节，`%8s` 输出；不派生摘要、不写 flag。页面存在不代表授权入口。
- 三个 Wireless 页面中的 `Debug (all channels)` 对应 rt_country_code/wl_country_code 国家码 DB，属于 Wi-Fi 信道设置。
- CN/EN 字典的 `menu5_13_debug` 文本没有在现有页面/JS 中被引用；不能仅凭“启用 Debug 模式”翻译确定 ADB 开关。
- USB_Mode 宽匹配实际多为 `USB_Modem` 的前缀及 WWAN 流量标签；factory 为恢复出厂文案；require-jquery 的 developer/factory 属于库代码。

未发现 adb_enable.asp、USB debug/factory/maintenance 启用页面，未发现 Web 向 ADB shell 参数提交合法授权的客户端代码。**厂商 Web ADB 专用开关：NO（本包内）。**

## 通用控制台的完整关联

console_response.asp 调用 `nvram_dump("syscmd.log","syscmd.sh")` → handler `0xaf84` → helper `0x8e38`。syscmd.sh 分支检查 httpd 存储的命令缓冲区与已有管理状态：空时 system 清空 `/tmp/syscmd.log`；可执行时设置 PATH，再于 `0x8ec2` doSystem 执行缓冲区内容并重定向到该 log，随后清空命令缓冲区。它不是直接读取一个已存在日志的纯只读端点。

其他 nvram_dump 参数选择日志/配置/脚本辅助路径。update_variables 有正常设置、AT、VPN、SMS、网络和 EDL 分支；notify_services 能通知 Wi-Fi 或整机重启；get_static_client/count 会生成临时终端列表；detect_internet 会 signal 检测进程。已在 Function Map 标注，未把全部 ASP GET 当成 R0。

通用管理命令可能间接操作 ADB/USB，但不是已验证的专用授权开关；不通过它写 flag、制造授权或改 boot 文件。get_usb_ports_info 的 USB 名称主要对应展示输出，未找到 PID/ADB 配置切换调用。

## 当前 Web：仅已知页面 GET

2026-10-02 06:50:34–06:50:35 UTC 重新请求此前清单中的 24 个 URL：**22 个 200，两个 8081 根入口 302，未跟随重定向**。公开 [CURRENT_WEB_ADB_AUDIT.csv](../fota-analysis/reports/CURRENT_WEB_ADB_AUDIT.csv) 保存 URL、时间、状态、字节数、SHA-256、关键词及字面 API；响应原文仅留在忽略目录 `test-results/adb-service-auth/web-current/`。

22 个响应中 21 个与前阶段 hash 相同，state.js hash 不同；该差异不证明固件升级。80 的 feiliu.asp 和 8081 的高级 HTML 页面不属于 FOTA `/www`，当前界面确实包含后加资源，不能把 FOTA 文件路径直接当成在线可调用入口。

当前 80 的 root/feiliu.asp/JS 未见 restart_adbd、ADB、9059 或 boot composition 开关；feiliu.asp 的 AT debug iframe 指向已知 8081 页面。当前 8081 页面未见 ADB/adbd/shell 专用控制。

system.html 中的 API 是页面实际引用的 system info、toggle、SIM、更新、activation、action 等；其中 action 的已见值包括 reboot/shutdown/restore_wallpaper/reboot_edl，toggle 为 upgrade/reverse_charge/reconnect/cloud/self_screen 等正常功能。activation 字段属于第三方后台/屏幕激活，未找到与 FOTA adbd 序列号摘要分支的关联，不把它称为 ADB license。

at_debug.html 的唯一接口为 `/api/at-debug`（POST），页面 `containsUSBcfg` 会拦截任何含 usbcfg 的输入，并显示敏感指令拦截提示。没有绕过这项检查，也没有发送 AT。其他页面的 USB 字样主要是充电/设备展示。

**8081 隐藏 ADB 控制：NO（已获得页面/静态 API 引用范围）。** 尚未取得在线后端程序或其完整路由表，不能据此证明服务器绝无未引用 API；没有猜测 URL 来补这个证据缺口。
