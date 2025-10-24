package com.example.pifagor.service;

import com.example.pifagor.model.User;
import com.example.pifagor.util.DateUtil;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "MathSchoolBot";
    private static final String SPREADSHEET_ID = "1ZX49DJPlOUfa6AjsjDCDVPfJCys7WBMUWq4SBUiVr5U";

    private final Sheets sheetsService;

    public GoogleSheetsService() {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            String clientId = System.getenv("GOOGLE_CLIENT_ID");
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            String refreshToken = System.getenv("GOOGLE_REFRESH_TOKEN");

            if (clientId == null || clientSecret == null || refreshToken == null) {
                throw new IllegalStateException("Не вистачає змінних середовища для Google OAuth");
            }

            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(jsonFactory)
                    .setClientSecrets(clientId, clientSecret)
                    .build()
                    .setRefreshToken(refreshToken);

            this.sheetsService = new Sheets.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося ініціалізувати GoogleSheetsService", e);
        }
    }

    public Sheets getSheetsService() {
        return sheetsService;
    }

    public String getSpreadsheetId() {
        return SPREADSHEET_ID;
    }




    /**
     * Отримати список учнів (2-й рядок у аркуші групи)
     */
    public List<String> getStudents(String groupName) throws Exception {
        String range = groupName + "!2:2"; // другий рядок
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<Object> row = response.getValues() != null ? response.getValues().get(0) : new ArrayList<>();
        List<String> students = new ArrayList<>();
        for (Object o : row) {
            String name = String.valueOf(o).trim();
            if (!name.isBlank() && !name.equalsIgnoreCase("Дата") && !name.equalsIgnoreCase("Тема")) {
                students.add(name);
            }
        }
        return students;
    }

    /**
     * Виставити оцінку учню
     */
    /** Виставляє оцінку */
    public void setGrade(String groupName, String studentName, String date, int grade) throws Exception {
        System.out.println("Date: "+ date);
        String formattedDate = DateUtil.toSheetsFormat(date);
        System.out.println("formattedDate: "+ formattedDate);
        String cell = findCell(groupName, studentName, formattedDate, false);

        ValueRange body = new ValueRange().setValues(List.of(List.of(grade)));
        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, cell, body)
                .setValueInputOption("RAW")
                .execute();
    }

    /** Виставляє ДЗ */
    public void setHomework(String groupName, String studentName, String date, String status) throws Exception {
        String formattedDate = DateUtil.toSheetsFormat(date);

        String cell = findCell(groupName, studentName, formattedDate, true);

        ValueRange body = new ValueRange().setValues(List.of(List.of(status)));
        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, cell, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public List<String> getHomeworkStatusesForLessons(User user, List<String> lessonDates) throws Exception {
        String groupName = user.getGroup().getName();
        String safeSheetName = "'" + groupName.replace("'", "") + "'";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate today = LocalDate.now();

        List<String> results = new ArrayList<>();
        List<ValueRange> updates = new ArrayList<>();

        for (String lessonDate : lessonDates) {
            try {
                // 1️⃣ Знаходимо адресу комірки ДЗ
                String cellRef = findCell(groupName, user.getName(), lessonDate, true);
                if (cellRef == null) {
                    results.add("—");
                    continue;
                }

                // 2️⃣ Зчитуємо поточне значення
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(SPREADSHEET_ID, cellRef)
                        .execute();

                String status = "—";
                if (response.getValues() != null && !response.getValues().isEmpty()) {
                    Object val = response.getValues().get(0).get(0);
                    if (val != null && !val.toString().trim().isEmpty()) {
                        status = val.toString().trim();
                    }
                }

                // 3️⃣ Якщо дата менша за сьогодні і клітинка пуста — ставимо "немає"
                LocalDate lessonDay = LocalDate.parse(lessonDate, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                if (status.equals("—") && lessonDay.isBefore(today)) {
                    updates.add(new ValueRange()
                            .setRange(cellRef)
                            .setValues(List.of(List.of("немає"))));
                    status = "немає";
                }

                results.add(status);

            } catch (Exception e) {
                e.printStackTrace();
                results.add("—");
            }
        }

        // 4️⃣ Якщо були оновлення — виконуємо пакетний запис
        if (!updates.isEmpty()) {
            BatchUpdateValuesRequest batchRequest = new BatchUpdateValuesRequest()
                    .setValueInputOption("RAW")
                    .setData(updates);
            sheetsService.spreadsheets().values().batchUpdate(SPREADSHEET_ID, batchRequest).execute();
            System.out.println("✅ Проставлено 'немає' у " + updates.size() + " клітинках для " + user.getName());
        }

        return results;
    }


    private String getColumnLetter(int columnIndex) {
        StringBuilder column = new StringBuilder();
        int index = columnIndex;
        while (index >= 0) {
            column.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return column.toString();
    }
    /** Пошук комірки учня+дата */
    private String findCell(String groupName, String studentName, String date, boolean isHomework) throws Exception {
        String safeSheetName = "'" + groupName.replace("'", "") + "'";
        String range = safeSheetName + "!A:A";

        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null) throw new Exception("Не знайдено жодної дати у групі " + groupName);

        // 🔹 Формат дати, який є у таблиці
        DateTimeFormatter sheetFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // 🔹 Очищаємо дату від часу, міняємо роздільники
        String cleanedDate = date.split(" ")[0].replace("-", ".");
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(cleanedDate, sheetFormat);
        } catch (Exception e) {
            throw new Exception("Неправильний формат дати: " + date);
        }

        // 🔹 Шукаємо рядок з датою
        int rowIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).isEmpty()) {
                Object val = rows.get(i).get(0);
                String sheetDateStr;

                if (val instanceof Double) {
                    // Якщо дата збережена як число
                    double serial = (Double) val;
                    long msSince1899 = (long) ((serial - 25569) * 86400 * 1000);
                    sheetDateStr = new java.text.SimpleDateFormat("dd.MM.yyyy")
                            .format(new java.util.Date(msSince1899));
                } else {
                    sheetDateStr = val.toString().trim();
                }

                try {
                    LocalDate sheetDate = LocalDate.parse(sheetDateStr, sheetFormat);
                    if (sheetDate.equals(targetDate)) {
                        rowIndex = i + 1;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (rowIndex == -1)
            throw new Exception("Дата " + cleanedDate + " не знайдена в групі " + groupName);

        // 🔹 Отримуємо рядок студентів
        String headerRange = safeSheetName + "!2:2";
        ValueRange headerResp = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRange)
                .execute();

        if (headerResp.getValues() == null || headerResp.getValues().isEmpty())
            throw new Exception("Рядок студентів не знайдено на аркуші " + groupName);

        List<Object> headerRow = headerResp.getValues().get(0);
        int colIndex = -1;
        for (int i = 0; i < headerRow.size(); i++) {
            if (studentName.equalsIgnoreCase(headerRow.get(i).toString().trim())) {
                colIndex = i;
                break;
            }
        }
        if (colIndex == -1)
            throw new Exception("Учень " + studentName + " не знайдений у групі " + groupName);

        if (isHomework) colIndex++;

        return safeSheetName + "!" + columnLetter(colIndex + 1) + rowIndex;
    }

    public void updateHomeworkDropdowns(List<String> sheetNames) throws Exception {
        for (String sheetName : sheetNames) {
            updateHomeworkDropdownForSheet(sheetName);
        }
    }

    private void updateHomeworkDropdownForSheet(String sheetName) throws Exception {
        // 1️⃣ Отримуємо ID аркуша
        Spreadsheet spreadsheet = sheetsService.spreadsheets()
                .get(SPREADSHEET_ID)
                .execute();

        Sheet sheet = spreadsheet.getSheets().stream()
                .filter(s -> s.getProperties().getTitle().equalsIgnoreCase(sheetName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Не знайдено аркуш: " + sheetName));

        /// Спробуємо знайти рядок, у якому є "ДЗ"
        int headerRowIndex = -1;
        String headerRangeToCheck = "'" + sheetName + "'!1:10"; // перевіряємо перші 10 рядків

        ValueRange headerRows = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRangeToCheck)
                .execute();

        List<List<Object>> headerValues = headerRows.getValues();

        if (headerValues != null) {
            for (int i = 0; i < headerValues.size(); i++) {
                List<Object> row = headerValues.get(i);
                for (Object cell : row) {
                    if (cell != null && cell.toString().toLowerCase().contains("дз")) {
                        headerRowIndex = i + 1; // рядки нумеруються з 1, а індекси з 0
                        break;
                    }
                }
                if (headerRowIndex != -1) break;
            }
        }

        if (headerRowIndex == -1) {
            System.out.println("❗ На аркуші " + sheetName + " не знайдено рядка з колонками ДЗ");
            return;
        }

        System.out.println("✅ На аркуші " + sheetName + " знайдено заголовки у рядку " + headerRowIndex);

// Тепер формуємо діапазон саме з цього рядка
        String headerRange = "'" + sheetName + "'!" + headerRowIndex + ":" + headerRowIndex;
        ValueRange headerResp = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRange)
                .execute();

        List<Object> header = headerResp.getValues() != null ? headerResp.getValues().get(0) : new ArrayList<>();

        // 3️⃣ Знаходимо всі колонки з назвою "ДЗ"
        List<Integer> homeworkCols = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            if (String.valueOf(header.get(i)).equalsIgnoreCase("ДЗ")) {
                homeworkCols.add(i);
            }
        }

        if (homeworkCols.isEmpty()) {
            System.out.println("❗ На аркуші " + sheetName + " не знайдено колонок ДЗ");
            return;
        }

        // 4️⃣ Формуємо нове правило
        List<String> values = List.of("чудово", "частково", "немає", "на перевірці", "виконано неправильно");

        List<Request> requests = new ArrayList<>();
        for (Integer col : homeworkCols) {
            GridRange range = new GridRange()
                    .setSheetId(sheet.getProperties().getSheetId())
                    .setStartRowIndex(2) // з 3-го рядка
                    .setStartColumnIndex(col)
                    .setEndColumnIndex(col + 1);

            DataValidationRule rule = new DataValidationRule()
                    .setCondition(new BooleanCondition()
                            .setType("ONE_OF_LIST")
                            .setValues(values.stream()
                                    .map(v -> new ConditionValue().setUserEnteredValue(v))
                                    .toList()))
                    .setStrict(true)
                    .setShowCustomUi(true);

            requests.add(new Request().setSetDataValidation(
                    new SetDataValidationRequest()
                            .setRange(range)
                            .setRule(rule)
            ));
        }

        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

        System.out.println("✅ Оновлено випадаючі списки для аркуша " + sheetName);
    }
    /**
     * Додає conditional formatting для всіх колонок "ДЗ" на аркуші.
     */
    public void addHomeworkColorRulesForSheet(String sheetName) throws Exception {
        String safeSheetName = sheetName.replace("'", "");
        // 1) Отримати metadata для пошуку sheetId
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).setIncludeGridData(false).execute();
        Integer sheetId = null;
        for (Sheet s : spreadsheet.getSheets()) {
            if (safeSheetName.equals(s.getProperties().getTitle())) {
                sheetId = s.getProperties().getSheetId();
                break;
            }
        }
        if (sheetId == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }

        // 2) Знайти заголовковий рядок (перші 10 рядків)
        String headerRangeToCheck = "'" + sheetName + "'!1:10";
        ValueRange headerRows = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRangeToCheck)
                .execute();
        List<List<Object>> headerValues = headerRows.getValues();
        if (headerValues == null) {
            throw new IllegalStateException("No header rows in sheet " + sheetName);
        }

        int headerRowIndex = -1;
        List<Object> headerRow = null;
        for (int i = 0; i < headerValues.size(); i++) {
            List<Object> row = headerValues.get(i);
            for (Object cell : row) {
                if (cell != null && cell.toString().toLowerCase().contains("дз")) {
                    headerRow = row;
                    headerRowIndex = i + 1; // 1-based
                    break;
                }
            }
            if (headerRowIndex != -1) break;
        }
        if (headerRowIndex == -1) {
            System.out.println("❗ На аркуші " + sheetName + " не знайдено колонок ДЗ");
            return;
        }

        // 3) Знайти індекси колонок, де є "ДЗ"
        List<Integer> dzColumnIndexes = new ArrayList<>(); // 0-based
        for (int c = 0; c < headerRow.size(); c++) {
            Object h = headerRow.get(c);
            if (h != null && h.toString().toLowerCase().contains("дз")) {
                dzColumnIndexes.add(c);
            }
        }
        if (dzColumnIndexes.isEmpty()) {
            System.out.println("❗ Не знайдено колонок ДЗ у заголовку на " + sheetName);
            return;
        }

        // 4) Параметри — від якого рядка нижче заголовка застосовувати формат (наприклад headerRowIndex+0 -> сам заголовок включно)
        int startRow = headerRowIndex + 0;            // 1-based
        int endRow = 1000; // наприклад поки до 1000. Можеш динамічно визначати останній рядок.

        // 5) Кольори (RGB в діапазоні 0..1)
        Color green = new Color().setRed(0f).setGreen(0.8f).setBlue(0.2f); // чудово
        Color yellow = new Color().setRed(1f).setGreen(0.85f).setBlue(0.2f); // частково
        Color red = new Color().setRed(0.94f).setGreen(0.2f).setBlue(0.2f); // виконано не правильно
        Color gray = new Color().setRed(0.15f).setGreen(0.15f).setBlue(0.15f); // немає (темний)
        Color neutral = new Color().setRed(0.9f).setGreen(0.9f).setBlue(0.9f); // на перевірці (світло-нейтральний)

        // Map value -> color
        Map<String, Color> map = new LinkedHashMap<>();
        map.put("чудово", green);
        map.put("частково", yellow);
        map.put("немає", red);
        map.put("на перевірці", neutral);

        // 6) Для кожної found column - створити ConditionalFormatRule з TEXT_EQ для кожного значення
        List<Request> requests = new ArrayList<>();
        for (Integer colIndex0 : dzColumnIndexes) {
            GridRange gridRange = new GridRange()
                    .setSheetId(sheetId)
                    // API використовує 0-based indices and endIndex exclusive
                    .setStartRowIndex(startRow - 1)        // робимо з headerRowIndex (якщо хочеш без заголовку -> +1)
                    .setEndRowIndex(endRow)               // exclusive
                    .setStartColumnIndex(colIndex0)
                    .setEndColumnIndex(colIndex0 + 1);

            for (Map.Entry<String, Color> e : map.entrySet()) {
                String targetValue = e.getKey();
                Color color = e.getValue();

                // BooleanRule з TEXT_EQ
                BooleanCondition cond = new BooleanCondition()
                        .setType("TEXT_EQ")
                        .setValues(List.of(new ConditionValue().setUserEnteredValue(targetValue)));

                CellFormat fmt = new CellFormat().setBackgroundColor(color);

                BooleanRule rule = new BooleanRule().setCondition(cond).setFormat(fmt);

                ConditionalFormatRule cfr = new ConditionalFormatRule()
                        .setRanges(List.of(gridRange))
                        .setBooleanRule(rule);

                requests.add(new Request().setAddConditionalFormatRule(new AddConditionalFormatRuleRequest().setRule(cfr).setIndex(0)));
            }
        }

        // 7) Виклик batchUpdate
        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

        System.out.println("✅ Conditional formatting added for sheet " + sheetName);
    }
    // 🔹 конвертація номера стовпця → букву (A,B,C,...)
    private String columnLetter(int col) {
        StringBuilder sb = new StringBuilder();
        while (col > 0) {
            int rem = (col - 1) % 26;
            sb.insert(0, (char) (rem + 'A'));
            col = (col - 1) / 26;
        }
        return sb.toString();
    }
}
