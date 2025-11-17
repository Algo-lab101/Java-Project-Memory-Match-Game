import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Sound manager for handling sound effects using Freesound API.
 * Downloads and caches audio files for efficient playback.
 */
public class SoundManager {
    private static final String FREESOUND_API_TOKEN = "xdm75dXQxsq5WMtg79spmL0cKc17ufoelBwD1KgP";
    private static final String FREESOUND_API_BASE = "https://freesound.org/apiv2";
    private static final String CACHE_DIR = "sounds_cache";
    
    private static SoundManager instance;
    private MediaPlayer flipSoundPlayer;
    private MediaPlayer matchSoundPlayer;
    private MediaPlayer mismatchSoundPlayer;
    private boolean soundsEnabled = true;
    
    private SoundManager() {
        // Create cache directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
        } catch (IOException e) {
            System.err.println("Could not create sounds cache directory: " + e.getMessage());
        }
        
        // Initialize sounds
        initializeSounds();
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    /**
     * Initialize sound effects by fetching from Freesound API or using cached files.
     */
    private void initializeSounds() {
        try {
            // Get piano sound for card flip
            String flipSoundUrl = getSoundUrl("piano", "flip");
            if (flipSoundUrl != null) {
                flipSoundPlayer = createMediaPlayer(flipSoundUrl);
            }
            
            // Get success sound for match (try different queries)
            String matchSoundUrl = getSoundUrl("success", "match");
            if (matchSoundUrl == null) {
                matchSoundUrl = getSoundUrl("piano", "match");
            }
            if (matchSoundUrl == null) {
                matchSoundUrl = getSoundUrl("bell", "match");
            }
            if (matchSoundUrl != null) {
                matchSoundPlayer = createMediaPlayer(matchSoundUrl);
            }
            
            // Get error sound for mismatch
            String mismatchSoundUrl = getSoundUrl("error", "mismatch");
            if (mismatchSoundUrl == null) {
                mismatchSoundUrl = getSoundUrl("whoosh", "mismatch");
            }
            if (mismatchSoundUrl == null) {
                mismatchSoundUrl = getSoundUrl("click", "mismatch");
            }
            if (mismatchSoundUrl != null) {
                mismatchSoundPlayer = createMediaPlayer(mismatchSoundUrl);
            }
        } catch (Exception e) {
            System.err.println("Error initializing sounds: " + e.getMessage());
            e.printStackTrace();
            // Continue without sounds if API fails
            soundsEnabled = false;
        }
    }
    
    /**
     * Search Freesound API for a sound and return the preview URL.
     * Uses simple string parsing to extract JSON data without external libraries.
     */
    private String getSoundUrl(String query, String soundType) {
        try {
            // Check cache first
            String cacheFile = CACHE_DIR + File.separator + soundType + ".mp3";
            if (Files.exists(Paths.get(cacheFile))) {
                return new File(cacheFile).toURI().toString();
            }
            
            // Search Freesound API
            String searchUrl = FREESOUND_API_BASE + "/search/text/?query=" + 
                              query.replace(" ", "%20") + 
                              "&token=" + FREESOUND_API_TOKEN + 
                              "&fields=id,name,previews&page_size=1";
            
            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // Parse JSON response using simple string parsing
                String jsonResponse = response.toString();
                String previewUrl = extractPreviewUrl(jsonResponse);
                
                if (previewUrl != null && !previewUrl.isEmpty()) {
                    // Download and cache the file
                    downloadAndCache(previewUrl, cacheFile);
                    return new File(cacheFile).toURI().toString();
                }
            } else {
                System.err.println("Freesound API returned error code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching sound from Freesound API for " + soundType + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract preview URL from JSON response using string parsing.
     * Looks for "preview-hq-mp3" or "preview-mp3" in the JSON.
     */
    private String extractPreviewUrl(String json) {
        try {
            // Look for preview-hq-mp3 first (higher quality)
            int hqIndex = json.indexOf("\"preview-hq-mp3\"");
            if (hqIndex != -1) {
                int colonIndex = json.indexOf(":", hqIndex);
                int startQuote = json.indexOf("\"", colonIndex) + 1;
                int endQuote = json.indexOf("\"", startQuote);
                if (endQuote > startQuote) {
                    return json.substring(startQuote, endQuote);
                }
            }
            
            // Fallback to preview-mp3
            int mp3Index = json.indexOf("\"preview-mp3\"");
            if (mp3Index != -1) {
                int colonIndex = json.indexOf(":", mp3Index);
                int startQuote = json.indexOf("\"", colonIndex) + 1;
                int endQuote = json.indexOf("\"", startQuote);
                if (endQuote > startQuote) {
                    return json.substring(startQuote, endQuote);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Download audio file and save to cache.
     */
    private void downloadAndCache(String urlString, String cacheFile) {
        try {
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                Files.copy(in, Paths.get(cacheFile), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✓ Cached sound file: " + cacheFile);
            }
        } catch (Exception e) {
            System.err.println("Error downloading sound file: " + e.getMessage());
        }
    }
    
    /**
     * Create a MediaPlayer from a sound URL.
     */
    private MediaPlayer createMediaPlayer(String soundUrl) {
        try {
            Media media = new Media(soundUrl);
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(0.5); // Set volume to 50%
            return player;
        } catch (Exception e) {
            System.err.println("Error creating MediaPlayer: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Play card flip sound.
     */
    public void playFlipSound() {
        if (soundsEnabled && flipSoundPlayer != null) {
            try {
                flipSoundPlayer.stop();
                flipSoundPlayer.seek(javafx.util.Duration.ZERO);
                flipSoundPlayer.play();
            } catch (Exception e) {
                // Silently fail if sound can't play
            }
        }
    }
    
    /**
     * Play match sound.
     */
    public void playMatchSound() {
        if (soundsEnabled && matchSoundPlayer != null) {
            try {
                matchSoundPlayer.stop();
                matchSoundPlayer.seek(javafx.util.Duration.ZERO);
                matchSoundPlayer.play();
            } catch (Exception e) {
                // Silently fail if sound can't play
            }
        }
    }
    
    /**
     * Play mismatch sound.
     */
    public void playMismatchSound() {
        if (soundsEnabled && mismatchSoundPlayer != null) {
            try {
                mismatchSoundPlayer.stop();
                mismatchSoundPlayer.seek(javafx.util.Duration.ZERO);
                mismatchSoundPlayer.play();
            } catch (Exception e) {
                // Silently fail if sound can't play
            }
        }
    }
    
    /**
     * Enable or disable sounds.
     */
    public void setSoundsEnabled(boolean enabled) {
        this.soundsEnabled = enabled;
    }
    
    /**
     * Check if sounds are enabled.
     */
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (flipSoundPlayer != null) {
            flipSoundPlayer.dispose();
        }
        if (matchSoundPlayer != null) {
            matchSoundPlayer.dispose();
        }
        if (mismatchSoundPlayer != null) {
            mismatchSoundPlayer.dispose();
        }
    }
}
