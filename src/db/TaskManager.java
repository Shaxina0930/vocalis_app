package db;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TaskManager {
    private static TaskManager instance;
    private final List<Task> tasks = new ArrayList<>();

    private TaskManager() {}

    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    // === CRUD ===
    public void addTask(Task task) {
        if (task == null || task.getId() == null) return;
        tasks.removeIf(t -> t.getId().equals(task.getId()));
        tasks.add(task);
        TaskStorage.saveTask(task);
    }

    public void addTask(String title, LocalDate date, LocalTime time, String description) {
        addTask(new Task(title, date, time, description));
    }

    public boolean deleteTask(String id) {
    boolean removed = tasks.removeIf(t -> t.getId().equals(id));
    if (removed) {
        TaskStorage.deleteTask(id);  // ✅ ADD THIS LINE
    }
    return removed;
}
    public boolean updateTask(Task updatedTask) {
    for (int i = 0; i < tasks.size(); i++) {
        if (tasks.get(i).getId().equals(updatedTask.getId())) {
            tasks.set(i, updatedTask);
            TaskStorage.saveTask(updatedTask);  // ✅ ADD THIS LINE
            return true;
        }
    }
    return false;
}

    // === READ ===
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksForToday() {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(t -> t.getDate() != null && t.getDate().equals(today))
                .sorted(Comparator.comparing(Task::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksForDate(LocalDate date) {
        if (date == null) return new ArrayList<>();
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .sorted(Comparator.comparing(Task::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Optional<Task> getTaskById(String id) {
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public void clearAllTasks() {
        tasks.clear();
    }


    public void loadTasks(List<Task> loadedTasks) {
        tasks.clear();
        if (loadedTasks != null) tasks.addAll(loadedTasks);
    }
}