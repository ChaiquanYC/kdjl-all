# Pet Attributes System

> PHP 宠物属性与成长率系统完整分析 — 优先度 #4
> 来源: `D:\code\kdjl\kdjl\kdjl\`

## 1. 宠物模板表 (bb)

**文件:** `kdjl_mysql8_compatible.sql` lines 487-522

| 字段 | 说明 | Java |
|------|------|------|
| `id` | 物种 ID | `Long` |
| `name` | 宠物名 | `String` |
| `wx` | 五行 (1-7) | `Integer` |
| `ac, mc` | 攻击/防御 | `Long` |
| `hp, mp` | 生命/魔法 | `Long` |
| `speed` | 速度 | `Long` |
| `hits, miss` | 命中/闪避 | `Long` |
| `czl` | 成长率范围 `"1.0,1.3"` | `String` |
| `kx` | 抗性 `"j,m,s,h,t"` | `String` |
| `skillist` | 默认技能列表 | `String` |
| `remakelevel/id/pid` | 进化等级/目标/道具 | `Integer/Long/Long` |
| `subyl~subkl` | 7种抗性 | `Integer` |
| `imgstand/ack/die` | 动画图片 | `String` |
| `nowexp, lexp` | 初始经验 | `Long` |

## 2. 玩家宠物表 (userbb)

继承 bb 全部字段，增加：

| 字段 | 说明 | Java |
|------|------|------|
| `uid, username` | 所属玩家 | `Long, String` |
| `level` | 当前等级 (1-130) | `Integer` |
| `srchp, srcmp` | 基础 HP/MP (不含装备) | `Long` |
| `addhp, addmp` | 技能/装备永久加成 | `Long` |
| `hp, mp` | 当前 HP/MP | `Long` |
| `nowexp, lexp` | 当前经验/升级所需 | `Long` |
| `czl` | 实际成长率 (如 `1.2`) | `String` → `double` |
| `stime` | 创建时间戳 | `Long` |
| `zb` | 装备槽 `"pos:bagId,..."` | `String` |
| `muchang` | 牧场状态 (0=出战) | `Integer` |

## 3. 宠物创建

### 3.1 三种创建路径

| 路径 | 文件 | czl 来源 | lexp |
|------|------|----------|------|
| 注册送宠 | `login/register.php:130-148` | `getCzl(bb.czl)` | 55 |
| 捕捉 | `function/get.Catch.php:246-271` | `getCzl(bb.czl)` | 100 |
| 道具开蛋 | `usedProps.php:1619-1700` | `getCzl(bb.czl)` | 100 |
| 培育合成 | `cmpGate1.php:415-453` | 亲代公式 | 55 |

### 3.2 创建 INSERT 模板

所有路径使用相同的 INSERT，初始属性从 bb 表直接复制：
```sql
INSERT INTO userbb(name,uid,username,level,wx,ac,mc,srchp,hp,srcmp,mp,...)
VALUES('{bb.name}', '{uid}', '{nick}', 1, '{bb.wx}', 
       '{bb.ac}', '{bb.mc}', '{bb.hp}', '{bb.hp}', '{bb.mp}', '{bb.mp}', ...)
```

`srchp = bb.hp`, `srcmp = bb.mp`, `hp = bb.hp`, `mp = bb.mp`

## 4. 成长率 (czl) 系统

**核心文件:** `sec/sec_common_fnc.php` lines 173-181

### 4.1 getCzl() 函数

```php
function getCzl($czl) {
    $ok = str_replace(".", "", $czl);  // "1.0,1.3" → "10,13"
    $arr = split(",", $ok);            // [10, 13]
    $num = rand($arr[0], $arr[1]);    // rand(10, 13) → 10~13
    return $num/10;                    // 1.0~1.3
}
```

### 4.2 czl 范围示例

| 宠物类型 | bb.czl | 实际范围 |
|---------|--------|----------|
| 初始宠 | `"1.0,1.3"` | 1.0~1.3 |
| 进化形态 | `"1.5,1.8"` | 1.5~1.8 |
| 高级宠 | `"2.8,3.0"` | 2.8~3.0 |
| 培育上限 | — | 50.0 (硬编码) |

### 4.3 czl 如何影响属性

每升一级，**所有属性和抗性**都乘以 czl：

```
每级 HP 增量 = wx系数.hp × czl
每级 金抗增量 = wx系数.j × czl
```

czl=3.0 的宠物每级增长是 czl=1.0 的 **3 倍**。

## 5. wx (元素) 表

**定义:** `config/tconfig.php` lines 115-129

| 字段 | 说明 |
|------|------|
| `wx` | 元素 ID (1-7) |
| `j, m, s, h, t` | 金木水火土抗性成长系数 |
| `hp, mp` | HP/MP 成长系数 |
| `ac, mc` | 攻击/防御成长系数 |
| `speed` | 速度成长系数 |
| `hits, miss` | 命中/闪避成长系数 |

### wx 数据值

```sql
(1,  0,  5,  0, -5,  2,  1, 22, 6, 4, 2, 9, 4, 2),  -- 金
(2, -5,  0,  2,  0,  5,  2, 20, 5, 4, 2, 10,5, 3),  -- 木
(3,  2,  0,  0,  5, -5,  3, 22, 7, 4, 2, 9, 3, 2),  -- 水
(4,  5,  2, -5,  0,  0,  4, 20, 9, 5, 1, 8, 4, 2),  -- 火
(5,  0, -5,  5,  2,  0,  5, 23, 6, 3, 3, 9, 3, 2),  -- 土
(6,  7,  7,  7,  7,  7,  6, 33, 6, 7, 4, 15,7, 4),  -- 神
(7,  7,  7,  7,  7,  7,  7, 33, 6, 7, 4, 15,7, 4);  -- 神圣
```

内存键: `MEM_WX_KEY = 'db_wx'`

### 升级时 wx 查找

```php
$czz = mem->dataGet('db_wx', "if(rs.wx == pet.wx) return rs");
```

宠物 wx=1 (金) → 取行1: `hp=22, mp=6, ac=4, mc=2, speed=9, hits=4, miss=2`

## 6. 抗性系统

### 6.1 五行抗性 (kx)

存储格式: `"j,m,s,h,t"` (逗号分隔 5 个整数)

每级增长: `新抗性 = int(wx系数 × czl) + 当前抗性`

在 `userbb.kx` 字段中持久化。

### 6.2 状态抗性 (sub)

| 字段 | 中文 | 说明 |
|------|------|------|
| `subyl` | 减晕 | 眩晕抵抗 |
| `subsl` | 减睡 | 睡眠抵抗 |
| `subdl` | 减毒 | 中毒抵抗 |
| `subxl` | 减虚 | 虚弱抵抗 |
| `subfl` | 减防 | 破防抵抗 |
| `subhl` | 减缓 | 减速抵抗 |
| `subkl` | 减抗 | 降抗抵抗 |

**注意:** 这些值从 bb 模板复制后**永不改变**，战斗中**未实际使用**。

## 7. 品质评定

- **唯一标准:** czl (成长率)
- **排行榜:** `GrowthRanking_Mod.php` 按 `czl+0 DESC` 排序
- **无颜色/品质等级系统**
- **组队 czl 限制:** `leaderCzl ± diff` 范围内

## 8. 已知 Bug

1. **task.v1.php saveGetOther()**: 使用 `$kx[0]~[4]` 但 `$kx` 未定义 (sec 版本正确 split 了 kx)
2. **Ack 类 addhp**: `$one .=` (字符串拼接) 而非 `$one +=` (数值累加)
3. **sub 抗性:** 显示但战斗中未实际应用

## 9. 实现要点 (Java)

### 需要新建

1. **WxRepository** — wx 表查询
2. **PetCreateService** — getCzl() + INSERT 逻辑
3. **LevelUpService** — wx × czl 属性计算
4. **ExpToLvRepository** — 经验表查询

### czl 生成

```java
double generateCzl(String czlRange) {
    String[] parts = czlRange.replace(".", "").split(",");
    int min = Integer.parseInt(parts[0]);
    int max = Integer.parseInt(parts[1]);
    int rand = ThreadLocalRandom.current().nextInt(min, max + 1);
    return rand / 10.0;
}
```
