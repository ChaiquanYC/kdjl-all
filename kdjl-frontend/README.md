# KDJL Frontend

口袋精灵2 游戏前端 — React 18 + TypeScript + Vite

> PHP jQuery 页面 → React SPA 迁移

---

## 快速启动

```bash
npm install
npm run dev        # http://localhost:3001
```

## 技术栈

- React 18 + TypeScript
- Vite 5
- Zustand (状态管理)
- Axios (API请求)
- CSS Modules (组件样式)
- WebSocket STOMP (聊天)

## 面板列表 (25个)

| 面板 | 说明 |
|------|------|
| BattlePanel | 即时回合战斗 |
| MapPanel | 3页世界地图(35个地点) |
| BagPanel | 背包/道具使用 |
| PetList | 宠物资料(装备/属性/技能) |
| DungeonPanel | 副本信息页(PHP对齐) |
| TowerPanel | 通天塔(55层) |
| ChallengePanel | 挑战模式(星级难度) |
| ZhanBuPanel | 占卜屋/魔法屋 |
| AuctionPanel | 拍卖行(金币/水晶/元宝) |
| ShopPanel | 商城 |
| DepotPanel | 仓库 |
| RanchPanel | 牧场 |
| CityPanel | 城镇中心 |
| GuildPanel | 公会 |
| TeamPanel | 组队 |
| FriendPanel | 好友 |
| PvpPanel | PvP战场 |
| InheritPanel | 宠物传承 |
| MarryPanel | 婚姻 |
| TaskPanel | 任务 |
| RankPanel | 排行榜 |
| GmPanel | GM工具 |
| ChatPanel | 聊天 |
| EquipPanel | 装备栏 |
| SmShopPanel | 神秘商店 |

## 布局体系

- 布局: 1000px宽, side.jpg侧栏 + content.jpg主区域
- 游戏区域: 788x319px, zdzd_bj/team系列背景
- 匹配PHP原版 `images/ui/` 图片资源(5119张)

## 后端

后端Spring Boot代码：[kdjl-backend](https://github.com/ChaiquanYC/kdjl-backend)
