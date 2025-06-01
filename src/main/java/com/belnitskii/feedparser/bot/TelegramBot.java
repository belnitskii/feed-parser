package com.belnitskii.feedparser.bot;

import com.belnitskii.feedparser.FeedService;
import com.belnitskii.feedparser.PostData;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

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
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    public static String escapeMarkdown(String text) {
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (Objects.equals(messageText, "/start")) {
                sendMessageWithKeyboard(chatId, "Hello, press the FEED button");
            }

            else if (Objects.equals(messageText, "FEED")) {
                sendMessage(chatId, "Trying to send top 5 posts");
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

            if (callbackData.startsWith("INSTAPAPER_")) {
                String postId = callbackData.substring(callbackData.indexOf("_") + 1);

                Optional.ofNullable(sentPosts.get(chatId))
                        .map(map -> map.get(postId))
                        .ifPresent(postData -> {
                            boolean success = feedService.sendInstapaper(postData);
                            if (success) {
                                sendMessageNoPreview(chatId, "Link sent to Instapaper\\!\n[" + escapeMarkdown(postData.getTitle()) + "](" + postData.getUrl() + ")", true);
                            } else {
                                logger.warn("Failed to save link for chatId={} postId={}", chatId, postId);
                                sendMessage(chatId, "Failed to save link to Instapaper.\n" +
                                        "Check login/password or try again later");
                            }
                        });
            }

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
                    assert rate != null;
                    feedService.rate(matchedPost, rate);
                    editMessage(update, "Thanks for your rating! " + matchedPost.getTitle());
                } else {
                    editMessage(update, "Post not found.");
                }
            }
        }
    }

    public void sendPostWithRatingButtons(long chatId, PostData postData) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode(ParseMode.MARKDOWNV2);
        String title = escapeMarkdown(postData.getTitle());
        message.setText("[" + title + "]" + "(" + postData.getUrl() + ")");

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

        InlineKeyboardButton instapaperButton = new InlineKeyboardButton("Instapaper");
        instapaperButton.setCallbackData("INSTAPAPER_" + postId);

        rows.add(List.of(likeButton, okayButton, dislikeButton));
        rows.add(List.of(instapaperButton));

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

    private void sendMessageNoPreview(long chatId, String text, boolean markdown) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setDisableWebPagePreview(true);
        if (markdown) {
            message.setParseMode(ParseMode.MARKDOWNV2);
        }

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
