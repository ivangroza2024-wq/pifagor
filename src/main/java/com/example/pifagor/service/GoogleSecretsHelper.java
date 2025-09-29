package com.example.pifagor.service;

import java.io.FileWriter;
import java.io.IOException;

public class GoogleSecretsHelper {
    public static void createClientSecretFile() throws IOException {
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

        String jsonContent = "{\n" +
                "  \"installed\": {\n" +
                "    \"client_id\": \"" + clientId + "\",\n" +
                "    \"project_id\": \"poetic-primer-472117-t2\",\n" +
                "    \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "    \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "    \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "    \"client_secret\": \"" + clientSecret + "\",\n" +
                "    \"redirect_uris\": [\"http://localhost\"]\n" +
                "  }\n" +
                "}";

        try (FileWriter file = new FileWriter("src/main/resources/client_secret.json")) {
            file.write(jsonContent);
        }
    }
}
