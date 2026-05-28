# 地图前端数据驱动重构计划

## 目标

新增地图时，只需在数据库 + 后端配置中操作，前端零改动自动适配。

## 当前问题

| 硬编码 | 文件:行 | 影响 |
|--------|---------|------|
| `DUNGEON_MAP_IDS = new Set([11,12,13,14,50,124,127,143,144,151])` | MapPanel.tsx:179 | 新增副本需改前端 |
| `id >= 100 \|\| id === 16` (判断队伍/难度) | MapPanel.tsx:376,440,458 | 新增高级地图需改前端 |
| `monsterNames` 显示 `gpclist` 但无结构化数据 | MapPanel.tsx:357 | 怪物名无法做交互 |
| `mapPage` 3 页硬编码翻页 | MapPanel.tsx:289 | 地图多了3页可能不够 |

## 计划

### 阶段 1：后端 MapController 返回完整元数据

**文件**: `MapController.java`

`listMaps()` 返回新增字段：

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `isDungeon` | boolean | 查 DungeonConfig | 是否副本 |
| `features` | string[] | 根据 map 属性计算 | 启用的功能模块 |
| `page` | int | 分组逻辑 | 世界地图分页 |

**features 枚举值**:

| 值 | 触发条件 | 对应 UI |
|----|---------|---------|
| `team` | `id >= 100 \|\| id == 16 \|\| multiMonsters == "3"` | 显示组队面板 |
| `difficulty` | `id >= 100 \|\| id == 16` | 显示难度选择 |
| `czlGate` | `czlprops` 不为空 | 显示成长限制并校验 |
| `unlockGate` | `needs` 不为空 | 显示解锁条件 |

**isDungeon 判定**:
```java
boolean isDungeon = DungeonConfig.DUNGEONS.stream()
    .anyMatch(d -> d.id() == m.getId());
```

**features 判定**:
```java
List<String> features = new ArrayList<>();
if (m.getId() >= 100 || m.getId() == 16 || "3".equals(m.getMultiMonsters())) {
    features.add("team");
    features.add("difficulty");
}
if (m.getCzlprops() != null && !m.getCzlprops().isEmpty()) {
    features.add("czlGate");
}
if (m.getNeeds() != null && !m.getNeeds().isEmpty() && !"0".equals(m.getNeeds())) {
    features.add("unlockGate");
}
```

### 阶段 2：前端 MapPanel 纯数据驱动

**文件**: `MapPanel.tsx`

#### 2.1 移除硬编码

- 删除 `DUNGEON_MAP_IDS` 常量
- 删除 `id >= 100 || id === 16` 判断
- 全部改为读 `map.isDungeon` 和 `map.features`

#### 2.2 路由逻辑简化

```tsx
const mm = map.multiMonsters;
// 副本 → DungeonPanel
if (map.isDungeon) { setDungeonMapId(mapId); return; }
// 挑战/通天塔 → 对应面板
if (mm === '1') { setChallengeMapId(mapId); return; }
if (mm === '2') { setTowerMapId(mapId); return; }
// 组队副本
if (mm === '3') { /* team dungeon flow */ return; }
// 其余 → 通用 tpl_team 模板
```

#### 2.3 功能模块按 features 渲染

```tsx
const features = selectedMap?.features ?? [];

// 组队面板
{features.includes('team') ? <TeamPanel ... /> : <SimpleTeamPlaceholder />}

// 难度选择
{features.includes('difficulty') && <DifficultyBar ... />}

// 成长限制
{features.includes('czlGate') && <CzlGate czl={selectedMap.czlprops} />}
```

#### 2.4 MapInfo 接口最终形态

```tsx
interface MapInfo {
  id: number;
  name: string;
  desc: string;
  level: string;
  unlocked: boolean;
  img?: string;
  gpclist?: string;
  needs?: string;
  multiMonsters?: string;
  czlprops?: string;
  isDungeon: boolean;
  features: string[];
  page: number;  // 世界地图分页
}
```

### 阶段 3：MAP_POINTS 也数据化

将 `MAP_POINTS` 的世界地图坐标也搬到后端（map 表已有基础信息）：

| map 表字段 | 说明 |
|-----------|------|
| `id` | 地图 ID |
| `name` | 地图名 |
| 新增 `page` | 世界地图页号（1/2/3） |
| 新增 `pos_x`, `pos_y`, `pos_w`, `pos_h` | 在地图图片上的位置 |

前端只保留背景图片路径，坐标完全从 API 数据读取。这样新增地图完全不需要改前端代码。

### 阶段 4：清理

- 删除 `MapPoint` 接口和 `MAP_POINTS` 常量
- MapPanel 中世界地图渲染改为遍历 `maps` 数组
- 后台新增地图 = 数据库 INSERT 一条记录 + 配置 DungeonConfig（如果是副本）

## 影响范围

| 阶段 | 后端改动 | 前端改动 | 风险 |
|------|---------|---------|------|
| 1 | MapController +20 行 | 无 | 低 |
| 2 | 无 | MapPanel ~30 行重构 | 中 |
| 3 | map 表 +3 列, MapController | MapPanel ~20 行, 删 MAP_POINTS | 中 |
| 4 | 无 | 删代码 | 低 |

## 建议顺序

阶段 1 → 阶段 2 一次完成（改动量小，收益高）。阶段 3 涉及数据库改表，可以单独迭代。
