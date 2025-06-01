package com.belnitskii.feedparser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class InstapaperService {

    private static final Logger logger = LoggerFactory.getLogger(InstapaperService.class);

    @Value("${instapaper.email}")
    private String email;

    @Value("${instapaper.password}")
    private String password;

    public boolean saveUrlToInstapaper(String urlToSave) {
        logger.info("Save the link in Instapaper: {}", urlToSave);

        try {
            HttpClient client = HttpClient.newHttpClient();

            String auth = email + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String body = "url=" + URLEncoder.encode(urlToSave, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.instapaper.com/api/add"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "curl/8.7.1")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                logger.info("Successfully added to Instapaper");
                return true;
            } else {
                logger.warn("Failed to add to Instapaper. Code: {}, Response: {}", response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("Exception occurred while trying to add to Instapaper", e);
            return false;
        }
    }
}
