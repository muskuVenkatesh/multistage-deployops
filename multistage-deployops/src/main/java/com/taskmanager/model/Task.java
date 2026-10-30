package com.taskmanager.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single task item in the system.
 */
public class Task {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final int id;
    private String title;
    private String category;
    private boolean completed;
    private final String createdAt;

    public Task(int id, String title, String category) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.completed = false;
        this.createdAt = LocalDateTime.now().format(FORMATTER);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        String status = completed ? "[✓] Completed" : "[ ] Pending  ";
        return String.format("%-4d | %-13s | %-12s | %-28s | %s",
                id, status, category, title, createdAt);
    }
}
