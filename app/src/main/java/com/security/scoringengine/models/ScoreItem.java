package com.security.scoringengine.models;

public class ScoreItem {
    private String description;
    private int points;
    private String category;
    private long timestamp;

    public ScoreItem(String description, int points, String category) {
        this.description = description;
        this.points = points;
        this.category = category;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public int getPoints() {
        return points;
    }

    public String getCategory() {
        return category;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
