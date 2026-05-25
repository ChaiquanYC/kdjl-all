# 道具使用系统文档

> 基于 PHP `usedProps.php` + `BagService.java` 整理

## 道具使用入口

**PHP**: `function/usedProps.php` — 所有道具双击使用统一入口  
**Java**: `BagService.useItem()` — 先判断类型再分流处理

## 道具分类 (varyname → 类型 + 图标)

| varyname | 类型名 | 图标路径 | 使用方式 |
|----------|--------|----------|----------|
| 0 | 普通 | `images/ui/bag/0.gif` | — |
| 1 | 辅助道具 | `images/ui/bag/1.gif` | 消耗品，解析 effect 字段 |
| 2 | 增益道具 | `images/ui/bag/2.gif` | 消耗品，解析 effect 字段 |
| 3 | 捕捉道具 | `images/ui/bag/3.gif` | 战斗中捕捉用 |
| 4 | 收集道具 | `images/ui/bag/4.gif` | — |
| 5 | 技能书 | `images/ui/bag/5.gif` | 宠物学习技能 |
| 6 | 卡片道具 | `images/ui/bag/6.gif` | — |
| 7 | 进化道具 | `images/ui/bag/7.gif` | 宠物进化 |
| 8 | 合体道具 | `images/ui/bag/8.gif` | 宠物合体 |
| 9 | **装备道具** | `images/ui/bag/9.gif` | **装备到宠物槽位** |
| 10 | 精练道具 | `images/ui/bag/10.gif` | 装备强化 |
| 11 | 宝箱道具 | `images/ui/bag/11.gif` | 随机开奖 |
| 12 | 特殊道具 | `images/ui/bag/12.gif` | — |
| 13 | 功能道具 | `images/ui/bag/13.gif` | — |
| 14 | 宠物卵 | `images/ui/bag/14.gif` | 孵化宠物 |
| 15 | 合成道具 | `images/ui/bag/15.gif` | 宠物合成 |
| 20 | 传承 | `images/ui/bag/20.gif` | 宠物传承 |
| 22 | 魔法石 | `images/ui/bag/22.gif` | 占卜屋使用 |
| 25-27 | 宝石类 | `images/ui/bag/25.gif` | 镶嵌合成 |

## effect 字段解析

格式：`key:value,key:value` 或 `key:min,max`（范围）

### 通用消耗品效果

| effect key | 作用 | 示例 | 效果 |
|------------|------|------|------|
| `hp` | 恢复生命 | `hp:200` | 宠物 HP +200 |
| `mp` | 恢复魔法 | `mp:100` | 宠物 MP +100 |
| `exp` | 获得经验 | `exp:500` | 宠物经验 +500，可能升级 |
| `openmap` | 解锁地图 | `openmap:5` | 解锁地图 ID=5 |
| `addbag` | 背包扩容 | `addbag:10` | maxbag+10（需≥150，上限200）|
| `addbag1` | 背包大扩容 | `addbag1:10` | maxbag+10（需≥200，上限300）|
| `addck` | 仓库扩容 | `addck:10` | maxbase+10（需≥150，上限200）|
| `addck1` | 仓库大扩容 | `addck1:10` | maxbase+10（需≥200，上限300）|
| `addsj` | 随机水晶 | `addsj:1,5` | 随机获得 1~5 水晶 |
| `addyb` | 随机元宝 | `addyb:10,50` | 随机获得 10~50 元宝 |
| `zhanshi` | 展示卷 | `zhanshi:10` | 宠物展示次数 +10 |

### 范围值处理

`addsj:1,5` → 用逗号分隔的数值表示 min,max 范围，使用时随机取值。

## 装备系统 (varyname==9)

### 装备使用流程

```
1. 用户双击装备道具
2. 检查是否有主战宠物 → 无则提示"请先设置主战宠物"
3. 解析 requires 字段检查条件：
   - lv:N → 宠物等级 ≥ N
   - wx:N → 宠物五行 = N（1金2木3水4火5土）
   → 不满足提示具体原因
4. 获取装备槽位 (postion: 0-10)
5. 更新宠物 zb 字段：
   - 格式 "pos:bagId,pos:bagId"
   - 同槽位已有装备 → 替换
   - 无装备 → 追加
6. 清除旧装备的 zbing=0 标记
7. 设置新装备 zbing=1, zbpets=petId
8. 应用装备属性加成到宠物
9. 提示"恭喜您，装备成功！"
```

### 装备槽位

| postion | 槽位名 | 显示位置 |
|---------|--------|----------|
| 0 | 武器 | pet_1_bg.jpg 上左 |
| 1 | 衣服 | 上中 |
| 2 | 头盔 | 上右 |
| 3 | 鞋子 | 中左 |
| 4 | 项链 | 中右 |
| 5 | 戒指左 | 下左 |
| 6 | 戒指右 | 下右 |
| 7 | 护腕 | 左下 |
| 8 | 腰带 | 右下 |
| 9 | 特殊 | 下中 |
| 10 | 翅膀 | 中下 |
| 11 | 翅膀(展) | — |

### 装备图片

- 道具自身图：`images/props/{props.img}`（如 `149.gif`）
- 槽位占位图：`images/props/zbsx.gif`（武器背景）

### requires 字段

格式：`lv:N,wx:N`  
- `lv:20` → 需要 20 级
- `wx:1` → 需要金系宠物
- `lv:10,wx:3` → 需要 10 级 + 水系

## 特殊道具 ID

| propId 范围 | 道具名 | 效果 |
|-------------|--------|------|
| 200 | 仓库升级卷轴 | maxbase+6（上限96）|
| 201 | 背包升级卷轴 | maxbag+6（上限96）|
| 202 | 牧场升级卷轴 | maxmc+6（上限40）|
| 1344 | 高级牧场升级卷轴 | maxmc+1（需≥40，上限80）|

## 道具图片体系

| 用途 | 路径 | 说明 |
|------|------|------|
| 分类图标 | `images/ui/bag/{varyname}.gif` | 16 类各一图 |
| 道具图片 | `images/props/{props.img}` | 843 张，含装备图 |
| 装备占位 | `images/props/zbsx.gif` | 武器背景 |
| 宠物图 | `images/bb/{imgstand}.gif` | 宠物站立图 |
| 宠物卡牌 | `images/bb/{cardimg}.gif` | 宠物卡片图 |
| 头像 | `images/head/{n}.gif` | 玩家头像 |

## 前端对应组件

| 功能 | 组件 | 弹窗 |
|------|------|------|
| 背包列表 | `BagPanel.tsx` | OverlayPanel (pack.gif) |
| 道具详情 | BagPanel tooltip | 深色浮窗 |
| 仓库 | `DepotPanel.tsx` | gameBox 全屏 |
| 道具商店 | `ShopPanel.tsx` | gameBox 全屏 |
| 铁匠铺 | `ZbPanel.tsx` | gameBox 全屏 |
| 装备穿戴 | PetList Tab1 | gameBox 全屏 |
