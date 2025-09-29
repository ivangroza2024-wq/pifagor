package com.example.pifagor.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleDriveService {

    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private final HttpRequestFactory requestFactory;
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private final String rootFolderId = "16mUO4OUdMjsjjbziYZTqwSWOvKo2qZOM";

    public GoogleDriveService() {
        try {
            GoogleSecretsHelper.createClientSecretFile();
            // Завантаження client_secret.json
            InputStream in = Files.newInputStream(Paths.get("src/main/resources/client_secret.json"));
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

            // OAuth flow
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(), jsonFactory, clientSecrets,
                    Collections.singletonList("https://www.googleapis.com/auth/drive"))
                    .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            requestFactory = new NetHttpTransport().createRequestFactory((HttpRequest request) -> {
                credential.initialize(request);
                request.setParser(new JsonObjectParser(jsonFactory));
            });

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося ініціалізувати GoogleDriveService", e);
        }
    }

    /** Створює папку або повертає існуючу */
    public String getOrCreateFolder(String parentId, String folderName) throws Exception {
        String query = String.format(
                "mimeType='application/vnd.google-apps.folder' and trashed=false and '%s' in parents and name='%s'",
                parentId, folderName
        );
        GenericUrl url = new GenericUrl(DRIVE_FILES_URL + "?q=" + java.net.URLEncoder.encode(query, "UTF-8"));
        HttpRequest request = requestFactory.buildGetRequest(url);
        HttpResponse response = request.execute();
        Map<?, ?> result = response.parseAs(Map.class);
        List<?> files = (List<?>) result.get("files");
        if (files != null && !files.isEmpty()) {
            Map<?, ?> first = (Map<?, ?>) files.get(0);
            return (String) first.get("id");
        }

        // Створюємо папку
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", folderName);
        metadata.put("mimeType", "application/vnd.google-apps.folder");
        if (parentId != null) {
            metadata.put("parents", Collections.singletonList(parentId));
        }

        HttpContent content = new JsonHttpContent(jsonFactory, metadata);
        HttpRequest createRequest = requestFactory.buildPostRequest(new GenericUrl(DRIVE_FILES_URL), content);
        HttpResponse createResponse = createRequest.execute();
        Map<?, ?> createdFolder = createResponse.parseAs(Map.class);
        return (String) createdFolder.get("id");
    }

    /** Завантаження файлу */
    public void uploadFile(String folderId, InputStream fileContent, String fileName, String mimeType) throws Exception {
        if (fileContent == null || fileName == null || mimeType == null) {
            throw new IllegalArgumentException("fileContent, fileName and mimeType must not be null");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", fileName);
        if (folderId != null) {
            metadata.put("parents", Collections.singletonList(folderId));
        }

        String metadataJson = new Gson().toJson(metadata);
        HttpContent metadataPart = new ByteArrayContent("application/json; charset=UTF-8", metadataJson.getBytes(StandardCharsets.UTF_8));
        HttpContent filePart = new InputStreamContent(mimeType, fileContent);

        MultipartContent multipartContent = new MultipartContent();
        multipartContent.setMediaType(new HttpMediaType("multipart/related").setParameter("boundary", "foo_bar_baz"));
        multipartContent.addPart(new MultipartContent.Part(new HttpHeaders(), metadataPart));
        multipartContent.addPart(new MultipartContent.Part(new HttpHeaders(), filePart));

        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(DRIVE_UPLOAD_URL), multipartContent);
        request.getHeaders().setContentType("multipart/related; boundary=foo_bar_baz");

        HttpResponse response = request.execute();
        System.out.println("Файл завантажено: " + response.parseAsString());
    }

    /** Генерує останні 5 дат уроків для групи */
    public List<String> getLastFiveLessonDates(String day1, String day2, String time1, String time2) {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        LocalDate today = LocalDate.now();
        int count = 0;
        int safety = 0;

        Map<String, DayOfWeek> dayMap = Map.of(
                "Пн", DayOfWeek.MONDAY,
                "Вт", DayOfWeek.TUESDAY,
                "Ср", DayOfWeek.WEDNESDAY,
                "Чт", DayOfWeek.THURSDAY,
                "Пт", DayOfWeek.FRIDAY,
                "Сб", DayOfWeek.SATURDAY,
                "Нд", DayOfWeek.SUNDAY
        );

        DayOfWeek d1 = (day1 != null) ? dayMap.get(day1.trim()) : null;
        DayOfWeek d2 = (day2 != null) ? dayMap.get(day2.trim()) : null;

        if (d1 == null && d2 == null) {
            System.out.println("⚠️ Невідомі дні: day1=" + day1 + ", day2=" + day2);
            return dates;
        }

        while (count < 5 && safety < 365) {
            DayOfWeek dow = today.getDayOfWeek();
            if (dow.equals(d1)) {
                dates.add(today.format(dateFormatter) + " " + time1);
                count++;
            } else if (dow.equals(d2)) {
                dates.add(today.format(dateFormatter) + " " + time2);
                count++;
            }
            today = today.minusDays(1);
            safety++;
        }

        Collections.reverse(dates);
        System.out.println("Згенеровані дати: " + dates);
        return dates;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }
}


