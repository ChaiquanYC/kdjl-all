# KDJL Backend

口袋精灵2 后端服务 — Spring Boot 3.3.5 + JPA + MySQL

> PHP 5 + Memcached → Java 21 + Spring Boot 3.x 迁移

---

## 模块结构

| 模块 | 端口 | 说明 |
|------|------|------|
| `kdjl-common` | — | 共享实体(63个) + DTO |
| `kdjl-server` | 8080 | 游戏API服务 |
| `kdjl-admin` | 8081 | 后台管理系统(Thymeleaf) |

## 快速启动

```bash
# 1. 导入数据库
mysql -u root -p kdjl < db/kdjl_mysql8_compatible.sql

# 2. 编译
mvn install -pl kdjl-common -DskipTests

# 3. 启动游戏服务 (:8080)
mvn spring-boot:run -pl kdjl-server

# 4. 启动后台管理 (:8081)
mvn spring-boot:run -pl kdjl-admin
```

## 账号

| 系统 | 地址 | 用户 | 密码 |
|------|------|------|------|
| 游戏API | http://localhost:8080 | testuser | test123 |
| 后台管理 | http://localhost:8081 | admin | admin123 |
| 数据库 | localhost:3306/kdjl | kdjl | kdjl_pass |

## 技术栈

- Java 21 + Spring Boot 3.3.5
- Spring Data JPA + Hibernate
- Spring Security + JWT (HMAC-SHA384)
- Spring WebSocket (STOMP聊天)
- MySQL 8.0 (117表)
- Maven 3

## 系统概览

| 系统 | 说明 |
|------|------|
| 战斗引擎 | 即时回合制/技能/装备特效/自动战斗/捕捉 |
| 副本系统 | 10副本波次/通天塔55层/挑战模式/组队副本/神圣地图 |
| 社交系统 | 好友/组队/PvP/公会/婚姻/传承/拍卖行 |
| 经济系统 | 商店/背包/仓库/道具使用(29/36效果) |
| 后台管理 | 玩家管理/道具发放/封禁/消费统计/战斗日志 |

## 项目结构

```
kdjl-backend/
├── kdjl-common/         # 共享模块 (63实体)
├── kdjl-server/         # 游戏服务 :8080
│   ├── controller/      # 23个Controller
│   ├── service/         # 18个Service
│   ├── repository/      # 19个Repository
│   ├── battle/          # 战斗会话管理
│   ├── security/        # JWT认证
│   └── websocket/       # STOMP聊天
├── kdjl-admin/          # 后台管理 :8081
│   ├── controller/      # PageController + AdminApiController
│   ├── service/         # AdminService
│   ├── repository/      # 8个Admin Repository
│   └── templates/       # Thymeleaf页面
├── db/                  # 数据库SQL (117表)
└── docs/                # 27份技术文档
```

## 文档

详见 `docs/` 目录，包括：
- [运维部署手册](docs/OPERATIONS.md)
- [迁移进度](docs/PROGRESS.md)
- [开发路线图](docs/ROADMAP.md)
- [道具使用系统](docs/ITEM-USAGE-SYSTEM.md)
- [副本类型对齐](docs/DUNGEON-TYPES-FIXPLAN.md)

## 前端

前端React代码：[kdjl-frontend](https://github.com/ChaiquanYC/kdjl-frontend)
