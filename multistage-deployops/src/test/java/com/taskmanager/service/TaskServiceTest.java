package com.taskmanager.service;

import com.taskmanager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void testInitialTasksLoaded() {
        List<Task> tasks = service.getAllTasks();
        assertFalse(tasks.isEmpty(), "Initial sample tasks should be pre-loaded");
    }

    @Test
    void testAddTask() {
        int initialCount = service.getAllTasks().size();
        Task task = service.addTask("Automate deployment", "DevOps");

        assertNotNull(task);
        assertTrue(task.getId() > 0);
        assertEquals("Automate deployment", task.getTitle());
        assertEquals("DevOps", task.getCategory());
        assertFalse(task.isCompleted());
        assertEquals(initialCount + 1, service.getAllTasks().size());
    }

    @Test
    void testAddTaskWithEmptyTitleThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.addTask("", "DevOps"));
        assertThrows(IllegalArgumentException.class, () -> service.addTask("   ", "DevOps"));
        assertThrows(IllegalArgumentException.class, () -> service.addTask(null, "DevOps"));
    }

    @Test
    void testFindById() {
        Task created = service.addTask("Find Me", "General");
        Optional<Task> found = service.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getTitle());

        Optional<Task> notFound = service.findById(99999);
        assertFalse(notFound.isPresent());
    }

    @Test
    void testToggleCompleted() {
        Task task = service.addTask("Toggle status", "QA");
        assertFalse(task.isCompleted());

        boolean toggled = service.toggleCompleted(task.getId());
        assertTrue(toggled);
        assertTrue(service.findById(task.getId()).get().isCompleted());

        service.toggleCompleted(task.getId());
        assertFalse(service.findById(task.getId()).get().isCompleted());
    }

    @Test
    void testDeleteTask() {
        Task task = service.addTask("Task to delete", "Cleanup");
        int countBefore = service.getAllTasks().size();

        boolean deleted = service.deleteTask(task.getId());
        assertTrue(deleted);
        assertEquals(countBefore - 1, service.getAllTasks().size());

        assertFalse(service.deleteTask(99999));
    }

    @Test
    void testSearchByTitle() {
        service.addTask("Special unique title keyword123", "SearchTest");
        List<Task> results = service.searchByTitle("keyword123");

        assertEquals(1, results.size());
        assertEquals("Special unique title keyword123", results.get(0).getTitle());
    }

    @Test
    void testSummaryStatistics() {
        TaskService.TaskSummary summary = service.getSummary();
        assertNotNull(summary);
        assertTrue(summary.total() > 0);
        assertEquals(summary.completed() + summary.pending(), summary.total());
    }
}
