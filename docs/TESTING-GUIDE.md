# 道具系统测试指南

> 更新时间: 2026-06-04 | 测试环境配置、账号、测试步骤、影响页面

---

## 一、测试环境配置

### 1.1 服务地址

| 服务 | 地址 | 说明 |
|:----:|:----:|------|
| 前端 | http://localhost:3000 | React + Vite 开发服务器 |
| 后端API | http://localhost:8080 | Spring Boot Game API |
| 管理后台 | http://localhost:8081 | Admin Panel (Thymeleaf) |
| MySQL | localhost:3306 | 数据库 |
| Redis | localhost:6379 | 缓存（可选） |

### 1.2 测试账号

| 账号 | 密码 | 角色 | UID | 主战宠物ID | 说明 |
|:----:|:----:|:----:|:---:|:----------:|------|
| testuser | test123 | 玩家 | 102 | 86 | 主测试账号 |
| admin | admin123 | 管理员 | — | — | 管理后台 |
| kdjl | kdjl_pass | DB用户 | — | — | 数据库访问 |

### 1.3 数据库配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kdjl?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: kdjl
    password: kdjl_pass
```

### 1.4 快速启动

```bash
# 1. 启动数据库（确保MySQL已安装）
mysql -u kdjl -p'kdjl_pass' kdjl

# 2. 启动后端
cd kdjl-backend
mvn install -pl kdjl-common -DskipTests
mvn spring-boot:run -pl kdjl-server

# 3. 启动前端
cd kdjl-frontend
npm install
npm run dev
```

---

## 二、测试工具

### 2.1 前端测试

```bash
cd kdjl-frontend

# 运行所有测试
npm run test

# 运行特定测试文件
npm run test -- path/to/test.ts

# 查看测试覆盖率
npm run test -- --coverage

# 监听模式
npm run test -- --watch
```

**测试框架**: Vitest + @testing-library/react

### 2.2 后端测试

```bash
cd kdjl-backend

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=BagServiceTest

# 跳过测试编译
mvn install -DskipTests
```

**测试框架**: JUnit 5 + Spring Boot Test

### 2.3 API 测试

```bash
# 登录获取Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'

# 使用Token访问API
curl -X GET http://localhost:8080/api/bag \
  -H "Authorization: Bearer <your-token>"
```

### 2.4 数据库直接查询

```bash
# 连接数据库
mysql -u kdjl -p'kdjl_pass' kdjl

# 常用查询
SELECT * FROM player WHERE id = 102;           -- 玩家信息
SELECT * FROM userbb WHERE uid = 102;          -- 玩家宠物
SELECT * FROM userbag WHERE uid = 102;         -- 玩家背包
SELECT * FROM props WHERE id = 4045;           -- 道具信息
```

---

## 三、功能测试用例

### 3.1 varyname=1 辅助类（药水）

**实现逻辑**: 战斗中使用，恢复宠物HP/MP
**影响页面**: BattlePanel（战斗面板）
**改动文件**: BagService.java

**测试步骤**:
1. 登录 testuser 账号
2. 进入任意地图开始战斗
3. 在战斗中点击"道具"按钮
4. 选择药水（如治疗药水）
5. 选择目标宠物
6. 验证HP/MP是否恢复

**验证SQL**:
```sql
-- 使用前
SELECT hp, mp FROM userbb WHERE id = 86;

-- 使用后（应该增加）
SELECT hp, mp FROM userbb WHERE id = 86;
```

---

### 3.2 varyname=2 增益类（永久属性药水）

**实现逻辑**: 给主战宠物永久增加属性
**影响页面**: BagPanel（背包面板）
**改动文件**: BagService.java

**测试用例**:

#### 3.2.1 addexp（经验药水）
```bash
# 准备测试数据
# 确保背包中有 addexp 效果的道具

# 测试步骤
1. 打开背包面板
2. 选择 addexp 道具
3. 点击使用
4. 验证宠物经验是否增加
```

**验证SQL**:
```sql
-- 使用前
SELECT level, nowexp FROM userbb WHERE id = 86;

-- 使用后
SELECT level, nowexp FROM userbb WHERE id = 86;
```

#### 3.2.2 addczl（成长率药水）
**特殊限制**: 每只宠物只能使用一次

```sql
-- 检查是否已使用过
SELECT chouqu_chongwu FROM player_ext WHERE uid = 102;

-- 使用前成长率
SELECT czl FROM userbb WHERE id = 86;

-- 使用后成长率（应该增加）
SELECT czl FROM userbb WHERE id = 86;
```

#### 3.2.3 其他属性药水（addac/addmc/addhp/addmp/addspeed/addhits/addmiss）

**测试步骤**:
1. 记录使用前属性值
2. 使用道具
3. 验证属性是否正确增加

**验证SQL**:
```sql
-- 使用前
SELECT ac, mc, srchp, srcmp, speed, hits, miss FROM userbb WHERE id = 86;

-- 使用后
SELECT ac, mc, srchp, srcmp, speed, hits, miss FROM userbb WHERE id = 86;
```

---

### 3.3 varyname=3 捕捉类（精灵球）

**实现逻辑**: 战斗中捕捉怪物为宠物
**影响页面**: BattlePanel（战斗面板）
**改动文件**: PetService.java

**测试步骤**:
1. 进入战斗
2. 将怪物HP打低（提高捕捉成功率）
3. 点击"捕捉"按钮
4. 选择精灵球
5. 验证是否成功捕捉

**验证SQL**:
```sql
-- 捕捉成功后，应新增宠物记录
SELECT * FROM userbb WHERE uid = 102 ORDER BY id DESC LIMIT 5;
```

---

### 3.4 varyname=5 技能书类

**实现逻辑**: 宠物学习新技能
**影响页面**: PetPanel（宠物面板）
**改动文件**: SkillService.java

**测试步骤**:
1. 打开宠物面板
2. 选择目标宠物
3. 点击"学习技能"
4. 选择技能书
5. 验证技能是否学会

**验证SQL**:
```sql
-- 学习前
SELECT * FROM skill WHERE bid = 86;

-- 学习后（应该新增技能记录）
SELECT * FROM skill WHERE bid = 86;
```

---

### 3.5 varyname=7 进化类（待实现）

**实现逻辑**: 宠物进化为更高级形态
**影响页面**: EvolvePanel（进化面板，待开发）
**改动文件**: BagService.java, PetService.java

**测试用例**:
```java
@Test
void testEvolvePet() {
    // 1. 准备测试数据
    //    - 宠物等级 >= 进化要求
    //    - 背包中有进化材料
    //    - 宠物未抽取过成长

    // 2. 调用进化接口
    // 3. 验证宠物形态变化
    // 4. 验证成长率变化
    // 5. 验证材料扣除
}
```

**验证SQL**:
```sql
-- 进化前
SELECT bbid, name, czl, remaketimes FROM userbb WHERE id = 86;

-- 进化后（bbid应变为进化目标，成长率应增加）
SELECT bbid, name, czl, remaketimes FROM userbb WHERE id = 86;

-- 材料应被扣除
SELECT count FROM userbag WHERE uid = 102 AND pid = <材料ID>;
```

---

### 3.6 varyname=8 合体类（待实现）

**实现逻辑**: 两只宠物融合为一只
**影响页面**: MergePanel（合体面板，待开发）
**改动文件**: BagService.java, PetService.java

**测试用例**:
```java
@Test
void testMergePets() {
    // 1. 准备两只宠物
    // 2. 准备合体道具
    // 3. 调用合体接口
    // 4. 验证宠物属性变化
    // 5. 验证宠物2被删除
}
```

---

### 3.7 varyname=9 装备类

**实现逻辑**: 给宠物穿戴装备
**影响页面**: EquipPanel（装备面板）
**改动文件**: BagService.java, EquipEffectService.java

**测试步骤**:
1. 打开装备面板
2. 选择装备
3. 点击"穿戴"
4. 验证装备是否生效

**验证SQL**:
```sql
-- 穿戴前
SELECT zb FROM userbb WHERE id = 86;

-- 穿戴后（应包含新装备ID）
SELECT zb FROM userbb WHERE id = 86;

-- 装备状态应更新
SELECT zbing, zbpets FROM userbag WHERE id = <装备ID>;
```

---

### 3.8 varyname=12 礼包类

**实现逻辑**: 开启礼包获得随机物品
**影响页面**: BagPanel（背包面板）
**改动文件**: BagService.java

**测试用例**:

#### 3.8.1 giveitems（固定掉落）
```bash
# 使用前记录背包物品
SELECT * FROM userbag WHERE uid = 102;

# 使用礼包
# 验证是否获得所有固定物品
```

#### 3.8.2 randitem（随机掉落）
```bash
# 使用前记录背包物品
# 使用礼包
# 验证是否获得随机物品（可能需要多次测试）
```

#### 3.8.3 needkey（需要钥匙）
```bash
# 测试1: 无钥匙时报错
# 测试2: 有钥匙时正常开启，钥匙被扣除
```

**验证SQL**:
```sql
-- 钥匙应被扣除
SELECT count FROM userbag WHERE uid = 102 AND pid = <钥匙ID>;

-- 应获得新物品
SELECT * FROM userbag WHERE uid = 102 ORDER BY id DESC LIMIT 10;
```

---

### 3.9 varyname=13 特殊类

**实现逻辑**: 各种功能道具
**影响页面**: BagPanel（背包面板）, PlayerInfoPanel（玩家信息面板）
**改动文件**: BagService.java

**测试用例**:

#### 3.9.1 exp（经验卷）
```sql
-- 使用前
SELECT dblexpflag, dblstime, maxdblexptime FROM player WHERE id = 102;

-- 使用后（应开启双倍经验）
SELECT dblexpflag, dblstime, maxdblexptime FROM player WHERE id = 102;
```

#### 3.9.2 addbag（背包扩展）
```sql
-- 使用前
SELECT maxbag FROM player WHERE id = 102;

-- 使用后（应增加）
SELECT maxbag FROM player WHERE id = 102;
```

#### 3.9.3 openmap（解锁地图）
```sql
-- 使用前
SELECT openmap FROM player WHERE id = 102;

-- 使用后（应包含新地图ID）
SELECT openmap FROM player WHERE id = 102;
```

#### 3.9.4 auto/autofree（自动战斗次数）
```sql
-- 使用前
SELECT sysautosum, maxautofitsum FROM player WHERE id = 102;

-- 使用后（应增加）
SELECT sysautosum, maxautofitsum FROM player WHERE id = 102;
```

---

### 3.10 varyname=15 宠物卵（待实现）

**实现逻辑**: 使用宠物蛋创建新宠物
**影响页面**: BagPanel（背包面板）
**改动文件**: BagService.java, PetService.java

**测试用例**:
```java
@Test
void testOpenPet() {
    // 1. 准备宠物蛋道具
    // 2. 检查携带宠物数 < 3
    // 3. 使用宠物蛋
    // 4. 验证新宠物创建
    // 5. 验证技能学习
    // 6. 验证道具扣除
}
```

**验证SQL**:
```sql
-- 使用前宠物数
SELECT COUNT(*) FROM userbb WHERE uid = 102 AND muchang = 0;

-- 使用后（应新增宠物）
SELECT * FROM userbb WHERE uid = 102 ORDER BY id DESC LIMIT 1;

-- 宠物技能
SELECT * FROM skill WHERE bid = <新宠物ID>;
```

---

### 3.11 varyname=16 合成类（待实现）

**实现逻辑**: 合成/重铸物品
**影响页面**: ComposePanel（合成面板，待开发）
**改动文件**: BagService.java

**测试用例**:

#### 3.11.1 hecheng（图纸合成）
```bash
# 准备材料和图纸
# 调用合成接口
# 验证材料扣除
# 验证产物获得
```

#### 3.11.2 chongzhu（重铸合成）
```bash
# 准备待重铸物品和图纸
# 调用重铸接口
# 验证物品变化
```

---

### 3.12 varyname=24 卡片类（待实现）

**实现逻辑**: 收集卡片获得称号
**影响页面**: CardPanel（卡片面板，待开发）
**改动文件**: BagService.java

**测试用例**:
```java
@Test
void testUseCard() {
    // 1. 使用卡片
    // 2. 验证卡片记录更新
    // 3. 检查是否集齐称号
    // 4. 验证称号获得
}
```

**验证SQL**:
```sql
-- 卡片信息
SELECT F_User_Card_Info, F_Has_Title FROM player_ext WHERE uid = 102;
```

---

### 3.13 varyname=25/26/27 宝石系统（待实现）

**实现逻辑**: 宝石合成、镶嵌、洗练
**影响页面**: GemPanel（宝石面板，待开发）
**改动文件**: BagService.java, EquipmentService.java

**测试用例**:

#### 3.13.1 宝石合成
```bash
# 准备两颗同级宝石
# 调用合成接口
# 验证高级宝石获得
```

#### 3.13.2 宝石镶嵌
```bash
# 准备装备和宝石
# 调用镶嵌接口
# 验证装备属性变化
```

#### 3.13.3 宝石洗练
```bash
# 准备已镶嵌宝石的装备
# 调用洗练接口
# 验证宝石效果清除
```

---

### 3.14 varyname=55/57/58 魔塔道具（待确认）

**实现逻辑**: 魔塔系统相关道具
**影响页面**: TowerPanel（魔塔面板）
**改动文件**: BagService.java

**测试用例**:

#### 3.14.1 varyname=55 洗点
```sql
-- 使用前
SELECT wash_talent_count FROM war_player WHERE player_id = 102;

-- 使用后
SELECT wash_talent_count FROM war_player WHERE player_id = 102;
```

#### 3.14.2 varyname=57 出战卷
```sql
-- 使用前
SELECT max_take_pet_num, take_pet_limit_time FROM war_player WHERE player_id = 102;

-- 使用后
SELECT max_take_pet_num, take_pet_limit_time FROM war_player WHERE player_id = 102;
```

#### 3.14.3 varyname=58 天赋经验
```sql
-- 使用前
SELECT current_experience FROM war_fighter_talent WHERE fighter_id = 86;

-- 使用后（所有天赋经验应增加）
SELECT current_experience FROM war_fighter_talent WHERE fighter_id = 86;
```

---

## 四、影响页面汇总

| 面板 | 路径 | 涉及的 varyname |
|:----:|:----:|:----------------|
| BattlePanel | 战斗界面 | 1(药水), 3(捕捉) |
| BagPanel | 背包界面 | 2(增益), 12(礼包), 13(特殊), 15(宠物卵), 16(合成), 24(卡片) |
| EquipPanel | 装备界面 | 9(装备), 10/11(精炼), 25/26/27(宝石) |
| PetPanel | 宠物界面 | 5(技能书), 7(进化), 8(合体) |
| ShopPanel | 商店界面 | 所有道具购买 |
| DepotPanel | 仓库界面 | 所有道具存储 |
| PlayerInfoPanel | 玩家信息 | 13(特殊-经验卷等) |
| TowerPanel | 魔塔界面 | 55/57/58(魔塔道具) |
| CardPanel | 卡片界面 | 24(卡片) |
| ComposePanel | 合成界面 | 16(合成), 25(宝石合成) |
| EvolvePanel | 进化界面 | 7(进化) |
| MergePanel | 合体界面 | 8(合体) |

---

## 五、测试数据准备

### 5.1 添加测试道具

```sql
-- 添加药水到背包
INSERT INTO userbag (uid, pid, sums, bsum, psum, zbing, zbpets)
VALUES (102, 1, 10, 0, 0, 0, 0);  -- 治疗药水(小)

-- 添加经验卷
INSERT INTO userbag (uid, pid, sums, bsum, psum, zbing, zbpets)
VALUES (102, 742, 5, 0, 0, 0, 0);  -- 1.5倍经验卷

-- 添加宠物蛋
INSERT INTO userbag (uid, pid, sums, bsum, psum, zbing, zbpets)
VALUES (102, <宠物蛋propId>, 1, 0, 0, 0, 0);

-- 添加进化材料
INSERT INTO userbag (uid, pid, sums, bsum, psum, zbing, zbpets)
VALUES (102, <材料propId>, 5, 0, 0, 0, 0);
```

### 5.2 修改测试数据

```sql
-- 提升宠物等级（用于测试进化）
UPDATE userbb SET level = 50 WHERE id = 86;

-- 增加金币
UPDATE player SET money = 999999 WHERE id = 102;

-- 增加元宝
UPDATE player SET yb = 99999 WHERE id = 102;

-- 扩展背包
UPDATE player SET maxbag = 200 WHERE id = 102;
```

### 5.3 重置测试数据

```sql
-- 清空背包
DELETE FROM userbag WHERE uid = 102;

-- 重置宠物属性
UPDATE userbb SET level = 1, nowexp = 0, czl = 5.0 WHERE id = 86;

-- 重置玩家数据
UPDATE player SET money = 1000, yb = 0, maxbag = 30 WHERE id = 102;
```

---

## 六、测试脚本

### 6.1 自动化测试脚本

```bash
#!/bin/bash
# test-items.sh

BASE_URL="http://localhost:8080"
TOKEN=""

# 登录获取Token
login() {
    RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"testuser","password":"test123"}')
    TOKEN=$(echo $RESPONSE | jq -r '.data.token')
    echo "Token: $TOKEN"
}

# 测试使用道具
test_use_item() {
    ITEM_ID=$1
    PET_ID=$2

    echo "Testing item $ITEM_ID..."
    RESPONSE=$(curl -s -X POST "$BASE_URL/api/bag/use/$ITEM_ID" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"petId\": $PET_ID}")
    echo "Response: $RESPONSE"
}

# 执行测试
login
test_use_item 1 86  # 药水
test_use_item 742 86  # 经验卷
```

### 6.2 数据库验证脚本

```bash
#!/bin/bash
# verify-data.sh

MYSQL_CMD="mysql -u kdjl -p'kdjl_pass' kdjl"

# 验证宠物属性
verify_pet_stats() {
    PET_ID=$1
    echo "=== Pet Stats (ID: $PET_ID) ==="
    $MYSQL_CMD -e "SELECT id, name, level, czl, ac, mc, hp, mp FROM userbb WHERE id = $PET_ID;"
}

# 验证背包物品
verify_bag() {
    UID=$1
    echo "=== Bag Items (UID: $UID) ==="
    $MYSQL_CMD -e "SELECT b.id, p.name, b.sums FROM userbag b JOIN props p ON b.pid = p.id WHERE b.uid = $UID;"
}

verify_pet_stats 86
verify_bag 102
```

---

## 七、常见问题排查

### 7.1 道具使用失败

**问题**: 使用道具时返回错误
**排查步骤**:
1. 检查道具是否存在: `SELECT * FROM props WHERE id = <道具ID>;`
2. 检查背包是否有该道具: `SELECT * FROM userbag WHERE uid = 102 AND pid = <道具ID>;`
3. 检查道具数量是否足够: `SELECT sums FROM userbag WHERE id = <背包ID>;`
4. 检查前置条件（等级、五行等）

### 7.2 宠物属性未变化

**问题**: 使用增益道具后属性未变化
**排查步骤**:
1. 检查是否刷新了数据（可能需要重新登录）
2. 检查数据库是否更新: `SELECT * FROM userbb WHERE id = 86;`
3. 检查是否有缓存问题

### 7.3 前端显示异常

**问题**: 道具图标或名称显示错误
**排查步骤**:
1. 检查props表数据: `SELECT id, name, img, varyname FROM props WHERE id = <道具ID>;`
2. 检查前端propsMap是否正确加载
3. 清除浏览器缓存

---

## 八、性能测试

### 8.1 并发测试

```bash
# 使用 Apache Bench 测试并发
ab -n 100 -c 10 -H "Authorization: Bearer $TOKEN" \
   http://localhost:8080/api/bag/use/1
```

### 8.2 响应时间监控

```bash
# 监控API响应时间
curl -w "@curl-format.txt" -o /dev/null -s \
   -X POST "$BASE_URL/api/bag/use/1" \
   -H "Authorization: Bearer $TOKEN"
```

---

## 九、测试报告模板

```markdown
# 测试报告

**测试日期**: YYYY-MM-DD
**测试人员**: XXX
**测试环境**: localhost

## 测试概要

| 功能 | 状态 | 备注 |
|:----:|:----:|------|
| varyname=1 药水 | ✅ | 正常 |
| varyname=2 增益 | ⚠️ | addczl有bug |
| ... | ... | ... |

## 发现问题

1. **问题描述**: ...
   **重现步骤**: ...
   **期望结果**: ...
   **实际结果**: ...

## 测试结论

...
```

---

## 十、持续集成

### 10.1 GitHub Actions 配置

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build and Test
        run: |
          cd kdjl-backend
          mvn install -pl kdjl-common -DskipTests
          mvn test

      - name: Frontend Test
        run: |
          cd kdjl-frontend
          npm install
          npm run test
```

### 10.2 测试覆盖率要求

- 单元测试覆盖率: >= 80%
- 集成测试覆盖所有 varyname 类型
- 每个 PR 必须通过所有测试
