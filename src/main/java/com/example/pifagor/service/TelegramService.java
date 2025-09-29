package com.example.pifagor.service;

import com.example.pifagor.bot.MathSchoolBot;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class TelegramService {

    private final MathSchoolBot bot;

    public TelegramService(@Lazy MathSchoolBot bot) {
        this.bot = bot;
    }

    public InputStream downloadFile(String fileId) throws Exception {
        org.telegram.telegrambots.meta.api.methods.GetFile getFileMethod =
                new org.telegram.telegrambots.meta.api.methods.GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File file = bot.execute(getFileMethod);
        return new java.net.URL(file.getFileUrl(bot.getBotToken())).openStream();
    }
}

