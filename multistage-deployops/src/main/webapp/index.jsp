<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.taskmanager.web.HelloServlet" %>
<%@ page import="com.taskmanager.web.HelloServlet.EnvConfig" %>
<%
    String env = HelloServlet.detectEnvironment(request);
    EnvConfig envConfig = new EnvConfig(env);
    String host = (request.getServerName() != null) ? request.getServerName() : "localhost";
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Java Multi-Environment Application - <%= envConfig.label %></title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background-color: #f8fafc; color: #0f172a; padding: 40px 20px; min-height: 100vh; display: flex; justify-content: center; align-items: center; }
        .card { background: white; max-width: 650px; width: 100%; border-radius: 16px; border: 1px solid #e2e8f0; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
        .env-banner {
            background: <%= envConfig.bannerBg %>;
            color: <%= envConfig.bannerColor %>;
            border: 1.5px solid <%= envConfig.bannerBorder %>;
            padding: 14px 20px;
            border-radius: 12px;
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .env-title { font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }
        .env-badge {
            background: <%= envConfig.badgeBg %>;
            color: <%= envConfig.badgeColor %>;
            border: 1px solid <%= envConfig.badgeBorder %>;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        h1 { font-size: 24px; font-weight: 700; color: #1e293b; margin-bottom: 8px; }
        p.desc { color: #64748b; font-size: 15px; margin-bottom: 24px; line-height: 1.5; }
        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
        .info-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; }
        .info-label { font-size: 12px; color: #64748b; font-weight: 600; text-transform: uppercase; margin-bottom: 4px; }
        .info-value { font-size: 14px; font-weight: 600; color: #1e293b; word-break: break-all; }
        .links { display: flex; gap: 12px; flex-wrap: wrap; }
        .btn { display: inline-block; padding: 10px 20px; border-radius: 8px; font-size: 14px; font-weight: 600; text-decoration: none; transition: background 0.2s; }
        .btn-primary { background: #2563eb; color: white; }
        .btn-primary:hover { background: #1d4ed8; }
        .btn-secondary { background: #e2e8f0; color: #1e293b; }
        .btn-secondary:hover { background: #cbd5e1; }
    </style>
</head>
<body>
    <div class="card">
        <div class="env-banner">
            <div style="display: flex; align-items: center; gap: 10px;">
                <span style="font-size: 22px;"><%= envConfig.icon %></span>
                <span class="env-title"><%= envConfig.label %></span>
            </div>
            <span class="env-badge">Active Deployment</span>
        </div>

        <h1>Java Multi-Environment Application</h1>
        <p class="desc">
            This application is running live on <strong>Apache Tomcat 10/11</strong> and was automatically deployed via your GitHub Actions multi-stage pipeline.
        </p>

        <div class="info-grid">
            <div class="info-box">
                <div class="info-label">Environment</div>
                <div class="info-value"><%= envConfig.icon %> <%= envConfig.label %></div>
            </div>
            <div class="info-box">
                <div class="info-label">Server Host IP</div>
                <div class="info-value"><%= host %></div>
            </div>
        </div>

        <div class="links">
            <a href="<%= contextPath %>/hello" class="btn btn-primary">Open Task Management Web UI &rarr;</a>
            <a href="<%= contextPath %>/health" class="btn btn-secondary">Check Health Endpoint</a>
        </div>
    </div>
</body>
</html>
