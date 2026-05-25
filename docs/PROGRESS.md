# KDJL Migration Progress

> 口袋精灵 (KDJL) — PHP → React + Spring Boot 迁移进度 | 最后更新: 2026-05-24 — 5种副本类型全部完成
> 最后更新: 2026-05-24 — **道具系统修复+后台管理完成+文档整理**

| Phase | 内容 | 状态 | 完成度 |
|-------|------|------|--------|
| Phase 0 | 环境准备 & 基础设施 | ✅ 完成 | 100% |
| Phase 1 | 数据层迁移 | ✅ 基本完成 | ~80% |
| Phase 2 | 后端 API 重建 | ✅ 基本完成 | ~92% |
| Phase 3 | 前端 React 重建 | ✅ 基本完成 | ~95% |
| Phase 4 | UI对齐PHP原版 | ✅ 完成 | 布局/主题/背景图 |
| Phase 4 | 集成测试 & 上线 | ⏳ 待开始 | 0% |

## 运行环境

| 组件 | 端口 | 说明 | 启动命令 |
|------|------|------|---------|
| 游戏后端 (kdjl-server) | 8080 | Spring Boot REST API | `cd backend && mvn spring-boot:run -pl kdjl-server` |
| 后台管理 (kdjl-admin) | 8081 | Thymeleaf 管理面板 | `cd backend && mvn spring-boot:run -pl kdjl-admin` |
| 前端 (Vite) | 3001 | React 游戏前端 | `cd frontend && npm run dev` |

### 账号信息

| 系统 | 地址 | 用户名 | 密码 |
|------|------|--------|------|
| 游戏登录 | http://localhost:3001 | testuser | test123 |
| 后台管理 | http://localhost:8081 | admin | admin123 |
| 数据库 MySQL | localhost:3306/kdjl | kdjl | kdjl_pass |

### 后台管理功能
- 📊 仪表盘 — 统计卡片 + 7日活跃趋势 + 服务器状态
- 👤 玩家管理 — 搜索/详情(宠物/背包)/发放道具+金币+元宝+水晶/封禁
- 📦 道具管理 — 4600+条分页浏览+搜索
- 🐾 宠物模板 — bb表分页浏览
- 💰 消费统计 — 元宝消费排行/记录
| Redis 7 (Docker) | ⚠️ 可选 | 端口 6379 |
| MySQL 9.7 | ✅ 运行中 | 端口 3306, kdjl 库 117 表 |
| Redis 7 (Docker) | ⚠️ 可选 | 端口 6379 (未启动时后端仍可运行) |

测试账号: `testuser` / `test123`

---

## 系统完成度总览

### P0 — 核心战斗 ✅ 完成 — 2026-05-24

**2026-05-24 后端核心系统实现:**

**经验升级系统 (Priority #2):**
- [x] `ExpToLvRepository` — exptolv 表查询 `nxtlvexp`
- [x] `LevelUpService` — PHP `saveGetOther()` 移植: wx×czl属性增长、递归多级升级
- [x] `BattleService.applyRewardsOnce` — 双倍经验倍率 (dblexpflag 1.5x-3x)、exp结算
- [x] 战斗胜利后 always read-back pet `nowexp/lexp` 更新 UI

**宠物属性系统 (Priority #4):**
- [x] `Wx` Entity + `WxRepository` — wx 五行元素表 (7元素成长系数)
- [x] `getCzl()` — 从 bb.czl 范围字符串生成随机成长率
- [x] `UserPet.kx` — 五行抗性字段 getter/setter
- [x] `LevelUpService.doLevelUp()` — 升级时 wx×czl 重算 7属性+5抗性

**技能系统 (Priority #3):**
- [x] `SkillService` — 学习(6步PHP检查)、升级(10级上限)、技能书消费
- [x] `applyPermBoost()` — imgeft 永久属性加成 (addac/addmc/addhp/addmp/addhits%)
- [x] 技能冷却 (skillDefId 319-323: 119s/179s/299s)

**装备特效引擎 (Priority #1):**
- [x] `EquipEffectService` — 4层特效解析: base effect → pluseffect → 宝石孔 → 套装
- [x] 18种 effect key: flat(ac/mc/hp/mp/speed/hits/miss) + percent(rate%) + special(hitshp/dxsh/shjs/crit)
- [x] 百分比→固定值转换 (getzbAttrib 逻辑)
- [x] 战斗中计算: 暴击率(5%+装备crit)、吸血(hitshp%×伤害)、抵消(dxsh%×受伤 ≤70%)、加深(shjs%×伤害)
- [x] `RoundLog` 扩展: petManasteal, petDamageDeepen, monsterDamageReduce
- [x] 修复 DB换行符导致 NumberFormatException (parseLong/parseInt strip \n\r)

**2026-05-24 战斗工具栏完善:**

**工具栏 8 按钮对齐 PHP:**
- [x] tb1 (自动): 2x2面板 金币版(1.2x)/元宝版(1.5x) 开/关, 调用后端检查次数
- [x] tb2 (设置): 选自动战斗默认技能 (PHP window.parent.usejn)
- [x] tb3 (攻击): 普通攻击 Usejn(1)
- [x] tb4 (技能): 面板显示 普通攻击 + 已学技能
- [x] tb5 (辅助): 背包恢复道具 (HP/MP药水)
- [x] tb6 (捕捉): 背包捕捉道具列表, 选具体球, 道具加成捕捉率
- [x] tb7 (道具): 挑战道具 (占位)
- [x] tb8 (逃跑): 逃跑

**自动战斗完整流程 (PHP ext_Fight.php + usedProps):**
- [x] 开启→立即攻击第一下→倒计时加速(金币4s/元宝3s)→循环
- [x] 每场消耗1次, 用尽自动停止 (player.sysautosum/maxautofitsum)
- [x] 双倍经验: 金币1.2x, 元宝1.5x (叠加 dblexpflag)
- [x] 道具: autofree(金币次数), auto(元宝次数), autoteam(组队次数)

**背包道具使用系统 (PHP usedProps.php):**
- [x] 自动战斗次数道具: autofree/auto/autoteam
- [x] 礼包/宝箱: giveitems(固定), randitem(1-in-N随机)
- [x] 宠物蛋: openpet(从bb模板创建, 随机czl)
- [x] 非宠物道具跳过宠物选择器

**掉落系统修复 (PHP getProps):**
- [x] droplist格式修正: propId:概率 (1-in-N), 非 propId:数量
- [x] 每个物品独立掷骰, 可掉落0~N个不同物品
- [x] 背包容量检查, 满格静默丢弃

**怪物出现修复 (PHP Fight_Mod.php):**
- [x] 按map.level字段读真实等级范围
- [x] 过滤boss=4, findByLevelBetweenAndBossNot

**战斗系统 Bug 修复:**
- [x] React StrictMode 移除 (双重effect导致双重API调用/倒计时)
- [x] countdown=0 粘滞修复 (cdFired ref 防止重复触发)
- [x] phase 守卫 doAction/doCapture/doFlee (动画期间禁用)
- [x] handleBattleResponse 合并状态 (保留skills字段)
- [x] 掉落物品入背包 (addDropsToBag)
- [x] Player entity 添加 setAutoFitFlag/setSysAutoSum/setMaxAutoFitSum

**UI修复:**
- [x] 技能面板始终显示普通攻击 (PHP Usejn(1)内置)
- [x] 战斗结算框 CSS 调整 (228px, 373px宽)
- [x] 结算框不被聊天区遮盖 (main_t/main_b z-index)
- [x] gameBox overflow:visible
- [x] 攻击图复位 (胜利/死亡时换回站立)

**文档 (当前共13份):**
- [x] 战斗背景按地图动态加载 `/images/map/t{id}/{id}.{ext}`
- [x] 攻击图复位 (怪物死亡时宠物恢复站立、宠物死亡时怪物恢复站立)
- [x] EXP 条动态显示 (petNowexp/petLexp 百分比进度)
- [x] 侧边栏不再切换回欢迎页 (PHP 行为)
- [x] 主战宠物自动选择 (设为主战 → 战斗默认使用)
- [x] 继续探险/返回村庄 + 10s冷却加载页
- [x] 新账号宠物创建 (AdminController 开发工具)

**2026-05-24 Sprint 2 地图完善 (全部完成):**
- [x] 难度选择 (普通1x/困难1.3x/冒险1.6x EXP+金币)
- [x] 组队面板 (创建/解散/加入/踢人/审批/暂离)
- [x] 地图解锁 (needitem道具消耗/needww威望消耗)
- [x] 团队战斗 (队长发起/全员主战参战/回合制/奖励分配)
- [x] 战前难度传入 BattleController.initBattle(difficulty)

**文档 (13份):**
- [x] BATTLE-SYSTEM.md, BATTLE-SCENE.md, DATA-MAPPING.md
- [x] EXP-LEVELING-SYSTEM.md, PET-ATTRIBUTES-SYSTEM.md
- [x] SKILL-SYSTEM.md, EQUIPMENT-EFFECTS-SYSTEM.md
- [x] PET-SYSTEM.md (总览)

**战斗引擎:**
- [x] P0-1 即时回合制战斗 + 两步流程 (宠物攻击→伤害显示→怪物反攻→复位)
- [x] P0-2 技能HP/MP消耗 + 冷却系统 (299s/179s/119s) + 技能选择面板
- [x] P0-3 装备加成带入战斗 (AC/MC/Hits/Miss/Speed via BattleSession)
- [x] P0-4 捕捉精灵球消耗 + HP比例捕捉率 + 失败怪物反击 + imgDie死亡图
- [x] P0-5 抗加速: 2s行动间隔 + 10s战斗间隔冷却(从战斗开始计时)

**战斗动画 (对齐PHP fight.js):**
- [x] 宠物攻击态图片切换 (imgstand→imgAck) + 伤害数字 (3s)
- [x] 怪物攻击态图片切换 + 伤害数字 (2s)
- [x] 伤害格式: `技能名! -伤害值 !!` (红色斜体) / `miss !` (单感叹号)
- [x] 暴击前缀(红色粗体) + 生命汲取(绿色 #14FD10)
- [x] 死亡动画: imgDie切换 + CSS opacity淡出 + 计时器KO
- [x] 伤害定位: 宠物攻击=右侧620px, 怪物攻击=左侧50px (匹配PHP pfont)

**结果面板 (对齐PHP #result):**
- [x] 深绿背景(#025B26) + 居中 + z-index:10000
- [x] 胜利: 经验/金币/掉落(单行: `道具 xN，道具 xN`)/升级/捕捉成功
- [x] 失败: `宝宝 XXX 受到了严重伤害，已经不能战斗！！！`
- [x] 继续探险按钮: 同地图随机怪物 → 新战斗(含10s冷却)
- [x] 返回村庄按钮: 回到地图详情页
- [x] 冷却页面: 米黄背景(#FFFCEB) + loading.gif + 橙色倒计时(#F98F2C, 2em)

**数据流:**
- [x] BattleSession持久化: petImgAck/Die, monsterImgAck/Die (1.5s死亡淡出)
- [x] RoundLog: petDamage, petCrit, petMiss, petLifeSteal, monsterDamage, monsterMiss
- [x] 战斗间状态: battleMapId持久化 + key={petId-monsterId}强制重新挂载

### P1 — 养成+社交 (5/5 ✅)
- [x] P1-1 任务系统: 接受/完成/放弃 + 条件检查 + 道具名/怪物名解析
- [x] P1-2 宠物传承(转生): `new=(base+own*Lv/400+mate*Lv/800)*pearl`
- [x] P1-3 公会系统: 创建(1万金)/加入/退出/解散
- [x] P1-4 背包增强: varyname分类 + 道具出售 + 容量检查
- [x] P1-5 结婚系统: 求婚(定情信物)/接受/离婚(2000水晶/24h冷却)

### P2 — 内容丰富 (4/4 ✅)
- [x] P2-1 注册系统: 创建角色 + 6选1初始宠物 + 新手道具
- [x] P2-2 组队系统: 创建/加入/退出 (最多4人)
- [x] P2-3 排行榜: 金币榜/等级榜/声望榜
- [x] P2-4 拍卖行: 挂售/购买/取消

### P3 — 扩展 (5/6 ✅)
> 2026-05-23: UI主题对齐PHP原版完成 — 登录页背景/主界面布局/左侧图片导航/聊天区背景/签到面板/米黄亮色主题
- [x] P3-1 每日签到/在线奖励 + 签到按钮
- [x] P3-2 多频道聊天 (世界/公会/队伍)
- [x] P3-3 GM管理后台 (搜索玩家/发道具/改属性)
- [x] P3-4 宠物治疗 + 全体治疗 + 免费回城治疗
- [x] P3-5 传承/婚姻/拍卖前端面板
- [ ] 战场PvP / 要塞 / 卡片 / 活动

---

## API 端点清单 (71个)

### 认证 (2)
| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/pets/heal/{id}` | POST | 治疗宠物 (50金恢复HP/MP) |
| `/api/pets/heal-all` | POST | 全体治疗 |
| `/api/pets/set-main/{id}` | POST | 设置主战宠物 |
| `/api/auth/login` | POST | 登录, MD5密码, 返回JWT |
| `/api/auth/register` | POST | 注册, 6选1初始宠物 |

### 玩家 (1)
| `/api/player/me` | GET | 玩家完整信息 + 在线缓存 |

### 宠物 (6)
| `/api/pets` | GET | 宠物列表 |
| `/api/pets/{id}` | GET | 宠物详情+技能+zb |
| `/api/pets/capture/{id}` | POST | 捕捉 (HP因子+道具加成) |
| `/api/pets/{id}/skills` | GET | 已学技能 |
| `/api/pets/{id}/skills/learnable` | GET | 可学技能 |
| `/api/pets/{id}/skills/learn/{sid}` | POST | 学习 (需技能书道具) |
| `/api/pets/{id}/skills/upgrade/{sid}` | POST | 升级 (需道具733/1666) |

### 背包 (6)
| `/api/bag` | GET | 背包 (含分类标签) |
| `/api/bag/equipment` | GET | 装备列表 |
| `/api/bag/use/{id}` | POST | 使用道具 |
| `/api/bag/equip/{id}` | POST | 穿戴装备 (10槽位+lv/wx约束) |
| `/api/bag/unequip/{id}` | POST | 卸下装备 |
| `/api/bag/sell/{id}` | POST | 出售道具 |

### 战斗 (5)
| `/api/battle/pve` | POST | 批量PvE (兼容旧版) |
| `/api/battle/init` | POST | 初始化战斗会话 |
| `/api/battle/action` | POST | 单回合操作 (宠物攻击阶段) |
| `/api/battle/monster-turn` | POST | 怪物反攻阶段 |
| `/api/battle/flee` | POST | 逃跑 |
| `/api/battle/state` | GET | 战斗状态查询 |

### 地图 (2)
| `/api/map/list` | GET | 地图列表 |
| `/api/map/{id}/monsters` | GET | 地图怪物 |

### 怪物 (2)
| `/api/monsters` | GET | 怪物列表 |
| `/api/monsters/boss` | GET | Boss列表 |

### 商店 (2)
| `/api/shop/list` | GET | 商店道具 |
| `/api/shop/buy/{id}` | POST | 购买 (金币/元宝) |

### 任务 (4)
| `/api/tasks` | GET | 任务列表 (含道具名/怪物名) |
| `/api/tasks/accept/{id}` | POST | 接受任务 |
| `/api/tasks/complete/{id}` | POST | 完成任务 |
| `/api/tasks/abandon/{id}` | POST | 放弃任务 |
| `/api/tasks/visit/{npcId}` | POST | 拜访NPC |

### 传承 (7)
| `/api/inherit/available` | GET | 可配对宠物 |
| `/api/inherit/mine` | GET | 我的传承宠物 |
| `/api/inherit/join/{petId}` | POST | 加入配对池 |
| `/api/inherit/cancel/{petId}` | POST | 取消配对 |
| `/api/inherit/pair/{my}/{other}` | POST | 配对 |
| `/api/inherit/breed/{petId}` | POST | 开始培育 |
| `/api/inherit/complete/{petId}` | POST | 完成传承 (水晶加速) |

### 公会 (6)
| `/api/guild/list` | GET | 公会列表 |
| `/api/guild/my` | GET | 我的公会 |
| `/api/guild/{id}` | GET | 公会详情+成员 |
| `/api/guild/create` | POST | 创建 (1万金) |
| `/api/guild/join/{id}` | POST | 加入 |
| `/api/guild/leave` | POST | 退出/解散 |

### 结婚 (5)
| `/api/marriage/status` | GET | 婚姻状态 |
| `/api/marriage/propose` | POST | 求婚 (定情信物) |
| `/api/marriage/accept/{id}` | POST | 接受求婚 |
| `/api/marriage/divorce/request` | POST | 离婚 (2000水晶) |
| `/api/marriage/divorce/cancel` | POST | 取消离婚 (退水晶) |

### 组队 (4)
| `/api/team/list` | GET | 队伍列表 |
| `/api/team/my` | GET | 我的队伍 |
| `/api/team/create` | POST | 创建 |
| `/api/team/join/{id}` | POST | 加入 |
| `/api/team/leave` | POST | 退出 |

### 排行榜 (1)
| `/api/rank/{type}` | GET | 排行 (money/level/prestige) |

### 拍卖行 (3)
| `/api/auction/list` | GET | 拍卖列表 |
| `/api/auction/sell` | POST | 上架 |
| `/api/auction/buy/{id}` | POST | 购买 |
| `/api/auction/cancel/{id}` | POST | 下架 |

### 每日 (3)
| `/api/daily/online-reward` | POST | 在线奖励 (5档时间段: 10/30/60/120/300分钟, 按宠物等级发奖) |
| `/api/daily/online-reward/check` | GET | 查询在线奖励状态 (当前档位/剩余时间) |
| `/api/player/online/count` | GET | 在线玩家数 (5分钟活跃) |

### 仓库 (3)
| `/api/depot` | GET | 仓库物品列表 (bsum>0) |
| `/api/depot/deposit/{id}` | POST | 存放 (sums→bsum) |
| `/api/depot/withdraw/{id}` | POST | 取出 (bsum→sums) |

### GM (3)
| `/api/gm/player/search` | GET | 搜索玩家 |
| `/api/gm/give-item` | POST | 发放道具 |
| `/api/gm/give-money` | POST | 发放金币元宝 |

### 聊天 (1)
| `/ws` | WebSocket | STOMP聊天 |

---

## Service 层 (18个)

| Service | 功能 |
|---------|------|
| AuthService | MD5认证+JWT+注册 |
| PlayerService | 玩家信息+在线缓存+在线人数统计 |
| PetService | 宠物CRUD+捕捉(HP公式)+模板匹配 |
| BagService | 背包+道具使用+10槽装备引擎+22字段道具详情(varyname/effect/usages/series/plus等) |
| BattleService | 战斗引擎(单回合+批量)+BattleSession管理 |
| SkillService | 技能学习/升级(道具驱动)+10级数组+永久属性 |
| ShopService | 商品列表+金币/元宝购买+订单记录 |
| CacheService | Redis缓存抽象(Memcached兼容) |
| TaskService | 任务引擎+条件检查+道具/怪物名解析 |
| InheritanceService | 传承状态机+公式+水晶加速 |
| GuildService | 公会创建/加入/退出/解散 |
| MarriageService | 求婚/结婚/离婚(2000水晶/24h) |
| TeamService | 队伍创建/加入/退出 |
| AuctionService | 拍卖上架/购买/下架 |
| + BattleSessionManager | 战斗会话管理(ConcurrentHashMap) |

## Controller 层 (22个)

新增: DepotController (仓库存取), MapController扩展(19地图), DailyController重写(5档在线奖励)

---

## 前端面板 (20个)

| 面板 | 功能 | PHP背景图 |
|------|------|-----------|
| LoginPage | 登录页 | — |
| GameLayout | 主框架+导航+状态栏 | side.jpg/content.jpg/chat_bg.jpg |
| WelcomeBox | 欢迎页 (新宠亮相+公告+进入城镇) | bg.gif/r.jpg/ann.gif/gocity.gif |
| CityPanel | 城镇中心 (2页19建筑+雪碧图) | bitmap1.png/bitmap2_1.png/map.png |
| MapPanel | 野外探险 (2页世界地图+进入地图打怪) | map6.jpg/map3.jpg/map1/*.png |
| PetList | 宠物资料 (3标签: 装备/属性/技能) | pet_bg_l.jpg/pet_bg_r.jpg/pet_1_bg.jpg |
| BagPanel | 背包 (表格式+16类筛选+物品悬浮详情) | pack.gif/ico_btn.gif |
| DepotPanel | 仓库 (左右对照+存取操作) | cangku01-04.jpg |
| EquipPanel | 10槽位装备+穿戴约束 | — |
| BattlePanel | 即时回合战斗+攻击动画+技能选择+捕捉+逃跑 | ✅ 完成 |
| ShopPanel | 商城+金币/元宝购买 | — |
| ChatPanel | WebSocket聊天 (flex布局) | — |
| TaskPanel | 任务双栏+道具名/怪物名解析 | — |
| GuildPanel | 公会列表+创建+加入 | — |
| RankPanel | 等级/金币/声望排行榜 | — |
| TeamPanel | 队伍创建+加入+退出 | — |
| AuctionPanel | 拍卖上架+购买+下架 | — |
| GmPanel | GM工具 | — |
| InheritPanel | 宠物传承配对+培育+取回 | — |
| MarryPanel | 求婚/接受/离婚 | — |
| OverlayPanel | 可拖拽浮动面板容器 (showHeader控制标题栏) | — |

---

## 项目结构

```
kdjl/
├── backend/
│   ├── pom.xml
│   ├── kdjl-common/             # 共享模块 (63实体 + DTO)
│   └── kdjl-server/             # 服务模块
│       ├── controller/          # 21个 Controller
│       ├── service/             # 17个 Service
│       ├── repository/          # 21个 Repository
│       ├── security/            # JWT + Spring Security
│       ├── websocket/           # STOMP 聊天
│       ├── battle/              # 战斗会话管理
│       └── config/              # Redis + 异常处理
├── frontend/
│   └── src/
│       ├── api/                 # Axios + WebSocket
│       ├── stores/              # Zustand (auth + game)
│       ├── hooks/               # useWebSocket
│       ├── components/
│       │   ├── layout/          # GameLayout + ErrorBoundary
│       │   └── game/            # 9个游戏面板
│       ├── pages/               # LoginPage
│       └── types/               # TypeScript 类型
├── docs/                        # 项目文档 (17份)
│   ├── PROGRESS.md              # 迁移进度总览
│   ├── ROADMAP.md               # 开发路线图 (5 Sprint)
│   ├── ADMIN-SYSTEM-PLAN.md     # 后台管理系统PHP调研+计划
│   ├── ITEM-USAGE-SYSTEM.md     # 道具使用系统PHP vs Java对比
│   ├── ITEM-ISSUES.md           # 道具系统36项问题清单
│   ├── DATA-MAPPING.md          # PHP DB→Java→前端数据映射
│   ├── BATTLE-SYSTEM.md         # 战斗系统完整技术文档
│   ├── BATTLE-SCENE.md          # PHP战斗场景分析
│   ├── PET-SYSTEM.md            # 宠物属性/升级/技能/装备特效概览
│   ├── EXP-LEVELING-SYSTEM.md   # 经验升级系统 (优先度#2)
│   ├── PET-ATTRIBUTES-SYSTEM.md # 宠物属性/czl/wx系统 (优先度#4)
│   ├── SKILL-SYSTEM.md          # 技能学习/升级/战斗系统 (优先度#3)
│   └── EQUIPMENT-EFFECTS-SYSTEM.md # 装备特效引擎 (优先度#1)
└── docker-compose.yml

---
## 2026-05-24 道具系统修复 & 后台完善

### 道具使用系统 Bug 修复 (11/36)
- [x] needsPet 判断逻辑修正 (BagPanel.tsx) — 改为 varyname 白名单模式
- [x] 11个增益 effect 键实现: addczl/addac/addmc/addhp/addmp/addspeed/addhits/addmiss/weiwang/add_cq_czl/add_zc_jifen
- [x] tuoguan 托管时间, needkey 宝箱钥匙, jg 军功令, ticket 彩票
- [x] HP/MP 恢复边缘情况修复 (maxHp=0风险, 满血提示)
- [x] openpet 宠物卵完善: 自动学普通攻击, 3只上限检查, 模板不存在提示

### kdjl-admin 独立后台 (:8081)
- [x] Thymeleaf服务端渲染, 独立Spring Security (admin/admin123)
- [x] 仪表盘: 统计卡片 + 7日活跃趋势图 + 服务器状态
- [x] 玩家管理: 搜索/详情(信息+宠物+背包)/发放道具+金币+元宝+水晶
- [x] 封禁管理: 封号/解封/禁言/解禁
- [x] 道具管理: 4609条分页浏览+特效解析
- [x] 宠物模板: bb表分页浏览+属性
- [x] 消费统计: 元宝总额/排行/最近交易
- [x] 战斗日志: 按玩家查询最近50条

### 道具系统已知差异 (25/36 未实现)
剩余项均为完整子系统: varyname=4彩票/14军功/15宠物卵(完成)/16合成/22魔法石/24卡片/28刮刮卡/55-58魔塔天赋

### 当前API端点: 90+ | Service: 18 | Controller: 23 | 前端面板: 25
```

## 快速启动

```bash
# 后端 (端口 8080)
cd backend && mvn install -DskipTests -q
cd kdjl-server && SERVER_PORT=8088 mvn spring-boot:run

# 前端 (端口 3000)
cd frontend && npm run dev
```
