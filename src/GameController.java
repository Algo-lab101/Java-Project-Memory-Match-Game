import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import java.util.*;
import java.util.Timer;

/**
 * Game controller for managing memory match game logic, state, and interactions.
 */
public class GameController {
    public enum Difficulty {
        EASY(3, 4, 1.0),    // 3x4 grid, 6 pairs
        MEDIUM(4, 4, 1.5),  // 4x4 grid, 8 pairs
        HARD(4, 6, 2.0);    // 4x6 grid, 12 pairs
        
        private final int rows;
        private final int cols;
        private final double multiplier;
        
        Difficulty(int rows, int cols, double multiplier) {
            this.rows = rows;
            this.cols = cols;
            this.multiplier = multiplier;
        }
        
        public int getRows() { return rows; }
        public int getCols() { return cols; }
        public int getTotalCards() { return rows * cols; }
        public int getPairs() { return getTotalCards() / 2; }
        public double getMultiplier() { return multiplier; }
    }
    
    private List<Card> cards;
    private GridPane grid;
    private Difficulty difficulty;
    private Card firstCard;
    private int score;
    private int moves;
    private int timeSeconds;
    private int matchedPairs;
    private boolean isPaused;
    private boolean gameStarted;
    private boolean gameWon;
    
    private Timeline timer;
    private Runnable onGameWonCallback;
    private Runnable onTimeUpdateCallback;
    private Runnable onScoreUpdateCallback;
    private Runnable onMovesUpdateCallback;
    
    public GameController(GridPane grid, Difficulty difficulty) {
        this.grid = grid;
        this.difficulty = difficulty;
        this.cards = new ArrayList<>();
        this.score = 0;
        this.moves = 0;
        this.timeSeconds = 0;
        this.matchedPairs = 0;
        this.isPaused = false;
        this.gameStarted = false;
        this.gameWon = false;
        this.firstCard = null;
        
        initializeGame();
    }
    
    /**
     * Initialize the game by creating and shuffling cards.
     */
    private void initializeGame() {
        grid.getChildren().clear();
        cards.clear();
        
        int pairs = difficulty.getPairs();
        List<Integer> ids = new ArrayList<>();
        
        // Create pairs
        for (int i = 1; i <= pairs; i++) {
            ids.add(i);
            ids.add(i);
        }
        
        Collections.shuffle(ids);
        
        // Create cards and add to grid
        for (int i = 0; i < ids.size(); i++) {
            Card card = new Card(ids.get(i));
            int row = i / difficulty.getCols();
            int col = i % difficulty.getCols();
            
            grid.add(card.getButton(), col, row);
            card.getButton().setOnAction(e -> handleCardClick(card));
            cards.add(card);
        }
        
        // Adjust grid spacing
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-alignment: center;");
    }
    
    /**
     * Start the game timer.
     */
    public void startGame() {
        if (gameStarted && !isPaused) {
            return;
        }
        
        gameStarted = true;
        isPaused = false;
        
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isPaused && !gameWon) {
                timeSeconds++;
                if (onTimeUpdateCallback != null) {
                    onTimeUpdateCallback.run();
                }
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }
    
    /**
     * Pause or resume the game.
     */
    public void togglePause() {
        isPaused = !isPaused;
        
        // Disable/enable all card buttons
        for (Card card : cards) {
            if (!card.isMatched()) {
                card.getButton().setDisable(isPaused);
            }
        }
    }
    
    /**
     * Reset the game to initial state.
     */
    public void reset() {
        if (timer != null) {
            timer.stop();
        }
        
        score = 0;
        moves = 0;
        timeSeconds = 0;
        matchedPairs = 0;
        isPaused = false;
        gameStarted = false;
        gameWon = false;
        firstCard = null;
        
        initializeGame();
        
        if (onScoreUpdateCallback != null) {
            onScoreUpdateCallback.run();
        }
        if (onMovesUpdateCallback != null) {
            onMovesUpdateCallback.run();
        }
        if (onTimeUpdateCallback != null) {
            onTimeUpdateCallback.run();
        }
    }
    
    /**
     * Handle card click event.
     */
    private void handleCardClick(Card card) {
        if (isPaused || gameWon || card.isMatched() || card.isFlipped() || card == firstCard) {
            return;
        }
        
        // Start timer on first card click
        if (!gameStarted) {
            startGame();
        }
        
        card.flip();
        
        if (firstCard == null) {
            firstCard = card;
        } else {
            // Second card selected
            moves++;
            if (onMovesUpdateCallback != null) {
                onMovesUpdateCallback.run();
            }
            
            // Disable all cards temporarily
            for (Card c : cards) {
                if (!c.isMatched()) {
                    c.getButton().setDisable(true);
                }
            }
            
            if (firstCard.getId() == card.getId()) {
                // Match found!
                // Play match sound
                SoundManager.getInstance().playMatchSound();
                
                firstCard.setMatched(true);
                card.setMatched(true);
                matchedPairs++;
                
                // Calculate score: (1000 - time - moves*10) * difficulty_multiplier
                int baseScore = Math.max(0, 1000 - timeSeconds - moves * 10);
                score = (int)(baseScore * difficulty.getMultiplier());
                
                if (onScoreUpdateCallback != null) {
                    onScoreUpdateCallback.run();
                }
                
                firstCard = null;
                
                // Re-enable unmatched cards
                for (Card c : cards) {
                    if (!c.isMatched() && !c.isFlipped()) {
                        c.getButton().setDisable(false);
                    }
                }
                
                // Check win condition
                if (matchedPairs >= difficulty.getPairs()) {
                    gameWon = true;
                    if (timer != null) {
                        timer.stop();
                    }
                    if (onGameWonCallback != null) {
                        Platform.runLater(() -> onGameWonCallback.run());
                    }
                }
            } else {
                // No match - play mismatch sound and flip back after delay
                SoundManager.getInstance().playMismatchSound();
                
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            firstCard.hide();
                            card.hide();
                            firstCard = null;
                            
                            // Re-enable all unmatched cards
                            for (Card c : cards) {
                                if (!c.isMatched()) {
                                    c.getButton().setDisable(false);
                                }
                            }
                        });
                    }
                }, 1000);
            }
        }
    }
    
    // Getters
    public int getScore() {
        return score;
    }
    
    public int getMoves() {
        return moves;
    }
    
    public int getTimeSeconds() {
        return timeSeconds;
    }
    
    public String getFormattedTime() {
        int minutes = timeSeconds / 60;
        int seconds = timeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public boolean isGameWon() {
        return gameWon;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    // Callback setters
    public void setOnGameWon(Runnable callback) {
        this.onGameWonCallback = callback;
    }
    
    public void setOnTimeUpdate(Runnable callback) {
        this.onTimeUpdateCallback = callback;
    }
    
    public void setOnScoreUpdate(Runnable callback) {
        this.onScoreUpdateCallback = callback;
    }
    
    public void setOnMovesUpdate(Runnable callback) {
        this.onMovesUpdateCallback = callback;
    }
    
    /**
     * Clean up resources when game controller is no longer needed.
     */
    public void cleanup() {
        if (timer != null) {
            timer.stop();
        }
    }
}
