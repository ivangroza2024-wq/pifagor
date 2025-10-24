package com.example.pifagor.service;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


    /** Пошук комірки учня+дата */
    private String findCell(String groupName, String studentName, String date, boolean isHomework) throws Exception {
        // 1. Безпечна назва аркуша (апострофи + пробіли)
        String safeSheetName = "'" + groupName.replace("'", "") + "'";

        // 2. Отримуємо всі дати (стовпець А)
        String range = safeSheetName + "!A:A";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null) throw new Exception("Не знайдено жодної дати у групі " + groupName);

        // 3. Шукаємо потрібну дату (ігноруємо формат пробілів)
        int rowIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).isEmpty()) {
                String sheetDate = rows.get(i).get(0).toString().trim();
                if (sheetDate.equalsIgnoreCase(date.trim())) {
                    rowIndex = i + 1; // Google Sheets індексація з 1
                    break;
                }
            }
        }
        if (rowIndex == -1) throw new Exception("Дата " + date + " не знайдена в групі " + groupName);

        // 4. Отримуємо заголовок (рядок студентів)
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
        if (colIndex == -1) throw new Exception("Учень " + studentName + " не знайдений у групі " + groupName);

        // 5. Для ДЗ зсув вправо
        if (isHomework) {
            colIndex++;
        }

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

        // 2️⃣ Отримуємо лише 2-й рядок (заголовок)
        String headerRange = "'" + sheetName + "'!2:2";
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
