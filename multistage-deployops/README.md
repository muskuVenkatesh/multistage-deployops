# Task Management Application

A clean, modular Java application with a web-based browser interface and console CLI, structured according to the official Apache Maven Standard Directory Layout.

---

## 📁 Project Directory Structure

```
JavaApplication/
├── pom.xml                                   # Maven Project Object Model
├── .gitignore                                # Git ignore file
├── README.md                                 # Documentation
└── src/
    ├── main/
    │   ├── java/                             # Application source code
    │   │   └── com/
    │   │       └── taskmanager/
    │   │           ├── Main.java             # Entry point & launcher
    │   │           ├── model/
    │   │           │   └── Task.java         # Task domain entity
    │   │           ├── service/
    │   │           │   └── TaskService.java  # Business logic & thread-safe storage
    │   │           └── web/
    │   │               └── TaskWebServer.java# Built-in HTTP web server & UI
    │   └── resources/                        # Configuration & static assets
    └── test/
        ├── java/                             # Automated test code
        │   └── com/
        │       └── taskmanager/
        │           └── service/
        │               └── TaskServiceTest.java # JUnit 5 test suite
        └── resources/                        # Test configurations
```

---

## 🚀 How to Build and Run with Maven

### 1. Compile the Source Code
```bash
mvn compile
```

### 2. Run Automated Tests
```bash
mvn test
```

### 3. Package into Executable JAR
```bash
mvn clean package
```
*(The packaged JAR is created at `target/task-manager-1.0.0.jar`)*

### 4. Run the Application

#### Option A: Run Packaged JAR
```bash
java -jar target/task-manager-1.0.0.jar
```

#### Option B: Run via Maven Plugin
```bash
mvn exec:java
```

---

## 🌐 Opening the Application in the Browser
Once started, open your web browser (Chrome, Safari, Firefox, Edge) and navigate to:
👉 **http://localhost:8081**

*(Press `Ctrl + C` in your terminal to stop the server)*

---

## 🖥️ Optional Console (CLI) Mode
To run the terminal-based menu instead of the web server:
```bash
java -jar target/task-manager-1.0.0.jar --cli
```
