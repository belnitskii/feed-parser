package com.belnitskii.feedparser.bot;

import com.belnitskii.feedparser.FeedService;
import com.belnitskii.feedparser.PostData;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class TelegramBot extends Executor {
    private final Map<Long, Map<String, PostData>> sentPosts = new HashMap<>();
    private final BotConfig botConfig;
    private final FeedService feedService;

    public TelegramBot(BotConfig botConfig, FeedService feedService) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.feedService = feedService;
        System.out.println("bot started");
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (Objects.equals(messageText, "/start")) {
                System.out.println("hi");
                sendMessageWithKeyboard(chatId, "Привет! Выберите действие:");
            }

            else if (Objects.equals(messageText, "FEED")) {
                sendMessage(chatId, "Сейчас попробую пукнуть топ 5 постов");
                try {
                    for (PostData postData : feedService.getTop5()) {
                        sendPostWithRatingButtons(chatId, postData);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("LIKE_") || callbackData.startsWith("DISLIKE_") || callbackData.startsWith("OKAY_")) {
                String rate = null;
                if (callbackData.startsWith("LIKE_")){
                    rate = "like";
                }
                if (callbackData.startsWith("DISLIKE_")){
                    rate = "dislike";
                }
                if (callbackData.startsWith("OKAY_")){
                    rate = "okay";
                }

                String postId = callbackData.substring(callbackData.indexOf("_") + 1);

                PostData matchedPost = Optional.ofNullable(sentPosts.get(chatId))
                        .map(map -> map.get(postId))
                        .orElse(null);

                if (matchedPost != null) {
                    feedService.rate(matchedPost, rate);
                    editMessage(update, "Спасибо за оценку! " + matchedPost.getTitle());
                } else {
                    editMessage(update, "Пост не найден.");
                }
            }
        }
    }

    private void editMessage(Update update, String newText) {
        EditMessageText editMessage = new EditMessageText();
        Message originalMessage = (Message) update.getCallbackQuery().getMessage();
        editMessage.setChatId(originalMessage.getChatId());
        editMessage.setMessageId(originalMessage.getMessageId());
        editMessage.setText(newText);
        executeSafely(editMessage);
    }

    private void sendMessageWithKeyboard(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        ReplyKeyboardMarkup keyboard = createReplyKeyboard();
        message.setReplyMarkup(keyboard);
        executeSafely(message);
    }

    private void sendPostWithRatingButtons(long chatId, PostData postData) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(postData.getUrl());

        String postId = DigestUtils.md5DigestAsHex(postData.getUrl().getBytes(StandardCharsets.UTF_8));
        sentPosts.computeIfAbsent(chatId, k -> new HashMap<>()).put(postId, postData);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton likeButton = new InlineKeyboardButton("👍 Like");
        likeButton.setCallbackData("LIKE_" + postId);

        InlineKeyboardButton okayButton = new InlineKeyboardButton("😑 Okay");
        okayButton.setCallbackData("OKAY_" + postId);

        InlineKeyboardButton dislikeButton = new InlineKeyboardButton("👎 Dislike");
        dislikeButton.setCallbackData("DISLIKE_" + postId);

        rows.add(List.of(likeButton, okayButton, dislikeButton));
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeSafely(message);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        executeSafely(message);
    }

    public ReplyKeyboardMarkup createReplyKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("FEED"));
        keyboardRows.add(row1);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}
