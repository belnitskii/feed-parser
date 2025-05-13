package com.belnitskii.feedparser;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FeedScheduler {

    private final FeedReader feedReader;

    public FeedScheduler(FeedReader feedReader) {
        this.feedReader = feedReader;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateFeedCache() {
        try {
            feedReader.refreshCache();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
