# Mixin 开发指南

> 创建日期: 2026-09-30 15:31
> 最后更新: 2026-10-02 20:06
> 作者: DAYGood_Time
> 状态: 长期维护

## 当前范围与依赖

本项目已初始化 Mixin 加载入口和三份空配置，没有任何 `@Mixin` 类，不改变 Minecraft、Forge 或其他模组行为。`gradle.properties` 保持 `usesMixins = true`、`mixinsPackage = mixins`、`usesMixinDebug = false`；`mixinPlugin` 和 `separateMixinSourceSet` 保持为空。

GTNH convention `2.0.33` 自动提供 UniMixins 及注解处理器；本次 `dependencyInsight --dependency unimixins --configuration compileClasspath` 实际解析为 `io.github.legacymoddingmc:unimixins:0.3.1`，编译输出的 Mixin 注解处理器版本为 `0.8.7`。没有手动添加另一套 Mixin 依赖或升级构建插件。配置使用 `minVersion = 0.8.5-GTNH` 和 `compatibilityLevel = JAVA_8`，与当前 Jabel / Java 8 字节码目标一致。

## 文件与加载链

| 文件（相对仓库根目录） | 职责 |
| --- | --- |
| `src/main/resources/mixins.thermonuclear.json` | convention 自动写入 JAR 的 `MixinConfigs` 清单项；空的引导配置，不填写 `package`，不收录实际注入 |
| `src/main/java/com/lin/thermonuclear/asm/ThermonuclearCorePlugin.java` | 实现 `IFMLLoadingPlugin` 和 `IEarlyMixinLoader`，提供 early JSON；没有 ASM transformer 或访问转换器 |
| `src/main/resources/mixins.thermonuclear.early.json` | 注入包 `com.lin.thermonuclear.mixins.early`；用于 Minecraft、Forge 和在早期已经可见的 CoreMod 类 |
| `src/main/java/com/lin/thermonuclear/mixins/LateMixinLoader.java` | `@LateMixin` 标记的 `ILateMixinLoader`，由 UniMixins 发现，不由 `CommonProxy` 手动调用 |
| `src/main/resources/mixins.thermonuclear.late.json` | 注入包 `com.lin.thermonuclear.mixins.late`；用于其他普通模组的类 |

`gradle.properties` 设置 `coreModClass = asm.ThermonuclearCorePlugin`。convention 将根包拼接后写入 `FMLCorePlugin: com.lin.thermonuclear.asm.ThermonuclearCorePlugin`，同时保留 `FMLCorePluginContainsFMLMod`、`ForceLoadAsMod` 和 MixinTweaker。无需在 `build.gradle.kts` 重复编写 manifest。

引导 JSON 不指定整个 `com.lin.thermonuclear.mixins` 为注入包，加载器保留在此根包中，实际注入分别放入 `.early`、`.late` 子包，避免将普通加载器类归入 Mixin 专用包。early/late JSON 的 `mixins`、`client`、`server` 均为空；两个加载器的 `getMixins(...)` 返回空列表，没有动态追加类。`injectors.defaultRequire = 1` 用于未来注入点匹配校验，不表示目前已有注入。

## 添加实际注入

1. 按 [GTNL 参考索引](../references/GT-Not-Leisure参考索引.md) 检索相关实现，核对目标模组实际解析版本和加载阶段。
2. 在 `src/main/java/com/lin/thermonuclear/mixins/early/` 或 `late/` 下创建实际 `@Mixin` 类。当前没有用占位注入来创建这些目录。
3. 在对应 JSON 填入相对其 `package` 的类名：通用类放入 `mixins`，仅客户端类放入 `client`，仅服务端类放入 `server`。例如 late 包下的 `gregtech.MixinExample` 注册值为 `"gregtech.MixinExample"`；该名称只是格式示例，不是现有类。
4. JSON 静态列表和加载器动态返回列表不重复登记同一个类。涉及可选模组或配置开关时，在加载器增加基于 `loadedMods` / `loadedCoreMods` 的选择逻辑，并核对客户端、服务端隔离。
5. 不在早期入口引用机器注册、普通模组主类或 `CommonProxy` 的初始化状态；目标类通过实际加载时序确定，不以类名判断 early / late。
6. 修改后执行 Wrapper 编译、打包，并在真实客户端与专用服务器验证加载和注入点。编译成功不证明运行阶段或注入目标正确。

所有配置引用 `mixins.thermonuclear.refmap.json`，该文件由注解处理器生成，不手写维护。目前没有 `@Mixin` 输入，构建未生成 refmap / `mixins.srg`；`reobfJar` 的 `mixins.srg does not exist` 提示仍存在，不能通过伪造空映射文件隐藏。

## 验证边界

已有加载器、JSON、manifest、编译与打包的静态验证记录，未执行游戏启动、实际注入、客户端／专用服务器或整合包兼容验收。空配置通过编译不证明未来的注入目标正确。历次命令、退出码和当时的格式阻断见[开发验证记录](../../开发验证记录.md#mixin-指南中的验证记录)，仅按需追溯。

## 已核对参考

本地只读参考 GTNL 的 `src/main/java/com/science/gtnl/asm/GTNLEarlyCoreMod.java`、`src/main/java/com/science/gtnl/mixins/LateMixinLoader.java`、`src/main/java/com/science/gtnl/mixins/Mixins.java` 和 `src/main/resources/mixins.sciencenotleisure{,.early,.late}.json`。采用 early / late 分离和空引导配置模式，未复制其功能注入、注册表、ASM 补丁、配置体系或 Java 17/21 分支。

API 对照本机解析的 UniMixins `0.3.1` sources JAR 中 `IEarlyMixinLoader`、`ILateMixinLoader`、`LateMixin`；manifest 和资源生成行为对照 GTNH convention `2.0.33` sources JAR 中 `MixinModule`、`ToolchainModule`、`GenerateMixinAssetsTask`。外部说明来自 [UniMixins GTNHMixins 文档](https://github.com/LegacyModdingMC/UniMixins/blob/master/module-gtnhmixins/README.original.md)。

初始化前的构建提示记载保留于 [Mixin 初始化前状态](../archive/development/Mixin初始化前状态.md)。
