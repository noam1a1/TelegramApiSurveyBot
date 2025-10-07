package org.example.service;

import org.example.model.ChatGPTResponse;
import org.example.util.Constants;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ChatGPTService {

    private final String baseUrl = Constants.API_BASE_URL;
    private final String id = Constants.USER_ID;
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public ChatGPTResponse checkBalance() {
        String url = baseUrl + "check-balance?id=" + enc(id);
        return sendGet(url);
    }

    public ChatGPTResponse clearHistory() {
        String url = baseUrl + "clear-history?id=" + enc(id);
        return sendGet(url);
    }

    public ChatGPTResponse sendMessage(String text) {
        String url = baseUrl + "send-message?id=" + enc(id) + "&text=" + enc(text);
        return sendGet(url);
    }

    private ChatGPTResponse sendGet(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/xml")
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404 && !url.endsWith("/")) {
                String retryUrl = url + "/";
                HttpRequest retry = HttpRequest.newBuilder()
                        .uri(URI.create(retryUrl))
                        .header("Accept", "application/xml")
                        .GET()
                        .build();
                res = client.send(retry, HttpResponse.BodyHandlers.ofString());
            }
            return parseResponse(res);
        } catch (Exception e) {
            return new ChatGPTResponse(false, "LOCAL_EXCEPTION", e.getMessage());
        }
    }

    private ChatGPTResponse sendPost(String url, String form) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Accept", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404 && !url.endsWith("/")) {
                String retryUrl = url + "/";
                HttpRequest retry = HttpRequest.newBuilder()
                        .uri(URI.create(retryUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .header("Accept", "application/xml")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                res = client.send(retry, HttpResponse.BodyHandlers.ofString());
            }
            return parseResponse(res);
        } catch (Exception e) {
            return new ChatGPTResponse(false, "LOCAL_EXCEPTION", e.getMessage());
        }
    }

    private ChatGPTResponse parseResponse(HttpResponse<String> httpRes) {
        String body = httpRes.body();
        if (body == null || body.isBlank()) {
            return new ChatGPTResponse(false, "EMPTY_BODY", "HTTP " + httpRes.statusCode());
        }
        boolean success = "true".equalsIgnoreCase(extractTag(body, "success"));
        String errorCode = extractTag(body, "errorCode");
        String extra = extractTag(body, "extra");
        if (!success && httpRes.statusCode() >= 400 && errorCode == null) {
            errorCode = "HTTP_" + httpRes.statusCode();
        }
        return new ChatGPTResponse(success, errorCode, extra);
    }

    private String extractTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        int j = xml.indexOf(close);
        if (i >= 0 && j > i) {
            return xml.substring(i + open.length(), j).trim();
        }
        String selfClosing = "<" + tag + "/>";
        if (xml.contains(selfClosing)) {
            return "";
        }
        return null;
    }

    private String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
