# Shortlink

一个基于 Spring Boot 的短链接服务，支持 PostgreSQL 持久化、Redis 缓存/分布式锁，附带复古风前端和浏览器压测面板。

## 快速开始

### 1. 启动依赖
- PostgreSQL（默认账号/库）：`shortlink` / `change-me` / `shortlink`
- Redis：默认无密码，端口 `6379`
- 方案：
  - Docker：`docker compose up -d`（启动 Postgres/Redis）
  - WSL 脚本：`.\scripts\start-local-db.ps1`（在 WSL 内安装并启动 Postgres/Redis）

### 2. 配置
`src/main/resources/application.properties` 默认指向 PostgreSQL/Redis，可用环境变量覆盖：
- `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`
- `DB_POOL_MAX`/`DB_POOL_MIN`（Hikari 连接池，默认 200/20）
- `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`

### 3. 运行
```bash
mvn spring-boot:run    # 或直接运行 ShortlinkApplication
```
启动后：
- API：`POST /api/shorten`，`GET /r/{code}`，`GET /api/info/{code}`
- 前端：`http://localhost:33333/`（复古风界面）
- 浏览器压测面板：`http://localhost:33333/load-test.html`

### 4. 预推送检查
项目使用 husky + Spotless：
- 钩子：`.husky/pre-push` 执行 `mvn -DskipTests spotless:check`
- 单独运行：`mvn -DskipTests spotless:apply`（自动格式化）

## 压测提示
- 浏览器面板受同域连接数限制，想达到高并发/QPS 请用命令行工具（如 wrk2、vegeta）并逐步升压。
- 示例（wrk2 固定 QPS）：
  ```bash
  wrk -t8 -c400 -d30s -R1000 -s post.lua http://localhost:33333/api/shorten
  ```
  `post.lua` 构造随机 URL 并设置 JSON 头。

## 目录导览
- `src/main/java/...`：核心代码（控制器、服务、缓存配置、分布式锁等）
- `src/main/resources/static/index.html`：复古风首页
- `src/main/resources/static/load-test.html`：浏览器压测面板
- `docker-compose.yml`：Postgres + Redis 容器编排
- `scripts/start-local-db.ps1` / `scripts/stop-local-db.ps1`：WSL 本地依赖启动/停止

## 备注
- H2 仅用于测试（依赖标记为 test 范围），运行时默认使用 PostgreSQL。
- 启动时有依赖自检（PostgreSQL/Redis），连接失败会直接报错，便于早期发现问题。
