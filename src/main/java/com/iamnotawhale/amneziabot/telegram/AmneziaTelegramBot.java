package com.iamnotawhale.amneziabot.telegram;

import com.iamnotawhale.amneziabot.api.dto.PlanResponse;
import com.iamnotawhale.amneziabot.api.dto.SubscriptionResponse;
import com.iamnotawhale.amneziabot.service.BadRequestException;
import com.iamnotawhale.amneziabot.service.NotFoundException;
import com.iamnotawhale.amneziabot.service.PlanService;
import com.iamnotawhale.amneziabot.service.SubscriptionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AmneziaTelegramBot extends TelegramLongPollingBot {

    private static final String CALLBACK_PLANS = "MENU_PLANS";
    private static final String CALLBACK_TRIAL = "MENU_TRIAL";
    private static final String CALLBACK_KEY = "MENU_KEY";
    private static final String CALLBACK_BACK = "MENU_BACK";
    private static final String CALLBACK_ACTIVATE_REQUEST_PREFIX = "ACT_REQ:";
    private static final String CALLBACK_ACTIVATE_CONFIRM_PREFIX = "ACT_OK:";
    private static final String CALLBACK_ACTIVATE_CANCEL = "ACT_CANCEL";
    private static final Duration ACTIVATE_COOLDOWN = Duration.ofSeconds(3);

    private final TelegramMessageService telegramMessageService;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final Map<Long, Instant> lastActivationAt = new ConcurrentHashMap<>();

    public AmneziaTelegramBot(
            TelegramMessageService telegramMessageService,
            PlanService planService,
            SubscriptionService subscriptionService
    ) {
        super(telegramMessageService.botToken());
        this.telegramMessageService = telegramMessageService;
        this.planService = planService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public String getBotUsername() {
        return telegramMessageService.botUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            onCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();
        String text = message.getText().trim();
        Long chatId = message.getChatId();
        Long telegramId = message.getFrom().getId();
        String username = telegramMessageService.resolveUsername(message);

        try {
            if (text.startsWith("/start") || text.startsWith("/help")) {
                send(chatId, telegramMessageService.startMessage(), mainMenuKeyboard());
                return;
            }
            if (text.startsWith("/plans")) {
                List<PlanResponse> plans = planService.listPlans();
                send(chatId, telegramMessageService.plansMessage(plans), plansKeyboard(plans));
                return;
            }
            if (text.startsWith("/trial")) {
                SubscriptionResponse response = subscriptionService.issueTrial(telegramId, username);
                send(chatId, telegramMessageService.subscriptionIssuedMessage(response), mainMenuKeyboard());
                return;
            }
            if (text.startsWith("/key")) {
                SubscriptionResponse response = subscriptionService.getActiveSubscription(telegramId);
                send(chatId, telegramMessageService.currentKeyMessage(response), mainMenuKeyboard());
                return;
            }
            if (text.startsWith("/activate")) {
                String[] parts = text.split("\\s+");
                if (parts.length < 2) {
                    send(chatId, "Формат: /activate PLAN_CODE\\nПример: /activate GB200_30D");
                    return;
                }
                SubscriptionResponse response = subscriptionService.activatePlan(telegramId, username, parts[1].trim());
                send(chatId, telegramMessageService.subscriptionIssuedMessage(response), mainMenuKeyboard());
                return;
            }
            send(chatId, "Неизвестная команда. Используйте /help", mainMenuKeyboard());
        } catch (BadRequestException | NotFoundException exception) {
            send(chatId, exception.getMessage(), mainMenuKeyboard());
        } catch (Exception exception) {
            send(chatId, "Ошибка сервера. Попробуйте позже.", mainMenuKeyboard());
        }
    }

    private void send(Long chatId, String text) {
        send(chatId, text, null);
    }

    private void send(Long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException ignored) {
        }
    }

    private void onCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Long telegramId = callbackQuery.getFrom().getId();
        String username = callbackQuery.getFrom().getUserName() != null && !callbackQuery.getFrom().getUserName().isBlank()
                ? callbackQuery.getFrom().getUserName()
            : (callbackQuery.getFrom().getFirstName() != null ? callbackQuery.getFrom().getFirstName() : "user");
        String data = callbackQuery.getData();

        try {
            if (CALLBACK_BACK.equals(data)) {
                send(chatId, telegramMessageService.startMessage(), mainMenuKeyboard());
                return;
            }
            if (CALLBACK_PLANS.equals(data)) {
                List<PlanResponse> plans = planService.listPlans();
                send(chatId, telegramMessageService.plansMessage(plans), plansKeyboard(plans));
                return;
            }
            if (CALLBACK_TRIAL.equals(data)) {
                SubscriptionResponse response = subscriptionService.issueTrial(telegramId, username);
                send(chatId, telegramMessageService.subscriptionIssuedMessage(response), mainMenuKeyboard());
                return;
            }
            if (CALLBACK_KEY.equals(data)) {
                SubscriptionResponse response = subscriptionService.getActiveSubscription(telegramId);
                send(chatId, telegramMessageService.currentKeyMessage(response), mainMenuKeyboard());
                return;
            }
            if (CALLBACK_ACTIVATE_CANCEL.equals(data)) {
                List<PlanResponse> plans = planService.listPlans();
                send(chatId, telegramMessageService.plansMessage(plans), plansKeyboard(plans));
                return;
            }
            if (data != null && data.startsWith(CALLBACK_ACTIVATE_REQUEST_PREFIX)) {
                String planCode = data.substring(CALLBACK_ACTIVATE_REQUEST_PREFIX.length());
                send(chatId, "Подтвердить активацию тарифа " + planCode + "?", confirmActivationKeyboard(planCode));
                return;
            }
            if (data != null && data.startsWith(CALLBACK_ACTIVATE_CONFIRM_PREFIX)) {
                if (isActivationRateLimited(telegramId)) {
                    send(chatId, "Слишком быстро. Подождите пару секунд и попробуйте снова.", mainMenuKeyboard());
                    return;
                }
                String planCode = data.substring(CALLBACK_ACTIVATE_CONFIRM_PREFIX.length());
                SubscriptionResponse response = subscriptionService.activatePlan(telegramId, username, planCode);
                send(chatId, telegramMessageService.subscriptionIssuedMessage(response), mainMenuKeyboard());
                return;
            }
        } catch (BadRequestException | NotFoundException exception) {
            send(chatId, exception.getMessage(), mainMenuKeyboard());
        } catch (Exception exception) {
            send(chatId, "Ошибка сервера. Попробуйте позже.", mainMenuKeyboard());
        } finally {
            acknowledgeCallback(callbackQuery.getId());
        }
    }

    private InlineKeyboardMarkup mainMenuKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(button("📋 Тарифы", CALLBACK_PLANS)));
        rows.add(List.of(button("🎁 Trial 1 день", CALLBACK_TRIAL)));
        rows.add(List.of(button("🔑 Мой ключ", CALLBACK_KEY)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup plansKeyboard(List<PlanResponse> plans) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        String foreverPlanCode = null;
        for (PlanResponse plan : plans) {
            if (plan.trial()) {
                continue;
            }
            if ("FOREVER_UNLIM".equals(plan.code())) {
                foreverPlanCode = plan.code();
                continue;
            }
            rows.add(List.of(button(
                    "Активировать " + plan.code(),
                    CALLBACK_ACTIVATE_REQUEST_PREFIX + plan.code()
            )));
        }
        if (foreverPlanCode != null) {
            rows.add(0, List.of(button(
                    "∞ Полный безлимит",
                    CALLBACK_ACTIVATE_REQUEST_PREFIX + foreverPlanCode
            )));
        }
        rows.add(List.of(button("⬅️ Назад", CALLBACK_BACK)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup confirmActivationKeyboard(String planCode) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(button("✅ Подтвердить", CALLBACK_ACTIVATE_CONFIRM_PREFIX + planCode)));
        rows.add(List.of(button("❌ Отмена", CALLBACK_ACTIVATE_CANCEL)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private void acknowledgeCallback(String callbackId) {
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
        } catch (TelegramApiException ignored) {
        }
    }

    private boolean isActivationRateLimited(Long telegramId) {
        Instant now = Instant.now();
        Instant last = lastActivationAt.get(telegramId);
        if (last != null && Duration.between(last, now).compareTo(ACTIVATE_COOLDOWN) < 0) {
            return true;
        }
        lastActivationAt.put(telegramId, now);
        return false;
    }
}
