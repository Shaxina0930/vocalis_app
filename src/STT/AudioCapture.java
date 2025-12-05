package STT;

import javax.sound.sampled.*;
import java.io.*;

public class AudioCapture {
    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean isRecording = false;
    private ByteArrayOutputStream audioOutputStream;
    private Thread recordingThread;

    public AudioCapture() {
        // 16kHz, 16-bit, mono - good quality for speech recognition
        format = new AudioFormat(16000, 16, 1, true, false);
    }

    public void startRecording() {
        if (isRecording) {
            System.out.println("Already recording!");
            return;
        }

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone not supported!");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            audioOutputStream = new ByteArrayOutputStream();
            isRecording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            });
            
            recordingThread.start();
            System.out.println("Recording started...");

        } catch (LineUnavailableException e) {
            System.err.println("Error starting microphone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public byte[] stopRecording() {
        if (!isRecording) {
            System.out.println("Not currently recording!");
            return new byte[0];
        }

        isRecording = false;

        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }

        byte[] audioData = audioOutputStream.toByteArray();
        System.out.println("Recording stopped. Captured " + audioData.length + " bytes");
        
        return audioData;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public AudioFormat getFormat() {
        return format;
    }

    // Save audio to WAV file for testing/debugging
    public void saveToWav(byte[] audioData, String filename) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            
            File wavFile = new File(filename);
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
            
            System.out.println("Audio saved to: " + wavFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving audio file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}