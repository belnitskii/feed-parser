package com.belnitskii.feedparser;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@Component
public class FeedReader {
    private final Logger logger = LoggerFactory.getLogger(FeedReader.class);
    @Getter
    private final Map<String, Double> userKeywords = UserKeywordsManager.loadKeywords();

    @Getter
    private List<PostData> cachedPosts = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            logger.info("Init post cache...");
            refreshCache();
        } catch (IOException e) {
            logger.error("Error while loading feed initially", e);
        }
    }

    public List<PostData> readAll() {
        List<SyndEntry> allEntries = new ArrayList<>();
        List<PostData> result = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (String feedUrl : FeedSources.FEED_URLS) {
            try {
                logger.info("Reading feed: {}", feedUrl);

                URLConnection connection = new URL(feedUrl).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

                try (InputStream is = connection.getInputStream();
                     XmlReader reader = new XmlReader(is)) {

                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(reader);
                    allEntries.addAll(feed.getEntries());

                    logger.info("Read {} records out of {}", feed.getEntries().size(), feedUrl);
                }
            } catch (Exception e) {
                logger.error("Error reading feed: {}", feedUrl, e);
            }
        }

        for (SyndEntry entry : allEntries) {
            String articleUrl = entry.getLink();

            if (seenUrls.contains(articleUrl)) {
                logger.debug("Repeat post, skip: {}", articleUrl);
                continue;
            }
            seenUrls.add(articleUrl);

            try {
                logger.debug("Processing post: {}", articleUrl);
                Document doc = Jsoup.connect(articleUrl)
                        .timeout(5000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .ignoreHttpErrors(true)
                        .get();
                String text = doc.text();
                List<String> keywords = extractKeywords(text);

                double score = isEnglishKeywords(keywords) ? 0 : getScore(keywords);

                logger.debug("Keyword extracted: {} | Score: {}", keywords.size(), score);

                PostData post = new PostData(
                        entry.getTitle(),
                        articleUrl,
                        entry.getPublishedDate(),
                        keywords,
                        score
                );
                result.add(post);
            } catch (Exception e) {
                logger.error("Error processing post: {}", articleUrl, e);
            }
        }

        logger.info("Total successfully processed {} unique posts", result.size());
        return result;
    }

    private List<String> extractKeywords(String text) {
        return Utils.extractKeywords(text, 15);
    }

    private double getScore(List<String> postKeywords) {
        double score = 0;
        StringBuilder allKeywordsLog = new StringBuilder("Keywords: ");
        StringBuilder weightedKeywordsLog = new StringBuilder("Weighted keywords: ");

        for (String keyword : postKeywords) {
            allKeywordsLog.append(keyword).append(" ");
            if (userKeywords.containsKey(keyword)) {
                double weight = userKeywords.get(keyword);
                score += weight;
                weightedKeywordsLog.append(String.format("[%s: %.2f] ", keyword, weight));
            }
        }

        logger.debug(allKeywordsLog.toString());
        if (score > 0) {
            logger.debug(weightedKeywordsLog.toString());
        }

        return score;
    }

    public void refreshCache() throws IOException {
        logger.info("Updating post cache...");
        this.cachedPosts = readAll();
        logger.info("Cache updated: {} posts", cachedPosts.size());
    }

    private boolean isEnglishKeywords(List<String> keywords) {
        int english = 0;
        for (String word : keywords) {
            if (word.matches("^[a-z]{3,}$")) english++;
        }
        double ratio = (double) english / keywords.size();
        return ratio > 0.8;
    }
}
