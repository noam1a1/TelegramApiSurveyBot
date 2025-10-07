package org.example.model;

public class User {

    private final long telegramId;
    private String name;
    private boolean hasVoted;

    public User(long telegramId, String name) {
        this.telegramId = telegramId;
        this.name = name;
        this.hasVoted = false;
    }

    public long getTelegramId() { return telegramId; }
    public String getName() { return name; }
    public boolean hasVoted() { return hasVoted; }
    public void setName(String name) { this.name = name; }

    public void markVoted() { this.hasVoted = true; }
    public void resetVote() { this.hasVoted = false; }

    @Override
    public String toString() {
        return "User{" +
                "telegramId=" + telegramId +
                ", name='" + name + '\'' +
                ", hasVoted=" + hasVoted +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User other = (User) obj;
        return telegramId == other.telegramId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(telegramId);
    }
}
