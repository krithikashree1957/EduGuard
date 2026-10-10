package com.eduguard.eduguard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final String riskServiceBaseUrl;

    public PageController(
            @Value("${eduguard.risk-service.base-url:http://localhost:8081}") String riskServiceBaseUrl) {
        this.riskServiceBaseUrl = riskServiceBaseUrl;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("riskServiceBaseUrl", riskServiceBaseUrl);
        return "dashboard";
    }

    @GetMapping("/students")
    public String students(Model model) {
        model.addAttribute("riskServiceBaseUrl", riskServiceBaseUrl);
        return "student";
    }
}