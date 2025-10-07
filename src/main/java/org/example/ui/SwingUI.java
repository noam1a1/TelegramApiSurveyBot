package org.example.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import org.example.bot.TelegramSurveyBot;
import org.example.model.Community;
import org.example.model.Survey;
import org.example.model.SurveyResult;
import org.example.model.User;
import org.example.service.ChatGPTService;
import org.example.service.SurveyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class SwingUI extends JFrame {

    private final Community community;
    private final SurveyManager surveyManager;
    private final ChatGPTService gptService;
    private final User creator;
    private final TelegramSurveyBot bot;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JLabel statusMembers = new JLabel("Members: 0");
    private final JLabel statusSurvey = new JLabel("Active survey: none");
    private final JTextArea appLog = new JTextArea(6, 40);

    private ManualSurveyPanel manualPanel;
    private AutoSurveyPanel autoPanel;
    private ResultsPanel resultsPanel;

    private final Timer uiTimer = new Timer(true);

    public SwingUI(Community community,
                   SurveyManager surveyManager,
                   ChatGPTService gptService,
                   User creator,
                   TelegramSurveyBot bot) {
        super("Survey Console");
        this.community = community;
        this.surveyManager = surveyManager;
        this.gptService = gptService;
        this.creator = creator;
        this.bot = bot;

        applyModernLookAndFeel();
        buildUI();
        updateStatusBar();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
    }

    private void applyModernLookAndFeel() {
        try { FlatLightLaf.setup(); } catch (Exception ignored) {}
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UIManager.put("defaultFont", new FontUIResource(new Font("Segoe UI", Font.PLAIN, 14)));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 999);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Button.focusWidth", 1);
        UIManager.put("ScrollBar.showButtons", false);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnManual  = new JButton("Manual Survey");
        JButton btnAuto    = new JButton("Auto Survey (API)");
        JButton btnResults = new JButton("Results");
        nav.add(btnManual);
        nav.add(btnAuto);
        nav.add(btnResults);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        statusMembers.setForeground(new Color(70, 70, 70));
        statusSurvey.setForeground(new Color(70, 70, 70));

        JToggleButton darkToggle = new JToggleButton("Dark Mode");
        darkToggle.addActionListener(e -> {
            FlatAnimatedLafChange.showSnapshot();
            if (darkToggle.isSelected()) {
                FlatDarkLaf.setup();
                darkToggle.setText("Light Mode");
            } else {
                FlatLightLaf.setup();
                darkToggle.setText("Dark Mode");
            }
            SwingUtilities.updateComponentTreeUI(this);
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            this.pack();
        });

        status.add(darkToggle);
        status.add(statusMembers);
        status.add(statusSurvey);

        toolbar.add(nav, BorderLayout.WEST);
        toolbar.add(status, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        manualPanel  = new ManualSurveyPanel(surveyManager, community, creator, this::onSurveyCreated, this::log);
        autoPanel    = new AutoSurveyPanel(surveyManager, gptService, community, creator, this::onSurveyCreated, this::log);
        resultsPanel = new ResultsPanel(this::log);

        cards.add(manualPanel, "manual");
        cards.add(autoPanel,   "auto");
        cards.add(resultsPanel,"results");
        add(cards, BorderLayout.CENTER);

        appLog.setEditable(false);
        appLog.setFont(UIManager.getFont("TextArea.font"));
        JScrollPane logScroll = new JScrollPane(appLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        add(logScroll, BorderLayout.SOUTH);

        btnManual.addActionListener(e -> cardLayout.show(cards, "manual"));
        btnAuto.addActionListener(e -> cardLayout.show(cards, "auto"));
        btnResults.addActionListener(e -> {
            Survey active = community.getActiveSurvey();
            if (active == null) {
                JOptionPane.showMessageDialog(this, "No active survey.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            resultsPanel.showResults(new SurveyResult(active));
            cardLayout.show(cards, "results");
        });
    }

    // כאן ה-Integer הוא sendDelayMinutes
    private void onSurveyCreated(Survey survey, int sendDelayMinutes) {
        updateStatusBar();
        log("Survey created: " + survey.getId() + " | questions=" + survey.getQuestions().size());

        if (bot != null) {
            int autoClose = 5; // לפי הדרישה
            if (sendDelayMinutes > 0) {
                log("Survey will be sent in " + sendDelayMinutes + " minute(s). Auto-close: " + autoClose + " min after send.");
                uiTimer.schedule(new TimerTask() {
                    @Override public void run() {
                        bot.sendSurveyToCommunity(survey, autoClose);
                        log("Survey sent to community via Telegram (auto-close " + autoClose + " min).");
                    }
                }, sendDelayMinutes * 60L * 1000L);
            } else {
                bot.sendSurveyToCommunity(survey, autoClose);
                log("Survey sent to community via Telegram (auto-close " + autoClose + " min).");
            }
        } else {
            log("No Telegram bot instance. Survey not sent. Scheduling local auto-close in 5 minutes.");
            surveyManager.scheduleSurveyClose(survey, 5);
        }
    }

    private void updateStatusBar() {
        statusMembers.setText("Members: " + community.getSize());
        String s = (community.getActiveSurvey() != null && community.getActiveSurvey().isActive()) ? "yes" : "none";
        statusSurvey.setText("Active survey: " + s);
    }

    private void log(String msg) {
        appLog.append(msg + "\n");
        appLog.setCaretPosition(appLog.getDocument().getLength());
    }

    public static void launch(Community community,
                              SurveyManager manager,
                              ChatGPTService gpt,
                              User creator,
                              TelegramSurveyBot bot) {
        SwingUtilities.invokeLater(() ->
                new SwingUI(community, manager, gpt, creator, bot).setVisible(true));
    }
}
