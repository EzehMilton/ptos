package com.ptos.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String index() {
        log.info("Redirecting to /pt/dashboard");
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        log.info("Redirecting to /pt/dashboard");
        return "auth/login";
    }
}
