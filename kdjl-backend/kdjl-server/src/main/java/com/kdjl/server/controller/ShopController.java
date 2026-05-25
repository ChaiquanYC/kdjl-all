package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.ShopService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listItems(@RequestParam(defaultValue = "props") String type) {
        return ApiResponse.success(shopService.listShopItems(type));
    }

    @PostMapping("/buy/{propsId}")
    public ApiResponse<Map<String, Object>> buyItem(
            Authentication auth,
            @PathVariable Long propsId,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int count = body.get("count") != null
            ? Integer.parseInt(body.get("count").toString()) : 1;
        String currency = body.get("currency") != null
            ? body.get("currency").toString() : "money";
        return ApiResponse.success(shopService.buyItem(uid, propsId, count, currency));
    }
}
