package com.example.pifagor.controller;

import com.example.pifagor.bot.MathSchoolBot;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    private final MathSchoolBot bot;

    public WebhookController(MathSchoolBot bot) {
        this.bot = bot;
    }

    @PostMapping("${telegram.webhook.path}")
    public BotApiMethod<?> onUpdate(@RequestBody Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
