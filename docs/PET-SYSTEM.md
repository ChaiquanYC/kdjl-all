# Pet, Skill, and Equipment Systems

> Data from PHP `D:\code\kdjl\kdjl\kdjl\` — analysis date: 2026-05-24

## 1. Pet Attributes

### Base Pet Table (bb) → UserPet.java

| Field | Chinese | Java Type | Description |
|-------|---------|-----------|-------------|
| `name` | 宝贝名字 | `String` | Pet name |
| `wx` | 五行 | `Integer` | Element: 1金2木3水4火5土6神7神圣 |
| `ac` | 攻击 | `Long` | Physical attack |
| `mc` | 防御 | `Long` | Magic/defense |
| `hp` | 生命 | `Long` | Health points |
| `mp` | 魔法 | `Long` | Magic points |
| `speed` | 速度 | `Long` | Speed |
| `hits` | 命中 | `Long` | Hit accuracy |
| `miss` | 躲避 | `Long` | Dodge/evasion |
| `czl` | 成长率 | `String` | Growth rate range e.g. "5.0,10.0" (bb table) |
| `kx` | 抗性 | `String` | 5-element resistances (comma-separated) |
| `nowexp` | 当前经验 | `Long` | Current EXP |
| `lexp` | 升级经验 | `Long` | EXP needed for next level |
| `subyl` | 减晕 | `Integer` | Stun resistance |
| `subsl` | 减睡 | `Integer` | Sleep resistance |
| `subdl` | 减毒 | `Integer` | Poison resistance |
| `subxl` | 减虚 | `Integer` | Void/weakness resistance |
| `subfl` | 减防 | `Integer` | Defense-break resistance |
| `subkl` | 减抗 | `Integer` | Resistance-debuff resistance |
| `subhl` | 减缓 | `Integer` | Slow resistance |

### Player Pet Table (userbb) — additional fields

| Field | Description |
|-------|-------------|
| `srchp / addhp` | Base HP + bonus HP → current max |
| `srcmp / addmp` | Base MP + bonus MP → current max |
| `addhp / addmp` | Equipment/skill permanent bonuses |
| `zb` | Equipment slots: `"pos:bagId,pos:bagId"` |
| `skillist` | Learned skills: `"skillId:level,skillId:level"` |

### Growth Rate (czl) System

**Source**: `sec/sec_common_fnc.php` lines 173-181, `saveGetOther()` lines 1893-2014

The `czl` field in `bb` (base pet) is stored as a range `"5.0,10.0"`. On pet creation:
```php
$ok = str_replace(".", "", $czl);  // "5.0,10.0" → "50,100"
$arr = split(",", $ok);            // [50, 100]
$num = rand($arr[0], $arr[1]);     // random 50-100
return $num/10;                    // 5.0-10.0
```

A pet with czl=10.0 gets 2x the stat gain per level compared to czl=5.0.

### Level-Up Stat Calculation

**Source**: `sec/sec_common_fnc.php` `saveGetOther()` lines 1949-1962

The wx (element) table provides per-element growth coefficients (`czz`). Each level-up:
```php
$hp   = intval($czz['hp']   * $bb['czl']) + $db_bb['srchp'];
$mp   = intval($czz['mp']   * $bb['czl']) + $db_bb['srcmp'];
$ac   = intval($czz['ac']   * $bb['czl']) + $db_bb['ac'];
$mc   = intval($czz['mc']   * $bb['czl']) + $db_bb['mc'];
$sp   = intval($czz['speed'] * $bb['czl']) + $db_bb['speed'];
$hits = intval($czz['hits']  * $bb['czl']) + $db_bb['hits'];
$miss = intval($czz['miss']  * $bb['czl']) + $db_bb['miss'];
```

Resistances are also recalculated per level using wx table element growth × czl.

### wx (Element) Table

**Source**: `config/tconfig.php` lines 115-129

| Field | Meaning |
|-------|---------|
| `j, m, s, h, t` | Per-element resistant growth coefficients |
| `hp, mp` | HP/MP growth per level |
| `ac, mc` | Attack/defense growth |
| `speed` | Speed growth |
| `hits, miss` | Hit/dodge growth |

---

## 2. Experience & Leveling

### EXP Table (exptolv)

| Field | Description |
|-------|-------------|
| `level` | Pet level |
| `nxtlvexp` | EXP required to reach next level |

Stored in memory as `MEM_EXP_KEY`.

### EXP Reward (FightGate.php lines 996-1015)

Base EXP from monster's `exps` field. Modified by:
- **Double-exp items**: `dblexpflag` → 1.5x (2), 2x (3), 2.5x (4), 3x (5)
- **Auto-fight modes**: Money mode 1.2x, YB mode 1.5x
- **System events**: `usedProps()` checks global double-exp flags

```php
$gs['exps'] = intval($gs['exps'] * $uProps['double']);
// With both auto-fight and double-exp:
$gs['exps'] = intval($gs['exps'] * $uProps['double'] * $uProps['doubleexp']);
```

### Level-Up Flow (sec_common_fnc.php lines 1893-2014)

1. Level cap: **130**
2. Check `nowexp + gained >= lexp` → level up
3. Recalculate all stats using wx coefficients × czl
4. Reset HP/MP to new max
5. Read new `lexp` from exptolv table
6. Carry over remaining EXP: `now = total - lexp`
7. Recalculate 5 resistances

---

## 3. Skill System

### Skill Definition (skillsys table)

**Source**: `config/tconfig.php` lines 100-113

| Field | Description |
|-------|-------------|
| `pid` | Skill book item ID |
| `name` | Skill name |
| `vary` | Type: 1=attack, 2=support |
| `wx` | Element (0=neutral) |
| `ackvalue` | Comma-separated damage values per level |
| `plus` | Comma-separated bonus multipliers per level |
| `requires` | Comma-separated level requirements per level |
| `uhp` | HP cost per level |
| `ump` | MP cost per level |
| `ackstyle` | 1=melee, 2=ranged, 3=self, 0=passive |
| `imgeft` | Effect image; also encodes permanent stat boosts |
| `skill_def_id` | Cooldown group ID |

### Skill Learning (get.Skill.php)

1. Check skill not already learned
2. Check player has skill book in bag
3. Check pet level >= required level from `requires[lv]`
4. Check element match (if skill wx != 0 and != pet wx → reject)
5. Check pet-specific requirements (if any)
6. Insert `skill` row at level 1
7. Append `skillId:1` to `userbb.skillist`
8. Consume skill book

### Skill Upgrading (get.sjSkill.php)

1. Check skill exists for this pet
2. Check upgrade material: pid=733 (normal) or pid=1666 (passive)
3. Check pet level >= required level for current skill level
4. Max level: **10**
5. Update skill row with next values from comma-separated arrays
6. Apply permanent stat boosts from `imgeft`:
   - `addmc:N%` — permanent defense increase
   - `addac:N%` — permanent attack increase
   - `addhits:N%` — permanent hit increase
   - `addhp:N%` — permanent max HP increase
   - `addmp:N%` — permanent max MP increase

### Skill Cooldowns

```
skillDefId 319/320 → 299s
skillDefId 321/322 → 179s
skillDefId 323      → 119s
```

---

## 4. Equipment Special Effects

### Effect Storage (props table)

Equipment items in the `props` table have these effect-related fields:

| Field | Description |
|-------|-------------|
| `effect` | Base effect: `"key:value,key:value"` |
| `pluseffect` | Bonus properties: `"key:value,key:value"` |
| `plusflag` | Enhancable: 1=yes, 2=no |
| `plusget` | Enhancement level effects |
| `plusnum` | Gem socket count |
| `plus_tms_eft` | Enhancement level × effect multiplier |
| `series` | Set ID |
| `serieseffect` | Set bonus effects |
| `F_item_hole_info` | Socketed gem effects |

### Effect Keys Reference

**Flat stat bonuses** (simple addition):
| Key | Effect |
|-----|--------|
| `ac` | Physical attack |
| `mc` | Magic/defense |
| `hp` | Health points |
| `mp` | Magic points |
| `speed` | Speed |
| `hits` | Hit accuracy |
| `miss` | Dodge |

**Percentage-based stats** (value% × baseStat):
| Key | Effect |
|-----|--------|
| `hprate` | N% of srchp → bonus hp |
| `mprate` | N% of srcmp → bonus mp |
| `acrate` | N% of ac → bonus attack |
| `mcrate` | N% of mc → bonus defense |
| `hitsrate` | N% of hits → bonus hit |
| `missrate` | N% of miss → bonus dodge |
| `speedrate` | N% of speed → bonus speed |

**Battle special effects** (value% × damage):
| Key | Effect | PHP Variable | Color |
|-----|--------|-------------|-------|
| `hitshp` | Lifesteal: X% of damage dealt → HP | `hp1` | Green #14FD10 |
| `hitsmp` | Manasteal: X% of damage dealt → MP | `mp1` | Blue #0067CB |
| `shjs` | Damage deepen: +X% of damage | `ack` | Purple #9900FF |
| `dxsh` | Damage reduce: -X% of incoming | `hpdx` | White |
| `sdmp` | Self-damage to MP: X% of taken dmg | — | — |
| `szmp` | Damage taken → MP gain | — | — |
| `crit` | Critical hit rate (additive) | — | Red |
| `addmoney` | Bonus gold per win | — | — |

### Effect Parsing (sec_common_fnc.php `formatMsgEffect()` lines 2760-3139)

Effects are aggregated in order:
1. Base `effect` field → flat values
2. Enhancement bonus (`plus_tms_eft[1]`) added to base
3. `pluseffect` field → flat + % + special effects
4. Gem/socket effects (`F_item_hole_info`) → same keys as pluseffect
5. Set bonuses (`serieseffect`) → incremental per set piece count

**Damage reduction cap**: 70% (`dxsh` capped at 70%, line 3124)

### Runtime Application (FightGate.php lines 490-648)

1. Flat stats added to pet stats: `ac += equipAc, hits += equipHits, etc.`
2. Medicine buffs applied
3. Critical rate = system_crit_rate + equipment_crit
4. `getzbAttrib()` called with damage values to compute lifesteal/hpdx/deepen
5. Passive skill effects parsed for hitshp/dxsh/shjs
6. Final monster damage: `actual = raw - hpdx`
7. Final pet damage: `actual = raw + ack`

---

## 5. Current Implementation Gaps

### Backend (Spring Boot)

| System | Status | Missing |
|--------|--------|---------|
| Pet attributes | ✅ | czl generation not implemented |
| EXP/Leveling | Partial | No wx table, level-up stat calc |
| Skill learning | ❌ | No learn/upgrade endpoints |
| Equipment effects | Partial | Only flat stats in battle; no %-based, no special effects (hitshp/dxsh/shjs/crit) |

### Frontend (React)

| System | Status | Missing |
|--------|--------|---------|
| Pet detail | ✅ | czl display, sub-resistance display |
| Skill panel | ❌ | Skill learn/upgrade UI |
| Equipment detail | ✅ | Special effect display in tooltips |

### Key Missing Backend Features
1. **wx table** — needed for level-up stat calculation and czl-based growth
2. **SkillService** — learn skill, upgrade skill, skill cooldowns
3. **Equipment effect engine** — parse pluseffect/gems/sets, calculate %-based stats, battle special effects
4. **Level-up** — saveGetOther() logic: stat recalculation, resistance recalculation, exptolv lookup
