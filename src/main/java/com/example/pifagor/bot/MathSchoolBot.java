package com.example.pifagor.bot;

import com.example.pifagor.model.Faculty;
import com.example.pifagor.model.Group;
import com.example.pifagor.model.RegistrationRequest;
import com.example.pifagor.model.Role;
import com.example.pifagor.model.User;
import com.example.pifagor.repository.FacultyRepository;
import com.example.pifagor.repository.GroupRepository;
import com.example.pifagor.repository.UserRepository;
import com.example.pifagor.service.GoogleDriveService;
import com.example.pifagor.service.GoogleSheetsService;
import com.example.pifagor.service.RegistrationRequestService;
import com.example.pifagor.service.TelegramService;
import com.example.pifagor.service.UserService;
import com.example.pifagor.util.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import java.io.InputStream;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
public class MathSchoolBot extends TelegramWebhookBot {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final TelegramService telegramService;
    private final GoogleDriveService driveService;
    private final GroupRepository groupRepository;
    private final RegistrationRequestService registrationRequestService;
    private final GoogleSheetsService sheetsService;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.admin.chatId}")
    private Long adminChatId;

    @Value("${TELEGRAM_BOT_WEBHOOK_PATH}")
    private String webhookPath;


    private final String rootFolderId = "16mUO4OUdMjsjjbziYZTqwSWOvKo2qZOM";

    public MathSchoolBot(UserService userService,
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
    @Override
    public String getBotUsername() {
        return botUsername;
    }


    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public String getBotPath() {
        return webhookPath; // наприклад "/webhook"
    }
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        System.out.println("=== NEW UPDATE (WEBHOOK) ===");
        System.out.println(update.toString());

        try {
            if (update.hasMessage()) {

                Message message = update.getMessage();
                Long userId = message.getFrom().getId();
                User user = userService.findByTelegramId(userId);

                if (message.hasText()) {
                    String text = message.getText();
                    switch (text) {
                        case "📚 Домашнє завдання" -> {
                            sendHomeworkMenu(message.getChatId(), null, user);
                            return null;
                        }
                        case "⚔️ Битва факультетів" -> {
                            handleFacultyBattle(message.getChatId(), user, null);
                            return null;
                        }
                        case "📋 Перевірити свої ДЗ" -> {
                            sendHomeworkStatus(message.getChatId(), user);
                            return null;
                        }
                        case "✏️ Поставити оцінку" -> {
                            /*
                            sheetsService.updateHomeworkDropdowns(List.of("6кл.Пн.Чт.16:00",
                                    "9кл.Пн.Чт.17:00",
                                    "6кл.Пн.Чт.18:00"));
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Пн.Чт.16:00");
                            sheetsService.addHomeworkColorRulesForSheet("9кл.Пн.Чт.17:00");
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Пн.Чт.18:00");
                            sheetsService.updateHomeworkDropdowns(List.of( "6кл.Пн.Чт.19:00",
                                            "8кл.Пн.Чт.20:00",
                                            "6кл.Вт.Пт.16:00"));
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Пн.Чт.19:00");
                            sheetsService.addHomeworkColorRulesForSheet("8кл.Пн.Чт.20:00");
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Вт.Пт.16:00");
                            sheetsService.updateHomeworkDropdowns(List.of("5кл.Вт.Пт.17:00",
                                    "6кл.Вт.Пт.18:00",
                                    "5кл.Вт.Пт.19:00"));
                            sheetsService.addHomeworkColorRulesForSheet("5кл.Вт.Пт.17:00");
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Вт.Пт.18:00");
                            sheetsService.addHomeworkColorRulesForSheet("5кл.Вт.Пт.19:00");
                            sheetsService.updateHomeworkDropdowns(List.of("8кл.Вт20:00.Сб.12:00",
                                    "8кл.Ср.17:00.Сб.15:00",
                                    "6кл.Ср.Сб.16:00"));
                            sheetsService.addHomeworkColorRulesForSheet("8кл.Вт20:00.Сб.12:00");
                            sheetsService.addHomeworkColorRulesForSheet("8кл.Ср.17:00.Сб.15:00");
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Ср.Сб.16:00");
                            sheetsService.updateHomeworkDropdowns(List.of(
                                    "8кл.Ср.18:00.Сб.13:00",
                                    "6кл.Ср.19:00.Сб.14:00"
                            ));
                            sheetsService.addHomeworkColorRulesForSheet("8кл.Ср.18:00.Сб.13:00");
                            sheetsService.addHomeworkColorRulesForSheet("6кл.Ср.19:00.Сб.14:00");
                        */
                            sendTeacherGroups(message.getChatId(), null);
                            return null;
                        }
                        case "📝 Реєстрація" -> {
                            sendGroupSelection(message.getChatId(), null);
                            return null;
                        }
                        default -> {
                            handleMessage(message);
                            return null;
                        }
                    }
                } else if (message.hasDocument() && user != null) {
                    handleDocument(message, user);
                    return null;
                } else if (message.hasPhoto() && user != null) {
                    handlePhoto(message, user);
                    return null;
                }
            } else if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // якщо нема що відправляти у відповідь відразу
    }


    // ================= MAIN MENU =================
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        User user = userService.findByTelegramId(chatId);
        ReplyKeyboardMarkup replyKeyboard = buildMainMenu(user);
        message.setReplyMarkup(replyKeyboard);

        executeMessage(message);
    }

    private ReplyKeyboardMarkup buildMainMenu(User user) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(true);

        List<KeyboardRow> rows = new ArrayList<>();

        if (user == null) {
            KeyboardRow r = new KeyboardRow();
            r.add("📝 Реєстрація");
            rows.add(r);
        } else {
            switch (user.getRole()) {
                case PARENT -> {
                    KeyboardRow r = new KeyboardRow();
                    r.add("📝 Надіслати ДЗ");
                    rows.add(r);
                }
                case STUDENT -> {
                    KeyboardRow r1 = new KeyboardRow();
                    r1.add("📝 Надіслати ДЗ");
                    rows.add(r1);
                    KeyboardRow r2 = new KeyboardRow();
                    r2.add("🏆 Битва факультетів");
                    rows.add(r2);
                    KeyboardRow r3 = new KeyboardRow();
                    r3.add("📋 Перевірити свої ДЗ");
                    rows.add(r3);
                }
                case TEACHER -> {
                    KeyboardRow r1 = new KeyboardRow();
                    r1.add("✏️ Поставити оцінку");
                    rows.add(r1);
                    KeyboardRow r2 = new KeyboardRow();
                    r2.add("🏆 Битва факультетів");
                    rows.add(r2);
                }
            }
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    // ================= CALLBACK =================
    private void handleCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String data = callbackQuery.getData() == null ? "" : callbackQuery.getData().trim();
        Long userId = callbackQuery.getFrom().getId();
        User user = userService.findByTelegramId(userId);

        System.out.println("Received callback: [" + data + "] from userId: " + userId);

        try {
            // ---- ГОЛОВНЕ МЕНЮ (inline) ----
            if ("menu_main".equals(data)) {
                sendMainMenu(chatId); // тут завжди показуємо ReplyKeyboard
                return;
            }
            if ("menu_homework".equals(data)) {
                if (user == null) {
                    editMessage(chatId, messageId, "❗ Ви ще не зареєстровані.", null);
                } else {
                    sendHomeworkMenu(chatId, messageId, user);
                }
                return;
            }
            if ("menu_battle".equals(data)) {
                handleFacultyBattle(chatId, user, messageId);
                return;
            }
            if ("check_homework_status".equals(data)) {
                sendHomeworkStatus(chatId, user);
                return;
            }
            if ("menu_teacher".equals(data)) {
                sendTeacherGroups(chatId, messageId);
                return;
            }

            // ---- РОЛІ під час реєстрації ----
            if (data.startsWith("role_")) {
                registrationRequestService.saveRole(userId, data.substring(5));
                sendGroupSelection(chatId, messageId);
                return;
            }

            // ---- Вибір групи ----
            if (data.startsWith("group_")) {
                Long groupId = Long.parseLong(data.substring(6));
                registrationRequestService.savePendingGroup(userId, groupId);
                String role = registrationRequestService.getRole(userId);
                if ("parent".equals(role)) {
                    Optional<Group> groupOpt = groupRepository.findById(groupId);
                    if (groupOpt.isPresent()) {
                        sendChildSelectionForParent(chatId, messageId, groupOpt.get(), userId);
                    } else {
                        editMessage(chatId, messageId, "❌ Групу не знайдено.", null);
                    }
                } else {
                    editMessage(chatId, messageId, "✍️ Введіть Прізвище та Ім’я:", null);
                }
                return;
            }

            // ---- БАТЬКИ: вибір дитини ----
            if (data.startsWith("parent_child_")) {
                Long childId = Long.parseLong(data.substring("parent_child_".length()));
                registrationRequestService.savePendingChild(userId, childId);
                editMessage(chatId, messageId, "✍️ Введіть ваше Прізвище та Ім’я (батьки):", null);
                return;
            }

            // ---- АДМІН: підтвердити/відхилити ----
            if (data.startsWith("approve_")) {
                Long requestId = Long.parseLong(data.substring(8));
                RegistrationRequest req = registrationRequestService.approve(requestId);

                editMessage(chatId, messageId, "✅ Заявка підтверджена!", null);
                sendMessage(req.getTelegramId(),
                        "✅ Ваша заявка підтверджена! Ви в групі '" +
                                groupRepository.findById(req.getGroupId()).map(Group::getName).orElse("Невідома") + "'");
                sendMainMenu(req.getTelegramId()); // ReplyKeyboard
                return;
            }
            if (data.startsWith("reject_")) {
                registrationRequestService.reject(Long.parseLong(data.substring(7)));
                editMessage(chatId, messageId, "❌ Заявка відхилена.", null);
                return;
            }
            if (data.startsWith("homework_")) {
                String date = data.substring(9);
                userService.setPendingHomeworkDate(userId, date);
                editMessage(chatId, messageId,
                        "📌 Обрано урок: " + date + "\nНадішліть фото або PDF домашнього завдання.", null);
                return;
            }
            // ---- TEACHER: групи → учні → оцінки ----
            if (data.startsWith("teacher_group_")) {
                Long groupId = Long.parseLong(data.substring("teacher_group_".length()));
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                if (groupOpt.isEmpty()) {
                    editMessage(chatId, messageId, "❌ Групу не знайдено", null);
                    return;
                }
                sendStudentSelection(chatId, messageId, groupOpt.get());
                return;
            }
            if (data.startsWith("teacher_student_")) {
                String payload = data.substring("teacher_student_".length());
                String[] parts = payload.split("\\|", 2);
                Long groupId = Long.parseLong(parts[0]);
                String student = parts[1].replace("~", " ");
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                if (groupOpt.isEmpty()) {
                    editMessage(chatId, messageId, "❌ Групу не знайдено", null);
                    return;
                }
                sendGradeSelection(chatId, messageId, groupOpt.get().getId(), groupOpt.get().getName(), student);
                return;
            }
            if (data.startsWith("teacher_grade_")) {
                String payload = data.substring("teacher_grade_".length());
                String[] parts = payload.split("\\|", 3);
                Long groupId = Long.parseLong(parts[0]);
                String student = parts[1].replace("~", " ");
                int grade = Integer.parseInt(parts[2]);
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                if (groupOpt.isEmpty()) {
                    editMessage(chatId, messageId, "❌ Групу не знайдено", null);
                    return;
                }
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM"));
                sheetsService.setGrade(groupOpt.get().getName(), student, today, grade);
                editMessage(chatId, messageId, "✅ Оцінка " + grade + " виставлена для " + student, null);
                List<User> users = userRepository.findByName(student);
                if (!users.isEmpty()) {
                    addPointsForUser(users.get(0), grade, "Оцінка за урок");
                }
                return;
            }

            // ---- FACULTY ----
            if ("faculty_sorting".equals(data)) {
                handleFacultySorting(chatId, user, messageId);
                return;
            }
            if ("faculty_reset".equals(data)) {
                if (user != null && Role.TEACHER.equals(user.getRole())) {
                    handleFacultyReset(chatId, messageId);
                } else {
                    editMessage(chatId, messageId, "❌ У вас немає прав на цю дію.", null);
                }
                return;
            } else {
                // ---- Якщо нічого не підійшло ----
                editMessage(chatId, messageId, "❌ Невідома команда: " + data, null);
            }
        } catch (Exception e) {
            editMessage(chatId, messageId, "❌ Сталася помилка: " + e.getMessage(), null);
            e.printStackTrace();
        }
    }


    // універсальний editMessage
    private void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup markup) {
        try {
            EditMessageText editMessage = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(markup)
                    .build();
            execute(editMessage);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, text); // fallback
        }
    }

    private void sendMainMenu(Long chatId, Integer messageId, User user) {
        String text = "📍 Головне меню";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (user == null) {
            rows.add(List.of(
                    InlineKeyboardButton.builder().text("📝 Реєстрація").callbackData("start_registration").build()
            ));
        } else {
            switch (user.getRole()) {
                case PARENT -> rows.add(List.of(
                        InlineKeyboardButton.builder().text("📚 Домашнє завдання").callbackData("menu_homework").build()
                ));
                case STUDENT -> {
                    rows.add(List.of(
                            InlineKeyboardButton.builder().text("📚 Домашнє завдання").callbackData("menu_homework").build()
                    ));
                    rows.add(List.of(
                            InlineKeyboardButton.builder().text("⚔️ Битва факультетів").callbackData("menu_battle").build()
                    ));
                    rows.add(List.of(
                            InlineKeyboardButton.builder().text("📋 Перевірити свої ДЗ").callbackData("check_homework_status").build()
                    ));
                }
                case TEACHER -> {
                    rows.add(List.of(
                            InlineKeyboardButton.builder().text("✏️ Поставити оцінку").callbackData("menu_teacher").build()
                    ));
                    rows.add(List.of(
                            InlineKeyboardButton.builder().text("⚔️ Битва факультетів").callbackData("menu_battle").build()
                    ));
                }
            }
        }

        markup.setKeyboard(rows);

        if (messageId == null) {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(markup)
                    .build();
            executeSafely(msg);
        } else {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(markup)
                    .build();
            executeSafely(edit);
        }
    }

    // ------------------- CALLBACK -------------------
    private <T extends Serializable, M extends BotApiMethod<T>> void executeSafely(M method) {
        try {
            execute(method);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChildSelectionForParent(Long chatId, Integer messageId, Group group, Long parentTelegramId) {
        List<User> students = userRepository.findByGroupAndRole(group, Role.STUDENT);
        if (students.isEmpty()) {
            sendMessage(chatId, "У цій групі ще немає зареєстрованих дітей.");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (User child : students) {
            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text(child.getName())
                            .callbackData("parent_child_" + child.getId())
                            .build()
            ));
        }
        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage(chatId.toString(),
                "Оберіть вашу дитину зі списку групи " + group.getName() + ":");
        msg.setReplyMarkup(markup);
        executeMessage(msg);
    }

    private void notifyAdminAboutNewParent(RegistrationRequest req, User child) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(adminChatId));
        message.setText("Нова заявка від батьків:\n" +
                "👨‍👩‍👧 Батьки: " + req.getName() +
                "\nДитина: " + (child != null ? child.getName() : "невідомо") +
                "\nГрупа: " + registrationRequestService.getGroupName(req.getGroupId()) +
                "\nTelegram ID (батьки): " + req.getTelegramId());
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("✅ Підтвердити").callbackData("approve_" + req.getId()).build());
        row.add(InlineKeyboardButton.builder().text("❌ Відхилити").callbackData("reject_" + req.getId()).build());
        markup.setKeyboard(Collections.singletonList(row));
        message.setReplyMarkup(markup);
        executeMessage(message);
    }

    // ------------------- TEACHER -------------------
    private void sendTeacherGroups(Long chatId, Integer messageId) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String todayShort = switch (today) {
            case MONDAY -> "Пн";
            case TUESDAY -> "Вт";
            case WEDNESDAY -> "Ср";
            case THURSDAY -> "Чт";
            case FRIDAY -> "Пт";
            case SATURDAY -> "Сб";
            default -> null;
        };

        List<Group> groups = groupRepository.findAll();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Group g : groups) {
            if (g.getDay1().equals(todayShort) || g.getDay2().equals(todayShort)) {
                rows.add(Collections.singletonList(
                        InlineKeyboardButton.builder()
                                .text(g.getName())
                                .callbackData("teacher_group_" + g.getId())
                                .build()
                ));
            }
        }

        // add reset button for teachers at the end of teacher menu
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("♻️ Скинути битву факультетів")
                        .callbackData("faculty_reset")
                        .build()
        ));

        if (rows.isEmpty()) {
            sendMessage(chatId, "Сьогодні немає занять.");
            return;
        }

        markup.setKeyboard(rows);
        SendMessage msg = new SendMessage(chatId.toString(), "Оберіть групу (або скиньте битву):");
        msg.setReplyMarkup(markup);
        executeMessage(msg);
    }

    private void sendStudentSelection(Long chatId, Integer messageId, Group group) {
        try {
            List<String> students = sheetsService.getStudents(group.getName());
            if (students == null || students.isEmpty()) {
                sendMessage(chatId, "❌ У цій групі немає учнів.");
                return;
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String s : students) {
                // replace spaces with ~ for callback safety
                String callbackData = "teacher_student_" + group.getId() + "|" + s.replace(" ", "~");
                rows.add(Collections.singletonList(
                        InlineKeyboardButton.builder()
                                .text(s)
                                .callbackData(callbackData)
                                .build()
                ));
            }

            markup.setKeyboard(rows);
            SendMessage msg = new SendMessage(chatId.toString(), "Оберіть учня:");
            msg.setReplyMarkup(markup);
            executeMessage(msg);

            System.out.println("Sent student selection for group: " + group.getName());

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "❌ Не вдалося завантажити учнів: " + e.getMessage());
        }
    }

    private void sendGradeSelection(Long chatId, Integer messageId, Long groupId, String groupName, String student) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String studentCallback = student.replace(" ", "~");

        for (int i = 0; i <= 12; i++) {
            String gradeText = i == 0 ? "н" : String.valueOf(i);
            String callbackData = "teacher_grade_" + groupId + "|" + studentCallback + "|" + i;

            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text(gradeText)
                            .callbackData(callbackData)
                            .build()
            ));
        }

        markup.setKeyboard(rows);
        SendMessage msg = new SendMessage(chatId.toString(),
                "Виставити оцінку для " + student + ":");
        msg.setReplyMarkup(markup);
        executeMessage(msg);

        System.out.println("Sent grade selection for student: " + student + " in group: " + groupName);
    }

    // ------------------- FACULTY BATTLE -------------------
    private void handleFacultyBattle(Long chatId, User user, Integer messageId) {
        if (user == null) {
            sendMessage(chatId, "❗ Ви ще не зареєстровані.");
            return;
        }

        if (user.getFaculty() == null) {
            // show sorting hat message + button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text("🎩 Розподілити")
                            .callbackData("faculty_sorting")
                            .build()
            ));
            markup.setKeyboard(rows);

            SendMessage msg = new SendMessage(chatId.toString(),
                    "🎩 Розподіляюча шляпа каже:\n\n" +
                            "Щоб взяти участь у Битві факультетів, потрібно:\n" +
                            "• Виконувати домашні завдання\n" +
                            "• Бути активним на уроках\n" +
                            "• Мати хорошу поведінку\n\n" +
                            "Якщо погоджуєтесь — натисніть «Розподілити» і шляпа обере для вас факультет.");
            msg.setReplyMarkup(markup);
            executeMessage(msg);
        } else {
            // show faculties ranking + user's faculty top-3
            List<Faculty> allFaculties = facultyRepository.findAll();
            allFaculties.sort(Comparator.comparingInt(Faculty::getPoints).reversed());

            StringBuilder sb = new StringBuilder("🏆 Поточний рейтинг факультетів:\n\n");
            for (int i = 0; i < allFaculties.size(); i++) {
                Faculty f = allFaculties.get(i);
                sb.append(i + 1).append(". ").append(f.getName())
                        .append(" ").append(f.getSymbol() == null ? "" : f.getSymbol())
                        .append(" — ").append(f.getPoints()).append(" балів\n");
            }

            sb.append("\nВаш факультет: ")
                    .append(user.getFaculty().getName())
                    .append(" ").append(user.getFaculty().getSymbol() == null ? "" : user.getFaculty().getSymbol())
                    .append(" (особисто: ").append(user.getFaculty() == null ? 0 : user.getFacultyPoints()) // note: faculty points
                    .append(")\n\n");

            // top-3 users in user's faculty
            List<User> allUsers = userRepository.findAll();
            List<User> sameFacultyUsers = new ArrayList<>();
            for (User u : allUsers) {
                if (u.getFaculty() != null && u.getFaculty().getId().equals(user.getFaculty().getId())) {
                    sameFacultyUsers.add(u);
                }
            }
            sameFacultyUsers.sort((a, b) -> Integer.compare(b.getFacultyPoints(), a.getFacultyPoints()));

            sb.append("⭐ Топ-3 учасники вашого факультету:\n");
            for (int i = 0; i < Math.min(3, sameFacultyUsers.size()); i++) {
                User u = sameFacultyUsers.get(i);
                sb.append(i + 1).append(". ").append(u.getName()).append(" — ").append(u.getFacultyPoints()).append(" балів\n");
            }

            sendMessage(chatId, sb.toString());
        }
    }

    private void handleFacultySorting(Long chatId, User user, Integer messageId) {
        if (user == null) {
            sendMessage(chatId, "❗ Ви ще не зареєстровані.");
            return;
        }

        // 1️⃣ Перевірка, чи вже має факультет
        if (user.getFaculty() != null) {
            sendMessage(chatId, "❌ Ви вже належите до факультету "
                    + user.getFaculty().getName() + " і не можете обрати новий.");
            return;
        }

        // 2️⃣ Отримуємо всі факультети
        List<Faculty> faculties = facultyRepository.findAll();
        if (faculties.isEmpty()) {
            sendMessage(chatId, "❌ Факультети не налаштовані в системі.");
            return;
        }

        // 3️⃣ Знаходимо факультет(и) з мінімальною кількістю учнів
        // знаходимо мінімальну кількість учнів у факультетах
        int minCount = faculties.stream()
                .mapToInt(f -> userRepository.countByFaculty(f))
                .min()
                .orElse(0);

// вибираємо факультети з мінімальною кількістю учнів
        List<Faculty> candidates = faculties.stream()
                .filter(f -> userRepository.countByFaculty(f) == minCount)
                .toList();


        // 4️⃣ Вибираємо випадковий факультет серед кандидатів
        Faculty chosen = candidates.get(new Random().nextInt(candidates.size()));

        // 5️⃣ Прив'язуємо учня до факультету
        user.setFaculty(chosen);
        user.setFacultyPoints(0);
        userService.save(user);

        // 6️⃣ Повідомлення користувачу
        String text = "🎉 Вітаємо! Ви потрапили до факультету *"
                + chosen.getName() + "* "
                + (chosen.getSymbol() == null ? "" : chosen.getSymbol());
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setParseMode("Markdown");
        executeMessage(msg);

        // 7️⃣ Відправка картинки факультету, якщо існує
        try {
            String resourceName = "/faculties/" + chosen.getName() + ".png";
            InputStream pic = getClass().getResourceAsStream(resourceName);
            if (pic != null) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(chatId.toString());
                photo.setPhoto(new InputFile(pic, chosen.getName() + ".png"));
                photo.setCaption("Ваш факультет: " + chosen.getName() + " "
                        + (chosen.getSymbol() == null ? "" : chosen.getSymbol()));
                execute(photo);
            } else {
                System.out.println("Faculty image not found for resource: " + resourceName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFacultyReset(Long chatId, Integer messageId) {
        // Reset all users' faculty link and personal points, and faculty points
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            u.setFaculty(null);
            u.setFacultyPoints(0);
        }
        userRepository.saveAll(allUsers);

        List<Faculty> allFaculties = facultyRepository.findAll();
        for (Faculty f : allFaculties) {
            f.setPoints(0);
        }
        facultyRepository.saveAll(allFaculties);

        sendMessage(chatId, "♻️ Усі факультети та особисті бали скинуті. Починаємо новий сезон!");
        System.out.println("Faculties and user faculty-points reset by teacher (chatId=" + chatId + ")");
    }

    // helper to add points both to user and to faculty total
    private void addPointsForUser(User u, int points, String reason) {
        if (u == null) return;
        if (points <= 0) return;

        // add to user's personal faculty points
        u.setFacultyPoints(u.getFacultyPoints() + points);
        userRepository.save(u);

        // add to faculty aggregate
        if (u.getFaculty() != null) {
            Faculty f = u.getFaculty();
            f.setPoints(f.getPoints() + points);
            facultyRepository.save(f);
            System.out.println("Added " + points + " points to user " + u.getName() + " and faculty " + f.getName() + " (reason: " + reason + ")");
        } else {
            System.out.println("User " + u.getName() + " has no faculty — points added to user only (reason: " + reason + ")");
        }
    }

    // ------------------- MAIN MENU -------------------
    private void sendMainMenu(Long chatId) {
        User user = userService.findByTelegramId(chatId);
        String text = "🎉 Головне меню. Оберіть дію:";
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setReplyMarkup(buildMainMenu(user));
        executeMessage(msg);
    }

    // ------------------- MESSAGES & HELPERS -------------------
    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText();

        if ("/start".equals(text)) {
            if (registrationRequestService.isApproved(userId)) {
                sendMainMenu(chatId);
            } else {
                sendRoleSelection(chatId);
            }
            return;
        }

        // registration flow: if pending group and user sends name
        if (!registrationRequestService.isApproved(userId)
                && !"unknown".equals(registrationRequestService.getRole(userId))
                && registrationRequestService.getPendingGroup(userId) != null
                && !text.startsWith("/")) {

            RegistrationRequest req = registrationRequestService.createRequest(userId, text.trim());
            sendMessage(chatId, "✅ Ваші дані надіслані на підтвердження адміністратору.");
            notifyAdminAboutNewChild(req);
            return;
        }

        if (registrationRequestService.isApproved(userId)) {
            User user = userService.findByTelegramId(userId);
            Integer messageId = null;
            if ("📝 Надіслати ДЗ".equalsIgnoreCase(text)) {
                if (user == null) {
                    sendMessage(chatId, "❗ Ви ще не зареєстровані.");
                } else {
                    sendHomeworkMenu(chatId, messageId, user);
                }
            } else if ("🏆 Битва факультетів".equalsIgnoreCase(text)) {
                handleFacultyBattle(chatId, user, messageId);
            } else {
                sendMessage(chatId, "Оберіть дію з меню ⬇️");
            }
        }
    }

    private void handlePhoto(Message message, User user) {
        String pendingDate = userService.getPendingHomeworkDate(user.getTelegramId());
        if (pendingDate == null) {
            sendMessage(message.getChatId(), "Будь ласка, спочатку оберіть урок через меню ДЗ.");
            return;
        }

        try {
            // ✅ якщо це батьки, працюємо з дитиною
            User targetUser = (user.getRole() == Role.PARENT && user.getChild() != null) ? user.getChild() : user;

            // ✅ нормалізуємо дату
            String formattedDate = DateUtil.toSheetsFormat(pendingDate);

            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.get(photos.size() - 1);
            InputStream fileStream = telegramService.downloadFile(largestPhoto.getFileId());
            if (fileStream == null) {
                sendMessage(message.getChatId(), "Помилка: не вдалося завантажити файл з Telegram");
                return;
            }

            String groupFolderId = driveService.getOrCreateFolder(rootFolderId, targetUser.getGroup().getName());
            String dateFolderId = driveService.getOrCreateFolder(groupFolderId, formattedDate);

            String fileName = targetUser.getName() + "_homework_" + System.currentTimeMillis() + ".jpg";
            driveService.uploadFile(dateFolderId, fileStream, fileName, "image/jpeg");

            // ✅ якщо ще не надсилали ДЗ за цю дату — даємо бали і пишемо в Sheets
            if (!userService.hasSubmittedHomework(targetUser, formattedDate)) {
                addPointsForUser(targetUser, 5, "ДЗ (завантаження)");

                sheetsService.setHomework(
                        targetUser.getGroup().getName(),
                        targetUser.getName(),
                        formattedDate,
                        "чудово"
                );

                userService.markHomeworkSubmitted(targetUser, formattedDate);
                sendMessage(message.getChatId(), "✅ ДЗ успішно надіслано! 🎁 Ви отримали +5 балів.");
            } else {
                sendMessage(message.getChatId(), "✅ Додано ще одне фото до вашого ДЗ.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(message.getChatId(), "Помилка при завантаженні фото на Google Drive.");
        }
    }

    private void handleDocument(Message message, User user) {
        String pendingDate = userService.getPendingHomeworkDate(user.getTelegramId());
        if (pendingDate == null) {
            sendMessage(message.getChatId(), "Будь ласка, спочатку оберіть урок через меню ДЗ.");
            return;
        }

        try {
            User targetUser = (user.getRole() == Role.PARENT && user.getChild() != null) ? user.getChild() : user;

            // ✅ нормалізуємо дату
            String formattedDate = DateUtil.toSheetsFormat(pendingDate);

            InputStream fileStream = telegramService.downloadFile(message.getDocument().getFileId());
            String groupFolderId = driveService.getOrCreateFolder(rootFolderId, targetUser.getGroup().getName());
            String dateFolderId = driveService.getOrCreateFolder(groupFolderId, formattedDate);

            String fileName = targetUser.getName() + "_" + System.currentTimeMillis() + "_" + message.getDocument().getFileName();
            driveService.uploadFile(dateFolderId, fileStream, fileName, message.getDocument().getMimeType());

            if (!userService.hasSubmittedHomework(targetUser, formattedDate)) {
                addPointsForUser(targetUser, 5, "ДЗ (завантаження)");

                sheetsService.setHomework(
                        targetUser.getGroup().getName(),
                        targetUser.getName(),
                        formattedDate,
                        "чудово"
                );

                userService.markHomeworkSubmitted(targetUser, formattedDate);
                sendMessage(message.getChatId(), "✅ ДЗ успішно надіслано! 🎁 Ви отримали +5 балів.");
            } else {
                sendMessage(message.getChatId(), "✅ Додано ще один файл до вашого ДЗ.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(message.getChatId(), "Помилка при завантаженні файлу на Google Drive.");
        }
    }

    private void sendRoleSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Оберіть вашу роль:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("👶 Дитина").callbackData("role_child").build());
        row.add(InlineKeyboardButton.builder().text("👨‍👩‍👧 Батьки").callbackData("role_parent").build());
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeMessage(message);
    }

    private void sendGroupSelection(Long chatId, Integer messageId) {
        List<Group> groups = groupRepository.findAll();
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Оберіть групу:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Group g : groups) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(InlineKeyboardButton.builder().text(g.getName()).callbackData("group_" + g.getId()).build());
            rows.add(row);
        }
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeMessage(message);
    }

    private void notifyAdminAboutNewChild(RegistrationRequest req) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(adminChatId));
        message.setText("Нова заявка на підтвердження:\nДитина: " + req.getName() +
                "\nГрупа: " + registrationRequestService.getGroupName(req.getGroupId()) +
                "\nTelegram ID: " + req.getTelegramId());
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("✅ Підтвердити").callbackData("approve_" + req.getId()).build());
        row.add(InlineKeyboardButton.builder().text("❌ Відхилити").callbackData("reject_" + req.getId()).build());
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeMessage(message);
    }

    private void sendHomeworkMenu(Long chatId, Integer messageId, User user) {
        try {
            List<String> lastLessons = driveService.getLastFiveLessonDates(
                    user.getGroup().getDay1(), user.getGroup().getDay2(),
                    user.getGroup().getTime1(), user.getGroup().getTime2()
            );
            if (lastLessons.isEmpty()) {
                sendMessage(chatId, "Поки що немає доступних уроків для ДЗ.");
                return;
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (String lesson : lastLessons) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(InlineKeyboardButton.builder().text(lesson).callbackData("homework_" + lesson).build());
                rows.add(row);
            }
            markup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Оберіть урок, за який надсилаєте ДЗ:");
            message.setReplyMarkup(markup);
            executeMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Помилка при отриманні дат уроків.");
        }
    }
    private void sendHomeworkStatus(Long chatId, User user) {
        try {
            // 1. Отримуємо останні 5 уроків (дати)
            List<String> lastLessons = driveService.getLastFiveLessonDates(
                    user.getGroup().getDay1(), user.getGroup().getDay2(),
                    user.getGroup().getTime1(), user.getGroup().getTime2()
            );
            if (lastLessons.isEmpty()) {
                sendMessage(chatId, "Поки що немає доступних уроків для перевірки.");
                return;
            }

            // 2. Отримуємо статуси ДЗ з Google Sheets
            List<String> homeworkStatuses = sheetsService.getHomeworkStatusesForLessons(user, lastLessons);

            // 3. Формуємо текст повідомлення
            StringBuilder sb = new StringBuilder("📚 *Ваші ДЗ за останні 5 уроків:*\n\n");
            for (int i = 0; i < lastLessons.size(); i++) {
                String date = lastLessons.get(i);
                String status = (i < homeworkStatuses.size() && homeworkStatuses.get(i) != null)
                        ? homeworkStatuses.get(i)
                        : "—";
                sb.append("• ").append(date).append(" — ").append(status).append("\n");
            }

            // 4. Надсилаємо
            sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Помилка при отриманні статусів ДЗ 😔");
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
