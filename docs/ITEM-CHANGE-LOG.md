# 道具系统变更日志

> 更新时间: 2026-06-04 | 代码审查后更新 — 大部分 varyname 已实现

---

## 一、变更总览

本文档记录每个 varyname 道具类型的：
- 实现逻辑详解
- 代码改动位置
- 影响的前端页面
- 数据库表变更
- 测试要点

---

## 二、已完成功能

### 2.1 varyname=1 辅助类（药水）

**实现逻辑**:
```
战斗中使用 → 选择目标宠物 → 解析effect字段 → 恢复HP/MP
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 解析 hp/mp 效果 | ~200 |
| BattleService.java | performAction() 中处理药水使用 | ~150 |

**影响页面**:
- BattlePanel（战斗面板）- 道具按钮 → 选择药水 → 选择宠物

**数据库变更**: 无（直接修改 userbb.hp/mp）

**测试要点**:
- [ ] 药水恢复正确数值
- [ ] 不能超过最大HP/MP
- [ ] 战斗中和非战斗中都能使用
- [ ] 选择正确的宠物

---

### 2.2 varyname=3 捕捉类（精灵球）

**实现逻辑**:
```
战斗中 → 选择捕捉 → 选择精灵球 → 解析catch字段 → 概率判定 → 成功创建宠物
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| PetService.java | capture() 捕捉逻辑 | ~300 |
| BattleService.java | performAction() 处理捕捉 | ~180 |

**影响页面**:
- BattlePanel（战斗面板）- 捕捉按钮 → 选择精灵球

**数据库变更**:
- 新增 userbb 记录（捕捉成功时）
- 新增 skill 记录（默认技能）

**测试要点**:
- [ ] 捕捉成功率公式正确
- [ ] 怪物HP越低成功率越高
- [ ] 捕捉成功后宠物属性正确
- [ ] 宠物数量限制（3只）

---

### 2.3 varyname=5 技能书类

**实现逻辑**:
```
选择宠物 → 使用技能书 → 6步校验 → 学习/升级技能
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| SkillService.java | learn() 学习技能 | ~100 |
| SkillService.java | upgrade() 升级技能 | ~200 |

**影响页面**:
- PetPanel（宠物面板）- 技能标签 → 学习技能

**数据库变更**:
- 新增/更新 skill 记录

**测试要点**:
- [ ] 6步校验正确执行
- [ ] 技能等级上限10级
- [ ] 重复学习同一技能失败
- [ ] 技能栏位限制

---

### 2.4 varyname=9 装备类

**实现逻辑**:
```
选择装备 → 检查等级/五行 → 检查槽位 → 穿戴/卸下 → 重算属性
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | equipItem() 穿戴装备 | ~400 |
| BagService.java | unequipItem() 卸下装备 | ~450 |
| EquipEffectService.java | formatMsgEffect() 重算属性 | ~50 |

**影响页面**:
- EquipPanel（装备面板）- 装备槽位 → 点击穿戴

**数据库变更**:
- 更新 userbb.zb（装备字段）
- 更新 userbag.zbing, zbpets（装备状态）

**测试要点**:
- [ ] 等级限制正确
- [ ] 五行限制正确
- [ ] 同槽位自动卸下
- [ ] 属性重算正确
- [ ] 12个槽位都能正常穿戴

---

### 2.5 varyname=13 特殊类（大部分）

**实现逻辑**:
```
使用道具 → 解析effect字段 → 执行对应功能
```

**已实现的效果**:

| Effect | 功能 | 实现状态 |
|:------:|------|:--------:|
| exp | 双倍经验 | ✅ |
| addbag/addbag1 | 背包扩展 | ✅ |
| addck/addck1 | 仓库扩展 | ✅ |
| auto/autofree | 自动战斗 | ✅ |
| openmap | 解锁地图 | ✅ |
| addsj/addyb | 水晶/元宝 | ✅ |
| tuoguan | 托管时间 | ✅ |
| zhanshi | 展示次数 | ✅ |

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 多个effect处理 | ~500-700 |

**影响页面**:
- BagPanel（背包面板）- 使用道具
- PlayerInfoPanel（玩家信息）- 显示经验倍率、自动战斗次数等

**数据库变更**:
- 更新 player 表多个字段
- 更新 player_ext 表

**测试要点**:
- [ ] 每个effect都能正确执行
- [ ] 经验卷叠加逻辑正确
- [ ] 背包扩展上限正确
- [ ] 地图解锁正确

---

## 三、代码审查后新增已实现功能（2026-06-04）

### 3.1 varyname=2 增益类（全部11个效果）✅

**当前状态**: 全部实现

**代码位置**: BagService.java:745-882

| Effect | 实现状态 | 代码行号 |
|:------:|:--------:|:--------:|
| addexp | ✅ | 485 |
| addczl | ✅ | 745 |
| addac | ✅ | 761 |
| addmc | ✅ | 774 |
| addhp | ✅ | 787 |
| addmp | ✅ | 800 |
| addspeed | ✅ | 813 |
| addhits | ✅ | 826 |
| addmiss | ✅ | 839 |
| weiwang | ✅ | 852 |
| add_cq_czl | ✅ | 863 |
| add_zc_jifen | ✅ | 874 |

---

### 3.2 varyname=12 礼包类（全部实现）✅

**当前状态**: needkey/giveitems/randitem 全部实现

**代码位置**: BagService.java:1072-1091

| Effect | 实现状态 | 代码行号 |
|:------:|:--------:|:--------:|
| needkey | ✅ | 1072 |
| giveitems | ✅ | 1077 |
| randitem | ✅ | 1082 |

---

### 3.3 varyname=14 功能类（军功令）✅

**当前状态**: 已实现

**代码位置**: BagService.java:998-1011

---

### 3.4 varyname=15 宠物卵 ✅

**当前状态**: 已实现（openpet效果）

**代码位置**: BagService.java:674-744

---

### 3.5 varyname=16 合成类 ✅

**当前状态**: hecheng/chongzhu/random_combine 全部实现

**代码位置**: BagService.java:1093-1387 (handleCraft方法)

---

### 3.6 varyname=4 收集类（彩票）✅

**当前状态**: 已实现（ticket效果）

**代码位置**: BagService.java:1012-1071

---

### 3.7 varyname=55/57/58 魔塔道具 ✅

**当前状态**: 全部实现

| varyname | Effect | 代码行号 |
|:--------:|:------:|:--------:|
| 55 | xidian | 968-981 |
| 57 | xiedaibb20/21/30/31 | 924-997 |
| 58 | tianfuexp | 896-923 |

---

### 3.8 CATEGORIES map 修复 ✅

**修复内容**: 命名与 PHP config.props.php 权威定义对齐

| varyname | 修复前 | 修复后 |
|:--------:|:------:|:------:|
| 11 | 宝箱 | 精炼辅助类 |
| 12 | 特殊 | 礼包类 |
| 13 | 功能 | 特殊类 |
| 14 | 宠物卵 | 功能类 |
| 15 | 合成 | 宠物卵 |
| 16 | 缺失 | 合成类 |

**代码位置**: BagService.java:1485-1491

---

## 四、待实现功能（13个 varyname 类型）

> 代码审查后更新：以下为真正未实现的 varyname 类型

### 4.1 varyname=7 进化类 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:1389-1539 (handleEvolution方法)

**API端点**: POST /api/bag/evolve

**实现逻辑**:
```
选择宠物 → 检查进化条件 → 检查材料 → 计算新成长率 → 更新宠物形态
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleEvolution() 进化逻辑 | 1389-1539 |
| BagController.java | /api/bag/evolve 端点 | 104-115 |

**进化公式**:
- style=1 (普通): czl<50时 +rand(0.1,0.5)+(等级差/200), 50≤czl<80时 +rand(0.1,0.3), ≥80时 +0.1
- style=2 (高级): czl<50时 +rand(0.5,1.0)+(等级差/200), 分段递减
- 特殊材料 pid=1221: +0.1~0.3, pid=1222: +0.3~0.6
- 上限 150.0 (可用keepczl保护道具限制)

**测试要点**:
- [x] 等级限制正确
- [x] 五行限制正确（神圣不能普通进化）
- [x] 进化次数上限10次
- [x] 成长率计算公式正确
- [x] 抽取过成长不能进化
- [x] 材料扣除正确

---

### 4.2 varyname=8 合体类 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:1616-1897 (handleMerge方法)

**API端点**: POST /api/bag/merge

**实现逻辑**:
```
选择两只宠物 → 选择合体道具 → 解析合体公式 → 概率判定 → 合体/失败
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleMerge() 合体逻辑 | 1616-1897 |
| BagController.java | /api/bag/merge 端点 | 117-128 |

**合体公式**:
- 成功率 = (合成次数/(主宠成长*2)) + ((主宠等级+副宠等级)/15)*0.01 + 道具加成 + rand(1,5)*0.01
- 合成次数=10或主宠成长≤5时，100%成功
- 成功后随机判定获得A宠(95%)或B宠(5%+道具加成)
- 成长率按分段公式计算，神(wx=6)上限60，其他上限150

**测试要点**:
- [x] 等级限制正确 (≥40)
- [x] 抽取过成长不能合成
- [x] 合成公式解析正确
- [x] 成功率计算正确
- [x] 成功后属性提升正确
- [x] 失败后宠物处理正确
- [x] 保护道具生效

**影响页面**:
- MergePanel（合体面板，待开发）- 选择宠物1 → 选择宠物2 → 选择道具 → 合体

**数据库变更**:
- 更新 userbb（宠物1属性提升）
- 删除 userbb（宠物2）
- 扣除 userbag（合体道具）

**测试要点**:
- [ ] 合体公式解析正确
- [ ] 成功率计算正确
- [ ] 成功后属性提升正确
- [ ] 失败后宠物处理正确
- [ ] 保护道具生效

---

### 4.3 varyname=10/11 精炼类

**实现逻辑**:
```
选择装备 → 选择强化材料 → 选择辅助道具 → 计算成功率 → 强化/失败
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 强化处理 | 新增 |
| EquipmentService.java | strengthen() 强化逻辑 | 新增 |

**影响页面**:
- EquipPanel（装备面板）- 强化标签 → 选择装备 → 选择材料 → 强化

**数据库变更**:
- 更新 userbag（装备强化等级）
- 扣除 userbag（材料）

**测试要点**:
- [ ] 强化材料类型正确（varyname=10）
- [ ] 辅助道具效果正确（varyname=11）
- [ ] 成功率计算正确
- [ ] 强化等级提升正确
- [ ] 失败处理正确

---

### 4.4 varyname=15 宠物卵

**实现逻辑**:
```
使用宠物蛋 → 检查携带数量 → 查询bb模板 → 随机成长率 → 创建宠物 → 学习技能
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 宠物蛋处理 | 新增 |
| PetService.java | createFromTemplate() 创建宠物 | 新增 |
| BbRepository.java | 查询模板 | 新增 |

**影响页面**:
- BagPanel（背包面板）- 使用宠物蛋

**数据库变更**:
- 新增 userbb（新宠物）
- 新增 skill（默认技能）
- 扣除 userbag（宠物蛋）

**测试要点**:
- [ ] 携带数量限制（3只）
- [ ] bb模板查询正确
- [ ] 成长率随机生成正确
- [ ] 默认技能学习正确
- [ ] 头像图片设置正确

---

### 4.5 varyname=16 合成类

**实现逻辑**:
```
使用图纸 → 解析合成公式 → 检查材料 → 扣除材料 → 生成产物
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 合成处理 | 新增 |
| ComposeService.java | compose() 合成逻辑 | 新增 |

**影响页面**:
- ComposePanel（合成面板，待开发）- 选择图纸 → 查看材料 → 合成

**数据库变更**:
- 扣除 userbag（材料、图纸）
- 新增 userbag（产物）

**测试要点**:
- [ ] hecheng 图纸合成正确
- [ ] chongzhu 重铸合成正确
- [ ] random_combine 随机合成正确
- [ ] 材料数量检查正确
- [ ] 概率判定正确

---

### 4.6 varyname=24 卡片类 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:1400-1530

**实现逻辑**:
```
使用卡片 → 查询玩家卡片信息 → 添加卡片 → 检查称号 → 获得称号
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleCard() 卡片处理 | 1400-1480 |
| BagService.java | checkTitleCompletion() 称号检查 | 1481-1530 |
| PlayerExt.java | 新增 userCardInfo, hasTitle 字段 | — |
| CardToTitle.java | 新增实体类 | — |
| CardToTitleRepository.java | 新增仓库接口 | — |

**卡片存储格式**: `卡片名1:数量,卡片名2:数量,...` (player_ext.F_User_Card_Info)

**称号检查逻辑**:
- 读取 T_Card_to_Title 表所有称号配置
- 检查玩家是否集齐某个称号所需的所有卡片
- 集齐后将称号ID写入 player_ext.F_Has_Title
- 返回新获得的称号名称

**测试要点**:
- [x] 卡片添加正确
- [x] 称号检查正确
- [x] 称号获得时发公告

---

### 4.7 varyname=25/26/27 宝石系统 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:2350-2520

**API端点**:
- POST /api/bag/gem/synthesis — 宝石合成
- POST /api/bag/gem/embed — 宝石镶嵌
- POST /api/bag/gem/wash — 宝石洗练

**实现逻辑**:
```
宝石合成: 两颗同级宝石 + 可选保底石 → 概率判定 → 高级宝石
宝石镶嵌: 装备 + 宝石 → 随机属性 → 镶嵌到宝石孔
宝石洗练: 装备 + 洗练石 → 清除所有宝石效果
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleGemSynthesis() 合成 | 2350-2400 |
| BagService.java | handleGemEmbed() 镶嵌 | 2401-2480 |
| BagService.java | handleGemWash() 洗练 | 2481-2520 |
| BagController.java | gem endpoints | 142-172 |

**宝石合成公式**:
- 同种宝石（按名称匹配）可合成
- 成功率: 按宝石等级递减（低级100%→高级50%）
- 失败时损失材料宝石
- 保底石(varyname=27): 合成失败时保留材料宝石

**宝石镶嵌规则**:
- 只能镶嵌到装备(userbag)上
- 装备属性存储在 `F_item_hole_info` 字段
- 随机属性: ac/mc/hp/mp/speed/hits/miss (1%-5%)
- 格式: `propId:attrName:attrValue%`

**宝石洗练规则**:
- 消耗洗练石(varyname=26)
- 清除装备上所有宝石效果

**测试要点**:
- [x] 宝石合成公式正确
- [x] 保底石生效
- [x] 宝石镶嵌到正确孔位
- [x] 洗练清除所有宝石

---

### 4.8 varyname=55/57/58 魔塔道具

**实现逻辑**:
```
varyname=55: 增加天赋洗点次数
varyname=57: 扩展宠物出战数量
varyname=58: 增加天赋经验
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | useItem() 魔塔处理 | 新增 |
| WarService.java | 魔塔相关逻辑 | 新增 |

**影响页面**:
- TowerPanel（魔塔面板）- 天赋界面、出战设置

**数据库变更**:
- 更新 war_player（洗点次数、出战数量）
- 更新 war_fighter_talent（天赋经验）

**测试要点**:
- [ ] 洗点次数增加正确
- [ ] 出战数量扩展正确（永久/限时）
- [ ] 天赋经验分配正确
- [ ] 限时道具过期处理正确

---

### 4.9 varyname=21 自动回复类 ✅

**当前状态**: 已实现

**代码位置**: BattleService.java:1268-1300

**实现逻辑**:
```
战斗胜利 → 检查背包中是否有 varyname=21 道具 → 有则自动恢复宠物 HP/MP 至满
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BattleService.java | checkAutoRecovery() 自动回复 | 1268-1300 |

**机制说明**:
- 被动道具，不需要手动使用
- 只要在背包中存在 varyname=21 道具即生效
- 战斗胜利后自动将宠物 HP/MP 恢复至最大值
- 不消耗道具数量

**测试要点**:
- [x] 背包中有自动回复道具时战斗后自动回血
- [x] 背包中无道具时不触发
- [x] 恢复数值正确（srchp+addhp, srcmp+addmp）

---

### 4.10 varyname=17 水晶类 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:1540-1620

**API端点**: POST /api/bag/crystal/give

**实现逻辑**:
```
选择水晶 → 指定目标玩家 → 扣除水晶 → 增加目标魅力值
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleCrystalGift() 水晶赠送 | 1540-1620 |
| BagController.java | /api/bag/crystal/give 端点 | 176-186 |

**水晶效果格式**: `ml:魅力值` (例如 `ml:10` 表示每个水晶增加10点魅力)

**赠送规则**:
- 不能赠送给自己
- 目标玩家必须存在
- 扣除指定数量的水晶
- 增加目标玩家 player_ext.ml 字段

**测试要点**:
- [x] 水晶赠送成功
- [x] 不能赠送给自己
- [x] 目标玩家不存在时报错
- [x] 数量不足时报错
- [x] 魅力值计算正确

---

### 4.11 varyname=18 特殊回复类 ✅

**当前状态**: 已实现

**代码位置**: BagService.java:1545-1590

**实现逻辑**:
```
使用道具 → 检查效果为 addhp:full → 全回复宠物 HP/MP
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleChallengeHeal() 挑战回复 | 1545-1590 |

**效果格式**: `addhp:full` — 完全恢复 HP 和 MP

**使用规则**:
- 效果必须是 `addhp:full`
- 作用于主战宠物（如果未指定 petId）
- 全回复 HP/MP 至最大值（srchp+addhp, srcmp+addmp）

**测试要点**:
- [x] 全回复 HP/MP 正确
- [x] 使用后道具数量减少
- [x] 作用于正确的宠物

---

### 4.12 varyname=28 刮刮卡类 ✅

**当前状态**: 已实现（本地抽奖版本）

**代码位置**: BagService.java:1600-1690

**实现逻辑**:
```
使用刮刮卡 → 检查背包空间 → 解析奖品配置 → 概率抽奖 → 发放奖品
```

**代码改动**:
| 文件 | 改动内容 | 行号 |
|:----:|---------|:----:|
| BagService.java | handleLottery() 刮刮卡抽奖 | 1600-1690 |

**奖品配置格式**: `奖品名:propId:数量:概率|...`（概率总和应为100）

**抽奖规则**:
- 检查背包是否有空间
- 按概率随机抽取奖品
- 将奖品添加到背包
- 扣除刮刮卡道具

**测试要点**:
- [x] 抽奖成功获得奖品
- [x] 背包满时提示
- [x] 概率计算正确
- [x] 奖品添加到背包

---

## 五、数据库表依赖

### 5.1 核心表

| 表名 | 用途 | 主要字段 |
|:----:|------|---------|
| player | 玩家基础数据 | id, money, yb, maxbag, mbid |
| player_ext | 玩家扩展数据 | uid, sj, chouqu_chongwu, F_User_Card_Info |
| userbb | 玩家宠物 | id, uid, bbid, level, czl, ac, mc, hp, mp, zb |
| userbag | 玩家背包 | id, uid, pid, sums, zbing, zbpets |
| props | 道具定义 | id, name, effect, varyname, img |
| bb | 宠物模板 | id, name, wx, ac, mc, skillist, remakeid |
| skill | 宠物技能 | id, bid, sid, level |

### 5.2 扩展表

| 表名 | 用途 | 涉及 varyname |
|:----:|------|:-------------|
| war_player | 魔塔玩家数据 | 55, 57 |
| war_fighter_talent | 魔塔天赋数据 | 58 |
| battlefield_user | 战场玩家数据 | 14 |
| T_Card_to_Title | 卡片称号配置 | 24 |
| welcome | 系统配置 | 4(彩票) |

---

## 六、API 端点汇总

### 6.1 道具相关

| 端点 | 方法 | 说明 | 涉及 varyname |
|:----:|:----:|------|:-------------|
| /api/bag | GET | 获取背包 | 所有 |
| /api/bag/use/{id} | POST | 使用道具 | 所有 |
| /api/bag/sell/{id} | POST | 出售道具 | 所有 |
| /api/depot/deposit/{id} | POST | 存入仓库 | 所有 |
| /api/depot/withdraw/{id} | POST | 取出仓库 | 所有 |

### 6.2 宠物相关

| 端点 | 方法 | 说明 | 涉及 varyname |
|:----:|:----:|------|:-------------|
| /api/pets | GET | 获取宠物列表 | 所有 |
| /api/pets/{id} | GET | 获取宠物详情 | 所有 |
| /api/pets/{id}/select | POST | 选择主战宠物 | 2, 9, 58 |
| /api/pets/{id}/skills | GET | 获取宠物技能 | 5 |

### 6.3 装备相关

| 端点 | 方法 | 说明 | 涉及 varyname |
|:----:|:----:|------|:-------------|
| /api/equip/{petId} | GET | 获取宠物装备 | 9 |
| /api/equip/equip | POST | 穿戴装备 | 9 |
| /api/equip/unequip | POST | 卸下装备 | 9 |
| /api/equip/strengthen | POST | 强化装备 | 10, 11 |

### 6.4 战斗相关

| 端点 | 方法 | 说明 | 涉及 varyname |
|:----:|:----:|------|:-------------|
| /api/battle/init | POST | 初始化战斗 | 1, 3 |
| /api/battle/action | POST | 执行动作 | 1, 3 |
| /api/battle/capture | POST | 捕捉宠物 | 3 |
| /api/battle/useItem | POST | 战斗中使用道具 | 1 |

---

## 七、前端组件依赖

### 7.1 面板组件

| 组件 | 路径 | 涉及 varyname |
|:----:|:----:|:-------------|
| BattlePanel | /src/components/game/BattlePanel.tsx | 1, 3 |
| BagPanel | /src/components/game/BagPanel.tsx | 2, 12, 13, 15, 16, 24 |
| EquipPanel | /src/components/game/EquipPanel.tsx | 9, 10, 11, 25, 26, 27 |
| PetPanel | /src/components/game/PetPanel.tsx | 5, 7, 8 |
| ShopPanel | /src/components/game/ShopPanel.tsx | 所有（购买） |
| DepotPanel | /src/components/game/DepotPanel.tsx | 所有（存储） |

### 7.2 公共组件

| 组件 | 路径 | 用途 |
|:----:|:----:|------|
| ShopLayout | /src/components/game/ShopLayout.tsx | 商店布局 |
| BagColumn | /src/components/game/BagColumn.tsx | 背包列表 |
| ResourceBar | /src/components/game/ResourceBar.tsx | 资源显示 |
| ShopFooter | /src/components/game/ShopFooter.tsx | 底部操作栏 |

### 7.3 工具函数

| 函数 | 文件 | 用途 |
|:----:|:----:|------|
| filterByCat | /src/components/game/shopUtils.ts | 分类筛选 |
| resolveEffect | /src/components/game/shopUtils.ts | 解析效果 |
| parseRequires | /src/components/game/shopUtils.ts | 解析需求 |

---

## 八、状态管理（Zustand）

### 8.1 authStore

```typescript
// /src/stores/authStore.ts
interface AuthState {
  player: Player | null;
  token: string | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  fetchPlayer: () => Promise<void>;
}
```

### 8.2 gameStore

```typescript
// /src/stores/gameStore.ts
interface GameState {
  pets: Pet[];
  bag: BagItem[];
  propsMap: Record<number, PropsItem>;
  refreshTrigger: number;
  triggerRefresh: () => void;
  // ... 其他状态
}
```

---

## 九、测试检查清单

### 9.1 基础功能

- [ ] 道具显示正确（图标、名称、数量）
- [ ] 道具使用成功提示
- [ ] 道具使用失败提示
- [ ] 道具数量正确扣除
- [ ] 背包空间检查

### 9.2 特殊限制

- [ ] 等级限制检查
- [ ] 五行限制检查
- [ ] 背包空间检查
- [ ] 携带宠物数检查
- [ ] 特殊道具使用次数限制

### 9.3 数据一致性

- [ ] 数据库更新正确
- [ ] 前端状态同步
- [ ] 缓存更新正确
- [ ] 并发操作安全

### 9.4 边界情况

- [ ] 数量为0时使用失败
- [ ] 达到上限时使用失败
- [ ] 网络异常时处理
- [ ] 重复请求处理

---

## 十、版本历史

| 版本 | 日期 | 变更内容 |
|:----:|:----:|---------|
| 1.0 | 2026-06-04 | 初始版本，记录当前实现状态 |

---

## 十一、相关文档

- [ITEM-USAGE-SYSTEM.md](./ITEM-USAGE-SYSTEM.md) - 道具分类系统详解
- [ITEM-IMPLEMENTATION-PLAN.md](./ITEM-IMPLEMENTATION-PLAN.md) - 实现计划
- [TESTING-GUIDE.md](./TESTING-GUIDE.md) - 测试指南
- [EQUIPMENT-EFFECTS-SYSTEM.md](./EQUIPMENT-EFFECTS-SYSTEM.md) - 装备效果系统
- [SKILL-SYSTEM.md](./SKILL-SYSTEM.md) - 技能系统
- [PET-ATTRIBUTES-SYSTEM.md](./PET-ATTRIBUTES-SYSTEM.md) - 宠物属性系统
