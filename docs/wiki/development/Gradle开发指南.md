# Gradle 开发指南

> 创建日期: 2026-09-30 12:28
> 最后更新: 2026-10-07 11:29
> 作者: DAYGood_Time
> 状态: 长期维护

## 环境与构建入口

在仓库根目录使用 `gradlew.bat`（Windows）或 `./gradlew`，不替换为系统 Gradle。IDE 导入根目录 Gradle 项目，使用 Wrapper 与 `.java-version` 指定的 JDK `25`。当前 `gradle/wrapper/gradle-wrapper.properties` 指向 **Gradle 9.8.0**；旧版构建记录不证明该版本已通过验证。

构建配置入口：`settings.gradle.kts` 管理插件仓库与 settings convention，`build.gradle.kts` 应用 Elytra / GTNH convention，`gradle.properties` 管理模组、Jabel 与生成版本类，`dependencies.gradle` 声明整合包清单及依赖，`repositories.gradle` 配置仅服务 `net.glease` 的 glease 仓库。配置缓存与并行构建已启用；其余技术基线见[项目知识库](项目知识库.md#当前技术基线)。

JDK 25 是构建运行环境，Jabel 保持 Java 8 字节码目标，不等于模组要求现代 JVM。`Tags.VERSION` 由 Gradle 生成，不手写维护；产物版本结合 Git 状态生成，不写死带 Git 信息或 `dirty` 的文件名。

## 常用命令

在仓库根目录运行：

```powershell
.\gradlew.bat help --console=plain
.\gradlew.bat tasks --all --console=plain
.\gradlew.bat spotlessApply --console=plain
.\gradlew.bat compileJava processResources --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat dependencyInsight --dependency GT5-Unofficial --configuration compileClasspath --console=plain
```

| 命令 / 任务 | 用途与边界 |
| --- | --- |
| `help` / `tasks --all` | 检查配置初始化与任务注册，不代表编译或游戏通过 |
| `spotlessApply` | 修改代码后使用 Wrapper 格式化，不手动维护格式结果或关闭检查 |
| `compileJava processResources` | 检查 Java 编译、生成版本类与资源替换 |
| `build` | 编译、Spotless、Checkstyle、打包与重混淆；普通、`-dev`、`-sources` JAR 位于 `build/libs/` |
| `dependencyInsight` | 核对最终选中版本及冲突来源；API 以选中的 JAR 为准 |

开发任务包括 `setupDecompWorkspace`、`runClient`、`runServer`、`runClient25`、`runServer25`，以当前 `tasks --all` 输出为准。启动任务存在不代表客户端或专用服务器已验收。

## 本地调试模组

`dependencies.gradle` 仅在 `localMod.isFile()` 成立时，将 `libs/spark.jar`、`libs/OmniOcularUnofficial.jar` 经 `rfg.deobf(...)` 加入 `runtimeOnlyNonPublishable`。仓库不携带这两个 JAR；缺失时跳过，无效或不兼容时仍会转换失败。

## 排障要点

| 现象 | 检查与处理 |
| --- | --- |
| `Could not get unknown property 'handler'`，外层指向插件应用 | 检查 `repositories.gradle`：`exclusiveContent.forRepository` 内使用 `project.repositories.maven`，保留 `includeGroup('net.glease')`；不靠升级插件、删除缓存或取消过滤解决 |
| `spotlessJavaCheck` 拒绝格式（包括 `@Mod` 注解） | 执行 `spotlessApply` 后重新检查，不关闭 Spotless |
| 编译成功，`reobfJar` 报 `DependencyDeobfuscationTransform` | 检查运行时 classpath 的本地 JAR 是否存在、有效且兼容；保持存在性判断 |
| `mixins.srg does not exist` | 当前没有 `@Mixin` 输入，不伪造 refmap 或空映射；加载配置与注入步骤见 [Mixin 指南](Mixin开发指南.md) |
| 配置缓存重新计算 | 文件、Git 状态或动态插件版本缓存变化会触发重算，本身不等于构建失败 |

插件更新提示、Spotless 的 `indentWithSpaces` 弃用与 Eclipse JDT 版本提示不等于构建失败；以任务结果和退出码判断，不因提示自动升级构建配置。历次故障与验证经过不在正文重复累积。

## 验证边界与测试限制

构建成功仅覆盖执行的任务，不证明游戏启动、物料守恒、性能或整合包兼容，也不证明规划功能已实现。当前没有 `src/test/` 或 JUnit 测试依赖；历史六项辅助测试不是当前测试入口。

无 LaunchWrapper 的普通 JVM 调用 Forge 物品注册会触发 `ModClassLoader` 转换错误；隔离测试的底层数字 ID 绑定不能替代游戏注册验收。恢复测试时使用正常 JUnit 任务，不关闭无测试发现校验。

历次命令、退出码和测试结果见[开发验证记录](../../开发验证记录.md#2026-09-30)，仅按需追溯；不在本指南累积构建日志。

## 参考

- Gradle 官方文档：[依赖诊断](https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html)、[仓库内容过滤](https://docs.gradle.org/current/userguide/filtering_repository_content.html)；构建版本以本仓库 Wrapper 为准。
- [迁入资料使用说明](../references/迁入资料使用说明.md)：来源项目依赖与 API 的核验边界。
