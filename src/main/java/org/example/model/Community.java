package org.example.model;

import org.example.util.BotConfig;

import java.util.ArrayList;
import java.util.List;

public class Community {

    private List<User> members;
    private Survey activeSurvey;

    public Community() {
        this.members = new ArrayList<>();
        this.activeSurvey = null;
    }

    public boolean addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
            return true;
        }
        return false;
    }

    public int getSize() {
        return members.size();
    }

    public boolean canStartSurvey() {
        return members.size() >= BotConfig.MIN_MEMBERS;
    }

    public void notifyAllMembers(String message) {
        System.out.println("📢 Message to community: " + message);
        for (User user : members) {
            System.out.println("Sent to " + user.getName());
        }
    }

    public boolean hasActiveSurvey() {
        return activeSurvey != null && activeSurvey.isActive();
    }

    public Survey getActiveSurvey() {
        return activeSurvey;
    }

    public void setActiveSurvey(Survey survey) {
        this.activeSurvey = survey;
    }

    public void clearActiveSurvey() {
        this.activeSurvey = null;
    }

    public List<User> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "Community{" +
                "members=" + members.size() +
                ", activeSurvey=" + (activeSurvey != null ? "Yes" : "No") +
                '}';
    }
}
