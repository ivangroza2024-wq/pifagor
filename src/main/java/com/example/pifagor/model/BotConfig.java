package com.example.pifagor.model;

import com.example.pifagor.bot.MathSchoolBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultWebhook;

@Configuration
public class BotConfig {

    @Value("${telegram.bot.webhookUrl}")
    private String webhookUrl;

    @Bean
    public TelegramBotsApi telegramBotsApi(MathSchoolBot bot) throws Exception {
        DefaultWebhook webhook = new DefaultWebhook();

        SetWebhook setWebhook = SetWebhook.builder()
                .url(webhookUrl)
                .build();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class, webhook);
        botsApi.registerBot(bot, setWebhook);

        return botsApi;
    }
}
