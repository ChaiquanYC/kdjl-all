# KDJL 后台管理系统 — PHP功能调研 & 迁移计划

> 调研日期: 2026-05-24 | PHP源码: `custom/` 目录

---

## 一、PHP 后台系统现状

PHP 后台位于 `custom/` 目录，是一个**独立于游戏主应用的简单管理工具**，无认证机制，纯 HTML+JS+PHP。

### 后台页面结构

```
custom/
├── adminIndex.php          # 主框架 (左侧菜单 + iframe内容区)
├── adminCSS/adIndex.css    # 样式
├── addPropToUser.php       # 给玩家派送道具 (前端页面)
├── addPropToUserGate.php   # 派送道具后端逻辑
├── selectProps.php         # 道具表浏览 (分页)
├── selectPropsQuery.php    # 道具名称搜索
├── selectPlayer.php        # 玩家查询入口
├── selectPlayerQuery.php   # 玩家信息查询结果
├── selectBB.php            # 宠物模板查询入口
├── selectBBQuery.php       # 宠物模板查询结果
├── selectMerge.php         # 合成表浏览 (分页)
├── selectMergeQuery.php    # 合成表搜索
└── images/                 # 背景图片
```

### 功能清单

| 功能 | PHP文件 | 说明 |
|------|---------|------|
| 派送道具（不发公告） | addPropToUserGate.php?act=addProp | 给玩家背包加道具，已有则叠加sums |
| 派送道具（发公告） | addPropToUserGate.php?act=addPropGG | 同上 + 全服公告通知 |
| 派送元宝 | addPropToUserGate.php?act=addYB | 修改player.yb字段 |
| 派送水晶 | addPropToUserGate.php?act=addSJ | 修改player_ext.sj字段 |
| 验证玩家昵称 | addPropToUserGate.php?act=selectUserName | 输入ID → 返回昵称 |
| 验证道具名称 | addPropToUserGate.php?act=selectPropName | 输入ID → 返回道具名 |
| 浏览道具表 | selectProps.php | props表分页浏览 (10条/页) |
| 搜索道具 | selectPropsQuery.php | 按道具名模糊搜索 |
| 查询玩家 | selectPlayer.php → selectPlayerQuery.php | 按昵称搜player表 |
| 查询宠物 | selectBB.php → selectBBQuery.php | 按名称搜bb模板表 |
| 浏览合成表 | selectMerge.php | merge表分页 + 主/副/结果宠 |
| 搜索合成 | selectMergeQuery.php | 按主宠/副宠/结果宠ID搜索 |

### PHP后台的安全问题
- **无任何认证**：任何人知道URL即可访问
- **SQL注入风险**：玩家输入直接拼接到SQL语句中 (addPropToUserGate.php)
- **无操作日志**：不知道谁在什么时候做了什么

### 补充发现的PHP管理功能

| 功能 | PHP文件 | 说明 |
|------|---------|------|
| 聊天GM命令 | chatGate.php, chatProto.php | JY禁言/JJ解禁/YZ永禁/FH封号/@公告 |
| 远程封禁API | api/Gamemaster.php | MD5签名认证, 远程封禁/解封 |
| 数据库直连工具 | api/allToolsQl.php | IP+每日密码, 执行任意SQL |
| 金币消费统计 | api/gold_usage_sta.php | 按日期统计yblog元宝消费 |
| 缓存热重载 | vm1.php | 重载任务/技能/怪物/地图等游戏配置 |
| Memcache管理 | function/anounce.php | 踢人下线/清聊天/key管理 |
| 守卫线程 | guard_thread.php | MySQL长连接清理/战场结算/在线统计 |
| 反外挂检测 | sec/kick_cheater.php | 访问模式追踪+标记可疑玩家 |
| 冲级排行榜 | function/entrance.php | 按宠物等级排榜/强制重排 |
| 区服转移 | function/swap_Zone.php | 角色跨服迁移,MD5密码保护 |
| 卡密系统 | function/newcard.php | 外部卡券兑换, 发道具到账户 |
| 消费排行 | function/consumptionTop.php | 每日消费前三名发奖励 |

---

## 二、Java侧已有管理功能

| 端点 | 功能 | 所在Controller |
|------|------|---------------|
| POST /api/admin/reset-pets | 重置玩家所有宠物到1级 | AdminController |
| POST /api/admin/set-auto | 设置自动战斗次数 | AdminController |
| POST /api/admin/add-item | 给玩家发道具 | AdminController |
| POST /api/admin/add-skill | 直接教宠物技能 | AdminController |
| POST /api/admin/add-pet | 从模板创建宠物给玩家 | AdminController |
| GET /api/gm/player/search | 按昵称搜玩家 | GmController |
| POST /api/gm/give-item | 发道具（带道具名验证） | GmController |
| POST /api/gm/give-money | 发金币+元宝 | GmController |
| POST /api/gm/set-pet-level/{id} | 设宠物等级 | GmController |

---

## 三、迁移计划：kdjl-admin 模块

### 目标架构

```
backend/kdjl-admin/              # 独立Spring Boot :8081
├── pom.xml                       # thymeleaf + spring-boot-starter-web
├── src/main/java/com/kdjl/admin/
│   ├── KdjlAdminApplication.java
│   ├── config/
│   │   └── SecurityConfig.java   # Admin独立认证 (admin账号)
│   ├── controller/
│   │   ├── PageController.java   # Thymeleaf页面路由
│   │   └── AdminApiController.java # REST操作API
│   └── service/
│       └── AdminService.java     # 统计/查询/操作
└── src/main/resources/
    ├── application.yml            # 端口8081, 复用kdjl数据库
    └── templates/
        ├── layout.html            # 公共布局 (左侧菜单)
        ├── login.html             # 登录页
        ├── dashboard.html         # 首页仪表盘
        ├── player/list.html       # 玩家管理
        ├── player/detail.html     # 玩家详情 (含宠物/背包/日志)
        ├── props/list.html        # 道具管理
        ├── pets/list.html         # 宠物模板管理
        ├── merge/list.html        # 合成表管理
        └── stats.html             # 数据统计
```

### 功能对照表

| PHP原功能 | kdjl-admin 实现 | 增强 |
|-----------|----------------|------|
| 派送道具/元宝/水晶 | ✅ player/detail 页内操作 | 批量派送、操作日志 |
| 验证玩家昵称 | ✅ 搜索自动补全 | - |
| 验证道具名称 | ✅ 搜索自动补全 | - |
| 浏览道具表 | ✅ props/list 分页表格 | 搜索/排序/点击编辑 |
| 搜索道具 | ✅ 搜索栏 | - |
| 查询玩家 | ✅ player/list + 详情页 | 完整信息/宠物列表/背包 |
| 查询宠物模板 | ✅ pets/list | 搜索/分页 |
| 浏览合成表 | ✅ merge/list | 搜索/分页 |

### 新增功能（PHP没有的）

| 功能 | 说明 | 运营价值 |
|------|------|---------|
| 📊 **数据仪表盘** | 注册数/今日活跃/在线数/收入趋势 | 高 |
| 🐛 **战斗日志查询** | 查看玩家最近战斗记录 (fight_log表) | 中 |
| 🎁 **批量派送** | 按条件筛选玩家→批量发道具/元宝 | 高 |
| 📝 **操作日志** | 记录所有GM操作 (谁/何时/做了什么) | 高 |
| 🔒 **Admin认证** | 独立管理员账号密码登录 | 必须 |
| 🚫 **玩家封禁** | 禁止登录/发言 | 中 |

---

## 四、任务清单

| 编号 | 任务 | 优先级 | 预估 |
|------|------|--------|------|
| A1 | 创建 kdjl-admin Maven模块 (pom.xml + 主类) | P0 | 小 |
| A2 | 配置 application.yml (端口8081, 数据源) | P0 | 小 |
| A3 | Admin认证 (Spring Security, admin账号) | P0 | 中 |
| A4 | Thymeleaf布局 + 公共模板 (layout/header/menu) | P0 | 中 |
| A5 | 登录页 | P0 | 小 |
| B1 | 仪表盘首页 (统计卡片+图表) | P0 | 中 |
| B2 | 玩家列表页 (搜索/分页/列表) | P0 | 中 |
| B3 | 玩家详情页 (信息/宠物/背包/操作) | P0 | 大 |
| B4 | 派送道具/元宝/水晶 (含批量) | P0 | 中 |
| B5 | 宠物模板浏览 | P1 | 小 |
| B6 | 道具表浏览+搜索 | P1 | 小 |
| B7 | 合成表浏览+搜索 | P1 | 小 |
| C1 | 战斗日志查询 | P1 | 中 |
| C2 | 操作日志记录 | P1 | 中 |
| C3 | 玩家封禁/解封/禁言 (PHP:FH/JY/JJ) | P1 | 中 |
| D1 | 元宝消费统计 (PHP:gold_usage_sta) | P2 | 中 |
| D2 | 游戏配置热重载 (PHP:vm1.php) | P2 | 大 |
| D3 | 在线统计/活跃趋势图 | P2 | 中 |
| D4 | 卡密生成/管理 (PHP:newcard.php) | P3 | 中 |
