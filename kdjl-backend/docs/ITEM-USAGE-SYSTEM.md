# KDJL 道具使用系统 — PHP分析 & Java差异对比

> 2026-05-24 | PHP: `function/usedProps.php` (1980行) | Java: `BagService.useItem()` | 综合agent报告

---

## 一、道具分类完整映射 (varyname)

| varyname | 名称 | 使用对象 | PHP处理 | Java状态 |
|----------|------|---------|---------|---------|
| 0 | 普通 | — | 无使用逻辑 | — |
| 1 | 辅助 | 宠物 | HP/MP恢复 | ✅ hp/mp |
| 2 | **增益** | 主战宠物 | 属性药水(addexp/addczl/addac/addhp等) | ❌ 大部分缺失 |
| 3 | 捕捉 | — | 战斗捕捉球 | ✅ 战斗内 |
| 4 | 收集 | 玩家 | 彩票抽奖券 | ❌ 未实现 |
| 5 | 技能书 | 宠物 | 学习技能 | ✅ 战斗内 |
| 6 | 卡片 | 宠物 | 旧版卡片 | ❌ 未实现 |
| 7 | 进化 | 宠物 | 进化道具 | ❌ |
| 8 | 合体 | 宠物 | 合体道具 | ❌ |
| 9 | 装备 | 主战宠物 | 10槽穿戴 | ✅ equipItem |
| 10 | 精练 | 装备 | 装备强化 | ❌ |
| 11 | 宝箱 | 玩家 | 随机物品 | ⚠️ 部分 |
| 12 | **宝箱** | 玩家 | giveitems/randitem/needkey | ⚠️ 基础完成 |
| 13 | **特殊** | 玩家 | exp/auto/autofree/addsj/addyb/addbag/tuoguan/openmap/zhanshi | ⚠️ 大部分完成 |
| 14 | 功能 | 玩家 | 军功令(jg:xxx) | ❌ |
| 15 | **宠物卵** | 玩家 | openpet从bb模板创建 | ❌ 核心缺失 |
| 16 | 合成 | 玩家 | hecheng/chongzhu/random_combine | ❌ |
| 17 | 水晶 | 玩家 | 水晶道具 | ❌ |
| 18 | 特殊回复 | 宠物 | HP/MP全恢复 | ❌ |
| 19 | 涅槃 | 宠物 | 转生奖励 | ❌ |
| 20 | 传承 | 宠物 | 传承材料 | ❌ |
| 22 | 魔法石 | 玩家 | 占卜屋(giveitems/randitem) | ❌ |
| 23 | 神圣转生 | 宠物 | 神圣转生道具 | ❌ |
| 24 | 卡片 | 玩家 | 称号/成就卡片 | ❌ |
| 25 | 宝石 | 装备 | 装备镶嵌 | ❌ |
| 26 | 洗练石 | 装备 | 重铸属性 | ❌ |
| 27 | 合成保底 | 合成 | 合成保护 | ❌ |
| 28 | 刮刮卡 | 玩家 | 抽奖卡 | ❌ |
| 29 | 奇石 | — | 特殊石头 | ❌ |
| 30-32 | 扫雷 | 玩家 | 小游戏道具 | ❌ |
| 50-54 | 魔塔 | 宠物 | 魔塔药水/复活/BUFF | ❌ |
| 55 | 天赋洗点 | 玩家 | xidian:count | ❌ |
| 56 | 天赋洗点 | 玩家 | 同上 | ❌ |
| 57 | 宠物栏 | 玩家 | xiedaibb20/21/30/31 | ❌ |
| 58 | 天赋经验 | 主战宠物 | tianfuexp:min,max | ❌ |

---

## 二、Effect 效果键完整清单

### 2.1 增益类 (varyname=2) — 作用于主战宠物

| Key | 格式 | 说明 | Java |
|----|------|------|------|
| `addexp` | 整数 或 min,max | 直接加经验 | ✅ |
| `addczl` | 整数 | 永久增加成长率 | ❌ |
| `addac` | 整数 | 永久加攻击 | ❌ |
| `addmc` | 整数 | 永久加防御 | ❌ |
| `addhp` | 整数 | 永久加HP上限 | ❌ |
| `addmp` | 整数 | 永久加MP上限 | ❌ |
| `addspeed` | 整数 | 永久加速度 | ❌ |
| `addhits` | 整数 | 永久加命中 | ❌ |
| `addmiss` | 整数 | 永久加闪避 | ❌ |
| `weiwang` | 整数 | 增加威望 | ❌ |
| `add_cq_czl` | 整数 | 增加抽取成长点数 | ❌ |
| `add_zc_jifen` | 整数 | 战场积分倍数 | ❌ |

### 2.2 特殊类 (varyname=13) — 作用于玩家

| Key | 格式 | 说明 | Java |
|----|------|------|------|
| `exp` | 浮点数(1.5/2/2.5/3) | 双倍经验倍率 | ✅ |
| `autofree` | 整数 | 金币自动战斗次数 | ✅ |
| `auto` | 整数 | 元宝自动战斗次数 | ✅ |
| `autoteam` | 整数 | 组队自动次数 | ✅ |
| `addsj` | min,max | 随机水晶 | ✅ |
| `addyb` | min,max | 随机元宝 | ✅ |
| `addbag` | 整数 | 背包扩展(第二级) | ✅ |
| `addbag1` | 整数 | 背包扩展(第三级) | ✅ |
| `addck` | 整数 | 仓库扩展 | ✅ |
| `addck1` | 整数 | 仓库扩展(第三级) | ✅ |
| `zhanshi` | 整数 | 宠物展示次数 | ✅ |
| `tuoguan` | 整数(小时) | 托管时间 | ❌ |
| `openmap` | 地图ID | 解锁地图 | ✅ |
| `addmc` | 整数 | 牧场扩展 | ❌ |

### 2.3 宠物卵 (varyname=15)

| Key | 格式 | 说明 | Java |
|----|------|------|------|
| `openpet` | bb模板ID | 创建新宠物 | ❌ 核心缺失 |

### 2.4 宝箱 (varyname=11/12/22)

| Key | 格式 | 说明 | Java |
|----|------|------|------|
| `giveitems` | propId:count,... | 固定掉落 | ✅ |
| `randitem` | propId:count:prob:flag\|... | 随机选1 | ✅ |
| `needkey` | 钥匙propId | 需要钥匙 | ❌ |

### 2.5 装备 (varyname=9)

18种属性键: ac/mc/hp/mp/speed/hits/miss/hprate/mprate/acrate/mcrate/hitsrate/missrate/speedrate/dxsh/shjs/shft/hitshp/hitsmp/sdmp/szmp/addmoney/crit — Java EquipEffectService 已全部实现 ✅

### 2.6 未实现类别汇总

| 类别 | PHP行数 | 说明 |
|------|---------|------|
| varyname=2 增益 | ~170行 | 永久属性药水(11种effect) |
| varyname=4 彩票 | ~50行 | ticket表插入 |
| varyname=14 军功 | ~20行 | battlefield_user.jgvalue |
| varyname=15 宠物卵 | ~170行 | openpet创建宠物+技能 |
| varyname=16 合成 | ~260行 | hecheng/chongzhu/random_combine |
| varyname=24 卡片 | ~150行 | F_User_Card_Info称号 |
| varyname=28 抽奖 | ~80行 | 外部API抽奖 |
| varyname=55 洗点 | ~25行 | xidian天赋重置 |
| varyname=57 宠物栏 | ~90行 | xiedaibb扩展 |
| varyname=58 天赋经验 | ~60行 | tianfuexp分配 |

---

## 三、使用对象判断

### PHP 逻辑
| varyname | 目标 | 说明 |
|----------|------|------|
| 2 | 主战宠物 `$user['mbid']` | 不接受外部选择 |
| 9 | 主战宠物 | 装备绑定主战宠 |
| 12/13/14/15/16/22/24/28/55/57 | 玩家自身 | 效果作用于玩家表 |
| 58 | 主战宠物 | 天赋经验平分 |

### Java frontend 当前 needsPet (BagPanel.tsx:118)
```typescript
needsPet = (item) => {
  if (varyname==1 || varyname==2 || varyname==15) return true;  // 过宽
  if (effect.includes('hp')||effect.includes('mp')||
      effect.includes('exp')||effect.includes('openpet')) return true;
  return false;
}
```

### 问题
1. **varyname==2** 中的 `exp/addyb/addsj` 等效果不需要宠物，但 needsPet 返回 true
2. **varyname==15** 的 openpet 创建新宠物，不需要选目标，但弹宠物选择器
3. **varyname==13** 中的 `exp`(双倍经验) 等不需要宠物
4. 正确做法：PHP 是按 varyname 分支，不是按 effect 内容判断

---

## 四、修复优先级

### P0 — 核心缺失
| 项目 | 工作量 | 
|------|--------|
| varyname=15 openpet 宠物卵 | 中 |
| needsPet 逻辑修正 (改为 varyname 白名单) | 小 |

### P1 — 常用功能
| 项目 | 工作量 |
|------|--------|
| varyname=2 永久属性药水 (addac/addmc/addhp等) | 小 |
| tuoguan 托管时间 | 小 |
| addmc 牧场扩展 | 小 |
| needkey 宝箱钥匙 | 小 |

### P2 — 完整系统
| 项目 | 工作量 |
|------|--------|
| varyname=16 合成系统 | 大 |
| varyname=24 卡片系统 | 中 |
| varyname=55/57/58 天赋 | 中 |
