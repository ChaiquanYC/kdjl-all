# KDJL 验证手册

> 可验证的步骤、命令和前置条件。按依赖顺序排列。
> 最后更新: 2026-05-22 05:00 — 新增捕捉+商店+技能 API

---

## 当前环境

| 组件 | 状态 | 详情 |
|------|------|------|
| Java | ✅ | JDK 21.0.11 |
| Maven | ✅ | 3.9.16 |
| MySQL | ✅ | 9.7, 端口 3306, 用户 root/12345678, kdjl 库 117 表 |
| Redis | ✅ | Docker redis:7-alpine, 容器 kdjl-redis, 端口 6379 |
| Docker | ✅ | Desktop 4.74.0 |
| Node.js | ✅ | v24.16.0 |

## 快速启动

### 第一步: 确保 MySQL 运行
```bash
# MySQL 服务应自动运行，检查:
"C:/Program Files/MySQL/MySQL Server 9.7/bin/mysql.exe" -u root -p12345678 -e "SELECT 1"
```

### 第二步: 启动 Redis (Docker)
```bash
docker start kdjl-redis 2>/dev/null || docker run -d --name kdjl-redis -p 6379:6379 redis:7-alpine
docker exec kdjl-redis redis-cli ping   # 应返回 PONG
```

### 第三步: 编译并启动后端 (端口 8088)
```bash
cd backend
mvn install -DskipTests -q
cd kdjl-server
SERVER_PORT=8088 mvn spring-boot:run
# 启动成功标志: "Started KdjlApplication in X seconds"
# 如果 8088 被占用，换 SERVER_PORT=8089
```

### 第四步: 启动前端 (端口 3000)
```bash
cd frontend
npm run dev
# 浏览器打开: http://localhost:3000
```

### 第五步: 测试 API
```bash
# 1. 登录
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'
# 预期: {"code":0,"data":{"token":"eyJ...","uid":102,"nickname":"TestUser"}}

# 2. 玩家信息
curl http://localhost:8088/api/player/me -H "Authorization: Bearer <token>"

# 3. 宠物列表
curl http://localhost:8088/api/pets -H "Authorization: Bearer <token>"

# 4. 背包
curl http://localhost:8088/api/bag -H "Authorization: Bearer <token>"

# 5. 地图列表
curl http://localhost:8088/api/map/list -H "Authorization: Bearer <token>"

# 6. 地图怪物
curl http://localhost:8088/api/map/1/monsters -H "Authorization: Bearer <token>"

# 7. PvE 战斗 (需要有效的 petId 和 monsterId)
curl -X POST http://localhost:8088/api/battle/pve \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"petId":86,"monsterId":1}'
# 预期: 返回回合制战斗结果 (rounds, won, expGained, moneyGained, drops)

# 8. 使用道具 (需要 petId)
curl -X POST http://localhost:8088/api/bag/use/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"petId":86}'
# 预期: {"code":0,"data":{"usedItemId":1,"propName":"...","type":"healHP","healedHP":...}}

# 9. 装备穿戴
curl -X POST http://localhost:8088/api/bag/equip/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"petId":86}'
# 预期: {"code":0,"data":{"equipped":true,"equipId":1,"petId":86}}

# 10. 装备卸下
curl -X POST http://localhost:8088/api/bag/unequip/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{}'
# 预期: {"code":0,"data":{"unequipped":true,"equipId":1}}

# 11. 捕捉怪物
curl -X POST http://localhost:8088/api/pets/capture/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" -d '{}'
# 预期: {"code":0,"data":{"captured":true/false,"monsterName":"...","roll":...,"threshold":...}}

# 12. 商城列表
curl http://localhost:8088/api/shop/list -H "Authorization: Bearer <token>"

# 13. 购买道具
curl -X POST http://localhost:8088/api/shop/buy/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"count":1,"currency":"money"}'

# 14. 宠物技能+学习+升级
curl http://localhost:8088/api/pets/86/skills -H "Authorization: Bearer <token>"
curl http://localhost:8088/api/pets/86/skills/learnable -H "Authorization: Bearer <token>"
curl -X POST http://localhost:8088/api/pets/86/skills/learn/1 \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{}'
curl -X POST http://localhost:8088/api/pets/86/skills/upgrade/1 \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{}'
```

## 前端页面验证清单

| 页面 | 操作 | 预期结果 |
|------|------|----------|
| 登录 | testuser/test123 | 跳转游戏主界面 |
| 顶栏 | 查看 | 昵称/VIP/金币/元宝 正常显示 |
| 宠物 | 点击"宠物" | 卡片+点击展开技能面板(学习/升级) |
| 背包 | 点击"背包" | 道具网格+使用按钮, 可对宠物使用 |
| 装备 | 点击"装备" | 宠物选择+装备列表+穿戴/卸下 |
| 商城 | 点击"商城" | 道具列表+金币/元宝购买 |
| 地图 | 点击"地图" | 5个地图, 怪物挑战+捕捉按钮 |
| 战斗 | 地图→挑战→选宠 | VS界面, 回合动画, 胜负结果 |
| 退出 | 点击"退出" | 返回登录页 |

## 验证检查清单

| # | 检查项 | 命令 | 预期结果 |
|---|--------|------|----------|
| 1 | Java 版本 | `java -version` | 21.0.x |
| 2 | Maven 版本 | `mvn -v` | 3.9.x |
| 3 | 后端编译 | `cd backend && mvn compile` | BUILD SUCCESS |
| 4 | MySQL 连接 | `mysql -u root -p12345678 -e "SHOW DATABASES"` | 包含 kdjl |
| 5 | 表数量 | `mysql -u kdjl -pkdjl_pass kdjl -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='kdjl'"` | 117 |
| 6 | Redis | `docker exec kdjl-redis redis-cli ping` | PONG |
| 7 | 后端启动 | 见上方第三步 | Started KdjlApplication |
| 8 | 登录 API | 见上方第五步 | 返回 JWT token |
| 9 | 前端启动 | 见上方第四步 | Vite dev server |
| 10 | 前端构建 | `cd frontend && npm run build` | dist/ 输出 |

## 常见问题

### 端口被占用
```bash
# 查看占用
netstat -ano | findstr 8088
# 杀掉进程
powershell -Command "Stop-Process -Id <PID> -Force"
```

### Redis 容器未启动
```bash
docker start kdjl-redis
# 或重新创建
docker rm kdjl-redis 2>/dev/null
docker run -d --name kdjl-redis -p 6379:6379 redis:7-alpine
```

### 前端代理端口不对
确保 `frontend/vite.config.ts` 中 target 端口与后端一致（当前: 8088）
