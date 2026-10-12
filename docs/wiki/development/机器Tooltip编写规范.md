# 机器 Tooltip 编写规范

> 创建日期: 2026-10-12
> 作者: DAYGood_Time
> 状态: 当前生效

## 内容分工

- `addInfo` 描述运行模式、操作、容量、并行、机制与特性支持。原型声明放在开头；运行机制不同的模式独立成组，分别写清适用的发电／发热公式、变量、配置倍率与启动／寿命限制。仅流体类型、系数或准入限制不同且机制相同的模式合并说明，共用公式只写一次，分别列出系数与限制；热交换站与大型蒸汽涡轮机的蒸汽类型按此规则合并。模式、通用特性、独立机制之间使用原生 `addSeparator()`，不手写横线、不逐行分隔。
- `beginStructureBlock` 之后仅描述结构所需方块与仓口：材料、数量、等级一致性、朝向、替换要求和控制器位置。不放发电／换热规则、热容量、并行、兼容支持或增殖算法。
- 方块数量使用 `addCasing(count, name, isTiered)`；无法用原生方法表达的方块要求才使用 `addStructureInfo`。外壳按最大数量描述：固定外壳位置与所有允许该外壳替换的仓口位置全部计入，并标明「最大数量，含可替换仓口位置」；无外壳回退的专用仓口和控制器不计入外壳数量。数量以实际结构为准。
- 普通仓口使用带数量和提示编号的 `add*Hatch`／`add*Bus`；专用仓口使用 `addMiscHatch`。数量区分必需、可选、成对安装与模式条件，不把运行条件误写为成型条件。
- 仓口位置写「提示方块 n」及必要替换要求，编号必须对应结构定义的 `.hint(n)`，不以源码字母代替玩家可见提示编号。`addStructureHint` 仅补充可替换方块名称，不把机制说明合并到同编号提示行。

## 颜色与本地化

- 数值局部使用金色 `§6`，之后恢复正文颜色；普通说明恢复灰色 `§7`，`addCasing` 的方块名称恢复白色 `§f`。原生方法已着色的数量不重复包装。
- 分组标题使用青色 `§b`，颜色不扩散到后续正文。不整段高亮、不硬编码着色数值的新平衡。
- 多安培／激光仓支持提示放在最后一条主机 info，即 `beginStructureBlock` 之前，使用原版风格的绿色 `§a`；中文统一为「支持 多A能源仓|动力仓 激光仓」，写完恢复正文颜色。仅在已实现相应兼容的机器上添加，不据此扩展仓口接入逻辑。
- 中英文使用对应语言键，每行独立调用，不在 lang 值中写字面 `\n`。支持项、数值与模式说明核对源码及配置；配置值变化时不得继续展示失真的固定值。

## 当前范例与参考边界

当前核电站、热交换站与大型蒸汽涡轮机均按本规范组织 Tooltip。不同运行机制分组范例：[核电站 `createTooltip()`](../../../src/main/java/com/lin/thermonuclear/machine/MTENuclearPowerPlant.java)；共用公式与流体接口范例：[热交换站](../../../src/main/java/com/lin/thermonuclear/machine/MTEHeatExchangeStation.java)和[大型蒸汽涡轮机](../../../src/main/java/com/lin/thermonuclear/machine/MTELargeSteamTurbine.java)。后续新增或修改机器 Tooltip 均遵循本规范。

GT5U 参考本项目缓存 `5.09.54.133` 的 `MultiblockTooltipBuilder` 与 `MTELargeBoiler`；GTNL 参考 `common/machine/multiblock/NuclearReactor.java`、`wireless/FastNeutronBreederReactor.java` 的结构／仓口组织，以及 `WhiteNightGenerator.java` 的主题分隔写法。GTNL 声明版本较新，方法签名以本项目实际依赖为准；仅采用组织方式，不复制其数值、资源或扩展基类。访问方式见 [GTNL 参考索引](../references/GT-Not-Leisure参考索引.md)。

格式与语言键检查不替代游戏内宽度、颜色及提示投影验收。
