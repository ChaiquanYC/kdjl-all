# Equipment Special Effects System

> PHP 装备特效引擎完整分析 — 优先度 #1
> 来源: `D:\code\kdjl\kdjl\kdjl\`

## 1. 特效数据存储

### props 表装备相关字段

| 字段 | 说明 | 格式 |
|------|------|------|
| `effect` | 基础效果 | `"ac:50,hp:200"` |
| `pluseffect` | 附加属性 | `"acrate:10,hitshp:5"` |
| `plusflag` | 可否强化 (1=可) | Integer |
| `plusget` | 强化等级加成值 | `"0,5,10,15,..."` |
| `plusnum` | 宝石孔数 | Integer |
| `plus_tms_eft` | 强化等级+数值 (userbag) | `"3,15"` (等级3, +15) |
| `series` | 套装ID | `"套装名:id1\|id2\|id3"` |
| `serieseffect` | 套装效果 | `"hp:100,hprate:5,..."` |
| `F_item_hole_info` | 镶嵌宝石效果 (userbag) | `"ac:15%,crit:3%,..."` |

## 2. 三大特效解析层

特效聚合顺序 (`sec_common_fnc.php` `formatMsgEffect()` lines 2760-3139):

```
第1层: base effect 字段
  ↓
第2层: pluseffect 字段 (18种 key)
  ↓
第3层: 宝石孔 F_item_hole_info (14种 key)
  ↓
第4层: 套装 serieseffect (18种 key, 按件数递增)
```

**伤害抵消上限:** dxsh ≤ 70%

## 3. 全部特效 Key 一览

### 3.1 固定值加成 (直接累加)

| Key | 累加到 | 说明 |
|-----|--------|------|
| `ac` | `$arr['ac']` | 攻击力 |
| `mc` | `$arr['mc']` | 防御力 |
| `hp` | `$arr['hp']` | 生命值 |
| `mp` | `$arr['mp']` | 魔法值 |
| `speed` | `$arr['speed']` | 速度 |
| `hits` | `$arr['hits']` | 命中 |
| `miss` | `$arr['miss']` | 闪避 |
| `addmoney` | `$arr['addmoney']` | 获胜金币加成 |
| `time` | `$arr['time']` | 等待时间减少 |

### 3.2 百分比加成 (getzbAttrib 中转为固定值)

| Key | 公式 | 目标字段 |
|-----|------|----------|
| `acrate` | `round(value% × pet.ac)` | → `zbattrib.ac` |
| `mcrate` | `round(value% × pet.mc)` | → `zbattrib.mc` |
| `hprate` | `round(value% × pet.srchp)` | → `zbattrib.hp` |
| `mprate` | `round(value% × pet.srcmp)` | → `zbattrib.mp` |
| `hitsrate` | `round(value% × pet.hits)` | → `zbattrib.hits` |
| `missrate` | `round(value% × pet.miss)` | → `zbattrib.miss` |
| `speedrate` | `round(value% × pet.speed)` | → `zbattrib.speed` |

### 3.3 战斗特效 (依赖伤害值计算)

| Key | PHP 变量 | 公式 | 效果 | 颜色 |
|-----|---------|------|------|------|
| **`hitshp`** | `hp1` | `value% × 宠物造成伤害` | 吸血 (伤害转HP) | 绿 #14FD10 |
| **`hitsmp`** | `mp1` | `value% × 宠物造成伤害` | 吸魔 (伤害转MP) | 蓝 #0067CB |
| **`dxsh`** | `hpdx` | `value% × 怪物造成伤害` | 伤害抵消 | 白 |
| **`shjs`** | `ack` | `value% × 宠物造成伤害` | 伤害加深 | 紫 #9900FF |
| **`sdmp`** | `hpdx`+`mp1` | `hpdx += N%×怪伤; mp1 -= N%×怪伤` | 伤转MP消耗 | — |
| **`szmp`** | `mp1` | `value% × 怪物造成伤害` | 受伤转MP | — |
| **`crit`** | `crit` | 直接累加 | 暴击率 | — |

### 3.4 宝石特殊规则

宝石孔中的 `ac/mc/hp/mp/speed/hits/miss` → 全部转为 **rate (百分比)** 计算！

| 宝石 Key | 实际存入 | 说明 |
|----------|----------|------|
| `ac` | `$arr['acrate']` | 宝石的攻击 = 攻击% |
| `mc` | `$arr['mcrate']` | 宝石的防御 = 防御% |
| `hp` | `$arr['hprate']` | 宝石的HP = HP% |
| `speed` | `$arr['speedrate']` | 速度% |
| `hits` | `$arr['hitsrate']` | 命中% |
| `miss` | `$arr['missrate']` | 闪避% |
| `dxsh/hitshp/crit...` | 直接透传 | 战斗特效不变 |

## 4. 战斗中应用 (FightGate.php)

### 4.1 固定属性加成 (lines 490-494)

```php
$rs['ac']    += $att['ac'];
$rs['mc']    += $att['mc'];
$rs['hits']  += $att['hits'];
$rs['speed'] += $att['speed'];
$rs['miss']  += $att['miss'];
```

### 4.2 暴击率 (lines 535-568)

```php
$Crit_rate = system_base_crit_rate (from welcome table, code 'crit_rate');
$Crit_rate += intval($att['crit']);  // 装备暴击加成
$Crit = (rand(0, 100) <= $Crit_rate) ? 1 : 0;
```

### 4.3 吸血/抵消/加深 应用 (lines 601-648)

```php
// getzbAttrib() 计算阶段
$att['hp1']  += round(hitshp% × $ack1);   // 吸血 = N% × 宠伤
$att['hpdx'] += round(dxsh% × $ack);       // 抵消 = N% × 怪伤
$att['ack']  += round(shjs% × $ack1);      // 加深 = N% × 宠伤
$att['mp1']  += round(hitsmp% × $ack1);    // 吸魔 = N% × 宠伤

// 实际应用
$gwac1 = $gwac - $att['hpdx'];             // 怪物伤害减抵消
$aobj->skillack += $att['ack'];            // 宠物伤害加加深
$sumhp = $att['hp1'] + other_sources;      // 吸血加入 HP 恢复
```

### 4.4 战斗输出字符串

```
宠物HP,MP,伤害值,技能名#怪物HP,怪物攻击名#掉落#对话
+ 吸血: ,<br />吸血 {hp1}
+ 吸魔: &nbsp;==<br />吸魔{mp1}&nbsp;
+ 抵消: <dx>抵销：{hpdx}
+ 加深: #<ack>伤害加深：{ack}
*{crit_flag}*{wx_type}
```

### 4.5 被动技能额外特效 (FightGate.php lines 617-643)

技能 `s_imgeft` 字段在战斗中额外提供:

```php
switch (jnar[0]) {
    case "hitshp": $att['hp1']  += round(num × $bback); break;
    case "dxsh":   $att['hpdx'] += round(num × $gwac);  break;
    case "shjs":   $att['ack']  += round(num × $bback); break;
}
```

**技能 112 (特定被动):** 额外查询并追加 dxsh。

## 5. 装备强化系统

**文件:** `function/ext_zbstrength.php`

### 5.1 强化概率

| 等级 | 成功概率 | 金币消耗 |
|------|----------|----------|
| 0→2 | 6/10 (60%) | 100~600 |
| 3→5 | 5/10 (50%) | 1000~2000 |
| 6→7 | 4/10 (40%) | 3000~3500 |
| 8→10 | 3/10 (30%) | 5000~10000 |
| 11→13 | 2/10 (20%) | 15000~30000 |
| 14 | 1/10 (10%) | 50000 |

**上限:** 15级

### 5.2 保护道具

| 类型 | 效果 |
|------|------|
| `suc:N` | 成功率 +N |
| `100suc:N` | 等级<N 时 100% 成功 |
| `baodi` | 失败掉2级，不销毁 |
| `baodeng` | 失败不掉级，不销毁 |
| 无保护 | 失败 → DELETE 装备 |

### 5.3 强化加成计算

```php
$plusget = props.plusget;              // "0,5,10,15,20,..."
$plus = $plusget[level];               // 取对应等级的值
$plusstr = "$level,$plus";             // 存入 plus_tms_eft
UPDATE userbag SET plus_tms_eft = '{level},{value}' WHERE id = {id}
```

这个 flat value **只加到 base effect** 上，不影响 pluseffect/宝石/套装。

## 6. 套装系统

### 6.1 识别

`series` 格式: `"套装名:id1|id2|id3|id4|id5|id6"`

### 6.2 激活规则

```sql
SELECT COUNT(id) FROM userbag WHERE pid IN (id1,id2,id3) AND uid = {uid} AND zbpets = {bid}
```
穿 N 件 → 激活前 N 个 serieseffect 效果。

### 6.3 serieseffect 格式

`"ac:50,hp:200,hprate:10,dxsh:5,..."` (逗号分隔，按件数递增生效)

### 6.4 已知 Bug

```php
case 'miss':
    $arr['miss'] += $seriesarr[0];  // BUG: 应该是 $seriesarr[1]
```
套装 miss 效果取了 key 名而非 value 值。

## 7. 称号 Buff 叠加

`T_Card_to_Title` 表提供额外一层效果，叠加在装备之上：

```sql
SELECT F_add_hp, F_add_mp, ..., F_dxsh, F_hitshp, F_shjs, ...
FROM player_ext, T_Card_to_Title
WHERE player_ext.now_Achievement_title = T_Card_to_Title.F_title_name
AND player_ext.uid = {uid}
```

## 8. 缓存机制

`getzbAttrib()` 使用两层 Memcache:

| 缓存键 | 内容 | 失效 |
|--------|------|------|
| `User_bb_equip_info_a_{bid}_{uid}` | 固定属性 (ac,mc,hp...) | 装备变更 |
| `User_bb_equip_info_b_{bid}_{uid}` | 战斗属性 (hpdx,hp1,ack...) | 每次战斗重算 |

## 9. 实现要点 (Java) — 按优先级

### Phase 1: 战斗特效引擎 (最优先)

当前 `BattleService` 已有 `equipBonuses` (ac/mc/hits/miss/speed 固定值)。
需要新增:

```java
class EquipEffects {
    long ac, mc, hp, mp, speed, hits, miss;  // 固定
    long acRate, mcRate, hpRate, mpRate;       // 百分比
    long hitshp, hitsmp;  // 吸血/吸魔
    long dxsh, shjs;      // 抵消/加深
    long crit;            // 暴击率
    long addmoney;        // 金币加成
}
```

### 计算流程

```java
// 1. 解析装备效果
EquipEffects eff = parseAllEquipEffects(petId);

// 2. 百分比转固定值
eff.hp += round(eff.hpRate * pet.getSrchp() * 0.01);
eff.ac += round(eff.acRate * pet.getAc() * 0.01);
// ...

// 3. 战斗中应用 (需要伤害值)
long lifesteal = round(eff.hitshp * petDamage * 0.01);
long reduce = round(eff.dxsh * monsterDamage * 0.01);  // 上限70%
long deepen = round(eff.shjs * petDamage * 0.01);

// 4. 伤害计算
long actualMonsterDamage = monsterDamage - reduce;
long actualPetDamage = petDamage + deepen;
long hpRecovered = lifesteal + ...;
```

### 特效解析

```java
// pluseffect 解析: "hitshp:5,dxsh:10,acrate:15"
for (String pair : pluseffect.split(",")) {
    String[] kv = pair.split(":");
    switch (kv[0]) {
        case "ac" -> eff.ac += Long.parseLong(kv[1]);
        case "hitshp" -> eff.hitshp += Integer.parseInt(kv[1]);
        case "acrate" -> eff.acRate += Integer.parseInt(kv[1]);
        // ...
    }
}
```
