package com.taskmanager.service;

import com.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service managing task operations.
 */
public class TaskService {
    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    public TaskService() {
        // Pre-populate with initial tasks
        seedTasks();
    }

    private void seedTasks() {
        addTask("Set up environment", "Setup");
        addTask("Implement feature requirements", "Development");
        addTask("Perform testing", "QA");
        markCompleted(1);
    }

    public synchronized Task addTask(String title, String category) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        String cat = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
        Task task = new Task(nextId++, title.trim(), cat);
        tasks.add(task);
        return task;
    }

    public synchronized List<Task> getAllTasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public synchronized Optional<Task> findById(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    public synchronized boolean markCompleted(int id) {
        Optional<Task> taskOpt = findById(id);
        if (taskOpt.isPresent()) {
            taskOpt.get().setCompleted(true);
            return true;
        }
        return false;
    }

    public synchronized boolean toggleCompleted(int id) {
        Optional<Task> taskOpt = findById(id);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setCompleted(!task.isCompleted());
            return true;
        }
        return false;
    }

    public synchronized boolean deleteTask(int id) {
        return tasks.removeIf(task -> task.getId() == id);
    }

    public synchronized List<Task> searchByTitle(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String lowerKeyword = keyword.trim().toLowerCase();
        List<Task> results = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getTitle().toLowerCase().contains(lowerKeyword)) {
                results.add(task);
            }
        }
        return results;
    }

    public synchronized TaskSummary getSummary() {
        int total = tasks.size();
        int completed = 0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completed++;
            }
        }
        int pending = total - completed;
        double rate = total > 0 ? ((double) completed / total) * 100.0 : 0.0;
        return new TaskSummary(total, completed, pending, rate);
    }

    public record TaskSummary(int total, int completed, int pending, double completionRate) {
    }
}
