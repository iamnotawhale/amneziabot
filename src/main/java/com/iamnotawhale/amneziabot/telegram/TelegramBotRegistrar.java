package com.iamnotawhale.amneziabot.telegram;

import com.iamnotawhale.amneziabot.config.TelegramProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBotRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotRegistrar.class);

    private final TelegramProperties telegramProperties;
    private final AmneziaTelegramBot amneziaTelegramBot;

    public TelegramBotRegistrar(TelegramProperties telegramProperties, AmneziaTelegramBot amneziaTelegramBot) {
        this.telegramProperties = telegramProperties;
        this.amneziaTelegramBot = amneziaTelegramBot;
    }

    @PostConstruct
    public void registerBot() {
        if (!telegramProperties.isEnabled()) {
            log.info("Telegram bot disabled");
            return;
        }
        if (telegramProperties.getBotToken() == null || telegramProperties.getBotToken().isBlank()) {
            log.warn("Telegram bot token is empty, bot will not start");
            return;
        }
        if (telegramProperties.getBotUsername() == null || telegramProperties.getBotUsername().isBlank()) {
            log.warn("Telegram bot username is empty, bot will not start");
            return;
        }
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(amneziaTelegramBot);
            log.info("Telegram bot started: {}", telegramProperties.getBotUsername());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to register telegram bot", exception);
        }
    }
}
