# GT-Not-Leisure 本地代码参考索引

> 创建日期: 2026-09-30 12:35
> 最后更新: 2026-09-30 12:35
> 作者: DAYGood_Time
> 状态: 长期维护

## 参考项目与使用约定

用户指定本地参考仓库：`D:\DEV\mcmod\GT-Not-Leisure`，简称 GTNL。**编写或修改本项目功能前，先检索并阅读 GTNL 的相关代码，再实施本项目功能。** 该要求覆盖机器、仓口、配方、流体、GUI、同步、存档和兼容功能；纯文档整理和格式调整不要求检索机器实现。

此目录仅作为只读参考，不是 Thermonuclear 的源码目录、Git 子模块或构建依赖。本次没有复制代码、添加依赖、修改参考仓库或运行其构建。

本索引为 2026-09-30 的检索快照：

- 本地 HEAD：`e9f1ebbc7be7d1009829f950a654b61b48ab3c2c`。
- 本地工作区有未提交修改，读取结果不完全等同于该提交；后续核验以当时实际文件为准。
- origin：`https://github.com/DAYGoodTime/GT-Not-Leisure.git`。
- upstream：`https://github.com/ABKQPO/GT-Not-Leisure.git`。
- 根目录 `LICENSE.txt` 为 LGPL v3 文本；引用实现与直接复制代码/资源分开处理，复制前检查具体文件来源、版权声明和许可证要求。

本地绝对路径只适用于具备该目录的开发环境。目录不可访问时明确报告，不能将本索引视为已经完成当次参考核验。

## 大项目的按需检索流程

1. 将待实现功能拆成检索主题，例如“蒸汽识别”“流体消费”“发电仓输出”“结构校验”“NBT”“GUI 同步”。
2. 从下表对应目录按类名/文件名定位，内容搜索限定到相关子目录和 `*.java`；资源搜索限定到 `src/main/resources/assets/sciencenotleisure/`。默认不搜索 `.git/`、`build/`、`run/`、`temp/`、`bin/` 等生成或运行目录。
3. 阅读命中的具体方法，并追踪父类、工具类、调用方、注册入口和语言/结构资源。名称命中只用于定位，不能证明行为相同。
4. 对照 Thermonuclear 当前解析的依赖核验 API，区分 GTNL 自有扩展与上游 GT5U 能力。
5. 编写代码前简要说明参考文件、复用思路及不适用部分。没有对应实现时说明检索范围与结果，再依据本项目需求和上游 API 实现。
6. 实现后将实际采用的参考路径、核验版本和测试结果写入对应功能文档或长期 Wiki；不要把 GTNL 的验收结果记作本项目测试。

该流程按需阅读，不要求在每次开发前遍历全部多方块、复制整个参考库或建立全量源码镜像。

## 源码导航

下表路径均相对参考仓库根目录。定义 `J = src/main/java/com/science/gtnl/`，`R = src/main/resources/assets/sciencenotleisure/`；`J`、`R` 只是本文缩写，不是代码变量。

| 主题 | 已核对存在的入口 | 检索用途与边界 |
| --- | --- | --- |
| 模组生命周期 | `J/ScienceNotLeisure.java`、`J/CommonProxy.java` | 追踪初始化和代理分工；注册阶段以真实调用链为准 |
| 机器注册与物品引用 | `J/loader/MachineLoader.java`、`J/utils/enums/GTNLItemList.java` | 机器构造、物品枚举和引用入口；不得照搬注册 ID |
| 方块、物品、材料加载 | `J/loader/BlockLoader.java`、`J/loader/ItemLoader.java`、`J/loader/MaterialLoader.java` | 检查材料与跨模组资源的加载顺序 |
| 多方块公共基类 | `J/common/machine/multiMachineBase/MultiMachineBase.java` | 继承 `MTEExtendedPowerMultiBlockBase`，含自有仓口、处理逻辑和 GUI 依赖；不是可直接放入本项目的独立类 |
| 蒸汽机器基类 | `J/common/machine/multiMachineBase/SteamMultiMachineBase.java` | 蒸汽机器的公共实现入口，具体算法在使用时逐方法核验 |
| 无线能源机器基类 | `J/common/machine/multiMachineBase/WirelessEnergyMultiMachineBase.java` | 无线模式相关案例，不自动成为本项目需求 |
| 单方块蒸汽涡轮 | `J/common/machine/basicMachine/SteamTurbine.java` | 继承 `MTEBasicGenerator`；含蒸汽判定、燃料值、消费量、容量、纹理和 GUI 入口，不是多方块转子涡轮模板 |
| 多方块发电与同步案例 | `J/common/machine/multiblock/SiphonTurbine.java`、`J/common/gui/modularui/SiphonTurbineGui.java` | 无线蒸汽网络取用、EU 缓冲、发电仓、NBT、GUI/Waila 案例；不能据名称视为传统蒸汽涡轮 |
| 锅炉案例 | `J/common/machine/multiblock/structuralReconstructionPlan/LargeBoiler.java` | 包含多个材料等级子类、工作处理与蒸汽输出；不等同于 IC2 热冷却液热交换设备 |
| 太阳能锅炉案例 | `J/common/machine/multiblock/steam/MegaSolarBoiler.java`、`J/common/gui/modularui/MegaSolarBoilerGui.java` | 锅炉与界面实现参考；太阳能不是本项目规划的主热源 |
| 蒸汽分类 | `J/utils/enums/SteamTypes.java` | 流体识别和网络转换映射；其倍率不代表 Thermonuclear 的 EU/L 设计 |
| 仓口 | `J/common/machine/hatch/` | 根据输入、输出、并行、维护等功能继续限定检索 |
| 配方注册与处理逻辑 | `J/loader/RecipeLoader.java`、`J/loader/LoadCompleteRecipeScheduler.java`、`J/common/material/GTNLRecipeMaps.java`、`J/utils/recipes/GTNLProcessingLogic.java` | 追踪配方池、注册调用、处理逻辑与注册时机 |
| 配方实现 | `J/common/recipe/gtnl/`、`J/common/recipe/gregtech/` | 分别检索附属模组配方和上游相关配方 |
| 多方块 GUI | `J/common/gui/modularui/GTNLMultiBlockBaseGui.java`、`J/common/gui/modularui/GTNLSteamMultiBlockBaseGui.java` | 基类界面和同步组织；同时核验 GTNL 自有组件依赖 |
| 结构资源 | `J/utils/StructureUtils.java`、`R/multiblock/` | 资源读取、`.mbs` 结构定义、转置与控制器偏移 |
| 语言与配置 | `R/lang/zh_CN.lang`、`R/lang/en_US.lang`、`J/config/MainConfig.java` | 本地化键、提示和配置入口；数值不自动采用 |
| 兼容与注入 | `J/mixins/` | 只有具体功能确需注入时才追踪目标、加载条件与资源配置 |

## 与当前热核规划相关的边界

本次检索到蒸汽发电、锅炉和流体分类案例，但没有在 `J` 中定位到名称或关键字直接对应 `ic2hotcoolant` 热交换机的实现。这个结果只描述本次限定范围的搜索，不断言参考项目全部目录中不存在相关代码。实现热交换功能时继续按实际功能、材料和上游机器类名搜索，并核对目标 GT5U 的对应实现。

`SteamTypes.java` 包含普通蒸汽、过热蒸汽、致密超临界蒸汽和 GTNL 自有压缩蒸汽，具有网络换算属性。它们不是本项目规划中普通、过热、超临界和超超临界蒸汽的直接一一对应关系，不能据此自动确定流体名称或能量价值。

## 依赖版本差异

| 项目 | GT5U 基线 | 证据 |
| --- | --- | --- |
| Thermonuclear | 最终解析 `5.09.54.133` | 本项目 [Gradle 开发指南](../development/Gradle开发指南.md) 的 `dependencyInsight` 结果 |
| GTNL 本地参考 | 声明并强制 `5.09.54.183` | 参考仓库 `dependencies.gradle`；本次未运行其依赖解析 |

参考仓库还强制 ModularUI2 `2.3.91-1.7.10`、GTNHLib `0.11.51` 等版本。这些属于参考项目配置，不自动替换本项目依赖。遇到找不到类、签名冲突、结构校验差异或 GUI API 差异时，先核对最终解析版本，不通过直接升级本项目全部依赖来掩盖差异。

## 关联入口

- [项目知识库](../development/项目知识库.md)。
- [迁入资料使用说明](迁入资料使用说明.md)。
- [文档索引](../../README.md)。

---

## 修订记录

| 时间 | 作者 | 变更说明 |
| --- | --- | --- |
| 2026-09-30 12:35 | DAYGood_Time | 加入用户指定的只读参考项目，核对源码入口、依赖差异及功能开发前检索流程 |
