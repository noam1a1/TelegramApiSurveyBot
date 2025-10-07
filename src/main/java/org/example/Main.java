package org.example;

import org.example.bot.TelegramSurveyBot;
import org.example.model.Community;
import org.example.model.User;
import org.example.service.ChatGPTService;
import org.example.service.SurveyManager;
import org.example.ui.SwingUI;
import org.example.util.BotConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        Community community = new Community();
        ChatGPTService gpt = new ChatGPTService();
        SurveyManager manager = new SurveyManager(community, gpt);

        User creator = new User(BotConfig.ADMIN_TELEGRAM_ID, "Admin");
        community.addMember(creator);

        TelegramSurveyBot bot = TelegramSurveyBot.start(community, manager);
        System.out.println("Bot is running. Send /start from your account (DEV_MODE=true allows 1 member).");

        SwingUI.launch(community, manager, gpt, creator, bot);
    }
}
