package com.example.pifagor.model;

import com.example.pifagor.bot.MathSchoolBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Value("${telegram.bot.webhook.path}")
    private String webhookPath;

    @Value("${WEBHOOK_URL}")  // Змінна Render, https://your-app.onrender.com
    private String externalUrl;

    @Bean
    public TelegramBotsApi telegramBotsApi(MathSchoolBot bot) throws Exception {
        // TelegramBotsApi для Webhook
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Склеюємо externalUrl + webhookPath
        SetWebhook setWebhook = SetWebhook.builder()
                .url(externalUrl + webhookPath)
                .build();

        botsApi.registerBot(bot, setWebhook);

        System.out.println("Webhook встановлено: " + externalUrl + webhookPath);
        return botsApi;
    }
}
