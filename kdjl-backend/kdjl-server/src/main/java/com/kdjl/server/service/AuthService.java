package com.kdjl.server.service;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import com.kdjl.server.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserPetRepository userPetRepo;
    private final PetRepository petRepo;
    private final UserBagRepository bagRepo;
    private final PlayerExtRepository extRepo;

    public AuthService(PlayerRepository playerRepository,
                       JwtTokenProvider jwtTokenProvider,
                       UserPetRepository userPetRepo,
                       PetRepository petRepo,
                       UserBagRepository bagRepo,
                       PlayerExtRepository extRepo) {
        this.playerRepository = playerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPetRepo = userPetRepo;
        this.petRepo = petRepo;
        this.bagRepo = bagRepo;
        this.extRepo = extRepo;
    }

    public ApiResponse<Map<String, Object>> login(String username, String password) {
        String md5Hash = md5(password);
        Player player = playerRepository.findByUsernameAndSecret(username, md5Hash)
            .orElse(null);

        if (player == null) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(player.getId().longValue(), player.getUsername());

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
    public ApiResponse<Map<String, Object>> register(String username, String password, String nickname, Integer petChoice) {
        if (username == null || username.trim().isEmpty()) return ApiResponse.error("用户名不能为空");
        if (password == null || password.length() < 4) return ApiResponse.error("密码至少4位");
        if (playerRepository.existsByUsername(username)) return ApiResponse.error("用户名已存在");

        String md5Hash = md5(password);
        Player player = new Player();
        player.setUsername(username);
        player.setPassword(md5Hash);
        player.setNickname(nickname != null ? nickname : username);
        player.setSecret(md5Hash);
        player.setVip(0);
        player.setMoney(500);
        player.setYb(10);
        player.setScore(0);
        player.setPrestige(0);
        player.setMaxBag(30);
        player.setMaxMc(3);
        player.setOpenMap("1");
        player.setInMap(1);
        player.setSex("1");
        player.setRegtime((int)(System.currentTimeMillis() / 1000));
        player = playerRepository.save(player);

        // Auto-create PlayerExt
        PlayerExt ext = extRepo.findById(player.getId()).orElse(null);
        if (ext == null) {
            ext = new PlayerExt();
            ext.setPlayerId(player.getId()); ext.setSj(0); ext.setMerge(0);
            ext.setRequestMerge(0); ext.setRequest(0);
            extRepo.save(ext);
        }

        // Create initial pet (choose from 6 starter pets: bb id 1-6 for 金木水火土波姆)
        Long[] starterPets = {1L, 2L, 3L, 4L, 5L, 6L};
        Long petTplId = (petChoice != null && petChoice >= 1 && petChoice <= 6) ? starterPets[petChoice - 1] : 1L;

        Pet template = petRepo.findById(petTplId).orElse(null);
        UserPet newPet = new UserPet();
        if (template != null) {
            newPet.setName(template.getName());
            newPet.setWx(template.getWx());
            newPet.setAc(template.getAc()); newPet.setMc(template.getMc());
            newPet.setSrchp(template.getHp()); newPet.setHp(template.getHp());
            newPet.setSrcmp(template.getMp()); newPet.setMp(template.getMp());
            newPet.setHits(template.getHits() != null ? template.getHits().longValue() : 100L);
            newPet.setMiss(template.getMiss() != null ? template.getMiss().longValue() : 0L);
            newPet.setSpeed(template.getSpeed() != null ? template.getSpeed().longValue() : 10L);
            newPet.setImgstand(template.getImgstand()); newPet.setCardimg(template.getCardimg());
            newPet.setSkillList(template.getSkillList());
        } else {
            newPet.setName("波姆"); newPet.setWx(1); newPet.setAc(10L); newPet.setMc(10L);
            newPet.setSrchp(100L); newPet.setHp(100L); newPet.setSrcmp(50L); newPet.setMp(50L);
            newPet.setHits(100L); newPet.setMiss(0L); newPet.setSpeed(10L);
        }
        newPet.setPlayerId(player.getId().longValue());
        newPet.setUsername(player.getNickname());
        newPet.setLevel(1);
        newPet.setNowexp(0L); newPet.setLexp(55L);
        newPet.setStime(System.currentTimeMillis() / 1000);
        newPet.setCzl("1");
        newPet = userPetRepo.save(newPet);

        // Set main pet
        player.setMbid(newPet.getId().intValue());
        player.setFightBb(newPet.getId().intValue());
        playerRepository.save(player);

        // Give starter items: healing potion x5 + capture ball x3
        addStarterItem(player.getId(), 1L, 5);  // 治疗药水(小)
        addStarterItem(player.getId(), 4L, 3);  // 魔法药水(小)

        // Login
        String token = jwtTokenProvider.generateToken(player.getId().longValue(), player.getUsername());
        var data = new LinkedHashMap<String, Object>();
        data.put("token", token);
        data.put("uid", player.getId());
        data.put("username", player.getUsername());
        data.put("nickname", player.getNickname());
        data.put("petName", newPet.getName());
        data.put("petId", newPet.getId());
        return ApiResponse.success(data);
    }

    private void addStarterItem(Integer playerId, Long propId, int count) {
        UserBag item = new UserBag();
        item.setPlayerId(playerId.longValue());
        item.setPropId(propId);
        item.setSums(count);
        item.setVary(1);
        item.setZbing(0);
        item.setSell(0);
        item.setPyb(0); item.setPsell(0); item.setPstime(0L);
        item.setBsum(0); item.setPetime(0L); item.setPsum(0);
        item.setStime(System.currentTimeMillis() / 1000);
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
