package com.belnitskii.feedparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserKeywordsManager {
    private static final Logger logger = LoggerFactory.getLogger(UserKeywordsManager.class);
    private static final String FILE_PATH = "data/user_keywords.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Double> loadKeywords() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            logger.warn("File with keywords not found: {}. Empty map returned.", FILE_PATH);
            return new HashMap<>();
        }

        try {
            Map<String, Double> keywords = mapper.readValue(file, Map.class);
            logger.info("Keywords successfully loaded. Total: {}", keywords.size());
            if (!keywords.isEmpty()) {
                keywords.forEach((k, v) -> logger.debug("Keyword: '{}' with weight: {}", k, v));
            }
            return keywords;
        } catch (IOException e) {
            logger.error("Error loading keywords from file: {}", FILE_PATH, e);
            return new HashMap<>();
        }
    }

    public void saveKeywords(Map<String, Double> keywords) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), keywords);
            logger.info("Keywords saved successfully. Total: {}", keywords.size());
        } catch (IOException e) {
            logger.error("Error saving keywords to file: {}", FILE_PATH, e);
        }
    }
}
