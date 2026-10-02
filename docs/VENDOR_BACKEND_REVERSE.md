# 厂商包参考后端静态逆向

2026-10-02。**找到参考 ARM 后台，但未找到当前 8081 `/api/device/info` 实现。** 下列端口均为文件内默认参数/服务声明，不是本阶段在线监听结果；当前 PID、程序路径、hash 和语言仍 UNKNOWN。

## 可执行文件元数据

四个 ELF 均为 ARM 32 位小端 `ET_DYN`，解释器 `/lib/ld-linux-armhf.so.3`。精确清单见 [ELF_METADATA.json](../analysis/vendor-artifacts/reports/ELF_METADATA.json)。

| 文件（包内路径） | 字节数 | 原件 SHA256 | GNU build-id |
| --- | ---: | --- | --- |
| ALKMF650.zip!bin/webservers | 69,664 | `27d12ed84c9a19cfc3361c30faf8acb3d3983bda2be617a7ce212f79926b5b2e` | `96e950655bb422c3641768f0363c08a170c86a78` |
| ALKMF650.zip!bin/forwards | 63,600 | `299fe42affab306b35e06aec36b0185cbea7ba39986a87dfd061de78bb4b0111` | `db830ed00664f4647c0e2a2f2c2b8d52d74aa83a` |
| ALKMF650.zip!sbin/adbd | 30,380 | `e545cde0feedeb1deb2aa97ffe062b54caf522d4996e0db404b4fb0e7552f1c8` | `3c76f15602425e3878923352638238e182173acf` |
| 高级后台断网修复包1/main | 118,412 | `124b7bf3274d7a125da4a8ab28725c6088114dd850565fe3be00ecd449159494` | `15879bb35eed3b9e085b86e79f78441b864b9d82` |

`webservers` 依赖 libshared、libpthread、libcrypto 1.1、libc、ARM loader；`forwards` 依赖 libshared、libsqlite3、libc、loader；`main` 依赖 libgio 2.0、libglib 2.0、libpthread、libc、loader。adbd 的完整 12 项依赖记录在 JSON；它是守护进程参考文件，不是当前 8081 的候选。

语言判断：`webservers/main` 属于原生 C ABI，Mongoose/cJSON 或 C 函数调用特征支持 **C 实现的推断**。没有源文件，不能只凭 ELF 宣称已确证源码语言；更不能据此确定现机 8081 语言。`forwards` 为原生 ARM 转发服务，恢复字节含 Pushplus 发送地址，不能据此认定当前设备正在发送数据。

## 压缩恢复与 offset 约定

原件 `forwards` 有 UPX 标记；`webservers` 标记不同，但同样的 3 个压缩块可用纯 NRV2B 解码恢复原 ELF 头和两个 PT_LOAD 的完整文件内容。每块都验证长度、结束标记、输入消费、引用范围与 hash。派生镜像清除了不存在的 section table 声明，未恢复所有非装载区域，**不是完整原始 ELF，也不可作为补丁/安装原件**。

派生文件 SHA256：`webservers.load-segments.elf` 为 `eb10b8586137df9952412362c94fa7cdd0672e19e1c7d42090bd21f15c575246`；`forwards.load-segments.elf` 为 `55fb099f88964ea9fa01d4bdfd7fd3e25275944d63c6911c439fbf43e928ccfc`。块元数据见 [PACKED_ELF_RECOVERY.json](../analysis/vendor-artifacts/reports/PACKED_ELF_RECOVERY.json)。所有 offset 均注明是原件或派生镜像；不把拼接 decoded-stream 的 offset 混用成 ELF 文件 offset。

下面 Thumb 指令地址是 **VA**。其只读段 VA 与文件 offset 相同；RW 段需转换。反汇编器读取文件；没有运行解压桩、模拟或执行目标程序。现有专用 FOTA 反汇编辅助工具曾因不同 PLT 布局拒绝修复包 `main`，后改用 GNU ARM objdump，没有强行套用旧布局。

## webservers：默认 6391 的参考服务

入口代码/GOT 指向 Thumb main `0x5131`（函数 VA `0x5130`）；GOT 位置 VA `0x31ffc` 对应派生文件 offset `0x21ffc`。

| 静态链路 | 证据 |
| --- | --- |
| URL 常量 | 派生文件 offset/VA `0x1e6d8`：`http://0.0.0.0:6391` |
| main 取监听 URL | `0x51e2` 取 literal `0x5248`，值 `0x2ce40`；PC 基址 `0x51e8` 得全局 VA `0x32028`，文件 offset `0x22028`；该指针值 `0x1e6d8` 进入参数 r1 |
| 监听调用 | main VA `0x51f2` 调用 `0xc914` 的 HTTP 监听路径 |
| 事件轮询 | VA `0x5200` 调用 `0x16a5c` |
| 旧路由 | `/api/sysinfo @0x1f5ec`；并有 temp、流控、SIM/充电等旧路由 |
| 旧 RAM 字段 | `mem_usage @0x1ecc0`、`cpu_usage @0x1ecb4`；动态导入有 nvram_safe_get/cJSON，当前内存计算链未由这些字符串证明 |
| Web root 常量 | `./www @0x1e6ec`；service 未给 WorkingDirectory，不能直接把相对路径认定为 `/www` |

没有找到当前 `/api/device/info`、`memory_usage`、`v5.2.4`、`/proc/meminfo`、MemTotal/MemFree/MemAvailable。`mem_usage` 是旧字段；仅有字符串及导入不能确认哪个 handler 读取何处，更不能确认百分比计算或当前 NVRAM 消费关系。本轮不为参考服务补造 RAM 公式。

安装声明 `systemd/webservers.service:9` 为 `/usr/bin/webservers`，第 10 行为停止命令，第 11 行写作 `Restart=aways`，未修正或测试。`webserver.service` 单数版本则指 `/usr/bin/webserver`，包内没有该 binary。

**安装链有缺口：**内层 install.sh 第 37 行 chmod `/tmp/bin/*`，但第 42–45 行复制列表只有 kasb、cron、systemd、www；没有把 `/tmp/bin/webservers` 复制到 `/usr/bin` 的明确命令，第 55 行还删除 `/tmp/bin`。外层压缩 PE 安装器代码未完全恢复，可能承担其他步骤，但尚未证实。因此 `/usr/bin/webservers` 是 service 声明路径，不是已经恢复完整的部署路径证据。

## 8081：只有另一套 CGI 服务的声明

`systemd/kasb_cgi.service:7` 原文为：

`ExecStart=nohup busybox httpd -f -p 8081 -h /data/kasb/www -v >/dev/null 2>&1 &`

这是后台包内**确切的 8081 服务意图**；BusyBox binary 未包含，`kasb/www` 仅有充电脚本，不含现机三个 HTML 或 `/api/device/info` 实现。systemd ExecStart 未通过 `sh -c` 包装，不能按普通 shell 自动解释重定向/后台符号；未证明此 unit 在任何设备成功启动。其 PID、实际监听、CGI 路由与现机来源关系全部 UNKNOWN。`charge_xf` 文件名与声明的 `.sh` 名还有不一致，也未修改。

内层安装器复制 `/tmp/kasb → /data/`、`/tmp/systemd/* → /etc/systemd/system/`，尝试 enable/start kasb_cgi；不能把“尝试安装旧 CGI”当作当前 REST 后端身份链。

## main：默认 8152 的 AT/锁频服务

main VA `0x1b3c`，Mongoose handler `fn` VA `0x16a0`，`execute_atcmd` VA `0x1548`。

`main` 在 `0x1baa/0x1bac` 取 literal `0x1be8` 的 `0x2545c`，PC 基址 `0x1bb0` 得全局 VA `0x2700c`（文件 offset `0x1700c`），指针值 `0x14094` 为 `http://0.0.0.0:8152`。r1 传入 `0x1bba → mg_http_listen @0x8578`；r2 指向 Thumb `fn`。`0x1bc8` 进入轮询。启动前还取 system D-Bus；这证明代码依赖，不表示发现了新的设备入口。

`fn` 的 URI 匹配及调用链含 `/api/at @0x14120`、`/api/lockr @0x141d8`、`/api/lockw @0x14220`；`execute_atcmd` 用 `atcmd %s @0x140b0` 格式化，再调用 `popen`，锁频分支还写配置文件。细节见 [VENDOR_NETWORK_FIX_AUDIT.md](VENDOR_NETWORK_FIX_AUDIT.md)。没有当前 REST/RAM 字段或当前后台版本证据。

## 当前 memory_usage 数据链仍缺失

| 项目 | 当前状态 |
| --- | --- |
| route `/api/device/info` 对应源码/handler | NOT_FOUND |
| `system_status.memory_usage` 生成位置 | NOT_FOUND |
| 数据来源、单位、精度、序列化、错误处理 | UNKNOWN |
| Total-Free / Total-Available / 其他公式 | UNKNOWN |
| 是否读取 MemAvailable | UNKNOWN |
| 能否仅加原始 RAM 字段或调整公式 | 待当前实现与恢复条件确认；NOT_READY |

字面量搜索包含原始 372 文件和两个恢复的装载镜像，offset 见 [KEYWORD_OFFSETS.csv](../analysis/vendor-artifacts/reports/KEYWORD_OFFSETS.csv)、[RECOVERED_KEYWORD_OFFSETS.csv](../analysis/vendor-artifacts/reports/RECOVERED_KEYWORD_OFFSETS.csv)、[STATIC_BYTE_EVIDENCE.csv](../analysis/vendor-artifacts/reports/STATIC_BYTE_EVIDENCE.csv)。未找到 handler 的情况下不生成 `8081_MEMORY_HANDLER_REVERSE.md`，不宣称 CURRENT_TOTAL_FREE_CONFIRMED，不设计针对未知 binary 的 patch。版本/页面/当前执行文件 hash 尚未形成身份链。
