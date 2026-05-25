# EXP & Leveling System

> PHP 经验值与升级系统完整分析 — 优先度 #2
> 来源: `D:\code\kdjl\kdjl\kdjl\`

## 1. exptolv 表 (EXP Table)

**定义:** `config/tconfig.php` lines 40-43

| Field | 说明 |
|-------|------|
| `level` | 宠物等级 |
| `nxtlvexp` | 升到下一级所需经验值 |

内存键: `MEM_EXP_KEY = 'db_exptolv'` (config.game.php line 36)

启动时通过 `vm1.php` 的 `loadmem('exptolv')` 加载到 Memcache。

## 2. 核心升级函数 saveGetOther()

**文件:** `sec/sec_common_fnc.php` lines 1893-2014
**修复版:** `kernel/task.v1.php` lines 1147-1252 (支持递归多级升级)

### 2.1 流程

```
1. 等级上限检查: level >= 130 → return false
2. 计算累积经验: willexp = nowexp + gained
3. 判断升级: willexp >= lexp ?
   ├─ 否 → UPDATE nowexp = nowexp + gained → 结束
   └─ 是 → 升级流程
```

### 2.2 升级属性重算公式

**文件:** `sec/sec_common_fnc.php` lines 1950-1962

```php
// 五行抗性 (5个)
$jk = intval($czz['j'] * $bb['czl']) + $kx[0];  // 金抗
$mk = intval($czz['m'] * $bb['czl']) + $kx[1];  // 木抗
$sk = intval($czz['s'] * $bb['czl']) + $kx[2];  // 水抗
$hk = intval($czz['h'] * $bb['czl']) + $kx[3];  // 火抗
$tk = intval($czz['t'] * $bb['czl']) + $kx[4];  // 土抗

// 7项基础属性
$hp   = intval($czz['hp']   * $bb['czl']) + $db_bb['srchp'];
$mp   = intval($czz['mp']   * $bb['czl']) + $db_bb['srcmp'];
$ac   = intval($czz['ac']   * $bb['czl']) + $db_bb['ac'];
$mc   = intval($czz['mc']   * $bb['czl']) + $db_bb['mc'];
$sp   = intval($czz['speed'] * $bb['czl']) + $db_bb['speed'];
$hits = intval($czz['hits'] * $bb['czl']) + $db_bb['hits'];
$miss = intval($czz['miss'] * $bb['czl']) + $db_bb['miss'];
```

**公式:** `新属性 = int(wx系数 × czl) + 当前属性值`

每个属性是**累加**增长，czl 高的宠物每级获得更多属性。

### 2.3 SQL 更新

```sql
UPDATE userbb SET
  level = {$lv},
  ac = {$ac}, mc = {$mc},
  srchp = {$srchp}, hp = {$hp},
  srcmp = {$srcmp}, mp = {$mp},
  nowexp = {$now},
  lexp = {$lrs['nxtlvexp']},
  hits = {$hits}, miss = {$miss}, speed = {$sp},
  kx = '{$jk},{$mk},{$sk},{$hk},{$tk}'
WHERE id = {$bb['id']} AND uid = {$bb['uid']}
```

### 2.4 递归升级 (task.v1.php 修复)

`sec_common_fnc.php` 的版本有 bug：溢出经验存入 nowexp 但不检查再次升级。
`kernel/task.v1.php` 修复版用递归处理多级升级：

```php
if ($now > 0) {
    $bb = re-read from DB;
    $this->saveGetOther($bb, $now);  // 递归
}
```

## 3. 战斗经验获取

**文件:** `function/FightGate.php`

### 3.1 基础 EXP

怪物 `gpc.exps` 字段 → `$gs['exps']`

### 3.2 双倍经验系统 (usedProps)

**文件:** `sec/sec_common_fnc.php` lines 2243-2354

| dblexpflag | 倍率 | 来源 |
|-----------|------|------|
| 2 | 1.5x | 1.5倍经验卷轴 |
| 3 | 2x | 2倍经验卷轴 |
| 4 | 2.5x | 2.5倍经验卷轴 |
| 5 | 3x | 3倍经验卷轴 |

**道具 IDs:** 742-746, 1247, 1225, 2055

**时间堆叠:** 相同倍率可以叠加时间，上限 3600s
```php
if (同倍率) maxdblexptime = 3600 + 剩余时间
else maxdblexptime = 3600
```

### 3.3 自动战斗加成

| 模式 | 消耗 | 经验倍率 |
|------|------|----------|
| 金币版 | sysautosum-- | 1.2x |
| 元宝版 | maxautofitsum-- | 1.5x |

### 3.4 系统活动倍率

从 `db_timeconfig` 读取，支持每日时间段和每周时间段。

### 3.5 最终公式 (FightGate.php lines 996-1015)

```php
// 普通模式
$gs['exps'] = intval($gs['exps'] * $uProps['double']);

// 自动战斗模式 (exptype == 1)
$gs['exps'] = intval($gs['exps'] * $uProps['double'] * $uProps['doubleexp']);
```

## 4. 组队经验分配

**FightGate.php** lines 1136-1222

```php
$multiple = $memberNum * 0.2 + 1;     // 倍数
$expAvg = intval($totalExp * $multiple / $memberNum);
```

| 人数 | 倍数 | 每人获得 |
|------|------|----------|
| 2 | 1.4 | 70% |
| 4 | 1.8 | 45% |
| 5 | 2.0 | 40% |

队长自动战斗额外 1.2x。

## 5. 其他 EXP 来源

| 来源 | 文件 | 公式 |
|------|------|------|
| 经验月饼道具 | `usedProps.php` lines 1041-1052 | `addexp:MIN,MAX` → rand |
| 任务奖励 | `task.v1.php` lines 1022-1076 | 威望门槛过滤 |
| 战场功勋 | `ext_Battle.php` lines 369-427 | `exp = jg_points * bb_level * 100` |
| 消费转经验 | `consumption2exp.php` line 176 | `exp = 今日消费 * 等级 * 比例` |
| 挂机 (bot) | `botGate.php` line 116 | 累计地图怪物经验 |

## 6. 实现要点 (Java)

### 需要新建/修改

1. **ExpToLv 表/Repository** — 查找 `nxtlvexp` by level
2. **wx 表 Entity** — 元素成长系数
3. **LevelUpService** — `saveGetOther()` 移植
4. **BattleService 修改** — 应用 `usedProps()` 经验倍率

### 核心公式实现

```java
// 升级属性计算
int newHp = (int)(wxRow.getHp() * pet.getCzl()) + currentSrchp;
int newAc = (int)(wxRow.getAc() * pet.getCzl()) + currentAc;
// ... 7属性 + 5抗性

// 下一级 EXP
ExpToLv expRow = expToLvRepo.findByLevel(newLevel);
long newLexp = expRow.getNxtlvexp();

// 溢出经验递归
long overflow = totalExp - oldLexp;
if (overflow > 0) levelUp(pet, overflow);
```

### 双倍经验检查

```java
// dblexpflag → multiplier
double expMultiplier = switch (dblexpflag) {
    case 2 -> 1.5; case 3 -> 2.0;
    case 4 -> 2.5; case 5 -> 3.0;
    default -> 1.0;
};
// 自动战斗额外加成
if (isAutoFight) expMultiplier *= autoFightMode == YB ? 1.5 : 1.2;
```
