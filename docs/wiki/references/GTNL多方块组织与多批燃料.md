# GTNL 多方块组织与多批燃料

> 创建日期: 2026-10-23
> 状态: GTNL 只读参考条目

本条目集中记录本项目从 GTNL 多方块机器采用的组织方式。来源与检索约定见 [GTNL 参考索引](GT-Not-Leisure参考索引.md)；只采用组织思路，不复制固定布局、机器 ID、数值、源码或资源。

## 独立附加片段与多次 `buildPiece()`

`J/common/machine/multiblock/PCBFactory.java` 用三个结构片段名 `STRUCTURE_PIECE_MAIN`、`STRUCTURE_PIECE_MAIN_T2`、`STRUCTURE_PIECE_MAIN_T3` 分别登记主片段与附加片段，`checkStructure()` 按片段顺序逐段检查，`buildPiece()` 也按片段分段建造与预览。

| 入口 | 组织方式 |
| --- | --- |
| `getStructureDefinition()` | 三个 `addShape(...)` 并列，各片段独立 `transpose` |
| `checkStructure()` | 先 `checkPiece(STRUCTURE_PIECE_MAIN, ...)` 并直接返回，通过后依序检查附加片段 |
| `construction()`／提示 | `buildPiece()` 逐段调用，后段以先前条件为前提 |

对应的本地路径为 `D:/DEV/mcmod/GT-Not-Leisure/src/main/java/com/science/gtnl/common/machine/multiblock/PCBFactory.java`。本项目采用「主结构 + 扩展模块」的同类组织，不使用其固定布局或自有 API。

## 多批燃料：输入处理与输出提交分离

`J/common/machine/multiblock/NaquadahReactor.java` 的 `checkProcessing()`（第 165 行起）先运行 `doCheckRecipe()` 与 `postCheckRecipe(...)`，在注释 `inputs are consumed at this point` 之后统一调用 `updateSlots()`，再计算时长与 EU/t。即输入消耗、输出提交与电量计算分层，输出失败不回滚已提交的输入。

限定 `J/common/machine/` 检索未找到同名工作燃料池：GTNL 没有按批次保存燃料状态的通用实现。本项目只沿用「输入处理与输出提交分离」的思路，批次状态、事务回滚与待输出队列均按 GT5U `.205` 自行实现。

## 使用边界

- GTNL 与本项目依赖清单同为 GTNH `2.9.0-RC-2` 基线，但其声明版本不作准；结构相关 API 以本项目选中的 GT5U 与 StructureLib 为准。
- 以上为早期缓存核对记录，依赖对齐后未逐项重读源码；不据此宣称当前解析或游戏兼容已验收。
