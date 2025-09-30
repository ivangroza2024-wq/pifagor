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

    private final UserService userService;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final TelegramService telegramService;
    private final GoogleDriveService driveService;
    private final GroupRepository groupRepository;
    private final RegistrationRequestService registrationRequestService;
    private final GoogleSheetsService sheetsService;

    public BotConfig(UserService userService,
                     UserRepository userRepository,
                     FacultyRepository facultyRepository,
                     TelegramService telegramService,
                     GoogleDriveService driveService,
                     GroupRepository groupRepository,
                     RegistrationRequestService registrationRequestService,
                     GoogleSheetsService sheetsService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.telegramService = telegramService;
        this.driveService = driveService;
        this.groupRepository = groupRepository;
        this.registrationRequestService = registrationRequestService;
        this.sheetsService = sheetsService;
    }

    @Bean
    public MathSchoolBot mathSchoolBot() throws Exception {
        MathSchoolBot bot = new MathSchoolBot(
                userService,
                userRepository,
                facultyRepository,
                telegramService,
                driveService,
                groupRepository,
                registrationRequestService,
                sheetsService
        );

        // Реєструємо LongPolling бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        return bot;
    }
}
