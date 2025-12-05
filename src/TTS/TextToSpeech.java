package TTS;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Text-to-Speech using Google Cloud Text-to-Speech API
 * Get API key from: https://console.cloud.google.com/
 */
public class TextToSpeech {
    
    private static final String API_KEY = "AIzaSyDavwHp33naBbJiaf-ohcrAdooG3HdVcJs";
    private static final String API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + API_KEY;
    
    private Clip audioClip;
    private boolean isPlaying = false;

    /**
     * Speak the given text using Google Cloud TTS
     * @param text Text to convert to speech
     */
    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            System.out.println("No text to speak");
            return;
        }

        // Stop any current playback
        stop();

        new Thread(() -> {
            try {
                System.out.println("Speaking: " + text);
                
                // Get audio from Google Cloud TTS
                byte[] audioData = synthesizeSpeech(text);
                
                if (audioData != null && audioData.length > 0) {
                    playAudio(audioData);
                } else {
                    System.err.println("Failed to synthesize speech");
                }
                
            } catch (Exception e) {
                System.err.println("TTS Error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Call Google Cloud TTS API
     */
    private byte[] synthesizeSpeech(String text) {
        try {
            // Escape text for JSON
            String escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"");
            
            // Create JSON request
            String jsonRequest = String.format(
                "{\"input\":{\"text\":\"%s\"},\"voice\":{\"languageCode\":\"en-US\",\"name\":\"en-US-Neural2-F\",\"ssmlGender\":\"FEMALE\"},\"audioConfig\":{\"audioEncoding\":\"LINEAR16\",\"sampleRateHertz\":16000}}",
                escapedText
            );

            // Make HTTP POST request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                return parseAudioContent(response);
            } else {
                String error = readResponse(conn.getErrorStream());
                System.err.println("API Error: " + error);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error synthesizing speech: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String readResponse(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    /**
     * Parse base64 audio from JSON response
     */
    private byte[] parseAudioContent(String jsonResponse) {
        try {
            // Response format: {"audioContent":"base64_encoded_audio"}
            int audioIndex = jsonResponse.indexOf("\"audioContent\"");
            if (audioIndex == -1) {
                return null;
            }
            
            int start = jsonResponse.indexOf("\"", audioIndex + 15) + 1;
            int end = jsonResponse.indexOf("\"", start);
            
            if (start > 0 && end > start) {
                String base64Audio = jsonResponse.substring(start, end);
                return Base64.getDecoder().decode(base64Audio);
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error parsing audio: " + e.getMessage());
            return null;
        }
    }

    private void playAudio(byte[] audioData) {
        try {
            // Convert raw PCM to WAV format
            byte[] wavData = convertToWav(audioData, 16000, 16, 1);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(wavData);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bais);
            
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
            
            isPlaying = true;
            
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    isPlaying = false;
                    System.out.println("Playback finished");
                }
            });
            
            audioClip.start();
            System.out.println("Playing audio...");
            
        } catch (Exception e) {
            System.err.println("Error playing audio: " + e.getMessage());
            e.printStackTrace();
            isPlaying = false;
        }
    }

    private byte[] convertToWav(byte[] pcmData, int sampleRate, int bitsPerSample, int channels) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // WAV header
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + pcmData.length));
        dos.writeBytes("WAVE");
        
        // fmt chunk
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16)); // chunk size
        dos.writeShort(Short.reverseBytes((short) 1)); // PCM format
        dos.writeShort(Short.reverseBytes((short) channels));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(sampleRate * channels * bitsPerSample / 8)); // byte rate
        dos.writeShort(Short.reverseBytes((short) (channels * bitsPerSample / 8))); // block align
        dos.writeShort(Short.reverseBytes((short) bitsPerSample));
        
        // data chunk
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(pcmData.length));
        dos.write(pcmData);
        
        dos.close();
        return baos.toByteArray();
    }

    public void stop() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            audioClip.close();
            System.out.println("Playback stopped");
        }
        isPlaying = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void cleanup() {
        stop();
    }
}
