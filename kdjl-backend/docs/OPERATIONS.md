# KDJL 运维部署手册

> 口袋精灵2 — React + Spring Boot 运维文档 | 2026-05-25

---

## 一、服务架构

| 服务 | 端口 | 技术栈 | 目录 |
|------|------|--------|------|
| 游戏后端 | 8080 | Spring Boot 3.3.5 + JPA + MySQL | `backend/kdjl-server` |
| 后台管理 | 8081 | Spring Boot + Thymeleaf + Spring Security | `backend/kdjl-admin` |
| 游戏前端 | 3001 | React + TypeScript + Vite + Zustand | `frontend/` |

---

## 二、快速启动

### 前置条件
- Java 21
- Maven 3
- Node.js 18+
- MySQL 8+ (数据库: kdjl)

### 启动步骤

```bash
# 1. 数据库 (确保MySQL运行，kdjl库已导入)

# 2. 游戏后端 (端口 8080)
cd backend
mvn install -pl kdjl-common -DskipTests
mvn spring-boot:run -pl kdjl-server

# 3. 后台管理 (端口 8081)  
cd backend
mvn spring-boot:run -pl kdjl-admin

# 4. 前端 (端口 3001)
cd frontend
npm install
npm run dev
```

### 一键启动

```bash
# 后端
cd backend && mvn spring-boot:run -pl kdjl-server &
cd backend && mvn spring-boot:run -pl kdjl-admin &

# 前端
cd frontend && npm run dev
```

---

## 三、账号信息

| 系统 | URL | 用户名 | 密码 | 说明 |
|------|-----|--------|------|------|
| 游戏登录 | http://localhost:3001 | testuser | test123 | 测试账号 |
| 后台管理 | http://localhost:8081 | admin | admin123 | 管理员 |
| 数据库 | localhost:3306/kdjl | kdjl | kdjl_pass | MySQL |

---

## 四、数据库配置

### 连接信息
```
Host: localhost:3306
Database: kdjl
User: kdjl
Password: kdjl_pass
Charset: UTF-8
```

### 关键表

| 表名 | 说明 | 记录数 |
|------|------|--------|
| player | 玩家主表 | 14 |
| userbb | 用户宠物 | 85 |
| userbag | 背包物品 | 1000+ |
| props | 道具定义 | 4609 |
| gpc | 怪物定义 | 1000+ |
| bb | 宠物模板 | 100+ |
| map | 地图定义 | 50+ |
| fuben | 副本进度 | — |
| challenge | 挑战模式 | — |

### 配置文件

每个模块的 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kdjl
    username: kdjl
    password: kdjl_pass
```

---

## 五、后台管理功能

### 访问
`http://localhost:8081` → 登录 `admin / admin123`

### 功能清单

| 页面 | 路径 | 功能 |
|------|------|------|
| 仪表盘 | `/dashboard` | 注册/在线/活跃/宠物/道具统计 + 7日趋势 + 服务器状态 |
| 玩家管理 | `/players` | 搜索→列表→详情(宠物/背包) |
| 玩家详情 | `/players/{id}` | 发道具/金币/元宝/水晶 + 封号/禁言 |
| 道具管理 | `/props` | 4609条分页浏览+名称搜索 |
| 宠物模板 | `/pets` | bb表分页浏览 |
| 消费统计 | `/payments` | 元宝消费排行+最近交易 |

### API端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/stats` | 仪表盘统计 |
| GET | `/api/admin/players` | 搜索玩家 (keyword, page, size) |
| GET | `/api/admin/players/{id}` | 玩家详情+宠物+背包 |
| POST | `/api/admin/give-item` | 发放道具 `{playerId, propId, count}` |
| POST | `/api/admin/give-currency` | 发金币/元宝/水晶 `{playerId, type, amount}` |
| POST | `/api/admin/ban` | 封号 `{playerId}` |
| POST | `/api/admin/unban` | 解封 `{playerId}` |
| POST | `/api/admin/mute` | 禁言 `{playerId, minutes}` |
| POST | `/api/admin/unmute` | 解禁 `{playerId}` |
| GET | `/api/admin/props` | 道具列表 (keyword, page, size) |
| GET | `/api/admin/pets` | 宠物模板 (keyword, page, size) |
| GET | `/api/admin/fight-logs/{playerId}` | 战斗日志 |
| GET | `/api/admin/payment-stats` | 消费统计 |

---

## 六、项目结构

```
kdjl/
├── backend/
│   ├── pom.xml                    # Maven父POM
│   ├── kdjl-common/               # 共享模块 (63实体)
│   │   └── src/main/java/com/kdjl/common/entity/
│   ├── kdjl-server/               # 游戏服务 (8080)
│   │   └── src/main/java/com/kdjl/server/
│   │       ├── controller/        # 23个Controller
│   │       ├── service/           # 18个Service
│   │       ├── repository/        # 19个Repository
│   │       ├── battle/            # 战斗会话管理
│   │       ├── security/          # JWT认证
│   │       └── websocket/         # STOMP聊天
│   └── kdjl-admin/                # 后台管理 (8081)
│       └── src/main/java/com/kdjl/admin/
│           ├── controller/        # PageController + AdminApiController
│           ├── service/           # AdminService
│           ├── repository/        # 8个Admin Repository
│           ├── config/            # SecurityConfig
│           └── resources/
│               ├── application.yml
│               └── templates/     # Thymeleaf页面
├── frontend/
│   └── src/
│       ├── api/                   # Axios客户端
│       ├── stores/                # Zustand (auth + game)
│       ├── components/
│       │   ├── layout/            # GameLayout
│       │   └── game/              # 25个游戏面板
│       └── pages/                 # LoginPage
├── docs/                          # 项目文档 (18份)
│   ├── PROGRESS.md                # 迁移进度
│   ├── ROADMAP.md                 # 开发路线图
│   ├── OPERATIONS.md              # 运维部署手册
│   ├── DATA-MAPPING.md            # PHP→Java数据映射
│   ├── ADMIN-SYSTEM-PLAN.md       # 后台管理规划
│   ├── DUNGEON-TYPES-FIXPLAN.md   # 副本类型对齐
│   ├── ITEM-USAGE-SYSTEM.md       # 道具使用系统
│   ├── ITEM-ISSUES.md             # 道具问题清单
│   ├── BATTLE-SYSTEM.md           # 战斗系统
│   ├── EXP-LEVELING-SYSTEM.md     # 经验升级系统
│   ├── PET-ATTRIBUTES-SYSTEM.md   # 宠物属性系统
│   ├── SKILL-SYSTEM.md            # 技能系统
│   └── EQUIPMENT-EFFECTS-SYSTEM.md # 装备特效引擎
└── images/                        # PHP原始图片资源
```

---

## 七、常用运维命令

### 进程管理
```bash
# 查看端口占用
netstat -ano | grep "8080\|8081\|3001"

# 杀掉进程 (Windows)
taskkill -f -pid <PID>

# 重启游戏服务
cd backend && mvn spring-boot:run -pl kdjl-server
```

### 数据库操作
```bash
# 直接查询
mysql -u kdjl -pkdjl_pass kdjl -e "SELECT * FROM player LIMIT 5"

# 备份
mysqldump -u kdjl -pkdjl_pass kdjl > backup.sql
```

### 日志位置
- 游戏后端: 控制台输出
- 后台管理: 控制台输出
- 前端: 浏览器Console

---

## 八、已实现系统清单

| 系统 | 状态 | 说明 |
|------|------|------|
| 战斗引擎 | ✅ | 即时回合/技能/装备特效/自动战斗/捕捉 |
| 养成系统 | ✅ | 升级/czl属性/技能学习/装备穿戴 |
| 地图系统 | ✅ | 3页35个地点/5种类型/难度选择 |
| 副本系统 | ✅ | 10副本波次推进/冷却+水晶/通天塔/挑战模式 |
| 社交系统 | ✅ | 好友/组队/PvP/公会/婚姻/传承 |
| 经济系统 | ✅ | 商店/背包/仓库/拍卖行 |
| 牧场系统 | ✅ | 寄养/取出/主战/丢弃 |
| 道具系统 | ✅ | 29/36效果已实现 |
| 后台管理 | ✅ | 独立Spring Boot应用+Thymeleaf |
| 文档体系 | ✅ | 18份技术文档 |
