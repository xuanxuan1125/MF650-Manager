# ADB SYNC 文件系统定位状态

日期：2026-10-02。标准 sync: 在 OPEN 阶段返回 CLSE；stock pull、原始 STAT/LIST/RECV 都未取得文件或目录记录。以下区分路径线索和实际文件系统验证。

| 对象 | 已有线索 | 本轮 SYNC 验证 | 实际路径 / Web root |
| --- | --- | --- | --- |
| Port80 Web root | HTTP 前端与 Padavan ASP/JS 资源 | 无法 LIST/STAT | UNKNOWN |
| Port8081 Web root | / 重定向至 /mf650.html，另有 /html/mf650.html | 无法 LIST/STAT | UNKNOWN |
| mf650.html | 已有 HTTP 页面副本与内容 hash | 无法 RECV、未取得 mode/size/mtime | NOT FOUND（设备绝对路径未定位） |
| feiliu.sh | 旧审计及 ttyd 标题引用 /usr/bin/feiliu.sh | 未拉取，未验证文件存在与权限 | /usr/bin/feiliu.sh 仅为启动引用；实际文件未读 |
| ShellCrash | 前阶段菜单输出目录 /home/root/ShellCrash | 未 LIST/STAT | 目录线索；实际文件未定位 |
| /proc/meminfo | 已知 Linux 固定路径 | pull 和标准 sync: 都失败 | 内容 UNKNOWN |

没有列出根目录，没有遍历 /usr、/home、/tmp、/data，也没有递归 dump。目录 LIST 服务未建立，因此无法预览 /www 规模，没有整目录 pull。

没有拉取 RAM 页依赖的 JS/CSS，没有进行 feiliu.sh 本地源码分析，也没有得到文件 uid/gid。不得把 HTTP URL 当作绝对文件路径，或用 HTTP 副本替代权限、属主与回滚所需的设备备份。

本轮未生成 web-patches/ram-fix/mf650.html.modified 或 mf650.patch；它们依赖尚未满足的文件读取与真实内存数据条件。RAM 修复继续暂停，设备没有更改。
