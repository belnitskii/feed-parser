package com.belnitskii.feedparser;

import com.github.demidko.aot.WordformMeaning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Utils {
    private static final Set<String> STOP_WORDS = loadStopWords();

    public static Set<String> loadStopWords() {
        try {
            Path path = Paths.get("data/stopwords-ru.txt");
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load stop words", e);
        }
    }

    public static List<String> extractKeywords(String text, int limit) {
        text = text.toLowerCase().replaceAll("[^a-zа-яё\\s]", " ");
        if (text.length() > 5000) {
            text = text.substring(0, 5000);
        }
        String[] words = text.split("\\s+");

        Map<String, Integer> frequencyMap = Arrays.stream(words)
                .filter(word -> word.length() > 2 && !STOP_WORDS.contains(word))
                .map(word -> {
                    try {
                        List<WordformMeaning> meanings = lookupForMeanings(word);
                        return meanings.isEmpty() ? word : meanings.get(0).getLemma().toString();
                    } catch (IOException e) {
                        return word;
                    }
                })
                .filter(lemma -> lemma.length() > 2 && !STOP_WORDS.contains(lemma))
                .collect(Collectors.toConcurrentMap(
                        lemma -> lemma,
                        lemma -> 1,
                        Integer::sum
                ));

        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}