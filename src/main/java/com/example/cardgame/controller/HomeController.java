package com.example.cardgame.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * ルート (/) アクセスを login.html へ自動転送
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }
}