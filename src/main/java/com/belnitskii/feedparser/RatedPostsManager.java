package com.belnitskii.feedparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class RatedPostsManager {

    private final File file = new File("src/main/resources/rated_posts.json");
    private final ObjectMapper mapper = new ObjectMapper();
    private Set<String> ratedUrls = new HashSet<>();

    public RatedPostsManager() {
        loadRatedUrls();
    }

    private void loadRatedUrls() {
        if (file.exists()) {
            try {
                String[] urls = mapper.readValue(file, String[].class);
                ratedUrls = new HashSet<>(Set.of(urls));
            } catch (IOException e) {
                System.err.println("Error loading rated_posts.json");
                e.printStackTrace();
            }
        }
    }

    public void saveRatedUrls() {
        try {
            mapper.writeValue(file, ratedUrls);
        } catch (IOException e) {
            System.err.println("Error saving rated_posts.json");
            e.printStackTrace();
        }
    }

    public boolean isRated(String url) {
        return ratedUrls.contains(url);
    }

    public void markAsRated(String url) {
        ratedUrls.add(url);
        saveRatedUrls();
    }
}
