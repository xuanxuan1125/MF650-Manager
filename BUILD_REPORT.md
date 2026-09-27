# 构建与打包交付报告 (MF650 Manager v0.2.0)

## 1. 交付产物汇总

| 产物项 | 路径 | 大小 | 校验和 (SHA-256) |
| :--- | :--- | :--- | :--- |
| **正式 Release APK** | `build-artifacts/MF650_Manager_v0.2.0.apk` | 12,762,700 字节 (~12.17 MB) | `DBE38D238A05A01A746282032886018541D99CF8740AE6292BFF2D113B1ED819` |
| **调试 Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | 19,141,065 字节 (~18.25 MB) | -- |

---

## 2. 构建环境与参数配置

- **应用包名**：`com.mf650.manager`
- **版本号**：`versionCode = 2`, `versionName = "0.2.0"`
- **Compile SDK**：`35` (Android 15)
- **Min SDK**：`26` (Android 8.0 Oreo)
- **Target SDK**：`35`
- **JVM 目标**：Java 21 / Kotlin 2.0.21
- **Gradle**：8.13
- **签名配置**：配置有统一签名，可通过 Android 系统直接安装并覆盖升级。

---

## 3. 自动化流水线验证记录

- **单元测试**：`:app:test`
  - 结果：40 个测试用例全部 PASS，0 Failure，0 Error。
  - 核心通过验证：`BatteryNormalizerTest` (6项), `UnitNormalizerTest` (7项), `AdvancedApiSerializationTest` (5项), `SensitiveDataRedactorTest` (4项), `PadavanFormBuilderTest` (5项), `SignalQualityEvaluatorTest` (5项), `PadavanParserTest` (3项), `HostAllowlistInterceptorTest` (2项), `PadavanAuthInterceptorTest` (2项), `RepositoryFailoverTest` (1项)。
- **静态分析与代码优化**：
  - `:app:lintVitalRelease`：通过
  - `:app:optimizeReleaseResources`：通过
  - `:app:dexBuilderRelease`：通过
  - `:app:packageRelease`：通过

---

## 4. 交付文件清单

- `docs/BATTERY_DATA_MODEL.md`：电池物理模型与 150% 错误根因分析文档
- `docs/UNIT_NORMALIZATION_AUDIT.md`：速率与单位量化换算审计文档
- `docs/LIQUID_GLASS_DESIGN_SYSTEM.md`：液态玻璃设计系统完整设计令牌与组件规范
- `docs/ADVANCED_FEATURE_AUDIT.md`：Port 8081 飞流后台全量能力与风险分级审计报告
- `docs/WEB_TO_APP_COVERAGE.md`：原版 Web 后台功能 1:1 覆盖与替代度评估报告
- `docs/UI_REVIEW_PASS_1.md` ~ `4.md`：四轮细致界面走查与审计报告
- `docs/TEST_REPORT_v0.2.0.md`：单元测试与质量验证全景报告
- `CHANGELOG.md`：应用外部全量更新日志
- `app/src/main/java/com/mf650/manager/ui/screens/more/ChangelogScreen.kt`：应用内静态更新日志界面
- `build-artifacts/MF650_Manager_v0.2.0.apk`：生产发布版可执行文件
