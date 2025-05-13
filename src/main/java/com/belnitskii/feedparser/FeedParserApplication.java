package com.belnitskii.feedparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeedParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedParserApplication.class, args);
    }
}
