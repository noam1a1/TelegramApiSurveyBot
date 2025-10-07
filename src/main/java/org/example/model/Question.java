package org.example.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Question {

    private final String text;
    private final List<String> options;
    private final Map<String, Integer> results;

    public Question(String text, List<String> options) {
        this.text = text;
        this.options = new ArrayList<>(options);
        this.results = new LinkedHashMap<>();
        for (String option : this.options) {
            results.put(option, 0);
        }
    }

    public void addVote(String option) {
        Integer count = results.get(option);
        if (count != null) {
            results.put(option, count + 1);
        }
    }

    public Map<String, Double> getResultsPercent() {
        Map<String, Double> percentMap = new LinkedHashMap<>();
        int totalVotes = results.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            double percent = totalVotes == 0 ? 0.0 : (entry.getValue() * 100.0 / totalVotes);
            percentMap.put(entry.getKey(), Math.round(percent * 10) / 10.0);
        }
        return percentMap;
    }

    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public Map<String, Integer> getResults() { return results; }

    @Override
    public String toString() {
        return "Question{" +
                "text='" + text + '\'' +
                ", options=" + options +
                ", results=" + results +
                '}';
    }
}
