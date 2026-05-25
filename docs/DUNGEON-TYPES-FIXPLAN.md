# KDJL 副本/地图类型修复计划

> 2026-05-24 | PHP multi_monsters 5种类型 vs Java当前实现对比 | **全部完成 ✅**

---

## 类型总览

| 类型 | multi_monsters | 地图ID | 当前Java状态 |
|------|---------------|--------|-------------|
| 普通副本 | 0 | 11,12,13,14,50,124,127,151 | ⚠️ UI不对(用了teamContainer, 该用专用副本页) |
| 挑战模式 | 1 | 125(琥珀屋) | ❌ 完全未实现, 地图点都没加 |
| 通天塔 | 2 | 126 | ❌ 有API无前端, 地图点未加 |
| 组队副本 | 3 | 128,129,130 | ❌ 标记open:false, 完全未实现 |
| 神圣地图 | 4 | 131-150 | ⚠️ 当普通地图, 缺wx==7检查 |

---

## 一、普通副本 (multi_monsters=0) — 8个地图

### 当前问题
1. **UI用错**: 用了teamContainer 3列布局，PHP用的是专用副本页面(tpl_fb.html)
2. **缺少元素**: 副本专用背景图(fbdtXX.jpg)、波次进度、倒计时、下个怪物名

### 需要修改

| 文件 | 修改内容 |
|------|---------|
| `MapPanel.tsx` | 副本进入时用专用副本UI替代teamContainer |
| `MapPanel.module.css` | 新增副本专用样式(背景fbbg.jpg或类似) |
| 新增 `DungeonPanel.tsx` | 副本信息页: 顶部副本图+进度+波次+倒计时+宠物选择+进入按钮 |
| `DungeonController.java` | 已基本完成, 需补充: 返回下个怪物名称 |

### PHP关键代码
```
fb_Mod.php:33-58   — 按ID选择副本图(fbdt02/10/11/14/50/151.jpg)
fb_Mod.php:125-213 — info()函数: 等级/倒计时/怪物总数/当前进度/下个怪物名
tpl_fb.html        — 专用布局: fbbg.jpg背景 + 左侧副本图 + 右侧信息栏 + 宠物卡片
```

---

## 二、挑战模式 (multi_monsters=1) — 地图125(琥珀屋)

### 当前问题
1. 地图125完全未添加到MAP_POINTS
2. 没有任何前端/后端实现

### 需要修改

| 文件 | 修改内容 |
|------|---------|
| `MapPanel.tsx` | MAP_POINTS添加125(琥珀屋), page=3, 挑战模式特殊标记 |
| 新增 `ChallengePanel.tsx` | 挑战界面: 每日剩余次数+星级难度选择(★~★★★★★)+怪物列表+宠物选择 |
| `MapPanel.module.css` | 挑战模式按钮样式(ann07/08/09.gif难度按钮) |
| 新增 `ChallengeController.java` | `/api/challenge/state` 查询状态, `/api/challenge/enter` 开始挑战 |
| 新增 `ChallengeService.java` | 每日3次限制(challenge表), 怪物生成, 进度管理 |

### PHP关键代码
```
Team_Mod.php:349-442 — 挑战模式: 怪物列表刷新/难度选择/tpl_cteam.html
Team_Mod.php:580-596 — getGpc()按难度随机怪物
Fight_Mod.php:841-878 — 挑战战斗初始化
config.game.php      — challenge表结构
```

---

## 三、通天塔 (multi_monsters=2) — 地图126

### 当前问题
1. 地图126未添加到MAP_POINTS
2. TowerController有API但无前端UI
3. 缺少55层进度/排行榜/楼层显示

### 需要修改

| 文件 | 修改内容 |
|------|---------|
| `MapPanel.tsx` | MAP_POINTS添加126(通天塔), page=3 |
| 新增 `TowerPanel.tsx` | 通天塔界面: 当前楼层+排行榜(前5)+怪物列表+宠物选择+继续按钮 |
| `TowerController.java` | 已有(✅ state/challenge/progress/leaderboard), 可能需要微调 |
| `tower/` 模板 | 或后端用Thymeleaf也行, 但游戏内用React |

### PHP关键代码
```
Team_Mod.php:443-505 — 通天塔: 楼层显示+排行榜+tpl_tt.html
Tt_Mod.php           — 旧版通天塔(难度选择)
Fight_Mod.php:515-522 — multi_monsters=2映射到内部值3
FightGate.php:927-961 — 楼层递增/每5层Boss/全服公告
player_ext.tgt       — 最高楼层记录
```

---

## 四、组队副本 (multi_monsters=3) — 地图128/129/130 (遗忘宫殿)

### 当前问题
1. 地图128标记open:false, 129/130根本没加MAP_POINTS
2. 组队逻辑已有(TeamController/TeamService)但副本入口未对接
3. 3阶段×5波进度系统未实现

### 需要修改

| 文件 | 修改内容 |
|------|---------|
| `MapPanel.tsx` | 更新128为teamDungeon类型, 添加129/130(hard/hell难度) |
| `MapPanel.tsx` | 进入时检查是否在队伍中, 弹出组队提醒 |
| `TeamService.java` | 新增: team_fuben_step进度追踪(阶段+波次) |
| `DungeonController.java` 或 `TeamController.java` | 组队副本进入/波次推进端点 |
| `Fight_Mod`对应 | multi_monsters=3的战斗逻辑(队员宠物检查/czl差距/轮流战斗) |

### PHP关键代码
```
Fight_Mod.php:17-21   — $__teamFubenMap: 128/129/130→128
Fight_Mod.php:57-63   — 无队伍则拒绝
Fight_Mod.php:64-78   — 队员宠物检查(lv/czl/wx)
team.v1.php:710-753   — 3阶段×5波进度系统
Team_Mod.php:506-508  — tpl_team.html模板
manymapgate.php:59-79 — 战斗前团队状态验证
```

---

## 五、神圣地图 (multi_monsters=4) — 地图131-150

### 当前问题
1. 131-142,145-150当普通地图对待, 没有wx==7检查
2. 143(熔岩地宫)/144(幻魔之境)标记dungeon但也没有神圣检查

### 需要修改

| 文件 | 修改内容 |
|------|---------|
| `MapPanel.tsx` | MAP_POINTS中131-150标记sacred:true, 渲染不同标识(金色边框等) |
| `BagService.java` 或 `BattleService.java` | 战斗进入前检查pet.wx==7 (fbfight_Mod.php:50-56) |
| `DungeonController.java` | 143/144进入时也检查神圣宠物 |

### PHP关键代码
```
fbfight_Mod.php:50-56 — wx!=7则弹窗拒绝
fbfight_Mod.php:58-64 — 强制更新inmap
Fight_Mod.php:51-55   — chaoshenchongDituFlag标志
Fight_Mod.php:617-619 — 神圣宠物二次验证
```

---

## 修复优先级建议

| 优先级 | 类型 | 工作量 | 理由 |
|--------|------|--------|------|
| P0 | 普通副本UI修正 | ✅ | DungeonPanel.tsx + fuben图片 |
| P1 | 神圣地图检查 | ✅ | BattleService.pet.wx==7 + MAP_POINTS标记 |
| P1 | 通天塔前端 | ✅ | TowerPanel.tsx + TowerController API |
| P2 | 挑战模式 | ✅ | ChallengePanel.tsx + ChallengeModeController |
| P2 | 组队副本 | ✅ | teamDungeon入口 + 组队检查 + teamContainer |

---

## 文件变更预估

| 操作 | 文件数 | 说明 |
|------|--------|------|
| 新增 | ~4 | DungeonPanel.tsx, ChallengePanel.tsx, TowerPanel.tsx, ChallengeController.java |
| 修改 | ~5 | MapPanel.tsx/css, DungeonController.java, TeamService.java, BagService.java |
| 新增 | ~3 | ChallengeService.java, team dungeon API, sacred check helper |
| 总计 | ~12 | 约1500行代码 |
