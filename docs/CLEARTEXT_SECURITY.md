# 明文 HTTP 通信安全策略规范 (CLEARTEXT_SECURITY.md)

## 1. 为什么需要明文 HTTP？
阿乐卡 MF650 路由器固件中的 Padavan Web 服务器（Port 80）与飞流高级管理服务（Port 8081）均为嵌入式轻量级服务，固件内**未配置 SSL/TLS 证书**，且嵌入式设备算力有限，官方仅支持标准 HTTP 明文通信。
若直接全局放开 Android 清单中的 `usesCleartextTraffic="true"`，会导致 App 面临重大的中间人流量嗅探风险。

---

## 2. 网络安全配置文件设计 (network_security_config.xml)
为了在严格遵循现代 Android 安全标准的前提下兼容该设备，应用在 `res/xml/network_security_config.xml` 中实施了严格的**基于域名的精细化明文放行策略**：
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">192.168.100.1</domain>
        <domain includeSubdomains="false">192.168.1.1</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

### 防御效果：
- **默认全局禁止明文 HTTP**：任何针对公网、第三方域名或未知 IP 的明文请求，均会被 Android 系统网络安全层直接抛出异常拒绝。
- **仅限目标路由器子网**：仅允许向设备默认网关 `192.168.100.1`、备用网关 `192.168.1.1` 及本地模拟器环回地址发送明文 HTTP 请求。
- **配合 OkHttp 拦截器双重保险**：应用层更有 `HostAllowlistInterceptor` 再次校验 Host，形成“操作系统层 + 应用网络层”双锁保护。
