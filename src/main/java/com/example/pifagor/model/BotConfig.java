package com.example.pifagor.model;

import com.example.pifagor.bot.MathSchoolBot;
import com.example.pifagor.repository.GroupRepository;
import com.example.pifagor.repository.UserRepository;
import com.example.pifagor.repository.FacultyRepository;
import com.example.pifagor.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultWebhook;

@Configuration
public class BotConfig {
    @Value("${telegram.bot.webhookPath}")
    private String webhookPath;

    @Value("${WEBHOOK_URL}")  // <-- беремо з Environment Render-а
    private String externalUrl;

    @Bean
    public TelegramBotsApi telegramBotsApi(MathSchoolBot bot) throws Exception {
        DefaultWebhook defaultWebhook = new DefaultWebhook();
        defaultWebhook.setInternalUrl("http://0.0.0.0:8080");

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class, defaultWebhook);

        // Склеюємо WEBHOOK_URL + webhookPath
        SetWebhook setWebhook = SetWebhook.builder()
                .url(externalUrl + webhookPath)
                .build();

        botsApi.registerBot(bot, setWebhook);

        return botsApi;
    }

}
