import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.util.List;

/**
 * Main JavaFX application for the Memory Match Game.
 * Manages all screens and scene transitions.
 */
public class MemoryGameApp extends Application {
    private Stage primaryStage;
    private Scene mainMenuScene;
    private Scene gameScene;
    private Scene endGameScene;
    private Scene leaderboardScene;
    
    // Current game state
    private GameController currentGameController;
    private int currentPlayerId = -1;
    private String currentPlayerName = "Guest";
    private GameController.Difficulty currentDifficulty = GameController.Difficulty.MEDIUM;
    
    // UI Components that need updates
    private Label welcomeLabel;
    private Label scoreLabel;
    private Label timerLabel;
    private Label movesLabel;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // Initialize database
        DatabaseHelper.initializeDatabase();
        
        // Request player name on first launch
        requestPlayerName();
        
        // Create all scenes
        createMainMenuScene();
        createLeaderboardScene();
        
        // Show main menu
        stage.setScene(mainMenuScene);
        stage.setTitle("Memory Match Game");
        stage.setMinWidth(600);
        stage.setMinHeight(600);
        stage.show();
        
        // Cleanup on close
        stage.setOnCloseRequest(e -> {
            if (currentGameController != null) {
                currentGameController.cleanup();
            }
            SoundManager.getInstance().cleanup();
        });
    }
    
    /**
     * Request player name via dialog.
     */
    private void requestPlayerName() {
        TextInputDialog dialog = new TextInputDialog("Player");
        dialog.setTitle("Welcome!");
        dialog.setHeaderText("Enter Your Name");
        dialog.setContentText("Please enter your name:");
        
        dialog.showAndWait().ifPresent(name -> {
            if (name != null && !name.trim().isEmpty()) {
                currentPlayerName = name.trim();
                currentPlayerId = DatabaseHelper.createPlayer(currentPlayerName);
                if (welcomeLabel != null) {
                    welcomeLabel.setText("Welcome, " + currentPlayerName + "!");
                }
            }
        });
    }
    
    /**
     * Create the main menu scene.
     */
    private void createMainMenuScene() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
        
        // Title
        Label title = new Label("🧠 Memory Match Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        
        // Welcome label
        welcomeLabel = new Label("Welcome, " + currentPlayerName + "!");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        welcomeLabel.setTextFill(Color.WHITE);
        
        // Buttons
        Button startButton = createStyledButton("Start Game", 200, 50);
        startButton.setOnAction(e -> showDifficultyDialog());
        
        Button leaderboardButton = createStyledButton("Leaderboard", 200, 50);
        leaderboardButton.setOnAction(e -> showLeaderboard());
        
        Button settingsButton = createStyledButton("Settings", 200, 50);
        settingsButton.setOnAction(e -> showSettings());
        
        Button exitButton = createStyledButton("Exit", 200, 50);
        exitButton.setOnAction(e -> primaryStage.close());
        
        root.getChildren().addAll(title, welcomeLabel, startButton, leaderboardButton, settingsButton, exitButton);
        
        mainMenuScene = new Scene(root, 600, 600);
    }
    
    /**
     * Show difficulty selection dialog and start game.
     */
    private void showDifficultyDialog() {
        Dialog<GameController.Difficulty> dialog = new Dialog<>();
        dialog.setTitle("Select Difficulty");
        dialog.setHeaderText("Choose your difficulty level:");
        
        ButtonType easyButton = new ButtonType("Easy (3x4)", ButtonBar.ButtonData.OK_DONE);
        ButtonType mediumButton = new ButtonType("Medium (4x4)", ButtonBar.ButtonData.OK_DONE);
        ButtonType hardButton = new ButtonType("Hard (4x6)", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(easyButton, mediumButton, hardButton, cancelButton);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == easyButton) return GameController.Difficulty.EASY;
            if (buttonType == mediumButton) return GameController.Difficulty.MEDIUM;
            if (buttonType == hardButton) return GameController.Difficulty.HARD;
            return null;
        });
        
        dialog.showAndWait().ifPresent(difficulty -> {
            if (difficulty != null) {
                currentDifficulty = difficulty;
                showGameScreen();
            }
        });
    }
    
    /**
     * Create and show the game screen.
     */
    private void showGameScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");
        
        // Top panel with stats
        HBox topPanel = new HBox(20);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(15));
        topPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-background-radius: 10;");
        
        scoreLabel = new Label("Score: 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        timerLabel = new Label("Time: 00:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        movesLabel = new Label("Moves: 0");
        movesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        topPanel.getChildren().addAll(scoreLabel, timerLabel, movesLabel);
        
        // Center - Game grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));
        
        // Create game controller
        if (currentGameController != null) {
            currentGameController.cleanup();
        }
        currentGameController = new GameController(grid, currentDifficulty);
        
        // Set up callbacks
        currentGameController.setOnTimeUpdate(() -> {
            Platform.runLater(() -> timerLabel.setText("Time: " + currentGameController.getFormattedTime()));
        });
        
        currentGameController.setOnScoreUpdate(() -> {
            Platform.runLater(() -> scoreLabel.setText("Score: " + currentGameController.getScore()));
        });
        
        currentGameController.setOnMovesUpdate(() -> {
            Platform.runLater(() -> movesLabel.setText("Moves: " + currentGameController.getMoves()));
        });
        
        currentGameController.setOnGameWon(() -> {
            showEndGameScreen();
        });
        
        // Bottom panel with controls
        HBox bottomPanel = new HBox(15);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(15));
        
        Button pauseButton = createStyledButton("Pause", 120, 40);
        pauseButton.setOnAction(e -> {
            currentGameController.togglePause();
            pauseButton.setText(currentGameController.isPaused() ? "Resume" : "Pause");
        });
        
        Button resetButton = createStyledButton("Reset", 120, 40);
        resetButton.setOnAction(e -> {
            currentGameController.reset();
            pauseButton.setText("Pause");
            scoreLabel.setText("Score: 0");
            timerLabel.setText("Time: 00:00");
            movesLabel.setText("Moves: 0");
        });
        
        Button menuButton = createStyledButton("Menu", 120, 40);
        menuButton.setOnAction(e -> {
            if (currentGameController != null) {
                currentGameController.cleanup();
            }
            primaryStage.setScene(mainMenuScene);
        });
        
        bottomPanel.getChildren().addAll(pauseButton, resetButton, menuButton);
        
        root.setTop(topPanel);
        root.setCenter(grid);
        root.setBottom(bottomPanel);
        
        gameScene = new Scene(root, 800, 700);
        primaryStage.setScene(gameScene);
    }
    
    /**
     * Create and show the end game screen.
     */
    private void showEndGameScreen() {
        if (currentGameController == null) {
            return;
        }
        
        int finalScore = currentGameController.getScore();
        int finalTime = currentGameController.getTimeSeconds();
        int finalMoves = currentGameController.getMoves();
        String difficulty = currentGameController.getDifficulty().toString();
        
        // Save to database
        if (currentPlayerId > 0) {
            DatabaseHelper.saveGameSession(currentPlayerId, finalScore, finalTime, finalMoves, difficulty);
        }
        
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #11998e 0%, #38ef7d 100%);");
        
        // Win message
        Label winLabel = new Label("🎉 Congratulations! 🎉");
        winLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        winLabel.setTextFill(Color.WHITE);
        
        Label subLabel = new Label("You've matched all pairs!");
        subLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subLabel.setTextFill(Color.WHITE);
        
        // Score summary
        VBox summaryBox = new VBox(10);
        summaryBox.setAlignment(Pos.CENTER);
        summaryBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15; -fx-padding: 20;");
        
        Label scoreTitle = new Label("Final Score");
        scoreTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        
        Label scoreValue = new Label(String.valueOf(finalScore));
        scoreValue.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        scoreValue.setTextFill(Color.web("#11998e"));
        
        Label timeLabel = new Label("Time: " + currentGameController.getFormattedTime());
        timeLabel.setFont(Font.font("Arial", 16));
        
        Label movesLabel = new Label("Moves: " + finalMoves);
        movesLabel.setFont(Font.font("Arial", 16));
        
        Label difficultyLabel = new Label("Difficulty: " + difficulty);
        difficultyLabel.setFont(Font.font("Arial", 16));
        
        summaryBox.getChildren().addAll(scoreTitle, scoreValue, timeLabel, movesLabel, difficultyLabel);
        
        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button playAgainButton = createStyledButton("Play Again", 150, 45);
        playAgainButton.setOnAction(e -> {
            showGameScreen();
        });
        
        Button menuButton = createStyledButton("Main Menu", 150, 45);
        menuButton.setOnAction(e -> {
            if (currentGameController != null) {
                currentGameController.cleanup();
            }
            primaryStage.setScene(mainMenuScene);
        });
        
        Button leaderboardButton = createStyledButton("View Leaderboard", 150, 45);
        leaderboardButton.setOnAction(e -> {
            showLeaderboard();
        });
        
        buttonBox.getChildren().addAll(playAgainButton, menuButton, leaderboardButton);
        
        root.getChildren().addAll(winLabel, subLabel, summaryBox, buttonBox);
        
        endGameScene = new Scene(root, 600, 600);
        primaryStage.setScene(endGameScene);
    }
    
    /**
     * Create and show the leaderboard scene.
     */
    private void createLeaderboardScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
        
        Label title = new Label("🏆 Leaderboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        
        // Difficulty filter
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER);
        
        Button allButton = createStyledButton("All", 80, 35);
        Button easyButton = createStyledButton("Easy", 80, 35);
        Button mediumButton = createStyledButton("Medium", 80, 35);
        Button hardButton = createStyledButton("Hard", 80, 35);
        
        // Leaderboard display area
        VBox leaderboardBox = new VBox(5);
        leaderboardBox.setAlignment(Pos.CENTER);
        leaderboardBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15; -fx-padding: 20;");
        leaderboardBox.setPrefWidth(550);
        leaderboardBox.setPrefHeight(400);
        
        ScrollPane scrollPane = new ScrollPane(leaderboardBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(400);
        
        // Function to update leaderboard
        Runnable updateLeaderboard = () -> {
            String difficulty = null;
            if (easyButton.getStyle().contains("#4CAF50")) {
                difficulty = "EASY";
            } else if (mediumButton.getStyle().contains("#4CAF50")) {
                difficulty = "MEDIUM";
            } else if (hardButton.getStyle().contains("#4CAF50")) {
                difficulty = "HARD";
            }
            
            leaderboardBox.getChildren().clear();
            List<DatabaseHelper.LeaderboardEntry> entries = DatabaseHelper.getLeaderboard(difficulty, 10);
            
            if (entries.isEmpty()) {
                Label noData = new Label("No scores yet!");
                noData.setFont(Font.font("Arial", 16));
                leaderboardBox.getChildren().add(noData);
            } else {
                // Header
                HBox header = new HBox(10);
                header.setPadding(new Insets(5));
                header.setStyle("-fx-background-color: #667eea; -fx-background-radius: 5;");
                
                Label rankHeader = new Label("Rank");
                Label nameHeader = new Label("Player");
                Label scoreHeader = new Label("Score");
                Label timeHeader = new Label("Time");
                Label movesHeader = new Label("Moves");
                
                for (Label h : new Label[]{rankHeader, nameHeader, scoreHeader, timeHeader, movesHeader}) {
                    h.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    h.setTextFill(Color.WHITE);
                    h.setPrefWidth(80);
                }
                
                header.getChildren().addAll(rankHeader, nameHeader, scoreHeader, timeHeader, movesHeader);
                leaderboardBox.getChildren().add(header);
                
                // Entries
                for (DatabaseHelper.LeaderboardEntry entry : entries) {
                    HBox row = new HBox(10);
                    row.setPadding(new Insets(5));
                    row.setStyle("-fx-background-color: rgba(102, 126, 234, 0.1); -fx-background-radius: 5;");
                    
                    Label rank = new Label(String.valueOf(entry.getRank()));
                    Label name = new Label(entry.getUsername());
                    Label score = new Label(String.valueOf(entry.getScore()));
                    Label time = new Label(entry.getFormattedTime());
                    Label moves = new Label(String.valueOf(entry.getMoves()));
                    
                    for (Label l : new Label[]{rank, name, score, time, moves}) {
                        l.setFont(Font.font("Arial", 12));
                        l.setPrefWidth(80);
                    }
                    
                    row.getChildren().addAll(rank, name, score, time, moves);
                    leaderboardBox.getChildren().add(row);
                }
            }
        };
        
        // Button actions
        allButton.setOnAction(e -> {
            resetFilterButtons(allButton, easyButton, mediumButton, hardButton);
            allButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            updateLeaderboard.run();
        });
        
        easyButton.setOnAction(e -> {
            resetFilterButtons(allButton, easyButton, mediumButton, hardButton);
            easyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            updateLeaderboard.run();
        });
        
        mediumButton.setOnAction(e -> {
            resetFilterButtons(allButton, easyButton, mediumButton, hardButton);
            mediumButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            updateLeaderboard.run();
        });
        
        hardButton.setOnAction(e -> {
            resetFilterButtons(allButton, easyButton, mediumButton, hardButton);
            hardButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            updateLeaderboard.run();
        });
        
        filterBox.getChildren().addAll(allButton, easyButton, mediumButton, hardButton);
        
        // Back button
        Button backButton = createStyledButton("Back to Menu", 150, 40);
        backButton.setOnAction(e -> primaryStage.setScene(mainMenuScene));
        
        root.getChildren().addAll(title, filterBox, scrollPane, backButton);
        
        leaderboardScene = new Scene(root, 600, 600);
        
        // Initial load
        allButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        updateLeaderboard.run();
    }
    
    private void resetFilterButtons(Button... buttons) {
        for (Button b : buttons) {
            b.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }
    
    private void showLeaderboard() {
        primaryStage.setScene(leaderboardScene);
    }
    
    /**
     * Show settings dialog.
     */
    private void showSettings() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Change Player Name");
        
        TextField nameField = new TextField(currentPlayerName);
        nameField.setPromptText("Enter your name");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(new Label("Player Name:"), nameField);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return nameField.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(name -> {
            if (name != null && !name.trim().isEmpty()) {
                currentPlayerName = name.trim();
                currentPlayerId = DatabaseHelper.createPlayer(currentPlayerName);
                welcomeLabel.setText("Welcome, " + currentPlayerName + "!");
            }
        });
    }
    
    /**
     * Create a styled button with consistent appearance.
     */
    private Button createStyledButton(String text, double width, double height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setStyle(
            "-fx-background-color: #667eea; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10; " +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: #5568d3; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-cursor: hand;"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: #667eea; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-cursor: hand;"
            );
        });
        
        return button;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

