package com.belnitskii.feedparser.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class Executor extends TelegramLongPollingBot {
    static final Logger logger = LoggerFactory.getLogger(Executor.class);

    public Executor(String botToken) {
        super(botToken);
    }

    public void executeSafely(Object method) {
        try {
            switch (method) {
                case BotApiMethod<?> botApiMethod -> execute(botApiMethod);
                case SendPhoto sendPhoto -> execute(sendPhoto);
                case SendSticker sendSticker -> execute(sendSticker);
                case SendDocument sendDocument -> execute(sendDocument);
                case null, default -> {
                    assert method != null;
                    logger.error("Unknown command type: {}", method.getClass().getSimpleName());
                }
            }
        } catch (TelegramApiException e) {
            logger.error("Command execution error: {}", method.getClass().getSimpleName(), e);
        }
    }
}
