# Gradle 开发指南

> 创建日期: 2026-09-30 12:28
> 最后更新: 2026-09-30 12:28
> 作者: DAYGood_Time
> 状态: 长期维护

## 环境与构建入口

使用仓库的 `gradlew.bat`（Windows）或 `./gradlew`，不使用系统 Gradle 替代 Wrapper。当前 Wrapper 为 `9.4.0`，`.java-version` 指定 JDK `25`。本次验证环境为 Windows、Zulu OpenJDK `25.0.2`。

构建配置由以下文件共同组成：

- `settings.gradle.kts`：插件仓库与 GTNH settings convention `2.0.33`。
- `build.gradle.kts`：Elytra convention `v1.1.1` 和 GTNH project convention。
- `gradle.properties`：Minecraft `1.7.10`、Forge `10.13.4.1614`、Jabel、模组包名及生成版本类；配置缓存与并行构建已启用。
- `dependencies.gradle`：GTNH `2.9.0-beta-3` 清单和依赖声明。
- `repositories.gradle`：额外的 glease Maven 仓库，只负责 `net.glease` 组。

Gradle 运行 JDK 与模组字节码目标不是同一概念：当前使用 Jabel 模式，GT5U 依赖报告中的目标 JVM 为 Java 8。不要因 JDK 25 而直接改用仅现代 JVM 可加载的模组字节码。

## 常用命令

在仓库根目录运行：

```powershell
.\gradlew.bat help --console=plain
.\gradlew.bat tasks --all --console=plain
.\gradlew.bat compileJava processResources --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat dependencyInsight --dependency GT5-Unofficial --configuration compileClasspath --console=plain
```

- `help` 与 `tasks`：核验插件应用、脚本配置和任务注册。
- `compileJava processResources`：核验 Java 编译、生成版本类和资源替换。
- `build`：运行编译、Spotless、Checkstyle、JAR 打包和重混淆；产物位于 `build/libs/`。
- `dependencyInsight`：确认最终选中的依赖版本及冲突处理来源。

已注册的开发任务包括 `setupDecompWorkspace`、`runClient`、`runServer`、`runClient25` 和 `runServer25`；本次没有执行游戏启动或服务器验收。IDE 导入时选择根目录 Gradle 项目，并使用 Wrapper 与 JDK 25。

版本由构建插件结合 Git 状态生成。当前未打标签且存在工作区修改，产物文件名带 Git 信息与 `dirty`；不要将本次生成名写死为发布版本。

## 本地调试模组

`dependencies.gradle` 在文件存在时才添加以下本地运行时依赖：

```text
libs/spark.jar
libs/OmniOcularUnofficial.jar
```

本仓库未携带这两个 JAR。缺少它们不会阻止构建；提供真实 JAR 后通过 `rfg.deobf(...)` 加入 `runtimeOnlyNonPublishable`。该机制保留本地调试支持，不把这些文件变成强制构建前置条件。无效或不兼容的 JAR 仍会导致转换失败。

## 初始化与打包故障记录

### 仓库 DSL 引用不存在的 handler

原始错误：

```text
Could not get unknown property 'handler' for object of type
DefaultRepositoryHandler$ExclusiveContentRepositorySpec
```

异常外层指向 `build.gradle.kts` 的 GTNH 插件应用，实际根因在 `repositories.gradle` 的 `exclusiveContent.forRepository` 内。`handler` 不是该闭包的属性。修复为通过显式的项目仓库处理器创建仓库：

```groovy
forRepository {
    project.repositories.maven {
        name = 'glease'
        url = 'https://maven.glease.net/repos/releases/'
    }
}
```

保留 `includeGroup('net.glease')` 独占过滤规则。该问题不需要升级插件、降级 Gradle、删除缓存或取消过滤。

### Spotless 拦截入口注解格式

`Thermonuclear.java` 的单行 `@Mod(...)` 被 `spotlessJavaCheck` 拒绝。按检查输出改为多行注解，属性值及运行行为不变，没有关闭格式检查。

### 缺失本地 JAR 导致 reobfJar 失败

原依赖脚本无条件引用两个 `libs/` 文件。编译可以通过，但 `reobfJar` 解析运行时 classpath 时发生 `DependencyDeobfuscationTransform` 错误。修复为 `localMod.isFile()` 成立时才声明对应依赖。

## 当前非阻塞提示

- 构建提示 GTNH convention `2.0.34` 可用；当前仍使用 `2.0.33`，本次未执行 `updateBuildScript`。
- Spotless 上游配置输出 `indentWithSpaces` 弃用和 Eclipse JDT 版本写法提示；不阻止构建。
- `usesMixins = true`、`mixinsPackage = mixins` 已在迁入配置中开启，但 `src/` 中没有 Mixin 实现。`reobfJar` 输出 `mixins.srg does not exist`，本次任务仍成功。此提示不代表已经有可工作的 Mixin；本次保留了用户的配置。
- 配置缓存会因为文件修改、Git 状态输入变化或动态插件版本缓存到期而重算；出现重新配置消息不等于构建失败。

## 验证结果（2026-09-30）

| 验证 | 结果 / 范围 |
| --- | --- |
| `help` | 通过，初始化错误已消除 |
| `tasks --all` | 通过，RFG 与校验任务已注册 |
| `compileJava processResources` | 通过 |
| `build` | 修复后连续两次通过，含 Spotless、Checkstyle、打包及重混淆 |
| `dependencyInsight` | GT5U 最终选中 `5.09.54.133`；NewHorizonsCoreMod 为 `2.9.61`，其传递 GT5U `5.09.54.129` 被替换 |
| 单元测试 | `test NO-SOURCE`，当前没有测试源码，不能视为已有测试覆盖 |
| 游戏运行 / 整合包兼容 | 未执行 |

生成普通模组 JAR、`-dev.jar` 和 `-sources.jar`。构建成功不代表迁入的全部运行时模组已经共同通过游戏启动测试，也不代表规划中的机器或配方已实现。

## 参考

- [Gradle 9.4.0 仓库内容过滤文档](https://docs.gradle.org/9.4.0/userguide/filtering_repository_content.html)。
- [项目知识库](项目知识库.md)。
- [迁入资料使用说明](../references/迁入资料使用说明.md)。

---

## 修订记录

| 时间 | 作者 | 变更说明 |
| --- | --- | --- |
| 2026-09-30 12:28 | DAYGood_Time | 记录环境、初始化根因、格式及本地 JAR 修复，补充实际构建和依赖解析结果 |
