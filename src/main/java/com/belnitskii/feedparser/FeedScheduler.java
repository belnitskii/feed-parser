package com.belnitskii.feedparser;

import com.belnitskii.feedparser.bot.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class FeedScheduler {
    private final Logger logger = LoggerFactory.getLogger(FeedScheduler.class);

    private final TelegramBot telegramBot;
    private final FeedReader feedReader;
    private final FeedService feedService;

    @Value("${telegram.id}")
    private long targetChatId;

    public FeedScheduler(TelegramBot telegramBot, FeedReader feedReader, FeedService feedService) {
        this.telegramBot = telegramBot;
        this.feedReader = feedReader;
        this.feedService = feedService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void updateFeedCache() {
        try {
            logger.info("Scheduler triggered updateFeedCache() at {}", LocalDateTime.now());
            feedReader.refreshCache();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 52 4 * * *")
    public void sendScheduledPosts() {
        logger.info("Scheduler triggered sendScheduledPosts() at {}", LocalDateTime.now());

        try {
            for (PostData postData : feedService.getTop5()) {
                telegramBot.sendPostWithRatingButtons(targetChatId, postData);
            }
        } catch (IOException e) {
            logger.error("Error sending scheduled posts", e);
        }
    }
}
