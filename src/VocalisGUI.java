import db.TaskManager;
import db.Task;
import STT.AudioCapture;
import STT.SpeechRecognizer;
import STT.VoiceCommandHandler;
import TTS.TextToSpeech;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public class VocalisGUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private DefaultListModel<String> taskListModel;
    private JList<String> taskList;
    
    // STT components
    private AudioCapture audioCapture;
    private SpeechRecognizer speechRecognizer;
    private VoiceCommandHandler voiceCommandHandler;
    private JButton micButton;

    //TTS components
    private boolean isListening = false;
    private TextToSpeech textToSpeech;
    private JButton speakerButton;

    public VocalisGUI() {
        setTitle("Vocalis");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        
        // IMPORTANT: Initialize UI first so chatArea exists
        initUI();
        setLocationRelativeTo(null);
        
        // THEN initialize STT components
        initializeSTT();
        
        loadTasksFromDB();
    }

private void initializeSTT() {
    try {
        audioCapture = new AudioCapture();
        System.out.println("✓ AudioCapture initialized");
        
        speechRecognizer = new SpeechRecognizer();
        System.out.println("✓ SpeechRecognizer initialized");
        
        textToSpeech = new TextToSpeech();
        System.out.println("✓ TextToSpeech initialized");
        voiceCommandHandler = new VoiceCommandHandler();
        appendChat("System: Speech recognition and TTS initialized.");
    } catch (Exception e) {
        System.err.println("Failed to initialize: " + e.getMessage());
        e.printStackTrace();  // This will show the full error
        appendChat("System: Warning - Error: " + e.getMessage());
    }
}

    private void initUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // ignore, use default
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Left (Chat) panel
        JPanel chatPanel = new JPanel(new BorderLayout(10, 10));
        chatPanel.setBorder(new EmptyBorder(12, 12, 12, 6));

        JLabel appTitle = new JLabel("Vocalis");
        appTitle.setFont(appTitle.getFont().deriveFont(Font.BOLD, 18f));
        chatPanel.add(appTitle, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // Input bar (bottom)
        JPanel inputBar = new JPanel();
        inputBar.setLayout(new BorderLayout(8, 8));
        inputBar.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Left side: mic and speaker buttons
        JPanel leftControls = new JPanel();
        leftControls.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));

        micButton = createIconButton("/icons/mic.png", "🎤", "Start/Stop recording");
        speakerButton = createIconButton("/icons/speaker.png", "🔊", "Play/Stop TTS");
        leftControls.add(micButton);
        leftControls.add(speakerButton);

        inputBar.add(leftControls, BorderLayout.WEST);

        // Center: input text field
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        inputBar.add(inputField, BorderLayout.CENTER);

        // Send button
        JButton sendButton = createIconButton("/icons/send.png", "Send", "Send message");
        sendButton.addActionListener(e -> sendMessage());
        inputBar.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputBar, BorderLayout.SOUTH);

        // Tasks panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(12, 6, 12, 12));

        JLabel tasksTitle = new JLabel("Tasks & Scheduler");
        tasksTitle.setFont(tasksTitle.getFont().deriveFont(Font.BOLD, 16f));
        rightPanel.add(tasksTitle, BorderLayout.NORTH);

        // Tasks list
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setFixedCellHeight(36);
        taskList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane taskScroll = new JScrollPane(taskList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        taskScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        rightPanel.add(taskScroll, BorderLayout.CENTER);

        // Task controls
        JPanel taskControls = new JPanel();
        taskControls.setLayout(new BorderLayout(8, 8));
        taskControls.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton addBtn = createIconButton("/icons/add.png", "Add", "Add new task");
        JButton editBtn = createIconButton("/icons/edit.png", "Edit", "Edit selected task");
        JButton delBtn = createIconButton("/icons/delete.png", "Delete", "Delete selected task");

        buttonsPanel.add(addBtn);
        buttonsPanel.add(editBtn);
        buttonsPanel.add(delBtn);
        taskControls.add(buttonsPanel, BorderLayout.NORTH);

        // Scheduler placeholder
        JPanel schedulerPlaceholder = new JPanel();
        schedulerPlaceholder.setLayout(new BorderLayout());
        schedulerPlaceholder.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(8, 8, 8, 8)
        ));
        JLabel schedLabel = new JLabel("<html><b>Scheduler</b><br/><i>Reminders & upcoming tasks</i></html>");
        schedulerPlaceholder.add(schedLabel, BorderLayout.NORTH);

        JTextArea schedArea = new JTextArea(5, 20);
        schedArea.setEditable(false);
        schedArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        schedArea.setText("No reminders yet.");
        schedulerPlaceholder.add(new JScrollPane(schedArea), BorderLayout.CENTER);

        taskControls.add(schedulerPlaceholder, BorderLayout.CENTER);
        rightPanel.add(taskControls, BorderLayout.SOUTH);

        // Button actions
        addBtn.addActionListener(e -> addTask());
        editBtn.addActionListener(e -> editSelectedTask());
        delBtn.addActionListener(e -> deleteSelectedTask());

        // STT action
        micButton.addActionListener(e -> toggleRecording());

        // TTS action
        speakerButton.addActionListener(e -> toggleTTS());

        // Assemble split pane
        splitPane.setLeftComponent(chatPanel);
        splitPane.setRightComponent(rightPanel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    // ===================== STT FUNCTIONALITY =====================

    private void toggleRecording() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (audioCapture == null || speechRecognizer == null) {
            appendChat("System: Speech recognition not available.");
            return;
        }

        isListening = true;
        micButton.setBackground(Color.RED);
        micButton.setText("⏹️");
        appendChat("System: Listening... Speak now.");

        // Start recording in background
        new Thread(() -> {
            audioCapture.startRecording();
        }).start();
    }

    private void stopListening() {
    if (!isListening) return;

    isListening = false;
    micButton.setBackground(null);
    micButton.setText("🎤");
    appendChat("System: Processing speech...");

    // Stop recording and process in background
    new Thread(() -> {
        byte[] audioData = audioCapture.stopRecording();
        
        if (audioData.length > 0) {
            // Recognize speech
            String recognizedText = speechRecognizer.recognize(audioData);
            
            // Update GUI on Swing thread
            SwingUtilities.invokeLater(() -> {
                if (!recognizedText.startsWith("[")) {
                    inputField.setText(recognizedText);
                    appendChat("You: " + recognizedText);
                    
                    // PROCESS VOICE COMMAND
                    VoiceCommandHandler.CommandResult result = voiceCommandHandler.processCommand(recognizedText);
                    
                    if (result.wasCommand && result.response != null) {
                        // It was a voice command
                        appendChat("Vocalis: " + result.response);
                        
                        // Speak the response
                        if (textToSpeech != null) {
                            textToSpeech.speak(result.response);
                        }
                        
                        // Refresh task list if needed
                        loadTasksFromDB();
                    } else if (result.response != null) {
                        // Was trying to be a command but needs clarification
                        appendChat("Vocalis: " + result.response);
                        if (textToSpeech != null) {
                            textToSpeech.speak(result.response);
                        }
                    } else {
                        // Not a command, just show recognized text
                        appendChat("Vocalis: I heard: " + recognizedText);
                    }
                    
                } else {
                    appendChat("System: " + recognizedText);
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                appendChat("System: No audio captured.");
            });
        }
    }).start();
}
    // ===================== TTS FUNCTIONALITY =====================

    private void toggleTTS() {
        if (textToSpeech == null) {
            appendChat("System: Text-to-speech not available.");
            return;
        }

        if (textToSpeech.isPlaying()) {
            // Stop current playback
            textToSpeech.stop();
            speakerButton.setBackground(null);
            speakerButton.setText("🔊");
            appendChat("System: TTS stopped.");
        } else {
            // Get text to speak
            String textToSpeak = getLastBotResponse();
            
            if (textToSpeak == null || textToSpeak.isEmpty()) {
                // If no bot response, speak the input field text
                textToSpeak = inputField.getText().trim();
            }
            
            if (textToSpeak.isEmpty()) {
                appendChat("System: No text to speak. Type something first.");
                return;
            }

            speakerButton.setBackground(Color.GREEN);
            speakerButton.setText("⏸️");
            appendChat("System: Speaking...");
            
            textToSpeech.speak(textToSpeak);
            
            // Reset button after a delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(() -> {
                        if (!textToSpeech.isPlaying()) {
                            speakerButton.setBackground(null);
                            speakerButton.setText("🔊");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private String getLastBotResponse() {
        String chatText = chatArea.getText();
        if (chatText == null || chatText.isEmpty()) {
            return null;
        }

        int lastIndex = chatText.lastIndexOf("Vocalis:");
        if (lastIndex == -1) {
            return null;
        }

        String afterVocalis = chatText.substring(lastIndex + 9).trim();
        
        int newlineIndex = afterVocalis.indexOf("\n");
        if (newlineIndex > 0) {
            afterVocalis = afterVocalis.substring(0, newlineIndex).trim();
        }

        return afterVocalis;
    }

    // ===================== TASK MANAGEMENT =====================

    private void loadTasksFromDB() {
        taskListModel.clear();
        List<Task> tasks = TaskManager.getInstance().getAllTasks();
        for (Task t : tasks) {
            taskListModel.addElement(formatTaskDisplay(t));
        }
    }

    private String formatTaskDisplay(Task t) {
        String date = t.getDate() != null ? t.getDate().toString() : "";
        String time = t.getTime() != null ? t.getTime().toString() : "";
        return "[" + date + " " + time + "] " + t.getTitle();
    }

    private void addTask() {
        String title = JOptionPane.showInputDialog(this, "Task title:");
        if (title == null || title.trim().isEmpty()) return;

        String dateStr = JOptionPane.showInputDialog(this, "Date (YYYY-MM-DD) or leave empty:");
        LocalDate date = null;
        try { if (dateStr != null && !dateStr.isEmpty()) date = LocalDate.parse(dateStr); } catch(Exception ignored) {}

        String timeStr = JOptionPane.showInputDialog(this, "Time (HH:MM) or leave empty:");
        LocalTime time = null;
        try { if (timeStr != null && !timeStr.isEmpty()) time = LocalTime.parse(timeStr); } catch(Exception ignored) {}

        String description = JOptionPane.showInputDialog(this, "Description (optional):");
        if (description == null) description = "";

        Task task = new Task(title, date, time, description);
        TaskManager.getInstance().addTask(task);
        loadTasksFromDB();
    }

    private void editSelectedTask() {
        int index = taskList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a task to edit.");
            return;
        }

        Task task = TaskManager.getInstance().getAllTasks().get(index);

        String title = JOptionPane.showInputDialog(this, "Title:", task.getTitle());
        if (title != null && !title.trim().isEmpty()) task.setTitle(title);

        String dateStr = JOptionPane.showInputDialog(this, "Date (YYYY-MM-DD):", task.getDate());
        try { if (dateStr != null) task.setDate(LocalDate.parse(dateStr)); } catch(Exception ignored) {}

        String timeStr = JOptionPane.showInputDialog(this, "Time (HH:MM):", task.getTime());
        try { if (timeStr != null) task.setTime(LocalTime.parse(timeStr)); } catch(Exception ignored) {}

        String desc = JOptionPane.showInputDialog(this, "Description:", task.getDescription());
        if (desc != null) task.setDescription(desc);

        TaskManager.getInstance().updateTask(task);
        loadTasksFromDB();
    }

    private void deleteSelectedTask() {
        int index = taskList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a task to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Task task = TaskManager.getInstance().getAllTasks().get(index);
        TaskManager.getInstance().deleteTask(task.getId());
        loadTasksFromDB();
    }

    private void sendMessage() {
        String txt = inputField.getText().trim();
        if (txt.isEmpty()) return;
        
        appendChat("You: " + txt);
        inputField.setText("");
        
        // Generate response based on input
        String response = generateResponse(txt.toLowerCase());
        appendChat("Vocalis: " + response);
        
        // AUTOMATICALLY SPEAK THE RESPONSE
        if (textToSpeech != null) {
            textToSpeech.speak(response);
        }
        
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private String generateResponse(String input) {
        if (input.contains("hello") || input.contains("hi")) {
            return "Hello! How can I help you today?";
        } else if (input.contains("how are you")) {
            return "I'm doing great! Thanks for asking.";
        } else if (input.contains("task")) {
            return "You can manage your tasks using the panel on the right.";
        } else if (input.contains("time")) {
            return "The current time is " + java.time.LocalTime.now().toString();
        } else if (input.contains("date")) {
            return "Today is " + java.time.LocalDate.now().toString();
        } else {
            return "I heard you say: " + input;
        }
    }

    private void appendChat(String line) {
        chatArea.append(line + "\n\n");
    }

    private JButton createIconButton(String resourcePath, String fallbackText, String tooltip) {
        JButton btn = new JButton();
        btn.setToolTipText(tooltip);
        ImageIcon icon = loadIcon(resourcePath);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setPreferredSize(new Dimension(42, 36));
            btn.setFocusable(false);
        } else {
            btn.setText(fallbackText);
        }
        return btn;
    }

    private ImageIcon loadIcon(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            is.close();
            Image img = Toolkit.getDefaultToolkit().createImage(bytes);
            ImageIcon icon = new ImageIcon(img);
            Image scaled = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VocalisGUI gui = new VocalisGUI();
            gui.setVisible(true);
        });
    }
}