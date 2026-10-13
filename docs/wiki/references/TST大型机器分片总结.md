# TST 大型机器分片总结

> 创建日期: 2026-10-13 23:26
> 作者: lin1s（OpenCode 协助）
> 状态: 外部源码参考

## 来源与范围

来源为 [Nxer/Twist-Space-Technology-Mod](https://github.com/Nxer/Twist-Space-Technology-Mod)，本次查询 `main` 的提交为 `1735ccac7b7bb6b24f9853aeff0cfee5439538d5`。以下结论依据在线阅读的 Java 源码，不代表 Thermonuclear Expansion 已实现分片功能，也不代表已完成编译或游戏验证。

源码目录为 `src/main/java/com/Nxer/TwistSpaceTechnology/common/machine/multiStructureMachine/`：

| 入口 | 职责与重点方法 |
| --- | --- |
| [StructureLoader.java](https://github.com/Nxer/Twist-Space-Technology-Mod/blob/1735ccac7b7bb6b24f9853aeff0cfee5439538d5/src/main/java/com/Nxer/TwistSpaceTechnology/common/machine/multiStructureMachine/StructureLoader.java) | `load()` 读取分片；`getShape()`、`getOffSet()` 查询形状和偏移；`setOffSet()` 设置控制器整体偏移 |
| [GT_TileEntity_MultiStructureMachine.java](https://github.com/Nxer/Twist-Space-Technology-Mod/blob/1735ccac7b7bb6b24f9853aeff0cfee5439538d5/src/main/java/com/Nxer/TwistSpaceTechnology/common/machine/multiStructureMachine/GT_TileEntity_MultiStructureMachine.java) | `checkStructure()` 检查单片；`construct()`、`repair()` 建造和修复；`onPreTick()` 调度 |
| [TST_MegaUniversalSpaceStation.java](https://github.com/Nxer/Twist-Space-Technology-Mod/blob/1735ccac7b7bb6b24f9853aeff0cfee5439538d5/src/main/java/com/Nxer/TwistSpaceTechnology/common/machine/multiStructureMachine/structure/spaceStationModular/TST_MegaUniversalSpaceStation.java) | 空间站案例；`getStructureDefinition()` 注册分片和字符规则，静态块设置整体偏移 |

## 分片与检查机制

每片固定为 `16 × 16 × 16`，包含 4096 个结构字符。每片保存独立原点和形状，并以独立名称注册到同一个 StructureLib 结构定义中：

```java
builder.addShape(mName + i, StructureLoader.getShape(mName, mName + i));
```

分片覆盖结构局部的水平、垂直和深度三个方向。既支持沿单轴排列，也支持水平二维网格和三维分片；X、Z 两个水平方向均能覆盖。分片尺寸为 16 不代表与世界区块边界对齐。`horizontal / vertical / depth` 是机器结构的局部方向，机器朝向变化后的世界坐标转换由 StructureLib 处理。

检查使用片名及三个方向的偏移：

```java
checkPiece(
    mName + checkStructureCount,
    offSet.horizontalOffSet,
    offSet.verticalOffSet,
    offSet.depthOffSet);
```

当前代码每次检查一个完整分片，按编号推进，没有先扫描 X 轴再扫描 Z 轴的逻辑：

- `checkStructureCount` 保存当前片编号；检查成功后递增，到末片后回到 0；失败时停在当前片。
- `InConstruct` 保存待建造或修复的片编号；检查成功时移除，失败时加入。
- 服务端 `onPreTick()` 每 20 tick 调用一次检查；存在待建片时调用 `construct()`，并按集合是否为空设置 `mMachine`。
- `construct()` 从集合取一片，`repair(num)` 指定一片，均用该片偏移调用 `buildPiece()`；成功后移除编号。集合为 `HashSet`，取片顺序没有排序保证。

## 分片结构记录格式

需要记录三类信息：分片原点、分片方块字符、字符到方块或仓口的映射。前两项写入 ZIP 内的文本，映射写在机器类的 `StructureDefinition` 中。

资源位置为：

```text
src/main/resources/assets/gtnhcommunitymod/structure/<machineName>.zip
```

加载器仅打开 ZIP 的第一个条目，使用 UTF-8 读取。文本中每片占两行，连续排列，不包含片间空行或说明文字：

```text
分片0的 depth vertical horizontal 三个整数
分片0的4096个结构字符
分片1的 depth vertical horizontal 三个整数
分片1的4096个结构字符
……
```

上例为格式说明，不能直接作为加载文件。坐标行使用单个空格分隔整数，因为源码采用 `split(" ")`。坐标顺序由加载器读取 `off[2] / off[1] / off[0]` 确定；按局部坐标记作 `z y x`，不能直接代入世界坐标。

例如，一个水平尺寸为 `32 × 32`、高度为 16 的结构拆成四片，其原点记录为：

| 片编号 | 坐标行：`depth vertical horizontal` | 局部位置 |
| --- | --- | --- |
| 0 | `0 0 0` | 起始片 |
| 1 | `0 0 16` | 水平方向移动 16 |
| 2 | `16 0 0` | 深度方向移动 16 |
| 3 | `16 0 16` | 水平和深度均移动 16 |

编号按文本出现顺序生成，片名为传入的片名前缀加编号。当前基类使用 `load(mName, mName)`，因此片名为机器名加 `0、1、2……`。

字符行不能在片内换行。加载器还原为 `String[16][16]`，每条字符串长度为 16：

```java
int t = 256 * i + 16 * j;
shape[i][j] = line.substring(t, t + 16);
```

若已有每片的 `String[16][16] shape`，写出顺序如下：

```java
for (int i = 0; i < 16; i++) {
    for (int j = 0; j < 16; j++) {
        writer.write(shape[i][j]);
    }
}
writer.newLine();
```

加载器没有做转置，导出数组的方向必须与传入 StructureLib 的形状一致。边缘不足 `16³` 的片仍须补齐，补齐字符的检查语义由 StructureLib 约定或元素定义确定；要求空气与不参与检查的区域不能混为一谈。

字符映射在机器类统一定义，例如 `addElement('A', ofBlock(...))`，仓口位置使用对应仓口元素规则。

## 控制器偏移与资源案例

加载前必须通过 `setOffSet(machineName, controllerH, controllerV, controllerD)` 设置控制器相对整体结构原点的偏移。各片检查偏移按以下公式计算：

```text
horizontal = controllerH - 分片坐标行的第3项
vertical   = controllerV - 分片坐标行的第2项
depth      = controllerD - 分片坐标行的第1项
```

该偏移同时用于 `checkPiece()` 和 `buildPiece()`。空间站案例的资源为 [namemegauniversalspacestation.zip](https://github.com/Nxer/Twist-Space-Technology-Mod/blob/1735ccac7b7bb6b24f9853aeff0cfee5439538d5/src/main/resources/assets/gtnhcommunitymod/structure/namemegauniversalspacestation.zip)；本次确认资源路径存在，未解压核对内容。

## 当前实现边界

基类在 `checkStructure()` 上方注明需要优化和重写。`InConstruct` 初始为空，预先填入全部片编号的代码被注释；`checkMachine()` 仅调用一次没有效果的 `InConstruct.isEmpty()`。当前逻辑不能保证首次判定成型前已检查全部分片。

加载器未完整校验坐标、字符行长度及 ZIP 内容；缺失资源和非法格式不一定进入其 `IOException` 处理。当前源码中的语法和 GT5U／StructureLib 调用签名，迁入本项目前须按实际解析依赖核对；本次未验证版本兼容性。

关联入口：[文档索引](../../README.md)、[GT-Not-Leisure 参考索引](GT-Not-Leisure参考索引.md)。
