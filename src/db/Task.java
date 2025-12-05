package db;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Task {

    private String id;
    private String title;
    private LocalDate date;        // Keep as LocalDate
    private LocalTime time;        // Keep as LocalTime
    private String description;


    public Task(String id, String title, LocalDate date, LocalTime time,
                String description) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.description = description;

    }

    public Task(String title, LocalDate date, LocalTime time, String description) {
        this(UUID.randomUUID().toString(), title, date, time, description);
    }

    public Task() {
        this.id = UUID.randomUUID().toString();

    }

    // GETTERS
    public String getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getDescription() { return description; }


    // SETTERS
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setDescription(String description) { this.description = description; }
 
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", description='" + description +

                '}';
    }
}