# MF650 电池数据模型与溯源审计报告 (BATTERY_DATA_MODEL.md)

## 1. 真实故障根因分析 (Why "150%" Occurred)

在 v0.1.0 实机运行时，首页电池状态曾出现异常的 `150%`。
经逆向审查固件与真实抓包对照，确认存在**双重级联致命错误**：

### 错误一：Port 8081 `/api/device-status` DTO 结构定义错误
原始代码在 `Models.kt` 中将 `battery` 定义为单一整型：
```kotlin
// 错误定义：
data class BatteryStatus(
    @SerializedName("status") val status: Int = 1,
    @SerializedName("battery") val batteryLevel: Int = 0,
    ...
)
```
然而设备真实返回的 JSON 为嵌套对象（见 `test-results/v020_battery_raw/device-status.json`）：
```json
{
  "status": "success",
  "battery": {
    "level": "6/6",
    "voltage": "3.98V"
  },
  "usb_connection": "已连接",
  "charging": {
    "enabled": 0,
    "status": "未充电"
  },
  "auto_charge": {
    "enabled": 1,
    "min_level": 2,
    "max_level": 5,
    "controller_status": "已停止",
    "controller_running": true
  }
}
```
Gson 在反序列化 `battery` 字段时遭遇类型不匹配异常（`Expected an int but was BEGIN_OBJECT`），导致 `advancedApi.getBatteryStatus()` 每次均调用失败抛出异常！

### 错误二：Port 80 降级兜底逻辑中荒谬的乘法假定
由于 Port 8081 反序列化必然失败，数据仓库强制降级走 Port 80 `/system_status_data.asp` 兜底。
在 `system_status_data.asp` 中，路由器返回：
```javascript
bat_value: 6, bat_charge: 0
```
前任代码在未查阅硬件寄存器规格的情况下，闭着眼睛瞎猜“电池只有 4 格，每格 25%”，写下了灾难性的硬编码代码：
```kotlin
// 荒谬的乘法：
batteryLevel = sys.batteryLevelBar * 25
```
当电池满电时，`bat_value` 为 `6`，`6 * 25 = 150`！这就是真机显示 `150%` 的真正罪魁祸首！

---

## 2. 官方固件真实电池量化等级矩阵

查阅飞流高级后台源码 `11_pulled_web/8081/html/charge.html` 第 764-772 行：
```javascript
if (data.battery && data.battery.level) {
    const levelMatch = data.battery.level.match(/^(\d+)\/6$/);
    if (levelMatch) {
        const level = parseInt(levelMatch[1]);
        const percentages = [0, 10, 25, 50, 75, 90, 100];
        const percentage = percentages[level] || 0;
        updateBatteryRing(percentage);
    }
}
```

高通 SDX55 + IP5332 PMIC 硬件库仑计对外报告为 **0 到 6 档离散电量级别 (0/6 ~ 6/6)**。官方真实百分比映射表严格如下：

| 电量级别 (Raw Level) | Port 8081 `battery.level` | Port 80 `bat_value` | 实际对应百分比 (%) | 状态含义 |
| :---: | :---: | :---: | :---: | :--- |
| **0** | `"0/6"` | `0` | **0%** | 空电，即将低压关机 |
| **1** | `"1/6"` | `1` | **10%** | 极低电量红警 |
| **2** | `"2/6"` | `2` | **25%** | 低电量，自动充电默认下限 (`min_level: 2`) |
| **3** | `"3/6"` | `3` | **50%** | 半电中等状态 |
| **4** | `"4/6"` | `4` | **75%** | 充足状态 |
| **5** | `"5/6"` | `5` | **90%** | 高电量，自动充电默认上限 (`max_level: 5`) |
| **6** | `"6/6"` | `6` | **100%** | 满电 |

---

## 3. 规范化处理器设计 (BatteryNormalizer)

严禁使用 `battery.coerceIn(0, 100)` 掩耳盗铃。
新建 `BatteryNormalizer.kt`：
1. 优先解析 Port 8081 的 `battery.level`（如 `"6/6"`、`"4/6"`）。
2. 若来自 Port 80，则解析 `bat_value`（`0..6`）。
3. 严格查表转换。
4. 任何超出 0..6 范围的非法输入直接判定为 `INVALID`，返回 `percent = null`，UI 显示 `--`，记录报警日志，绝不猜值。
