package com.kdjl.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard"; }

    @GetMapping("/players")
    public String players() { return "player/list"; }

    @GetMapping("/players/{id}")
    public String playerDetail() { return "player/detail"; }

    @GetMapping("/props")
    public String props() { return "props/list"; }

    @GetMapping("/pets")
    public String pets() { return "pets/list"; }

    @GetMapping("/payments")
    public String payments() { return "payments"; }

    @GetMapping("/tasks")
    public String tasks() { return "tasks/list"; }

    @GetMapping("/initial-bag")
    public String initialBag() { return "initial-bag/list"; }

    @GetMapping("/logs")
    public String logs() { return "logs/list"; }

    @GetMapping("/auction")
    public String auction() { return "auction/list"; }

    @GetMapping("/online-rewards")
    public String onlineRewards() { return "online-rewards/list"; }
}
