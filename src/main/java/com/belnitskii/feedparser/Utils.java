package com.belnitskii.feedparser;

import com.github.demidko.aot.WordformMeaning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Utils {

    private static final Set<String> STOP_WORDS = loadStopWords();

    public static Set<String> loadStopWords() {
        try (InputStream is = Utils.class.getResourceAsStream("/stopwords-ru.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toSet());
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Не удалось загрузить стоп-слова", e);
        }
    }

    public static List<String> extractKeywords(String text, int limit) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        text = text.toLowerCase().replaceAll("[^a-zа-яё\\s]", " ");
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.length() <= 2 || STOP_WORDS.contains(word)) continue;

            try {
                List<WordformMeaning> meanings = lookupForMeanings(word);
                String lemma;
                if (!meanings.isEmpty()) {
                    lemma = meanings.get(0).getLemma().toString();
                } else {
                    lemma = word;
                }
                frequencyMap.put(lemma, frequencyMap.getOrDefault(lemma, 0) + 1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
