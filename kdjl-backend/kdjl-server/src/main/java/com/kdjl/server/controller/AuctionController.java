package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.AuctionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {
    private final AuctionService auctionService;
    public AuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "gold") String type,
            @RequestParam(required = false) Integer varyname) {
        return ApiResponse.success(auctionService.listAuctions(type, varyname));
    }

    @GetMapping("/bag-for-sell")
    public ApiResponse<List<Map<String, Object>>> bagForSell(
            Authentication auth, @RequestParam(required = false) Integer varyname) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.getPlayerBagForSell(uid, varyname));
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> my(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.myAuctions(uid));
    }

    public record ListRequest(Long bagId, int price, String type, int quantity) {
        public int quantity() { return quantity > 0 ? quantity : 1; }
    }
    @PostMapping("/sell")
    public ApiResponse<Map<String, Object>> sell(@RequestBody ListRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        String type = req.type() != null ? req.type() : "gold";
        return ApiResponse.success(auctionService.listForAuction(uid, req.bagId(), req.price(), type, req.quantity()));
    }

    public record BuyRequest(String type, int quantity) {
        public int quantity() { return quantity > 0 ? quantity : 1; }
    }
    @PostMapping("/buy/{bagId}")
    public ApiResponse<Map<String, Object>> buy(@PathVariable Long bagId, @RequestBody(required = false) BuyRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        String type = req != null && req.type() != null ? req.type() : "gold";
        int qty = req != null ? req.quantity() : 1;
        return ApiResponse.success(auctionService.buyAuction(uid, bagId, type, qty));
    }

    @PostMapping("/cancel/{bagId}")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long bagId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.cancelAuction(uid, bagId));
    }

    @PostMapping("/renew/{bagId}")
    public ApiResponse<Map<String, Object>> renew(@PathVariable Long bagId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.renewAuction(uid, bagId));
    }

    @PostMapping("/withdraw")
    public ApiResponse<Map<String, Object>> withdraw(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.withdrawMoney(uid));
    }
}
