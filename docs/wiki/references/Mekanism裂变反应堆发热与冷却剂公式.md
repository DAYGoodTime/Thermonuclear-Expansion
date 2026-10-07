# Mekanism 裂变反应堆发热与冷却剂公式

> 创建日期: 2026-10-02
> 最后更新: 2026-10-07 11:29
> 作者: OpenCode
> 状态: 外部源码参考，非 Thermonuclear 实现或游戏验收

## 适用范围与来源

原核验针对官方 **`1.21.x` 分支**，不同版本／整合包配置可能改变参数；不直接适用于 GTNH 的 Mekanism，也不作为本项目平衡。此次仅压缩既有资料，未重新核对外部源码或运行游戏。

- [FissionReactorMultiblockData.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/generators/java/mekanism/generators/common/content/fission/FissionReactorMultiblockData.java)：燃烧、储热与转换。
- [GeneratorsConfig.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/generators/java/mekanism/generators/common/config/GeneratorsConfig.java)、[GeneralConfig.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/java/mekanism/common/config/GeneralConfig.java)：配置。
- [HeatUtils.java](https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/java/mekanism/common/util/HeatUtils.java)：热量计算。
- [冷却剂数据](https://github.com/mekanism/Mekanism/blob/1.21.x/src/datagen/generated/mekanism/data/mekanism/data_maps/mekanism/chemical/chemical_attribute_cooled_coolant.json)：钠热变体与焓值。

## 燃烧、储热与沸腾

`burnFuel()` 每 tick 计算：

```text
B实际 = min(B设定, F库存, N组件 × burnPerAssembly)    （mB/t）
Q裂变 = B实际 × energyPerFissionFuel
C反应堆 = 结构位置数量 × fissionCasingHeatCapacity
ΔT = (Q裂变 - 冷却剂带走热量 - 环境散热) / C反应堆
η沸腾 = min(1, 平均燃料组件表面积 / surfaceAreaTarget)
H有效 = η沸腾 × (H储存 - 100°C 对应基础热量)
```

| 配置 | 默认值 |
| --- | ---: |
| `defaultBurnRate` | 0.1 mB/t |
| `burnPerAssembly` | 1 mB/t |
| `energyPerFissionFuel` | 1,000,000 热量/mB |
| `fissionCasingHeatCapacity` | 1,000 / 结构位置 |
| `surfaceAreaTarget` | 4 |

每 mB 燃料发热无随机系数，不因温度、损伤或冷却剂变化；这些因素影响散热和最终温度。四组件燃烧上限为 4 mB/t；0.1/0.5/1/4 mB/t 分别产生 100,000/500,000/1,000,000/4,000,000 热量/t。100 个结构位置、1 mB/t、忽略冷却与环境散热时，温升为 `1,000,000/(100×1,000)=10 K/t`。

热量先进入 `heatCapacitor`，只使用高于 100°C 基准的有效储热。按公式，平均有效表面积达到 `surfaceAreaTarget=4` 时效率为 1；低于该值按比例下降。

## 冷却剂转换

| 路线 | 参数默认值 | 每 tick 理论转换量 | 每 mB 产物实际扣热 |
| --- | --- | --- | ---: |
| 水→蒸汽 | 导热率 0.5、蒸汽能量效率 0.2、`maxEnergyPerSteam`（焓）10 | `floor(H有效×0.5×0.2/10)=floor(H有效×0.01)` | `10/0.2=50` |
| 钠→过热钠 | 导热率默认 1、`thermal_enthalpy=5`、热变体 `mekanism:superheated_sodium` | `floor(H有效/5)` | 约 5 |

导热率限制每 tick 可使用的有效热量，不是转换之后额外消失的热量。相同 `H有效` 下，过热钠与蒸汽**瞬时数量约为 20:1**；这不是多产能量，而是焓值和导热率不同。

忽略环境散热、库存／输出及整数取整限制的理想稳态：

```text
蒸汽产量 ≈ B实际 × 20,000 mB/t
过热钠产量 ≈ B实际 × 200,000 mB/t
```

例如 0.1 mB/t 发热 100,000/t，蒸汽约 2,000 mB/t、过热钠约 20,000 mB/t。**稳态数量比约 10:1，不能与相同储热下的瞬时 20:1 混用。**

实际产量还受基准温度、组件表面积／沸腾效率、热容量、环境散热、冷却剂库存、输出罐、服务器配置、整数取整及燃烧／冷却剂小数余量限制。实现前须重新核验目标 Minecraft、Forge、Mekanism、整合包 JAR 与配置。
