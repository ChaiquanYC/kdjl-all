# 注册系统文档

## 概述

用户注册流程：填写账号信息 → 选择头像/性别 → 选择初始宠物 → 创建角色 → 分配初始资源。

## PHP 注册流程 (原始实现)

### 入口文件

| 文件 | 用途 |
|------|------|
| `login/reg1.php` | 注册表单页面 (jQuery/Ajax) |
| `login/register.php` | 注册后端处理器 |
| `login/loginCheck.php` | 昵称可用性检查 (AJAX) |
| `passport/reg_check.php` | 用户名/昵称格式校验 |
| `sec/sec_common_fnc.php` | `getCzl()` 成长率生成函数 |
| `socketChat/badWord.txt` | 敏感词库 |

### 前端表单收集字段

| 字段 | 来源 | 校验规则 |
|------|------|----------|
| username | 输入框 | 仅字母数字，不能全为数字 |
| password | 输入框 | 最少4位 |
| nickname | 输入框 | 4-21字符，支持中文/英文/下划线 |
| sex | 头像选择 | 奇数头像=男(1)，偶数头像=女(2) |
| head | 头像选择 | 1-6，对应6个角色形象 |
| bc | 宠物弹窗 | 1-5，对应5只初始宠物 |

### 后端处理步骤 (register.php)

```
1. 维护模式检查 → timeconfig 表
2. 敏感词过滤 → socketChat/badWord.txt
3. 昵称唯一性 → SELECT id FROM player WHERE nickname='{$tu}'
4. 重复角色检查 → 同用户名仅当密码为零值hash时允许重新注册
5. 昵称长度校验 → 4-21字符
6. 创建 player 记录 → name, secret(MD5), nickname, sex, regtime, money=0, yb=0
7. 设置 Session → username, id, LoginApiState=1
8. 映射初始宠物 → bc: 1→bb#1, 2→bb#13, 3→bb#23, 4→bb#32, 5→bb#42
9. 读取宠物模板 → SELECT * FROM bb WHERE id={$tbc}
10. 随机成长率 → getCzl($bb['czl']) 从范围字符串随机取值
11. 创建 userbb 记录 → 复制模板属性 + 随机czl + 头像图片(t{id}.gif, k{id}.gif, q{id}.gif)
12. 分配技能 → 取 skillist 第一个技能，查 skillsys 表，插入 skill 表
13. 设为主宠 → UPDATE player SET mbid = pet_id
14. 奖励道具 → registertype=='prize' 时添加道具2047到 userbag
15. 创建 lock 记录
16. 系统欢迎消息 → memcached
```

### getCzl() 成长率生成

```php
function getCzl($czl) {
    $ok = str_replace(".", "", $czl);   // "1.5,2.5" → "15,25"
    $arr = split(",", $ok);             // → ["15","25"]
    $num = rand($arr[0], $arr[1]);      // → 随机 15~25
    return $num/10;                     // → 1.5~2.5
}
```

### 初始宠物映射 (PHP)

| 宠物选择 | bb ID | 五行 |
|----------|-------|------|
| 1 | 1 | 金 |
| 2 | 13 | 木 |
| 3 | 23 | 水 |
| 4 | 32 | 火 |
| 5 | 42 | 土 |

### 涉及数据库表

| 表 | 操作 |
|----|------|
| `player` | INSERT (创建角色), UPDATE (设置mbid) |
| `userbb` | INSERT (创建初始宠物) |
| `bb` | SELECT (读取宠物模板) |
| `skillsys` | SELECT (读取技能定义) |
| `skill` | INSERT (分配初始技能) |
| `userbag` | INSERT (奖励道具，条件性) |
| `lock` | INSERT (创建锁定记录) |
| `timeconfig` | SELECT (维护模式检查) |

---

## Java 注册流程 (当前实现)

### 文件

| 文件 | 用途 |
|------|------|
| `AuthController.java` | POST /api/auth/register 端点 |
| `AuthService.java` | register() 方法 (62-145行) |

### 请求参数

```java
record RegisterRequest(String username, String password, String nickname, Integer petChoice) {}
```

### 处理步骤

```
1. 校验 → username非空, password>=4位, username不重复
2. 创建 Player → name, secret(MD5), nickname, sex="1", money=500, yb=10
3. 创建 PlayerExt → sj=0, merge=0
4. 初始宠物 → petChoice 1-6 映射 bb ID 1-6 (全部金系)
5. czl 硬编码为 "1" (未随机)
6. 创建 UserPet → 仅复制部分属性
7. 初始道具 → 回复药水x5, 魔法药水x3
8. 返回 JWT token + uid + username + nickname + petName + petId
```

### 初始宠物映射 (Java)

| petChoice | bb ID | 宠物名 |
|-----------|-------|--------|
| 1 | 1 | 金波姆 |
| 2 | 2 | 波光姆 |
| 3 | 3 | 金波姆王 |
| 4 | 4 | 黄金鸟 |
| 5 | 5 | 金光鼠 |
| 6 | 6 | 雷光鼠 |

---

## 前端注册 (当前实现)

### 文件

| 文件 | 用途 |
|------|------|
| `pages/LoginPage.tsx` | 登录/注册表单 (共用组件) |
| `pages/LoginPage.module.css` | 样式 |
| `stores/authStore.ts` | setAuth() 注册后登录 |
| `api/client.ts` | apiPost() API调用 |
| `types/index.ts` | Player 类型定义 |

### 表单字段

- username 输入框 (仅 required)
- password 输入框 (仅 required)
- nickname 输入框 (可选，默认=username)
- 宠物选择 (3列网格，6只宠物，文字显示)

### 缺失功能

- 无头像/性别选择
- 无客户端校验 (正则、长度等)
- 无昵称可用性实时检查
- 注册后 Player 对象由前端硬编码构造，非服务端返回
- 无敏感词过滤
