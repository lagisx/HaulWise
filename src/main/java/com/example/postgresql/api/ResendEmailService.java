package com.example.postgresql.API;

import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResendEmailService {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final OkHttpClient http = new OkHttpClient();
    private final String apiKey;
    private final String fromAddress;

    private static ResendEmailService instance;
    public static ResendEmailService getInstance() {
        if (instance == null) instance = new ResendEmailService();
        return instance;
    }

    private ResendEmailService() {
        this.apiKey      = AppConfig.getInstance().getResendApiKey();
        this.fromAddress = AppConfig.getInstance().getResendFrom();
    }

    

    public CompletableFuture<Boolean> sendOtpCode(String toEmail, String code) {
        String subject = "Код сброса пароля — Биржа грузоперевозок Haulwise";
        String html = buildOtpEmail(code);
        return sendEmail(toEmail, subject, html);
    }

    

    public CompletableFuture<Boolean> sendEmail(String to, String subject, String html) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonObject body = new JsonObject();
        body.addProperty("from",    fromAddress);
        body.addProperty("to",      to);
        body.addProperty("subject", subject);
        body.addProperty("html",    html);

        Request req = new Request.Builder()
                .url(RESEND_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(),
                        MediaType.parse("application/json")))
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Resend] Ошибка сети: " + e.getMessage());
                future.complete(false);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String responseBody = r.body() != null ? r.body().string() : "";
                System.out.println("[Resend] HTTP " + r.code() + ": " + responseBody);
                future.complete(r.isSuccessful());
                r.close();
            }
        });

        return future;
    }

    

    private String buildOtpEmail(String code) {
        try (var is = getClass().getResourceAsStream("/otp_email.html")) {
            if (is == null) {
                System.err.println("[ResendEmailService] otp_email.html не найден!");
                return "<p>Ваш код: <b>" + code + "</b></p>";
            }
            String template = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            return template.replace("{{CODE}}", code);
        } catch (Exception e) {
            System.err.println("[ResendEmailService] Ошибка шаблона: " + e.getMessage());
            return "<p>Ваш код: <b>" + code + "</b></p>";
        }
    }
}