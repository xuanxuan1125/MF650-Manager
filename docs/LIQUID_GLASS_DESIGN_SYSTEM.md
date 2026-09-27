# Liquid Glass 设计系统规范 (MF650 Manager v0.2.0)

## 1. 核心理念与设计原则

在 CPE 设备管理工具（参考 CPE++）的演进过程中，传统的扁平卡片往往显得冰冷机械，而过度滥用模糊与厚重拟物的方案则会造成低端 Android 设备卡顿与强光下文字发糊。

MF650 Manager v0.2.0 正式确立并落地了 **Liquid Glass（液态玻璃）设计系统**：
- **微克制通透**：以深色半透明（PanelAlpha: 0.65 ~ 0.80）作为表面底色，辅以微弱（0.03 ~ 0.05）的径向环境光晕（Ambient Glow）。
- **物理渐变边缘高光 (Gradient Border Highlight)**：用 1.dp 细边框与从左上（浅色反射 0.35~0.60 Alpha）到右下（微弱暗色反射 0.10 Alpha）的双色线性渐变，塑造犹如经过精密车削打磨的玻璃厚度感。
- **触控微动效反馈**：当用户手指按下 Liquid Glass 卡片时，产生 0.985x 微缩放与高光加深动画（spring 阻尼感），松开时平滑弹回，提供明确的实体触感。
- **无障碍对比度保障**：严禁在模糊背景上直接使用低对比度文字，所有正文与标题均基于 `MaterialTheme.colorScheme.onSurface`（Light: #1A202C, Dark: #F7FAFC），确保 WCAG AA 级别以上可读性。

---

## 2. 设计令牌 (Design Tokens)

### 2.1 圆角与间距 (`GlassTokens.kt`)
| 令牌名称 | 数值 | 使用场景 |
| :--- | :--- | :--- |
| `RadiusLarge` | `22.dp` | 页面 Hero 大卡片、核心仪表盘、外层功能大容器 |
| `RadiusMedium` | `16.dp` | 标准 LiquidGlassCard、折叠面板、对话框表面 |
| `RadiusSmall` | `10.dp` | 内部子卡片、气泡标签、二级输入框 |
| `RadiusPill` | `999.dp` | 胶囊状态药丸（BatteryCapsule）、网络状态徽章 |
| `ScreenPadding` | `16.dp` | 全局页面左右安全间距 |
| `ItemSpacing` | `12.dp` | 卡片间垂直流布局间隙 |

### 2.2 透明度阶梯 (`Surface & Border Alphas`)
| 令牌名称 | Alpha | 说明 |
| :--- | :--- | :--- |
| `AlphaGlassHigh` | `0.65f` | 高端机型极致通透效果，背景折射更明显 |
| `AlphaGlassBalanced` | `0.80f` | 默认标准模式，在通透与省电之间取得最佳平衡 |
| `AlphaGlassPerformance` | `0.95f` | 性能优先模式，近乎实色，低端芯片超低能耗 |
| `AlphaBorderLight` | `0.35f` | 浅色高光边缘基准透明度 |
| `AlphaBorderHighlight` | `0.60f` | 按压时激活的高光折射反射层 |

### 2.3 语义色彩系统 (`Color.kt`)
| 语义通道 | 十六进制色彩 | 用途说明 |
| :--- | :--- | :--- |
| `AccentCyan` | `#00E5FF` | 核心主题亮色、5G SA、主动作按钮、上行/下行高亮 |
| `AccentTeal` | `#00BFA5` | 次级功能高亮、Wi-Fi 状态指示 |
| `SpeedDownload` | `#00E5FF` | 实时下行速率图表与文本标签 |
| `SpeedUpload` | `#FF4081` | 实时上行速率图表与文本标签（洋红区分） |
| `StatusSuccess` | `#00E676` | 极佳信号 (-80dBm及以上)、连接建立、安全正常 |
| `StatusWarning` | `#FFB300` | 边缘信号、高负载、R2 中危操作提示 |
| `StatusDanger` | `#FF3D00` | 极弱信号、温度过高、R3 重启确认 |
| `RiskColorR4` | `#D50000` | R4 极高危底层操作（IMEI 写入、裸 AT 调试）专用猩红色 |

---

## 3. 核心组件结构

1. **`AmbientBackground`**
   - 采用双层渲染，底层为主题基色（`DarkBg: #0C1017` / `AmoledBg: #000000`），表层叠加偏移为 `(200f, 150f)` 的径向微弱渐变光斑，营造深邃空间感。
2. **`LiquidGlassCard`**
   - 带有按压动效监听，外层包裹 `BorderStroke(1.dp, Brush.linearGradient(...))`，内嵌柔和玻璃填充，保证各页面视觉统一。
3. **`BatteryCapsule`**
   - 紧凑型胶囊药丸，左侧集成微型电池图元（含充电时微型闪电徽标），右侧显示精确到百分比的标准化电量及电压。
4. **`BackendHealthBadge`**
   - 独立解耦展示 Port 80 Padavan 后台、Port 8081 飞流定制后台、蜂窝注网状态三合一诊断标签。
5. **`NetworkHeroCard`**
   - 顶部聚合大卡，集成运营商 Logo、5G SA/NSA 模式徽标、实时上下行双轨折线图与当前主频段。
