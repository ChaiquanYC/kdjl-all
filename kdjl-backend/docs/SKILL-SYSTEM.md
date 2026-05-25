# Skill System

> PHP 技能学习/升级/战斗系统完整分析 — 优先度 #3
> 来源: `D:\code\kdjl\kdjl\kdjl\`

## 1. 技能定义表 (skillsys)

**文件:** `config/tconfig.php` lines 100-113

| 字段 | 说明 | 格式 |
|------|------|------|
| `pid` | 技能书道具 ID | Integer |
| `name` | 技能名称 | String |
| `vary` | 类型: 1=攻击, 2=辅助, 4=被动 | Integer |
| `wx` | 五行 (0=无属性) | Integer |
| `ackvalue` | 伤害值 (每级逗号分隔) | `"50,100,200,..."` |
| `plus` | 伤害倍率 (每级逗号分隔) | `"0,0.1,0.2,..."` |
| `requires` | 学习要求 (每级逗号分隔) | `"lv:10,lv:20,..."` |
| `uhp` | HP 消耗 (每级逗号分隔) | `"0,10,20,..."` |
| `ump` | MP 消耗 (每级逗号分隔) | `"5,10,15,..."` |
| `ackstyle` | 0=被动, 1=近战, 2=远程, 3=自身 | Integer |
| `imgeft` | 特效图 + 永久属性加成 | `"addac:5%,..."` |
| `img` | 技能图标 | String |
| `skill_def_id` | 冷却组 ID | Integer |

**技能配置常量:** `config/config.skill.php`
```php
$_skill['vary']     = [1=>'攻击', 2=>'辅助'];
$_skill['wx']       = [1=>'金', 2=>'木', 3=>'水', 4=>'火', 5=>'土'];
$_skill['ackstyle'] = [1=>'进程', 2=>'远程', 3=>'自身', 0=>'被动'];
```

## 2. 技能学习 (get.Skill.php)

**文件:** `function/get.Skill.php`

### 检查流程

| 步骤 | 检查 | 失败 |
|------|------|------|
| 1 | 参数验证 (id, bid 整数) | die('0') |
| 2 | 宠物是否已学此技能 | die('10') |
| 3 | 背包中是否有技能书 (pid 匹配) | die('2') |
| 4 | 宠物等级 >= requires 要求 | die('3') |
| 5 | 元素匹配 (wx==0 或 wx==宠wx) | die('4') |
| 6 | 宠物限定检查 (only:模板ID) | die('11') |

### 学习成功

```sql
INSERT INTO skill(bid, sid, name, level, vary, wx, value, plus, img, uhp, ump)
VALUES({bid}, {skillsys_id}, '{name}', 1, '{vary}', '{wx}',
       '{ackvalue[0]}', '{plus[0]}', '{imgeft[0]}', '{uhp[0]}', '{ump[0]}')
```

```sql
UPDATE userbb SET skillist = CONCAT(skillist, ',', '{id}:1')
WHERE uid = {uid} AND id = {bid}
```

```sql
DELETE FROM userbag WHERE uid = {uid} AND pid = {skillsys.pid} AND id = {book.id}
```

**注意:** 技能书被整个删除 (DELETE)，不递减数量。

### imgeft 永久加成 (首次学习时)

| imgeft 类型 | 效果 | 修改字段 |
|------------|------|----------|
| `addmc:N%` | 永久加防御 N% | `userbb.mc += mc * N%` |
| `addac:N%` | 永久加攻击 N% | `userbb.ac += ac * N%` |
| `addhp:N%` | 永久加 HP N% | `userbb.srchp += srchp * N%` |
| `addmp:N%` | 永久加 MP N% | `userbb.srcmp += srcmp * N%` |
| `addhits:N%` | 永久加命中 N% | `userbb.hits += hits * N%` |

应用后将 `skill.img` 设为 0 (防止重复应用)。

## 3. 技能升级 (get.sjSkill.php)

**文件:** `function/get.sjSkill.php`

### 检查流程

| 步骤 | 检查 | 失败 |
|------|------|------|
| 1 | 宠物是否已学此技能 | die('0') |
| 2 | 升级材料检查 | die('2') |
| 3 | 宠物等级 >= 当前等级对应 requires | die('3') |
| 4 | 当前等级 < 10 (上限) | die('4') |

### 升级材料

| 技能类型 | 道具 ID |
|----------|---------|
| 非被动 (vary ≠ 4) | 733 |
| 被动 (vary = 4) | 1666 |

### 升级 SQL

```sql
UPDATE skill SET
  level = {nl}, value = '{ack[cl]}', plus = '{plus[cl]}',
  uhp = '{uhp[cl]}', ump = '{ump[cl]}', img = '{img[cl]}'
WHERE bid = {bid} AND sid = {id}
```

```sql
UPDATE userbag SET sums = ABS(sums - 1)
WHERE uid = {uid} AND id = {book.id} AND sums > 0
```

### 10级上限

`if (had['level'] >= 10) die('4')`

## 4. 技能在战斗中的应用

### 4.1 战斗加载 (Fight_Mod.php lines 764-815)

1. 解析 `userbb.skillist` → 提取技能 ID 列表
2. 加载所有技能记录
3. **过滤被动 (vary=4)** — 不显示在战斗 UI
4. 技能 112 (特定被动) 显式跳过
5. 注入模板 `#bbjn#` 占位符

### 4.2 技能伤害计算 (Ack::getSkillAck)

**文件:** `sec/sec_common_fnc.php` lines 2435-2517

```
基础伤害 = (攻击者ac + skill_ackvalue) × (1 + skill_plus/100) - 防御者mc
基础伤害 = max(1, 基础伤害)
命中倍率 = (攻击者hits - 防御者miss) / 100, 限制 [0.1, 1.5]
基础 = round(基础伤害 × 命中倍率)

暴击: ×2
非暴击: × (100 + rand(-10, +5)) / 100  (浮动 -10%~+5%)
最低: 1
```

### 4.3 HP/MP 消耗

**FightGate.php** lines 577-583:
```php
if (s_uhp < 0 || s_ump < 0) {
    // 负值 = 治疗技能，加回 HP/MP
} else {
    // 正常消耗
}
```

数据库更新 (lines 856-862):
```sql
UPDATE userbb SET hp = {nhp_bb}, mp = {nmp}, addmp = {addmp}, addhp = {addhp}
WHERE id = {rs['id']} AND uid = {uid}
```

### 4.4 技能冷却

**FightGate.php** lines 112-137 (session-based):

| skill_def_id | 冷却时间 |
|-------------|----------|
| 319, 320 | 299s |
| 321, 322 | 179s |
| 323 | 119s |

前端 `fight.js:180` 也有同步检查。

### 4.5 被动技能处理

被动技能 (vary=4 / ackstyle=0):
1. 不显示在战斗 UI
2. 学习/升级时通过 `imgeft` 应用永久属性加成
3. 技能 112 在战斗中提供额外伤害抵消 (`hpdx`)

## 5. 技能删除

**无独立删除端点。** 技能仅在以下情况被删除：
- 宠物对比替换 (`cmpGate.php:561` / `cmpGate1.php:392`)
- 宠物合体 (`mcGate.php:161`)
- 宠物转生 (`zsGate.php:538`)
- 超级转生 (`sszsInfo.php:480`)

所有使用相同 SQL: `DELETE FROM skill WHERE bid = {petId}`

## 6. Java 后端现状

已有 `SkillService.java` + `SkillController.java`:
- `learnSkill()` — 学习技能 (6技能上限)
- `upgradeSkill()` — 升级技能

**缺失:**
- 技能书消费 (DELETE userbag)
- imgeft 永久属性加成解析
- 技能冷却系统
- 被动技能过滤 (vary=4)
- 技能伤害公式中的 plus/super 解析

## 7. 实现要点

### 学习技能完整流程
```java
1. 验证技能书在背包
2. 检查宠物等级 >= requires[0]
3. 检查元素匹配
4. INSERT skill (level=1, 各字段取[0])
5. UPDATE userbb.skillist 追加
6. DELETE userbag 技能书
7. 处理 imgeft 永久加成
```

### 升级技能完整流程
```java
1. 检查材料 (733 或 1666) 在背包
2. 检查当前等级 < 10
3. 检查宠物等级 >= requires[currentLevel]
4. UPDATE skill (各字段取[currentLevel])
5. UPDATE userbag.sums -= 1
6. 处理 imgeft 永久加成
```

### 冷却实现
```java
Map<Long, Long> cooldowns; // skillDefId → cooldownEndTime
if (now < cooldowns.get(skillDefId)) throw "冷却中";
cooldowns.put(skillDefId, now + COOLDOWNS.get(skillDefId));
```
