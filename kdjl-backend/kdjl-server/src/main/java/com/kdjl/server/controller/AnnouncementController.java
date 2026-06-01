package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.Announcement;
import com.kdjl.server.repository.AnnouncementRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepo;

    public AnnouncementController(AnnouncementRepository announcementRepo) {
        this.announcementRepo = announcementRepo;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> list = announcementRepo.findActive(now).stream()
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("msg", a.getMsg());
                if (a.getHref() != null && !a.getHref().isBlank()) {
                    m.put("href", a.getHref());
                }
                return m;
            })
            .collect(Collectors.toList());
        return ApiResponse.success(list);
    }
}
