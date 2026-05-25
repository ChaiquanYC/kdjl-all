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

    /** List auctions: type=gold/sj/yb, varyname=category filter */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "gold") String type,
            @RequestParam(required = false) Integer varyname) {
        return ApiResponse.success(auctionService.listAuctions(type, varyname));
    }

    /** Get player bag for selling (with optional category filter) */
    @GetMapping("/bag-for-sell")
    public ApiResponse<List<Map<String, Object>>> bagForSell(
            Authentication auth, @RequestParam(required = false) Integer varyname) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.getPlayerBagForSell(uid, varyname));
    }

    /** My auctions */
    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> my(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.myAuctions(uid));
    }

    public record ListRequest(Long bagId, int price, String type) {}
    @PostMapping("/sell")
    public ApiResponse<Map<String, Object>> sell(@RequestBody ListRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        String type = req.type() != null ? req.type() : "gold";
        return ApiResponse.success(auctionService.listForAuction(uid, req.bagId(), req.price(), type));
    }

    public record BuyRequest(String type) {}
    @PostMapping("/buy/{bagId}")
    public ApiResponse<Map<String, Object>> buy(@PathVariable Long bagId, @RequestBody(required = false) BuyRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        String type = req != null && req.type() != null ? req.type() : "gold";
        return ApiResponse.success(auctionService.buyAuction(uid, bagId, type));
    }

    @PostMapping("/cancel/{bagId}")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long bagId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(auctionService.cancelAuction(uid, bagId));
    }
}
