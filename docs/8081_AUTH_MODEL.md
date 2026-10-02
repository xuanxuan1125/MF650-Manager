# MF650 8081 权限模型

2026-10-02。依据当前页面源码、19 次 API 只读 GET（18个变体）和响应头；未发送任何写请求或激活尝试。前端行为、实测读取权限与未验证后端规则分别记录。

## FACT：匿名读取

本轮请求没有 Authorization、Cookie、登录 token 或 session 参数；urllib 没有 CookieJar，也未跟随重定向。根入口返回 302 到 mf650.html，12 个 HTML URL及 19 次 API GET 均返回 200；ad.txt 同样 200。33 个响应中没有 Set-Cookie 或 WWW-Authenticate。

实测匿名可读的 API 路径及变体：

| 路径 | 本轮 GET 参数 | 结果 |
| --- | --- | --- |
| `/api/device/info` | 无 | 200，含 CPU/RAM 百分比及设备/网络字段 |
| `/api/device/status` | 无 | 200，含版本/电源/功能状态 |
| `/api/device-status` | 无 | 200，电池/充电状态 |
| `/api/system/info` | 无 | 200，版本/激活/功能状态 |
| `/api/system/bigscreen` | 无 | 200，屏幕状态/serial |
| `/api/cell` | ACTION=get_status、verify_activation | 两个 200 |
| `/api/nr5g-band` | 无 | 200，当前频段数组 |
| `/api/imei` | get-imei-info | 200，IMEI及历史字段 |
| `/api/liuliang` | traffic-usage、liuliang-cx、feixing-cx | 三个 200 |
| `/api/cron` | ACTION=get | 200，已有定时任务 |
| `/api/traffic-config` | 无 | 200，推送配置字段 |
| `/api/forward` | action=load、status、log | 三个 200 |
| `/api/photos/device-model` | 无 | 200，机型 |

这是 13 条 API 路径的 18 个变体、19 次请求，不代表所有路由或 POST 都无需授权。响应中的 serial、machine_code、IMEI、ICCID、MAC/IP、token、token2、reg_code、lcd_reg_code 等数据只留在本地忽略目录，公开报告保留字段名/类型而不公开值。

## FACT：前端没有通用登录链

已取得的内联 JS 没有登录表单、Basic/Bearer header、JWT/session 注入、document.cookie 或 CSRF token 提交流程。系统页 `postApi`/`getApi` 用普通 fetch，写请求传业务表单；浏览器的默认 same-origin cookie 行为本身不证明存在 session。

localStorage 只用于首页敏感字段显隐和壁纸页面动画偏好，未发现作为授权凭据。转发/流量配置的 token/token2 是推送渠道参数，不是 8081 登录 token。

## FACT：功能激活及操作约束

系统页 `updateActivationUI` 根据 activation_status 控制高级后台/屏幕按钮；未激活时禁用部分开关和动作。`getApi`/`postApi` 将 HTTP 403 转成 DEVICE_LOCKED 提示。当前只读 `/api/system/info` 返回 `activation_status=activated_both`。

cell 页有 GET `ACTION=verify_activation` 和 POST `ACTION=activate,activation_code`；system 页有 activate、activate-lcd、bigscreen-activate。它们是高级后台/屏幕/小区功能激活链，**没有与 adbd 服务许可的代码关联**，不能当作 ADB 解锁凭据。

系统页部分动作传业务 password/key/activation_code：关闭自动更新包含前端固定操作口令，EDL 要求输入 password，功能激活要求相应码。本文不公开实际固定口令或设备返回的激活数据，也不验证其有效性。AT 页浏览器会拦截 usbcfg，但不能据此证明后端独立执行同一检查。

## UNKNOWN：写接口的实际权限

| 问题 | 判定 |
| --- | --- |
| POST 是否要求登录或具备服务器端激活检查 | UNKNOWN，未调用 |
| 403 DEVICE_LOCKED 是否覆盖全部操作 | UNKNOWN，仅有前端处理代码 |
| 管理员/匿名角色划分与会话生命周期 | UNKNOWN，缺当前后端路由/鉴权实现 |
| 固定操作口令或前端禁用是否足以保护写接口 | UNKNOWN，不能由 UI 证明 |
| CSRF/Origin/来源网络检查 | UNKNOWN，未做主动攻击测试 |
| `ssh.enabled=true` 是否对应可用 root 登录 | UNKNOWN；只是状态字段，未连接或扫描 SSH |

因此目前只能确认上述读取公开可用，不能宣称“所有写接口匿名可执行”，也不通过绕过激活、修改前端或直接 POST 来验证。完整动作清单见 [8081_API_ENDPOINTS.md](8081_API_ENDPOINTS.md)。
