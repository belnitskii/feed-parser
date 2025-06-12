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
        logger.info("Attempting to save link to Instapaper: {}", urlToSave);

        try {
            logger.debug("Creating HTTP client");
            HttpClient client = HttpClient.newHttpClient();

            String auth = email + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            logger.debug("Encoded Auth: Basic {}", maskBase64(encodedAuth));

            String body = "url=" + URLEncoder.encode(urlToSave, StandardCharsets.UTF_8);
            logger.debug("Request Body: {}", body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.instapaper.com/api/add"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "curl/8.7.1")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            logger.debug("Sending HTTP request to Instapaper...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Received response from Instapaper. Status code: {}, Body: {}", response.statusCode(), response.body());

            if (response.statusCode() == 201) {
                logger.info("Successfully added to Instapaper");
                return true;
            } else {
                logger.warn("Failed to add to Instapaper. Status: {}, Response: {}", response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("Exception occurred while trying to connect to Instapaper", e);
            return false;
        }
    }

    private String maskBase64(String encodedAuth) {
        if (encodedAuth.length() <= 6) return "***";
        return encodedAuth.substring(0, 3) + "..." + encodedAuth.substring(encodedAuth.length() - 3);
    }
}
