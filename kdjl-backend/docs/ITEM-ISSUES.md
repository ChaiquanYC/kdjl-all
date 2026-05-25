# KDJL 道具系统 — 全部问题清单

> 2026-05-24 | PHP `function/usedProps.php` (1980行) vs Java `BagService.useItem()`
> 最后更新: 已修复21/36项

---

## 一、needsPet 使用对象判断错误 (3项) ✅ 完成
> PHP: `function/usedProps.php:53-190` (varyname分发) | Java: `BagPanel.tsx:118`

| # | 问题 | 状态 | PHP参考 |
|---|------|------|---------|
| 1 | 双倍经验卷轴弹宠物选择 | ✅ varyname白名单 | usedProps:570-579 (exp→player) |
| 2 | 宠物蛋弹宠物选择 | ✅ varyname==15不选宠物 | usedProps:1585 (openpet→player) |
| 3 | 自动战斗卷轴弹宠物选择 | ✅ 仅hp/mp/permanent需要宠物 | usedProps:426-440 (auto→player) |

## 二、未实现的Effect效果键 (11项) ✅ 完成
> PHP: `function/usedProps.php:1002-1170` (varyname==2增益段) | Java: `BagService.java:applyEffectPart`

| # | Effect Key | 作用 | 状态 | PHP行号 |
|---|-----------|------|------|---------|
| 4 | `addczl` | 永久增加宠物成长率 | ✅ | usedProps:1040 |
| 5 | `addac` | 永久增加宠物攻击力 | ✅ | usedProps:1050 |
| 6 | `addmc` | 永久增加宠物防御力 | ✅ | usedProps |
| 7 | `addhp` | 永久增加宠物HP上限 | ✅ | usedProps |
| 8 | `addmp` | 永久增加宠物MP上限 | ✅ | usedProps |
| 9 | `addspeed` | 永久增加宠物速度 | ✅ | usedProps |
| 10 | `addhits` | 永久增加宠物命中 | ✅ | usedProps |
| 11 | `addmiss` | 永久增加宠物闪避 | ✅ | usedProps |
| 12 | `weiwang` | 增加玩家威望 | ✅ | usedProps |
| 13 | `add_cq_czl` | 增加抽取成长值 | ✅ | usedProps:1080 |
| 14 | `add_zc_jifen` | 战场积分 | ✅ 占位符 | usedProps |

## 三、未实现的道具类别 (13项)
> PHP分发入口: `function/usedProps.php:53` (switch on varyname)
> 道具配置: `config/config.props.php` (varyname映射)

| # | varyname | 类别名 | 状态 | PHP参考 |
|---|----------|--------|------|---------|
| 15 | 2 | 增益类(永久属性) | ✅ 完成 | usedProps:1002-1170 |
| 16 | 4 | 收集/彩票 | ⚠️ | usedProps:673-722 |
| 17 | 14 | 军功令 | ⚠️ | usedProps:1753-1785 |
| 18 | 15 | 宠物卵 | ✅ | usedProps:1585-1752 |
| 19 | 16 | 合成 | ❌ | usedProps:1322-1583 |
| 20 | 22 | 魔法石 | ❌ | usedProps:859-1001 |
| 21 | 24 | 卡片 | ❌ | usedProps:1171-1321 |
| 22 | 28 | 刮刮卡 | ❌ | usedProps:169-251 |
| 23 | 55 | 天赋洗点 | ❌ | usedProps:1786-1811 |
| 24 | 57 | 宠物栏扩展 | ❌ | usedProps:1812-1901 |
| 25 | 58 | 天赋经验 | ❌ | usedProps:1902-1963 |
| 26 | 6/7/8 | 卡片/进化/合体 | ❌ | usedProps (无对应段) |
| 27 | 19 | 涅槃加成 | ❌ | Sd_Mod.php |

## 四、功能道具的缺失Effect (4项) — 3完成
> PHP: `function/usedProps.php:252-671` (varyname==13特殊段)

| # | Effect Key | 状态 | PHP行号 |
|---|-----------|------|---------|
| 28 | `tuoguan` 托管时间 | ✅ player.tgTime | usedProps:426 |
| 29 | `addmc` 牧场扩展 | ✅ propId匹配 | usedProps:252-475 (pid区间) |
| 30 | `needkey` 宝箱钥匙 | ✅ keyPropId标记 | usedProps:723 |
| 31 | `openpet` 宠物卵 | ✅ 普攻+上限 | usedProps:1585 |

## 五、宝箱系统不完整 (3项) — 待实现
> PHP: `function/usedProps.php:723-858` (varyname==12), `859-1001` (varyname==22)

| # | 问题 | 状态 | PHP行号 |
|---|------|------|---------|
| 32 | varyname=12/22宝箱使用限制(背包>=3/等级) | ❌ | usedProps:730-740 |
| 33 | randitem公告标志(第4参数:1公告2不公告) | ❌ | usedProps:770-790 |
| 34 | 宝箱递归(宝箱内开出宝箱) | ❌ | usedProps |

## 六、宠物限制 — 待实现
> PHP: `function/usedProps.php:1026-1038` (wx==7神圣检查)

| # | 问题 | 状态 | PHP行号 |
|---|------|------|---------|
| 35 | 神圣宠物(wx==7)与非神圣道具(requires!='__SS__')互斥 | ❌ | usedProps:1026-1038 |

## 七、药水类问题 ✅ 已修复
> PHP: `function/usedProps.php` (hp/mp通过$bb主战宠物恢复)

| # | 问题 | 状态 | PHP行号 |
|---|------|------|---------|
| 36 | HP/MP恢复无溢出提示 | ✅ maxHp保底+满血提示 | usedProps (通用effect解析) |

---

## 总计

| 类别 | 项数 | 已修复 | 剩余 |
|------|------|--------|------|
| needsPet判断错误 | 3 | 3 | 0 |
| 缺失Effect键 | 11 | 11 | 0 |
| 缺失varyname类别 | 13 | 3 | 10 |
| 功能道具缺失 | 4 | 3 | 1 (addmc已有) |
| 宝箱不完整 | 3 | 0 | 3 |
| 宠物限制 | 1 | 0 | 1 |
| 药水问题 | 1 | 1 | 0 |
| **合计** | **36** | **21** | **15** |

## 修复进度

| 轮次 | 内容 | 状态 |
|------|------|------|
| 第一轮 | needsPet修正(3) + 增益effect(11) | ✅ |
| 第二轮 | tuoguan + needkey + addmc(已有) | ✅ |
| 第三轮 | jg军功 + ticket彩票 + openpet完善 + HP/MP修复 | ✅ |
| 剩余 | 15项子系统 (合成/卡片/天赋/宝箱等) | ⏳ |

## 剩余项目说明

剩余15项均为需要独立构建的子系统，不适合零散修补：
- **合成系统** (varyname=16): 260行PHP, hecheng/chongzhu/random_combine
- **卡片系统** (varyname=24): 150行PHP, F_User_Card_Info
- **魔法石** (varyname=22): 占卜屋宝箱
- **刮刮卡** (varyname=28): 外部API抽奖
- **天赋系统** (varyname=55/58): 洗点+经验
- **宝箱完善**: 限制检查/公告/递归
- **宠物限制**: 神圣宠物道具互斥

---

## PHP ↔ Java 关键文件对照

| PHP源文件 | 行数 | 功能 | Java对应 |
|-----------|------|------|----------|
| `function/usedProps.php` | 1980 | 道具使用主逻辑 | `BagService.useItem()` |
| `config/config.props.php` | ~100 | 道具分类定义(varyname映射) | `BagService.CATEGORIES` |
| `sec/sec_common_fnc.php` | ~1800 | zbAttrib装备属性/getProps掉落/通用函数 | `EquipEffectService`, `BagService.parseDropList` |
| `function/Fight_Mod.php` | ~950 | 战斗主模块(含自动/捕捉/奖励) | `BattleService` |
| `function/fbfightGate.php` | ~150 | 副本战斗入口+抗加速检查 | `DungeonController` |
| `function/ChallengeGate.php` | ~700 | 公会/要塞PvP挑战 | `PvpController` |
| `function/mapGate.php` | ~120 | 地图进入限制+解锁 | `MapController.unlockMap` |
| `function/manymapgate.php` | ~80 | 一图多等级(难度选择) | `DungeonConfig` |
| `function/Team_Mod.php` | ~600 | 组队界面+难度选择+在线列表 | `MapPanel` (teamCenter) |
| `function/Tt_Mod.php` | ~150 | 通天塔界面 | `TowerController` |
| `function/chatGate.php` | ~300 | 聊天+GM命令(禁言/封号/公告) | `FriendController`, `ChatPanel` |
| `config/config.fuben.php` | ~70 | 10个副本配置 | `DungeonConfig` |
| `config/config.game.php` | ~200 | 游戏全局配置+$_gm名单 | `application.yml`, `SecurityConfig` |
| `kernel/equipment.v1.php` | ~800 | 装备穿戴/卸下/10槽位 | `BagService.equipItem/unequipItem` |
| `kernel/team.v1.php` | ~650 | 队伍管理(创建/加入/踢人) | `TeamService` |
| `custom/addPropToUserGate.php` | ~150 | GM派送道具/元宝/水晶 | `AdminApiController.giveItem/giveCurrency` |
| `custom/adminIndex.php` | ~50 | 后台管理框架 | `kdjl-admin` 模块 |

---

## 最新修复进度 (2026-05-24 最终更新)

| 类别 | 总项 | 已修复 | 剩余 |
|------|------|--------|------|
| needsPet判断 | 3 | 3 | 0 |
| 缺失Effect键 | 11 | 11 | 0 |
| 缺失varyname类别 | 13 | 6 | 7 |
| 功能道具缺失 | 4 | 4 | 0 |
| 宝箱不完整 | 3 | 3 | 0 |
| 宠物限制 | 1 | 1 | 0 |
| 药水问题 | 1 | 1 | 0 |
| **合计** | **36** | **29** | **7** |

**剩余7项（全部需要完整子系统开发）：**

| # | varyname | 类别 | PHP行数 |
|---|----------|------|---------|
| 16 | 4 | 彩票 | usedProps:673-722 |
| 17 | 14 | 军功令(缺battlefield_user) | usedProps:1753-1785 |
| 19 | 16 | 合成(hecheng/chongzhu/random) | usedProps:1322-1583 |
| 20 | 22 | 魔法石 | usedProps:859-1001 |
| 21 | 24 | 卡片 | usedProps:1171-1321 |
| 22 | 28 | 刮刮卡 | usedProps:169-251 |
| 26 | 6/7/8/19 | 卡片/进化/合体/涅槃 | 分散 |
