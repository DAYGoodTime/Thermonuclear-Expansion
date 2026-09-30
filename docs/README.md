# Thermonuclear Expansion 文档索引

> 创建日期: 2026-09-30 12:23
> 最后更新: 2026-09-30 12:35
> 作者: DAYGood_Time
> 状态: 长期维护

## 开始阅读

1. [项目知识库](wiki/development/项目知识库.md)：项目定位、当前实现、文件入口和未定事项。
2. [迁入资料使用说明](wiki/references/迁入资料使用说明.md)：MessTech 资料的适用范围及 API 核验规则。
3. [Gradle 开发指南](wiki/development/Gradle开发指南.md)：构建环境、验证命令与初始化排障。
4. [仓库开发上下文](../AGENTS.md)：后续编码会话的简要约束。
5. [GT-Not-Leisure 本地参考索引](wiki/references/GT-Not-Leisure参考索引.md)：编写功能前必查的本地代码参考、主题导航与依赖差异。

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

## 修订记录

| 时间 | 作者 | 变更说明 |
| --- | --- | --- |
| 2026-09-30 12:23 | DAYGood_Time | 初始化本项目知识入口，标明迁入资料与规划边界 |
| 2026-09-30 12:28 | DAYGood_Time | 完成 Gradle 指南，记录已验证的初始化、编译与打包结果 |
| 2026-09-30 12:35 | DAYGood_Time | 添加 GTNL 本地参考项目索引和功能开发前检索约束 |
