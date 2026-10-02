# MF650 8081 更新通道取证

2026-10-02。**CASE C：只取得更新元数据，未取得当前或新版后端制品。** 当前在线 check-update 返回 local_version=v5.2.4，没有更新服务器、下载 URL 或源码仓库。设备写接口调用为 0。

## 范围与只读门槛

本阶段已完整读取既有高级后台审计、后端搜索/逆向、RAM 公式/显示链/修复计划、ADB 在线预检及 FOTA 报告。只重新取得 system.html 并恢复更新代码；未重复端口扫描、ADB shell/SYNC、ttyd Ctrl+C、全 FOTA 搜索或前端全量 API 提取。

| 接口 | 决策 | 判定证据 | 实际执行 |
| --- | --- | --- | --- |
| check-update | **SAFE_GET** | getApi 默认 GET；1837 行无 query/body；完整回调只有查询/DOM 操作，安装为独立按钮 | GET 一次；PASS |
| update-log | **SAFE_GET** | getApi 默认 GET；2266 行无 query/body；完整回调只展示日志 | GET 一次；PASS |
| apply-update | 禁止 | 1874 行显式调用 POST 包装函数，界面说明下载并安装 | NOT EXECUTED |

SAFE_GET 的含义是**满足用户指定的前端只读调用审查条件**。缺少后端制品，不能验证后台日志、缓存或远端查询实现。本次响应也未出现后台下载、写包、准备升级或进度状态；没有据此追加安装或控制请求。

人工门槛保存在本地 `test-results/update-channel/READ_ONLY_GATE.json`，绑定当前页面 SHA256。捕获脚本仅允许三个固定 GET 路径，不接受其他 URL；API 请求要求 gate 决策及 source hash 匹配。每个 endpoint 在请求前独占创建 attempt.json，已尝试就拒绝再次请求；不重试、不跟随重定向、不带认证。**本阶段三个请求已完成，不要再次运行采集。**

## 已执行请求

| 路径 | 上海时间（2026-10-02，+08:00） | HTTP | body bytes |
| --- | --- | --- | --- |
| `/html/system.html` | 19:23:00.843331 | 200 OK | 90,819 |
| `/api/system/check-update` | 19:24:35.531875 | 200 OK | 82 |
| `/api/system/update-log` | 19:25:46.799702 | 200 OK | 76 |

check-update 原始 body SHA256：`834414aab6a65347bb1f16358629761237ffe364b968f9db2003b96b00f9f281`。

update-log 原始 body SHA256：`46f8214d0000e978600dfce2c2062a54c03a46b7752f698a9d8ae66a3e96dd6f`。

system.html 原始 body SHA256：`8b3721397ad8682f2483fef7129097e54b5fb59c969442fa753516892c3ae204`，与已有缓存一致。完整 UTC/本地时间、URL、响应 URL、状态、长度与 SHA 见 [HTTP_GET_MANIFEST.csv](../analysis/update-channel/reports/HTTP_GET_MANIFEST.csv)。时间是客户端请求开始时间，不是服务器 Date。

## 响应完整字段与 headers

check-update：status=`success`；message=`当前已是最新版本`；local_version=`v5.2.4`。**没有 cloud_version 或 latest_version**，也没有 version/current_version、下载 URL、file/filename/path、sha256/md5/size、release/changelog、server/mirror/cdn/github/gitee/update_required。

update-log：status=`success`；log_content=`新增自研大屏幕（MF650L屏幕）`。只有这两个字段；正文无 URL、版本历史、包名、发布日期、构建号、下载源、开发者标识、仓库或服务器。它是功能说明，不是可验证的发布 manifest。

两次 API 返回的全部已解析 headers：

| Header | check-update | update-log |
| --- | --- | --- |
| Content-Type | application/json; charset=utf-8 | application/json; charset=utf-8 |
| Content-Length | 82 | 76 |
| Access-Control-Allow-Origin | * | * |
| Connection | close | close |

页面 headers 为 Content-Type=`text/html; charset=utf-8`、Content-Length=`90819`、Cache-Control=`no-cache`、Connection=`close`。headers 未提供 Server 或后端语言标识；不能从缺失字段识别程序。

原始 body.bin、解析器保存的完整 header 名值、headers.txt、parsed.json、response.json、attempt.json 均保存在 `test-results/update-channel/{system-page,check-update,update-log}/`。HTML 不解析为 JSON。headers.txt 是 HTTP 库重新序列化的完整 header 文本，**不宣称保存了逐字节网络报文**；body.bin 保留收到的 body 字节。

## 从更新渠道继续追踪

更新调用链没有前端下载 URL/base URL、显式 manifest、更新 hostname 或用于包下载的二次 API。apply-update 只有空表单；禁用该接口时，无法通过它观察后端下载源。没有调用其他 API、刷新更新、猜服务器目录或路径。

因此未进行包下载，未生成包 SHA/文件类型/解包 manifest，也没有执行任何下载的程序。PC 本地已建 `analysis/update-channel/downloads/{original,working-copy,manifest}/` 与 `analysis/update-channel/extracted/`，均为空且 gitignored。没有把原始 HTML 当作后端包。

## 公开来源检索

使用已知 MF650/MF650L、高级后台、v5.2.4、mf650.html、memory_usage、device/info、check-update 等组合进行有界公开检索，具体查询与来源记录在 [PUBLIC_SOURCE_SEARCH.json](../analysis/update-channel/reports/PUBLIC_SOURCE_SEARCH.json)。没有查到可确认属于当前/新版 8081 后端的公开仓库或可直接读取的包 URL；这不是对全网不存在源码的证明。

| 公开来源 | 观察与处理 |
| --- | --- |
| [奶昔论坛：阿乐卡MF650高级后台](https://forum.naixi.net/forum.php?mod=viewthread&tid=5344)，yagamil，2025-08-31 | 搜索索引列出三个 ZIP 附件，并提示需登录下载/查看。未披露可确认的后台版本。web 直接读取失败；PC 无认证 GET 返回 302，Location 指向同一公开页面，body 0 bytes，未跟随。未登录、未请求附件、未绕过认证 |
| [Bilibili：新增 MF650 高级功能](https://www.bilibili.com/video/BV1py411q7Ym/)，流量自由人，2024-06-19 | 搜索索引仅有早期功能介绍；无可用包 URL 或 v5.2.4 对应证据，web 直接读取失败 |
| [Bilibili：MF650 网页版 AT 工具](https://www.bilibili.com/video/BV1DZ421M7Tf/)，流量自由人，2024-06-28 | 搜索索引提示私信获取工具地址；未发送私信，未取得文件，web 直接读取失败 |
| [51CTO：MF650/650L 高级后台 V2](https://blog.51cto.com/M82A1/13799617)，沧州虎王科技，2025-04-13 | 搜索索引为 V2 功能描述；无当前 v5.2.4 身份证据或可用包/源码 URL，web 直接读取失败 |

以上只作为可追溯的线索，不把宣传、附件名或同名作者视为当前程序身份，也不采信其固件/云控等技术结论。本项目 MF650-Manager 仓库是客户端维护仓库，不等同于 8081 daemon 源码。

## 验证与状态

离线复核三个 body 的 SHA、实际长度与 Content-Length；两个 parsed.json 与原 body 解析结果一致；gate 与新取得页面/既有缓存 hash 一致。导出字段存在性表，检查报告引用与脱敏产物，不为验证重发设备请求。

对捕获脚本执行离线 mock 检查：已有尝试阻止开连接、hash 不匹配及 UNSAFE 决策阻止开连接、请求失败也不允许第二次尝试、仅发无 body/认证的 GET、拒绝重定向，均 PASS。网络 opener 全程替身，未增加设备请求；复核 API 调用行号、空制品目录、JSON/本地文档链接及页面凭据未出现在提交内容。

设备 GET：3；设备 POST/控制请求：0；设备修改：**NO**；apply-update：**NOT EXECUTED**；ADB Patch：**PATCH_READY_OFFLINE / NOT INSTALLED**；9008：**NOT USED**。服务器是否自行写访问日志/缓存属于 UNKNOWN，不能以客户端只读请求证明系统零内部写入。

当前后端文件、语言、监听 PID/路径、device/info handler、RAM 来源/公式/MemAvailable 使用情况仍 UNKNOWN。未满足生成 handler 逆向报告、source diff 或更新 RAM_FIX_PLAN 的条件。后端分类及下一步见 [8081_BACKEND_ARTIFACT_REPORT.md](8081_BACKEND_ARTIFACT_REPORT.md)。
