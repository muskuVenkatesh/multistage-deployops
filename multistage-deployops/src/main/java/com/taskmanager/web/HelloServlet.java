package com.taskmanager.web;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * Full CRUD Task Management Servlet for Apache Tomcat 11.
 * Provides both interactive Web UI and REST API endpoints.
 */
@WebServlet(urlPatterns = {"/", "/hello", "/api/tasks", "/api/tasks/toggle", "/api/tasks/delete"})
public class HelloServlet extends HttpServlet {

    private final TaskService taskService = new TaskService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getServletPath();

        if ("/api/tasks".equals(path)) {
            sendJsonResponse(response, HttpServletResponse.SC_OK, buildTasksJsonResponse(request));
            return;
        }

        // Serve the Web UI for "/" or "/hello"
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println(renderHtmlPage(request));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getServletPath();

        switch (path) {
            case "/api/tasks" -> handleAddTask(request, response);
            case "/api/tasks/toggle" -> handleToggleTask(request, response);
            case "/api/tasks/delete" -> handleDeleteTask(request, response);
            default -> sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, "{\"error\":\"Endpoint not found\"}");
        }
    }

    private void handleAddTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String title = request.getParameter("title");
        String category = request.getParameter("category");

        if (title == null || title.trim().isEmpty()) {
            // Attempt to parse simple JSON if sent as raw body
            String body = readRequestBody(request);
            title = extractJsonValue(body, "title");
            category = extractJsonValue(body, "category");
        }

        if (title == null || title.trim().isEmpty()) {
            sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Task title cannot be empty\"}");
            return;
        }

        taskService.addTask(title, category);
        sendJsonResponse(response, HttpServletResponse.SC_CREATED, buildTasksJsonResponse());
    }

    private void handleToggleTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = extractTaskId(request);
        if (id <= 0) {
            sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid task ID\"}");
            return;
        }

        boolean updated = taskService.toggleCompleted(id);
        if (updated) {
            sendJsonResponse(response, HttpServletResponse.SC_OK, buildTasksJsonResponse());
        } else {
            sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, "{\"error\":\"Task not found\"}");
        }
    }

    private void handleDeleteTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = extractTaskId(request);
        if (id <= 0) {
            sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid task ID\"}");
            return;
        }

        boolean deleted = taskService.deleteTask(id);
        if (deleted) {
            sendJsonResponse(response, HttpServletResponse.SC_OK, buildTasksJsonResponse());
        } else {
            sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, "{\"error\":\"Task not found\"}");
        }
    }

    private int extractTaskId(HttpServletRequest request) throws IOException {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Integer.parseInt(idParam);
            } catch (NumberFormatException ignored) {}
        }
        String body = readRequestBody(request);
        String idStr = extractJsonValue(body, "id");
        if (idStr != null && !idStr.isEmpty()) {
            try {
                return Integer.parseInt(idStr);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private void sendJsonResponse(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        try (PrintWriter out = response.getWriter()) {
            out.print(json);
        }
    }

    public static String detectEnvironment(HttpServletRequest request) {
        String env = System.getenv("APP_ENV");
        if (env == null || env.isBlank()) env = System.getenv("ENVIRONMENT");
        if (env != null && !env.isBlank()) return env.trim().toUpperCase();

        env = System.getProperty("app.env");
        if (env == null || env.isBlank()) env = System.getProperty("environment");
        if (env != null && !env.isBlank()) return env.trim().toUpperCase();

        java.io.File envFile = new java.io.File("/etc/app_env");
        if (envFile.exists()) {
            try {
                String val = java.nio.file.Files.readString(envFile.toPath()).trim();
                if (!val.isEmpty()) return val.toUpperCase();
            } catch (Exception ignored) {}
        }

        if (request != null) {
            String serverName = request.getServerName();
            if (serverName != null) {
                String lower = serverName.toLowerCase();
                if (lower.contains("test") || lower.equals("98.130.122.16") || lower.equals("10.0.11.209")) {
                    return "TEST";
                }
                if (lower.contains("preprod") || lower.contains("pre-prod") || lower.contains("stage")) {
                    return "PRE-PROD";
                }
                if (lower.contains("prod")) {
                    return "PROD";
                }
                if (lower.contains("dev") || lower.equals("16.113.75.120") || lower.equals("16.112.158.179") || lower.equals("10.0.31.163")) {
                    return "DEV";
                }
            }
        }

        return "DEV";
    }

    public static class EnvConfig {
        public final String name;
        public final String label;
        public final String icon;
        public final String badgeBg;
        public final String badgeColor;
        public final String badgeBorder;
        public final String bannerBg;
        public final String bannerColor;
        public final String bannerBorder;

        public EnvConfig(String name) {
            this.name = name;
            switch (name) {
                case "TEST" -> {
                    this.label = "TEST ENVIRONMENT";
                    this.icon = "🔵";
                    this.badgeBg = "#dbeafe";
                    this.badgeColor = "#1e40af";
                    this.badgeBorder = "#93c5fd";
                    this.bannerBg = "#eff6ff";
                    this.bannerColor = "#1e3a8a";
                    this.bannerBorder = "#bfdbfe";
                }
                case "PRE-PROD", "PREPROD", "STAGE", "STAGING" -> {
                    this.label = "PRE-PROD ENVIRONMENT";
                    this.icon = "🟡";
                    this.badgeBg = "#fef3c7";
                    this.badgeColor = "#92400e";
                    this.badgeBorder = "#fcd34d";
                    this.bannerBg = "#fffbeb";
                    this.bannerColor = "#78350f";
                    this.bannerBorder = "#fde68a";
                }
                case "PROD", "PRODUCTION" -> {
                    this.label = "PRODUCTION ENVIRONMENT";
                    this.icon = "🔴";
                    this.badgeBg = "#fee2e2";
                    this.badgeColor = "#991b1b";
                    this.badgeBorder = "#fca5a5";
                    this.bannerBg = "#fef2f2";
                    this.bannerColor = "#7f1d1d";
                    this.bannerBorder = "#fecaca";
                }
                default -> {
                    this.label = "DEV ENVIRONMENT";
                    this.icon = "🟢";
                    this.badgeBg = "#dcfce7";
                    this.badgeColor = "#166534";
                    this.badgeBorder = "#86efac";
                    this.bannerBg = "#f0fdf4";
                    this.bannerColor = "#14532d";
                    this.bannerBorder = "#bbf7d0";
                }
            }
        }
    }

    private String buildTasksJsonResponse() {
        return buildTasksJsonResponse(null);
    }

    private String buildTasksJsonResponse(HttpServletRequest request) {
        List<Task> tasks = taskService.getAllTasks();
        TaskService.TaskSummary summary = taskService.getSummary();
        String env = detectEnvironment(request);
        EnvConfig envConfig = new EnvConfig(env);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"environment\":{");
        sb.append("\"name\":\"").append(envConfig.name).append("\",");
        sb.append("\"label\":\"").append(envConfig.label).append("\",");
        sb.append("\"icon\":\"").append(envConfig.icon).append("\"");
        sb.append("},");
        sb.append("\"summary\":{");
        sb.append("\"total\":").append(summary.total()).append(",");
        sb.append("\"completed\":").append(summary.completed()).append(",");
        sb.append("\"pending\":").append(summary.pending()).append(",");
        sb.append(String.format(Locale.US, "\"completionRate\":%.1f", summary.completionRate()));
        sb.append("},");

        sb.append("\"tasks\":[");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("{");
            sb.append("\"id\":").append(t.getId()).append(",");
            sb.append("\"title\":\"").append(escapeJson(t.getTitle())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(t.getCategory())).append("\",");
            sb.append("\"completed\":").append(t.isCompleted()).append(",");
            sb.append("\"createdAt\":\"").append(escapeJson(t.getCreatedAt())).append("\"");
            sb.append("}");
            if (i < tasks.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]}");
        return sb.toString();
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

    private String renderHtmlPage(HttpServletRequest request) {
        String env = detectEnvironment(request);
        EnvConfig envConfig = new EnvConfig(env);
        String host = (request != null && request.getServerName() != null) ? request.getServerName() : "localhost";

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{ENV_TITLE}}</title>
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
                
                .env-banner {
                    background: {{ENV_BANNER_BG}};
                    color: {{ENV_BANNER_COLOR}};
                    border: 1.5px solid {{ENV_BANNER_BORDER}};
                    padding: 12px 18px;
                    border-radius: 10px;
                    margin-bottom: 22px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    font-weight: 600;
                    font-size: 14px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
                }
                .env-banner-left { display: flex; align-items: center; gap: 10px; }
                .env-banner-title { font-size: 15px; font-weight: 700; letter-spacing: 0.5px; }
                .env-banner-sub { font-size: 12px; font-weight: 400; opacity: 0.85; }
                .env-banner-host { font-size: 12px; font-weight: 600; background: rgba(255,255,255,0.7); padding: 4px 10px; border-radius: 6px; }

                header { margin-bottom: 25px; display: flex; align-items: center; justify-content: space-between; }
                h1 { font-size: 26px; font-weight: 700; color: #1e293b; }
                .badge-env {
                    background: {{ENV_BADGE_BG}};
                    color: {{ENV_BADGE_COLOR}};
                    border: 1.5px solid {{ENV_BADGE_BORDER}};
                    padding: 6px 14px;
                    border-radius: 20px;
                    font-weight: 700;
                    font-size: 13px;
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                }
                p.subtitle { color: var(--text-muted); font-size: 14px; margin-top: 4px; }
                
                /* Stats Grid */
                .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-bottom: 25px; }
                .stat-card { background: var(--card-bg); padding: 18px; border-radius: 12px; border: 1px solid var(--border); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .stat-label { font-size: 13px; color: var(--text-muted); font-weight: 500; margin-bottom: 6px; }
                .stat-value { font-size: 24px; font-weight: 700; color: #1e293b; }
                
                .progress-bar-container { background: #e2e8f0; height: 8px; border-radius: 9999px; margin-top: 10px; overflow: hidden; }
                .progress-bar-fill { background: var(--primary); height: 100%; border-radius: 9999px; transition: width 0.3s ease; }
                
                /* Add Task Form */
                .card { background: var(--card-bg); border-radius: 12px; border: 1px solid var(--border); padding: 22px; margin-bottom: 25px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .form-row { display: flex; gap: 10px; flex-wrap: wrap; }
                .form-input { flex: 3; min-width: 220px; padding: 11px 14px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; outline: none; transition: border 0.2s; }
                .form-input:focus { border-color: var(--primary); }
                .form-select { flex: 1; min-width: 140px; padding: 11px 14px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; background: white; outline: none; }
                .btn-add { padding: 11px 22px; background: var(--primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .btn-add:hover { background: var(--primary-hover); }
                
                /* Toolbar */
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
                .checkbox-btn.checked { background: var(--success); border-color: var(--success); }
                .checkbox-btn svg { width: 14px; height: 14px; stroke: white; stroke-width: 3; fill: none; }
                
                .task-content { flex: 1; }
                .task-title { font-size: 15px; font-weight: 500; color: #1e293b; margin-bottom: 3px; }
                .task-title.completed { text-decoration: line-through; color: #94a3b8; }
                .task-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--text-muted); }
                
                .badge { padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; background: #e0f2fe; color: #0369a1; }
                .btn-delete { background: none; border: none; color: #94a3b8; cursor: pointer; padding: 6px; border-radius: 6px; transition: color 0.2s, background 0.2s; }
                .btn-delete:hover { color: var(--danger); background: #fee2e2; }
                .empty-state { text-align: center; padding: 40px; color: var(--text-muted); font-size: 14px; }
                footer { text-align: center; margin-top: 30px; font-size: 12px; color: var(--text-muted); }
            </style>
        </head>
        <body>
            <div class="container">
                <!-- Multi-Environment Banner -->
                <div class="env-banner">
                    <div class="env-banner-left">
                        <span style="font-size: 20px;">{{ENV_ICON}}</span>
                        <div>
                            <div class="env-banner-title">{{ENV_LABEL}}</div>
                            <div class="env-banner-sub">Automated Multi-Stage Deployment &bull; Apache Tomcat</div>
                        </div>
                    </div>
                    <div class="env-banner-host">
                        Host: {{HOST}}
                    </div>
                </div>

                <header>
                    <div>
                        <h1>Task Management System</h1>
                        <p class="subtitle">Multi-Environment CRUD Application on Apache Tomcat 11</p>
                    </div>
                    <span class="badge-env">{{ENV_ICON}} {{ENV_LABEL}}</span>
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
                        <div class="stat-label">Completion Rate</div>
                        <div class="stat-value" id="stat-rate">0%</div>
                        <div class="progress-bar-container">
                            <div class="progress-bar-fill" id="progress-bar" style="width: 0%;"></div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <form id="add-form" class="form-row" onsubmit="handleAddTask(event)">
                        <input type="text" id="task-title" class="form-input" placeholder="Enter task title (e.g. Configure AWS Security Group)" required>
                        <select id="task-category" class="form-select">
                            <option value="DevOps">DevOps</option>
                            <option value="Development">Development</option>
                            <option value="QA">QA</option>
                            <option value="Setup">Setup</option>
                            <option value="General">General</option>
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

                <footer>
                    Built with Jakarta Servlet &bull; Apache Maven &bull; Apache Tomcat 11
                </footer>
            </div>

            <script>
                const BASE_URL = window.location.pathname.startsWith('/task-manager') ? '/task-manager' : '';
                let allTasks = [];
                let currentFilter = 'all';

                async function fetchTasks() {
                    try {
                        const res = await fetch(BASE_URL + '/api/tasks');
                        const data = await res.json();
                        allTasks = data.tasks || [];
                        updateStats(data.summary);
                        renderTasks();
                    } catch (e) {
                        console.error("Failed to load tasks", e);
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
                                <button class="checkbox-btn ${task.completed ? 'checked' : ''}" title="Toggle Complete" onclick="toggleTask(${task.id})">
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
                        const res = await fetch(BASE_URL + '/api/tasks?title=' + encodeURIComponent(title) + '&category=' + encodeURIComponent(category), {
                            method: 'POST'
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
                        const res = await fetch(BASE_URL + '/api/tasks/toggle?id=' + id, { method: 'POST' });
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
                        const res = await fetch(BASE_URL + '/api/tasks/delete?id=' + id, { method: 'POST' });
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
        """
        .replace("{{ENV_TITLE}}", "Task Management - " + envConfig.label)
        .replace("{{ENV_BANNER_BG}}", envConfig.bannerBg)
        .replace("{{ENV_BANNER_COLOR}}", envConfig.bannerColor)
        .replace("{{ENV_BANNER_BORDER}}", envConfig.bannerBorder)
        .replace("{{ENV_BADGE_BG}}", envConfig.badgeBg)
        .replace("{{ENV_BADGE_COLOR}}", envConfig.badgeColor)
        .replace("{{ENV_BADGE_BORDER}}", envConfig.badgeBorder)
        .replace("{{ENV_ICON}}", envConfig.icon)
        .replace("{{ENV_LABEL}}", envConfig.label)
        .replace("{{HOST}}", host);
    }
}
