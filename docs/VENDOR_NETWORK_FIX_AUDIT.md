# 高级后台断网修复包静态审计

2026-10-02。**实际功能为添加 8152 AT/锁频服务并覆盖旧 Web 目录，包含 R2/R4 写能力；不是只读工具，也未证明包含当前 8081 v5.2.4 后端。** 附件内 BAT/ELF 均未执行，以下是静态行为记录。

ZIP SHA256：`a18d097fb331565543b4a61385b55711623678d93d19e402db4f0465bc8925ea`；264 个非目录文件。安装 BAT SHA256：`16480dec108c254fca7bbba9f226c193afc21a064a6f31f5d3dba45b7b20a5ba`；main.service SHA256：`9a4143316beed9405d84eb65633f5503a7308a1c5724e37b4d866ab40c2821e5`。

## 安装脚本的全部设备命令

`双击一键刷入www.bat` 为 GB18030 文本，共 42 行。除提示/按键/退出外，设备动作只有下表；原脚本失败提示之后仍可继续后续动作，没有事务回滚。

| 行 | 静态命令引用 | 写入影响 / 风险 |
| ---: | --- | --- |
| 13 | adb shell mount -o remount,rw / | 根文件系统改为可写，R4 |
| 17 | adb push .\main /home/root/ | 新增/覆盖 `/home/root/main`，R4 |
| 20 | adb push .\main.service /etc/systemd/system/ | 新增/覆盖 systemd unit，R2 |
| 24 | adb push .\www / | 覆盖 `/www` 的包内树，R4 |
| 30 | adb shell chmod 777 /etc/systemd/system/main.service | 放宽服务文件权限，R2 |
| 31 | adb shell chmod 777 /home/root/main | 放宽程序权限，R2 |
| 32 | adb shell systemctl daemon-reload | 刷新服务配置，R2 |
| 33 | adb shell systemctl start main.service | 启动新服务，R2 |
| 34 | adb shell systemctl enable main.service | 加入自动启动，R2 |

没有显式 tar、设备备份、回滚、恢复 remount 状态或 reboot 命令。BAT 未指定网络地址、端口或单设备 serial；不据此认定任何当前连接可用。

`main.service:6–9` 声明 ExecStart=`/home/root/main`、Restart=`always`、User=`root`、WorkingDirectory=`/home/root`。未给权限更小的用户或参数。包内 binary 默认服务 URL 为 **8152**，不是 8081。

## main 的 API 与文件/AT 写行为

binary SHA256：`124b7bf3274d7a125da4a8ab28725c6088114dd850565fe3be00ecd449159494`。ARM32 小端原生 ELF，C ABI/Mongoose 特征支持 C 实现推断；完整元数据与监听参数链见 [VENDOR_BACKEND_REVERSE.md](VENDOR_BACKEND_REVERSE.md)。

下列 string offset 位于原件只读 PT_LOAD，等于 VA；调用地址为 Thumb VA。仅分析，不发送请求或 AT。

| API / 功能 | 已核对的静态行为 |
| --- | --- |
| `/api/at @0x14120` | `fn` 匹配 URI，条件分支读取 JSON 字符串后在 `0x1740` 调用 `execute_atcmd @0x1548`。是否可调用/鉴权完整条件未作线上验证 |
| AT 执行器 | `snprintf` 使用 `atcmd %s @0x140b0`，`popen @0x15a0` 以模式 `r` 获取命令输出；**r 是读取管道输出，不表示 AT 请求只读** |
| 序列号/状态相关分支 | `fn @0x17f2` 以 `w @0x141b4` 打开 `/home/root/.sn @0x141b8`，随后 `fputs @0x1804`；此分支会写文件，具体业务/校验意义未完整恢复，不推断为硬件序列号修改 |
| `/api/lockr @0x141d8` | `fn @0x1880` 以 `r` 打开 `/home/root/.lock @0x141e4`，读取锁频配置 |
| `/api/lockw @0x14220` | 条件分支解析 JSON 字符串；在 `0x194c/0x1956/0x1960` 调用 AT 执行器，命令分别为 `AT+SFUN=5`、`AT+SPFORCEFRQ=16,0`、`AT+SPFORCEFRQ=12,0`；非空输入可拼接 `AT+SPFORCEFRQ=`，在 `0x19ac` 执行；`0x19b6` 执行 `AT+SFUN=4`；`0x19ca/0x19dc` 以 w 打开并写入 `.lock` |

因此修复包存在改无线功能状态/锁频参数及配置文件的能力，按 R4 对待；不测试它们。它也包含服务自动重启配置，这是 systemd 运行策略，不代表本阶段发生重启。没有证据表明本包只是修补原 8081 daemon 的断网判断。

binary Web root 常量为 `./html`，service 工作目录指 `/home/root`，对应声明路径 `/home/root/html`；包内却上传 `/www`，未含该 HTML 目录，不能假定这些静态资源由 main 提供。包内旧 ASP/JS 的其他端口和菜单引用仅是参考资源，未追踪或访问这些端口；不重复当前前端逆向。

## 高级后台包的安装风险与回滚材料

作为对照，`ALKMF650.zip!install.sh` SHA256 为 `d18204bdd7d4da3233b6630fef3e91753191c3bd321adae04757ed9412f015b7`。可见脚本第 3 行 remount rw，第 6–31 行停止/禁用多种服务，第 34–39 行 chmod，第 42–45 行复制 kasb/cron/systemd/www，第 47–52 行按旧 adbd MD5 条件覆盖 `/sbin/adbd`，第 55/117 行删除临时目录及自身，第 59–90 行 enable/start 服务，第 101/103/104 行 USB 配置 AT 切换与持久标记。没有执行这些步骤。

随服务脚本包含 localhost:2358 的 SIM/充电 RPC、NVRAM 版本字符串写入和保存，以及定时或断网触发的 reboot/poweroff。这是 R2/R3/R4 能力，不能当成安全 shell/备份入口。包内 adbd 不等于项目离线 ADB Patch，保持全部未安装。

高级后台外层安装 EXE 是压缩 PE64，SHA256 `04f96673066698e011f9c80c7062841f9840ccd067ce86e54eeb4dfa3e87bcdc`；未完全恢复代码，不能声称所有外层行为已查清。

两个后台包的 `www_bak/1`、`www_bak/2`、`www_bak/3` 是**随包历史 UI 变体**；不含当前设备安装前 hash/权限/配置身份。未找到可验证为当前 `/api/device/info` daemon 原件的 `.bak/.orig` 或恢复包。当前二进制缺失，无法生成当前→修复包的有效 binary diff，也不能保证回滚。

结论：本包只作参考，**不运行、不上传、不安装**。它不能解决本轮当前后端身份与 RAM 公式缺失的问题。
