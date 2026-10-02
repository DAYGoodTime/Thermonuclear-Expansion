# Mekanism 裂变反应堆发热与冷却剂公式

> 创建日期: 2026-10-02  
> 最后更新: 2026-10-02  
> 作者: OpenCode  
> 状态: 外部源码核对资料，不代表 Thermonuclear 已实现或已完成游戏验收

## 适用范围

本文根据 Mekanism 官方仓库 `1.21.x` 分支源码整理。Mekanism 不同 Minecraft 版本、配置文件或整合包可能修改这些参数；实际计算应以运行实例的配置和对应版本源码为准。

主要源码入口：

- [FissionReactorMultiblockData.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/generators/java/mekanism/generators/common/content/fission/FissionReactorMultiblockData.java)
- [GeneratorsConfig.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/generators/java/mekanism/generators/common/config/GeneratorsConfig.java)
- [HeatUtils.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/java/mekanism/common/util/HeatUtils.java)
- [GeneralConfig.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/java/mekanism/common/config/GeneralConfig.java)
- [冷却剂数据定义](https://github.com/mekanism/Mekanism/blob/1.21.x/src/datagen/generated/mekanism/data/mekanism/data_maps/mekanism/chemical/chemical_attribute_cooled_coolant.json)

## 计算链

裂变反应堆的计算链为：

```text
燃料组件数量
    -> 限制实际燃烧速率
实际燃烧量
    -> 乘以每单位燃料发热量
裂变产生热量
    -> 加入反应堆热容器
反应堆储热与温度
    -> 按导热率、效率和焓值转换冷却剂
水 -> 蒸汽
钠 -> 过热钠
```

## 核燃料发热量

### 实际燃烧量

`FissionReactorMultiblockData.burnFuel()` 每 tick 计算实际燃烧量：

```text
B实际 = min(B设定, F库存, N组件 × B组件上限)
```

其中：

| 符号 | 含义 |
| --- | --- |
| `B实际` | 本 tick 实际消耗的裂变燃料量，单位 mB/t |
| `B设定` | 反应堆设置的燃烧速率 |
| `F库存` | 当前可用裂变燃料量 |
| `N组件` | 已形成的燃料组件数量 |
| `B组件上限` | 每个燃料组件允许的最大燃烧速率 |

默认配置：

```text
defaultBurnRate = 0.1 mB/t
burnPerAssembly = 1 mB/t
```

因此，若有 4 个燃料组件，组件数量造成的最大燃烧速率为：

```text
4 × 1 = 4 mB/t
```

### 裂变产生的热量

实际燃烧量乘以 `energyPerFissionFuel`：

```text
Q裂变 = B实际 × E燃料
```

默认配置：

```text
energyPerFissionFuel = 1,000,000
```

所以默认情况下：

```text
1 mB 裂变燃料 = 1,000,000 热量单位
```

示例：

| 实际燃烧速率 | 裂变发热量 |
| ---: | ---: |
| `0.1 mB/t` | `100,000/t` |
| `0.5 mB/t` | `500,000/t` |
| `1 mB/t` | `1,000,000/t` |
| `4 mB/t` | `4,000,000/t` |

该计算没有随机系数。温度、损伤和冷却剂种类不会改变单位燃料的发热量，但会影响热量是否能及时被移除，以及反应堆最终温度。

## 热量与温度

裂变热量先加入 `heatCapacitor`。形成反应堆后，默认每个结构位置提供：

```text
fissionCasingHeatCapacity = 1,000
```

反应堆总热容量为：

```text
C反应堆 = 结构位置数量 × fissionCasingHeatCapacity
```

不考虑冷却和环境散热时，温度变化近似为：

```text
ΔT = Q裂变 ÷ C反应堆
```

考虑完整热量收支时：

```text
ΔT = (裂变发热量 - 冷却剂带走热量 - 环境散热) ÷ C反应堆
```

例如结构位置数量为 `100`，实际燃烧速率为 `1 mB/t`：

```text
C反应堆 = 100 × 1,000 = 100,000
Q裂变 = 1 × 1,000,000 = 1,000,000/t
理论温升 = 1,000,000 ÷ 100,000 = 10 K/t
```

实际温升会因为冷却剂和环境散热而低于该理论值。

## 反应堆可用于沸腾的热量

反应堆不会把全部储热直接转换为冷却剂产物。源码先取高于 `100°C` 基准温度的热量，并乘以沸腾效率：

```text
H有效 = η沸腾 × (H储存 - 100°C对应的基础热量)
```

沸腾效率为：

```text
η沸腾 = min(1, 平均燃料组件表面积 ÷ surfaceAreaTarget)
```

默认：

```text
surfaceAreaTarget = 4
```

当平均有效表面积达到目标值的 4 倍时，效率封顶为 `1`；低于目标值时，冷却剂转换效率按比例下降。

## 水转蒸汽

源码使用以下公式：

```text
H水 = H有效 × 水导热率

蒸汽量 = floor(H水 × 蒸汽能量效率 ÷ 水焓值)
```

默认参数：

```text
水导热率 = 0.5
蒸汽能量效率 = 0.2
水焓值 = maxEnergyPerSteam = 10
```

因此：

```text
蒸汽量 = floor(H有效 × 0.01)
```

每生成 `1 mB` 蒸汽，反应堆实际扣除的热量为：

```text
水焓值 ÷ 蒸汽能量效率
= 10 ÷ 0.2
= 50 热量单位
```

水导热率 `0.5` 表示水每 tick 最多使用当前有效热量的一部分，并不是蒸汽转换后额外消失的效率。

## 钠转过热钠

Mekanism 的钠冷却剂数据为：

```json
{
  "hot_variant": "mekanism:superheated_sodium",
  "thermal_enthalpy": 5.0
}
```

未显式定义导热率时，冷却剂 API 默认使用：

```text
钠导热率 = 1.0
钠焓值 = 5.0
```

源码公式：

```text
H钠 = H有效 × 钠导热率

过热钠量 = floor(H钠 ÷ 钠焓值)
```

代入默认值：

```text
过热钠量 = floor(H有效 ÷ 5)
```

也就是每产生 `1 mB` 过热钠，反应堆扣除约 `5` 热量单位。

## 水冷与钠冷对比

在相同的 `H有效` 下：

```text
蒸汽量   ≈ H有效 ÷ 100
过热钠量 ≈ H有效 ÷ 5
```

因此瞬时转换数量约为：

```text
过热钠量 ÷ 蒸汽量 = 100 ÷ 5 = 20
```

理想稳态下，若实际燃烧速率为 `B实际`，默认燃料发热量为 `1,000,000`：

```text
裂变发热量 = B实际 × 1,000,000
蒸汽产量   ≈ B实际 × 20,000 mB/t
过热钠产量 ≈ B实际 × 200,000 mB/t
```

例如 `B实际 = 0.1 mB/t`：

```text
裂变发热量 = 100,000/t
蒸汽产量   ≈ 2,000 mB/t
过热钠产量 ≈ 20,000 mB/t
```

## 影响实际产量的因素

上述产量是便于换算的理想值，实际游戏中还会受到以下因素限制：

- 反应堆当前温度是否高于 `100°C` 基准。
- 燃料组件平均表面积造成的沸腾效率。
- 冷却剂库存是否足够。
- 蒸汽或过热钠输出罐是否有空间。
- 反应堆结构的热容量。
- 反应堆向环境散热。
- `generators` 和 `general` 服务器配置是否被整合包修改。
- 源码中的整数取整，以及燃烧量和冷却剂余量的小数缓存。

## 结论

默认配置下，最重要的换算关系是：

```text
1 mB 裂变燃料 = 1,000,000 热量单位
1 mB 蒸汽     = 约消耗 50 热量单位
1 mB 过热钠   = 约消耗 5 热量单位
```

因此，在相同实际燃烧速率和有效热储量下，钠冷却路线的冷却剂转换数量约为水冷蒸汽路线的 `20` 倍；这不表示钠路线凭空产生更多能量，而是钠的热焓值更低、导热率更高。

## 核对边界

本文核对的是 Mekanism 官方 `1.21.x` 源码。没有将其公式写入 Thermonuclear 实现，也没有声称 Mekanism 公式适用于 GTNH 的 Mekanism 版本或本项目最终平衡。若后续据此实现本项目功能，应重新核对目标 Minecraft、Forge、Mekanism 和整合包版本的实际 JAR 与配置。
