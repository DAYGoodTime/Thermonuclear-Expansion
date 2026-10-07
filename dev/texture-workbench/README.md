# 方块材质工作目录

> 创建日期: 2026-10-07 14:40
> 最后更新: 2026-10-07 14:40
> 作者: DAYGood_Time
> 状态: 当前材质来源与运行资源映射

用户提供的处理后 PNG 位于 [generated](generated/)。运行时读取 [assets/thermonuclear/textures/blocks](../../src/main/resources/assets/thermonuclear/textures/blocks/)，不会读取本开发目录；两处文件当前按原始字节复制，不转换颜色或分辨率。

## 当前映射

| 方块 | 生成稿相对路径 | 面绑定 |
| --- | --- | --- |
| 低压涡轮轴 | `turbine_shaft/turbine_shaft_side_1.png`、`turbine_shaft_top_bottom_1.png` | 四侧使用 side，上下使用 top_bottom |
| 高压涡轮轴 | `turbine_shaft/turbine_shaft_side_4.png`、`turbine_shaft_top_bottom_4.png` | 四侧使用 side，上下使用 top_bottom |
| 一级至四级燃料棒方块 | `fuel_rod/fuel_rod_{side,top,bottom}_{1,2,3,4}.png` | 各等级分别绑定四侧、顶、底 |

运行资源保留同样的子目录与文件名，共 16 张不透明的 16×16 PNG。方块采用默认立方体渲染、固定竖直朝向，无专属模型、旋转或发光层。

材质修改后须同步复制对应运行资源；当前没有自动同步脚本或构建任务。本轮没有执行 Gradle 资源处理、打包或游戏渲染验收。

## 来源边界

设计草案记录涡轮轴参考 GT++、燃料棒参考 Mekanism。本目录当前仅包含用户交付的处理后 PNG，不含原始参考、生成提示词或许可证文件；处理后资源的来源与再分发授权未在本轮核验，不据此宣称已获第三方授权。

相关：[低压涡轮轴](../../docs/wiki/blocks/低压涡轮轴.md)、[高压涡轮轴](../../docs/wiki/blocks/高压涡轮轴.md)、[燃料棒方块](../../docs/wiki/blocks/燃料棒方块.md)。
