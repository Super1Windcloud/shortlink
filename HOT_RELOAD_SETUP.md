# 热重载配置说明

## 当前配置
1. 已添加 `spring-boot-devtools` 依赖
2. 已在 `application.properties` 中配置热重载参数

## 需要启用的功能:
1. IDEA 编译器自动构建
2. 运行配置中启用 "Build project automatically"

## 在 IDEA 中启用热重载的步骤:

### 1. 启用自动编译
- 按 `Ctrl+Shift+A` 打开 "Find Action"
- 搜索 "Registry"
- 找到 `compiler.automake.allow.when.app.running` 并勾选

### 2. 启用构建项目自动
- 进入 `File` -> `Settings` -> `Build, Execution, Deployment` -> `Compiler`
- 勾选 "Build project automatically"

### 3. 启动应用
- 运行 `ShortlinkApplication` 类
- 任何保存的 Java 文件更改将自动触发重新加载

## 验证热重载是否工作
修改任何控制器、服务或配置类，观察控制台是否出现 "Restarting" 信息。

## 注意事项
- 热重载只适用于运行中的 Spring Boot 应用
- 大部分更改会通过热重载生效，但某些类级别的更改可能仍需重启