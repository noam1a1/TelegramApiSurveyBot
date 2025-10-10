package org.example.bot;

import org.example.model.Community;
import org.example.model.Question;
import org.example.model.Survey;
import org.example.model.SurveyResult;
import org.example.model.User;
import org.example.service.SurveyManager;
import org.example.util.BotConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramSurveyBot extends TelegramLongPollingBot {

    private final Community community;
    private final SurveyManager surveyManager;
    private final Map<Long, List<Integer>> partialAnswers = new ConcurrentHashMap<>();

    public TelegramSurveyBot(Community community, SurveyManager surveyManager) {
        this.community = community;
        this.surveyManager = surveyManager;
    }

    public static TelegramSurveyBot start(Community community, SurveyManager manager) throws Exception {
        TelegramSurveyBot bot = new TelegramSurveyBot(community, manager);
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return bot;
    }

    @Override public String getBotUsername() { return BotConfig.TELEGRAM_BOT_USERNAME; }
    @Override public String getBotToken()    { return BotConfig.TELEGRAM_BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                onText(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                onCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onText(Message msg) throws Exception {
        String text = msg.getText().trim();
        long chatId = msg.getChatId();
        String name = displayName(msg.getFrom());

        boolean wantsJoin =
                "/start".equals(text) ||
                        "Hi".equalsIgnoreCase(text) ||
                        "היי".equals(text);

        if (!wantsJoin) {
            return;
        }

        User user = new User(chatId, name);
        boolean added = community.addMember(user);

        if (added) {
            sendText(chatId, "Welcome, " + name + "! Community size now: " + community.getSize());
            String announce = "A new member has joined: " + name + " Total: " + community.getSize() + ")";
            broadcastToAllExcept(announce, chatId);
        } else {
            sendText(chatId, "You are already a member of the community! ");
        }
    }

    private void onCallback(CallbackQuery cb) throws Exception {
        String data = cb.getData();
        long userId = cb.getFrom().getId();
        String callbackId = cb.getId();

        Survey active = community.getActiveSurvey();
        if (active == null || !active.isActive()) {
            answerCallback(callbackId, "No active survey. ");
            return;
        }

        User u = findUser(userId);
        if (u == null) {
            answerCallback(callbackId, "You are not a member of the community. ");
            return;
        }
        if (u.hasVoted()) {
            answerCallback(callbackId, "You have already replied to this survey. ");
            return;
        }

        ParsedData parsed = parseCallback(data);
        if (parsed == null || !active.getId().equals(parsed.surveyId)) {
            answerCallback(callbackId, "⚠️ Choice invalid.");
            return;
        }

        List<Integer> bucket = partialAnswers.computeIfAbsent(userId, k ->
                initEmptyAnswers(active.getQuestions().size()));

        if (parsed.qIndex < 0 || parsed.qIndex >= bucket.size()) {
            answerCallback(callbackId, "Invalid question. ");
            return;
        }
        int maxOpt = active.getQuestions().get(parsed.qIndex).getOptions().size() - 1;
        if (parsed.optIndex < 0 || parsed.optIndex > maxOpt) {
            answerCallback(callbackId, "Invalid option. ");
            return;
        }

        bucket.set(parsed.qIndex, parsed.optIndex);
        answerCallback(callbackId, " A choice has been made " + (parsed.optIndex + 1) + " in question " + (parsed.qIndex + 1));

        if (!bucket.contains(null)) {
            boolean ok = active.collectResponse(u, bucket);
            if (ok) {
                sendText(userId, "Thank you! Answer recieved. ");
                if (active.getResponses().size() == community.getSize()) {
                    surveyManager.closeSurveyIfOpen(active);
                    sendResultsToCreator(active);
                }
            } else {
                sendText(userId, "Can't submit answer now, maybe survey has closed. ");
            }
        }
    }

    public void sendSurveyToCommunity(Survey survey, int autoCloseMinutes) {
        if (survey == null || !survey.isActive()) return;

        partialAnswers.clear();

        String header = "📣 A new survey has opened\n" +
                "⏱ Answering time until " + autoCloseMinutes + " minutes.\n" +
                "Please answer all questions (Press a button for each question.)";

        for (User u : community.getMembers()) {
            sendText(u.getTelegramId(), header);
            partialAnswers.put(u.getTelegramId(), initEmptyAnswers(survey.getQuestions().size()));
            for (int qi = 0; qi < survey.getQuestions().size(); qi++) {
                sendQuestion(u.getTelegramId(), survey, qi);
            }
        }

        surveyManager.scheduleSurveyClose(survey, autoCloseMinutes);

        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            @Override public void run() {
                if (survey.isActive()) {
                    surveyManager.closeSurveyIfOpen(survey);
                    sendResultsToCreator(survey);
                }
            }
        }, autoCloseMinutes * 60L * 1000L);
    }

    private void sendQuestion(long chatId, Survey survey, int qIndex) {
        Question q = survey.getQuestions().get(qIndex);

        String text = (qIndex + 1) + ") " + q.getText();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < q.getOptions().size(); i++) {
            String opt = q.getOptions().get(i);
            InlineKeyboardButton b = InlineKeyboardButton.builder()
                    .text((i + 1) + ". " + opt)
                    .callbackData("sv|" + survey.getId() + "|q|" + qIndex + "|o|" + i)
                    .build();
            row.add(b);
        }
        rows.add(row);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(rows).build();

        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(kb)
                .build();
        exec(sm);
    }

    private void sendResultsToCreator(Survey survey) {
        SurveyResult result = new SurveyResult(survey);
        StringBuilder sb = new StringBuilder("📊Survey results: ").append(survey.getId());
        for (Question q : survey.getQuestions()) {
            sb.append("\n\nQuestion: ").append(q.getText());
            q.getResultsPercent().entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(e -> sb.append("\n - ").append(e.getKey()).append(": ").append(e.getValue()).append("%"));
        }
        long creatorId = survey.getCreator().getTelegramId();
        sendText(creatorId, sb.toString());
    }

    private void sendText(long chatId, String text) {
        exec(SendMessage.builder().chatId(String.valueOf(chatId)).text(text).build());
    }

    private void broadcastToAllExcept(String text, long exceptId) {
        for (User m : community.getMembers()) {
            if (m.getTelegramId() != exceptId) sendText(m.getTelegramId(), text);
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).showAlert(false).build());
        } catch (Exception ignored) {}
    }

    private void exec(SendMessage sm) {
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private User findUser(long telegramId) {
        for (User u : community.getMembers()) if (u.getTelegramId() == telegramId) return u;
        return null;
    }

    private static List<Integer> initEmptyAnswers(int count) {
        return new ArrayList<>(Collections.nCopies(count, null));
    }

    private static String displayName(User user) { return user.getName(); }

    private static String displayName(org.telegram.telegrambots.meta.api.objects.User u) {
        String n = (u.getFirstName() != null ? u.getFirstName() : "");
        if (u.getLastName() != null && !u.getLastName().isBlank()) n += " " + u.getLastName();
        if (n.isBlank() && u.getUserName() != null) n = u.getUserName();
        if (n.isBlank()) n = "User";
        return n.trim();
    }

    private record ParsedData(String surveyId, int qIndex, int optIndex) {}

    private ParsedData parseCallback(String data) {
        try {
            String[] t = data.split("\\|");
            if (t.length != 6) return null;
            if (!"sv".equals(t[0]) || !"q".equals(t[2]) || !"o".equals(t[4])) return null;
            return new ParsedData(t[1], Integer.parseInt(t[3]), Integer.parseInt(t[5]));
        } catch (Exception e) {
            return null;
        }
    }
}
