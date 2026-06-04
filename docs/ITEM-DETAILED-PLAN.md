# 道具系统详细实现计划

> 更新时间: 2026-06-04 | 实现思路、影响分析、关联内容

---

## 一、varyname=2 增益类（永久属性药水）

### 1.1 实现思路

**核心逻辑**:
```
使用道具 → 解析effect字段 → 分发到对应处理函数 → 更新宠物/玩家属性 → 扣除道具
```

**详细流程**:
```java
// BagService.java - useItem() 方法中

case 2: // 增益类
    // 1. 获取主战宠物
    UserPet mainPet = getMainPet(playerId);
    if (mainPet == null) {
        throw new BusinessException("没有主战宠物");
    }

    // 2. 解析effect字段
    Map<String, String> effects = EffectParser.parse(item.getEffect());

    // 3. 数据库行锁（防并发）
    lockManager.lock(playerId);

    // 4. 遍历每个effect执行
    for (Map.Entry<String, String> entry : effects.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        switch (key) {
            case "addexp":
                handleAddExp(mainPet, value);  // 调用LevelUpService
                break;
            case "addczl":
                handleAddCzl(playerId, mainPet, value);  // 特殊限制
                break;
            case "addac":
                handleAddAc(mainPet, value);
                break;
            case "addmc":
                handleAddMc(mainPet, value);
                break;
            case "addhp":
                handleAddHp(mainPet, value);
                break;
            case "addmp":
                handleAddMp(mainPet, value);
                break;
            case "addspeed":
                handleAddSpeed(mainPet, value);
                break;
            case "addhits":
                handleAddHits(mainPet, value);
                break;
            case "addmiss":
                handleAddMiss(mainPet, value);
                break;
            case "weiwang":
                handleWeiwang(playerId, value);  // 作用于玩家
                break;
            case "add_cq_czl":
                handleAddCqCzl(playerId, value);
                break;
            case "add_zc_jifen":
                handleAddZcJifen(playerId, value);
                break;
        }
    }

    // 5. 扣除道具
    bagRepository.decreaseCount(playerId, itemId, 1);
    break;
```

**每个effect的具体实现**:

```java
// addczl - 永久增加成长率（每宠物只能用1次）
private void handleAddCzl(Integer playerId, UserPet pet, String value) {
    // 检查是否已使用过
    PlayerExt ext = playerExtRepository.findById(playerId).orElseThrow();
    String usedPets = ext.getChouquChongwu();
    if (usedPets != null && usedPets.contains(String.valueOf(pet.getId()))) {
        throw new BusinessException("该宠物已使用过成长率药水");
    }

    // 检查神圣宠物上限
    if (pet.getWx() == 7) {
        // 查询 super_jh 表的 max_czl
        SuperJh superJh = superJhRepository.findByPlayerId(playerId);
        if (superJh != null && pet.getCzl() + Double.parseDouble(value) > superJh.getMaxCzl()) {
            throw new BusinessException("超过神圣宠物成长率上限");
        }
    }

    // 更新成长率
    pet.setCzl(pet.getCzl() + Double.parseDouble(value));
    userPetRepository.save(pet);

    // 记录已使用
    ext.setChouquChongwu(usedPets == null ? String.valueOf(pet.getId())
        : usedPets + "," + pet.getId());
    playerExtRepository.save(ext);
}

// addac - 永久加攻击
private void handleAddAc(UserPet pet, String value) {
    pet.setAc(pet.getAc() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addmc - 永久加防御
private void handleAddMc(UserPet pet, String value) {
    pet.setMc(pet.getMc() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addhp - 永久加HP上限
private void handleAddHp(UserPet pet, String value) {
    pet.setSrchp(pet.getSrchp() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addmp - 永久加MP上限
private void handleAddMp(UserPet pet, String value) {
    pet.setSrcmp(pet.getSrcmp() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addspeed - 永久加速度
private void handleAddSpeed(UserPet pet, String value) {
    pet.setSpeed(pet.getSpeed() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addhits - 永久加命中
private void handleAddHits(UserPet pet, String value) {
    pet.setHits(pet.getHits() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// addmiss - 永久加闪避
private void handleAddMiss(UserPet pet, String value) {
    pet.setMiss(pet.getMiss() + Integer.parseInt(value));
    userPetRepository.save(pet);
}

// weiwang - 增加威望（作用于玩家）
private void handleWeiwang(Integer playerId, String value) {
    Player player = playerRepository.findById(playerId).orElseThrow();
    player.setPrestige(player.getPrestige() + Integer.parseInt(value));
    playerRepository.save(player);
}

// add_cq_czl - 增加抽取成长点数
private void handleAddCqCzl(Integer playerId, String value) {
    PlayerExt ext = playerExtRepository.findById(playerId).orElseThrow();
    ext.setCzlSs(ext.getCzlSs() + Math.abs(Integer.parseInt(value)));
    playerExtRepository.save(ext);
}

// add_zc_jifen - 战场积分倍数
private void handleAddZcJifen(Integer playerId, String value) {
    PlayerExt ext = playerExtRepository.findById(playerId).orElseThrow();
    String buffStatus = ext.getBuffStatus();
    String newBuff = "add_zc_jifen:" + System.currentTimeMillis() / 1000 + "," + value + ";";
    ext.setBuffStatus(buffStatus == null ? newBuff : buffStatus + newBuff);
    playerExtRepository.save(ext);
}
```

### 1.2 影响分析

**直接影响**:
- 宠物属性变化（ac, mc, hp, mp, speed, hits, miss, czl）
- 玩家属性变化（prestige, czl_ss, buff_status）
- 背包道具数量减少

**间接影响**:
- 战斗伤害计算（ac, mc影响攻击力和防御力）
- 战斗速度（speed影响出手顺序）
- 闪避率（miss影响闪避概率）
- 命中率（hits影响命中概率）

**UI刷新**:
- BagPanel - 道具数量更新
- PetPanel - 宠物属性更新
- BattlePanel - 战斗属性变化
- PlayerInfoPanel - 玩家威望更新

### 1.3 关联内容

**依赖的模块**:
- `LevelUpService` - addexp效果需要调用经验计算
- `EquipEffectService` - 属性变化后需要重新计算装备效果
- `BattleService` - 属性变化影响战斗计算

**依赖的数据库表**:
- `userbb` - 宠物属性
- `player` - 玩家属性
- `player_ext` - 玩家扩展属性
- `super_jh` - 神圣宠物上限
- `userbag` - 背包道具

**依赖的实体类**:
- `UserPet` - 宠物实体
- `Player` - 玩家实体
- `PlayerExt` - 玩家扩展实体
- `SuperJh` - 神圣进化实体

**需要新增**:
- `SuperJhRepository` - 查询神圣宠物上限
- `EffectParser` - 解析effect字段（可复用）

---

## 二、varyname=7 进化类

### 2.1 实现思路

**核心逻辑**:
```
选择宠物 → 检查进化条件 → 检查材料 → 计算新成长率 → 更新宠物形态 → 扣除材料和金币
```

**详细流程**:
```java
// PetService.java - evolve() 方法

public UserPet evolvePet(Integer playerId, Integer petId, Integer materialPropId) {
    // ========== 第一步：获取宠物和模板 ==========
    UserPet pet = userPetRepository.findById(petId)
        .orElseThrow(() -> new BusinessException("宠物不存在"));

    // 检查宠物归属
    if (!pet.getUid().equals(playerId.longValue())) {
        throw new BusinessException("这不是你的宠物");
    }

    BbTemplate currentTemplate = bbRepository.findById(pet.getBbId())
        .orElseThrow(() -> new BusinessException("宠物模板不存在"));

    // ========== 第二步：检查进化条件 ==========

    // 2.1 检查是否有进化目标
    if (currentTemplate.getRemakeId() == null || currentTemplate.getRemakeId() == 0) {
        throw new BusinessException("该宠物无法进化");
    }

    // 2.2 检查等级要求
    if (pet.getLevel() < currentTemplate.getRemakeLevel()) {
        throw new BusinessException("等级不足，需要" + currentTemplate.getRemakeLevel() + "级");
    }

    // 2.3 检查五行限制（神圣宠物不能普通进化）
    if (pet.getWx() > 6) {
        throw new BusinessException("神圣宠物无法普通进化，需要使用神圣进化");
    }

    // 2.4 检查进化次数上限
    if (pet.getRemakeTimes() != null && pet.getRemakeTimes() >= 10) {
        throw new BusinessException("已达最大进化次数(10次)");
    }

    // 2.5 检查是否抽取过成长
    PlayerExt ext = playerExtRepository.findById(playerId).orElse(null);
    if (ext != null && ext.getChouquChongwu() != null) {
        List<String> usedPets = Arrays.asList(ext.getChouquChongwu().split(","));
        if (usedPets.contains(String.valueOf(petId))) {
            throw new BusinessException("抽取过成长的宠物无法进化");
        }
    }

    // ========== 第三步：检查材料 ==========

    // 3.1 解析所需材料（remakepid格式：材料1|材料2|材料3，多个可选）
    String[] materialOptions = currentTemplate.getRemakePid().split("\\|");
    boolean hasMaterial = false;
    Integer usedMaterialId = null;

    for (String materialId : materialOptions) {
        BagItem material = bagRepository.findByPlayerIdAndPropId(playerId, Integer.parseInt(materialId));
        if (material != null && material.getCount() > 0) {
            hasMaterial = true;
            usedMaterialId = Integer.parseInt(materialId);
            break;
        }
    }

    if (!hasMaterial) {
        throw new BusinessException("缺少进化材料");
    }

    // 3.2 检查金币
    Player player = playerRepository.findById(playerId)
        .orElseThrow(() -> new BusinessException("玩家不存在"));
    if (player.getMoney() < 1000) {
        throw new BusinessException("金币不足1000");
    }

    // ========== 第四步：计算新成长率 ==========

    double newCzl = calculateEvolutionCzl(pet, currentTemplate);

    // ========== 第五步：执行进化 ==========

    // 5.1 获取进化目标模板
    BbTemplate newTemplate = bbRepository.findById(currentTemplate.getRemakeId())
        .orElseThrow(() -> new BusinessException("进化目标模板不存在"));

    // 5.2 更新宠物属性
    pet.setBbId(newTemplate.getId());
    pet.setName(newTemplate.getName());
    pet.setCzl(newCzl);
    pet.setWx(newTemplate.getWx());
    pet.setRemakeTimes((pet.getRemakeTimes() == null ? 0 : pet.getRemakeTimes()) + 1);

    // 5.3 更新图片
    // 图片格式：t{id}.gif, k{id}.gif, q{id}.gif
    // 由前端根据bbId自动计算

    // 5.4 保存宠物
    userPetRepository.save(pet);

    // 5.5 扣除材料
    bagRepository.decreaseCount(playerId, usedMaterialId, 1);

    // 5.6 扣除金币
    player.setMoney(player.getMoney() - 1000);
    playerRepository.save(player);

    return pet;
}

// 计算进化后的新成长率
private double calculateEvolutionCzl(UserPet pet, BbTemplate template) {
    double oldCzl = pet.getCzl();
    double randomBonus;
    int style = template.getStyle() != null ? template.getStyle() : 1;

    // 根据进化方式计算随机加成
    if (style == 1) {
        // 方式1：较小随机范围
        randomBonus = 0.1 + Math.random() * 0.4; // rand(0.1, 0.5)
    } else {
        // 方式2：较大随机范围
        randomBonus = 0.5 + Math.random() * 0.5; // rand(0.5, 1.0)
    }

    // 等级差加成
    double levelBonus = (pet.getLevel() - template.getRemakeLevel()) / 200.0;

    // 基础新增长率
    double newCzl = oldCzl + randomBonus + levelBonus;

    // 成长率衰减机制
    if (newCzl >= 50) {
        // czl >= 50 时增长减半
        newCzl = oldCzl + (randomBonus + levelBonus) * 0.5;
    }

    // 成长率上限
    if (newCzl >= 150) {
        newCzl = 150;
    }

    return newCzl;
}
```

### 2.2 影响分析

**直接影响**:
- 宠物形态变化（bbid, name改变）
- 宠物成长率变化（czl增加）
- 宠物五行可能变化（wx）
- 进化次数增加（remaketimes）
- 背包材料减少
- 玩家金币减少

**间接影响**:
- 宠物属性重算（新模板的base属性）
- 宠物技能可能变化（新模板的skillist）
- 装备穿戴检查（新五行可能影响装备穿戴）
- 宠物图片变化

**UI刷新**:
- PetPanel - 宠物信息更新
- BagPanel - 材料数量更新
- PlayerInfoPanel - 金币数量更新
- BattlePanel - 宠物属性变化

### 2.3 关联内容

**依赖的模块**:
- `EquipEffectService` - 进化后需要重新计算装备效果
- `SkillService` - 进化后可能需要学习新技能
- `BattleService` - 属性变化影响战斗

**依赖的数据库表**:
- `userbb` - 宠物数据
- `bb` - 宠物模板（remakeid, remakepid, remakelevel, style）
- `player` - 玩家金币
- `player_ext` - 检查是否抽取过成长
- `userbag` - 背包材料
- `props` - 材料道具信息

**需要新增**:
- `BbTemplate` 实体类（或扩展现有实体）
  - `remakeId` - 进化目标ID
  - `remakePid` - 所需材料propId
  - `remakeLevel` - 进化等级要求
  - `style` - 进化方式（1或2）
- `BbRepository` - 查询bb模板
- `UserPet.remakeTimes` 字段

**需要修改**:
- `UserPet` 实体 - 添加 `remakeTimes` 字段
- `PlayerExt` 实体 - 确认 `chouquChongwu` 字段存在

---

## 三、varyname=8 合体类

### 3.1 实现思路

**核心逻辑**:
```
选择两只宠物 → 选择合体道具 → 解析合体公式 → 检查材料 → 概率判定 → 执行合体/失败处理
```

**详细流程**:
```java
// PetService.java - mergePets() 方法

public MergeResult mergePets(Integer playerId, Integer pet1Id, Integer pet2Id,
                              Integer itemId, List<Integer> auxiliaryIds) {
    // ========== 第一步：获取宠物和道具 ==========

    UserPet pet1 = userPetRepository.findById(pet1Id)
        .orElseThrow(() -> new BusinessException("主宠物不存在"));
    UserPet pet2 = userPetRepository.findById(pet2Id)
        .orElseThrow(() -> new BusinessException("副宠物不存在"));

    // 检查宠物归属
    if (!pet1.getUid().equals(playerId.longValue()) ||
        !pet2.getUid().equals(playerId.longValue())) {
        throw new BusinessException("宠物不属于你");
    }

    // 不能合体同一只宠物
    if (pet1Id.equals(pet2Id)) {
        throw new BusinessException("不能与自己合体");
    }

    Props item = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("合体道具不存在"));

    // ========== 第二步：解析合体公式 ==========

    // effect格式：hecheng:A:10%,B:4%|addczl:8%|1
    // 解析为：
    // - requiredPets: [A的bbid, B的bbid]
    // - successRate: 10%, 4% (主宠物成功率, 副宠物成功率)
    // - czlBonus: 8%
    // - style: 1

    MergeFormula formula = parseMergeFormula(item.getEffect());

    // ========== 第三步：检查合体条件 ==========

    // 3.1 检查宠物是否匹配公式要求
    if (!formula.getRequiredPets().isEmpty()) {
        boolean pet1Match = formula.getRequiredPets().contains(pet1.getBbId());
        boolean pet2Match = formula.getRequiredPets().contains(pet2.getBbId());
        if (!pet1Match || !pet2Match) {
            throw new BusinessException("宠物不匹配合体公式要求");
        }
    }

    // 3.2 检查辅助道具（提升成功率）
    double auxiliaryBonus = 0;
    for (Integer auxId : auxiliaryIds) {
        BagItem auxItem = bagRepository.findById(auxId).orElseThrow();
        Props auxProps = propsRepository.findById(auxItem.getPropId()).orElseThrow();
        // 解析辅助道具效果
        auxiliaryBonus += parseAuxiliaryBonus(auxProps.getEffect());
    }

    // ========== 第四步：计算成功率并判定 ==========

    double baseRate = formula.getSuccessRate();
    double finalRate = Math.min(baseRate + auxiliaryBonus, 100);

    boolean success = Math.random() * 100 < finalRate;

    // ========== 第五步：执行合体结果 ==========

    if (success) {
        // 5.1 合体成功 - 提升主宠物属性
        double czlBonus = formula.getCzlBonus();
        pet1.setCzl(pet1.getCzl() + czlBonus);
        pet1.setRemakeTimes((pet1.getRemakeTimes() == null ? 0 : pet1.getRemakeTimes()) + 1);

        // 如果公式指定了新形态，改变宠物形态
        if (formula.getResultBbId() != null && formula.getResultBbId() > 0) {
            BbTemplate newTemplate = bbRepository.findById(formula.getResultBbId()).orElseThrow();
            pet1.setBbId(newTemplate.getId());
            pet1.setName(newTemplate.getName());
            pet1.setWx(newTemplate.getWx());
        }

        userPetRepository.save(pet1);

        // 5.2 删除副宠物
        userPetRepository.delete(pet2);

        // 5.3 扣除合体道具
        bagRepository.decreaseCount(playerId, itemId, 1);

        // 5.4 扣除辅助道具
        for (Integer auxId : auxiliaryIds) {
            bagRepository.decreaseCount(playerId, auxId, 1);
        }

        return MergeResult.success(pet1);
    } else {
        // 5.5 合体失败
        boolean hasProtection = checkProtectionItem(playerId, auxiliaryIds);

        if (hasProtection) {
            // 有保护道具，副宠物不消失
            // 扣除保护道具
            for (Integer auxId : auxiliaryIds) {
                BagItem aux = bagRepository.findById(auxId).orElseThrow();
                Props auxProps = propsRepository.findById(aux.getPropId()).orElseThrow();
                if (isProtectionItem(auxProps)) {
                    bagRepository.decreaseCount(playerId, auxId, 1);
                    break;
                }
            }
        } else {
            // 无保护，副宠物消失
            userPetRepository.delete(pet2);
        }

        // 扣除合体道具
        bagRepository.decreaseCount(playerId, itemId, 1);

        return MergeResult.failure(hasProtection);
    }
}

// 解析合体公式
private MergeFormula parseMergeFormula(String effect) {
    MergeFormula formula = new MergeFormula();

    // 格式：hecheng:A:10%,B:4%|addczl:8%|1
    String[] parts = effect.split("\\|");

    // 第一部分：hecheng:A:10%,B:4%
    String hechengPart = parts[0].replace("hecheng:", "");
    String[] petParts = hechengPart.split(",");

    List<Integer> requiredPets = new ArrayList<>();
    List<Double> successRates = new ArrayList<>();

    for (String petPart : petParts) {
        String[] petInfo = petPart.split(":");
        // petInfo[0] = 宠物标识（如A, B）
        // petInfo[1] = 成功率（如10%）
        successRates.add(Double.parseDouble(petInfo[1].replace("%", "")));
    }

    formula.setRequiredPets(requiredPets);
    formula.setSuccessRate(successRates.get(0)); // 取第一个成功率

    // 第二部分：addczl:8%
    if (parts.length > 1) {
        String czlPart = parts[1];
        if (czlPart.startsWith("addczl:")) {
            double czlBonus = Double.parseDouble(czlPart.replace("addczl:", "").replace("%", ""));
            formula.setCzlBonus(czlBonus);
        }
    }

    // 第三部分：样式（1或2）
    if (parts.length > 2) {
        formula.setStyle(Integer.parseInt(parts[2]));
    }

    return formula;
}
```

### 3.2 影响分析

**直接影响**:
- 主宠物属性提升（czl增加）
- 主宠物形态可能变化（bbid, name）
- 副宠物被删除（合体成功时）
- 背包道具减少（合体道具、辅助道具）

**间接影响**:
- 宠物数量减少
- 宠物属性重算
- 装备穿戴检查

**UI刷新**:
- PetPanel - 宠物列表更新，属性变化
- BagPanel - 道具数量更新
- MergePanel - 合体结果展示

### 3.3 关联内容

**依赖的模块**:
- `EquipEffectService` - 合体后重新计算装备效果
- `BattleService` - 属性变化影响战斗

**依赖的数据库表**:
- `userbb` - 宠物数据（读取、更新、删除）
- `props` - 合体道具效果
- `userbag` - 背包道具
- `bb` - 宠物模板（如果需要改变形态）

**需要新增**:
- `MergeFormula` DTO - 合体公式
- `MergeResult` DTO - 合体结果
- `PetService.mergePets()` 方法

**需要修改**:
- `BagService.useItem()` - 调用合体逻辑

---

## 四、varyname=10/11 精炼类

### 4.1 实现思路

**核心逻辑**:
```
选择装备 → 选择强化材料(varyname=10) → 选择辅助道具(varyname=11) → 计算成功率 → 执行强化
```

**详细流程**:
```java
// EquipmentService.java - strengthen() 方法

public StrengthenResult strengthen(Integer playerId, Integer equipId,
                                    List<Integer> materialIds, List<Integer> auxiliaryIds) {
    // ========== 第一步：获取装备 ==========

    BagItem equip = bagRepository.findById(equipId)
        .orElseThrow(() -> new BusinessException("装备不存在"));

    if (equip.getVaryname() != 9) {
        throw new BusinessException("只能强化装备");
    }

    // ========== 第二步：检查强化材料（varyname=10） ==========

    int totalMaterialCount = 0;
    for (Integer materialId : materialIds) {
        BagItem material = bagRepository.findById(materialId)
            .orElseThrow(() -> new BusinessException("强化材料不存在"));

        if (material.getVaryname() != 10) {
            throw new BusinessException("无效的强化材料");
        }

        totalMaterialCount += material.getCount();
    }

    // 检查材料数量是否足够（根据当前强化等级）
    int currentLevel = parseStrengthenLevel(equip);
    int requiredMaterial = getRequiredMaterialCount(currentLevel);

    if (totalMaterialCount < requiredMaterial) {
        throw new BusinessException("强化材料不足，需要" + requiredMaterial + "个");
    }

    // ========== 第三步：检查辅助道具（varyname=11） ==========

    double auxiliaryBonus = 0;
    boolean hasProtection = false;

    for (Integer auxId : auxiliaryIds) {
        BagItem aux = bagRepository.findById(auxId)
            .orElseThrow(() -> new BusinessException("辅助道具不存在"));

        if (aux.getVaryname() != 11) {
            throw new BusinessException("无效的辅助道具");
        }

        Props auxProps = propsRepository.findById(aux.getPropId()).orElseThrow();
        String usages = auxProps.getUsages();

        // 解析辅助效果
        if (usages.contains("成功率")) {
            auxiliaryBonus += parseSuccessRateBonus(usages);
        }
        if (usages.contains("保护")) {
            hasProtection = true;
        }
    }

    // ========== 第四步：计算强化成功率 ==========

    double baseRate = getStrengthenRate(currentLevel);
    double finalRate = Math.min(baseRate + auxiliaryBonus, 100);

    // ========== 第五步：执行强化 ==========

    boolean success = Math.random() * 100 < finalRate;

    if (success) {
        // 5.1 强化成功 - 提升装备强化等级
        updateStrengthenLevel(equip, currentLevel + 1);
        bagRepository.save(equip);

        // 5.2 扣除材料
        int remaining = requiredMaterial;
        for (Integer materialId : materialIds) {
            BagItem material = bagRepository.findById(materialId).orElseThrow();
            int deduct = Math.min(material.getCount(), remaining);
            material.setCount(material.getCount() - deduct);
            bagRepository.save(material);
            remaining -= deduct;
            if (remaining <= 0) break;
        }

        // 5.3 扣除辅助道具
        for (Integer auxId : auxiliaryIds) {
            bagRepository.decreaseCount(playerId, auxId, 1);
        }

        return StrengthenResult.success(currentLevel + 1);
    } else {
        // 5.4 强化失败
        if (hasProtection) {
            // 有保护，装备不降级
            // 扣除保护道具
            for (Integer auxId : auxiliaryIds) {
                BagItem aux = bagRepository.findById(auxId).orElseThrow();
                Props auxProps = propsRepository.findById(aux.getPropId()).orElseThrow();
                if (isProtectionItem(auxProps)) {
                    bagRepository.decreaseCount(playerId, auxId, 1);
                    break;
                }
            }
        } else {
            // 无保护，装备可能降级
            if (currentLevel > 0 && Math.random() < 0.5) {
                updateStrengthenLevel(equip, currentLevel - 1);
                bagRepository.save(equip);
            }
        }

        // 扣除材料
        for (Integer materialId : materialIds) {
            BagItem material = bagRepository.findById(materialId).orElseThrow();
            material.setCount(0); // 失败时材料全部消失
            bagRepository.save(material);
        }

        return StrengthenResult.failure(hasProtection);
    }
}

// 解析装备强化等级
private int parseStrengthenLevel(BagItem equip) {
    // 强化等级存储在 userbag.plusnum 字段
    // 或者解析装备名称中的+数字
    return equip.getPlusnum() != null ? equip.getPlusnum() : 0;
}

// 更新装备强化等级
private void updateStrengthenLevel(BagItem equip, int newLevel) {
    equip.setPlusnum(newLevel);
    // 可能需要更新装备属性
}

// 获取强化成功率
private double getStrengthenRate(int currentLevel) {
    // 成功率表
    double[] rates = {100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50};
    if (currentLevel < rates.length) {
        return rates[currentLevel];
    }
    return 50; // 默认50%
}

// 获取所需材料数量
private int getRequiredMaterialCount(int currentLevel) {
    // 材料需求表
    int[] counts = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10};
    if (currentLevel < counts.length) {
        return counts[currentLevel];
    }
    return 10;
}
```

### 4.2 影响分析

**直接影响**:
- 装备强化等级提升/降低
- 背包材料减少
- 背包辅助道具减少

**间接影响**:
- 装备属性变化（强化加成）
- 战斗伤害变化

**UI刷新**:
- EquipPanel - 装备强化等级显示
- BagPanel - 材料数量更新

### 4.3 关联内容

**依赖的模块**:
- `EquipEffectService` - 强化后重新计算装备效果
- `BattleService` - 装备属性变化影响战斗

**依赖的数据库表**:
- `userbag` - 装备数据（plusnum字段）
- `props` - 材料和辅助道具信息

**需要新增**:
- `EquipmentService.strengthen()` 方法
- `StrengthenResult` DTO
- 强化等级解析逻辑

---

## 五、varyname=12 礼包类（needkey）

### 5.1 实现思路

**核心逻辑**:
```
使用礼包 → 解析effect → 检查needkey → 扣除钥匙 → 执行giveitems/randitem → 给予物品
```

**详细流程**:
```java
// BagService.java - useItem() 中补充 needkey 处理

case 12: // 礼包类
    // 解析effect
    Map<String, String> effects = EffectParser.parse(item.getEffect());

    // 1. 处理 needkey
    if (effects.containsKey("needkey")) {
        String keyPropId = effects.get("needkey");
        BagItem keyItem = bagRepository.findByPlayerIdAndPropId(
            playerId, Integer.parseInt(keyPropId));

        if (keyItem == null || keyItem.getCount() < 1) {
            throw new BusinessException("您没有开启宝箱的钥匙!");
        }

        // 扣除钥匙
        keyItem.setCount(keyItem.getCount() - 1);
        bagRepository.save(keyItem);
    }

    // 2. 检查背包空间
    int bagUsed = bagRepository.countByPlayerId(playerId);
    int maxBag = playerRepository.findById(playerId).orElseThrow().getMaxbag();

    // 计算需要的空间
    int requiredSpace = calculateRequiredSpace(effects);
    if (maxBag - bagUsed < requiredSpace) {
        throw new BusinessException("背包空间不足，需要" + requiredSpace + "格");
    }

    // 3. 处理 giveitems（固定掉落）
    if (effects.containsKey("giveitems")) {
        String giveitems = effects.get("giveitems");
        String[] items = giveitems.split(",");

        for (String itemStr : items) {
            String[] parts = itemStr.split(":");
            int propId = Integer.parseInt(parts[0]);
            int count = Integer.parseInt(parts[1]);

            // 添加物品到背包
            bagRepository.addItem(playerId, propId, count);
        }
    }

    // 4. 处理 randitem（随机掉落）
    if (effects.containsKey("randitem")) {
        String randitem = effects.get("randitem");
        String[] items = randitem.split("\\|");

        for (String itemStr : items) {
            String[] parts = itemStr.split(":");
            int propId = Integer.parseInt(parts[0]);
            int count = Integer.parseInt(parts[1]);
            int prob = Integer.parseInt(parts[2]); // 1/N 概率
            int flag = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

            // 概率判定
            if (Math.random() * prob < 1) {
                // 命中
                bagRepository.addItem(playerId, propId, count);

                // 发送系统公告
                if (flag == 2) {
                    Props wonProps = propsRepository.findById(propId).orElseThrow();
                    broadcastSystemMessage("恭喜玩家获得" + wonProps.getName());
                }

                break; // 只命中第一个
            }
        }
    }

    // 5. 扣除礼包自身
    bagRepository.decreaseCount(playerId, itemId, 1);
    break;
```

### 5.2 影响分析

**直接影响**:
- 钥匙道具减少（如果有needkey）
- 礼包道具减少
- 获得新物品

**间接影响**:
- 背包空间变化
- 可能触发系统公告

**UI刷新**:
- BagPanel - 道具数量变化
- 系统公告 - 如果触发公告

### 5.3 关联内容

**依赖的模块**:
- 无新增依赖

**依赖的数据库表**:
- `userbag` - 背包道具
- `props` - 道具信息
- `player` - 背包上限

**需要修改**:
- `BagService.useItem()` - 添加needkey处理逻辑

---

## 六、varyname=15 宠物卵

### 6.1 实现思路

**核心逻辑**:
```
使用宠物蛋 → 检查携带数量 → 查询bb模板 → 随机成长率 → 创建宠物 → 学习技能 → 扣除道具
```

**详细流程**:
```java
// PetService.java - createFromEgg() 方法

public UserPet createFromEgg(Integer playerId, Integer itemId) {
    // ========== 第一步：检查携带宠物数量 ==========

    long carriedCount = userPetRepository.countByPlayerIdAndMuchang(playerId, 0);
    if (carriedCount >= 3) {
        throw new BusinessException("携带宠物已满，最多3只");
    }

    // ========== 第二步：获取宠物蛋信息 ==========

    Props egg = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("宠物蛋不存在"));

    // 解析 effect: openpet:bb模板ID
    String effect = egg.getEffect();
    if (!effect.startsWith("openpet:")) {
        throw new BusinessException("无效的宠物蛋");
    }

    int bbTemplateId = Integer.parseInt(effect.replace("openpet:", ""));

    // ========== 第三步：查询bb模板 ==========

    BbTemplate template = bbRepository.findById(bbTemplateId)
        .orElseThrow(() -> new BusinessException("宠物模板不存在"));

    // ========== 第四步：随机生成成长率 ==========

    double czl = generateRandomCzl(template.getCzlRange());
    // czlRange格式："5.0,10.0"

    // ========== 第五步：创建宠物 ==========

    UserPet pet = new UserPet();
    pet.setUid(playerId.longValue());
    pet.setBbId(template.getId());
    pet.setName(template.getName());
    pet.setWx(template.getWx());
    pet.setCzl(czl);
    pet.setLevel(1);
    pet.setNowexp(0L);
    pet.setAc(template.getAc());
    pet.setMc(template.getMc());
    pet.setHp(template.getHp());
    pet.setMp(template.getMp());
    pet.setSrchp(template.getHp());
    pet.setSrcmp(template.getMp());
    pet.setSpeed(template.getSpeed());
    pet.setHits(template.getHits());
    pet.setMiss(template.getMiss());
    pet.setMuchang(0); // 在背包中

    // 设置图片（根据bbId）
    // 图片格式由前端根据bbId自动计算

    userPetRepository.save(pet);

    // ========== 第六步：学习默认技能 ==========

    String skillList = template.getSkillList();
    if (skillList != null && !skillList.isEmpty()) {
        learnDefaultSkills(pet.getId(), skillList);
    }

    // ========== 第七步：扣除宠物蛋 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);

    return pet;
}

// 随机生成成长率
private double generateRandomCzl(String czlRange) {
    if (czlRange == null || czlRange.isEmpty()) {
        return 5.0; // 默认值
    }

    String[] parts = czlRange.split(",");
    double min = Double.parseDouble(parts[0]);
    double max = Double.parseDouble(parts[1]);

    return min + Math.random() * (max - min);
}

// 学习默认技能
private void learnDefaultSkills(Integer petId, String skillList) {
    // skillList格式："技能ID:等级,技能ID:等级,..."
    String[] skills = skillList.split(",");

    for (String skillStr : skills) {
        String[] parts = skillStr.split(":");
        int skillId = Integer.parseInt(parts[0]);
        int level = Integer.parseInt(parts[1]);

        // 查询技能信息
        SkillSys skillSys = skillSysRepository.findById(skillId).orElse(null);
        if (skillSys == null) continue;

        // 创建技能记录
        Skill skill = new Skill();
        skill.setBid(petId);
        skill.setSid(skillId);
        skill.setLevel(level);
        skill.setName(skillSys.getName());
        skill.setEffect(skillSys.getEffect());
        skill.setImg(skillSys.getImg());

        skillRepository.save(skill);
    }
}
```

### 6.2 影响分析

**直接影响**:
- 新增宠物到背包
- 宠物蛋道具减少
- 新增技能记录

**间接影响**:
- 宠物数量增加
- 可能影响主战宠物选择

**UI刷新**:
- PetPanel - 宠物列表更新
- BagPanel - 道具数量更新

### 6.3 关联内容

**依赖的模块**:
- `SkillService` - 学习技能

**依赖的数据库表**:
- `userbb` - 新增宠物
- `skill` - 新增技能
- `bb` - 宠物模板
- `skillsys` - 技能模板
- `userbag` - 背包道具

**需要新增**:
- `PetService.createFromEgg()` 方法
- `BbRepository` - 查询bb模板
- `SkillSysRepository` - 查询技能模板

**需要修改**:
- `BagService.useItem()` - 调用创建宠物逻辑

---

## 七、varyname=16 合成类

### 7.1 实现思路

**核心逻辑**:
```
使用图纸 → 解析合成公式 → 检查材料 → 扣除材料 → 生成产物
```

**详细流程**:
```java
// ComposeService.java - compose() 方法

public ComposeResult compose(Integer playerId, Integer itemId) {
    // ========== 第一步：获取图纸信息 ==========

    Props blueprint = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("图纸不存在"));

    String effect = blueprint.getEffect();

    // ========== 第二步：解析合成公式 ==========

    if (effect.startsWith("hecheng:")) {
        return handleHecheng(playerId, itemId, effect);
    } else if (effect.startsWith("chongzhu:")) {
        return handleChongzhu(playerId, itemId, effect);
    } else if (effect.startsWith("random_combine:")) {
        return handleRandomCombine(playerId, itemId, effect);
    } else {
        throw new BusinessException("未知的合成公式");
    }
}

// 处理图纸合成
private ComposeResult handleHecheng(Integer playerId, Integer itemId, String effect) {
    // 格式：hecheng:(材料1ID:数量|材料2ID:数量|...):产物1ID:数量|产物2ID:数量

    String content = effect.replace("hecheng:", "");
    String[] parts = content.split(":");

    // 解析材料
    String materialsPart = parts[0].replace("(", "").replace(")", "");
    String[] materials = materialsPart.split("\\|");

    // 解析产物
    String productsPart = parts[1];
    String[] products = productsPart.split("\\|");

    // ========== 检查材料 ==========

    for (String materialStr : materials) {
        String[] materialParts = materialStr.split(":");
        int propId = Integer.parseInt(materialParts[0]);
        int required = Integer.parseInt(materialParts[1]);

        BagItem material = bagRepository.findByPlayerIdAndPropId(playerId, propId);
        if (material == null || material.getCount() < required) {
            Props materialProps = propsRepository.findById(propId).orElseThrow();
            throw new BusinessException(materialProps.getName() + "数量不足");
        }
    }

    // ========== 检查背包空间 ==========

    int bagUsed = bagRepository.countByPlayerId(playerId);
    int maxBag = playerRepository.findById(playerId).orElseThrow().getMaxbag();
    int productCount = products.length;

    if (maxBag - bagUsed < productCount) {
        throw new BusinessException("背包空间不足");
    }

    // ========== 扣除材料 ==========

    for (String materialStr : materials) {
        String[] materialParts = materialStr.split(":");
        int propId = Integer.parseInt(materialParts[0]);
        int required = Integer.parseInt(materialParts[1]);

        // 扣除材料
        bagRepository.decreaseCountByPropId(playerId, propId, required);
    }

    // ========== 扣除图纸 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);

    // ========== 生成产物 ==========

    for (String productStr : products) {
        String[] productParts = productStr.split(":");
        int propId = Integer.parseInt(productParts[0]);
        int count = Integer.parseInt(productParts[1]);

        bagRepository.addItem(playerId, propId, count);
    }

    return ComposeResult.success();
}

// 处理重铸合成
private ComposeResult handleChongzhu(Integer playerId, Integer itemId, String effect) {
    // 格式：chongzhu:(材料propId1|材料propId2|...):产物1ID:概率|产物2ID:概率

    String content = effect.replace("chongzhu:", "");
    String[] parts = content.split(":");

    // 解析材料选项
    String materialsPart = parts[0].replace("(", "").replace(")", "");
    String[] materialOptions = materialsPart.split("\\|");

    // 解析产物
    String productsPart = parts[1];
    String[] products = productsPart.split("\\|");

    // ========== 从背包中随机选择一个符合条件的材料 ==========

    List<BagItem> availableMaterials = new ArrayList<>();
    for (String materialId : materialOptions) {
        BagItem material = bagRepository.findByPlayerIdAndPropId(
            playerId, Integer.parseInt(materialId));
        if (material != null && material.getCount() > 0) {
            availableMaterials.add(material);
        }
    }

    if (availableMaterials.isEmpty()) {
        throw new BusinessException("背包中没有可重铸的物品");
    }

    // 随机选择一个
    BagItem selectedMaterial = availableMaterials.get(
        (int) (Math.random() * availableMaterials.size()));

    // ========== 按概率判定产物 ==========

    int random = (int) (Math.random() * 100);
    int cumulative = 0;

    for (String productStr : products) {
        String[] productParts = productStr.split(":");
        int propId = Integer.parseInt(productParts[0]);
        int probability = Integer.parseInt(productParts[1]);

        cumulative += probability;

        if (random < cumulative) {
            // 命中该产物
            bagRepository.addItem(playerId, propId, 1);

            // 扣除材料和图纸
            bagRepository.decreaseCount(playerId, selectedMaterial.getId(), 1);
            bagRepository.decreaseCount(playerId, itemId, 1);

            // 记录日志
            Props productProps = propsRepository.findById(propId).orElseThrow();
            logService.addLog(playerId, "重铸成功，获得" + productProps.getName());

            return ComposeResult.success();
        }
    }

    // 未命中任何产物（重铸失败）
    bagRepository.decreaseCount(playerId, selectedMaterial.getId(), 1);
    bagRepository.decreaseCount(playerId, itemId, 1);

    return ComposeResult.failure();
}
```

### 7.2 影响分析

**直接影响**:
- 材料道具减少
- 图纸道具减少
- 产物道具增加

**间接影响**:
- 背包空间变化
- 可能触发系统公告

**UI刷新**:
- BagPanel - 道具数量变化
- ComposePanel - 合成结果展示

### 7.3 关联内容

**依赖的模块**:
- 无新增依赖

**依赖的数据库表**:
- `userbag` - 背包道具
- `props` - 道具信息

**需要新增**:
- `ComposeService` - 合成服务
- `ComposeResult` DTO

**需要修改**:
- `BagService.useItem()` - 调用合成逻辑

---

## 八、varyname=24 卡片类

### 8.1 实现思路

**核心逻辑**:
```
使用卡片 → 查询玩家卡片信息 → 添加卡片 → 检查称号 → 获得称号
```

**详细流程**:
```java
// CardService.java - useCard() 方法

public CardResult useCard(Integer playerId, Integer itemId) {
    // ========== 第一步：获取卡片信息 ==========

    Props card = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("卡片不存在"));

    String cardName = card.getName();

    // ========== 第二步：获取玩家卡片信息 ==========

    PlayerExt ext = playerExtRepository.findById(playerId)
        .orElseThrow(() -> new BusinessException("玩家数据不存在"));

    String cardInfo = ext.getUserCardInfo();
    Map<String, Integer> cards = parseCardInfo(cardInfo);

    // ========== 第三步：添加卡片 ==========

    cards.put(cardName, cards.getOrDefault(cardName, 0) + 1);

    // ========== 第四步：保存卡片信息 ==========

    ext.setUserCardInfo(serializeCardInfo(cards));
    playerExtRepository.save(ext);

    // ========== 第五步：扣除道具 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);

    // ========== 第六步：检查称号 ==========

    TitleResult titleResult = checkTitleCompletion(playerId, ext, cards);

    return CardResult.success(cards.get(cardName), titleResult);
}

// 解析卡片信息
private Map<String, Integer> parseCardInfo(String cardInfo) {
    Map<String, Integer> cards = new LinkedHashMap<>();

    if (cardInfo == null || cardInfo.isEmpty()) {
        return cards;
    }

    // 格式："卡片名1:数量,卡片名2:数量,..."
    String[] entries = cardInfo.split(",");
    for (String entry : entries) {
        String[] parts = entry.split(":");
        if (parts.length == 2) {
            cards.put(parts[0], Integer.parseInt(parts[1]));
        }
    }

    return cards;
}

// 序列化卡片信息
private String serializeCardInfo(Map<String, Integer> cards) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Integer> entry : cards.entrySet()) {
        if (sb.length() > 0) sb.append(",");
        sb.append(entry.getKey()).append(":").append(entry.getValue());
    }
    return sb.toString();
}

// 检查称号完成
private TitleResult checkTitleCompletion(Integer playerId, PlayerExt ext,
                                          Map<String, Integer> cards) {
    // 查询所有称号要求
    List<TitleRequirement> requirements = titleRequirementRepository.findAll();

    for (TitleRequirement req : requirements) {
        // 解析称号所需卡片
        Map<String, Integer> required = parseCardInfo(req.getRequiredCards());

        // 检查是否集齐
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
                Player player = playerRepository.findById(playerId).orElseThrow();
                broadcastSystemMessage(
                    "恭喜玩家" + player.getNickname() + "获得称号【" + req.getTitleName() + "】");

                return TitleResult.achieved(req.getTitleName());
            }
        }
    }

    return TitleResult.none();
}
```

### 8.2 影响分析

**直接影响**:
- 卡片道具减少
- 玩家卡片信息更新
- 可能获得称号

**间接影响**:
- 称号可能带来属性加成
- 系统公告

**UI刷新**:
- BagPanel - 道具数量更新
- CardPanel - 卡片收集进度
- PlayerInfoPanel - 称号显示

### 8.3 关联内容

**依赖的模块**:
- 无新增依赖

**依赖的数据库表**:
- `userbag` - 背包道具
- `player_ext` - 卡片信息、称号
- `props` - 道具信息
- `T_Card_to_Title` - 称号配置

**需要新增**:
- `CardService` - 卡片服务
- `TitleRequirement` 实体 + Repository
- `CardResult` DTO

---

## 九、varyname=25/26/27 宝石系统

### 9.1 实现思路

#### 9.1.1 宝石合成（varyname=25 + varyname=27）

```java
// GemService.java - mergeGems() 方法

public GemMergeResult mergeGems(Integer playerId, Integer gem1Id, Integer gem2Id,
                                 Integer protectId) {
    // ========== 第一步：获取宝石 ==========

    BagItem gem1 = bagRepository.findById(gem1Id)
        .orElseThrow(() -> new BusinessException("宝石1不存在"));
    BagItem gem2 = bagRepository.findById(gem2Id)
        .orElseThrow(() -> new BusinessException("宝石2不存在"));

    if (gem1.getVaryname() != 25 || gem2.getVaryname() != 25) {
        throw new BusinessException("只能合成宝石");
    }

    // ========== 第二步：检查宝石是否同级 ==========

    Props gem1Props = propsRepository.findById(gem1.getPropId()).orElseThrow();
    Props gem2Props = propsRepository.findById(gem2.getPropId()).orElseThrow();

    if (!gem1Props.getName().equals(gem2Props.getName())) {
        throw new BusinessException("只能合成同种宝石");
    }

    // ========== 第三步：获取高级宝石 ==========

    Props nextLevelGem = getNextLevelGem(gem1Props);
    if (nextLevelGem == null) {
        throw new BusinessException("已达最高级，无法继续合成");
    }

    // ========== 第四步：计算成功率 ==========

    double successRate = getGemMergeRate(gem1Props);
    boolean success = Math.random() * 100 < successRate;

    // ========== 第五步：执行合成 ==========

    if (success) {
        // 合成成功
        bagRepository.addItem(playerId, nextLevelGem.getId(), 1);
    } else {
        // 合成失败
        if (protectId != null) {
            // 检查保底石
            BagItem protect = bagRepository.findById(protectId)
                .orElseThrow(() -> new BusinessException("保底石不存在"));

            if (protect.getVaryname() == 27) {
                // 使用保底石，宝石不消失
                bagRepository.decreaseCount(playerId, protectId, 1);
            } else {
                // 保底石类型错误，宝石消失
            }
        } else {
            // 无保底石，宝石消失
        }
    }

    // 扣除材料宝石
    bagRepository.decreaseCount(playerId, gem1Id, 1);
    bagRepository.decreaseCount(playerId, gem2Id, 1);

    return GemMergeResult.success(success, nextLevelGem);
}
```

#### 9.1.2 宝石镶嵌（varyname=25 → varyname=9）

```java
// GemService.java - embedGem() 方法

public void embedGem(Integer playerId, Integer equipId, Integer gemId, int slotIndex) {
    // ========== 第一步：获取装备和宝石 ==========

    BagItem equip = bagRepository.findById(equipId)
        .orElseThrow(() -> new BusinessException("装备不存在"));
    BagItem gem = bagRepository.findById(gemId)
        .orElseThrow(() -> new BusinessException("宝石不存在"));

    if (equip.getVaryname() != 9) {
        throw new BusinessException("只能镶嵌到装备上");
    }

    if (gem.getVaryname() != 25) {
        throw new BusinessException("只能镶嵌宝石");
    }

    // ========== 第二步：检查宝石孔 ==========

    String holeInfo = equip.getItemHoleInfo();
    // holeInfo格式："孔位1:宝石ID,孔位2:宝石ID,..."

    if (!hasEmptySlot(holeInfo, slotIndex)) {
        throw new BusinessException("该位置已有宝石");
    }

    // ========== 第三步：镶嵌宝石 ==========

    equip.setItemHoleInfo(updateHoleInfo(holeInfo, slotIndex, gem.getPropId()));
    bagRepository.save(equip);

    // ========== 第四步：扣除宝石 ==========

    bagRepository.decreaseCount(playerId, gemId, 1);
}
```

#### 9.1.3 宝石洗练（varyname=26）

```java
// GemService.java - washGems() 方法

public void washGems(Integer playerId, Integer equipId) {
    // ========== 第一步：获取装备 ==========

    BagItem equip = bagRepository.findById(equipId)
        .orElseThrow(() -> new BusinessException("装备不存在"));

    if (equip.getVaryname() != 9) {
        throw new BusinessException("只能洗练装备");
    }

    // ========== 第二步：清除宝石孔 ==========

    equip.setItemHoleInfo(null);
    bagRepository.save(equip);

    // ========== 第三步：扣除洗练石 ==========

    // 洗练石在调用前已扣除
}
```

### 9.2 影响分析

**直接影响**:
- 宝石合成：高级宝石获得，材料宝石减少
- 宝石镶嵌：装备属性变化，宝石减少
- 宝石洗练：装备宝石效果清除

**间接影响**:
- 装备属性变化影响战斗

**UI刷新**:
- GemPanel - 宝石合成/镶嵌/洗练界面
- EquipPanel - 装备宝石孔显示
- BagPanel - 宝石数量变化

### 9.3 关联内容

**依赖的模块**:
- `EquipEffectService` - 宝石镶嵌后重新计算装备效果

**依赖的数据库表**:
- `userbag` - 装备（F_item_hole_info字段）、宝石
- `props` - 宝石信息

**需要新增**:
- `GemService` - 宝石服务
- `GemMergeResult` DTO

**需要修改**:
- `BagItem` 实体 - 添加 `itemHoleInfo` 字段

---

## 十、varyname=55/57/58 魔塔道具

### 10.1 实现思路

#### 10.1.1 varyname=55 洗点

```java
// WarService.java - useTalentReset() 方法

public void useTalentReset(Integer playerId, Integer itemId) {
    // ========== 第一步：获取道具效果 ==========

    Props item = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("道具不存在"));

    String effect = item.getEffect(); // xidian:次数
    int count = Integer.parseInt(effect.replace("xidian:", ""));

    // ========== 第二步：查询或创建war_player ==========

    WarPlayer warPlayer = warPlayerRepository.findByPlayerId(playerId);
    if (warPlayer == null) {
        warPlayer = new WarPlayer();
        warPlayer.setPlayerId(playerId);
        warPlayer.setWashTalentCount(0);
    }

    // ========== 第三步：增加洗点次数 ==========

    warPlayer.setWashTalentCount(warPlayer.getWashTalentCount() + count);
    warPlayerRepository.save(warPlayer);

    // ========== 第四步：扣除道具 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);
}
```

#### 10.1.2 varyname=57 出战卷

```java
// WarService.java - usePetSlotExpand() 方法

public void usePetSlotExpand(Integer playerId, Integer itemId) {
    // ========== 第一步：获取道具效果 ==========

    Props item = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("道具不存在"));

    String effect = item.getEffect(); // xiedaibb20/xiedaibb21/xiedaibb30/xiedaibb31

    // ========== 第二步：查询或创建war_player ==========

    WarPlayer warPlayer = warPlayerRepository.findByPlayerId(playerId);
    if (warPlayer == null) {
        warPlayer = new WarPlayer();
        warPlayer.setPlayerId(playerId);
    }

    // ========== 第三步：更新出战数量 ==========

    if (effect.contains("xiedaibb20")) {
        // 2只，永久
        warPlayer.setMaxTakePetNum(2);
        warPlayer.setTakePetLimitTime(0L);
    } else if (effect.contains("xiedaibb21")) {
        // 2只，30天
        warPlayer.setMaxTakePetNum(2);
        warPlayer.setTakePetLimitTime(
            System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
    } else if (effect.contains("xiedaibb30")) {
        // 3只，永久
        warPlayer.setMaxTakePetNum(3);
        warPlayer.setTakePetLimitTime(0L);
    } else if (effect.contains("xiedaibb31")) {
        // 3只，30天
        warPlayer.setMaxTakePetNum(3);
        warPlayer.setTakePetLimitTime(
            System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
    }

    warPlayerRepository.save(warPlayer);

    // ========== 第四步：扣除道具 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);
}
```

#### 10.1.3 varyname=58 天赋经验

```java
// WarService.java - useTalentExp() 方法

public void useTalentExp(Integer playerId, Integer itemId) {
    // ========== 第一步：检查主战宠物 ==========

    Player player = playerRepository.findById(playerId)
        .orElseThrow(() -> new BusinessException("玩家不存在"));

    Integer mainPetId = player.getMbid();
    if (mainPetId == null) {
        throw new BusinessException("没有主战宠物");
    }

    // ========== 第二步：获取天赋列表 ==========

    List<WarFighterTalent> talents = warFighterTalentRepository.findByFighterId(mainPetId);
    if (talents.isEmpty()) {
        throw new BusinessException("宠物未进入魔塔，没有天赋数据");
    }

    // ========== 第三步：解析经验范围 ==========

    Props item = propsRepository.findById(itemId)
        .orElseThrow(() -> new BusinessException("道具不存在"));

    String effect = item.getEffect(); // tianfuexp:min,max
    String[] parts = effect.replace("tianfuexp:", "").split(",");
    int min = Integer.parseInt(parts[0]);
    int max = Integer.parseInt(parts[1]);

    int totalExp = min + new Random().nextInt(max - min + 1);

    // ========== 第四步：平均分配给所有天赋 ==========

    int expPerTalent = (int) Math.ceil((double) totalExp / talents.size());

    for (WarFighterTalent talent : talents) {
        talent.setCurrentExperience(talent.getCurrentExperience() + expPerTalent);
        warFighterTalentRepository.save(talent);
    }

    // ========== 第五步：扣除道具 ==========

    bagRepository.decreaseCount(playerId, itemId, 1);
}
```

### 10.2 影响分析

**直接影响**:
- war_player 表更新（洗点次数、出战数量）
- war_fighter_talent 表更新（天赋经验）
- 背包道具减少

**间接影响**:
- 魔塔战斗能力变化
- 宠物出战数量变化

**UI刷新**:
- TowerPanel - 魔塔界面
- BagPanel - 道具数量更新

### 10.3 关联内容

**依赖的模块**:
- 无新增依赖

**依赖的数据库表**:
- `war_player` - 魔塔玩家数据
- `war_fighter_talent` - 魔塔天赋数据
- `userbag` - 背包道具
- `player` - 主战宠物ID

**需要新增**:
- `WarService` - 魔塔服务
- `WarPlayer` 实体 + Repository
- `WarFighterTalent` 实体 + Repository

---

## 十一、总结

### 11.1 新增模块清单

| 模块 | 类型 | 涉及 varyname |
|:----:|:----:|:-------------|
| EffectParser | 工具类 | 所有 |
| BbService | Service | 7, 8, 15 |
| MergeService | Service | 8 |
| EquipmentService | Service | 10, 11, 25, 26, 27 |
| CardService | Service | 24 |
| GemService | Service | 25, 26, 27 |
| WarService | Service | 55, 57, 58 |
| ComposeService | Service | 16 |

### 11.2 新增实体清单

| 实体 | 对应表 | 涉及 varyname |
|:----:|:------:|:-------------|
| BbTemplate | bb | 7, 8, 15 |
| MergeFormula | (DTO) | 8 |
| StrengthenResult | (DTO) | 10, 11 |
| TitleRequirement | T_Card_to_Title | 24 |
| WarPlayer | war_player | 55, 57 |
| WarFighterTalent | war_fighter_talent | 58 |

### 11.3 修改现有模块

| 模块 | 修改内容 |
|:----:|---------|
| BagService.useItem() | 添加各种 effect 处理逻辑 |
| UserPet | 添加 remakeTimes 字段 |
| PlayerExt | 确认 chouquChongwu, userCardInfo, hasTitle 字段 |
| BagItem | 添加 itemHoleInfo, plusnum 字段 |

### 11.4 数据库变更

| 表名 | 变更类型 | 说明 |
|:----:|:--------:|------|
| userbb | 添加字段 | remake_times INT |
| userbag | 确认字段 | item_hole_info, plusnum |
| war_player | 新表 | 魔塔玩家数据 |
| war_fighter_talent | 新表 | 魔塔天赋数据 |
| T_Card_to_Title | 确认存在 | 称号配置表 |
