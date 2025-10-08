package com.example.pifagor.service;

import java.io.FileWriter;
import java.io.IOException;

public class GoogleSecretsHelper {
    public static void createClientSecretFile() throws IOException {
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

        String jsonContent = "{\n" +
                "  \"web\": {\n" +
                "    \"client_id\": \"" + clientId + "\",\n" +
                "    \"project_id\": \"vital-victor-474511-g6\",\n" +
                "    \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "    \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "    \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "    \"client_secret\": \"" + clientSecret + "\",\n" +
                "    \"redirect_uris\": [\"https://pifagor.onrender.com\"]\n" +
                "  }\n" +
                "}";


        try (FileWriter file = new FileWriter("src/main/resources/client_secret.json")) {
            file.write(jsonContent);
        }
    }
}
