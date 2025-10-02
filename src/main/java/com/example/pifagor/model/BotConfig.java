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

    @Value("${telegram.webhook.url}")
    private String webhookUrl;

    @Bean
    public TelegramBotsApi telegramBotsApi(MathSchoolBot bot) throws Exception {
        // Внутрішній URL (тобто твій локальний сервер)
        String internalUrl = "http://0.0.0.0:8080";

        // Зовнішній URL (той, що видає Render або твій домен)
        String externalUrl = System.getenv("WEBHOOK_URL");

        // Створюємо webhook
        DefaultWebhook defaultWebhook = new DefaultWebhook();
        defaultWebhook.setInternalUrl(internalUrl);

        // TelegramBotsApi з webhook
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class, defaultWebhook);

        // Реєструємо бота з webhook
        botsApi.registerBot(bot, new SetWebhook(externalUrl + "/webhook"));

        return botsApi;
    }
}