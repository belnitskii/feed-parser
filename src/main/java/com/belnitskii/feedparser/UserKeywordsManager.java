package com.belnitskii.feedparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserKeywordsManager {
    private static final String FILE_PATH = "src/main/resources/user_keywords.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Double> loadKeywords() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(file, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public  void saveKeywords(Map<String, Double> keywords) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), keywords);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
