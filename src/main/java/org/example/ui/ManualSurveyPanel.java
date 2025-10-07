package org.example.ui;

import org.example.model.*;
import org.example.service.SurveyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ManualSurveyPanel extends JPanel {

    private final SurveyManager surveyManager;
    private final Community community;
    private final User creator;

    private final JPanel questionsContainer = new JPanel();
    private final JSpinner sendDelaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
    private final JTextArea statusArea = new JTextArea(6, 40);

    private final Consumer<String> appLog;
    private final BiConsumer<Survey, Integer> onSurveyCreated; // Integer = sendDelayMinutes

    public ManualSurveyPanel(SurveyManager surveyManager,
                             Community community,
                             User creator,
                             BiConsumer<Survey, Integer> onSurveyCreated,
                             Consumer<String> appLog) {
        this.surveyManager = surveyManager;
        this.community = community;
        this.creator = creator;
        this.onSurveyCreated = onSurveyCreated;
        this.appLog = appLog;

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Manual Survey");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JButton btnAddQuestion = new JButton("Add Question");
        JButton btnCreate = new JButton("Create Survey");

        JPanel top = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(btnAddQuestion);
        actions.add(new JLabel("Send delay (minutes):"));
        actions.add(sendDelaySpinner);
        actions.add(btnCreate);

        top.add(title, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        questionsContainer.setLayout(new BoxLayout(questionsContainer, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(questionsContainer);
        scroll.setBorder(BorderFactory.createTitledBorder("Questions (1-3) | Each with 2-4 options"));
        add(scroll, BorderLayout.CENTER);

        statusArea.setEditable(false);
        statusArea.setFont(UIManager.getFont("TextArea.font"));
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(new TitledBorder("Status"));
        add(statusScroll, BorderLayout.SOUTH);

        addQuestionEditor();

        btnAddQuestion.addActionListener(e -> addQuestionEditor());
        btnCreate.addActionListener(e -> onCreate());
    }

    private void addQuestionEditor() {
        if (questionsContainer.getComponentCount() >= 3) {
            JOptionPane.showMessageDialog(this, "Maximum 3 questions.", "Limit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        QuestionEditor editor = new QuestionEditor();
        questionsContainer.add(editor);
        questionsContainer.add(Box.createVerticalStrut(10));
        revalidate();
        repaint();
    }

    private void onCreate() {
        try {
            if (!community.canStartSurvey()) {
                JOptionPane.showMessageDialog(this, "You need more members in the community.", "Not enough members", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<Question> questions = new ArrayList<>();
            for (Component c : questionsContainer.getComponents()) {
                if (c instanceof QuestionEditor qe) {
                    Question q = qe.buildQuestion();
                    if (q != null) questions.add(q);
                }
            }
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Add at least one valid question.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Survey survey = surveyManager.createManualSurvey(questions, creator);
            int sendDelay = (Integer) sendDelaySpinner.getValue();
            onSurveyCreated.accept(survey, sendDelay);

            status("Survey created. Will be sent in " + sendDelay + " minute(s). Auto-close: 5 minutes after send.");
            appLog.accept("Manual survey created via UI.");
        } catch (Exception ex) {
            status("Error: " + ex.getMessage());
        }
    }

    private void status(String s) {
        statusArea.append(s + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    private static class QuestionEditor extends JPanel {
        private final JTextField txtQuestion = new JTextField();
        private final DefaultListModel<String> optionsModel = new DefaultListModel<>();
        private final JList<String> optionsList = new JList<>(optionsModel);

        QuestionEditor() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 210, 210)),
                    new EmptyBorder(12, 12, 12, 12)
            ));

            JLabel lblQ = new JLabel("Question:");
            JPanel qRow = new JPanel(new BorderLayout(6, 6));
            qRow.add(lblQ, BorderLayout.WEST);
            qRow.add(txtQuestion, BorderLayout.CENTER);

            JPanel optsPanel = new JPanel(new BorderLayout(6, 6));
            optsPanel.setBorder(BorderFactory.createTitledBorder("Options (2-4)"));
            optionsList.setVisibleRowCount(4);
            optsPanel.add(new JScrollPane(optionsList), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            JTextField newOpt = new JTextField(16);
            JButton btnAdd = new JButton("Add Option");
            JButton btnRemove = new JButton("Remove Selected");
            buttons.add(new JLabel("New:"));
            buttons.add(newOpt);
            buttons.add(btnAdd);
            buttons.add(btnRemove);
            optsPanel.add(buttons, BorderLayout.SOUTH);

            add(qRow, BorderLayout.NORTH);
            add(optsPanel, BorderLayout.CENTER);

            optionsModel.addElement("Option 1");
            optionsModel.addElement("Option 2");

            btnAdd.addActionListener(e -> {
                String t = newOpt.getText().trim();
                if (t.isEmpty()) return;
                if (optionsModel.size() >= 4) {
                    JOptionPane.showMessageDialog(this, "Maximum 4 options.", "Limit", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                optionsModel.addElement(t);
                newOpt.setText("");
            });
            btnRemove.addActionListener(e -> {
                int idx = optionsList.getSelectedIndex();
                if (idx >= 0) optionsModel.remove(idx);
            });
        }

        Question buildQuestion() {
            String text = txtQuestion.getText().trim();
            if (text.isEmpty()) return null;
            if (optionsModel.size() < 2) return null;
            java.util.List<String> opts = new java.util.ArrayList<>();
            for (int i = 0; i < optionsModel.size(); i++) {
                String v = optionsModel.get(i).trim();
                if (!v.isEmpty()) opts.add(v);
            }
            if (opts.size() < 2 || opts.size() > 4) return null;
            return new Question(text, opts);
        }
    }
}
