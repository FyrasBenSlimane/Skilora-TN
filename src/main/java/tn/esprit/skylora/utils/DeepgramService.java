package tn.esprit.skylora.utils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class DeepgramService {
    //////6ea6cb427382abdf1987c86e2f1e6300569e8379
    private static final String API_KEY = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    private static final String API_URL = "https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true";

    /**
     * Sends an audio stream to Deepgram for transcription.
     * 
     * @param audioData Byte array of the audio (WAV, MP3, etc.)
     * @return The transcribed text or an empty string if failed.
     */
    public static String transcribe(byte[] audioData) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Token " + API_KEY)
                    .header("Content-Type", "audio/wav")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String transcript = json.getJSONObject("results")
                        .getJSONArray("channels")
                        .getJSONObject(0)
                        .getJSONArray("alternatives")
                        .getJSONObject(0)
                        .getString("transcript");
                
                // Remove periods that appear after each word
                return transcript.replaceAll("\\.\\s*", " ").trim();
            } else {
                System.err.println("Deepgram Error: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
