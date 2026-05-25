# KDJL 口袋精灵2 开发总结

> 口袋精灵 (KDJL) PHP → React + Spring Boot 迁移项目
> 版本: 2026-05-24 | 编译状态: ✅ BUILD SUCCESS
> 目标读者: 接手二次开发的工程师

---

## 1. 项目概览

### 1.1 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 前端框架 | React + TypeScript | Vite 6.4 |
| 状态管理 | Zustand | - |
| UI 样式 | CSS Modules | - |
| HTTP 客户端 | Axios (baseURL: /api) | - |
| 后端框架 | Spring Boot | 3.3.5 |
| Java 版本 | 21 | - |
| 数据库 | MySQL | 9.7 |
| ORM | Hibernate/JPA | 6.5 |
| 缓存 | Redis (可选) | 7 |
| 认证 | JWT (HMAC-SHA384) | 24h 过期 |

### 1.2 运行环境

```
前端: npm run dev → http://localhost:3001
后端: mvn spring-boot:run → http://localhost:8080
数据库: MySQL localhost:3306, 库名 kdjl, 117 张表
测试账号: testuser / test123
管理账号: admin / admin
```

### 1.3 项目结构

```
kdjl/
├── frontend/                    # React 前端
│   ├── src/
│   │   ├── api/                 # Axios 客户端 + WebSocket
│   │   ├── stores/              # Zustand (authStore, gameStore)
│   │   ├── hooks/               # useWebSocket
│   │   ├── components/
│   │   │   ├── layout/          # GameLayout, ErrorBoundary, OverlayPanel
│   │   │   └── game/            # 20+ 游戏面板
│   │   ├── pages/               # LoginPage
│   │   └── types/               # TypeScript 类型定义
│   └── public/images/           # UI 资源 (bb/, gpc/, ui/, map/)
├── backend/
│   ├── kdjl-common/             # 共享模块 (Entity, DTO)
│   │   └── src/.../entity/      # 17 个 JPA 实体
│   └── kdjl-server/             # 服务模块
│       └── src/.../
│           ├── controller/      # 15 个 REST Controller
│           ├── service/         # 12 个 Service
│           ├── repository/      # 19 个 JPA Repository
│           ├── battle/          # BattleSession, BattleSessionManager
│           ├── security/        # JWT 认证
│           └── config/          # 全局异常处理, Redis 配置
└── docs/                        # 13 份技术文档
```

---

## 2. 数据库映射

### 2.1 核心表 → Java Entity

| PHP 表 | Java Entity | 主键类型 | 关键字段 |
|--------|-------------|----------|----------|
| `player` | `Player.java` | `Integer` | money, yb, prestige, mbid, maxBag, maxMc, dblexpflag, sysautosum, maxautofitsum, autofitflag |
| `player_ext` | `PlayerExt.java` | `Integer` | sj, petShow, onlineTime, teamAutoTimes, expGotStep |
| `bb` | `Pet.java` | `Long` | name, wx, hp/mp/ac/mc/speed/hits/miss, czl, kx, imgstand/ack/die, headimg, cardimg, effectimg, skillist, subyl~subkl |
| `userbb` | `UserPet.java` | `Long` | 继承 bb + uid, level, srchp/srcmp, addhp/addmp, hp/mp, nowexp/lexp, muchang, zb, kx, czl |
| `gpc` | `Monster.java` | `Long` | name, level, hp/mp/ac/mc, hits/miss, catchv, catchid, imgstand/ack/die, droplist, exps, money, boss, wx |
| `props` | `Props.java` | `Long` | name, effect, pluseffect, buy/yb/sj/prestige, vary, varyname, postion, series/serieseffect, plusflag/pid/get, requires |
| `userbag` | `UserBag.java` | `Long` | pid(propId), uid(playerId), sums, vary, zbing, zbpets, plusTimesEffect, holeInfo |
| `skill` | `Skill.java` | `Long` | bid(petId), sid(skillDefId), name, level, wx, vary, value, plus, uhp, ump, img |
| `skillsys` | `SkillSys.java` | `Long` | pid, name, vary, wx, ackvalue, plus, requires, uhp, ump, ackstyle, imgeft |
| `map` | `GameMap.java` | `Integer` | name, level, img, gpclist, descs |
| `wx` | `Wx.java` | `Integer` | wx, j/m/s/h/t, hp/mp/ac/mc/speed/hits/miss |
| `exptolv` | `ExpToLv.java` | `Long` | level, nxtlvexp |

### 2.2 muchang 状态码

| 值 | 含义 |
|----|------|
| 0/null | 携带中 (出战栏) |
| 1 | 牧场中 |
| 3-7 | 传承相关状态 |

### 2.3 五行 (wx) 映射

```
1=金 2=木 3=水 4=火 5=土 6=神 7=神圣
```

---

## 3. REST API 完整清单

### 3.1 认证 (2)
```
POST /api/auth/login     {username, password} → {token, uid, username, nickname}
POST /api/auth/register  {username, password, nickname, petId} → player
```

### 3.2 玩家 (1)
```
GET /api/player/me  → {id, username, nickname, money, yb, sj, prestige, mbid, headImg, maxBag, maxMc, ...}
```

### 3.3 宠物 (10)
```
GET    /api/pets                         → 携带宠物列表 (muchang=0)
GET    /api/pets/ranch                   → 牧场宠物列表 (muchang=1)
GET    /api/pets/{id}                    → 宠物详情 + skills + growth
GET    /api/pets/{id}/skills             → 已学技能
GET    /api/pets/{id}/skills/learnable   → 可学技能
POST   /api/pets/{id}/skills/learn/{sid} → 学习技能 (需技能书)
POST   /api/pets/{id}/skills/upgrade/{id}→ 升级技能 (需道具733/1666)
POST   /api/pets/set-main/{id}           → 设为主战
POST   /api/pets/{id}/deposit            → 寄养 (muchang→1)
POST   /api/pets/{id}/withdraw           → 取出 (muchang→0)
POST   /api/pets/{id}/discard            → 丢弃 (免费,删除技能+装备)
POST   /api/pets/heal/{id}               → 治疗 (50金币)
POST   /api/pets/heal-all                → 全体治疗
```

### 3.4 战斗 (7)
```
POST /api/battle/init         {petId, monsterId} → BattleState + skills + message
POST /api/battle/action       {action, skillId?, bagId?} → Phase 1 结果
POST /api/battle/monster-turn {sessionId} → Phase 2 怪物反攻
POST /api/battle/use-item     {bagId, sessionId} → 战斗中使用道具
POST /api/battle/auto-fight   {mode: "gold"|"yb"|"stop"} → 自动战斗开关
POST /api/battle/flee         {sessionId} → 逃跑
GET  /api/battle/state        → 当前战斗状态 (重连)
```

### 3.5 背包 (6)
```
GET  /api/bag             → 背包列表
GET  /api/bag/equipment   → 装备列表
POST /api/bag/use/{id}    {petId} → 使用道具
POST /api/bag/equip/{id}  {petId} → 穿戴装备
POST /api/bag/unequip/{id} → 卸下装备
POST /api/bag/sell/{id}   → 出售
```

### 3.6 地图 (2)
```
GET /api/map/list          → 地图列表 (含 img 字段)
GET /api/map/{id}/monsters → 地图怪物 (按 map.level 范围)
```

### 3.7 商店 (2)
```
GET  /api/shop/list?type=props|equip|prestige|yb|sj → 商店物品
POST /api/shop/buy/{id}   {count, currency} → 购买
```

### 3.8 管理 (4)
```
POST /api/admin/reset-pets?uid=       → 重置宠物等级
POST /api/admin/add-pet?uid=&petName= → 创建宠物
POST /api/admin/add-item?uid=&propId= → 添加道具
POST /api/admin/add-skill?petId=&skillSysId= → 添加技能
POST /api/admin/set-auto?uid=&gold=&yb= → 设置自动战斗次数
```

---

## 4. 战斗系统详解

### 4.1 状态机

```
WAITING → (player action) → PET_ACT → (monster turn) → WAITING
                                                 → WON (怪物死亡)
                                                 → LOST (宠物死亡)
Any → FLED (逃跑)
```

### 4.2 伤害公式

```java
baseDamage = (petAc + skillAckValue) × skillPlus - monsterMc
baseDamage = max(1, baseDamage)
finalDamage = baseDamage × hitRate + 1

hitRate = (attackerHits - defenderMiss) / 100, clamped [0.1, 1.5]

元素克制: 金克木 木克土 土克水 水克火 火克金
  优势 ×1.5, 劣势 ×0.7

暴击: 5% 基础 + 装备 crit%
  暴击时 damage ×2

随机浮动: -10% ~ +5%
```

### 4.3 BattleSession 数据结构

```java
// 不可变 (创建后不改)
sessionId, playerId, userPetId, monsterId
petName, petImg, petHeadImg, petImgAck, petImgDie, petLevel
monsterName, monsterImg, monsterImgAck, monsterImgDie, monsterLevel, monsterWx

// 可变状态
petHp/MaxHp, petMp/MaxMp, monsterHp/MaxHp
round, state, lastActionTime, createdAt

// 装备加成
equipAc, equipMc, equipHits, equipMiss, equipSpeed

// 技能冷却
Map<skillId, cooldownEndTime>

// 战斗日志
List<RoundLog> // {round, action, petDamage, petCrit, petMiss, petLifeSteal, petManasteal, petDamageDeepen, monsterDamage, monsterMiss, monsterDamageReduce, monsterDead, petDead}
```

### 4.4 技能冷却

```
skillDefId 319/320 → 299s
skillDefId 321/322 → 179s
skillDefId 323      → 119s
```

### 4.5 捕捉系统

```
捕捉率 = 球自带成功率% (从 effect: catch:id1|id2:rate%:flag 解析)
球的 target 怪物 ID 必须匹配当前怪物

成功: 创建 UserPet (从 bb 模板取图+属性), 携带满则自动入牧场
失败: 怪物反击
牧场满 + 携带满 → 拒绝捕捉
```

---

## 5. 经验与升级系统

### 5.1 升级公式

```java
// PHP saveGetOther() 移植
每级增量 = int(wx系数 × czl)

新HP = 当前HP + int(wx.hp × czl)
新MP = 当前MP + int(wx.mp × czl)
新攻击 = 当前攻击 + int(wx.ac × czl)
新防御 = 当前防御 + int(wx.mc × czl)
新命中 = 当前命中 + int(wx.hits × czl)
新闪避 = 当前闪避 + int(wx.miss × czl)
新速度 = 当前速度 + int(wx.speed × czl)

5元素抗性:
新金抗 = int(wx.j × czl) + 当前金抗
// 同理 m=木, s=水, h=火, t=土
```

### 5.2 双倍经验

```
dblexpflag:
  2 → 1.5x, 3 → 2x, 4 → 2.5x, 5 → 3x

自动战斗叠加:
  金币 auto → 1.2x
  元宝 auto → 1.5x

最终 = baseExp × dblexpflag倍率 × auto倍率
```

### 5.3 经验道具

```
addexp:(MIN,MAX) → 直接加经验, 触发升级检查
exp:MULT:DUR    → 设置 dblexpflag + 持续时间
```

---

## 6. 装备特效引擎

### 6.1 4 层解析顺序

```
1. Base effect (props.effect) + plus_tms_eft 强化加成
2. pluseffect (props.pluseffect) — 18种 key
3. 宝石孔 (userbag.F_item_hole_info) — 14种 key
4. 套装 (props.series + serieseffect) — 按件数递增
```

### 6.2 18种 Effect Key

**固定值:**
ac, mc, hp, mp, speed, hits, miss, addmoney, time

**百分比 (战斗中转为固定值):**
acrate = round(rate% × pet.ac)
mcrate, hprate, mprate, speedrate, hitsrate, missrate

**战斗特效 (依赖伤害值):**
```
hitshp (吸血):    rate% × 宠物造成伤害 → 回HP
hitsmp (吸魔):    rate% × 宠物造成伤害 → 回MP
dxsh (抵消):      rate% × 怪物造成伤害 → 减伤 (上限70%)
shjs (加深):      rate% × 宠物造成伤害 → 增伤
sdmp (伤转MP):    减伤 + 扣MP
szmp (受伤转MP):  rate% × 怪物造成伤害 → 加MP
crit (暴击率):    直接加成
```

### 6.3 宝石特殊规则

宝石孔中的 ac/mc/hp/mp/speed/hits/miss → 全部转为 **rate** (百分比) 计算!

---

## 7. 道具使用系统

### 7.1 使用逻辑

```
前端 needsPet() 判断是否需要宠物:
  varyname=1(辅助)/2(增益)/15(宠物蛋) → 需要
  effect 含 hp/mp/exp/addexp/openpet → 需要
  其他 → 直接使用

EXP 道具 (effect 含 exp/addexp):
  自动 target 主战宠物 (player.mbid)
  无主战 → alert

其他需要宠物的:
  1只宠物 → 自动选
  多只宠物 → 弹出选择框
```

### 7.2 效果类型

```
hp:N / mp:N                  → 回复HP/MP
addexp:(MIN,MAX) / addexp:N  → 增加经验 (直接加, 触发升级)
exp:MULT:DUR                 → 双倍经验 (设置 dblexpflag)
addyb:MIN,MAX                → 加元宝
addsj:MIN,MAX                → 加水晶
autofree:N / auto:N          → 加自动战斗次数
openpet:模板ID               → 创建宠物 (随机czl)
openmap:地图ID               → 解锁地图
addbag:N / addbag1:N         → 背包扩容
addck:N / addck1:N           → 仓库扩容
zhanshi:N                    → 加展示次数
giveitems:id:count,...       → 固定礼包
randitem:id:count:prob|...   → 随机宝箱 (1-in-N)
```

---

## 8. 前端面板清单 (24个)

### 8.1 游戏主视图

| 组件 | 功能 | gameView |
|------|------|----------|
| MapPanel | 野外探险 | 'map' |
| CityPanel | 中心城镇 (19栋建筑) | 'city' |
| PetList | 宠物资料 (3标签) | 'pets' |
| RanchPanel | 牧场 (PHP 对齐) | 'ranch' |

### 8.2 商店

| 组件 | 功能 | gameView |
|------|------|----------|
| ShopPanel | 道具店 | 'shop' |
| ZbPanel | 铁匠铺 | 'zb' |
| SmShopPanel | 神秘商店 | 'smshop' |
| AuctionPanel | 拍卖行 | 'auction' |
| DepotPanel | 仓库 | 'depot' |

### 8.3 叠加面板 (OverlayPanel)

| 组件 | Panel 名 |
|------|----------|
| BagPanel | 'bag' |
| EquipPanel | 'equip' |
| TaskPanel | 'tasks' |
| GuildPanel | 'guild' |
| RankPanel | 'rank' |
| TeamPanel | 'team' |
| InheritPanel | 'inherit' |
| MarryPanel | 'marry' |
| GmPanel | 'gm' |

### 8.4 战斗

| 组件 | 说明 |
|------|------|
| BattlePanel | 即时回合战斗 (动画+技能+捕捉+道具) |

---

## 9. Zustand Store 设计

### gameStore

```typescript
{
  // 宠物/背包
  pets: Pet[]; bag: Item[]; chatMessages: ChatMessage[]

  // 视图状态
  currentMapId: number; inBattle: boolean
  activePanel: Panel        // 叠加面板
  gameView: GameView        // 游戏主视图

  // 战斗状态
  battlePet: {id, name}
  battleMonster: {id, name}
  battleMapId: number | null

  // 刷新触发器
  refreshTrigger: number
}

Panel = 'pets'|'bag'|'equip'|'tasks'|'map'|'city'|'depot'|'zb'|'smshop'|'guild'|'shop'|'rank'|'gm'|'team'|'auction'|'inherit'|'marry'|null
GameView = 'map'|'city'|'pets'|'shop'|'depot'|'zb'|'smshop'|'auction'|'ranch'|null
```

### authStore

```typescript
{
  player: Player | null
  token: string | null
  loading: boolean
  login(username, password)
  logout()
  fetchPlayer()
}
```

---

## 10. 关键技术决策

1. **CSS Modules** — 组件级样式隔离，类名 hash 化
2. **BattleSession 内存管理** — ConcurrentHashMap, 30分钟过期
3. **React key 强制重挂载** — `key={petId-monsterId}` 确保战斗状态干净
4. **无 StrictMode** — 生产模式下 Effect 双次执行导致战斗 API 双重调用
5. **phase 状态守卫** — doAction/doCapture/doFlee 检查 phase==='waiting'
6. **cdFired ref** — 防止 countdown=0 粘滞重复触发
7. **状态合并** — handleBattleResponse 使用 `{...prev, ...s}` 保留 skills
8. **AdminController** — 开发期工具，上线前需移除

---

## 11. 文档索引

| 文档 | 内容 |
|------|------|
| BATTLE-SYSTEM.md | 战斗系统: 状态机, 伤害公式, API, 动画流程, 冷却 |
| BATTLE-SCENE.md | PHP 战斗场景分析 |
| DATA-MAPPING.md | PHP DB → Java Entity → 前端 Type 映射表 |
| PET-SYSTEM.md | 宠物/技能/装备系统概览 |
| EXP-LEVELING-SYSTEM.md | 经验升级: saveGetOther 公式, 双倍经验, 组队分配 |
| PET-ATTRIBUTES-SYSTEM.md | 宠物属性: czl 生成, wx 五行表, 创建流程 |
| SKILL-SYSTEM.md | 技能系统: 学习/升级/imgeft/冷却 |
| EQUIPMENT-EFFECTS-SYSTEM.md | 装备特效: 4层解析, 18种 key, 战斗中计算 |

---

## 12. 地图系统

### 地图等级→怪物映射

PHP 逻辑: 从 `map.level` 读范围 `"min,max"`, 随机等级 → `SELECT * FROM gpc WHERE level = $randLevel AND boss != 4`, 随机选 1 只。

```
Map ID | 名称       | 等级范围   | 显示怪物(来自 gpclist)
-------|-----------|-----------|---------------------------
1      | 新手基地   | 1-6      | 金波姆, 绿波姆, 水波姆, 火波姆, 土波姆
2      | 妖精森林   | 7-13     | 波光姆, 金光鼠, 波碧姆, 碧蟾, 波波姆, 水仙, 波纳姆, 火芒, 波岩姆, 魔岩卵
3      | 潮汐海崖   | 14-20    | 金波姆王, 绿波姆王, 水波姆王, 火波姆王, 土波姆王, 雷光鼠, 光驹, 老爷蛙, 弹簧蛇, 雪芙, 冰石机, 吸血蚊, 火龙蛋, 黑蚁
4      | 巨石山脉   | 21-26    | 黄金鸟, 雷炎鼠, 紫冥蟾, 冰露, 赤锦, 蚂蚁守卫
5      | 黄金陵     | 27-33    | 黄金独角兽, 化蛇王, 青龙兽, 火羽, 波姆兔
6      | 炽热沙滩   | 34-40    | 青龙兽, 血炎兽, 波姆兔王, 赌博鼠, 流氓奶牛, 贪食蛇, 香蕉猴
7      | 尤玛火山   | 41-50    | 圣羽天马, 黄金独角兽, 鬼精灵欧姆, 涅盘兽, 月亮兔, 战神兔
8      | 死亡沙漠   | 51-60    | 冰波姆, 雪孩子, 金鱼
9      | 海市蜃楼   | 61-70    | 高阶怪物 (100+级)
10     | 冰滩       | 71-80    | 高阶怪物 (100+级)
16     | 海底世界   | 81-83    | 海底怪物
100+   | 高阶地图   | 91-117   | 冰波姆/金鱼/高阶变异
```

### 世界地图 MAP_POINTS (27 个)

**Page 1 (map6.jpg):**

| ID | 名称 | 状态 |
|----|------|------|
| 1 | 新手训练营 | ✅ 开放 |
| 2 | 妖精森林 | ✅ 开放 |
| 3 | 潮汐海涯 | ✅ 开放 |
| 16 | 海底世界 | ✅ 开放 |
| 4 | 巨石山脉 | ✅ 开放 |
| 5 | 黄金陵 | ✅ 开放 |
| 6 | 炽热沙滩 | ✅ 开放 |
| 7 | 尤玛火山 | ✅ 开放 |
| 8 | 死亡沙漠 | ✅ 开放 |
| 9 | 海市盛楼 | ✅ 开放 |
| 10 | 冰滩 | ✅ 开放 |
| 11 | 伊苏王神墓 | 🔒 未开放 |
| 12 | 火龙王宫殿 | 🔒 未开放 |
| 13 | 史芬克斯穴 | 🔒 未开放 |
| 14 | 玲珑城 | 🔒 未开放 |
| 151 | 辉煌大道 | 🔒 未开放 |
| 15 | 圣诞小屋 | 🔒 未开放 |
| 128 | 遗忘宫殿 | 🔒 未开放 |
| 50 | 神圣战场 | 🔒 未开放 |

**Page 2 (map3.jpg):** 8 个开放地图 (100-121)

## 13. 怪物掉落概率

### droplist 格式

```
PHP: "propId:概率,propId:概率,..."  每个物品独立掷骰 1-in-N
```

### 示例

| 怪物 | droplist | 解读 |
|------|----------|------|
| 金波姆 | `1:100,149:200` | 精灵球 1/100, 金波姆球 1/200 |
| Boss | `94:3:1,748:200,749:5000` | 进化之书 1/3, ... |

### drop 计算

```java
for (part : droplist.split(",")) {
    propId:probability = part.split(":");
    if (rng.nextInt(probability) < 1) drops.add(propId);
}
// 每次最多掉落 count(entries) 种, 最少 0 种
```

## 14. 经验值参考

| 怪物等级 | 基础 EXP | 金币 |
|----------|----------|------|
| 1-10 | 10-50 | 5-20 |
| 11-20 | 50-200 | 20-80 |
| 21-30 | 200-500 | 80-200 |
| Boss | 1000-5000 | 500-2000 |

### 经验倍率叠加

```
最终EXP = 怪物基础EXP × dblexpflag倍率 × 自动战斗倍率
dblexpflag: 1=1x, 2=1.5x, 3=2x, 4=2.5x, 5=3x
自动战斗: 金币=1.2x, 元宝=1.5x
```

## 15. 商店分类

| 商店 | API type | 筛选条件 | 货币 |
|------|----------|----------|------|
| 道具店 | props | buy>0, yb=0, varyname≠9 | 金币 |
| 铁匠铺 | equip | buy>0, yb=0, varyname=9 | 金币/威望 |
| 威望道具 | prestige | prestige>0, varyname≠9 | 威望 |
| 威望装备 | zprestige | prestige>0, varyname=9 | 威望 |
| 元宝商店 | yb | yb>0 | 元宝 |
| 灵石商店 | sj | sj>0 | 水晶(灵石) |

## 16. 副本系统 (Dungeon/Instance)

### 三种模式

| 模式 | 标志 | 怪物来源 | 进度表 | 重置 |
|------|------|----------|--------|------|
| 副本(fb) | `gpclist='0'` | config.fuben.php gwid固定序列 | `fuben` (gwid, lttime, srctime) | 10-24h |
| 挑战(challenge) | `multi_monsters=1` | `c_gpc` 按难度随机 | `challenge` + `challenge_log` | 每日3次免费 |
| 通天塔(tower) | `multi_monsters=2` | `c_gpc` 按楼层 | `tgt` + `player_ext.tgt` | 每日1次免费, 55层 |

### 10 个副本配置

| ID | 名称 | 波数 | 冷却 |
|----|------|------|------|
| 11 | 伊苏王神墓 | 22 | 24h |
| 12 | 火龙王宫殿 | 30 | 10h |
| 13 | 史芬克斯密穴 | 27 | 12h |
| 14 | 玲珑城 | 29 | 15h |
| 50 | 厄非斯深渊 | 27 | 15h |
| 124 | 阿尔提密林 | 9 | 16.7h |
| 127 | 菲拉苛地域 | 16 | 20h |
| 143 | 熔岩地宫 | 15 | 20h |
| 144 | 幻魔之境 | 25 | 20h |
| 151 | 辉煌大道 | 12 | 24h |

### 进度保存

```
副本: fuben 表 (uid, gwid, inmap, lttime, srctime)
  - 怪物死亡→gwid指向下一个
  - 全部清完→gwid='' + 开始冷却计时
  - 冷却期满→重置gwid

挑战: challenge 表 (uid, gid, nums, snums, flag, vary)
  - nums: 已完成尝试次数(每日重置)
  - snums: 水晶重启次数
  
通天塔: tgt 表 (uid, gid, boss) + player_ext(tgt, tgttime)
  - boss: 当前楼层(最大55)
  - 每日重置
```

### 水晶成本

| 操作 | 水晶 | 条件 |
|------|------|------|
| 挑战刷新怪物 | 10 | 每天限2次 |
| 挑战额外次数 | 50 | 超过3次免费后 |
| 通天塔重进 | (楼层+1)×20 | 同一天第二次 |
| 副本跳过冷却 | 道具或水晶 | map.needs配置 |

### 反外挂 (fight_log)

```sql
fight_log(uid, time, vary)
vary=1: 战斗动作, <2秒→封禁
vary=2: 怪物加载, <1秒→封禁
```

### Boss冷却 (boss_refresh)

```
boss=3 的首领怪:
  - 击杀后 dtime=now+rand(60,900)  →  1-15分钟冷却
  - glock=1 时 rtime+120 > now  →  战斗中锁定
  - glock=1 且 rtime+600 < now  →  锁过期可抢占
```

### 掉落日志

- `T_fight_log`: 仅 Boss 掉落 (boss==3)
- `gamelog`: Boss刷新/装备分解等 (保留15天)
- 普通掉落直接入 userbag，无独立日志

## 17. 相关代码文件映射

### PHP → Java 对照

| PHP 文件 | 功能 | Java 对应 |
|----------|------|-----------|
| `Fight_Mod.php` | 战斗入口页面 | `BattleService.initBattle()` |
| `FightGate.php` | 战斗AJAX后端 | `BattleService.performAction()` |
| `FightGate.php:1074` | 挑战进度更新 | ❌ |
| `FightGate.php:1333` | 通天塔死亡处理 | ❌ |
| `fbfight_Mod.php` | 副本战斗入口 | ❌ |
| `fbfightGate.php:699-758` | 副本进度保存 | ❌ |
| `fb_Mod.php` | 副本入口页 | ❌ |
| `getmap.php` | 地图验证+水晶操作 | ❌ |
| `mapGate.php:120-205` | 副本冷却跳过 | ❌ |
| `ttGate.php` | 通天塔入口 | ❌ |
| `sec_common_fnc.php:1754` | `getProps()` 掉落计算 | `BattleService.parseDropList()` |
| `sec_common_fnc.php:1793` | `saveGetPropsa()` 保存道具 | `BattleService.addDropsToBag()` |
| `sec_common_fnc.php:1893` | `saveGetOther()` 升级 | `LevelUpService.addExp()` |
| `sec_common_fnc.php:2693` | `updateBoss()` Boss冷却 | ❌ |
| `sec_common_fnc.php:3346` | `tgtgw()` 通天塔怪物 | ❌ |
| `config.fuben.php` | 副本配置 (10个) | ❌ |

### varyname 分类 (29种)

| varyname | 分类 | varyname | 分类 |
|----------|------|----------|------|
| 1 | 辅助类(药水) | 9 | 装备类 |
| 2 | 增益类 | 10 | 精炼类 |
| 3 | 捕捉类(精灵球) | 12 | 礼包类 |
| 5 | 技能书类 | 13 | 特殊类(功能) |
| 7 | 进化类 | 15 | 宠物卵 |
| 8 | 合体类 | 22-32 | 魔法石/宝石/洗练等 |
| PROGRESS.md | 迁移进度总览 + P0-P3 清单 |
