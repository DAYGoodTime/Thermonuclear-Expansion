# Mixin 开发指南

> 创建日期: 2026-09-30 15:31
> 最后更新: 2026-10-07 11:29
> 作者: DAYGood_Time
> 状态: 长期维护

## 当前范围与依赖

当前只有加载入口与三份空 JSON，没有 `@Mixin` 类、静态或动态注入，也无 ASM 补丁。`gradle.properties` 设置 `usesMixins = true`、`mixinsPackage = mixins`、`usesMixinDebug = false`，`mixinPlugin` / `separateMixinSourceSet` 为空。

GTNH convention 自动提供 UniMixins 与注解处理器，不另加一套依赖。已有核验记录为 UniMixins `0.3.1`、处理器 `0.8.7`，本次未重新解析；依赖核验命令见 [Gradle 指南](Gradle开发指南.md)。JSON 使用 `minVersion = 0.8.5-GTNH`、`compatibilityLevel = JAVA_8`；early / late 的 `injectors.defaultRequire = 1` 约束未来注入点匹配，不表示已有注入。

## 文件与加载链

下表 Java 路径相对 `src/main/java/com/lin/thermonuclear/`，JSON 均位于 `src/main/resources/`。

| 文件 | 职责 |
| --- | --- |
| `mixins.thermonuclear.json` | manifest 的 `MixinConfigs` 引导配置；不填 `package`，不收录实际注入 |
| `asm/ThermonuclearCorePlugin.java` | `IFMLLoadingPlugin` / `IEarlyMixinLoader`，提供 early JSON，无 ASM transformer 或访问转换器 |
| `mixins.thermonuclear.early.json` | 包 `com.lin.thermonuclear.mixins.early`；面向 Minecraft、Forge 及早期可见的 CoreMod 类 |
| `mixins/LateMixinLoader.java` | `@LateMixin` / `ILateMixinLoader`，由 UniMixins 发现，不由 `CommonProxy` 调用 |
| `mixins.thermonuclear.late.json` | 包 `com.lin.thermonuclear.mixins.late`；面向普通模组类 |

`coreModClass = asm.ThermonuclearCorePlugin` 由 convention 拼接根包，生成 `FMLCorePlugin: com.lin.thermonuclear.asm.ThermonuclearCorePlugin`，并配置 `FMLCorePluginContainsFMLMod`、`ForceLoadAsMod` 和 MixinTweaker，不重复手写 manifest。普通加载器留在 `mixins` 根包，实际注入使用 `.early` / `.late` 子包，不能将整个根包设为注入包。

## 添加实际注入

1. 按 [GTNL 参考索引](../references/GT-Not-Leisure参考索引.md) 查相关实现，核对目标模组实际依赖与加载时序；不凭类名选择 early / late。
2. 在 `src/main/java/com/lin/thermonuclear/mixins/early/` 或 `late/` 创建实际 `@Mixin` 类，当前这两个目录未创建。不在早期入口依赖机器注册、普通模组主类或 `CommonProxy` 初始化状态。
3. 在对应 JSON 登记相对 `package` 的类名；通用、客户端、服务端类分别放入 `mixins`、`client`、`server`，保持侧隔离。JSON 与加载器动态列表不重复登记同一类。
4. 可选模组或配置开关由加载器选择注入，early 使用 `loadedCoreMods`，late 使用 `loadedMods`。
5. 按 [Gradle 指南](Gradle开发指南.md#常用命令) 格式化、编译和打包，再在真实客户端与专用服务器核验加载和注入点；编译成功不证明运行时正确。

三份 JSON 共用 `mixins.thermonuclear.refmap.json`，由注解处理器生成，不手写。目前无注入输入，已有构建记录未生成 refmap / `mixins.srg`；不伪造空文件掩盖 `mixins.srg does not exist` 提示。

## 参考与验证边界

已采用 GTNL 的 early / late 分离与空引导模式，未复制功能注入、ASM 补丁、注册／配置体系或 Java 17/21 分支。既有参考路径：`src/main/java/com/science/gtnl/` 下的 `asm/GTNLEarlyCoreMod.java`、`mixins/LateMixinLoader.java`、`mixins/Mixins.java`，以及 `src/main/resources/mixins.sciencenotleisure{,.early,.late}.json`。

既有 API 核验对应 UniMixins `0.3.1` sources 中的 `IEarlyMixinLoader`、`ILateMixinLoader`、`LateMixin`，生成行为对应 convention `2.0.33` sources 中的 `MixinModule`、`ToolchainModule`、`GenerateMixinAssetsTask`；外部说明见 [UniMixins 文档](https://github.com/LegacyModdingMC/UniMixins/blob/master/module-gtnhmixins/README.original.md)。本次仅核对本项目源码与 JSON，未重新核验参考仓库或依赖 JAR。

已有静态、编译和打包记录，不代表游戏启动、实际注入或整合包兼容验收。历史命令与结果见[开发验证记录](../../开发验证记录.md#2026-09-30)，初始化前状态见[归档](../archive/development/Mixin初始化前状态.md)，均按需追溯。
