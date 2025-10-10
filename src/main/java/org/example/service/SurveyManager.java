package org.example.service;

import org.example.model.ChatGPTResponse;
import org.example.model.Community;
import org.example.model.Question;
import org.example.model.Survey;
import org.example.model.User;
import org.example.util.BotConfig;
import org.example.util.ResponseUtils;
import org.example.util.SurveyJsonParser;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class SurveyManager {

    private final Community community;
    private final ChatGPTService gptService;
    private final Timer timer = new Timer(true);

    public SurveyManager(Community community, ChatGPTService gptService) {
        this.community = community;
        this.gptService = gptService;
    }

    public Survey createManualSurvey(List<Question> questions, User creator) {
        validateCanCreate();
        validateQuestions(questions);
        Survey survey = new Survey(UUID.randomUUID().toString(), questions, creator);
        community.setActiveSurvey(survey);
        return survey;
    }

    public Survey createAutoSurvey(String topic, User creator) {
        validateCanCreate();

        String prompt1 =
                "החזר רק JSON נקי (ללא הסברים וללא ```) בפורמט: " +
                        "{\"questions\":[{\"text\":\"שאלה\",\"options\":[\"אפשרות 1\",\"אפשרות 2\"]}]} " +
                        "נושא: \"" + topic + "\". " +
                        "מגבלות: 1–3 שאלות; לכל שאלה 2–4 אפשרויות קצרות.";

        gptService.clearHistory();

        ChatGPTResponse res = gptService.sendMessage(prompt1);
        if (!res.isSuccess()) {
            throw new IllegalStateException("Failed to recieve questions from the API: " + res.getErrorCode() + " " + res.getExtra());
        }

        String jsonBlob = ResponseUtils.extractFirstJsonBlock(res.getExtra());
        List<Question> questions = SurveyJsonParser.parseQuestions(jsonBlob);

        if (questions.isEmpty()) {
            String prompt2 =
                    "{\"questions\":[{\"text\":\"שאלה\",\"options\":[\"אפשרות 1\",\"אפשרות 2\"]}]} " +
                            "נושא: \"" + topic + "\". " +
                            "החזר אך ורק JSON נקי התואם במדויק לפורמט לעיל.";
            ChatGPTResponse res2 = gptService.sendMessage(prompt2);
            if (res2.isSuccess()) {
                String blob2 = ResponseUtils.extractFirstJsonBlock(res2.getExtra());
                questions = SurveyJsonParser.parseQuestions(blob2);
            }
        }

        validateQuestions(questions);
        Survey survey = new Survey(UUID.randomUUID().toString(), questions, creator);
        community.setActiveSurvey(survey);
        return survey;
    }

    public void scheduleSurveyClose(Survey survey, int delayMinutes) {
        int minutes = Math.min(5, Math.max(1, delayMinutes));
        timer.schedule(new TimerTask() {
            @Override public void run() {
                closeSurveyIfOpen(survey);
            }
        }, minutes * 60L * 1000L);
    }

    public void closeSurveyIfOpen(Survey survey) {
        if (survey != null && survey.isActive()) {
            survey.closeSurvey();
            for (User u : community.getMembers()) {
                u.resetVote();
            }
        }
    }

    private void validateCanCreate() {
        if (community.getSize() < BotConfig.MIN_MEMBERS) {
            throw new IllegalStateException("Required atleast " + BotConfig.MIN_MEMBERS + " members in community to open a survey");
        }
        if (community.hasActiveSurvey()) {
            throw new IllegalStateException("There is an active survey, close it before creating a new one. ");
        }
    }

    private void validateQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty() || questions.size() > 3)
            throw new IllegalArgumentException("Survey must contain 1-3 questions. ");
        for (Question q : questions) {
            int n = q.getOptions().size();
            if (n < 2 || n > 4)
                throw new IllegalArgumentException("Each question must have 2-4 choices. ");
        }
    }
}
