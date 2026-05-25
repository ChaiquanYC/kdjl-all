# 地图 & 组队 & 战斗系统文档

> 基于 PHP Expore_Mod / Team_Mod / Fight_Mod 分析

## 页面层级

```
世界地图 (MapPanel)
  └→ 点击地图标签 → handleEnterMap(mapId)
       └→ 怪物区域 (teamContainer 三栏布局)
            ├── 左栏 (.teamLeft) — 地图介绍 + 宠物卡片
            ├── 中栏 (.teamCenter) — 队伍列表 + 操作按钮 (插槽)
            └── 右栏 (.teamRight) — 在线玩家 (插槽)
                 └→ 点击挑战 → BattlePanel (战斗)
```

## 三栏布局数据映射

### 左栏 (.teamLeft — 301px, zdzd_bj1.jpg)

| PHP 变量 | 数据来源 | React 字段 | 状态 |
|----------|----------|------------|------|
| `#mapname#` | `map.name` | `selectedMap.name` | ✅ |
| `#mapinfo#` | `map.descs` | `selectedMap.desc` | ✅ (已从数据库读取) |
| `#level#` | `map.level` | `selectedMap.level` | ✅ |
| `#czl#` | 宠物成长率 | — | ⏳ |
| `#gw#` | 出现怪物列表 | — | ⏳ |
| `#head1#` | `user.headimg` | — | ⏳ |
| `#one#` `#two#` `#three#` | 宠物卡片 | `pets[0-2].cardImg` | ✅ |

### 中栏 (.teamCenter — 283px, zdzd_bj2.jpg)

| PHP 内容 | 说明 | React 状态 |
|----------|------|------------|
| `.zhong_list` / `#team#` | 队伍列表 iframe (260x210) | 插槽 (teamList) |
| `#table#` | 难度选择 (ann07/08/09.gif) | ⏳ |
| `.anniu` / `#team1#` | 底部按钮区 | ✅ |

**底部按钮 (PHP)**:

| 状态 | 按钮1 | 按钮2 |
|------|-------|-------|
| 未组队 | `zd.gif` 组队 | `cjdw.gif` 创建队伍 |
| 已组队(成员) | `zlgd.png` 暂离 | `lk.gif` 离开 |
| 已组队(队长) | `zd.gif` 战斗 | `jsdw.png` 解散 |

**React 当前**: `zd.gif`(组队) + `lk.gif`(离开)

### 右栏 (.teamRight — 204px, zdzd_bj3.jpg)

| PHP 变量 | 说明 | React 状态 |
|----------|------|------------|
| `#otherlist#` | 在线玩家列表 | 插槽 (teamOnline) |
| 右键菜单 | 邀请组队/加好友/挑战/侦察/私聊 | ⏳ |

## 地图数据 (map 表)

| 字段 | 说明 | API 返回 |
|------|------|----------|
| `id` | 地图ID | ✅ |
| `name` | 地图名 | ✅ |
| `descs` | 地图描述 (富文本) | ✅ (从DB读取) |
| `level` | 等级范围 "min,max" | ✅ |
| `gpclist` | 怪物ID列表 | ✅ |
| `czlprops` | 成长率限制 | ⏳ |

## 地图相关道具

| propId | 道具名 | 解锁地图 | effect 字段 |
|--------|--------|----------|-------------|
| 85 | 妖精钥匙 | 妖精森林(id=2) | openmap:2 |
| 86 | 潮汐钥匙 | 潮汐海涯(id=3) | openmap:3 |
| 87 | 巨石钥匙 | 巨石山脉(id=4) | openmap:4 |
| 88 | 黄金钥匙 | 黄金陵(id=5) | openmap:5 |
| 89 | 炽热钥匙 | 炽热沙滩(id=6) | openmap:6 |
| 90 | 火山钥匙 | 尤玛火山(id=7) | openmap:7 |

## 文件对应

| PHP | React | Backend |
|-----|-------|---------|
| `Expore_Mod.php` | `MapPanel.tsx` (worldMap) | `MapController.java` |
| `tpl_mapnew.html` | MapPanel (worldBg) | — |
| `Team_Mod.php` | `MapPanel.tsx` (teamContainer) | — |
| `tpl_team.html` | MapPanel (teamLeft/Center/Right) | — |
| `team.php` | 插槽, 待实现 | — |
| `Fight_Mod.php` | `BattlePanel.tsx` | `BattleController.java` |

## 组件插槽接口

```tsx
// MapPanel 内部插槽 — 待对接组队系统
interface TeamSlot {
  teamList: ReactNode;    // 中栏队伍列表 (替换 teamListEmpty)
  teamButtons: ReactNode; // 中栏按钮 (替换当前 anniuBtn)
  onlineList: ReactNode;  // 右栏在线玩家 (替换 teamOnline)
}
```

当前为独立 state 管理，未暴露 props。后续组队功能实现时改为可注入插槽。
