# MF650 Manager 版本发布与质量门禁规范 (RELEASE_PROCESS.md)

## 1. 强制版本升级与发布准则

从 `v0.2.0` 开始，所有后续版本发布必须严格遵守以下 12 步流水线流程，严禁跳步或跳过测试：

```
[1. 修改版本号]       -> app/build.gradle.kts (versionCode 单调递增, versionName 符合 SemVer)
       │
[2. 更新更新日志]     -> CHANGELOG.md (遵循 Keep a Changelog 规范) & In-App Changelog
       │
[3. 更新项目文档]     -> README.md (下载链接与版本说明) & HANDOFF.md
       │
[4. 运行单元测试]     -> ./gradlew test (全部测试用例必须 100% PASS)
       │
[5. 静态代码分析]     -> ./gradlew lint (零致命错误)
       │
[6. 构建 Debug 产物]  -> ./gradlew assembleDebug
       │
[7. 构建 Release 产物] -> ./gradlew assembleRelease (生成自签名可直接安装 APK)
       │
[8. 实机只读冒烟测试] -> 运行 python run_smoke_test.py 针对真机 192.168.100.1 验证
       │
[9. 敏感凭据脱敏扫描] -> git grep 扫描源码，确保零真实私有数据外泄
       │
[10. Git 提交与打标]  -> git commit -m "feat/release: ..." && git tag -a vX.Y.Z -m "..."
       │
[11. 推送代码至远程]  -> git push origin <branch> && git push origin vX.Y.Z
       │
[12. GitHub Release]  -> gh release create vX.Y.Z 上传 APK 与 SHA256 校验和并核验
```

---

## 2. 变更日志维护准则 (Keep a Changelog)

版本日志必须按以下标准段落维护，且只允许记录**已真实完成并经测试验证的功能**，绝不允许将预研或未完成项提前写入：
- **Added**: 新增功能、新界面、新控制器。
- **Changed**: 架构变更、重构、UI 刷新。
- **Deprecated**: 不推荐使用即将废弃的逻辑。
- **Removed**: 移除的旧功能或无用依赖。
- **Fixed**: Bug 修复、数据源纠偏、异常处理改进。
- **Security**: 认证强化、脱敏改进、安全拦截机制。
