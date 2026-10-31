# GT-Not-Leisure 本地代码参考索引

> 创建日期: 2026-09-30 12:35
> 作者: DAYGood_Time
> 状态: 长期维护

## 参考项目与使用约定

用户指定只读仓库 `D:\DEV\mcmod\GT-Not-Leisure`（GTNL）。

*如果本地仓库不存在，则读取远程仓库* : https://github.com/DAYGoodTime/GT-Not-Leisure

**功能开发前先查相关实现**，覆盖机器、仓口、配方、流体、GUI、同步、NBT 与兼容；纯文档／格式整理不要求查机器实现。它不是本项目源码、子模块或依赖，不自动复制内容、复用 ID／数值、添加依赖或运行其构建。

## 大项目的按需检索流程

1. 按功能主题从下表定位，内容检索限定相关子目录／`*.java`，资源限定 `R`；排除 `.git/build/run/temp/bin`，不整库通读或建全量镜像。
2. 阅读具体方法并追父类、工具、调用、注册和资源，不能只凭类名照搬；对照本项目准确依赖，区分 GTNL 扩展与上游 API。
3. 实现前说明参考路径、可复用思路及版本差异；无对应实现则说明范围与结果再继续。实现后将采用入口与版本写入 Wiki，命令／结果写入验证记录，不借用 GTNL 验收结论。

## 源码导航

下表路径均相对参考仓库根目录。定义 `J = src/main/java/com/science/gtnl/`，`R = src/main/resources/assets/sciencenotleisure/`；`J`、`R` 只是本文缩写，不是代码变量。

「本项目采用」列指向集中记录的参考条目；主题文档通过这些锚点引用做法，不再各自保留路径清单。

| 主题 | 已核对存在的入口 | 检索用途与边界 | 本项目采用 |
| --- | --- | --- | --- |
| 模组生命周期 | `J/ScienceNotLeisure.java`、`J/CommonProxy.java` | 追踪初始化和代理分工；注册阶段以真实调用链为准 | — |
| 机器注册与物品引用 | `J/loader/MachineLoader.java`、`J/utils/enums/GTNLItemList.java` | 机器构造、物品枚举和引用入口；不得照搬注册 ID | [物品引用与方块注册](#物品引用与方块注册) |
| 方块、物品、材料加载 | `J/loader/BlockLoader.java`、`J/loader/ItemLoader.java`、`J/loader/MaterialLoader.java` | 检查材料与跨模组资源的加载顺序 | [物品引用与方块注册](#物品引用与方块注册) |
| 多方块公共基类 | `J/common/machine/multiMachineBase/MultiMachineBase.java` | 继承 `MTEExtendedPowerMultiBlockBase`，含自有仓口、处理逻辑和 GUI 依赖；不是可直接放入本项目的独立类 | — |
| 蒸汽机器基类 | `J/common/machine/multiMachineBase/SteamMultiMachineBase.java` | 蒸汽机器的公共实现入口，具体算法在使用时逐方法核验 | — |
| 无线能源机器基类 | `J/common/machine/multiMachineBase/WirelessEnergyMultiMachineBase.java` | 无线模式相关案例，不自动成为本项目需求 | — |
| 单方块蒸汽涡轮 | `J/common/machine/basicMachine/SteamTurbine.java` | 继承 `MTEBasicGenerator`；含蒸汽判定、燃料值、消费量、容量、纹理和 GUI 入口，不是多方块转子涡轮模板 | — |
| 多方块发电与同步案例 | `J/common/machine/multiblock/SiphonTurbine.java`、`J/common/gui/modularui/SiphonTurbineGui.java` | 无线蒸汽网络取用、EU 缓冲、发电仓、NBT、GUI/Waila 案例；不能据名称视为传统蒸汽涡轮 | — |
| 锅炉案例 | `J/common/machine/multiblock/structuralReconstructionPlan/LargeBoiler.java` | 包含多个材料等级子类、工作处理与蒸汽输出；不等同于 IC2 热冷却液热交换设备 | — |
| 太阳能锅炉案例 | `J/common/machine/multiblock/steam/MegaSolarBoiler.java`、`J/common/gui/modularui/MegaSolarBoilerGui.java` | 锅炉与界面实现参考；太阳能不是本项目规划的主热源 | — |
| 蒸汽分类 | `J/utils/enums/SteamTypes.java` | 流体识别和网络转换映射；其倍率不代表 Thermonuclear 的 EU/L 设计 | — |
| 仓口 | `J/common/machine/hatch/` | 根据输入、输出、并行、维护等功能继续限定检索 | [大数量流体输入与双仓事务](#大数量流体输入与双仓事务) |
| 配方注册与处理逻辑 | `J/loader/RecipeLoader.java`、`J/loader/LoadCompleteRecipeScheduler.java`、`J/common/material/GTNLRecipeMaps.java`、`J/utils/recipes/GTNLProcessingLogic.java` | 追踪配方池、注册调用、处理逻辑与注册时机 | [配方注册](#配方注册) |
| 配方实现 | `J/common/recipe/gtnl/`、`J/common/recipe/gregtech/` | 分别检索附属模组配方和上游相关配方 | [配方注册](#配方注册) |
| 多方块 GUI | `J/common/gui/modularui/GTNLMultiBlockBaseGui.java`、`J/common/gui/modularui/GTNLSteamMultiBlockBaseGui.java` | 基类界面和同步组织；同时核验 GTNL 自有组件依赖 | — |
| 结构资源 | `J/utils/StructureUtils.java`、`R/multiblock/` | 资源读取、`.mbs` 结构定义、转置与控制器偏移 | [多方块组织与多批燃料](GTNL多方块组织与多批燃料.md) |
| 多方块组织 | `J/common/machine/multiblock/PCBFactory.java`、`J/common/machine/multiblock/NaquadahReactor.java` | 主／附加结构片段、分段检查与建造、输入处理与输出提交分离 | [多方块组织与多批燃料](GTNL多方块组织与多批燃料.md) |
| Tooltip | `J/common/machine/multiblock/NuclearReactor.java`、`J/common/machine/multiblock/wireless/FastNeutronBreederReactor.java`、`J/common/machine/multiblock/WhiteNightGenerator.java` | 结构／仓口提示组织与主题分隔写法；签名以本项目依赖为准 | [Tooltip 与仓口](#tooltip-与仓口) |
| 语言与配置 | `R/lang/zh_CN.lang`、`R/lang/en_US.lang`、`J/config/MainConfig.java` | 本地化键、提示和配置入口；数值不自动采用 | — |
| 兼容与注入 | `J/asm/GTNLEarlyCoreMod.java`、`J/mixins/LateMixinLoader.java`、`J/mixins/Mixins.java`、`R/mixins.sciencenotleisure{,.early,.late}{,.j17,.j21}.json` | 只有具体功能确需注入时才追踪目标、加载条件与资源配置 | [兼容与注入](#兼容与注入) |

## 集中参考条目

以下各节收录本项目已从 GTNL 采用的参考做法。主题文档通过锚点引用这些结论，不再各自保存路径清单；新建条目须在[参考索引](README.md) 登记。

### 物品引用与方块注册

- 方块／物品注册分层：`J/common/block/casings/{base,column}/` 与 `J/common/item/items/fuelRod/RadioactiveItem.java`。只采用职责划分，不复制其元数据表、构造时注册或辐射机制。
- 统一物品引用：`J/utils/enums/GTNLItemList.java` 与 `J/loader/{MachineLoader,ItemLoader}.java` 的简单枚举、Loader 绑定、副本读取与物品比对。本项目按 GT5U `.205` 核对并使用 `GTUtility.copyAmountUnsafe`／`areUnificationsEqual`：允许重新绑定，无效绑定记警告；不引入备用物品、公开模板或额外注册逻辑，未绑定读取仍报错。燃料等级与增殖路线维护业务映射，不放入 ItemList。
- 方块提示列表：`J/common/block/casings/base/ItemBlockBase.java` 与 `J/utils/item/MetaTooltipUtils.java`。本项目按语言键存储说明，不复制其预翻译字符串和元数据映射。
- 扳手采集与机器更新：`J/common/block/casings/casing/MetaBlockCasingBase.java`。实际采用 GT5U `.205` 的 `BlockCasingsAbstract` 二级采集要求，以及 `BehaviourWrench` 调用 Forge 旋转接口的原生路径，不复制工具交互逻辑。

### 配方注册

- 配方池、注册调用与处理逻辑入口：`J/loader/RecipeLoader.java`、`J/loader/LoadCompleteRecipeScheduler.java`、`J/common/material/GTNLRecipeMaps.java`、`J/utils/recipes/GTNLProcessingLogic.java`。本项目沿用 load-complete 注册。
- 电路区分与螺丝刀转换：`J/common/recipe/gregtech/AssemblerRecipes.java`、`J/common/recipe/gtnl/CraftingTableRecipes.java`，用于增殖输入／输出总线的组装机与合成台配方。
- 双流体搅拌机配方：`J/common/recipe/gregtech/MixerRecipes.java`，用于复合冷却液制造；配方表与机器调用按本项目选中的 GT5U 核验。
- 换热类型组织：`J/utils/enums/SteamTypes.java` 只采用组织思路，不复用其蒸汽体系、倍率或自有 API。
- 不迁入参考库的自有配方工具类、`RecipeBuilder` 变体或工具箱扩展。

### Tooltip 与仓口

- 结构／仓口提示组织：`NuclearReactor.java`、`wireless/FastNeutronBreederReactor.java`；主题分隔与分组写法：`WhiteNightGenerator.java`。
- 编号语言键与逐条 `addInfo`：`SiphonTurbine.createTooltip()`。
- 主说明模板迁移前核对 `NuclearReactor#createTooltip()` 与 `WhiteNightGenerator#createTooltip()`，这两个入口仍采用逐行 `addInfo`／`addSeparator`；本项目只沿用主题分组与结构页分工，模板 API 及语法按 GT5U `.205` 的 `MTECleanroom`、`MultiblockTooltipBuilder` 和 `MarkdownTooltipLoader` 实现，不从参考库复制文本或资源。
- 解析受限的 `SiphonTurbineGui` 同步显示方式仅参考实现思路，本项目沿用既有 info 接口。以上签名均以本项目实际依赖为准，不复制数值、资源或扩展基类。
- GUI 终端参考 `common/gui/modularui/SiphonTurbineGui.java` 的服务端数据同步与客户端文本显示分离；其固定字段不适用于本项目条件行。本项目按 ModularUI2 `2.3.92-1.7.10` 的 `GenericListSyncHandler`／`TextWidget` 核对完整有序快照同步与动态多行文本尺寸更新，保留客户端本地化，不复制参考库的机器状态 setter。

### 无线与电网接入

`NaquadahReactor.java` 的无动力仓判定与所有者入网、`LargeEssentiaGenerator.java` 的右键升级，以及 `common/item/items/WirelessUpgradeChip.java` 的物品用途，用于无线升级芯片与自动入网。本项目直接按 GT5U 核对 `WirelessNetworkManager`、右键与掉落 NBT API 后调用 GT 电网，不依赖 GTNL，也不采用其批次入网与自有无线接口。

### 周期处理与事务边界

- 参考 `J/common/machine/multiblock/NaquadahReactor.java` 的 `checkProcessing()`、`onRunningTick()` 与 `outputAfterRecipe()`，并追至父类 `J/common/machine/multiMachineBase/MultiMachineBase.java`。采用输入处理与产物提交分离的思路，不复制其配方逻辑、批次无线输出或数值；这两个入口没有本项目所需的独立 `CycleContext` 实现。
- API 按本地 GT5U `5.09.54.205` 的 `MTEMultiBlockBase.checkRecipe()`、`startRecipeProcessing()`、`endRecipeProcessing()` 核对：原生入口先开输入事务，再调用处理逻辑，末尾仓口结算仍可改变处理结果。本项目在原生结算成功后提交产物，失败时撤销内部状态与未执行动作；不能撤回其他仓口已经完成的外部扣料或已开始的产物提交。保留原周期与扩展 NBT 键；未运行参考库构建。
- 周期交货按同版本 `runMachine()`、`addItemOutputs()`、`addFluidOutputs()` 与 `stopMachine()` 核对：事务提交只填充原生待产物，最终入口重新建立输出 helper，完成后由 GT 清空缓存；不采用 GTNL 自定义调度。GUI 参考 `J/common/gui/modularui/GTNLMultiBlockBaseGui.java` 的父类扩展方式；当前产物显示改用本模组有界摘要，诊断与按钮继续使用 GT 接口，见[GUI 产物显示与网络边界](#gui-产物显示与网络边界)。

### 兼容与注入

- 加载入口：`J/asm/GTNLEarlyCoreMod.java` 提供 early；`J/mixins/LateMixinLoader.java`（`@LateMixin`／`ILateMixinLoader`）与 `J/mixins/Mixins.java`（带目标模组过滤的注册表）提供 late。
- 资源配置：`R/mixins.sciencenotleisure.json`、`.early`、`.late`，另有 `.early.j17`／`.late.j17`／`.late.j21` 等 Java 版本变体。
- 本项目只采用 early / late 分离与空引导模式，不复制功能注入、ASM 补丁、注册／配置体系或 Java 17/21 分支。

### 大数量流体输入与双仓事务

- 参考 `J/common/machine/hatch/SuperInputHatchME.java` 的配置限额、原始影子栈与差值结算，以及 `SuperDualInputHatchME.java` 的事务开启、`inventories()` 和结束结算；`J/utils/machine/FluidTankG.java` 将大数量拆成多个原始流体栈。本项目仅采用来源适配与保留原始事务引用的思路，不复制其实现、资源或机器注册；参考仓库许可证为 GPL-3.0。
- `J/api/IRecipeProcessingAwareDualHatch.java` 不继承 GT 的 `IRecipeProcessingAwareHatch`，但 `J/mixins/late/gregtech/MixinMTEMultiBlockBaseRecipeProcessing.java` 已在 GT 父类开始／结束方法的 RETURN 注入双仓回调，注册入口为 `J/mixins/Mixins.java`。本模组调用父类即可沿该路径执行，不能重复反射开关事务；运行版本须实际包含并启用该注入。
- 本项目 `2.9.0` 本地清单指定 GT5U `.264`；参考仓库声明 `.205`。按[本地同版本源码](README.md#本地-gt5u-源码)读取 GT 周期、双仓库存和输出 helper 入口，不把源码比对视为实际运行版本验收。本轮不运行依赖解析、构建或测试。
- 大数量沿用 `IDualInputHatch` 暴露的原始物品／流体分片；本模组按 long 汇总并递减原始引用，真实外部提取归仓口结束回调。不使用 GTNL 专用反射、公开来源注册 API 或直接 AE 扣料路径，保留原生可见量和限额。普通 ME 网络身份仅用于重复视图识别，不代替仓口决定可提取量。
- `SuperDualInputHatchME.endRecipeProcessing()` 的本地实现将提取返回量与自身比较，无法拒绝非空的部分提取；该上游缺陷未在本项目修补，也不修改参考仓库。原生结束结果不能被视为跨仓口原子扣料保证。
- 库存候选按 GT5U `.264` 的 `MTEMultiBlockBase.doCheckRecipe()`、`IDualInputHatch.inventories()`／`getSharedItems()` 和 `IDualInputInventory` 核对：同一分区不合并双仓缓冲区，机器的独立流体分区分别选择候选。GTNL `DualInputHatch.inventories()`、`SuperDualInputHatchME.getItr()` 和 `FluidTankG.flat()` 用于核对原始分片引用；不以 `getProxy()` 推断所有双仓都是网络影子库存。
- 普通 ME 物品槽位按 `.264` 的 `MTEHatchInputBusME.getStackInSlot()` 和 `CommonMetaTileEntity.getRealInventory()` 核对；通过原生 `isValidSlot()`／`getCircuitSlot()` 标识实体引用，避免写死虚拟槽偏移，手动／电路槽不参与网络去重。GTNL `SuperInputBusME` 的扩展槽位用于交叉核对，不调用其专用方法。输出按同版 `ItemEjectionHelper`／`FluidEjectionHelper` 的 recipe-check 构造参数及 `GTUtility.saveItem()`／`loadItem()` 核对，大数量物品 Count 由 GT 原生整数协议保存。
- 流体位置的双仓不参与物品收集，也不计入核电站普通燃料总线。候选隔离和统一预留已接入代码，方案见[原生大数量输入输出设计](../../features/2026-10-31-原生大数量输入输出/1-设计.md)；测试及游戏验证由用户执行。

### GUI 产物显示与网络边界

- GTNL 只读参考：`J/utils/machine/FluidTankG.java` 使用多个 int 流体栈承载大数量，`J/common/gui/modularui/GTNLMultiBlockBaseGui.java` 继续委托 GT 父类注册同步；没有本项目所需的产物摘要协议。参考仓库声明 GT5U `.205`，本次运行日志确认本项目使用 `.264`／ModularUI2 `2.3.92`。
- 按本地 GT5U `.264` 核对 `ParallelHelper.addItemsLong()`／`addFluidsLong()`、`FluidEjectionHelper`，原生输出也使用 int 分片；`MTEMultiBlockBaseGui.registerSyncValues()` 传完整数组，`createRecipeInfoWidget()`／`notifyRecipeHandler()` 在显示汇总前再次序列化完整列表。
- 按缓存 ModularUI2 同版本源码核对 `GenericListSyncHandler.read()`、`GenericCollectionSyncHandler.onSetCache()`、`ValueSyncHandler.onValueChanged()` 与 `DynamicSyncHandler.notifyUpdate()`：客户端收到同步也触发变更监听器，启用 C2S 的动态组件会回传列表。GT 该文件使用旧 ModularUI 的 `NetworkUtils` 编解码产物；本模组新协议不依赖这条序列化路径。
- 本地 Forge/Minecraft 源码的 `C17PacketCustomPayload` 在 payload 长度达到 `32767` 字节时抛出 32k 异常，S2C 使用不同上限；不能把栈数预算当作网络字节预算。
- Waila 的 `MTEMultiBlockBase.getWailaNBTData()` 无条件逐片写产物，而正文仅展示前三项。当前本模组通过公开接口维护标量契约，GUI／Waila 共用有界摘要；不迁入 GTNL 代码，不修改上游或增加 Mixin。GT 公开许可为 LGPL-3.0，参考实现只用于核对接口与行为。
- 本次原生接口核对及实现边界见[产物显示摘要同步](../../features/2026-10-31-产物显示摘要同步/1-设计.md)。
- 动态空列参考 `J/common/gui/modularui/EnergyMonitorGui.java` 的 `MonitoringListWidget.dynamicRows`，使用 `coverChildrenHeight(1)`。按 ModularUI2 `2.3.92` 的 `Flow.shouldIgnoreChildSize()` 与 `StandardResizer.applyPosSizeLayout()` 核对：所有子行被折叠时，没有独立高度来源且最小高度为零，会持续报告 SIZING 错误。

## 与当前热核规划相关的边界

快照检索在 `J` 未定位到名称／关键字直接对应 `ic2hotcoolant` 换热的实现，不断言全库不存在；开发时仍按功能、材料和上游类名核验。`SteamTypes` 的致密超临界／自有压缩蒸汽及网络倍率，不与本项目四级蒸汽一一对应，也不决定流体名或 EU/L。

## 依赖版本差异

| 项目 | GT5U 基线 | 证据 |
| --- | --- | --- |
| Thermonuclear | 声明 GTNH `2.9.0`；本地对应清单指定 GT5U `5.09.54.264` | [项目技术基线](../development/项目知识库.md#当前技术基线)；本轮按清单对应源码静态核对，未重跑 `compileClasspath`／`runtimeClasspath` 解析 |
| GTNL 本地参考 | 声明并强制 `5.09.54.205` | 参考仓库 `dependencies.gradle`（`rc2` 提交起）；本次未运行其依赖解析 |

两个项目当前声明基线不同，参考库还强制 ModularUI2 `2.3.92-1.7.10`、GTNHLib `0.11.52`。类／签名／结构／GUI 差异仍先查最终依赖，不升级全依赖掩盖问题。其他来源边界见[迁入资料使用说明](迁入资料使用说明.md)。
