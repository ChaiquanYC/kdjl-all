package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.Player;
import com.kdjl.server.repository.PlayerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final PlayerRepository playerRepo;

    public FriendController(PlayerRepository playerRepo) {
        this.playerRepo = playerRepo;
    }

    /** List friends with online status */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listFriends(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        String friendList = player.getFriendList();
        if (friendList == null || friendList.isEmpty()) return ApiResponse.success(List.of());

        List<Map<String, Object>> friends = new ArrayList<>();
        int fiveMinAgo = (int)(System.currentTimeMillis() / 1000) - 300;
        String[] ids = friendList.split(",");
        for (String idStr : ids) {
            try {
                int fid = Integer.parseInt(idStr.trim());
                Player f = playerRepo.findById(fid).orElse(null);
                if (f != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("nickname", f.getNickname());
                    m.put("level", f.getScore() != null ? f.getScore() : 0);
                    m.put("online", f.getLastVisitTime() != null && f.getLastVisitTime() > fiveMinAgo);
                    friends.add(m);
                }
            } catch (NumberFormatException ignored) {}
        }
        return ApiResponse.success(friends);
    }

    /** Add a friend */
    @Transactional
    @PostMapping("/add/{playerId}")
    public ApiResponse<Map<String, Object>> addFriend(@PathVariable Long playerId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        if (uid.longValue() == playerId) return ApiResponse.error("不能添加自己为好友");

        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        Player target = playerRepo.findById(playerId.intValue()).orElse(null);
        if (target == null) return ApiResponse.error("目标玩家不存在");

        String friendList = player.getFriendList();
        if (friendList != null && !friendList.isEmpty()) {
            Set<String> existing = new HashSet<>(Arrays.asList(friendList.split(",")));
            if (existing.contains(String.valueOf(playerId))) return ApiResponse.error("已经是好友了");
            if (existing.size() >= 50) return ApiResponse.error("好友已满(50人)");
            player.setFriendList(friendList + "," + playerId);
        } else {
            player.setFriendList(String.valueOf(playerId));
        }
        playerRepo.save(player);
        return ApiResponse.success(Map.of("added", true, "nickname", target.getNickname()));
    }

    /** Remove a friend */
    @Transactional
    @PostMapping("/remove/{playerId}")
    public ApiResponse<Map<String, Object>> removeFriend(@PathVariable Long playerId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        String friendList = player.getFriendList();
        if (friendList == null || friendList.isEmpty()) return ApiResponse.error("好友列表为空");

        List<String> friends = new ArrayList<>(Arrays.asList(friendList.split(",")));
        friends.removeIf(s -> s.trim().equals(String.valueOf(playerId)));
        player.setFriendList(String.join(",", friends));
        playerRepo.save(player);
        return ApiResponse.success(Map.of("removed", true));
    }
}
