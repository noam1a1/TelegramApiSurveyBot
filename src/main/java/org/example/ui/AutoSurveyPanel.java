package org.example.ui;

import org.example.model.*;
import org.example.service.ChatGPTService;
import org.example.service.SurveyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutoSurveyPanel extends JPanel {

    private final SurveyManager surveyManager;
    private final ChatGPTService gptService;
    private final Community community;
    private final User creator;

    private final JTextField topicField = new JTextField();
    private final JButton btnGenerate = new JButton("Generate");
    private final JButton btnCreate   = new JButton("Create Survey");
    private final JSpinner sendDelaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
    private final JTextArea preview = new JTextArea(12, 40);
    private final JTextArea statusArea = new JTextArea(6, 40);

    private Survey pendingSurvey;

    private final Consumer<String> appLog;
    private final BiConsumer<Survey, Integer> onSurveyCreated; // Integer = sendDelayMinutes

    public AutoSurveyPanel(SurveyManager surveyManager,
                           ChatGPTService gptService,
                           Community community,
                           User creator,
                           BiConsumer<Survey, Integer> onSurveyCreated,
                           Consumer<String> appLog) {
        this.surveyManager = surveyManager;
        this.gptService = gptService;
        this.community = community;
        this.creator = creator;
        this.onSurveyCreated = onSurveyCreated;
        this.appLog = appLog;
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Auto Survey (API)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; controls.add(new JLabel("Topic:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1; controls.add(topicField, gc);
        gc.gridx = 2; gc.gridy = 0; gc.weightx = 0; controls.add(btnGenerate, gc);

        gc.gridx = 0; gc.gridy = 1; controls.add(new JLabel("Send delay (minutes):"), gc);
        gc.gridx = 1; gc.gridy = 1; controls.add(sendDelaySpinner, gc);
        gc.gridx = 2; gc.gridy = 1; controls.add(btnCreate, gc);

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.NORTH);
        north.add(controls, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        preview.setEditable(false);
        preview.setFont(UIManager.getFont("TextArea.font"));
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);

        statusArea.setEditable(false);
        statusArea.setFont(UIManager.getFont("TextArea.font"));
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);

        JScrollPane previewScroll = new JScrollPane(preview);
        previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));

        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Status"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewScroll, statusScroll);
        split.setResizeWeight(0.7);
        add(split, BorderLayout.CENTER);

        btnGenerate.addActionListener(e -> onGenerate());
        btnCreate.addActionListener(e -> onCreate());
    }

    private void onGenerate() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a topic.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!community.canStartSurvey()) {
            JOptionPane.showMessageDialog(this, "You need more members to start a survey.", "Not enough members", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            gptService.clearHistory();
            Survey survey = surveyManager.createAutoSurvey(topic, creator);
            this.pendingSurvey = survey;

            preview.setText(renderSurveyPreview(survey));
            preview.setComponentOrientation(
                    containsHebrew(preview.getText())
                            ? ComponentOrientation.RIGHT_TO_LEFT
                            : ComponentOrientation.LEFT_TO_RIGHT
            );

            status("Survey generated. ID=" + survey.getId() + "  |  " + survey.getQuestions().size() + " question(s).");
            appLog.accept("Auto survey generated via API.");
        } catch (Exception ex) {
            status("Error: " + ex.getMessage());
        }
    }

    private void onCreate() {
        if (pendingSurvey == null) {
            JOptionPane.showMessageDialog(this, "Generate a survey first.", "No survey", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int sendDelay = (Integer) sendDelaySpinner.getValue();
        onSurveyCreated.accept(pendingSurvey, sendDelay);
        status("Survey scheduled to send in " + sendDelay + " minute(s). Auto-close: 5 minutes after send.");
    }

    private String renderSurveyPreview(Survey s) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Question q : s.getQuestions()) {
            sb.append(i++).append(") ").append(q.getText()).append("\n   ");
            String opts = q.getOptions().stream().collect(Collectors.joining(" | "));
            sb.append(opts).append("\n\n");
        }
        return sb.toString();
    }

    private void status(String s) {
        statusArea.append(s + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    private static boolean containsHebrew(String s) {
        if (s == null) return false;
        return s.codePoints().anyMatch(cp -> cp >= 0x0590 && cp <= 0x05FF);
    }
}
