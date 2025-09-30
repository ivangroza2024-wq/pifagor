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
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "MathSchoolBot";
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/client_secret.json";

    private static final String SPREADSHEET_ID = "1ZX49DJPlOUfa6AjsjDCDVPfJCys7WBMUWq4SBUiVr5U";

    private final Sheets sheetsService;

    public GoogleSheetsService() throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        Credential credential = authorize(httpTransport, jsonFactory);

        this.sheetsService = new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private GoogleCredential authorize(com.google.api.client.http.HttpTransport httpTransport,
                                       GsonFactory jsonFactory) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                jsonFactory, new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH))
        );

        String refreshToken = System.getenv("GOOGLE_REFRESH_TOKEN");
        if (refreshToken == null) {
            throw new IllegalStateException("GOOGLE_REFRESH_TOKEN не задано у змінних середовища");
        }

        return new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientSecrets)
                .build()
                .setRefreshToken(refreshToken);
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
        // знайти рядок з датою
        String range = groupName + "!A:A";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null) throw new Exception("Не знайдено жодної дати у групі " + groupName);

        int rowIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).isEmpty() && date.equals(rows.get(i).get(0).toString())) {
                rowIndex = i + 1; // Google Sheets індексація з 1
                break;
            }
        }
        if (rowIndex == -1) throw new Exception("Дата " + date + " не знайдена в групі " + groupName);

        // знайти колонку учня
        String headerRange = groupName + "!2:2";
        ValueRange headerResp = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRange)
                .execute();

        List<Object> headerRow = headerResp.getValues().get(0);
        int colIndex = -1;
        for (int i = 0; i < headerRow.size(); i++) {
            if (studentName.equalsIgnoreCase(headerRow.get(i).toString())) {
                colIndex = i;
                break;
            }
        }
        if (colIndex == -1) throw new Exception("Учень " + studentName + " не знайдений у групі " + groupName);

        // якщо ДЗ — зсув на 1 вправо
        if (isHomework) {
            colIndex++;
        }

        return groupName + "!" + columnLetter(colIndex + 1) + rowIndex;
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
