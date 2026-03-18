package com.iamnotawhale.amneziabot.telegram;

import com.iamnotawhale.amneziabot.api.dto.PlanResponse;
import com.iamnotawhale.amneziabot.api.dto.SubscriptionResponse;
import com.iamnotawhale.amneziabot.config.TelegramProperties;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TelegramMessageService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");
    private final TelegramProperties telegramProperties;

    public TelegramMessageService(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
    }

    public String botToken() {
        return telegramProperties.getBotToken();
    }

    public String botUsername() {
        return telegramProperties.getBotUsername();
    }

    public String resolveUsername(Message message) {
        if (message.getFrom().getUserName() != null && !message.getFrom().getUserName().isBlank()) {
            return message.getFrom().getUserName();
        }
        return message.getFrom().getFirstName() != null ? message.getFrom().getFirstName() : "user";
    }

    public String startMessage() {
        return "AmneziaBot команды:\n"
                + "/plans - список тарифов\n"
                + "/trial - выдать бесплатный тест (1 раз)\n"
                + "/activate PLAN_CODE - активировать тариф\n"
                + "/key - показать активный ключ\n"
                + "\nПример: /activate GB200_30D";
    }

    public String plansMessage(List<PlanResponse> plans) {
        StringBuilder builder = new StringBuilder("Доступные тарифы:\n\n");
        for (PlanResponse plan : plans) {
            builder.append(plan.code())
                    .append(" — ")
                    .append(plan.name())
                    .append("\nСрок: ")
                    .append(formatDuration(plan.durationDays()))
                    .append(", Устройств: ")
                    .append(formatDeviceLimit(plan.deviceLimit()))
                    .append("\nТрафик: ")
                    .append(plan.trafficLimitBytes() == null ? "безлимит" : toGigabytes(plan.trafficLimitBytes()) + " GB")
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    public String subscriptionIssuedMessage(SubscriptionResponse response) {
        return "Подписка активирована: " + response.planCode() + "\n"
                + "Действует до: " + formatEndDate(response.endsAt()) + "\n"
                + "Лимит устройств: " + formatDeviceLimit(response.deviceLimit()) + "\n"
                + "Ключ:\n" + response.vlessLink();
    }

    public String currentKeyMessage(SubscriptionResponse response) {
        return "Ваш активный тариф: " + response.planCode() + "\n"
                + "До: " + formatEndDate(response.endsAt()) + "\n"
                + "Трафик: " + (response.trafficLimitBytes() == null
                ? "безлимит"
                : toGigabytes(response.trafficUsedBytes()) + " / " + toGigabytes(response.trafficLimitBytes()) + " GB")
                + "\nКлюч:\n" + response.vlessLink();
    }

    private String formatDuration(Integer durationDays) {
        return durationDays == null ? "без ограничений" : durationDays + " дн.";
    }

    private String formatDeviceLimit(Integer deviceLimit) {
        return deviceLimit == null ? "без ограничений" : String.valueOf(deviceLimit);
    }

    private String formatEndDate(java.time.OffsetDateTime endsAt) {
        return endsAt == null ? "без ограничений" : DATE_TIME_FORMATTER.format(endsAt.atZoneSameInstant(ZoneOffset.UTC));
    }

    private long toGigabytes(long bytes) {
        return bytes / 1024 / 1024 / 1024;
    }
}
