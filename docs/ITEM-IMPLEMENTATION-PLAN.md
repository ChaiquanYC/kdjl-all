# 道具系统实现计划

> 基于 `ITEM-USAGE-SYSTEM.md` 分析 | 更新时间: 2026-06-04（代码审查后更新）

---

## 一、当前状态总览（2026-06-04 代码审查后）

| 状态 | 数量 | 说明 |
|:----:|:----:|------|
| ✅ 已完成 | 28 | 1(药水), 2(增益-全部), 3(捕捉), 4(收集/彩票), 5(技能书), 7(进化), 8(合体), 9(装备), 10(精炼), 11(精炼辅助), 12(礼包-全部), 13(特殊), 14(军功), 15(宠物卵), 16(合成), 17(水晶), 18(特殊回复), 21(自动回复), 22(魔法石), 24(卡片), 25/26/27(宝石), 28(刮刮卡), 55(洗点), 57(出战卷), 58(天赋) |
| ❌ 未实现 | 2 | 19(涅槃), 20(传承), 23(神圣转生) |
| — 系统独立 | 3 | 30-32(扫雷道具) |
| — 无实现/废弃 | 6 | 6(废弃), 29(仅任务), 50-54/56(魔塔无道具) |

**完成度**: 28/35 (80%) 完全可用，2/35 (6%) 需要实现

**已修复**: CATEGORIES map 命名与 PHP config.props.php 对齐（BagService.java:1485-1491）

---

## 二、优先级分层（待实现功能）

### P0 - 核心功能（影响主要玩法）
- ✅ varyname=7 进化类（宠物成长）— 已实现
- ✅ varyname=8 合体类（宠物融合）— 已实现

### P1 - 重要功能（影响玩家体验）
- ✅ varyname=10/11 精炼类（装备强化）— 已实现
- ✅ varyname=25/26/27 宝石系统（装备镶嵌）— 已实现
- ✅ varyname=24 卡片类（成就系统）— 已实现

### P2 - 次要功能（可延后）
- ✅ varyname=17 水晶类（魅力系统）— 已实现
- ✅ varyname=18 特殊回复类 — 已实现
- varyname=19/20/23 宠物系统扩展
- ✅ varyname=28 刮刮卡（外部API）— 已实现

### P3 - 独立系统（可单独开发）
- ✅ varyname=21 自动回复（被动道具）— 已实现
- varyname=30-32 扫雷道具

---

## 三、详细实现计划

### 阶段1: 补全核心功能（2周）

> ✅ varyname=2(增益类)、12(礼包类)、15(宠物卵) 已在代码审查中确认实现
> ✅ varyname=14(军功令)、16(合成类)、55/57/58(魔塔道具) 已实现

#### 1.1 varyname=7 进化类 - 实现宠物进化
**当前状态**: 未实现
**工作量**: 3天
**依赖**: PetService, BbRepository

**实现逻辑**:
```java
// 进化流程 (jhGate.php)
public void evolvePet(Integer playerId, Integer petId, Integer materialPropId) {
    // 1. 获取宠物
    UserPet pet = userPetRepository.findById(petId)
        .orElseThrow(() -> new RuntimeException("宠物不存在"));

    // 2. 检查进化条件
    BbTemplate template = bbRepository.findById(pet.getBbId())
        .orElseThrow(() -> new RuntimeException("宠物模板不存在"));

    // 检查是否有进化目标
    if (template.getRemakeId() == null || template.getRemakeId() == 0) {
        throw new RuntimeException("该宠物无法进化");
    }

    // 检查等级
    if (pet.getLevel() < template.getRemakeLevel()) {
        throw new RuntimeException("等级不足，需要" + template.getRemakeLevel() + "级");
    }

    // 检查五行（神圣宠物不能普通进化）
    if (pet.getWx() > 6) {
        throw new RuntimeException("神圣宠物无法普通进化");
    }

    // 检查进化次数
    if (pet.getRemakeTimes() >= 10) {
        throw new RuntimeException("已达最大进化次数");
    }

    // 检查是否抽取过成长
    PlayerExt ext = playerExtRepository.findById(playerId).orElse(null);
    if (ext != null && ext.getChouquChongwu() != null
        && ext.getChouquChongwu().contains(String.valueOf(petId))) {
        throw new RuntimeException("抽取过成长的宠物无法进化");
    }

    // 3. 检查材料
    String[] materialIds = template.getRemakePid().split("\\|");
    boolean hasMaterial = false;
    for (String mid : materialIds) {
        BagItem material = bagRepository.findByPlayerIdAndPropId(playerId, Integer.parseInt(mid));
        if (material != null && material.getCount() > 0) {
            hasMaterial = true;
            // 扣除材料
            material.setCount(material.getCount() - 1);
            bagRepository.save(material);
            break;
        }
    }
    if (!hasMaterial) {
        throw new RuntimeException("缺少进化材料");
    }

    // 4. 扣除金币
    Player player = playerRepository.findById(playerId)
        .orElseThrow(() -> new RuntimeException("玩家不存在"));
    if (player.getMoney() < 1000) {
        throw new RuntimeException("金币不足1000");
    }
    player.setMoney(player.getMoney() - 1000);
    playerRepository.save(player);

    // 5. 计算新成长率
    double newCzl = calculateEvolutionCzl(pet, template);

    // 6. 更新宠物
    BbTemplate newTemplate = bbRepository.findById(template.getRemakeId())
        .orElseThrow(() -> new RuntimeException("进化目标不存在"));
    pet.setBbId(newTemplate.getId());
    pet.setName(newTemplate.getName());
    pet.setCzl(newCzl);
    pet.setRemakeTimes(pet.getRemakeTimes() + 1);
    userPetRepository.save(pet);
}

private double calculateEvolutionCzl(UserPet pet, BbTemplate template) {
    double oldCzl = pet.getCzl();
    double bonus;
    if (template.getStyle() == 1) {
        bonus = 0.1 + Math.random() * 0.4; // rand(0.1, 0.5)
    } else {
        bonus = 0.5 + Math.random() * 0.5; // rand(0.5, 1.0)
    }
    bonus += (pet.getLevel() - template.getRemakeLevel()) / 200.0;

    double newCzl = oldCzl + bonus;
    // czl >= 50 时增长递减
    if (newCzl >= 50) {
        newCzl = oldCzl + bonus * 0.5;
    }
    // czl >= 150 时封顶
    if (newCzl >= 150) {
        newCzl = 150;
    }
    return newCzl;
}
```

**需要新增**:
- `BbTemplate.getRemakeId()`, `getRemakePid()`, `getRemakeLevel()`, `getStyle()`
- `UserPet.getRemakeTimes()`, `setRemakeTimes()`
- `PlayerExt.getChouquChongwu()`

---

#### 1.5 varyname=8 合体类 - 实现宠物合体
**当前状态**: 未实现
**工作量**: 3天
**依赖**: PetService, BbRepository

**实现逻辑**:
```java
// 合体公式: hecheng:A:10%,B:4%|addczl:8%|1
public void mergePets(Integer playerId, Integer pet1Id, Integer pet2Id, Integer itemId) {
    // 1. 获取两只宠物
    UserPet pet1 = userPetRepository.findById(pet1Id).orElseThrow();
    UserPet pet2 = userPetRepository.findById(pet2Id).orElseThrow();

    // 2. 获取合体道具的effect
    Props item = propsRepository.findById(itemId).orElseThrow();
    String effect = item.getEffect();

    // 3. 解析合体公式
    MergeFormula formula = parseMergeFormula(effect);

    // 4. 检查宠物是否匹配公式要求
    if (!formula.getRequiredPets().contains(pet1.getBbId())
        || !formula.getRequiredPets().contains(pet2.getBbId())) {
        throw new RuntimeException("宠物不匹配合体公式");
    }

    // 5. 计算成功率
    double successRate = formula.getBaseRate();
    // 检查是否有额外材料提升成功率
    // ...

    // 6. 概率判定
    if (Math.random() * 100 > successRate) {
        // 合体失败
        // 检查是否有保护道具
        if (!hasProtectionItem(playerId)) {
            // 宠物消失
            userPetRepository.delete(pet2);
        }
        throw new RuntimeException("合体失败");
    }

    // 7. 合体成功 - 更新宠物属性
    pet1.setCzl(pet1.getCzl() + formula.getCzlBonus());
    pet1.setRemakeTimes(pet1.getRemakeTimes() + 1);
    userPetRepository.save(pet1);

    // 8. 消耗宠物2
    userPetRepository.delete(pet2);

    // 9. 扣除合体道具
    bagRepository.decreaseCount(playerId, itemId, 1);
}
```

**需要新增**:
- `MergeFormula` DTO - 解析合体公式
- `parseMergeFormula()` - 解析effect字段
- 合体界面 API

---

### 阶段2: 装备系统完善（1.5周）

> ✅ varyname=16(合成类) 已在代码审查中确认实现（BagService.java:1093-1387 handleCraft方法）

#### 2.1 varyname=10/11 精炼类 - 装备强化
**当前状态**: 未实现
**工作量**: 3天
**依赖**: EquipmentService

**实现逻辑**:
```java
// 装备强化
public void strengthenEquipment(Integer playerId, Integer equipId,
                                 List<Integer> materialIds, List<Integer> auxiliaryIds) {
    // 1. 获取装备
    BagItem equip = bagRepository.findById(equipId).orElseThrow();
    if (equip.getVaryname() != 9) {
        throw new RuntimeException("只能强化装备");
    }

    // 2. 检查强化材料（varyname=10）
    int materialCount = 0;
    for (Integer mid : materialIds) {
        BagItem material = bagRepository.findById(mid).orElseThrow();
        if (material.getVaryname() != 10) {
            throw new RuntimeException("无效的强化材料");
        }
        materialCount += material.getCount();
    }

    // 3. 检查辅助道具（varyname=11）
    double bonusRate = 0;
    for (Integer aid : auxiliaryIds) {
        BagItem auxiliary = bagRepository.findById(aid).orElseThrow();
        if (auxiliary.getVaryname() != 11) {
            throw new RuntimeException("无效的辅助道具");
        }
        // 解析辅助效果
        bonusRate += parseAuxiliaryEffect(auxiliary.getUsages());
    }

    // 4. 计算强化成功率
    int currentLevel = parseStrengthenLevel(equip);
    double baseRate = getStrengthenRate(currentLevel);
    double finalRate = Math.min(baseRate + bonusRate, 100);

    // 5. 概率判定
    if (Math.random() * 100 > finalRate) {
        // 强化失败
        throw new RuntimeException("强化失败");
    }

    // 6. 强化成功 - 更新装备
    updateStrengthenLevel(equip, currentLevel + 1);
    bagRepository.save(equip);

    // 7. 扣除材料
    for (Integer mid : materialIds) {
        bagRepository.decreaseCount(playerId, mid, 1);
    }
    for (Integer aid : auxiliaryIds) {
        bagRepository.decreaseCount(playerId, aid, 1);
    }
}
```

**需要新增**:
- `EquipmentService.strengthenEquipment()`
- 强化等级解析/更新逻辑
- 强化成功率表
- 强化界面 API

---

#### 2.2 varyname=25/26/27 宝石系统
**当前状态**: 未实现
**工作量**: 3天
**依赖**: EquipmentService

**功能点**:
1. **宝石合成** (varyname=25 + varyname=27)
   - 两颗同级宝石合成高级宝石
   - 使用保底石(varyname=27)防止失败损失

2. **宝石镶嵌** (varyname=25 → varyname=9)
   - 将宝石镶嵌到装备的宝石孔
   - 检查装备是否有空宝石孔

3. **宝石洗练** (varyname=26)
   - 清除装备上已镶嵌的宝石效果

**实现步骤**:
```java
// 宝石合成
public void mergeGems(Integer playerId, Integer gem1Id, Integer gem2Id, Integer protectId) {
    BagItem gem1 = bagRepository.findById(gem1Id).orElseThrow();
    BagItem gem2 = bagRepository.findById(gem2Id).orElseThrow();

    // 检查都是宝石
    if (gem1.getVaryname() != 25 || gem2.getVaryname() != 25) {
        throw new RuntimeException("只能合成宝石");
    }

    // 检查同级
    Props gem1Props = propsRepository.findById(gem1.getPropId()).orElseThrow();
    Props gem2Props = propsRepository.findById(gem2.getPropId()).orElseThrow();
    if (!gem1Props.getName().equals(gem2Props.getName())) {
        throw new RuntimeException("只能合成同种宝石");
    }

    // 计算合成结果
    Props resultGem = getNextLevelGem(gem1Props);
    if (resultGem == null) {
        throw new RuntimeException("已达最高级");
    }

    // 概率判定
    double successRate = getGemMergeRate(gem1Props);
    boolean success = Math.random() * 100 <= successRate;

    if (success) {
        // 合成成功
        bagRepository.addItem(playerId, resultGem.getId(), 1);
    } else {
        // 合成失败
        if (protectId != null) {
            // 使用保底石
            BagItem protect = bagRepository.findById(protectId).orElseThrow();
            if (protect.getVaryname() == 27) {
                bagRepository.decreaseCount(playerId, protectId, 1);
                // 保留宝石
            }
        } else {
            // 损失宝石
        }
    }

    // 扣除材料
    bagRepository.decreaseCount(playerId, gem1Id, 1);
    bagRepository.decreaseCount(playerId, gem2Id, 1);
}

// 宝石镶嵌
public void embedGem(Integer playerId, Integer equipId, Integer gemId, int slotIndex) {
    BagItem equip = bagRepository.findById(equipId).orElseThrow();
    BagItem gem = bagRepository.findById(gemId).orElseThrow();

    // 检查装备
    if (equip.getVaryname() != 9) {
        throw new RuntimeException("只能镶嵌到装备上");
    }

    // 检查宝石
    if (gem.getVaryname() != 25) {
        throw new RuntimeException("只能镶嵌宝石");
    }

    // 检查宝石孔
    String holeInfo = equip.getItemHoleInfo();
    if (!hasEmptySlot(holeInfo, slotIndex)) {
        throw new RuntimeException("该位置已有宝石");
    }

    // 镶嵌
    equip.setItemHoleInfo(updateHoleInfo(holeInfo, slotIndex, gem.getPropId()));
    bagRepository.save(equip);

    // 扣除宝石
    bagRepository.decreaseCount(playerId, gemId, 1);
}

// 宝石洗练
public void washGems(Integer playerId, Integer equipId) {
    BagItem equip = bagRepository.findById(equipId).orElseThrow();

    // 检查装备
    if (equip.getVaryname() != 9) {
        throw new RuntimeException("只能洗练装备");
    }

    // 清除宝石孔
    equip.setItemHoleInfo(null);
    bagRepository.save(equip);
}
```

**需要新增**:
- `EquipmentService.mergeGems()`, `embedGems()`, `washGems()`
- `BagItem.getItemHoleInfo()`, `setItemHoleInfo()`
- 宝石合成界面 API
- 宝石镶嵌界面 API

---

### 阶段3: 社交与收集系统（1周）

#### 3.1 varyname=24 卡片类 - 称号系统
**当前状态**: 未实现
**工作量**: 2天
**依赖**: PlayerExtRepository

**实现逻辑**:
```java
// 使用卡片
public void useCard(Integer playerId, Integer itemId) {
    // 1. 获取卡片信息
    Props card = propsRepository.findById(itemId).orElseThrow();

    // 2. 获取玩家卡片信息
    PlayerExt ext = playerExtRepository.findById(playerId)
        .orElseThrow(() -> new RuntimeException("玩家数据不存在"));

    String cardInfo = ext.getUserCardInfo();
    Map<String, Integer> cards = parseCardInfo(cardInfo);

    // 3. 添加卡片
    String cardName = card.getName();
    cards.put(cardName, cards.getOrDefault(cardName, 0) + 1);

    // 4. 保存卡片信息
    ext.setUserCardInfo(serializeCardInfo(cards));
    playerExtRepository.save(ext);

    // 5. 扣除道具
    bagRepository.decreaseCount(playerId, itemId, 1);

    // 6. 检查是否集齐称号
    checkTitleCompletion(playerId, ext, cards);
}

private void checkTitleCompletion(Integer playerId, PlayerExt ext, Map<String, Integer> cards) {
    // 查询所有称号要求
    List<TitleRequirement> requirements = titleRequirementRepository.findAll();

    for (TitleRequirement req : requirements) {
        Map<String, Integer> required = parseCardInfo(req.getRequiredCards());
        boolean complete = true;

        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (cards.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                complete = false;
                break;
            }
        }

        if (complete) {
            // 检查是否已有该称号
            String titles = ext.getHasTitle();
            if (titles == null || !titles.contains(req.getTitleName())) {
                // 添加称号
                ext.setHasTitle(titles == null ? req.getTitleName()
                    : titles + "," + req.getTitleName());
                playerExtRepository.save(ext);

                // 发送系统公告
                broadcastTitleAchievement(playerId, req.getTitleName());
            }
        }
    }
}
```

**需要新增**:
- `TitleRequirement` 实体 + Repository
- `PlayerExt.getUserCardInfo()`, `setUserCardInfo()`, `getHasTitle()`, `setHasTitle()`
- 卡片查询 API
- 称号列表 API

---

#### 3.2 varyname=17 水晶类 - 魅力系统
**当前状态**: 未实现
**工作量**: 1.5天
**依赖**: PlayerExtRepository

**实现逻辑**:
```java
// 赠送水晶
public void giveCrystal(Integer playerId, Integer targetId, Integer itemId) {
    // 1. 检查目标玩家存在
    Player target = playerRepository.findById(targetId)
        .orElseThrow(() -> new RuntimeException("目标玩家不存在"));

    // 2. 检查水晶道具
    BagItem crystal = bagRepository.findByPlayerIdAndPropId(playerId, itemId);
    if (crystal == null || crystal.getCount() < 1) {
        throw new RuntimeException("水晶不足");
    }

    // 3. 获取水晶魅力值
    Props crystalProps = propsRepository.findById(itemId).orElseThrow();
    int charmValue = parseCharmValue(crystalProps.getEffect());

    // 4. 增加目标魅力值
    PlayerExt targetExt = playerExtRepository.findById(targetId)
        .orElseThrow(() -> new RuntimeException("目标数据不存在"));
    targetExt.setMl(targetExt.getMl() + charmValue);
    playerExtRepository.save(targetExt);

    // 5. 扣除水晶
    crystal.setCount(crystal.getCount() - 1);
    bagRepository.save(crystal);

    // 6. 发送通知
    sendCharmNotification(playerId, targetId, charmValue);
}
```

**需要新增**:
- `PlayerExt.getMl()`, `setMl()` (魅力值字段)
- 魅力赠送界面 API
- 魅力排行榜 API

---

> ✅ varyname=14(军功令) 已在代码审查中确认实现（BagService.java:998-1011）

---

### 阶段4: 魔塔与独立系统（1周）

> ✅ varyname=55(洗点)、57(出战卷)、58(天赋经验) 已在代码审查中确认实现（BagService.java:896-997）

---

#### 4.1 varyname=21 自动回复 - 被动道具
**当前状态**: 未实现
**工作量**: 0.5天
**依赖**: BattleService

**实现逻辑**:
```java
// 战斗结束后检查
public void onBattleEnd(Integer playerId, Integer petId) {
    // 检查是否有自动回复道具
    List<BagItem> autoHealItems = bagRepository.findByPlayerIdAndVaryname(playerId, 21);

    if (!autoHealItems.isEmpty()) {
        // 自动回复HP/MP
        UserPet pet = userPetRepository.findById(petId).orElseThrow();
        pet.setHp(pet.getSrchp() + pet.getAddhp());
        pet.setMp(pet.getSrcmp() + pet.getAddmp());
        userPetRepository.save(pet);
    }
}
```

---

#### 4.2 varyname=30-32 扫雷道具
**当前状态**: 未实现（独立小游戏）
**工作量**: 5天
**依赖**: 无

**实现步骤**:
1. 创建 `SaoleiController` - 扫雷游戏 API
2. 创建 `SaoleiService` - 游戏逻辑
3. 创建扫雷界面前端组件
4. 实现道具使用:
   - varyname=30: 扫雷闯关卡 - 进入游戏
   - varyname=31: 扫雷复活卡 - 死亡复活
   - varyname=32: 扫雷刷新券 - 刷新奖励

**API 设计**:
```java
@RestController
@RequestMapping("/api/saolei")
public class SaoleiController {

    @PostMapping("/enter")
    public ApiResponse<SaoleiGame> enterGame(@RequestParam Integer itemId) {
        // 使用闯关卡进入游戏
    }

    @PostMapping("/click")
    public ApiResponse<SaoleiResult> clickCell(@RequestParam Integer cellIndex) {
        // 点击格子
    }

    @PostMapping("/revive")
    public ApiResponse<Void> revive(@RequestParam Integer itemId) {
        // 使用复活卡
    }

    @PostMapping("/refresh")
    public ApiResponse<SaoleiGame> refreshPrizes(@RequestParam Integer itemId) {
        // 使用刷新券
    }
}
```

---

## 四、技术债务与优化

### 4.1 三端命名统一 ✅ 已完成
**问题**: varyname=11,12,14,15,16 命名不一致
**完成时间**: 2026-06-04 代码审查时

**已修正**:
- BagService.java:1485-1491 CATEGORIES map 已与 PHP config.props.php 权威定义对齐
- 修正内容: 11→精炼辅助类, 12→礼包类, 13→特殊类, 14→功能类, 15→宠物卵, 新增16→合成类

---

### 4.2 Effect 解析器重构
**问题**: 当前 effect 解析逻辑分散
**工作量**: 1天

**重构方案**:
```java
// 新增 EffectParser 工具类
public class EffectParser {

    public static Map<String, String> parse(String effect) {
        Map<String, String> result = new LinkedHashMap<>();
        if (effect == null || effect.isEmpty()) return result;

        // 处理多键组合: key1:value1,key2:value2
        String[] parts = effect.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }

    public static String getValue(String effect, String key) {
        return parse(effect).get(key);
    }

    public static int getIntValue(String effect, String key, int defaultValue) {
        String value = getValue(effect, key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
```

---

### 4.3 统一错误处理
**问题**: 各种 RuntimeException 混乱
**工作量**: 0.5天

**重构方案**:
```java
// 新增业务异常类
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    // 常用异常工厂方法
    public static BusinessException itemNotFound() {
        return new BusinessException("道具不存在");
    }

    public static BusinessException itemNotEnough() {
        return new BusinessException("道具数量不足");
    }

    public static BusinessException petNotFound() {
        return new RuntimeException("宠物不存在");
    }

    public static BusinessException levelNotEnough(int required) {
        return new BusinessException("等级不足，需要" + required + "级");
    }
}
```

---

## 五、测试计划

### 5.1 单元测试
每个 varyname 实现后都需要编写单元测试:

```java
@SpringBootTest
class BagServiceTest {

    @Autowired
    private BagService bagService;

    @Test
    void testUseItem_AddExp() {
        // 测试 addexp 效果
    }

    @Test
    void testUseItem_AddCzl() {
        // 测试 addczl 效果
        // 测试每宠物只能使用一次的限制
    }

    @Test
    void testUseItem_NeedKey() {
        // 测试 needkey 效果
        // 测试有钥匙/无钥匙的情况
    }

    @Test
    void testUseItem_OpenPet() {
        // 测试 openpet 效果
        // 测试宠物数量限制
    }

    // ... 更多测试用例
}
```

### 5.2 集成测试
```java
@SpringBootTest
@AutoConfigureMockMvc
class ItemIntegrationTest {

    @Test
    void testFullEvolutionFlow() {
        // 1. 创建宠物
        // 2. 升级到足够等级
        // 3. 获取进化材料
        // 4. 执行进化
        // 5. 验证属性变化
    }
}
```

---

## 六、时间安排

| 阶段 | 内容 | 工作量 | 开始时间 | 结束时间 |
|:----:|------|:------:|:--------:|:--------:|
| 阶段1 | 核心功能（增益/礼包/宠物卵/进化/合体） | 2周 | Week 1 | Week 2 |
| 阶段2 | 装备系统（精炼/宝石） | 1.5周 | Week 3 | Week 4 |
| 阶段3 | 社交与收集（卡片/水晶/军功） | 1周 | Week 5 | Week 5 |
| 阶段4 | 独立系统（魔塔/扫雷） | 1周 | Week 6 | Week 6 |
| 优化 | 命名统一/代码重构 | 1周 | Week 7 | Week 7 |

**总工作量**: 6.5周（约320小时）

---

## 七、验收标准

### 7.1 功能验收
- [ ] 所有 varyname 道具可正常使用
- [ ] 前端提示信息与 PHP 一致
- [ ] 错误处理完善，不会导致数据异常
- [ ] 数据库操作原子性保证

### 7.2 性能验收
- [ ] 单次道具使用响应时间 < 200ms
- [ ] 批量操作（如10连抽）响应时间 < 2s
- [ ] 无 N+1 查询问题

### 7.3 代码质量
- [ ] 单元测试覆盖率 > 80%
- [ ] 无 Sonar 严重问题
- [ ] 代码审查通过

---

## 八、风险与依赖

### 8.1 外部依赖
- 外部API（刮刮卡）可能不可用
- 数据库表结构可能需要调整

### 8.2 技术风险
- 进化/合体系统复杂度高，需要仔细测试
- 宝石系统涉及多表关联，性能需关注

### 8.3 缓解措施
- 先实现核心功能，次要功能延后
- 每个阶段进行充分测试
- 保持与 PHP 代码的对比验证

---

## 九、后续扩展

### 9.1 魔塔副本系统（varyname 50-54）
- 需要完整的魔塔副本系统设计
- 独立的战斗和奖励机制

### 9.2 扫雷小游戏（varyname 30-32）
- 独立的小游戏系统
- 前端需要专门的UI设计

### 9.3 高级功能
- 装备套装效果
- 宠物传承系统
- 结婚系统道具
