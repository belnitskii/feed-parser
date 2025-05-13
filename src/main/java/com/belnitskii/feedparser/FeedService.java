package com.belnitskii.feedparser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedService {
    static final Logger logger = LoggerFactory.getLogger(FeedService.class);


    double like = 2.0;
    double dislike = -3.0;
    double okay = 0.25;

    private final UserKeywordsManager keywordsManager;
    private final FeedReader feedReader;
    private final RatedPostsManager ratedPostsManager;

    public FeedService(UserKeywordsManager keywordsManager, FeedReader feedReader, RatedPostsManager ratedPostsManager) {
        this.keywordsManager = keywordsManager;
        this.feedReader = feedReader;
        this.ratedPostsManager = ratedPostsManager;
    }

    public List<PostData> getTop5() throws IOException {
        List<PostData> top5 = feedReader.getCachedPosts().stream()
                .filter(post -> !ratedPostsManager.isRated(post.getUrl()))
                .sorted(Comparator.comparingDouble(PostData::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());

        logger.info("Top 5 posts selected (by score):");
        for (PostData post : top5) {
            logger.info("Title: {}, Score: {}, URL: {}", post.getTitle(), post.getScore(), post.getUrl());
        }

        return top5;
    }

    public void rate(PostData postData, String rate) {
        double ratePost = switch (rate) {
            case "like" -> like;
            case "dislike" -> dislike;
            case "okay" -> okay;
            default -> {
                logger.warn("Unknown rating: {}", rate);
                yield 0.0;
            }
        };

        logger.info("Rating post: {} | Rate: {} | Score Modifier: {}", postData.getTitle(), rate, ratePost);

        Map<String, Double> userKeywords = feedReader.getUserKeywords();

        for (String keyword : postData.getKeywords()) {
            double previous = userKeywords.getOrDefault(keyword, 0.0);
            double updated = previous + ratePost;
            userKeywords.put(keyword, updated);
            logger.debug("Updated keyword score: '{}' from {} to {}", keyword, previous, updated);
        }

        keywordsManager.saveKeywords(userKeywords);
        ratedPostsManager.markAsRated(postData.getUrl());
        logger.info("Post marked as rated: {}", postData.getUrl());
    }
}
