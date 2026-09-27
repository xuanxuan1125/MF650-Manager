package com.mf650.manager.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusSuccess
import com.mf650.manager.ui.theme.StatusWarning

data class ReleaseLog(
    val version: String,
    val date: String,
    val isLatest: Boolean,
    val added: List<String>,
    val changed: List<String>,
    val fixed: List<String>,
    val security: List<String> = emptyList()
)

val APP_CHANGELOG_HISTORY = listOf(
    ReleaseLog(
        version = "v0.2.0",
        date = "2026-09-27",
        isLatest = true,
        added = listOf(
            "全新的统一 Liquid Glass / 液态玻璃设计系统，支持标准高光边框与动态折射",
            "首页新增【高级控制中心】8 大快捷入口（锁频、锁小区、SIM、直供电、流量守护、转发、定时、LCD）",
            "新增多级后台连通性诊断徽章（Port 80、Port 8081、5G 蜂窝各自独立指示）",
            "蜂窝页面支持【简洁模式】与【专业模式】无缝切换",
            "新增 5G NR 与 LTE 支持频段 FilterChip 多选控制与恢复自动",
            "新增应用内更新日志（ChangelogScreen），无需联网随时查阅版本演进",
            "新增玻璃渲染画质（High / Balanced / Performance）与 AMOLED 纯黑主题",
            "引入 BatteryNormalizer 与 UnitNormalizer 严谨量化转换与校验引擎"
        ),
        changed = listOf(
            "彻底重构首页 Dashboard：引入 Network Hero 核心大卡、紧凑四宫格与设备健康卡片",
            "移除所有 Emoji 图标，全面换装统一规格的 Material Vector Symbols",
            "底部导航栏升级为悬浮玻璃胶囊栏，第二项正式定名为【蜂窝】",
            "Wi-Fi 设置页面重构为卡片化分级管理（基础、安全、射频、高级）",
            "短信中心重构为沉浸式消息卡片流，右上角直达高级转发设置",
            "更多功能页按【设备】、【网络】、【自动化】、【显示】、【系统】、【高级】六大玻璃组重构"
        ),
        fixed = listOf(
            "彻底修复真机电池电量显示为 150% 的恶性 BUG（溯源发现 Port 8081 嵌套对象反序列化及 Port 80 误判 6 档库仑计为 4 档并错误乘以 25）",
            "对准官方 charge.html 提取 [0, 10, 25, 50, 75, 90, 100]% 硬件档位映射表，杜绝 clamp 假修",
            "修复蜂窝状态返回非数值字符串（如 tac/cgi/rssi）导致的反序列化崩溃",
            "修复实时网速单位混淆（MB/s 与 KB/s 换算纠偏），消除折线图瞬时溢出",
            "修复高危操作确认弹窗无倒计时锁的问题，R3/R4 强制锁定 3 秒方可确认"
        ),
        security = listOf(
            "强制 Network Security Config 仅放行局域网路由器网关明文通信",
            "HostAllowlistInterceptor 严格校验目标主机名，阻断任何外网恶意重定向与凭据劫持",
            "基于 Android Keystore 生成硬件级 AES-256 GCM 密钥加密存储路由管理密码"
        )
    ),
    ReleaseLog(
        version = "v0.1.0",
        date = "2026-09-27",
        isLatest = false,
        added = listOf(
            "初始版本发布，奠定纯原生 Android (Kotlin + Jetpack Compose) 基础",
            "支持 Port 80 Padavan 与 Port 8081 飞流定制后台双端口通信",
            "支持 2.4G/5G Wi-Fi 基础配置与 15 秒重启倒计时",
            "集成 ttyd Port 7689 Web Terminal 终端会话",
            "实现 27 项基础协议单元测试并全部通过"
        ),
        changed = listOf(
            "使用 Retrofit 与 OkHttp 替代原始 Web 界面"
        ),
        fixed = listOf(
            "解决 Port 80 apply.cgi 隐藏字段不全导致无法生效的问题"
        )
    )
)

@Composable
fun ChangelogScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlassTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        APP_CHANGELOG_HISTORY.forEach { release ->
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = release.version,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (release.isLatest) AccentCyan else MaterialTheme.colorScheme.onSurface
                            )
                            if (release.isLatest) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                                        .background(AccentCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }
                            }
                        }
                        Text(
                            text = release.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (release.added.isNotEmpty()) {
                        SectionLog("Added", release.added, StatusSuccess)
                    }
                    if (release.changed.isNotEmpty()) {
                        SectionLog("Changed", release.changed, AccentCyan)
                    }
                    if (release.fixed.isNotEmpty()) {
                        SectionLog("Fixed", release.fixed, StatusWarning)
                    }
                    if (release.security.isNotEmpty()) {
                        SectionLog("Security", release.security, Color(0xFFE040FB))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLog(title: String, items: List<String>, accentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        items.forEach { item ->
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
