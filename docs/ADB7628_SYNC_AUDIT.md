# MF650 7628 标准 ADB SYNC 只读审计

日期：2026-10-02。范围：既有设备的 7628 Transport、标准 sync: 服务及只读文件请求。没有重复完整端口扫描或 shell 测试，RAM 部署保持暂停。

## 结论

**Case B：ADB Transport=YES；ADB Shell=NO（前阶段实测）；ADB Sync Read=NO。**

四次 stock adb pull 都返回 connect failed: closed。关闭本机既有 ADB 客户端连接后，三次直接 ADB CNXN 均成功，但 OPEN sync: 均收到 CLSE(arg0=0,arg1=1)，没有 OKAY。因此 STAT、LIST、RECV 文件请求都没有实际发送。

当前可称为 **ADB-compatible restricted transport**，不能称“完整 ADB”。本轮只验证了标准 sync:；没有枚举其他非 shell 服务，不能宣称所有非 shell 服务都关闭。服务被禁用、访问受限或厂商只实现部分协议的具体原因尚未知。

## ADB 状态

| 检查 | 结果 |
| --- | --- |
| adb version | 1.0.41；platform-tools 35.0.1-11580240 |
| adb devices -l | 目标列为 device |
| get-state | device，退出码 0 |
| get-serialno | 返回目标 TCP endpoint，退出码 0；不是已验证硬件序列号 |
| get-devpath | unknown，退出码 0 |
| features | 空输出，退出码 0；没有宣告扩展特性 |

这些 stock adb 查询主要读取本机 ADB server 的 Transport 信息，不证明远端支持 shell 或文件服务。完整 stdout/stderr 和安装路径只保存在本地 test-results/adb7628/adb-state-results.json，不公开本机细节。

## Stock adb pull

按用户要求顺序执行，目标是 PC 本地 pulled 目录：

| 远端路径 | 退出码 | 完整错误 | 获得文件 |
| --- | --- | --- | --- |
| /proc/version | 1 | adb: error: connect failed: closed | NO |
| /proc/meminfo | 1 | adb: error: connect failed: closed | NO |
| /proc/mounts | 1 | adb: error: connect failed: closed | NO |
| /etc/passwd | 1 | adb: error: connect failed: closed | NO |

失败发生在 SYNC 连接阶段，不能据此判断路径不存在、文件权限不足或文件内容为空。没有执行 adb pull /www，避免未预览规模的目录拉取。

## 原始标准协议复核

使用 tools/adb_sync_readonly.py，不依赖 stock adb 的 SYNC 实现；协议依据 [AOSP Transport](https://android.googlesource.com/platform/system/core/%2B/refs/tags/android-11.0.0_r20/adb/protocol.txt) 与 [AOSP SYNC.TXT](https://android.googlesource.com/platform/system/adb/%2B/fda46ba4632b59d6054b67c6c7e79898839f785b/SYNC.TXT)。

第一次直接 CNXN 超时。当时 stock adb 仍保持目标连接。本机路由核实指向设备接口；关闭本机该 Transport 连接后，直接 CNXN 可用。关闭的是 PC 客户端连接，没有停止或重启设备服务；尚未证明造成超时的内部机制。

之后三次测试分别选择 stat /proc/version、list /www、recv /proc/meminfo。每次实际网络序列均为：

```text
client → CNXN
server → CNXN (legacy version, max payload 4096)
client → OPEN sync:
server → CLSE (remote id 0, local id 1)
```

没有收到 service OKAY，也没有发出 WRTE 包；操作选择只表示客户端打算执行的读取，不代表已经发送 STAT/LIST/RECV。原始帧、payload、源地址与日志仅保存在 PC 的 test-results/adb7628，不提交公共仓库。

测试后恢复了本机原有 adb connect，adb devices 再次显示 device。没有执行 root、remount、push、install、reboot、tcpip、setprop、service 启停或 ShellCrash 菜单。

## 只读工具与验证

工具只提供 stat、list、recv，固定 OPEN sync:。远端请求白名单只有 STAT、LIST、RECV；没有上传 SEND、上传 DATA/DONE 或其他服务接口。ADB WRTE 是承载只读 SYNC 请求的 Transport 帧，不提供文件写入操作。RECV 的 DATA/DONE 只作为服务器响应读取，输出只写入 PC 本地文件。

没有递归枚举功能。路径长度、响应长度、文件总量和目录条目数量有边界；RECV 必须收到完整 DONE 后才保存本地文件。没有启用 LSTAT v2，因为目标未宣告对应特性。

```text
python -X utf8 tools/adb_sync_readonly.py stat /proc/version
python -X utf8 tools/adb_sync_readonly.py list /www
python -X utf8 tools/adb_sync_readonly.py recv /proc/meminfo test-results/adb7628/pulled/proc_meminfo.txt
```

--trace 仅供保存本地原始证据，必须留在忽略的 test-results 目录；本轮痕迹不提交 Git。

6 个离线测试通过：分片 STAT、零尺寸虚拟文件 metadata、目录 LIST、RECV 与 ACK、OPEN 被关闭时不发文件请求、拒绝上传/非 sync 服务、失败与异常大小处理。离线内存文本是测试夹具，不是设备采样。运行命令：python -B -X utf8 -m unittest discover -s tools/tests -v。

## 文件系统、内存与端口线索

- Directory LIST：NO；根目录与有限目录枚举均未开展。
- /usr/bin/feiliu.sh：未拉取；只保留旧启动链路中的路径线索。
- mf650.html：文件系统路径 NOT FOUND；HTTP 副本不能证明设备绝对路径、mode 或 owner。
- Port80 / Port8081 Web root：UNKNOWN。详见 ADB_SYNC_FILESYSTEM_MAP.md。
- MemTotal / MemAvailable：本轮 UNKNOWN；没有取得 meminfo，不生成伪造的 memory_calculation.json 或 RAM_FORMULA_VERIFICATION.md。
- 新百分比未计算，未新采样 /api/device/info，未生成修改副本或 patch。
- 仅在前阶段已下载的 HTML/JS/CSS 中搜索 2358、7777，没有找到引用；没有重新连接这两个端口，没有 fuzz 或命令猜测。

## 后续条件与发布边界

目前不具备 Web RAM 修复部署条件：仍缺少设备文件读取方式、原文件实际路径及权限/属主、实时 MemAvailable、后台扩展方式与完整备份/rollback。

下一步需取得已有合法文件读取渠道、厂商对受限 Transport 的说明，或用户已有的 feiliu.sh / Web 后端原文件资料，再继续静态分析。没有依据尝试未知 service 或绕过限制。

本轮 Git 提交仅包含脱敏技术报告与只读工具/离线测试。原始抓包、MAC、本机网络细节、pulled 内容和设备标识均保留在本地忽略目录。设备修改=NO；ADB Push=NOT EXECUTED。
