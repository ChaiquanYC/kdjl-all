# PHP 战斗场景完整分析

> 基于 `Fight_Mod.php` + `tpl_fight.html` + `fight.js`

## 容器结构

```html
<div id="fm" style="width:778px; height:311px;
  background-image:url(../images/map/t{inmap}/{inmap}.{bgtype})">
</div>
```

战斗场景 778×311，背景图按地图 ID 动态加载。

## 数据变量

| PHP 变量 | 来源 | 说明 |
|----------|------|------|
| `#bbinfo#` | UserPet | 宠物数据数组 bb[] |
| `#gwinfo#` | Monster(gpc) | 怪物数据数组 gg[] |
| `#bbjn#` | Skill | 宠物技能 |
| `#bbfzp#` | UserBag | 战斗可用道具 |
| `#catcharr#` | config | 捕捉概率 |
| `#petsid#` | session | 宠物ID |
| `#nickname#` | session | 玩家昵称 |
| `#head0#` | UserPet.headimg | 宠物头像 (如 t1.gif) |
| `#inmap#` | session | 当前地图ID |
| `#fttime#` | config | 战斗时限 |

### bb[] 数组索引 (宠物)

| idx | 字段 | 示例 |
|-----|------|------|
| 0 | name | 金波姆 |
| 1 | level | 99 |
| 2 | wx (元素) | 1 |
| 3 | ac (攻击) | 28 |
| 4 | mc (防御) | 15 |
| 5 | hp (当前) | 160 |
| 6 | mp (当前) | 100 |
| 7 | speed | 102 |
| 8 | imgstand | z1.gif |
| 9 | hits | 118 |
| 10 | miss | 77 |
| 11 | czl (成长) | 1.1 |
| 12 | maxHp | 160 |
| 13 | maxMp | 100 |
| 14 | nowexp | 0 |
| 15 | lexp | 55 |

### gg[] 数组索引 (怪物)

| idx | 字段 |
|-----|------|
| 0 | name |
| 1 | level |
| 2 | wx (not used) |
| 3 | ac |
| 4 | mc |
| 5 | maxHp |
| 6 | maxMp |
| 7 | speed |
| 8 | imgstand |
| 11 | monster ID |

## 宠物侧 (左侧) — createLeft()

全部由 JS 动态创建，模板只有 #team0 空容器。

| 元素 | 类型 | 位置 | 图片 | 内容 |
|------|------|------|------|------|
| 宠物名栏 | DIV | left:8px, top:2px, 147x19 | zd01.gif | #nickname# |
| 头像框 | DIV | left:5px, top:21px, 90x47 | zd02.gif | 内含 36x36 头像 |
| 头像图 | IMG | left:23px, top:1px, 36x36 | bb/{head0} | pet.headimg |
| 血条背景 | IMG | left:70px, top:23px, 160x48 | zd04.gif | 半透明 |
| HP 条 | IMG | left:70px, top:24px, 宽可变 | xthong01.gif(红) | PHP 用 table 实现 |
| HP 尾 | IMG | 紧接HP条后, 4x16 | xthong00.gif | 圆角收尾 |
| HP 数值 | DIV | left:106px, top:27px | — | bb[5]/bb[12] |
| MP 条 | IMG | left:70px, top:41px, 宽可变 | xtlan01.gif(蓝) | |
| MP 尾 | IMG | 紧接后, 5x16 | xtlan00.gif | |
| MP 数值 | DIV | left:106px, top:42px | — | bb[6]/bb[13] |
| EXP 条 | IMG | left:70px, top:59px, 宽可变 | xthuang01.gif(黄) | |
| EXP 尾 | IMG | 紧接后, 4x11 | xthuang00.gif | |
| EXP 数值 | DIV | left:106px, top:59px | — | bb[14]/bb[15] |
| 宠物名+级 | DIV | left:30px, top:90px | — | bb[0] + bb[1] |
| 宠物大图 | IMG | left:10px, top:120px, 自然大小 | bb/{bb[8]} | pet.imgstand(z1.gif) |

## 怪物侧 (右侧) — createRight()

| 元素 | 类型 | 位置 | 图片 | 内容 |
|------|------|------|------|------|
| 等级徽章 | DIV | left:620px, top:49px, 31x37 | dr02.gif | gg[2] |
| 头部背景 | DIV | left:620px+13, top:49px, 114x28 | dr01.gif | opacity:0.7 |
| 怪物名 | SPAN | left:620px+19, top:51px | — | gg[0] |
| 等级文本 | SPAN | left:620px+13, top:64px | — | LV：gg[1] |
| HP 条容器 | DIV | left:620px, top:65px, 110x11 | — | |
| HP 条背景 | DIV | 内含, 96x11 | dr03.gif | |
| HP 条填充 | DIV | 内含, 91x11 可变 | dr04.gif | |
| HP 数值 | DIV | left:620px+13, top:65px | — | gg[5]/gg[5] |
| 怪物大图 | IMG | left:530px, top:120px, 250x180 | gpc/{gg[8]} | monster.imgstand |

## 底部工具栏

工具栏背景 `zdzsk.gif` (778×71)，位置 left:0, bottom:0。

| 按钮 | ID | 位置(left,top) | 功能 |
|------|-----|---------------|------|
| 自动 | tauto | 218,7 | loadtool(1) — 自动战斗 |
| 设置 | tset | 259,7 | loadtool(2) — 战斗设置 |
| 攻击 | tack | 299,10 | loadtool(3) — 普通攻击 |
| 技能 | tskill | 340,10 | loadtool(4) — 技能面板 |
| 求助 | thelp | 384,10 | loadtool(5) — 求助 |
| 捕捉 | tcatch | 428,10 | loadtool(6) — 捕捉 |
| 道具 | tprops | 472,10 | loadtool(7) — 道具面板 |
| 逃跑 | texit | 513,10 | loadtool(8) — 逃跑 |

## 回合计时器

`db.gif` (81×52)，位置 left:361px, top:10px。显示倒计时秒数。

## 战斗结果面板

`#result` DIV，位置 left:270, top:87, 246x160，默认 hidden。战斗结束时显示：
- 胜利/失败
- 获得经验
- 获得金币
- 掉落道具
- 宠物升级

## 图片清单

| 图片 | 尺寸 | 用途 | 侧 |
|------|------|------|-----|
| zd01.gif | 147x19 | 宠物名栏 | 左 |
| zd02.gif | 90x47 | 头像框 | 左 |
| zd03.gif | 99x48 | 宠物大图框 | 左 |
| zd04.gif | 160x48 | 血条背景 | 左 |
| xthong01.gif | 1x16 | HP条填充(红) | 左 |
| xthong00.gif | 4x16 | HP条尾端 | 左 |
| xtlan01.gif | 1x16 | MP条填充(蓝) | 左 |
| xtlan00.gif | 5x16 | MP条尾端 | 左 |
| xthuang01.gif | 1x11 | EXP条填充(黄) | 左 |
| xthuang00.gif | 4x11 | EXP条尾端 | 左 |
| dr01.gif | 114x28 | 怪物头部背景 | 右 |
| dr02.gif | 31x37 | 怪物等级徽章 | 右 |
| dr03.gif | 96x11 | 怪物血条背景 | 右 |
| dr04.gif | 1x11 | 怪物血条填充 | 右 |
| zdzsk.gif | 778x71 | 工具栏 | 底 |
| db.gif | 81x52 | 回合计时器 | 中上 |
| bb/{headimg} | 36x36 | 宠物头像 | 左 |
| bb/{imgstand} | 250x180 | 宠物大图 | 左 |
| gpc/{imgstand} | 250x180 | 怪物大图 | 右 |

## 战斗流程

1. `Fight_Mod.php` 初始化：加载地图、检查组队、取宠物/怪物数据
2. 模板渲染：输出 #bbinfo# #gwinfo# 等 JS 变量
3. `fight.js` 执行：`createLeft()` + `createRight()` 动态构建 UI
4. 玩家点击工具栏按钮 → `loadtool(n)` → 发送 Ajax
5. 服务端 `FightGate.php` 处理回合逻辑
6. 返回数据 → JS 更新血条/日志/状态
7. 循环直到一方死亡 or 逃跑/捕捉成功
