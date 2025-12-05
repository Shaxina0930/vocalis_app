package STT;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Google Cloud Speech-to-Text using only core Java.
 * No JSON libraries required.
 */
public class SpeechRecognizer {

    // Insert your API key here
    private static final String API_KEY = "AIzaSyC2ESlWWkZUTFXyFSqnNQ2v5HzP2fBmfbg";

    private static final String API_URL =
            "https://speech.googleapis.com/v1/speech:recognize?key=" + API_KEY;

    /**
     * Recognize speech from WAV byte array.
     */
    public String recognize(byte[] audioBytes) {

        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            return "[Error: please insert your Google Cloud API key]";
        }

        try {
            // Convert WAV to base64
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // Build JSON manually (no org.json needed)
            String jsonBody =
                    "{ \"config\": { " +
                            "\"encoding\": \"LINEAR16\", " +
                            "\"sampleRateHertz\": 16000, " +
                            "\"languageCode\": \"en-US\"" +
                            " }, \"audio\": { " +
                            "\"content\": \"" + base64Audio + "\"" +
                            " } }";

            // Send POST request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Write JSON body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // Handle response
            int status = conn.getResponseCode();
            InputStream input = (status == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String response = readStream(input);

            if (status != 200) {
                return "[API Error] " + response;
            }

            // Extract transcript manually
            return extractTranscript(response);

        } catch (Exception e) {
            e.printStackTrace();
            return "[Error: " + e.getMessage() + "]";
        }
    }

    /**
     * Read entire InputStream into a String.
     */
    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    /**
     * Manually extract "transcript" value from JSON.
     * Google response example:
     * {"results":[{"alternatives":[{"transcript":"hello world"}]}]}
     */
    private String extractTranscript(String json) {
        String key = "\"transcript\":";
        int idx = json.indexOf(key);
        if (idx == -1) return "[No speech recognized]";

        int firstQuote = json.indexOf("\"", idx + key.length());
        int secondQuote = json.indexOf("\"", firstQuote + 1);

        if (firstQuote == -1 || secondQuote == -1) return "[No speech recognized]";

        return json.substring(firstQuote + 1, secondQuote);
    }

    public void cleanup() {
        // nothing to clean
    }
}
