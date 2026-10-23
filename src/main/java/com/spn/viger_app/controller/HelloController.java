package com.spn.viger_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.web.csrf.CsrfToken;

@Controller
public class HelloController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(CsrfToken csrfToken) {
        csrfToken.getToken(); 
        return "login";
    }

    @GetMapping("/ve-chung-toi")
    public String about() {
        return "about";
    }

    @GetMapping("/lien-he")
    public String contact() {
        return "contact";
    }

    @GetMapping("/giao-duc-tai-duc")
    public String education() {
        return "education";
    }

    @GetMapping("/tu-van")
    public String consult() {
        return "consult";
    }

    @GetMapping("/tu-van/quy-trinh-tu-van")
    public String consultProcess() {
        return "consult/consult-process";
    }
}