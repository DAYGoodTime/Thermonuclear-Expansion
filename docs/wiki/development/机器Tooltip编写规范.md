# 机器 Tooltip 编写规范

> 创建日期: 2026-10-12
> 作者: DAYGood_Time
> 状态: 当前生效

## 内容分工

- 主说明页描述运行模式、操作、容量、并行、机制与特性支持；三台生产主机通过 `addMarkdown` 加载中英文模板，其他逐行说明仍可使用 `addInfo`。原型声明放在开头；运行机制不同的模式独立成组，分别写清适用的发电／发热公式、变量、配置倍率与启动／寿命限制。仅流体类型、系数或准入限制不同且机制相同的模式合并说明，共用公式只写一次，分别列出系数与限制；热交换站与大型蒸汽涡轮机的蒸汽类型按此规则合并。主题之间使用模板 `{gray:{hr}}` 或原生 `addSeparator()`，不手写横线、不逐行分隔。
- `beginStructureBlock` 之后仅描述结构所需方块与仓口：材料、数量、等级一致性、朝向、替换要求和控制器位置。不放发电／换热规则、热容量、并行、兼容支持或增殖算法。
- 方块数量使用 `addCasing(count, name, isTiered)`；无法用原生方法表达的方块要求才使用 `addStructureInfo`。外壳按最大数量描述：固定外壳位置与所有允许该外壳替换的仓口位置全部计入，并标明「最大数量，含可替换仓口位置」；无外壳回退的专用仓口和控制器不计入外壳数量。数量以实际结构为准。
- 普通仓口使用带数量和提示编号的 `add*Hatch`／`add*Bus`；专用仓口使用 `addMiscHatch`。数量区分必需、可选、成对安装与模式条件，不把运行条件误写为成型条件。
- 仓口位置写「提示方块 n」及必要替换要求，编号必须对应结构定义的 `.hint(n)`，不以源码字母代替玩家可见提示编号。`addStructureHint` 仅补充可替换方块名称，不把机制说明合并到同编号提示行。
- 运行机制不同的模式独立分组并写明对应发电／发热公式；仅流体类型、系数或准入限制不同且机制相同的模式合并说明，共用公式只写一次。多安培／激光仓支持提示为绿色，放在最后一条主机 info，中文统一为「支持 多A能源仓(动力仓) 激光仓 」括号内的根据机器的属性更换描述。结构外壳数量按最大数量统计，包含全部允许替换为该外壳的仓口位置，不计控制器及无外壳回退的专用仓口，并标明最大数量。

## 颜色与本地化

- 模板数值使用局部 `{gold:内容}`，标题使用 `{aqua:内容}`，支持提示使用 `{green:内容}`；作用域结束后自动恢复外层灰色正文。Builder／语言键仍使用原有颜色码：数值金色 `§6`、标题青色 `§b`、支持提示绿色 `§a`，随后恢复灰色 `§7`；`addCasing` 的方块名称恢复白色 `§f`。原生方法已着色的数量不重复包装，不整段高亮、不硬编码着色数值的新平衡。
- 多安培／激光仓支持提示放在最后一条主机 info，即 `beginStructureBlock` 之前，使用原版风格的绿色 `§a`；中文统一为「支持 多A能源仓|动力仓 激光仓」，写完恢复正文颜色。仅在已实现相应兼容的机器上添加，不据此扩展仓口接入逻辑。
- 模板中英文各提供完整文件，每行对应一行 Tooltip；结构页及其他语言键说明每行独立调用，不在 lang 值中写字面 `\n`。支持项、数值与模式说明核对源码及配置；配置值变化时不得继续展示失真的固定值。

## 主说明模板

- 三台主机模板位于 `src/main/resources/assets/thermonuclear/lang/{en_US,zh_CN}/tooltip/`，文件名分别为 `nuclear-power-plant.md`、`heat-exchange-station.md`、`large-steam-turbine.md`。Java 传入 `new ResourceLocation(Thermonuclear.MODID, "文件名（无后缀）")`，不包含 `lang/`、语言目录或 `.md`。
- 数值仍来自 Java：核电站容量／并行来自 `BlockFuelRod`，融毁延迟来自 `NuclearMeltdownTimer`，模块上限来自结构规则；换热系数来自换热规则，涡轮能值／启动时间来自燃料定义与配置。通过变量表传入模板，整数先用 `GTUtility.formatNumber`，小数沿用机器公共 `decimal`，模板用 `{var:变量名}` 插入。不把机器实例的实时状态放入缓存 Tooltip。
- 仅使用本地 GT5U `5.09.54.205` 支持的类 Markdown 命令；它不是标准 Markdown，不使用标题、表格或链接排版，也不使用上游 master 才有的条件、引用、语义配色与自动数值格式化。
- 客户端按当前语言读取资源并允许资源包覆盖；发生 I/O 错误时整份回退到 `en_US`，语法错误不会回退。独立服务器读取类路径英文资源，因此英文文件必须完整。客户端资源重载会清除多方块 Tooltip 缓存，下次访问重新加载。
- `addMarkdown` 只追加主说明页；结构、仓口、通道继续使用原生 Builder。仍被结构页使用的核电扩展语言键保留在 `.lang`，不因迁移主说明而删除。

## 当前范例与参考边界

当前核电站、热交换站与大型蒸汽涡轮机均按本规范组织 Tooltip。不同运行机制分组范例：[核电站 `createTooltip()`](../../../src/main/java/com/lin/thermonuclear/common/machine/nuclear/MTENuclearPowerPlant.java)；共用公式与流体接口范例：[热交换站](../../../src/main/java/com/lin/thermonuclear/common/machine/MTEHeatExchangeStation.java)和[大型蒸汽涡轮机](../../../src/main/java/com/lin/thermonuclear/common/machine/MTELargeSteamTurbine.java)。后续新增或修改机器 Tooltip 均遵循本规范。

GT5U 参考本项目当前依赖 `5.09.54.205` 的 `MultiblockTooltipBuilder`、`MarkdownTooltipLoader` 与 `MTECleanroom#createTooltip()`。GTNL 的结构／仓口组织与主题分隔写法仅供组织参考，具体参考路径见 [GTNL 参考索引](../references/GT-Not-Leisure参考索引.md#tooltip-与仓口)；方法签名以本项目实际依赖为准，仅采用组织方式，不复制其数值、资源或扩展基类。

格式与语言键检查不替代游戏内宽度、颜色及提示投影验收。
