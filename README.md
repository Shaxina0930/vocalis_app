# Vocalis - Voice-Enabled Task Manager

## Overview
Vocalis is a Java desktop application for managing tasks and reminders with voice input support. It uses PostgreSQL for data persistence and CMU Sphinx for offline speech recognition.

## Features
- Create, edit, and delete tasks with date/time
- Speech-to-text input using microphone
- PostgreSQL database for persistent storage
- Clean split-panel interface with task list
- Auto-load tasks on startup

## Technologies Used
- Java Swing - GUI framework
- PostgreSQL - Database
- JDBC - Database connectivity
- CMU Sphinx - Speech recognition
- Java Time API - Date/time handling

## How to Run the Project

Prerequisites:
- Java JDK 11+
- PostgreSQL installed and running

Database Setup:
CREATE DATABASE reminder_db;

Update credentials in db/TaskStorage.java:
private static final String USER = "postgres";
private static final String PASS = "your_password";

Compile & Run:
javac -cp "lib/*" -d bin src/VocalisGUI.java src/db/*.java
java -cp "bin:lib/*" VocalisGUI

## Project Structure
vocalis/
├── src/
│   ├── VocalisGUI.java       # Main GUI
│   └── db/
│       ├── Task.java         # Task model
│       ├── TaskManager.java  # Task logic (Singleton)
│       ├── TaskStorage.java  # Database operations
│       └── SphinxSTT.java    # Speech recognition
├── lib/
│   └── sphinx4-core.jar
└── icons/

## Future Improvements
- Text-to-speech responses
- Calendar view for tasks
- Natural language processing
- Task priorities and categories
- Notification reminders
- Cloud sync support

## Author
[Your Name]  
University Project - [Course Name]  
[Year]
