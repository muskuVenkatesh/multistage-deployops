package com.taskmanager.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Built-in HTTP Web Server serving the Task Management Web UI.
 * Zero external libraries or build tools required.
 */
public class TaskWebServer {

    private final int port;
    private final TaskService taskService;
    private HttpServer server;

    public TaskWebServer(int port, TaskService taskService) {
        this.port = port;
        this.taskService = taskService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new WebUiHandler());
        server.createContext("/api/tasks", new TasksApiHandler());
        server.createContext("/api/tasks/toggle", new ToggleTaskHandler());
        server.createContext("/api/tasks/delete", new DeleteTaskHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Serves the single-page HTML / CSS / JS web application.
     */
    private class WebUiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }
            String html = getWebPageHtml();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * API endpoint to list all tasks or create a new task.
     */
    private class TasksApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                String json = buildTasksJsonResponse();
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> params = parseRequestBody(exchange);
                String title = params.get("title");
                String category = params.get("category");

                if (title == null || title.trim().isEmpty()) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"error\":\"Title cannot be empty\"}");
                    return;
                }

                taskService.addTask(title, category);
                sendResponse(exchange, 201, "application/json; charset=UTF-8", buildTasksJsonResponse());
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    /**
     * API endpoint to toggle task completion status.
     */
    private class ToggleTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            int id = extractId(exchange);
            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"error\":\"Invalid task ID\"}");
                return;
            }

            boolean updated = taskService.toggleCompleted(id);
            if (updated) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", buildTasksJsonResponse());
            } else {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"error\":\"Task not found\"}");
            }
        }
    }

    /**
     * API endpoint to delete a task.
     */
    private class DeleteTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            int id = extractId(exchange);
            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"error\":\"Invalid task ID\"}");
                return;
            }

            boolean deleted = taskService.deleteTask(id);
            if (deleted) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", buildTasksJsonResponse());
            } else {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"error\":\"Task not found\"}");
            }
        }
    }

    private int extractId(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=");
                if (parts.length == 2 && "id".equalsIgnoreCase(parts[0])) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        Map<String, String> body = parseRequestBody(exchange);
        if (body.containsKey("id")) {
            try {
                return Integer.parseInt(body.get("id"));
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();

        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON parser for basic key-values
            String trimmed = body.substring(1, body.length() - 1);
            String[] tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replaceAll("^\"|\"$", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "");
                    map.put(k, v);
                }
            }
        } else {
            // Form urlencoded parser
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    private String buildTasksJsonResponse() {
        List<Task> tasks = taskService.getAllTasks();
        TaskService.TaskSummary summary = taskService.getSummary();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"summary\":{");
        json.append("\"total\":").append(summary.total()).append(",");
        json.append("\"completed\":").append(summary.completed()).append(",");
        json.append("\"pending\":").append(summary.pending()).append(",");
        json.append(String.format(java.util.Locale.US, "\"completionRate\":%.1f", summary.completionRate()));
        json.append("},");

        json.append("\"tasks\":[");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            json.append("{");
            json.append("\"id\":").append(t.getId()).append(",");
            json.append("\"title\":\"").append(escapeJson(t.getTitle())).append("\",");
            json.append("\"category\":\"").append(escapeJson(t.getCategory())).append("\",");
            json.append("\"completed\":").append(t.isCompleted()).append(",");
            json.append("\"createdAt\":\"").append(escapeJson(t.getCreatedAt())).append("\"");
            json.append("}");
            if (i < tasks.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        return json.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Self-contained Web UI HTML, CSS & JavaScript.
     */
    private String getWebPageHtml() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Task Management System</title>
            <style>
                :root {
                    --primary: #2563eb;
                    --primary-hover: #1d4ed8;
                    --bg: #f8fafc;
                    --card-bg: #ffffff;
                    --text: #0f172a;
                    --text-muted: #64748b;
                    --border: #e2e8f0;
                    --success: #10b981;
                    --success-bg: #ecfdf5;
                    --danger: #ef4444;
                    --danger-hover: #dc2626;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
                body { background-color: var(--bg); color: var(--text); padding: 30px 20px; min-height: 100vh; }
                .container { max-width: 800px; margin: 0 auto; }
                
                header { margin-bottom: 25px; }
                h1 { font-size: 26px; font-weight: 700; color: #1e293b; margin-bottom: 6px; }
                p.subtitle { color: var(--text-muted); font-size: 14px; }
                
                /* Stats Grid */
                .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-bottom: 25px; }
                .stat-card { background: var(--card-bg); padding: 18px; border-radius: 12px; border: 1px solid var(--border); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .stat-label { font-size: 13px; color: var(--text-muted); font-weight: 500; margin-bottom: 6px; }
                .stat-value { font-size: 24px; font-weight: 700; color: #1e293b; }
                
                .progress-bar-container { background: #e2e8f0; height: 8px; border-radius: 9999px; margin-top: 10px; overflow: hidden; }
                .progress-bar-fill { background: var(--primary); height: 100%; border-radius: 9999px; transition: width 0.3s ease; }
                
                /* Add Task Card */
                .card { background: var(--card-bg); border-radius: 12px; border: 1px solid var(--border); padding: 22px; margin-bottom: 25px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .form-row { display: flex; gap: 10px; flex-wrap: wrap; }
                .form-input { flex: 3; min-width: 220px; padding: 11px 14px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; outline: none; transition: border 0.2s; }
                .form-input:focus { border-color: var(--primary); }
                .form-select { flex: 1; min-width: 140px; padding: 11px 14px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; background: white; outline: none; }
                .btn-add { padding: 11px 20px; background: var(--primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .btn-add:hover { background: var(--primary-hover); }
                
                /* Filter & Search Toolbar */
                .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
                .filter-tabs { display: flex; gap: 6px; background: #e2e8f0; padding: 4px; border-radius: 8px; }
                .filter-tab { padding: 6px 14px; border: none; background: transparent; border-radius: 6px; font-size: 13px; font-weight: 600; color: var(--text-muted); cursor: pointer; }
                .filter-tab.active { background: white; color: var(--text); box-shadow: 0 1px 2px rgba(0,0,0,0.08); }
                .search-input { padding: 8px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 13px; min-width: 200px; outline: none; }
                
                /* Task List */
                .task-list { display: flex; flex-direction: column; gap: 10px; }
                .task-item { display: flex; align-items: center; justify-content: space-between; background: var(--card-bg); border: 1px solid var(--border); padding: 14px 18px; border-radius: 10px; box-shadow: 0 1px 2px rgba(0,0,0,0.04); transition: transform 0.15s, border-color 0.15s; }
                .task-item:hover { border-color: #cbd5e1; }
                .task-left { display: flex; align-items: center; gap: 14px; flex: 1; }
                
                .checkbox-btn { width: 22px; height: 22px; border: 2px solid #cbd5e1; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; background: white; flex-shrink: 0; }
                .checkbox-btn.checked { background: var(--success); border-color: var(--success); color: white; }
                .checkbox-btn svg { width: 14px; height: 14px; stroke: white; stroke-width: 3; fill: none; }
                
                .task-content { flex: 1; }
                .task-title { font-size: 15px; font-weight: 500; color: #1e293b; margin-bottom: 3px; }
                .task-title.completed { text-decoration: line-through; color: #94a3b8; }
                .task-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--text-muted); }
                
                .badge { padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; background: #e0f2fe; color: #0369a1; }
                .btn-delete { background: none; border: none; color: #94a3b8; cursor: pointer; padding: 6px; border-radius: 6px; transition: color 0.2s, background 0.2s; }
                .btn-delete:hover { color: var(--danger); background: #fee2e2; }
                .empty-state { text-align: center; padding: 40px; color: var(--text-muted); font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>Task Management System</h1>
                    <p class="subtitle">Organize, track, and complete your tasks</p>
                </header>

                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-label">Total Tasks</div>
                        <div class="stat-value" id="stat-total">0</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Completed</div>
                        <div class="stat-value" id="stat-completed" style="color: var(--success);">0</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Pending</div>
                        <div class="stat-value" id="stat-pending" style="color: #f59e0b;">0</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Progress</div>
                        <div class="stat-value" id="stat-rate">0%</div>
                        <div class="progress-bar-container">
                            <div class="progress-bar-fill" id="progress-bar" style="width: 0%;"></div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <form id="add-form" class="form-row" onsubmit="handleAddTask(event)">
                        <input type="text" id="task-title" class="form-input" placeholder="What task do you want to accomplish?" required>
                        <select id="task-category" class="form-select">
                            <option value="General">General</option>
                            <option value="Development">Development</option>
                            <option value="QA">QA</option>
                            <option value="Setup">Setup</option>
                            <option value="Work">Work</option>
                            <option value="Personal">Personal</option>
                        </select>
                        <button type="submit" class="btn-add">+ Add Task</button>
                    </form>
                </div>

                <div class="toolbar">
                    <div class="filter-tabs">
                        <button class="filter-tab active" onclick="setFilter('all', this)">All</button>
                        <button class="filter-tab" onclick="setFilter('pending', this)">Pending</button>
                        <button class="filter-tab" onclick="setFilter('completed', this)">Completed</button>
                    </div>
                    <input type="text" id="search-input" class="search-input" placeholder="Search tasks..." oninput="renderTasks()">
                </div>

                <div id="task-list" class="task-list"></div>
            </div>

            <script>
                let allTasks = [];
                let currentFilter = 'all';

                async function fetchTasks() {
                    try {
                        const res = await fetch('/api/tasks');
                        const data = await res.json();
                        allTasks = data.tasks || [];
                        updateStats(data.summary);
                        renderTasks();
                    } catch (e) {
                        console.error("Failed to fetch tasks", e);
                    }
                }

                function updateStats(summary) {
                    if (!summary) return;
                    document.getElementById('stat-total').innerText = summary.total;
                    document.getElementById('stat-completed').innerText = summary.completed;
                    document.getElementById('stat-pending').innerText = summary.pending;
                    document.getElementById('stat-rate').innerText = summary.completionRate + '%';
                    document.getElementById('progress-bar').style.width = summary.completionRate + '%';
                }

                function setFilter(filter, btn) {
                    currentFilter = filter;
                    document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
                    btn.classList.add('active');
                    renderTasks();
                }

                function renderTasks() {
                    const search = (document.getElementById('search-input').value || '').toLowerCase();
                    const container = document.getElementById('task-list');

                    const filtered = allTasks.filter(task => {
                        const matchesFilter = currentFilter === 'all' || 
                            (currentFilter === 'pending' && !task.completed) ||
                            (currentFilter === 'completed' && task.completed);
                        const matchesSearch = task.title.toLowerCase().includes(search) || 
                            task.category.toLowerCase().includes(search);
                        return matchesFilter && matchesSearch;
                    });

                    if (filtered.length === 0) {
                        container.innerHTML = '<div class="empty-state">No tasks found.</div>';
                        return;
                    }

                    container.innerHTML = filtered.map(task => `
                        <div class="task-item">
                            <div class="task-left">
                                <button class="checkbox-btn ${task.completed ? 'checked' : ''}" onclick="toggleTask(${task.id})">
                                    ${task.completed ? '<svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"></polyline></svg>' : ''}
                                </button>
                                <div class="task-content">
                                    <div class="task-title ${task.completed ? 'completed' : ''}">${escapeHtml(task.title)}</div>
                                    <div class="task-meta">
                                        <span class="badge">${escapeHtml(task.category)}</span>
                                        <span>${task.createdAt}</span>
                                    </div>
                                </div>
                            </div>
                            <button class="btn-delete" title="Delete Task" onclick="deleteTask(${task.id})">
                                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                    <polyline points="3 6 5 6 21 6"></polyline>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                </svg>
                            </button>
                        </div>
                    `).join('');
                }

                async function handleAddTask(e) {
                    e.preventDefault();
                    const titleInput = document.getElementById('task-title');
                    const categoryInput = document.getElementById('task-category');
                    const title = titleInput.value.trim();
                    const category = categoryInput.value;

                    if (!title) return;

                    try {
                        const res = await fetch('/api/tasks', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ title, category })
                        });
                        const data = await res.json();
                        allTasks = data.tasks || [];
                        updateStats(data.summary);
                        titleInput.value = '';
                        renderTasks();
                    } catch (err) {
                        console.error('Failed to add task', err);
                    }
                }

                async function toggleTask(id) {
                    try {
                        const res = await fetch(`/api/tasks/toggle?id=${id}`, { method: 'POST' });
                        const data = await res.json();
                        allTasks = data.tasks || [];
                        updateStats(data.summary);
                        renderTasks();
                    } catch (err) {
                        console.error('Failed to toggle task', err);
                    }
                }

                async function deleteTask(id) {
                    if (!confirm('Are you sure you want to delete this task?')) return;
                    try {
                        const res = await fetch(`/api/tasks/delete?id=${id}`, { method: 'POST' });
                        const data = await res.json();
                        allTasks = data.tasks || [];
                        updateStats(data.summary);
                        renderTasks();
                    } catch (err) {
                        console.error('Failed to delete task', err);
                    }
                }

                function escapeHtml(text) {
                    const div = document.createElement('div');
                    div.innerText = text;
                    return div.innerHTML;
                }

                // Initial fetch
                fetchTasks();
            </script>
        </body>
        </html>
        """;
    }
}
