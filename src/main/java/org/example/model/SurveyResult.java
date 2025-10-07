package org.example.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class SurveyResult {

    private final Survey survey;
    private final Map<String, Map<String, Double>> resultsByQuestion;

    public SurveyResult(Survey survey) {
        this.survey = survey;
        this.resultsByQuestion = new LinkedHashMap<>();
        calculateResults();
    }

    private void calculateResults() {
        for (Question question : survey.getQuestions()) {
            resultsByQuestion.put(question.getText(), question.getResultsPercent());
        }
    }

    public void displayResults() {
        System.out.println("📊 תוצאות סקר: " + survey.getId());
        for (Map.Entry<String, Map<String, Double>> entry : resultsByQuestion.entrySet()) {
            System.out.println("\nשאלה: " + entry.getKey());
            for (Map.Entry<String, Double> answer : entry.getValue().entrySet()) {
                System.out.println(" - " + answer.getKey() + ": " + answer.getValue() + "%");
            }
        }
    }

    public Survey getSurvey() { return survey; }
    public Map<String, Map<String, Double>> getResultsByQuestion() { return resultsByQuestion; }

    @Override
    public String toString() {
        return "SurveyResult{" +
                "survey=" + survey.getId() +
                ", questions=" + resultsByQuestion.size() +
                '}';
    }
}
