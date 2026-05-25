# KDJL 技术参考

> 项目技术细节、架构、关键约定。下次对话快速加载上下文用。
> 最后更新: 2026-05-22

---

## 项目概述

口袋精灵 (KDJL) — 网页宠物养成对战游戏。
原始: PHP 5 (裸脚本) + MySQL 5.7 + Memcached + Comet/Flash 长轮询 + iframe 导航
目标: React 18 + Spring Boot 3.x + MySQL 9.7 + Redis 7 + WebSocket STOMP

**路径:** `D:\code\kdjl\kdjl\kdjl`  
**117 张数据库表, ~319 个 PHP 源文件, ~78,000 行 PHP 代码**
**测试账号:** `testuser` / `test123` (uid=102, 宠物 id=86)

### 当前运行状态
| 服务 | 端口 | 状态 |
|------|------|------|
| Backend (Spring Boot) | 8088 | ✅ |
| Frontend (Vite) | 3000 | ✅ |
| MySQL | 3306 | ✅ |
| Redis (Docker) | 6379 | ✅ |

---

## 技术栈

| 层 | 原技术 | 新技术 | 版本 |
|----|--------|--------|------|
| 前端 | PHP 渲染 + Prototype/jQuery | React + TypeScript + Vite | 18 / 5.6 / 6 |
| 后端 | PHP 5 裸脚本 | Spring Boot + Java | 3.3.x / 21 |
| 数据库 | MySQL 5.7.44 | MySQL 9.7 (本地) | 9.7 |
| 缓存 | Memcached | Redis | 7.x |
| 实时 | Comet + Flash Socket + WS | WebSocket STOMP | — |
| 认证 | PHP Session (MySQL/Memcached) | JWT (jjwt 0.12.x) | — |
| 状态管理 | — | Zustand + React Query | 5.x / 5.x |
| 动画 | — | CSS Animation + Framer Motion | 11.x |

---

## 项目结构

```
D:\code\kdjl\kdjl\kdjl\
├── .claude/
│   ├── PROGRESS.md          # 完成进度 (当前状态)
│   ├── VERIFICATION.md      # 验证步骤手册
│   ├── ROADMAP.md           # 路线图 & 里程碑
│   └── TECH_REFERENCE.md    # 本文件
│
├── kdjl_mysql8_compatible.sql    # MySQL 8.0+ 兼容 SQL dump (117 表)
├── mysql8_migrate.js             # SQL 迁移脚本
├── docker-compose.yml            # Docker 全栈编排
├── migration-plan.html           # 原始迁移分析
│
├── backend/                      # Spring Boot 后端
│   ├── pom.xml                   # Parent POM (Maven 多模块)
│   ├── Dockerfile
│   ├── kdjl-common/              # 共享模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/kdjl/common/
│   │       ├── dto/ApiResponse.java     # 统一 API 响应 {code,message,data,total,page,limit}
│   │       └── entity/                  # 63 个 JPA 实体
│   │           ├── Player.java          # player 表
│   │           ├── PlayerExt.java       # player_ext 表
│   │           ├── UserPet.java         # userbb 表 (玩家宠物)
│   │           ├── UserBag.java         # userbag 表 (背包)
│   │           ├── Pet.java             # bb 表 (宠物模板)
│   │           ├── Skill.java           # skill 表 (宠物技能)
│   │           ├── Props.java           # props 表 (道具定义)
│   │           ├── Monster.java         # gpc 表 (怪物)
│   │           ├── Battlefield*.java    # battlefield 系列
│   │           ├── War*.java            # war_* 系列 (15 个)
│   │           ├── Guild*.java          # guild 系列
│   │           ├── Team*.java           # team 系列
│   │           ├── Task*.java           # task 系列
│   │           ├── Card*.java           # card 系列
│   │           ├── [ShopOrder, Trade, Yb, YbLog, Zs, ...]
│   │           └── [... 63 entities total]
│   │
│   └── kdjl-server/              # 服务模块
│       ├── pom.xml
│       ├── Dockerfile
│       └── src/main/java/com/kdjl/server/
│           ├── KdjlApplication.java         # @SpringBootApplication 入口
│           ├── config/
│           │   ├── GlobalExceptionHandler.java
│           │   └── RedisConfig.java
│           ├── security/
│           │   ├── SecurityConfig.java      # Spring Security + JWT
│           │   ├── JwtAuthFilter.java       # JWT 解析过滤器
│           │   └── JwtTokenProvider.java    # JWT 签发
│           ├── websocket/
│           │   ├── WebSocketConfig.java     # STOMP 配置
│           │   └── ChatHandler.java         # 聊天消息处理 (频率限制)
│           ├── controller/
│           │   ├── AuthController.java      # POST /api/auth/login
│           │   ├── PlayerController.java    # GET /api/player/me
│           │   ├── PetController.java       # GET /api/pets, /api/pets/{id}
│           │   ├── BagController.java       # GET /api/bag, /api/bag/equipment
│           │   ├── BattleController.java    # POST /api/battle/pve
│           │   ├── MonsterController.java   # GET /api/monsters, /api/monsters/boss
│           │   └── MapController.java       # GET /api/map/*
│           ├── service/
│           │   ├── AuthService.java
│           │   ├── PlayerService.java       # 玩家信息 + @Cacheable
│           │   ├── PetService.java          # 宠物列表/详情
│           │   ├── BagService.java          # 背包/装备
│           │   ├── BattleService.java       # 战斗引擎 (核心)
│           │   └── CacheService.java        # Redis 缓存抽象
│           ├── repository/
│           │   ├── PlayerRepository.java, PlayerExtRepository.java
│           │   ├── UserPetRepository.java, UserBagRepository.java
│           │   ├── PetRepository.java, SkillRepository.java
│           │   ├── PropsRepository.java, MonsterRepository.java
│           │   └── [共 8 个 Repository]
│           └── resources/
│               ├── application.yml          # 主配置
│               ├── application-test.yml     # 测试配置
│               └── application-docker.yml   # Docker 配置
│
├── frontend/                     # React 前端
│   ├── package.json              # 依赖: react, zustand, @tanstack/react-query, stompjs, framer-motion
│   ├── vite.config.ts            # Vite 配置 (端口 3000, /api 代理到 8080)
│   ├── tsconfig.json             # TypeScript 配置
│   ├── index.html                # 入口 HTML
│   ├── nginx.conf                # 生产 Nginx 配置
│   └── src/
│       ├── main.tsx              # React 入口
│       ├── App.tsx               # 路由: /login, /* (游戏)
│       ├── index.css             # 全局样式 (暗色主题 #1a1a2e)
│       ├── vite-env.d.ts         # CSS Module 类型声明
│       ├── types/index.ts        # ApiResponse, Player, Pet, Item, ChatMessage
│       ├── api/
│       │   ├── client.ts         # Axios + JWT 拦截器
│       │   └── websocket.ts      # STOMP WebSocket 客户端
│       ├── stores/
│       │   ├── authStore.ts      # 认证状态 (Zustand)
│       │   └── gameStore.ts      # 游戏状态 (宠物/背包/聊天)
│       ├── hooks/
│       │   └── useWebSocket.ts   # useChat, useSendMessage
│       ├── components/
│       │   ├── layout/GameLayout.tsx   # 游戏主框架 (顶栏+导航+聊天)
│       │   └── game/ChatPanel.tsx      # 聊天面板
│       └── pages/
│           └── LoginPage.tsx     # 登录页面
│
├── api/                          # PHP 原有 API 层 (迁移源)
├── config/                       # PHP 配置 (config.game.php 等)
├── function/                     # PHP 核心逻辑 (战斗/宠物/任务等)
│   ├── FightGate.php             # 战斗入口 (1569 行)
│   ├── Fight_Mod.php             # 战斗逻辑 (1719 行)
│   ├── Pets_Mod.php              # 宠物系统
│   ├── usedProps.php             # 道具使用 (1982 行)
│   ├── task.v1.php               # 任务系统 (1936 行)
│   ├── chatGate.php              # 聊天入口 (632 行)
│   └── [... 319 PHP files]
├── sec/                          # PHP 安全/common 函数
│   └── sec_common_fnc.php        # Ack 战斗类定义 (类 Ack, 类 Ack1)
├── kernel/                       # PHP 内核 (Memcached 封装等)
├── socketChat/                   # PHP WebSocket 聊天服务器
├── template/                     # HTML 模板 (.tpl 文件)
├── images/                       # 游戏图片资源
└── javascript/                   # 原始 JS 文件 (Prototype/jQuery)
```

---

## 数据库关键信息

### 兼容性修复
| 修复项 | 数量 | 说明 |
|--------|------|------|
| MyISAM → InnoDB | 17 张 | chat_login_auth(21万行), T_fight_log(15万行), game_count(31万行) |
| latin1 → utf8mb4 | 9 张 | challenge, fb_gw_hp, fight_log, guild_members, lock, ml, session, shop_order, trade |
| utf8 → utf8mb4 | 17 张 | 全部 war_* 系列表 |
| ROW_FORMAT=FIXED → DYNAMIC | 3 张 | game_count, mty_temp, tmp_mty |
| int(11) → int | 699 处 | 移除显示宽度 |

### MySQL 8.0 保留字
- **表名:** `lock` (InnoDB, latin1), `merge` (InnoDB, utf8mb4) — 通过反引号兼容
- **列名:** `desc`, `level`, `type`, `value` — 通过反引号兼容

### 核心表关系
```
player (1) ─1:1─ player_ext
player (1) ─1:N─ userbb (UserPet)     ── bb (Pet 模板)
userbb     ─1:N─ skill (宠物技能)
player     ─1:N─ userbag (UserBag)    ── props (道具定义)
player     ─N:N─ guild (via guild_members)
player     ─N:N─ team (via team_members)

battlefield ─1:N─ battlefield_user
war_team    ─1:N─ war_fighter ─1:N─ war_fighter_talent
war_map     ─1:N─ war_map_progress
```

### 五行元素系统 (wx 字段)
```
1 = 金 → 克木
2 = 木 → 克土
3 = 水 → 克火
4 = 火 → 克金
5 = 土 → 克水
```

### 表统计
| 分类 | 数量 | 已映射 |
|------|------|--------|
| 核心 | 8 | 8 |
| 战斗 | 27 | 27 |
| 公会/社交 | 7 | 7 |
| 经济/商店 | 7 | 7 |
| 任务/活动 | 8 | 4 |
| 配置/日志 | ~60 | 10 |
| **总计** | **117** | **63** |

---

## 战斗引擎公式

从 `sec/sec_common_fnc.php` 类 `Ack` 提取:

### 命中率
```
hitRate = (攻击方 hits - 防御方 miss) / 100
clamp 到 [0.1, 1.5]
随机判定: rand(1,10) <= 阈值则命中
```

### 伤害计算 (getSkillAck)
```
ackvalue  = 技能配置的攻击值 (按等级索引)
plus      = 技能倍率 / 100 + 1
base      = (宠物 ac + ackvalue) × plus - 怪物 mc
base      = max(1, base)
damage    = round(base × hitRate) + 1
浮动: random(-10%, +5%)
暴击: damage × 2 (触发率 ~5%)
```

### 五行克制倍数
```
攻击方克防御方 → 1.5×
攻击方被克制   → 0.7×
同系或无       → 1.0×
```

### 命中/闪避详细判定 (His_and_miss)
```
攻方命中 > 防御方闪避的 3×  → 90% 命中
攻方命中 = 防御方闪避的 2-3× → 80%
攻方命中 = 防御方闪避的 1-2× → 60%
攻方命中 = 防御方闪避的 0.5-1× → 40%
攻方命中 < 防御方闪避的 0.5× → 10%
```

---

## API 约定

### 统一响应格式
```json
{
  "code": 0,           // 0=成功, -1=通用错误, 401=未登录, 403=无权限
  "message": "ok",
  "data": { ... },
  "total": null,       // 分页时使用
  "page": null,
  "limit": null
}
```

### 认证流程 (已对齐 PHP 原始逻辑)

PHP 原始认证 (deal.php):
```sql
SELECT id,name,nickname,password,secret 
FROM player 
WHERE secret = MD5(input_password) AND name = input_username
```

关键发现：
- **`password` 列是"锁定时间"**，不是密码！
- **`secret` 列是 MD5(密码)**，真正的认证凭据
- **`name` 列是用户名**（不是 `user`）

Java 实现 (AuthService):
```java
String md5Hash = md5(password);
Player player = playerRepository.findByUsernameAndSecret(username, md5Hash);
```

### JWT 认证
- Token 格式: `Authorization: Bearer <jwt>`
- Payload: `{ sub: username, uid: playerId (Long), iat, exp }`
- 过期: 24 小时 (可配)
- 密钥: `app.jwt.secret` (环境变量 JWT_SECRET)
- **注意**: JWT 中 uid 是 Long，但 Player 实体 ID 是 Integer，Controller 中需转换

### WebSocket
- 端点: `ws://host:port/ws`
- 协议: STOMP over WebSocket
- 聊天发送: `SEND /app/chat` `{content, channel}`
- 聊天接收: `SUBSCRIBE /topic/chat`, `/topic/guild`, `/topic/team`
- 频率限制: 3 条/2 秒 (CacheService.tryAcquire)

---

## 环境变量

```bash
MYSQL_HOST=localhost
MYSQL_USER=kdjl
MYSQL_PASSWORD=kdjl_pass
REDIS_HOST=localhost
REDIS_PASSWORD=
JWT_SECRET=change-me-in-production
SERVER_PORT=8080
```

---

## 编译 & 运行

### 后端
```bash
# 编译安装 (必须先 install，kdjl-server 依赖 kdjl-common)
cd backend
mvn install -DskipTests -q

# 启动 (当前用 8088，8080 被占用)
cd kdjl-server
SERVER_PORT=8088 mvn spring-boot:run

# 如果启动失败，先杀旧进程
netstat -ano | grep ":8088.*LISTENING" | awk '{print $NF}' | while read p; do cmd.exe /c "taskkill /F /PID $p" 2>/dev/null; done
```

### 前端
```bash
cd frontend
npm run dev                           # 开发服务器 (端口 3000, /api 代理到 8088)
npm run build                         # 生产构建 (dist/)
npx tsc --noEmit                      # 类型检查
```

### 环境变量
```bash
MYSQL_HOST=localhost
MYSQL_USER=kdjl
MYSQL_PASSWORD=kdjl_pass
REDIS_HOST=localhost
REDIS_PASSWORD=
JWT_SECRET=change-me-in-production
SERVER_PORT=8088
```

### 常用 Maven 坐标
- GroupId: `com.kdjl`
- ArtifactId: `kdjl-backend` (parent), `kdjl-common`, `kdjl-server`
- Java: 21
- Spring Boot: 3.3.5
- 打包: `mvn spring-boot:run` 在 `kdjl-server/`

---

## 关键 PHP 源文件 (迁移参考)

| 文件 | 行数 | 说明 |
|------|------|------|
| function/FightGate.php | 1569 | 战斗回合入口 + 装备加成 + 扣血逻辑 |
| function/Fight_Mod.php | 1719 | 战斗页面渲染 + 组队副本检查 |
| function/Fight_Mod_Mobile.php | 1733 | 移动版战斗 |
| sec/sec_common_fnc.php | ~2600 | class Ack / Ack1 战斗公式核心 |
| function/chatGate.php | 632 | 聊天入口 |
| function/chatProto.php | 730 | 聊天协议 + 敏感词过滤 |
| function/usedProps.php | 1982 | 道具使用逻辑 |
| function/task.v1.php | 1936 | 任务系统 |
| function/Pets_Mod.php | — | 宠物管理 |
| function/Zb_Mod.php | 444 | 装备系统 |
| function/merge.php | — | 宠物合成 |
| function/Merge_Mod.php | — | 合成逻辑 |
| kernel/memory.v1.php | — | Memcached 封装 (已替代: CacheService) |
| kernel/memSession.v1.php | — | Session 管理 (已替代: CacheService.saveSession) |
| socketChat/server/snb.php | 1262 | WebSocket 聊天服务 |
| custom/*, api/* | — | GM 管理后台 |

---

## 下次快速启动命令

```
# 读取上下文
Read .claude/PROGRESS.md           # 当前进度
Read .claude/ROADMAP.md            # 路线图

# 验证环境
docker start kdjl-redis
"C:/Program Files/MySQL/MySQL Server 9.7/bin/mysql.exe" -u kdjl -pkdjl_pass kdjl -e "SELECT 1"

# 启动后端 (8088)
cd backend && mvn install -DskipTests -q
cd kdjl-server && SERVER_PORT=8088 mvn spring-boot:run

# 启动前端 (3000)
cd frontend && npm run dev
```

## 已修复的关键 Bug (备忘)

| Bug | 根因 | 修复 |
|-----|------|------|
| JDBC URL `utf8mb4` 不识别 | MySQL Connector/J 只认 `UTF-8` | 改为 `characterEncoding=UTF-8` |
| Entity 未扫描 | `kdjl-common` 实体跨模块 | 加 `@EntityScan("com.kdjl.common.entity")` |
| `scale has no meaning for float` | `Double` + `scale=2` | TShopLog.buyPrice → `BigDecimal` |
| Player 实体全错 | 列名/类型都不对 | 基于实际 DB schema 重写 |
| 认证 500 | BCrypt vs MD5 不匹配 | AuthService 改用 MD5 |
| Pets API 500 | UserPet 有不存在的列 | 移除 `ac_up`/`mc_up`/`hp_up`/`mp_up`/`position`/`exp_got_step` |
| 端口 8080 占用 | 旧 Java 进程残留 | 换用 8088，前端代理同步更新 |
