package com.spn.viger_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {
    @GetMapping("/")
    public String index() {
        return "index";
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

    @GetMapping("/giao-duc-tai-duc/tong-quan")
    public String overview() {
        return "overview";
    }

    @GetMapping("/giao-duc-tai-duc/he-thong-giao-duc")
    public String educationSystem() {
        return "education-system";
    }

    @GetMapping("/giao-duc-tai-duc/nghien-cuu-va-ung-dung")
    public String researchApplied() {
        return "research-applied";
    }

    @GetMapping("/giao-duc-tai-duc/chi-phi-va-co-hoi")
    public String costsOpportunities() {
        return "costs-opportunities";
    }

    @GetMapping("/giao-duc-tai-duc/co-hoi-nghe-nghiep")
    public String careerOpportunities() {
        return "career-opportunities";
    }

    @GetMapping("/tu-van")
    public String consult() {
        return "consult";
    }

    @GetMapping("/tu-van/du-hoc-dai-hoc")
    public String consultBachelor() {
        return "consult-bachelor";
    }

    @GetMapping("/tu-van/du-hoc-cao-hoc")
    public String consultMaster() {
        return "consult-master";
    }

    @GetMapping("/tu-van/quy-trinh-tu-van")
    public String consultProcess() {
        return "consult-process";
    }

    @GetMapping("/chuong-trinh-hoc")
    public String programs() {
        return "programs";
    }
}