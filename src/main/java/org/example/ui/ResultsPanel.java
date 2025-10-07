package org.example.ui;

import org.example.model.Question;
import org.example.model.SurveyResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

public class ResultsPanel extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final Consumer<String> appLog;

    public ResultsPanel(Consumer<String> appLog) {
        this.appLog = appLog;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Results");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        add(title, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    public void showResults(SurveyResult result) {
        tabs.removeAll();
        int qi = 1;
        for (Question q : result.getSurvey().getQuestions()) {
            JPanel panel = new JPanel(new BorderLayout());
            String[] cols = {"Option", "Percent"};
            Object[][] rows = q.getResultsPercent().entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed())
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .toArray(Object[][]::new);

            JTable table = new JTable(rows, cols);
            table.setRowHeight(24);
            table.setEnabled(false);
            table.setFillsViewportHeight(true);

            panel.add(new JLabel(q.getText()), BorderLayout.NORTH);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            tabs.add("Q" + (qi++), panel);
        }
        appLog.accept("Results refreshed.");
    }
}
