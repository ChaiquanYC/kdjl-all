package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/gm")
public class GmController {

    private final PlayerRepository playerRepo;
    private final UserBagRepository bagRepo;
    private final UserPetRepository petRepo;
    private final PropsRepository propsRepo;

    public GmController(PlayerRepository p, UserBagRepository b, UserPetRepository pp, PropsRepository pr) {
        this.playerRepo = p; this.bagRepo = b; this.petRepo = pp; this.propsRepo = pr;
    }

    /** Search players by name */
    @GetMapping("/player/search")
    public ApiResponse<List<Map<String, Object>>> searchPlayer(@RequestParam String name) {
        List<Player> players = playerRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Player p : players) {
            if (p.getNickname() != null && p.getNickname().contains(name)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId()); m.put("username", p.getUsername());
                m.put("nickname", p.getNickname()); m.put("money", p.getMoney());
                m.put("yb", p.getYb()); m.put("vip", p.getVip());
                result.add(m);
            }
        }
        return ApiResponse.success(result);
    }

    /** Give item to player */
    public record GiveItemRequest(Integer playerId, Long propId, int count) {}
    @PostMapping("/give-item")
    @Transactional
    public ApiResponse<Map<String, Object>> giveItem(@RequestBody GiveItemRequest req) {
        Player player = playerRepo.findById(req.playerId()).orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        Props props = propsRepo.findById(req.propId()).orElseThrow(() -> new IllegalArgumentException("道具不存在"));

        UserBag existing = bagRepo.findByPlayerId(req.playerId().longValue()).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(req.propId())).findFirst().orElse(null);
        if (existing != null) {
            existing.setSums((existing.getSums() != null ? existing.getSums() : 0) + req.count());
            bagRepo.save(existing);
        } else {
            UserBag item = new UserBag();
            item.setPlayerId(req.playerId().longValue()); item.setPropId(req.propId()); item.setSums(req.count());
            item.setVary(props.getVary()); item.setSell(props.getSell()); item.setZbing(0);
            item.setPyb(0); item.setPsell(0); item.setPstime(0L); item.setBsum(0);
            item.setPetime(0L); item.setPsum(0); item.setStime(System.currentTimeMillis()/1000);
            bagRepo.save(item);
        }
        return ApiResponse.success(Map.of("given", props.getName() + " x" + req.count(), "to", player.getNickname()));
    }

    /** Give money/yb to player */
    public record GiveMoneyRequest(Integer playerId, int money, int yb) {}
    @PostMapping("/give-money")
    @Transactional
    public ApiResponse<Map<String, Object>> giveMoney(@RequestBody GiveMoneyRequest req) {
        Player player = playerRepo.findById(req.playerId()).orElseThrow();
        if (req.money() > 0) player.setMoney((player.getMoney()!=null?player.getMoney():0) + req.money());
        if (req.yb() > 0) player.setYb((player.getYb()!=null?player.getYb():0) + req.yb());
        playerRepo.save(player);
        return ApiResponse.success(Map.of("done", true));
    }

    /** Set pet level */
    @PostMapping("/set-pet-level/{petId}")
    @Transactional
    public ApiResponse<Map<String, Object>> setPetLevel(@PathVariable Long petId, @RequestParam int level) {
        UserPet pet = petRepo.findById(petId).orElseThrow();
        pet.setLevel(Math.max(1, Math.min(100, level)));
        petRepo.save(pet);
        return ApiResponse.success(Map.of("pet", pet.getName(), "level", pet.getLevel()));
    }
}
