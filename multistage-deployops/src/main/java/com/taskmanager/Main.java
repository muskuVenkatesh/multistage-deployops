package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import com.taskmanager.web.TaskWebServer;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for the Task Management System.
 */
public class Main {

    private static final int DEFAULT_PORT = 8081;
    private static final TaskService taskService = new TaskService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // If '--cli' argument is provided, run in terminal menu mode
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            runCliMode();
            return;
        }

        // Default: Start Web Server and allow opening in browser
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        startWebServer(port);
    }

    private static void startWebServer(int port) {
        TaskWebServer webServer = new TaskWebServer(port, taskService);
        try {
            webServer.start();

            String url = "http://localhost:" + port;
            System.out.println("==================================================");
            System.out.println("             Task Management System               ");
            System.out.println("==================================================");
            System.out.println("Web server started successfully!");
            System.out.println();
            System.out.println("👉 Open in your browser: " + url);
            System.out.println();
            System.out.println("Features available in browser:");
            System.out.println("  • Add and manage tasks");
            System.out.println("  • Mark tasks completed / pending");
            System.out.println("  • Real-time progress statistics");
            System.out.println("  • Search and category filtering");
            System.out.println();
            System.out.println("Press Ctrl+C in this terminal to stop the server.");
            System.out.println("==================================================");

            // Attempt to automatically open the default browser if supported
            tryOpenBrowser(url);

        } catch (Exception e) {
            System.err.println("Error starting web server on port " + port + ": " + e.getMessage());
            System.out.println("Falling back to CLI mode...");
            runCliMode();
        }
    }

    private static void tryOpenBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Throwable ignored) {
            // Safe fallback: user can click the printed URL
        }
    }

    private static void runCliMode() {
        System.out.println("==================================================");
        System.out.println("             Task Management System               ");
        System.out.println("==================================================");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInteger("Enter your choice (1-7): ");

            switch (choice) {
                case 1 -> listTasks();
                case 2 -> addTask();
                case 3 -> completeTask();
                case 4 -> deleteTask();
                case 5 -> searchTasks();
                case 6 -> showSummary();
                case 7 -> {
                    System.out.println("\nThank you for using Task Management System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please choose a number between 1 and 7.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n----------------- MAIN MENU -----------------");
        System.out.println("1. View all tasks");
        System.out.println("2. Add a new task");
        System.out.println("3. Mark task as completed");
        System.out.println("4. Delete a task");
        System.out.println("5. Search tasks by title");
        System.out.println("6. View summary & statistics");
        System.out.println("7. Exit");
        System.out.println("---------------------------------------------");
    }

    private static void listTasks() {
        System.out.println("\n=== Task List ===");
        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks found. Use option 2 to create a task.");
            return;
        }

        System.out.println(String.format("%-4s | %-13s | %-12s | %-28s | %s",
                "ID", "Status", "Category", "Title", "Created At"));
        System.out.println("-----+---------------+--------------+------------------------------+------------------");
        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    private static void addTask() {
        System.out.println("\n=== Add New Task ===");
        System.out.print("Enter task title: ");
        String title = scanner.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("Error: Task title cannot be empty.");
            return;
        }

        System.out.print("Enter category (e.g. Work, Personal, Study): ");
        String category = scanner.nextLine().trim();

        Task task = taskService.addTask(title, category);
        System.out.println("✓ Task successfully created with ID: " + task.getId());
    }

    private static void completeTask() {
        System.out.println("\n=== Mark Task as Completed ===");
        int id = readInteger("Enter Task ID to complete: ");

        if (taskService.markCompleted(id)) {
            System.out.println("✓ Task #" + id + " marked as completed.");
        } else {
            System.out.println("Error: Task with ID " + id + " was not found.");
        }
    }

    private static void deleteTask() {
        System.out.println("\n=== Delete Task ===");
        int id = readInteger("Enter Task ID to delete: ");

        if (taskService.deleteTask(id)) {
            System.out.println("✓ Task #" + id + " deleted successfully.");
        } else {
            System.out.println("Error: Task with ID " + id + " was not found.");
        }
    }

    private static void searchTasks() {
        System.out.println("\n=== Search Tasks ===");
        System.out.print("Enter search keyword: ");
        String keyword = scanner.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        List<Task> results = taskService.searchByTitle(keyword);
        if (results.isEmpty()) {
            System.out.println("No tasks matched keyword: '" + keyword + "'");
        } else {
            System.out.println("Found " + results.size() + " matching task(s):");
            for (Task task : results) {
                System.out.println("  " + task);
            }
        }
    }

    private static void showSummary() {
        System.out.println("\n=== Task Summary & Statistics ===");
        TaskService.TaskSummary summary = taskService.getSummary();
        System.out.println("Total Tasks     : " + summary.total());
        System.out.println("Completed Tasks : " + summary.completed());
        System.out.println("Pending Tasks   : " + summary.pending());
        System.out.printf("Completion Rate : %.1f%%\n", summary.completionRate());
    }

    private static int readInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }
}
