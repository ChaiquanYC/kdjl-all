package com.kdjl.server.service;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import com.kdjl.server.security.JwtTokenProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserPetRepository userPetRepo;
    private final PetRepository petRepo;
    private final UserBagRepository bagRepo;
    private final PlayerExtRepository extRepo;
    private final LevelUpService levelUpService;
    private final SkillSysRepository skillSysRepo;
    private final SkillRepository skillRepo;
    private final OnlineTimeService onlineTimeService;
    private final InitialBagConfigRepository initialBagConfigRepo;
    private final PropsRepository propsRepo;
    private final PlayerLogService playerLogService;
    private Set<String> badWords;

    public AuthService(PlayerRepository playerRepository,
                       JwtTokenProvider jwtTokenProvider,
                       UserPetRepository userPetRepo,
                       PetRepository petRepo,
                       UserBagRepository bagRepo,
                       PlayerExtRepository extRepo,
                       LevelUpService levelUpService,
                       SkillSysRepository skillSysRepo,
                       SkillRepository skillRepo,
                       OnlineTimeService onlineTimeService,
                       InitialBagConfigRepository initialBagConfigRepo,
                       PropsRepository propsRepo,
                       PlayerLogService playerLogService) {
        this.playerRepository = playerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPetRepo = userPetRepo;
        this.petRepo = petRepo;
        this.bagRepo = bagRepo;
        this.extRepo = extRepo;
        this.levelUpService = levelUpService;
        this.skillSysRepo = skillSysRepo;
        this.skillRepo = skillRepo;
        this.onlineTimeService = onlineTimeService;
        this.initialBagConfigRepo = initialBagConfigRepo;
        this.propsRepo = propsRepo;
        this.playerLogService = playerLogService;
        loadBadWords();
    }

    private void loadBadWords() {
        badWords = new HashSet<>();
        try {
            var resource = new ClassPathResource("badword.txt");
            try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty()) badWords.add(word);
                }
            }
        } catch (Exception e) {
            // badword.txt not found — skip filtering
        }
    }

    private boolean containsBadWord(String text) {
        if (text == null || badWords.isEmpty()) return false;
        for (String word : badWords) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    public ApiResponse<Void> checkNickname(String nickname) {
        if (nickname == null || nickname.trim().length() < 4 || nickname.trim().length() > 21) {
            return ApiResponse.error("角色名长度需为4-21字符");
        }
        if (containsBadWord(nickname)) {
            return ApiResponse.error("角色名包含禁止使用的词");
        }
        if (playerRepository.existsByNickname(nickname)) {
            return ApiResponse.error("角色名已存在");
        }
        return ApiResponse.success(null);
    }

    public ApiResponse<Map<String, Object>> login(String username, String password) {
        String md5Hash = md5(password);
        Player player = playerRepository.findByUsernameAndSecret(username, md5Hash)
            .orElse(null);

        if (player == null) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(player.getId().longValue(), player.getUsername());
        onlineTimeService.onLogin(player.getId());

        // --- 记录登录日志 ---
        playerLogService.log(player.getId(), player.getNickname(), "LOGIN", "用户登录");

        var data = new LinkedHashMap<String, Object>();
        data.put("token", token);
        data.put("uid", player.getId());
        data.put("username", player.getUsername());
        data.put("nickname", player.getNickname());
        data.put("money", player.getMoney() != null ? player.getMoney() : 0);
        data.put("yb", player.getYb() != null ? player.getYb() : 0);
        data.put("vip", player.getVip() != null ? player.getVip() : 0);
        return ApiResponse.success(data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> register(String username, String password, String nickname, Integer petChoice, String sex, Integer headImg) {
        // --- 校验 ---
        if (username == null || username.trim().isEmpty()) return ApiResponse.error("用户名不能为空");
        if (!username.matches("^[A-Za-z0-9]+$")) return ApiResponse.error("用户名仅允许字母和数字");
        if (username.matches("^[0-9]+$")) return ApiResponse.error("用户名不能全为数字");
        if (password == null || password.length() < 4) return ApiResponse.error("密码至少4位");

        if (nickname == null || nickname.isEmpty()) nickname = username;
        nickname = nickname.trim();
        if (nickname.length() < 4 || nickname.length() > 21) return ApiResponse.error("角色名长度需为4-21字符");
        if (containsBadWord(username) || containsBadWord(nickname)) return ApiResponse.error("名称中包含禁止使用的词");
        if (playerRepository.existsByUsername(username)) return ApiResponse.error("用户名已存在");
        if (playerRepository.existsByNickname(nickname)) return ApiResponse.error("角色名已存在");

        // --- 创建 Player ---
        String md5Hash = md5(password);
        Player player = new Player();
        player.setUsername(username);
        player.setPassword(md5Hash);
        player.setSecret(md5Hash);
        player.setNickname(nickname);
        player.setSex(sex != null ? sex : "帅哥");
        player.setHeadImg(headImg != null ? headImg : Integer.valueOf(1));
        player.setVip(0);
        player.setMoney(0);
        player.setYb(0);
        player.setScore(0);
        player.setPrestige(0);
        player.setMaxBag(30);
        player.setMaxMc(3);
        player.setOpenMap("1");
        player.setInMap(1);
        player.setRegtime((int)(System.currentTimeMillis() / 1000));
        player = playerRepository.save(player);

        // --- 创建 PlayerExt ---
        PlayerExt ext = extRepo.findById(player.getId()).orElse(null);
        if (ext == null) {
            ext = new PlayerExt();
            ext.setPlayerId(player.getId());
            ext.setSj(0);
            ext.setPaisj(0);
            ext.setPaiyb(0);
            ext.setMerge(0);
            ext.setRequestMerge(0);
            ext.setRequest(0);
            extRepo.save(ext);
        }

        // --- 初始宠物 (PHP: 1→bb#1, 2→bb#13, 3→bb#23, 4→bb#32, 5→bb#42) ---
        Long[] starterPets = {1L, 13L, 23L, 32L, 42L};
        Long petTplId = (petChoice != null && petChoice >= 1 && petChoice <= 5) ? starterPets[petChoice - 1] : 1L;

        Pet template = petRepo.findById(petTplId).orElse(null);
        UserPet newPet = new UserPet();
        if (template != null) {
            newPet.setName(template.getName());
            newPet.setWx(template.getWx());
            newPet.setAc(template.getAc());
            newPet.setMc(template.getMc());
            newPet.setSrchp(template.getHp());
            newPet.setHp(template.getHp());
            newPet.setSrcmp(template.getMp());
            newPet.setMp(template.getMp());
            newPet.setHits(template.getHits() != null ? template.getHits().longValue() : 100L);
            newPet.setMiss(template.getMiss() != null ? template.getMiss().longValue() : 0L);
            newPet.setSpeed(template.getSpeed() != null ? template.getSpeed().longValue() : 10L);
            newPet.setKx(template.getKx());
            newPet.setImgstand(template.getImgstand());
            newPet.setImgack(template.getImgack());
            newPet.setImgdie(template.getImgdie());
            newPet.setSkillList(template.getSkillList());
            // 图片按 PHP 逻辑: t{id}.gif, k{id}.gif, q{id}.gif
            newPet.setHeadimg("t" + petTplId + ".gif");
            newPet.setCardimg("k" + petTplId + ".gif");
            newPet.setEffectimg("q" + petTplId + ".gif");
            // czl 随机生成 (对齐 PHP getCzl)
            double czl = levelUpService.generateCzl(template.getCzl());
            newPet.setCzl(String.valueOf(czl));
        } else {
            newPet.setName("波姆"); newPet.setWx(1); newPet.setAc(10L); newPet.setMc(10L);
            newPet.setSrchp(100L); newPet.setHp(100L); newPet.setSrcmp(50L); newPet.setMp(50L);
            newPet.setHits(100L); newPet.setMiss(0L); newPet.setSpeed(10L);
            newPet.setCzl("1.0");
        }
        newPet.setPlayerId(player.getId().longValue());
        newPet.setUsername(player.getNickname());
        newPet.setLevel(1);
        newPet.setNowexp(0L);
        newPet.setLexp(55L);
        newPet.setStime(System.currentTimeMillis() / 1000);
        newPet = userPetRepo.save(newPet);

        // --- 初始技能 (PHP: 取 skillist 第一个技能) ---
        assignInitialSkill(template, newPet);

        // --- 初始背包物品 (从配置表读取) ---
        assignInitialBag(player.getId());

        // --- 设为主宠 ---
        player.setMbid(newPet.getId().intValue());
        player.setFightBb(newPet.getId().intValue());
        playerRepository.save(player);

        // --- 记录注册日志 ---
        playerLogService.log(player.getId(), player.getNickname(), "REGISTER", "pet", newPet.getId().longValue(), newPet.getName(), "新用户注册");

        // --- 返回 ---
        String token = jwtTokenProvider.generateToken(player.getId().longValue(), player.getUsername());
        onlineTimeService.onLogin(player.getId());
        var data = new LinkedHashMap<String, Object>();
        data.put("token", token);
        data.put("uid", player.getId());
        data.put("username", player.getUsername());
        data.put("nickname", player.getNickname());
        data.put("petName", newPet.getName());
        data.put("petId", newPet.getId());
        return ApiResponse.success(data);
    }

    private void assignInitialSkill(Pet template, UserPet pet) {
        if (template == null || template.getSkillList() == null || template.getSkillList().isEmpty()) return;
        String firstSkill = template.getSkillList().split(",")[0];
        String[] parts = firstSkill.split(":");
        if (parts.length < 1) return;
        try {
            Long skillId = Long.parseLong(parts[0]);
            int skillLevel = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            // Skip if this pet already has this skill (prevent duplicates)
            boolean exists = skillRepo.findByPetId(pet.getId()).stream()
                .anyMatch(s -> s.getSkillDefId() != null && s.getSkillDefId().equals(skillId));
            if (exists) return;
            SkillSys sys = skillSysRepo.findById(skillId).orElse(null);
            if (sys == null) return;
            Skill skill = new Skill();
            skill.setPetId(pet.getId());
            skill.setSkillDefId(skillId);
            skill.setName(sys.getName());
            skill.setLevel(skillLevel);
            skill.setVary(sys.getVary());
            skill.setWx(sys.getWx());
            skill.setValue(splitFirst(sys.getAckvalue()));
            skill.setPlus(splitFirst(sys.getPlus()));
            skill.setImg(sys.getImg());
            skill.setUhp(parseFirstInt(sys.getUhp()));
            skill.setUmp(parseFirstInt(sys.getUmp()));
            skillRepo.save(skill);
        } catch (NumberFormatException ignored) {}
    }

    private void assignInitialBag(Integer playerId) {
        List<InitialBagConfig> configs = initialBagConfigRepo.findByEnabledOrderBySortOrderAsc(1);
        if (configs.isEmpty()) return;

        long now = System.currentTimeMillis() / 1000;
        for (InitialBagConfig config : configs) {
            Props props = propsRepo.findById(config.getPropId()).orElse(null);
            if (props == null) continue;

            UserBag item = new UserBag();
            item.setPlayerId(playerId.longValue());
            item.setPropId(config.getPropId());
            item.setSums(config.getCount());
            item.setVary(props.getVary() != null ? props.getVary() : 1);
            item.setSell(props.getSell());
            item.setZbing(0);
            item.setPyb(0);
            item.setPsell(0);
            item.setPstime(0L);
            item.setBsum(0);
            item.setPetime(0L);
            item.setPsum(0);
            item.setStime(now);
            bagRepo.save(item);
        }
    }

    private String splitFirst(String csv) {
        if (csv == null || csv.isEmpty()) return "0";
        return csv.split(",")[0];
    }

    private Integer parseFirstInt(String csv) {
        try { return Integer.parseInt(splitFirst(csv)); } catch (NumberFormatException e) { return 0; }
    }

    private void addStarterItems(int playerId) {
        long now = System.currentTimeMillis() / 1000;
        // 治疗药水(小) x5
        addItem(playerId, 1L, 1, 5, now);
        // 魔法药水(小) x5
        addItem(playerId, 4L, 1, 5, now);
        // 金波姆·精灵球 x3
        addItem(playerId, 149L, 1, 3, now);
    }

    private void addItem(int playerId, Long propId, int vary, int sums, long stime) {
        UserBag item = new UserBag();
        item.setPlayerId((long) playerId);
        item.setPropId(propId);
        item.setVary(vary);
        item.setSums(sums);
        item.setSell(1);
        item.setStime(stime);
        item.setCantrade(0);
        bagRepo.save(item);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
