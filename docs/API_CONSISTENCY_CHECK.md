# MF650 双后端协议端点一致性核验报告 (API_CONSISTENCY_CHECK.md)

## 1. 跨阶段与跨文档端点数量核验矩阵

在第一阶段与第二阶段的审计过程中，由于扫描工具粒度、合并规则及动态参数分支识别的演进，各文档在表面统计数字上存在差异。本报告依据原始 HTML/JS 代码与 [`COMPLETE_API_MATRIX.csv`](file:///c:/Users/fqxku/.gemini/antigravity/scratch/MF650_AUDIT/SECOND_STAGE/PROTOCOL/COMPLETE_API_MATRIX.csv) 重新进行了逐项计算与交叉对账：

| 统计口径 / 来源文档 | 记录端点数 | 统计边界与差异原因说明 |
| :--- | :--- | :--- |
| **第一阶段初始清单** (`08_api/API_MATRIX.csv`) | **51 个** | 包含 8081 端口 47 个接口 + 80 端口 4 个核心端点 (`system_status_data.asp`, `status_internet.asp`, `log_content.asp`, `apply.cgi`)。 |
| **第二阶段 8081 重新审计清单** (`RE_AUDIT_8081.json`) | **61 个** | 仅统计 8081 端口。对 `system.html` 进行了深度 AST 遍历，挖出了第一阶段遗漏的 14 个扩展接口（包括 OTA 升级、卡密激活、大屏控制等），并归一化为 61 个端点。 |
| **OpenAPI 规范** (`advanced_8081_openapi.yaml`) | **61 个** | 仅涵盖 8081 端口的 RESTful JSON 接口（Paths 共 18 条基础路径，展开不同 Method 与 Query 变体后对应 61 个 Operation）。 |
| **Padavan 80 端口写入协议** (`PADAVAN_WRITE_PROTOCOL.csv`) | **27 个** | 涵盖 80 端口全部活跃页面的 POST `/apply.cgi` 表单动作分支（如 Wi-Fi 重启、LAN 重启、DNS 重启、防火墙规则重载、PIN 校验等）。 |
| **Padavan 80 端口数据读取流** | **5 个** | `system_status_data.asp`, `status_internet.asp`, `sms_in.asp`, `sms_out.asp`, `log_content.asp`。 |
| **Port 7689 ttyd 终端** | **1 个** | `ws://192.168.100.1:7689/ws` WebSocket 终端连接。 |
| **全量终极统一矩阵** (`COMPLETE_API_MATRIX.csv`) | **94 个** | **绝对事实标准**：8081 (61) + Padavan 80 (32: 5读+27写) + ttyd (1) = **94 个端点**。 |

---

## 2. 差异对账与合并规则结论

1. **8081 端口从 47 到 61 的净增核验**:
   在 `system.html` 中新增解析出：
   - `/api/system/apply-update` (POST)
   - `/api/system/toggle-sim` (POST)
   - `/api/system/toggle` (POST: reverse_charge, reconnect, cloud, self_screen)
   - `/api/system/activate` (POST)
   - `/api/system/activate-lcd` (POST)
   - `/api/system/bigscreen` (GET & POST)
   - `/api/system/bigscreen-activate` (POST)
   在 `cron.html` 中补齐了 `POST /api/traffic-config` 与 `GET /api/cron?ACTION=get`。
2. **Padavan 80 端口由 4 个粗粒度扩展为 32 个精准端点**:
   第一阶段仅记录了一个笼统的 `/apply.cgi`，第二阶段细化了 27 种具有独立 `action_mode`、`action_script`、`sid_list` 的业务写入行为，确保 Android SDK 不会因为缺失特定隐藏字段导致配置失效。
3. **Android App 唯一实现基准**:
   所有 Retrofit 接口、OkHttp 拦截器及 Repository 均**严格以 94 个端点的 `COMPLETE_API_MATRIX.csv` 为准**。
