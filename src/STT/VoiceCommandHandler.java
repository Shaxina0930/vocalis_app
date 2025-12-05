package STT;

import db.TaskManager;
import db.Task;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles voice commands for task management
 */
public class VoiceCommandHandler {
    
    private TaskManager taskManager;
    
    public VoiceCommandHandler() {
        this.taskManager = TaskManager.getInstance();
    }
    
    /**
     * Process a voice command and return a response
     * @param command The recognized speech text
     * @return Response message
     */
    public CommandResult processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandResult(false, "No command recognized");
        }
        
        String lowerCommand = command.toLowerCase().trim();
        
        // ADD TASK commands
        if (lowerCommand.contains("add task") || lowerCommand.contains("create task") || 
            lowerCommand.contains("new task") || lowerCommand.contains("add a task")) {
            return handleAddTask(command);
        }
        
        // DELETE TASK commands
        if (lowerCommand.contains("delete task") || lowerCommand.contains("remove task") || 
            lowerCommand.contains("delete the task") || lowerCommand.contains("remove the task")) {
            return handleDeleteTask(command);
        }
        
        // LIST TASKS commands
        if (lowerCommand.contains("list task") || lowerCommand.contains("show task") || 
            lowerCommand.contains("what are my task") || lowerCommand.contains("read my task")) {
            return handleListTasks();
        }
        
        // CLEAR ALL TASKS
        if (lowerCommand.contains("clear all task") || lowerCommand.contains("delete all task")) {
            return handleClearAllTasks();
        }
        
        // COUNT TASKS
        if (lowerCommand.contains("how many task")) {
            return handleCountTasks();
        }
        
        // HELP
        if (lowerCommand.contains("help") || lowerCommand.contains("what can you do")) {
            return new CommandResult(false, 
                "I can help you with: Add task, Delete task, List tasks, Clear all tasks. " +
                "Try saying 'add task buy groceries' or 'list tasks'");
        }
        
        // No command recognized
        return new CommandResult(false, null);
    }
    
    /**
     * Handle "add task" command
     * Examples: 
     * - "add task buy groceries"
     * - "create task meeting at 3pm"
     * - "new task call mom tomorrow"
     */
    private CommandResult handleAddTask(String command) {
        String lowerCommand = command.toLowerCase();
        
        // Extract task title
        String title = extractTaskTitle(lowerCommand);
        if (title == null || title.isEmpty()) {
            return new CommandResult(true, "What task would you like to add? Please say 'add task' followed by the task name.");
        }
        
        // Extract date if mentioned
        LocalDate date = extractDate(lowerCommand);
        
        // Extract time if mentioned
        LocalTime time = extractTime(lowerCommand);
        
        // Create task
        Task task = new Task(title, date, time, "");
        taskManager.addTask(task);
        
        String response = "Task added: " + title;
        if (date != null) response += " on " + date;
        if (time != null) response += " at " + time;
        
        return new CommandResult(true, response);
    }
    
    /**
     * Extract task title from command
     */
    private String extractTaskTitle(String command) {
        // Remove command keywords
        String title = command.replaceAll("(add task|create task|new task|add a task)", "").trim();
        
        // Remove time/date related words
        title = title.replaceAll("(today|tomorrow|at \\d|on \\w+)", "").trim();
        
        return title.isEmpty() ? null : capitalize(title);
    }
    
    /**
     * Extract date from command (today, tomorrow, specific dates)
     */
    private LocalDate extractDate(String command) {
        if (command.contains("today")) {
            return LocalDate.now();
        }
        if (command.contains("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }
        
        // Try to find date patterns like "on monday", "next friday"
        // For simplicity, we'll skip complex date parsing for now
        
        return null;
    }
    
    /**
     * Extract time from command (3pm, 15:00, three o'clock)
     */
    private LocalTime extractTime(String command) {
        // Pattern for "3pm", "3 pm", "15:00"
        Pattern timePattern = Pattern.compile("(\\d{1,2})\\s*(am|pm|:\\d{2})?");
        Matcher matcher = timePattern.matcher(command);
        
        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                String ampm = matcher.group(2);
                
                if (ampm != null && ampm.contains("pm") && hour < 12) {
                    hour += 12;
                }
                
                return LocalTime.of(hour, 0);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        return null;
    }
    
    /**
     * Handle "delete task" command
     * Examples:
     * - "delete task buy groceries"
     * - "remove task 1"
     */
    private CommandResult handleDeleteTask(String command) {
        String lowerCommand = command.toLowerCase();
        
        // Extract task identifier
        String taskIdentifier = lowerCommand
            .replaceAll("(delete task|remove task|delete the task|remove the task)", "")
            .trim();
        
        if (taskIdentifier.isEmpty()) {
            return new CommandResult(true, "Which task would you like to delete?");
        }
        
        List<Task> tasks = taskManager.getAllTasks();
        
        // Try to find by number (e.g., "delete task 1")
        try {
            int taskNumber = Integer.parseInt(taskIdentifier);
            if (taskNumber > 0 && taskNumber <= tasks.size()) {
                Task task = tasks.get(taskNumber - 1);
                taskManager.deleteTask(task.getId());
                return new CommandResult(true, "Deleted task: " + task.getTitle());
            }
        } catch (NumberFormatException e) {
            // Not a number, try to match by title
        }
        
        // Try to find by partial title match
        for (Task task : tasks) {
            if (task.getTitle().toLowerCase().contains(taskIdentifier)) {
                taskManager.deleteTask(task.getId());
                return new CommandResult(true, "Deleted task: " + task.getTitle());
            }
        }
        
        return new CommandResult(true, "Could not find task: " + taskIdentifier);
    }
    
    /**
     * Handle "list tasks" command
     */
    private CommandResult handleListTasks() {
        List<Task> tasks = taskManager.getAllTasks();
        
        if (tasks.isEmpty()) {
            return new CommandResult(true, "You have no tasks.");
        }
        
        StringBuilder response = new StringBuilder("You have " + tasks.size() + " task");
        if (tasks.size() > 1) response.append("s");
        response.append(": ");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            response.append((i + 1)).append(". ").append(task.getTitle());
            if (i < tasks.size() - 1) {
                response.append(", ");
            }
        }
        
        return new CommandResult(true, response.toString());
    }
    
    /**
     * Handle "clear all tasks" command
     */
    private CommandResult handleClearAllTasks() {
        List<Task> tasks = taskManager.getAllTasks();
        int count = tasks.size();
        
        for (Task task : tasks) {
            taskManager.deleteTask(task.getId());
        }
        
        return new CommandResult(true, "Deleted all " + count + " tasks.");
    }
    
    /**
     * Handle "how many tasks" command
     */
    private CommandResult handleCountTasks() {
        int count = taskManager.getAllTasks().size();
        String response = "You have " + count + " task";
        if (count != 1) response += "s";
        return new CommandResult(true, response + ".");
    }
    
    /**
     * Capitalize first letter of string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Result of command processing
     */
    public static class CommandResult {
        public final boolean wasCommand;
        public final String response;
        
        public CommandResult(boolean wasCommand, String response) {
            this.wasCommand = wasCommand;
            this.response = response;
        }
    }
}