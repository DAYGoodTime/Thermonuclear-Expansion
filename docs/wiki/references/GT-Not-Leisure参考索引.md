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
| 仓口 | `J/common/machine/hatch/` | 根据输入、输出、并行、维护等功能继续限定检索 | — |
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
- 周期交货按同版本 `runMachine()`、`addItemOutputs()`、`addFluidOutputs()` 与 `stopMachine()` 核对：事务提交只填充原生待产物，最终入口重新建立输出 helper，完成后由 GT 清空缓存；不采用 GTNL 自定义调度。GUI 参考 `J/common/gui/modularui/GTNLMultiBlockBaseGui.java` 的父类扩展方式，本项目直接复用 `.205` 的 `MTEMultiBlockBaseGui.createTerminalTextWidget()`，保留完整原生诊断与产量同步，不复制参考库布局或额外接口。

### 兼容与注入

- 加载入口：`J/asm/GTNLEarlyCoreMod.java` 提供 early；`J/mixins/LateMixinLoader.java`（`@LateMixin`／`ILateMixinLoader`）与 `J/mixins/Mixins.java`（带目标模组过滤的注册表）提供 late。
- 资源配置：`R/mixins.sciencenotleisure.json`、`.early`、`.late`，另有 `.early.j17`／`.late.j17`／`.late.j21` 等 Java 版本变体。
- 本项目只采用 early / late 分离与空引导模式，不复制功能注入、ASM 补丁、注册／配置体系或 Java 17/21 分支。

## 与当前热核规划相关的边界

快照检索在 `J` 未定位到名称／关键字直接对应 `ic2hotcoolant` 换热的实现，不断言全库不存在；开发时仍按功能、材料和上游类名核验。`SteamTypes` 的致密超临界／自有压缩蒸汽及网络倍率，不与本项目四级蒸汽一一对应，也不决定流体名或 EU/L。

## 依赖版本差异

| 项目 | GT5U 基线 | 证据 |
| --- | --- | --- |
| Thermonuclear | 已重跑解析，选中 `5.09.54.205` | [项目技术基线](../development/项目知识库.md#当前技术基线)；依赖清单对齐 GTNH `2.9.0-RC-2` 后由 `compileClasspath`／`runtimeClasspath` 确认 |
| GTNL 本地参考 | 声明并强制 `5.09.54.205` | 参考仓库 `dependencies.gradle`（`rc2` 提交起）；本次未运行其依赖解析 |

两个项目的依赖清单现同为 GTNH `2.9.0-RC-2` 基线，参考库还强制 ModularUI2 `2.3.92-1.7.10`、GTNHLib `0.11.52`。类／签名／结构／GUI 差异仍先查最终依赖，不升级全依赖掩盖问题。其他来源边界见[迁入资料使用说明](迁入资料使用说明.md)。
