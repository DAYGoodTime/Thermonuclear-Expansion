# GUI 数值输入框规范

> 创建日期: 2026-10-28
> 作者: OpenCode
> 状态: 当前生效

## 适用范围

机器设置面板中的数值输入框，即 `SettingsPanel.builder()` 的 `addStringEditor` / `addIntEditor` / `addLongEditor` 配置。控件与解析器来自 ModularUI2 `2.3.92-1.7.10`，为 GT5U `5.09.54.205` 解析出的版本。

## 解析方式

- `numbersInt` / `numbersLong` 安装 `MathUtils.PARSER_WHOLE_NUMBER`（`parseExpressionWholeNumber`）：整数表达式解析器，大小写不敏感，支持 `+ - * / %`、括号与小数系数。可用后缀为 `k`、`m`、`b`／`g`、`t`，另有 `i` = 144（锭）与 `s` = 64（组），因此 `1K`、`2M`、`3G`、`4T`、`1.5K` 均可输入。
- `numbersDouble` 默认 `MathUtils.PARSER_WITH_SI`，SI 前缀范围更大。
- 空文本按 `defaultNumber`（默认 0）处理；表达式非法时 `TextFieldWidget#parse` 记录错误并返回 `defaultNumber`，不会提交非法文本。
- 校验结果经 `DecimalFormat("###.###")` 转成字符串提交，同步值是纯数字，例如输入 `1K` 提交 `"1000"`；范围限制（如负数取 0）写在 validator 内，例如 `numbersLong(value -> Math.max(0L, value))`。

## 调用顺序与常见错误

- `numbersLong` / `numbersInt` 内部就是 `setValidator(...)`。之后再调用 `setValidator` 会覆盖表达式解析，调用 `setPattern(...)` 会在输入阶段拦截字符；`setPattern(Pattern.compile("[0-9]*"))` 会让 `1K` 无法输入，必须移除。
- 单参 `numbersLong(UnaryLongOperator)` 不设置整数显示格式，带 min／max 的重载才调用 `formatAsInteger(true)`。需要千位分隔时显式加 `.formatAsInteger(true)`；它只装饰显示文本，不改变存储文本与同步值，`1.5K` 这类非整数文本原样显示。
- 用 `setMaxLength(19)` 覆盖 `long` 的十进制长度即可，字符由解析器负责，不靠长度或正则过滤。
- 超过 2^53 的 `long` 会经内部 `double` 往返丢失精度，原生 GT long 输入框同样如此。
- `numbersLong` / `numbersInt` 调用的 `defaultWholeNumberScrollValues()` 设完滚动步长后把 `usingScrollStep` 复位为 `false`，滚轮加减实际不生效；需要时显式 `.usingScrollStep()`。
- `SettingsPanelBuilder` 的 `addIntEditor` / `addLongEditor` 已内置 `numbersInt`／`numbersLong` 加 `formatAsInteger(true)`，但配对的是 `IntSyncValue`／`LongSyncValue`。同步链本身是 `StringSyncValue` 时继续用 `addStringEditor` 并显式配置解析，不改动同步类型。

## 本项目现状

- 大型蒸汽涡轮机 `LargeSteamTurbineGui` 的耗汽上限：`setMaxLength(19).numbersLong(value -> Math.max(0L, value)).formatAsInteger(true).width(125)`，仍提交字符串给 `MTELargeSteamTurbine#setSteamLimitFromGui`，保留原有 `StringSyncValue` 与同步键。
- 核电站并行沿用 GT 原生写法 `formatAsInteger(true).numbersInt(1, Integer.MAX_VALUE)`。

## 参考与验证边界

- 原生范例：GT5U `gregtech/common/gui/modularui/multiblock/MTEPurificationUnitBaseGui#openParallelSelectPanel`；GTNL `MixinMTEPurificationUnitBaseGui` 的 long 版本为 `numbersLong(() -> 1L, () -> Long.MAX_VALUE)`。
- 解析器与控件行为按 ModularUI2 `2.3.92` 源码静态核对（`TextFieldWidget`、`TextFieldRenderer`、`MathUtils`），版本对应 GT5U `5.09.54.205` 的 POM。游戏内输入 `1K`／`1.5K`／负值／超长串的实际表现待验收。
