package org.example.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Survey {

    private String id;
    private List<Question> questions;
    private LocalDateTime startTime;
    private int durationMinutes = 5;
    private boolean active;
    private Map<Long, List<Integer>> responses;
    private User creator;

    public Survey(String id, List<Question> questions, User creator) {
        this.id = id;
        this.questions = questions;
        this.creator = creator;
        this.startTime = LocalDateTime.now();
        this.active = true;
        this.responses = new HashMap<>();
    }

    public boolean collectResponse(User user, List<Integer> answers) {
        if (!active) return false;
        if (responses.containsKey(user.getTelegramId())) return false;

        responses.put(user.getTelegramId(), answers);

        for (int i = 0; i < questions.size(); i++) {
            int chosenIndex = answers.get(i);
            String chosenOption = questions.get(i).getOptions().get(chosenIndex);
            questions.get(i).addVote(chosenOption);
        }

        user.markVoted();
        return true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(startTime.plusMinutes(durationMinutes));
    }

    public void closeSurvey() {
        this.active = false;
    }

    public boolean isActive() { return active; }
    public String getId() { return id; }
    public User getCreator() { return creator; }
    public List<Question> getQuestions() { return questions; }
    public Map<Long, List<Integer>> getResponses() { return responses; }
    public LocalDateTime getStartTime() { return startTime; }

    @Override
    public String toString() {
        return "Survey{" +
                "id='" + id + '\'' +
                ", questions=" + questions.size() +
                ", active=" + active +
                ", responses=" + responses.size() +
                '}';
    }
}
