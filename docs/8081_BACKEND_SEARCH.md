# 8081 后端搜索记录

2026-10-02。**当前 `/api/device/info` 的实现尚未取得；已确认 FOTA 的两条旧 RAM 数据路径。** 本轮只读 PC 上的固件、反汇编及已有审计元数据，设备请求、设备命令和写接口均为 0。没有重新分析 HTML、JS、CSS 或提取前端 API。

## 搜索范围与复核

参考文件：`MF650_2.3_Fota.zip`，58,614,865 bytes，SHA-256：

`ef46382fb3b1126c42780e141b60c0a638f7b68d3125659bfaa1c42a14ae8e8e`

直接读取 ZIP 的全部 4,551 个非目录成员，包括 bin、sbin、usr/bin、www、WEBSERVER/cgi-bin、脚本、库、配置和 boot.img。逐个通过 ZIP CRC、大小及既有 manifest SHA-256 复核，扫描 116,633,721 bytes。直接读 ZIP 避免 NTFS 大小写冲突导致遗漏。

对关键词做不区分大小写的 ASCII、UTF-16LE、UTF-16BE **字面子串**搜索；不把子串命中当作路由或调用关系。原始扫描产生 9,789 条记录，每条含原 ZIP 路径、SHA-256、零起始 byte offset 和转义字节上下文。另扫描 `mem_usage`、`get_mem_usage`、`/api/`、`/api/device`、`showall`，产生 16 条记录；合计 9,805 个原始成员命中 offset 已重新核对。

还解压并扫描 423 个文件头识别出的 bzip2 成员，全部为 SELinux policy；加上 boot.img 中的 gzip 内核，共 424 个派生单元、90,535,265 bytes。这些字节与原成员范围重叠，不能相加当作独立固件大小。未识别的嵌入压缩流、动态拼接路由、运行时下载和当前设备可写层不在“字面量未命中”的证明范围。

公开复核产物：

- [BACKEND_SEARCH_RESULT.json](../analysis/web8081/reports/BACKEND_SEARCH_RESULT.json)：范围、哈希、当前未知项、动作计数。
- [BACKEND_KEYWORD_COUNTS.csv](../analysis/web8081/reports/BACKEND_KEYWORD_COUNTS.csv)：143 行分范围/编码统计。
- [BACKEND_BYTE_EVIDENCE.csv](../analysis/web8081/reports/BACKEND_BYTE_EVIDENCE.csv)：19 条人工复核的字符串、指令和启动项。
- [BACKEND_RESOURCE_PROVENANCE.csv](../analysis/web8081/reports/BACKEND_RESOURCE_PROVENANCE.csv)：复用上阶段缓存资源哈希的差异比较。

完整上下文仅存本地忽略目录 `test-results/web8081-backend/`：`hits.jsonl`、`upstream-hits.jsonl`、`decompressed-hits.jsonl`、`config-hits.jsonl`；扫描脚本、摘要及 GNU 输出同目录保留。含原始固件上下文的文件不公开提交。

## 关键词结果

以下为 ASCII 子串统计，括号内为命中文件数；UTF-16 的核心路由/RAM 字段均为 0。泛词 `api` 另有 UTF-16LE/BE 各 1 条偶然命中，详见 CSV。

| 关键词 | 原 ZIP 成员 | 解压派生单元 |
| --- | --- | --- |
| `/api/device/info`、`device/info` | 各 0 | 各 0 |
| `memory_usage` | 0 | 0 |
| `system_status` | 19（14） | 0 |
| `cpu_usage` | 17（3） | 0 |
| `battery` | 289（49） | 112 |
| `temperature` | 123（8） | 24 |
| `8081` | 25（6） | 2 |
| `mf650` | 15（6） | 0 |
| `api` | 8,410（236） | 见 CSV |
| `/proc/meminfo` | 12（5） | 0 |
| `MemTotal` | 5（4） | 1 |
| `MemFree` | 39（12） | 1 |
| `MemAvailable` | 0 | 1：内核 |
| `feiliu.sh`、`mf650.html`、`v5.2.4` | 各 0 | 各 0 |

额外搜索的 `mem_usage` 有 12 条，其中用户态非 www 命中仅在 `system/usr/bin/mobile_svr`：字段 `mem_usage` 位于 `0x1524`，`get_mem_usage` 位于 `0x15b4`。`/api/device` 未命中；唯一 `/api/` 位于 `system/usr/bin/lcd:0x8e82c`，上下文是远端设备地址二维码 URL，不是本地 8081 路由。`feiliu` 的 `https://%s/api%s` 在调用 `curl_easy_perform` 的出站请求函数内，也不能当作本地 API 实现。

## 排除 8081 假线索

| 原成员/派生单元 | Offset 示例 | 上下文与判断 |
| --- | --- | --- |
| `system/etc/services` | `0x4666` | `tproxy 8081/tcp # Transparent Proxy`；端口名称表，不启动监听 |
| `system/etc/initscripts/power_config` | `0x4ab2` | `0x17808100`；DCC 硬件地址的一部分 |
| `boot.img` | `0x6adacd` 等，20 条 | `qcom,clock-cpu@17808100`；设备树硬件地址 |
| `wlan-mag.ko` | `0x512757` | `__func__.148081`；符号名数字的一部分 |
| `libprotobuf-lite.so.13.0.0` / `libprotobuf.so.13.0.0` | `0x36788` / `0x185960` | 十进制转换查表中的 `...79808182...` |
| SELinux `base/cil!bzip2` | `0x24c3f` | `portcon tcp 8081 ... transproxy_port_t`；标签规则，不是启动服务 |
| 解压内核 | `0xf3b418` | `Micrel KSZ8081 or KSZ8091`；PHY 型号 |

这些 offset 均按对应原成员或解压单元计数，不是 ZIP 物理 offset。二进制也可能使用数值常量、参数或配置端口，因此没有 ASCII `8081` 不能排除某程序支持该端口。

## 程序与启动项

ZIP 有 729 个 ELF 文件。针对相关程序只读取 ELF header、sections/imports、字符串和指令；没有运行目标程序。httpd、feiliu、mobile_svr 都是 little-endian ARM32 ET_DYN 原生 ELF；这不是当前 8081 的语言证据。

| FOTA 服务 | 有效 ExecStart | 可确认内容 |
| --- | --- | --- |
| `httpd.service` | `/usr/bin/httpd -d` | main 默认端口 80，支持 `-p`；`0x5c4c` bind、`0x5c5c` listen，工作目录 `/www` |
| `feiliu.service` | `/usr/bin/feiliu` | 有出站 curl 云端 API 调用；没有识别出当前 REST 实现 |
| `mobile_svr.service` | `/usr/bin/mobile_svr` | 原厂 NVRAM `mem_usage` 的计算生产者 |

服务文件位于 `system/etc/systemd/system/`。httpd.service 中 lighttpd ExecStart 是注释；不据此宣称当前使用 lighttpd。另有旧 QCMAP CGI 与 BusyBox httpd applet，但没有当前 `/api/device/info` 的实现证据。FOTA 文件清单未发现 Python/Node 运行程序或 `.py/.pyc/.go/.lua/.php` 文件；程序语言仍不能靠文件名缺失判断。

**在线 8081 的 LISTEN PID、`/proc/<PID>/exe` 和 argv 未取得。** `/var/run/httpd.pid` 只是旧 ELF 内路径，不是本次读出的 PID；不能把旧默认 80 的 httpd 或同名 feiliu 填成当前 8081 程序。

另只读核对上阶段已保存的 19 条 API 响应头记录，没有 Server、X-Powered-By、Via 指纹字段；JSON Content-Type 不能区分 C、Go、Python 等语言。本轮没有为此发起新请求，也不把记录中缺少指纹当作完整后端身份证明。

## 配置检查

按文件名含 config，或扩展名 json/yaml/yml/ini/conf/cfg，筛出 142 个成员；包含 ldconfig、iwconfig、libconfigdb 三个 ELF 名称假命中，139 个非 ELF 候选。对这些候选搜索完整单词 memory/ram/system，共 293 条、40 个文件，并结合全包 RAM/API 关键词结果核对。

memory/ram 的非 ELF 上下文是 GNSS 日志/批量定位、low_ram 进程策略、limits.conf 限制、tinyproxy 回收注释、Wi-Fi deep sleep、D-Bus 配额。未发现可控制当前 RAM 公式、MemAvailable 或 REST 输出字段的统计/API 开关。**这是对该参考包候选配置的结果；当前后台配置未取得。**

## FOTA 与当前定制资源

复用上阶段 HTTP_GET_MANIFEST 的 12 个成功 HTML URL、11 个不同 body 哈希，与 FOTA 所有成员哈希比较，**0 个内容完全相同**；本轮没有读取页面代码或重新下载页面。当前首页的已保存 SHA-256 为 `bc4a01ccbb817ee801c7a16c32e18385b9aa0dc34e75e8f53182c6361b2fbbc8`。

| 资料 | 来源判定 |
| --- | --- |
| httpd、mobile_svr、feiliu、旧 CGI、对应 systemd units | 确实来自 2.3 FOTA；hash 见 byte evidence/manifest |
| `/mf650.html`、`/html/mf650.html` 及已缓存菜单页面 | 当前设备资料，与 2.3 FOTA 成员不相同；物理部署路径未知 |
| `/api/device/info`、v5.2.4 标签 | 上阶段真实 API 返回；对应后端文件尚未取得 |
| 当前 `feiliu.sh` | 已有 ttyd 启动链记录；FOTA 未包含该命名文件/字符串，文件内容未取得 |

后几项属于“当前新增或替换、来源待核实”的资源。差异支持存在定制层或后续版本，**不能单凭差异断言由第三方安装、确定作者或安装时间**。2.3 FOTA 不是当前 v5.2.4 后台的完整备份。

## 尚需取得的证据

下一步需要当前监听进程的只读导出或维护方提供的实际后台文件/源码及配置。若之后已有可用只读 shell，先按已定流程确认身份，然后依次查看 `ss -lntp`、必要时 `netstat -lntp`、`ps`；定位 8081 的 PID，再读取 `/proc/<PID>/exe`、cmdline、服务配置及文件 hash。未验证访问入口时不重试 ttyd/ADB、不猜端口。

取得文件后只读追踪路由、`mem_usage` 的消费者、meminfo 解析和字段序列化。现在没有读取进程列表、没有发起文件拉取或执行任何服务操作。公式证据及条件方案见 [MEMORY_USAGE_FORMULA.md](MEMORY_USAGE_FORMULA.md)，最终结果见 [8081_BACKEND_REVERSE_REPORT.md](8081_BACKEND_REVERSE_REPORT.md)。
