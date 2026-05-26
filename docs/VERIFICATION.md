# KDJL 验证手册

> 可验证的步骤、命令和前置条件。按依赖顺序排列。
> 最后更新: 2026-05-26 — 礼包系统修复 + 宠物进化链补全 + 45级礼包数据修复

---

## 当前环境

| 组件 | 详情 |
|------|------|
| Java | JDK 21.0.6 |
| Maven | 3.8.8 (D:\enviroment\apache-maven-3.8.8) |
| MySQL | 8.0.40, 端口 3306, 用户 kdjl/kdjl_pass |
| Node.js | npm on PATH |

## 首次安装

```bash
# 1. 导入数据库
"D:/software/mysql-8.0.40-winx64/mysql-8.0.40-winx64/bin/mysql" -u kdjl -pkdjl_pass kdjl < db/kdjl_mysql8_compatible.sql

# 2. 执行数据修复脚本
"D:/software/mysql-8.0.40-winx64/mysql-8.0.40-winx64/bin/mysql" -u kdjl -pkdjl_pass kdjl < db/fixes/001-fix-props-effect-prefix.sql
"D:/software/mysql-8.0.40-winx64/mysql-8.0.40-winx64/bin/mysql" -u kdjl -pkdjl_pass kdjl < db/fixes/002-backfill-remake-fields.sql
```

## 快速启动

### 第一步: 确保 MySQL 运行
```bash
"D:/software/mysql-8.0.40-winx64/mysql-8.0.40-winx64/bin/mysql" -u kdjl -pkdjl_pass -e "SELECT 1"
```

### 第二步: 编译并启动后端 (端口 8080)
```bash
cd kdjl-backend
mvn install -pl kdjl-common -DskipTests -q
mvn spring-boot:run -pl kdjl-server
# 启动成功标志: "Started KdjlApplication in X seconds"
```

### 第三步: 启动前端 (端口 3000)
```bash
cd kdjl-frontend
npm install
npm run dev
# 浏览器打开: http://localhost:3000
```

### 第四步: 测试 API
```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'

# 宠物列表
curl http://localhost:8080/api/pets -H "Authorization: Bearer <token>"

# 背包
curl http://localhost:8080/api/bag -H "Authorization: Bearer <token>"

# 使用道具
curl -X POST http://localhost:8080/api/bag/use/<bagItemId> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" -d '{}'

# 开宝箱
curl -X POST http://localhost:8080/api/bag/use/<bagItemId> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" -d '{"petId":<mainPetId>}'
```

## 前端页面验证清单

| 页面 | 操作 | 预期结果 |
|------|------|----------|
| 登录 | testuser/test123 | 跳转游戏主界面 |
| 宠物神殿 | 选择宠物 Tab1进化 | 显示进化等级/材料 (非"不可进化") |
| 背包 | 悬停道具 | 中文效果描述, 装备显示位置/可强化 |
| 背包 | 使用宠物卵 | 只检查携带中宠物(≤3), 牧场不计 |
| 背包 | 开启等级礼包 | needkey钥匙检查, 等级不足提示 |
| 背包 | 开宝箱 | randitem概率+公告正确 |
| 装备 | 悬停装备 | 卡槽宝石/套装信息正确 |
| 战斗 | 地图→挑战→选宠 | 回合动画, 胜负结果 |

## 验证检查清单

| # | 检查项 | 命令 | 预期结果 |
|---|--------|------|----------|
| 1 | Java 版本 | `java -version` | 21.0.x |
| 2 | Maven | `mvn -v` | 3.8.x+ |
| 3 | MySQL | `mysql -u kdjl -pkdjl_pass -e "SELECT 1"` | 1 |
| 4 | 后端编译 | `cd kdjl-backend && mvn compile -q` | BUILD SUCCESS |
| 5 | 后端启动 | `cd kdjl-backend && mvn spring-boot:run -pl kdjl-server` | Started |
| 6 | 登录API | curl POST /api/auth/login | JWT token |
| 7 | 前端构建 | `cd kdjl-frontend && npm run build` | dist/ |

## 数据修复脚本

| 脚本 | 说明 |
|------|------|
| `db/fixes/001-fix-props-effect-prefix.sql` | 修复45级礼包 double giveitems 前缀 |
| `db/fixes/002-backfill-remake-fields.sql` | 补全已有宠物的进化链字段 |

## 常见问题

### 端口被占用
```bash
netstat -ano | findstr 8080
powershell -Command "Get-Process -Name java | Stop-Process -Force"
```

### 宠物显示"不可进化"
运行 `db/fixes/002-backfill-remake-fields.sql` 补全进化链。
新创建宠物已通过代码修复 (BagService.java + PetService.java)。

### 宝箱无法打开
- 背包需留至少3格空位
- 主战宠物需达到等级要求 (props.requires:lv:N)
- 需要钥匙的宝箱需有对应钥匙道具
