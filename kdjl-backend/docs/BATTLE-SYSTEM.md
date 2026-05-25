# Battle System

## Architecture Overview

Battle system is an interactive single-round combat between a player's pet and a wild monster. Each round: pet attacks → damage display → monster counter-attacks → damage display → wait for next action.

| Layer | Technology | Key Files |
|-------|-----------|-----------|
| Frontend | React + TypeScript + CSS | `src/components/game/BattlePanel.tsx`, `BattlePanel.module.css` |
| Backend | Spring Boot + Java 21 | `service/BattleService.java`, `battle/BattleSession.java`, `battle/BattleSessionManager.java` |
| State | In-memory ConcurrentHashMap | `BattleSessionManager.sessions` |

## API Endpoints

| Method | URL | Purpose | Phase |
|--------|-----|---------|-------|
| POST | `/api/battle/init` | Create battle session | Init |
| POST | `/api/battle/action` | Pet attacks (attack/skill/capture) | Phase 1 |
| POST | `/battle/monster-turn` | Monster counter-attacks | Phase 2 |
| POST | `/api/battle/flee` | Flee from battle | - |
| GET | `/api/battle/state` | Reconnection state query | - |

## BattleSession Fields

```
Session ID, Player ID, Pet/Monster IDs
Permanent display: petImg(stand), petHeadImg, petImgAck, petImgDie,
                   monsterImg(stand), monsterImgAck, monsterImgDie,
                   petName, monsterName, petLevel, monsterLevel, monsterWx
Mutable state: petHp/MaxHp, petMp/MaxMp, monsterHp/MaxHp, round, state
Equipment bonuses: equipAc, equipMc, equipHits, equipMiss, equipSpeed
Cooldowns: Map<skillId, cooldownEndTime>
Round logs: List<RoundLog>
```

Auto-expires after 30 minutes.

## Battle State Machine

```
WAITING → (player acts) → PET_ACT → (monster turn) → WAITING
                                                → WON (monster dead)
                                                → LOST (pet dead)
Any state → FLED (player flees)
```

## Damage Formula (PHP Ack.getSkillAck)

```
baseDamage = (petAc + skillAckValue) × skillPlus - monsterMc
baseDamage = max(1, baseDamage)
finalDamage = baseDamage × hitRate + 1
randomFloat: -10% ~ +5%
crit: ×2

Element multiplier:
  金(1)克木(2), 木克土(5), 土克水(3), 水克火(4), 火克金(1)
  Advantage: ×1.5, Disadvantage: ×0.7
```

Hit rate: `(attackerHits - defenderMiss) / 100`, clamped [0.1, 1.5].

## Animation Flow (matches PHP fight.js)

### Pet Attack (Phase 1)
```
t=0:     pet → imgAck (attack pose), damage number appears on right
         "暴击! -XXX !!" in yellow, miss in red italic
         Lifesteal "+XHP" in green
t=3.0s:  pet → imgstand (standing), damage fades, monster turn called
```

### Monster Counter-Attack (Phase 2)
```
t=0:     monster → imgAck (attack pose), damage number appears on left
t=2.0s:  monster → imgstand (standing), damage fades, countdown restarts
```

### Death
PHP uses `FadeOrShow(element, startAlpha, step)` — recursive setTimeout fading opacity.

### Countdown Timer
- 10 seconds displayed via `db.gif` background
- Counts down each second
- Restarts after each round completes
- Auto-attack when countdown reaches 0 (PHP feature, not yet implemented in React)

## Toolbar Buttons (zdzsk.gif background)

| Button | Position | Function | Status |
|--------|----------|----------|--------|
| tb1 | left:218px, top:7px | 自动 (auto-attack) | Calls attack |
| tb2 | left:259px, top:7px | 设置 (settings) | Placeholder |
| tb3 | left:299px, top:10px | 攻击 (attack) | Implemented |
| tb4 | left:340px, top:10px | 技能 (skill) | Opens skill panel |
| tb5 | left:384px, top:10px | 求助 (help) | Placeholder |
| tb6 | left:428px, top:10px | 捕捉 (capture) | Implemented |
| tb7 | left:472px, top:10px | 道具 (item) | Placeholder |
| tb8 | left:513px, top:10px | 逃跑 (flee) | Implemented |

## Image System

Each pet/monster has 3 image states stored in DB:

| DB Field | Purpose | Example | Directory |
|----------|---------|---------|-----------|
| `imgstand` | Standing/idle | `p1001z.gif` | `/images/bb/` or `/images/gpc/` |
| `imgack` | Attack pose | `p1001g.gif` | Same |
| `imgdie` | Death/defeat | `p1001d.gif` | Same |

PHP convention: standing filename contains `z`, attack contains `g`, skill contains `s`.
React uses `imgAck` field directly from session (pre-resolved from DB).

## Capture System

- Requires 精灵球 (poke ball) in bag (item IDs: 1, 1201)
- Catch rate: `max(0.1, 1.0 - hpRatio × 0.8)` — lower HP = easier
- Success: creates new UserPet from monster stats, session ends
- Failure: monster counter-attacks

## Skill Cooldowns

```
skillDefId 319/320 → 299s cooldown
skillDefId 321/322 → 179s cooldown
skillDefId 323      → 119s cooldown
```

## Anti-Speed-Hack

- **Per-action**: 2-second minimum interval between consecutive player actions (`checkActionInterval`). First attack after session creation is always allowed.
- **Inter-battle**: 10-second cooldown from battle START time (PHP `ftime+10`). If battle ends quickly, clicking "继续探险" shows cooldown page with loading spinner and countdown.

## Battle Result Panel

Matches PHP `#result` div styling:
- Dark green background (`#025B26`), positioned at `left:270px, top:87px`, 246px wide
- z-index: 10000 (above all battle elements)
- Victory: yellow "战斗胜利！" header, exp/money/drops/levelup/capture info
- Defeat: red "宝宝 {name} 受到了严重伤害，已经不能战斗！！！"
- Drops: single line format `掉落：道具A x1，道具B x2`
- Buttons: "继续探险" (yellow, continues with random monster on same map) + "返回村庄" (gray, goes back to map view)

## Inter-Battle Cooldown Page

When clicking "继续探险" before 10s has passed since battle start:
- Full-screen overlay: beige background (`#FFFCEB`)
- Centered `loading.gif` spinner (48x48px)
- Orange countdown number (`#F98F2C`, 2em bold) overlaid on spinner
- Counts down from remaining seconds to 0
- At 0: auto-navigates to next battle (matching PHP `pause(0)` → `Fight_Mod.php?s=t`)
- Replaces entire battle area (not an overlay — standalone page like PHP)

## Death Animation

- Killed side: image swaps to `imgDie` for 1.5s, then CSS opacity transition fades out
- Monster death: 0.6s fade-out → show victory result
- Pet death: 1s fade-out → show defeat result
- Timer displays red "KO" on battle end
- PHP `FadeOrShow` uses recursive setTimeout; React uses CSS `opacity` transition
