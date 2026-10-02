# Thermonuclear Expansion 文档索引

## 开始阅读

1. [项目知识库](wiki/development/项目知识库.md)：项目定位、当前实现、文件入口和未定事项。
2. [迁入资料使用说明](wiki/references/迁入资料使用说明.md)：MessTech 资料的适用范围及 API 核验规则。
3. [Gradle 开发指南](wiki/development/Gradle开发指南.md)：构建环境、验证命令与初始化排障。
4. [仓库开发上下文](../AGENTS.md)：后续编码会话的简要约束。
5. [GT-Not-Leisure 本地参考索引](wiki/references/GT-Not-Leisure参考索引.md)：编写功能前必查的本地代码参考、主题导航与依赖差异。
6. [第一阶段原型开发指南](wiki/development/第一阶段原型开发指南.md)：三个控制器、独立结构、公共与专属 GUI、GT 送电链、批准 ID、20 tick 处理、燃料批次与实际验证记录；未进行游戏验收。
7. [Mixin 开发指南](wiki/development/Mixin开发指南.md)：空引导配置、early / late 加载入口、依赖核验、添加注入流程及实际验证边界。
8. [IC2 2.2.828 反应堆燃料棒公式](wiki/references/IC2-2.2.828反应堆燃料棒公式.md)：基于 GTNH 实例 JAR 核对的燃料棒热量、能量、MOX 与相邻组件公式。
9. [Mekanism 裂变反应堆发热与冷却剂公式](wiki/references/Mekanism裂变反应堆发热与冷却剂公式.md)：核对裂变燃料发热量，以及水／钠冷却剂转换为蒸汽／过热钠的公式；不代表本项目已实现。

## 当前代码交付

核电站、固定结构热交换站和大型普通蒸汽涡轮机原型代码已交付，配套双语资源与存档实现；2026-09-30 15:12 最终 Java 编译和资源处理通过，退出码 0。没有制造配方、温度/布局机制、高等级蒸汽或最终平衡。旧的“仅模板入口”说明见[历史状态归档](wiki/archive/development/第一阶段前项目状态.md)。

Mixin early / late 基础配置已初始化，尚无实际注入；2026-09-30 15:31 编译、资源处理、打包、重混淆和 Checkstyle 通过。完整 `build` 被现有原型代码的 Spotless 格式问题阻断，未执行游戏启动；详见 [Mixin 开发指南](wiki/development/Mixin开发指南.md)。

2026-09-30 16:26 已修复共享多方块结构轴向与主机偏移，并为全部当前注册物品添加本模组标识。启用 NEI 提示词搜索后使用 `#thermonuclear`。本次完整 `build` 通过，先前 Spotless 阻断已消除；没有游戏内结构显示、材料数量或搜索验收，详见[原型开发指南](wiki/development/第一阶段原型开发指南.md#结构与物品标识修复验证2026-09-30-1626)。

2026-09-30 18:36 已接入标准处理结果、固定 20 tick 批处理、最低寿命优先且单批最多 256 根的核燃料、独立 GUI 与 Waila 发电量、GT 通用仓室事务。`spotlessApply` 和 `test build` 通过，新增 6 项无游戏环境回归全通过；游戏行为与真实仓室兼容未验收，详见[原型开发指南](wiki/development/第一阶段原型开发指南.md#20-tick-公共处理层验证2026-09-30-1836)。被替换的旧处理约定见[历史归档](wiki/archive/development/20tick改进前原型处理说明.md)。

2026-10-02 已移除类名与主机显示名中的原型标识；三台结构分别移入各自主机，公共 GUI 基类配三个专属子类，送电改用继承的 GT `addEnergyOutput()` → `addEnergyOutputMultipleDynamos()`。格式化、编译、资源处理及源码接线检查通过，注册、存档键和处理公式未变；没有游戏验收。当前工作区无 `src/test/`，早先六项测试属于历史验证。详见[拆分验证](wiki/development/第一阶段原型开发指南.md#主机拆分验证2026-10-02-1237)，拆分前说明见[归档](wiki/archive/development/主机拆分前结构与送电说明.md)。

## 产品规划

| 文档 | 状态 | 说明 |
| --- | --- | --- |
| [GTNH 热核扩展模组规划](../GTNH-Thermonuclear-Expansion-Plan.md) | 规划原稿，包含待确认项 | 热冷却液、热交换、蒸汽涡轮和回收循环；不代表已经实现 |

## 迁入来源资料

原文保留在原路径，以便追溯；它们不是 Thermonuclear 的实现说明。

| 文档 | 来源与用途 |
| --- | --- |
| [knowledge.md](knowledge.md) | MessTech 上下文记录；用于查阅该项目的设计和排障案例 |
| [GT5U-NOTES.md](GT5U-NOTES.md) | MessTech 开发期间的 GT5U 多方块、配方、GUI、同步经验；使用前核验依赖版本 |

## 本地参考项目

| 项目 | 本地路径 | 使用规则 |
| --- | --- | --- |
| [GT-Not-Leisure](wiki/references/GT-Not-Leisure参考索引.md) | `D:\DEV\mcmod\GT-Not-Leisure` | 编写或修改功能前按主题检索并阅读；只读参考，不自动引入依赖 |

## 文档维护

长期开发知识写入 `wiki/development/`，跨项目资料说明写入 `wiki/references/`。新文档加入本索引，并记录创建时间、更新时间、作者和修订记录。现有 `docs/` 已被 Git 跟踪，不添加本地忽略规则。

---
