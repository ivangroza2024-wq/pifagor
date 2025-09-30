package com.example.pifagor.model;

import com.example.pifagor.bot.MathSchoolBot;
import com.example.pifagor.repository.GroupRepository;
import com.example.pifagor.repository.UserRepository;
import com.example.pifagor.repository.FacultyRepository;
import com.example.pifagor.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;



    @Configuration
    public class BotConfig {

        private final MathSchoolBot bot;

        public BotConfig(MathSchoolBot bot) {
            this.bot = bot;
        }

        @Bean
        public void registerBot() throws Exception {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        }
    }

