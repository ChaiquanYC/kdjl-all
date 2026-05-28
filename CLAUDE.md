# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

口袋精灵2 (KDJL) — browser-based pet-collecting/battling MMORPG. Migration from PHP 5 + jQuery to React 18 + Spring Boot 3.3.5 + Java 21.

```
kdjl-all/
├── kdjl/               # Original PHP codebase (~319 files, ~78K lines — reference only)
├── kdjl-backend/        # Java Spring Boot (Maven multi-module)
│   ├── kdjl-common/     # Shared JPA entities (63) + DTOs
│   ├── kdjl-server/     # Game API server (:8080) — 26 controllers, 18 services, 19 repos
│   └── kdjl-admin/      # Admin backend (:8081, Thymeleaf, separate SecurityConfig)
├── kdjl-frontend/       # React 18 + TypeScript + Vite (:3000)
├── db/                  # MySQL dumps (117 tables, ~4600 props, ~1000 monsters)
└── docs/                # Technical documentation (18 files)
```

## Build & Run

```bash
# Database (one-time)
mysql -u root -p kdjl < db/kdjl_mysql8_compatible.sql

# Backend — build common first, then run (order matters)
cd kdjl-backend
mvn install -pl kdjl-common -DskipTests
mvn spring-boot:run -pl kdjl-server     # Game API :8080
mvn spring-boot:run -pl kdjl-admin      # Admin panel :8081

# Frontend
cd kdjl-frontend
npm install
npm run dev         # :3000, proxies /api → :8080, /ws → ws://:8080
npm run build       # TypeScript check + production build
npm run test        # Vitest
npm run lint        # ESLint
```

**Accounts:** `testuser`/`test123` (game, uid=102, pet id=86) | `admin`/`admin123` (admin) | `kdjl`/`kdjl_pass` (DB)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript 5.6, Vite 6, Zustand 5, Axios, CSS Modules, @stomp/stompjs 7 |
| Backend | Java 21, Spring Boot 3.3.5, JPA/Hibernate, Spring Security + JWT (jjwt 0.12.6), Spring WebSocket STOMP |
| Database | MySQL 8.0+, 117 tables |
| Cache | Redis 7 (optional — backend runs without it) |

## Architecture

### Backend (kdjl-backend)

**Module layout:**
- `kdjl-common` — JPA entities + `ApiResponse` DTO (`{code, message, data, total, page, limit}`)
- `kdjl-server` — 26 Controllers, 18 Services, 19 Repositories, plus `battle/` (BattleSession/BattleSessionManager), `security/` (JWT filter), `websocket/` (STOMP chat)
- `kdjl-admin` — Thymeleaf admin panel, separate port + SecurityConfig (`admin`/`admin123`). PageController for pages, AdminApiController for REST

**Key service responsibilities:**

| Service | Key Functions |
|---------|--------------|
| `BattleService` | initBattle, performAction, monsterTurn, flee, auto-fight, parseDropList, addDropsToBag |
| `PetService` | CRUD, capture (HP-based formula), main-pet selection, ranch deposit/withdraw |
| `BagService` | useItem (effect parsing), equipItem/unequipItem (10 slots + lv/wx constraints), sell |
| `LevelUpService` | PHP `saveGetOther()` port — wx×czl stat growth, recursive multi-level, double EXP stacking |
| `EquipEffectService` | 4-layer effect parsing: base→pluseffect→gem holes→set bonuses, 18 effect keys |
| `SkillService` | learn (6-step PHP check), upgrade (10-level cap), imgeft permanent stat bonuses |
| `AuthService` | MD5 password (matches PHP), JWT HMAC-SHA384 24h, 6-pet starter registration |

**Battle system:**
- State machine: `WAITING → PET_ACT → (monster turn) → WAITING / WON / LOST / FLED`
- Damage: `(petAc + skillAckValue) × skillPlus - monsterMc`, min 1, then ×hitRate + random variance (-10% to +5%)
- Element advantage: 金→木→土→水→火→金 (×1.5 advantage, ×0.7 disadvantage)
- Crit: 5% base + equipment crit%, ×2 damage
- Skill cooldowns: skillDefId 319/320=299s, 321/322=179s, 323=119s
- BattleSession stored in `ConcurrentHashMap`, 30-min expiry
- Anti-cheat: `fight_log` table — action <2s or monster load <1s → ban

**Capture formula:**
- Ball's `effect` field contains `catch:id1|id2:rate%:flag` — target monster ID must match
- Success → create UserPet from bb template, random czl, auto-learn normal attack, 3-pet party limit
- Failure → monster counter-attacks
- Ranch full + party full → reject capture

### Frontend (kdjl-frontend)

**Layout system:** 1000px fixed width, side.jpg sidebar + content.jpg main area. Game area 788×319px using `zdzd_bj`/`team` background images matching PHP's `images/ui/` directory (5119 images). CSS Modules for component isolation.

**State management (Zustand):**
- `authStore` — `player`, `token`, `loading`, `login()`, `logout()`, `fetchPlayer()`
- `gameStore` — `pets`, `bag`, `chatMessages`, `currentMapId`, `inBattle`, `activePanel` (overlay), `gameView` (main), `battlePet`, `battleMonster`, `battleMapId`, `refreshTrigger`

**Panel system (26 panels):**
- Main views (gameView): `map`, `city`, `pets`, `shop`, `depot`, `zb`, `smshop`, `auction`, `ranch`
- Overlay panels (activePanel): `bag`, `equip`, `tasks`, `guild`, `rank`, `team`, `inherit`, `marry`, `gm`
- Battle (full-screen): `BattlePanel` — toolbar (8 buttons: auto/settings/attack/skill/assist/capture/item/flee), animated rounds, result screen, cooldown page

**Battle UI patterns:**
- React `key={petId-monsterId}` forces full remount per encounter (clean state)
- `cdFired` ref prevents sticky countdown=0 re-fires
- `phase` guards on doAction/doCapture/doFlee (disabled during animation)
- `handleBattleResponse` uses `{...prev, ...s}` merge to preserve `skills` field
- No `<StrictMode>` — double-mount effects trigger duplicate battle API calls

### Key Domain Concepts

**五行 (wx) elements:** 1=金, 2=木, 3=水, 4=火, 5=土, 6=神, 7=神圣
- Rock-paper-scissors: 金克木 木克土 土克水 水克火 火克金
- 神(6) neutral to all; 神圣(7) special (items require `__SS__` tag to use on them)

**czl (成长率/growth rate):**
- bb template stores a range string like `"5.0,10.0"`; on pet creation, random value picked from range
- Each level-up: `newStat = int(wx.coefficient × czl) + currentStat` — czl=10.0 gets 2x stats vs czl=5.0
- Level cap: 130

**muchang (牧场) states:** 0 = in party (carried), 1 = in ranch, 3-7 = inheritance-related states

**Equipment effects — 4-layer parsing order:**
1. Base effect (`props.effect`) + `plus_tms_eft` enhancement bonus
2. `pluseffect` — 18 effect keys (ac, mc, hp, mp, speed, hits, miss, addmoney, time, acrate, mcrate, hprate, mprate, speedrate, hitsrate, missrate, hitshp, hitsmp, dxsh, shjs, sdmp, szmp, crit)
3. Gem holes (`userbag.F_item_hole_info`) — 14 keys, ac/mc/hp/mp/speed/hits/miss all converted to rate%
4. Set bonuses (`props.series` + `serieseffect`) — scaling by piece count, dxsh capped at 70%

**EXP system:**
- Base EXP from monster → ×dblexpflag (1=1x, 2=1.5x, 3=2x, 4=2.5x, 5=3x) → ×auto-battle (gold 1.2x, yb 1.5x)
- `exptolv` table defines `nxtlvexp` per level
- Recursive level-up: if exp exceeds multiple level thresholds, level up repeatedly

**varyname (道具分类):** 1=辅助(药水), 2=增益(永久属性), 3=捕捉(精灵球), 5=技能书, 7=进化, 8=合体, 9=装备, 10=精炼, 12=礼包/宝箱, 13=特殊(功能), 15=宠物卵, 22=魔法石, 24=卡片, 25=宝石, 26=洗练石, 28=刮刮卡, 55-58=魔塔天赋

**Map types** — determined by TWO separate mechanisms (matches PHP):

1. **`multi_monsters` field** (`map` table → `GameMap.multiMonsters`): drives UI template selection in `Team_Mod.php`
   | multi_monsters | Type | Maps | Template |
   |---|---|---|---|
   | "1" | Challenge/挑战 | 125 | `tpl_cteam.html` |
   | "2" | Tower/通天塔 | 126 | `tpl_tt.html` |
   | "3" | Team dungeon | 128,129,130 | `tpl_team.html` |
   | "4" | Sacred/神圣 | 131-150 | `tpl_team.html` |
   | null/""/"0" | Normal (includes dungeons) | all others | `tpl_team.html` |

2. **`fuben` config** (`config.fuben.php` → Java `DungeonConfig`): independent list of 10 dungeon map IDs (11,12,13,14,50,124,127,143,144,151). NOT related to `multi_monsters` — many non-dungeon maps have `multi_monsters="0"` (e.g., 1,15,20,117-123).

**PHP behavior:**
- `Team_Mod.php`: `multi_monsters == 1` → challenge template, `== 2` → tower template, else → normal template (handles normal + dungeon maps identically)
- `Fight_Mod.php`: stores session flag from `multi_monsters` (1=challenge, 3=tower, 2=normal), used by `FightGate.php` for battle behavior
- `manymapgate.php`: checks `multi_monsters == 3` to validate team membership

**Current implementation:**
- `MapController.listMaps()` returns `multiMonsters` field ✓
- `MapPanel.tsx` uses `multiMonsters` for challenge/tower/team routing, `DUNGEON_MAP_IDS` Set for dungeon routing ✓
- `BattleService.initBattle()` only checks `== "4"` (sacred map wx==7 requirement) — challenge/tower battle logic not yet implemented

### Data Flow

1. Vite dev server (:3000) proxies `/api` → Spring Boot (:8080), `/ws` → ws://:8080
2. Auth: POST login → JWT token in authStore → Axios interceptor adds `Authorization: Bearer <token>`
3. Battle: POST init → BattleSession in memory → action/monster-turn exchange via REST (not WebSocket)
4. Admin (:8081): no frontend build, Thymeleaf server-side rendering, separate SecurityConfig

## Migration Status

| Phase | Content | Status |
|-------|---------|--------|
| Phase 0 | Environment & infra | 100% |
| Phase 1 | Data layer (63 entities, 117 tables) | ~80% |
| Phase 2 | Backend API (90+ endpoints, 18 services) | ~92% |
| Phase 3 | Frontend React (26 panels) | ~95% |
| Phase 4 | Integration testing & production | 0% — **not started** |

**Known gaps (not yet implemented):**
- 25/36 item effect categories still unimplemented (varyname=4彩票, 6/7/8卡片进化合体, 14军功, 16合成, 22魔法石, 24卡片, 28刮刮卡, 55-58魔塔天赋)
- Team dungeons (multi_monsters=3, maps 128-130) — marked `open:false`
- Sacred map wx==7 check (maps 131-150 currently treated as normal)
- Boss refresh cooldown system (`boss_refresh` table logic)
- Auction house bidding, marriage divorce cooldown, guild management features
- PHP's `custom/` admin tools not fully migrated (some DB-only tools exist in admin panel)

## Documentation Index

These docs are essential reading when working on specific systems:

| Doc | Covers |
|-----|--------|
| `TECH_REFERENCE.md` | **Most comprehensive** — architecture, API list, domain model, PHP→Java mapping |
| `DEVELOPMENT-SUMMARY.md` | Full system overview, all APIs, battle/capture/EXP/equipment formulas |
| `PROGRESS.md` | Migration progress, API checklist (71+ endpoints), sprint status |
| `ROADMAP.md` | 5-sprint plan, P0-P3 priority, completed checklist |
| `OPERATIONS.md` | Deployment, DB config, admin panel features, ops commands |
| `VERIFICATION.md` | Step-by-step startup, curl test commands, frontend verification checklist |
| `BATTLE-SYSTEM.md` | State machine, damage formula, animation flow, toolbar buttons, image system |
| `BATTLE-SCENE.md` | PHP battle scene analysis (original reference) |
| `EQUIPMENT-EFFECTS-SYSTEM.md` | 4-layer parsing, 18 effect keys, gem special rules, set bonuses |
| `EXP-LEVELING-SYSTEM.md` | saveGetOther() formula, exptolv table, double EXP stacking, recursive level-up |
| `PET-ATTRIBUTES-SYSTEM.md` | bb/userbb tables, czl generation, wx table, 3 pet creation paths |
| `SKILL-SYSTEM.md` | skillsys table, 6-step learn check, upgrade array, imgeft bonuses, cooldowns |
| `PET-SYSTEM.md` | Pet/skill/equipment overview |
| `ITEM-USAGE-SYSTEM.md` | varyname categories, effect keys, PHP usedProps.php vs Java BagService |
| `ITEM-ISSUES.md` | 36-item bug/feature checklist with PHP line references |
| `DUNGEON-TYPES-FIXPLAN.md` | 5 map types (multi_monsters), dungeon/challenge/tower implementation |
| `DATA-MAPPING.md` | PHP DB fields → Java Entity fields → Frontend TypeScript types |
| `MAP-TEAM-SYSTEM.md` | 3-column team layout, map data, PHP page hierarchy |
| `ADMIN-SYSTEM-PLAN.md` | PHP admin tools analysis, security issues, migration plan |

## Key PHP Reference Files

When implementing or debugging, these PHP files are the source of truth for game logic:

| PHP File | What It Does | Java Equivalent |
|----------|-------------|----------------|
| `sec/sec_common_fnc.php` | Core: `saveGetOther()` (level-up), `getProps()` (drops), `updateBoss()` | `LevelUpService`, `BattleService` |
| `function/usedProps.php` | All item usage logic (1980 lines, varyname dispatch) | `BagService.useItem()` |
| `FightGate.php` | Battle AJAX backend | `BattleService.performAction()` |
| `Fight_Mod.php` | Battle entry page, monster spawning | `BattleService.initBattle()` |
| `function/get.Catch.php` | Pet capture logic | `PetService` |
| `function/get.Skill.php` | Skill learning (6-step validation) | `SkillService` |
| `config/config.props.php` | Item definitions, varyname mapping | `Props.java` entity |
| `config/config.fuben.php` | 10 dungeon configurations | Dungeon system |
| `config/config.game.php` | Global game constants | `application.yml` |

## Important Notes

- **AdminController (kdjl-server) is dev-only** — must be removed/restricted before production. The kdjl-admin module (:8081) is the proper admin interface with authentication
- **No StrictMode in React** — it causes double-mount effects that trigger duplicate battle API calls
- **PHP codebase** (`kdjl/`) is reference material — all game formulas, config, and behavior originate here. When in doubt, read the PHP
- **Password hashing is MD5** (matches PHP original) — not bcrypt. This is intentional for migration compatibility
- **DB number parsing** — some fields have `\n\r` embedded; `parseLong`/`parseInt` must strip these
- **Frontend images** reference PHP's `images/ui/` structure (5119 images). Paths use `/images/bb/`, `/images/gpc/`, `/images/ui/`, `/images/map/`
- **Dungeon progress** saved in `fuben` table (uid, gwid=current wave, lttime/srctime=cooldown). Tower progress in `tgt` table + `player_ext.tgt`
