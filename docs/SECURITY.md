# MF650 Manager 安全架构与凭据保护 (SECURITY.md)

## 1. 威胁建模与防御体系

| 威胁场景 | 潜在危害 | App 防御措施 |
| :--- | :--- | :--- |
| **明文 HTTP 嗅探** | 局域网恶意节点嗅探管理密码 | 严格绑定 Cleartext Network Security Config，限制仅允许局域网路由器网关 |
| **恶意重定向与 SSRF** | 伪造 HTTP 重定向诱骗 App 访问外网恶意服务器并带出凭据 | `HostAllowlistInterceptor` 拦截器校验目标 Host，非法主机立刻阻断并抛出 `SecurityException` |
| **设备端凭据泄露** | ROOT 手机或反编译读取本地配置文件中的路由明文密码 | AndroidX `EncryptedSharedPreferences` + Android Keystore 硬件级 AES-256 GCM 加密 |
| **日志数据外泄** | 崩溃或运行日志中泄露 IMEI、Wi-Fi 密码、短信验证码 | `SensitiveDataRedactor` 正则过滤器自动掩码敏感信息 |
| **误触与恶意改写** | 用户手滑或误触发改串、改基带导致硬件变砖 | R3/R4 强制二次确认对话框与 3 秒倒计时锁定按钮 |

---

## 2. 硬件级凭据存储机制
所有登录路由器所需的敏感凭据（Padavan Web 账号与密码、自定义网关 IP、短信转发 Token）均存储在 `SecureCredentialStorage` 中，采用 Android Keystore 生成的 MasterKey 对数据进行加密存储：
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "mf650_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## 3. 敏感数据脱敏规范
在所有的 UI 显示、控制台日志、导出文本中，敏感字段必须遵循如下脱敏规则：
- **IMEI**: 前 6 位与后 3 位保留，中间 6 位掩码（例如 `860123******012`）。
- **MAC 地址**: 中间两组掩码（例如 `00:E0:4C:**:**:77`）。
- **手机号码**: 中间 4 位掩码（例如 `138****5678`）。
- **密码 / Token**: 统一替换为 `[PROTECTED_CREDENTIAL]`。
