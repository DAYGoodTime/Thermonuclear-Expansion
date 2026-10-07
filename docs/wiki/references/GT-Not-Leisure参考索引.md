# GT-Not-Leisure 本地代码参考索引

> 创建日期: 2026-09-30 12:35
> 最后更新: 2026-10-07 11:29
> 作者: DAYGood_Time
> 状态: 长期维护

## 参考项目与使用约定

用户指定只读仓库 `D:\DEV\mcmod\GT-Not-Leisure`（GTNL）。**功能开发前先查相关实现**，覆盖机器、仓口、配方、流体、GUI、同步、NBT 与兼容；纯文档／格式整理不要求查机器实现。它不是本项目源码、子模块或依赖，不自动复制内容、复用 ID／数值、添加依赖或运行其构建。

本索引是 **2026-09-30 快照**：HEAD `e9f1ebbc7be7d1009829f950a654b61b48ab3c2c`，当时有未提交修改；origin 为 `https://github.com/DAYGoodTime/GT-Not-Leisure.git`，upstream 为 `https://github.com/ABKQPO/GT-Not-Leisure.git`。后续以实际文件为准；目录不可访问须明确报告，索引不等于当次核验。根 `LICENSE.txt` 为 LGPL v3，复制代码／资源前另查文件来源、版权与署名要求。

## 大项目的按需检索流程

1. 按功能主题从下表定位，内容检索限定相关子目录／`*.java`，资源限定 `R`；排除 `.git/build/run/temp/bin`，不整库通读或建全量镜像。
2. 阅读具体方法并追父类、工具、调用、注册和资源，不能只凭类名照搬；对照本项目准确依赖，区分 GTNL 扩展与上游 API。
3. 实现前说明参考路径、可复用思路及版本差异；无对应实现则说明范围与结果再继续。实现后将采用入口与版本写入 Wiki，命令／结果写入验证记录，不借用 GTNL 验收结论。

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

快照检索在 `J` 未定位到名称／关键字直接对应 `ic2hotcoolant` 换热的实现，不断言全库不存在；开发时仍按功能、材料和上游类名核验。`SteamTypes` 的致密超临界／自有压缩蒸汽及网络倍率，不与本项目四级蒸汽一一对应，也不决定流体名或 EU/L。

## 依赖版本差异

| 项目 | GT5U 基线 | 证据 |
| --- | --- | --- |
| Thermonuclear | 已有最终解析记录 `5.09.54.133` | [项目技术基线](../development/项目知识库.md#当前技术基线)；本轮未重跑解析 |
| GTNL 本地参考 | 声明并强制 `5.09.54.183` | 参考仓库 `dependencies.gradle`；本次未运行其依赖解析 |

参考库还强制 ModularUI2 `2.3.91-1.7.10`、GTNHLib `0.11.51`。类／签名／结构／GUI 差异先查最终依赖，不升级全依赖掩盖问题。其他来源边界见[迁入资料说明](迁入资料使用说明.md)。
