package com.example.pifagor.model;

import com.example.pifagor.bot.MathSchoolBot;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;

@Configuration
public class BotConfig {

    private final MathSchoolBot testBot; // або твій MathSchoolBot

    public BotConfig(MathSchoolBot testBot) {
        this.testBot = testBot;
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(testBot);
            System.out.println("Bot registered!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

