# Data Mapping Reference

> PHP DB tables → Java Entities → Frontend Types

## Battle System

### gpc (Monsters) → Monster.java → BattleState.monster*

| PHP DB Field | Java Entity | Frontend BattleState | Notes |
|-------------|-------------|---------------------|-------|
| `id` | `Monster.id (Long)` | `monsterId` | PK |
| `name` | `Monster.name` | `monsterName` | |
| `level` | `Monster.level` | `monsterLevel` | |
| `hp` | `Monster.hp` | `monsterHp/MaxHp` | Current & max |
| `mp` | `Monster.mp` | - | |
| `ac` | `Monster.ac` | - | Physical attack |
| `mc` | `Monster.mc` | - | Magic attack |
| `hits` | `Monster.hits` | - | Hit rate |
| `miss` | `Monster.miss` | - | Dodge rate |
| `speed` | `Monster.speed` | - | |
| `wx` | `Monster.wx (Integer)` | `monsterWx` | Element: 1金2木3水4火5土 |
| `imgstand` | `Monster.imgstand` | `monsterImg` | Standing pose |
| `imgack` | `Monster.imgack` | `monsterImgAck` | Attack pose |
| `imgdie` | `Monster.imgdie` | `monsterImgDie` | Death pose |
| `catchv` | `Monster.catchv` | - | Catch probability |
| `catchid` | `Monster.catchItemId` | - | Required ball item ID |
| `droplist` | `Monster.droplist` | `drops[]` | `propId:count,propId:count` |
| `exps` | `Monster.exps` | `expGained` | EXP reward |
| `money` | `Monster.money` | `moneyGained` | Gold reward |
| `boss` | `Monster.boss` | - | 1=boss, 3=mini-boss |
| `kx` | `Monster.kx` | - | Element resistances |

### userbb (Player Pets) → UserPet.java → BattleState.pet*

| PHP DB Field | Java Entity | Frontend BattleState | Notes |
|-------------|-------------|---------------------|-------|
| `id` | `UserPet.id` | `petId` | PK |
| `name` | `UserPet.name` | `petName` | |
| `level` | `UserPet.level (Integer)` | `petLevel` | |
| `uid` | `UserPet.playerId (Long)` | - | Owner |
| `hp` | `UserPet.hp` | `petHp` | Current |
| `srchp` + `addhp` | `UserPet.srchp + addhp` | `petMaxHp` | Base + bonus |
| `mp` | `UserPet.mp` | `petMp` | Current |
| `srcmp` + `addmp` | `UserPet.srcmp + addmp` | `petMaxMp` | Base + bonus |
| `ac` | `UserPet.ac` | - | Physical attack |
| `mc` | `UserPet.mc` | - | Magic attack |
| `hits` | `UserPet.hits` | - | Hit rate |
| `miss` | `UserPet.miss` | - | Dodge |
| `speed` | `UserPet.speed` | - | |
| `wx` | `UserPet.wx (Integer)` | - | Element |
| `imgstand` | `UserPet.imgstand` | `petImg` | Standing pose |
| `imgack` | `UserPet.imgack` | `petImgAck` | Attack pose |
| `imgdie` | `UserPet.imgdie` | `petImgDie` | Death pose |
| `headimg` | `UserPet.headimg` | `petHeadImg` | Avatar icon |
| `nowexp` | `UserPet.nowexp` | - | Current EXP |
| `lexp` | `UserPet.lexp` | - | EXP needed for next level |
| `skillist` | `UserPet.skillList` | `skills[]` | Comma-separated skill IDs |
| `zb` | `UserPet.zb` | - | Equipment: `pos:bagId,pos:bagId` |

### skills (Pet Skills) → Skill.java → SkillInfo

| PHP DB Field | Java Entity | Frontend | Notes |
|-------------|-------------|----------|-------|
| `id` | `Skill.id` | `SkillInfo.id` | PK |
| `s_name` | `Skill.name` | `SkillInfo.name` | Skill name |
| `s_level` | `Skill.level` | `SkillInfo.level` | |
| `s_vary` | `Skill.vary` | - | Skill type |
| `s_uhp` | `Skill.uhp` | `SkillInfo.uhp` | HP cost |
| `s_ump` | `Skill.ump` | `SkillInfo.ump` | MP cost |
| `s_value` | `Skill.value` | - | Damage formula |
| `s_plus` | `Skill.plus` | - | Bonus multiplier |
| `skill_def_id` | `Skill.skillDefId` | - | Cooldown group |

### userbag (Player Inventory) → UserBag.java

| PHP DB Field | Java Entity | Notes |
|-------------|-------------|-------|
| `id` | `UserBag.id` | PK |
| `pid` | `UserBag.propId (Long)` | Item type ID |
| `uid` | `UserBag.playerId (Long)` | Owner |
| `sums` | `UserBag.sums (Integer)` | Quantity |
| `vary` | `UserBag.vary (Integer)` | 1=item, 2=equipment |
| `zbing` | `UserBag.zbing (Integer)` | 0=unequipped, 1=equipped (hide from bag views) |
| `zbpets` | `UserBag.equipPetId (Long)` | Which pet is equipped on |
| `sell` | `UserBag.sell (Integer)` | Sale price |
| `cantrade` | `UserBag.cantrade (Integer)` | Tradable flag |

### BattleState (Frontend) — Composite type

```typescript
interface BattleState {
  // Session
  sessionId: string; round: number; state: string;  // WAITING|PET_ACT|WON|LOST|FLED
  phase?: string;  // "monster_turn" to trigger monster phase

  // Pet
  petHp/MaxHp: number; petMp/MaxMp: number;
  petName: string; petLevel?: number;
  petImg?: string; petHeadImg?: string;
  petImgAck?: string; petImgDie?: string;

  // Monster
  monsterHp/MaxHp: number;
  monsterName: string; monsterLevel?: number; monsterWx?: number;
  monsterImg?: string; monsterImgAck?: string; monsterImgDie?: string;

  // Combat data
  skills?: SkillInfo[];
  message?: string;
  log?: RoundLog;  // Damage/round result

  // Result (WON only)
  won?: boolean; expGained?: number; moneyGained?: number;
  drops?: { propId: number; name: string; count: number }[];
  levelUp?: boolean; newLevel?: number;
  captureSuccess?: boolean; capturedPetId?: number;
}

interface RoundLog {
  round: number; action: string;  // "attack" | "skill:name" | "capture"
  petDamage: number; petCrit: boolean; petMiss: boolean;
  petLifeSteal: number;
  monsterDamage: number; monsterMiss: boolean;
  monsterDead: boolean; petDead: boolean;
}
```

### Image File Naming (PHP Convention)

| State | Pet Path | Monster Path | Suffix |
|-------|----------|-------------|--------|
| Standing | `/images/bb/p1001z.gif` | `/images/gpc/p1001z.gif` | `z` |
| Attack | `/images/bb/p1001g.gif` | `/images/gpc/p1001g.gif` | `g` |
| Skill | `/images/bb/p1001s.gif` | `/images/gpc/p1001s.gif` | `s` |
| Death | `/images/bb/p1001d.gif` | `/images/gpc/p1001d.gif` | `d` |

PHP does string replacement `filename.replace('z', 'g')` to swap attack image.
React uses `petImgAck` / `petImgDie` fields directly (pre-resolved from DB).

### Element System (五行)

| Value | Element | Char | Beats | Beaten By |
|-------|---------|------|-------|-----------|
| 1 | 金 (Metal) | 金 | 木 | 火 |
| 2 | 木 (Wood) | 木 | 土 | 金 |
| 3 | 水 (Water) | 水 | 火 | 土 |
| 4 | 火 (Fire) | 火 | 金 | 水 |
| 5 | 土 (Earth) | 土 | 水 | 木 |

Advantage multiplier: ×1.5, Disadvantage: ×0.7

### Battle UI Image Assets

| Image | Path | Size | Purpose |
|-------|------|------|---------|
| `battle_bg.jpg` | `/images/ui/` | 788×319 | Battle background |
| `zd01.gif` | `/images/ui/newmap/` | - | Pet name bar bg |
| `zd02.gif` | `/images/ui/newmap/` | 90×70 | Pet avatar frame |
| `dr01.gif` | `/images/ui/newmap/` | 114×28 | Monster header bg |
| `dr02.gif` | `/images/ui/newmap/` | 31×37 | Element badge |
| `dr03.gif` | `/images/ui/newmap/` | 96×11 | Monster HP bar bg (gray) |
| `dr04.gif` | `/images/ui/newmap/` | - | Monster HP fill (green) |
| `xthong01.gif` | `/images/ui/newmap/` | - | Pet HP bar fill (red) |
| `xtlan01.gif` | `/images/ui/newmap/` | - | Pet MP bar fill (blue) |
| `zdzsk.gif` | `/images/ui/newmap/` | 788×71 | Bottom toolbar |
| `db.gif` | `/images/ui/newmap/` | 81×83 | Countdown timer |
| `loading.gif` | `/images/ui/fight/` | - | Cooldown spinner |
