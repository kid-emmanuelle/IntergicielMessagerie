package org.insa.intergicielmessagerie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Home page redirect to chat page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/chat";
    }

    /**
     * Chat page
     */
    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    /**
     * Login page
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
