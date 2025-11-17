import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * Represents a card in the memory matching game.
 * Each card has an ID, can be flipped, matched, and animated.
 */
public class Card {
    private int id;
    private Button button;
    private BooleanProperty isFlipped;
    private BooleanProperty isMatched;
    private String imageUrl;
    
    // Card styling constants
    private static final String HIDDEN_STYLE = 
        "-fx-font-size: 24px; -fx-font-weight: bold; " +
        "-fx-background-color: #4A90E2; -fx-text-fill: white; " +
        "-fx-background-radius: 10; -fx-border-radius: 10; " +
        "-fx-border-color: #2E5C8A; -fx-border-width: 2;";
    
    private static final String SHOWN_STYLE = 
        "-fx-font-size: 24px; -fx-font-weight: bold; " +
        "-fx-background-color: #7ED321; -fx-text-fill: white; " +
        "-fx-background-radius: 10; -fx-border-radius: 10; " +
        "-fx-border-color: #5BA317; -fx-border-width: 2;";
    
    private static final String MATCHED_STYLE = 
        "-fx-font-size: 24px; -fx-font-weight: bold; " +
        "-fx-background-color: #9013FE; -fx-text-fill: white; " +
        "-fx-background-radius: 10; -fx-border-radius: 10; " +
        "-fx-border-color: #6A0DAD; -fx-border-width: 2; " +
        "-fx-opacity: 0.7;";

    public Card(int id) {
        this.id = id;
        this.isFlipped = new SimpleBooleanProperty(false);
        this.isMatched = new SimpleBooleanProperty(false);
        this.imageUrl = null;
        
        this.button = new Button("?");
        this.button.setPrefSize(100, 100);
        this.button.setMinSize(80, 80);
        this.button.setStyle(HIDDEN_STYLE);
        
        // Add hover effect
        this.button.setOnMouseEntered(e -> {
            if (!isFlipped.get() && !isMatched.get()) {
                button.setStyle(HIDDEN_STYLE.replace("#4A90E2", "#5BA0F2"));
            }
        });
        
        this.button.setOnMouseExited(e -> {
            if (!isFlipped.get() && !isMatched.get()) {
                button.setStyle(HIDDEN_STYLE);
            }
        });
    }

    public int getId() { 
        return id; 
    }
    
    public Button getButton() { 
        return button; 
    }
    
    public boolean isMatched() { 
        return isMatched.get(); 
    }
    
    public void setMatched(boolean matched) { 
        this.isMatched.set(matched);
        if (matched) {
            button.setStyle(MATCHED_STYLE);
            button.setDisable(true);
        }
    }
    
    public BooleanProperty isMatchedProperty() {
        return isMatched;
    }
    
    public boolean isFlipped() {
        return isFlipped.get();
    }
    
    public void setFlipped(boolean flipped) {
        this.isFlipped.set(flipped);
    }
    
    public BooleanProperty isFlippedProperty() {
        return isFlipped;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Flip the card to show its value with animation.
     */
    public void flip() {
        if (isMatched.get()) {
            return;
        }
        
        isFlipped.set(true);
        button.setText(String.valueOf(id));
        
        // Play flip sound
        SoundManager.getInstance().playFlipSound();
        
        // Fade transition for smooth flip
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), button);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.5);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), button);
        fadeIn.setFromValue(0.5);
        fadeIn.setToValue(1.0);
        
        fadeOut.setOnFinished(e -> {
            button.setStyle(SHOWN_STYLE);
            fadeIn.play();
        });
        
        fadeOut.play();
    }

    /**
     * Hide the card (flip back) with animation.
     */
    public void hide() {
        if (isMatched.get()) {
            return;
        }
        
        isFlipped.set(false);
        
        // Fade transition for smooth flip back
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), button);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.5);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), button);
        fadeIn.setFromValue(0.5);
        fadeIn.setToValue(1.0);
        
        fadeOut.setOnFinished(e -> {
            button.setText("?");
            button.setStyle(HIDDEN_STYLE);
            fadeIn.play();
        });
        
        fadeOut.play();
    }
    
    /**
     * Show the card without animation (for immediate display).
     */
    public void show() {
        if (isMatched.get()) {
            return;
        }
        
        isFlipped.set(true);
        button.setText(String.valueOf(id));
        button.setStyle(SHOWN_STYLE);
    }

    /**
     * Reset the card to its initial state.
     */
    public void reset() {
        isFlipped.set(false);
        isMatched.set(false);
        button.setText("?");
        button.setStyle(HIDDEN_STYLE);
        button.setDisable(false);
        button.setOpacity(1.0);
    }
}
