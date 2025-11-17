import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database helper class for managing MySQL database connections and operations
 * for the Memory Match Game.
 */
public class DatabaseHelper {
    private static final String DB_NAME = "memorygame";
    private static final String BASE_URL = "jdbc:mysql://localhost:3306/";
    private static final String URL = BASE_URL + DB_NAME;
    private static final String USER = "root"; // your MySQL username
    private static final String PASSWORD = "arnav2006"; // your MySQL password
    
    private static boolean initialized = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    /**
     * Initialize the database and create tables if they don't exist.
     * This method is called automatically on first connection.
     */
    public static void initializeDatabase() {
        if (initialized) {
            return;
        }

        try {
            // First, connect without database to create it if needed
            try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                // Create database if it doesn't exist
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                System.out.println("Database '" + DB_NAME + "' ready.");
            }

            // Now connect to the database and create tables
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                // Create players table
                String createPlayersTable = """
                    CREATE TABLE IF NOT EXISTS players (
                        player_id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) UNIQUE NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;
                stmt.executeUpdate(createPlayersTable);
                System.out.println("Table 'players' ready.");

                // Create game_sessions table
                String createSessionsTable = """
                    CREATE TABLE IF NOT EXISTS game_sessions (
                        session_id INT PRIMARY KEY AUTO_INCREMENT,
                        player_id INT,
                        score INT NOT NULL,
                        time_seconds INT NOT NULL,
                        moves INT NOT NULL,
                        difficulty VARCHAR(20),
                        played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
                    )
                    """;
                stmt.executeUpdate(createSessionsTable);
                System.out.println("Table 'game_sessions' ready.");
                
                initialized = true;
                System.out.println("Database initialization complete!");
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get a database connection.
     * Initializes the database if not already done.
     */
    private static Connection getConnection() throws SQLException {
        if (!initialized) {
            initializeDatabase();
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Create a new player or get existing player by username.
     * @param username The player's username
     * @return The player_id, or -1 if error
     */
    public static int createPlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Username cannot be empty");
            return -1;
        }

        try (Connection conn = getConnection()) {
            // First, try to get existing player
            String selectQuery = "SELECT player_id FROM players WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                stmt.setString(1, username.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int playerId = rs.getInt("player_id");
                        System.out.println("Player '" + username + "' already exists with ID: " + playerId);
                        return playerId;
                    }
                }
            }

            // If not found, create new player
            String insertQuery = "INSERT INTO players (username) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, username.trim());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int playerId = rs.getInt(1);
                            System.out.println("Created new player '" + username + "' with ID: " + playerId);
                            return playerId;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating/getting player: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Save a game session to the database.
     * @param playerId The player's ID
     * @param score The final score
     * @param timeSeconds The time taken in seconds
     * @param moves The number of moves
     * @param difficulty The difficulty level (Easy, Medium, Hard)
     * @return true if successful, false otherwise
     */
    public static boolean saveGameSession(int playerId, int score, int timeSeconds, int moves, String difficulty) {
        if (playerId <= 0) {
            System.err.println("Invalid player ID");
            return false;
        }

        String query = "INSERT INTO game_sessions (player_id, score, time_seconds, moves, difficulty) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, score);
            stmt.setInt(3, timeSeconds);
            stmt.setInt(4, moves);
            stmt.setString(5, difficulty);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Game session saved successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving game session: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get the leaderboard for a specific difficulty level.
     * @param difficulty The difficulty level (null for all difficulties)
     * @param limit Maximum number of results to return
     * @return List of leaderboard entries
     */
    public static List<LeaderboardEntry> getLeaderboard(String difficulty, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        String query;
        if (difficulty == null || difficulty.isEmpty()) {
            query = """
                SELECT p.username, gs.score, gs.time_seconds, gs.moves, gs.difficulty, gs.played_at
                FROM game_sessions gs
                JOIN players p ON gs.player_id = p.player_id
                ORDER BY gs.score DESC, gs.time_seconds ASC, gs.moves ASC
                LIMIT ?
                """;
        } else {
            query = """
                SELECT p.username, gs.score, gs.time_seconds, gs.moves, gs.difficulty, gs.played_at
                FROM game_sessions gs
                JOIN players p ON gs.player_id = p.player_id
                WHERE gs.difficulty = ?
                ORDER BY gs.score DESC, gs.time_seconds ASC, gs.moves ASC
                LIMIT ?
                """;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (difficulty == null || difficulty.isEmpty()) {
                stmt.setInt(1, limit);
            } else {
                stmt.setString(1, difficulty);
                stmt.setInt(2, limit);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    LeaderboardEntry entry = new LeaderboardEntry(
                        rank++,
                        rs.getString("username"),
                        rs.getInt("score"),
                        rs.getInt("time_seconds"),
                        rs.getInt("moves"),
                        rs.getString("difficulty"),
                        rs.getTimestamp("played_at")
                    );
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
        
        return entries;
    }

    /**
     * Get the best score for a player at a specific difficulty.
     * @param playerId The player's ID
     * @param difficulty The difficulty level (null for all difficulties)
     * @return The best score, or -1 if no records found
     */
    public static int getPlayerBestScore(int playerId, String difficulty) {
        if (playerId <= 0) {
            return -1;
        }

        String query;
        if (difficulty == null || difficulty.isEmpty()) {
            query = """
                SELECT MAX(score) as best_score
                FROM game_sessions
                WHERE player_id = ?
                """;
        } else {
            query = """
                SELECT MAX(score) as best_score
                FROM game_sessions
                WHERE player_id = ? AND difficulty = ?
                """;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, playerId);
            if (difficulty != null && !difficulty.isEmpty()) {
                stmt.setString(2, difficulty);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int bestScore = rs.getInt("best_score");
                    return rs.wasNull() ? -1 : bestScore;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving player best score: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }

    /**
     * Inner class to represent a leaderboard entry.
     */
    public static class LeaderboardEntry {
        private final int rank;
        private final String username;
        private final int score;
        private final int timeSeconds;
        private final int moves;
        private final String difficulty;
        private final Timestamp playedAt;

        public LeaderboardEntry(int rank, String username, int score, int timeSeconds, 
                               int moves, String difficulty, Timestamp playedAt) {
            this.rank = rank;
            this.username = username;
            this.score = score;
            this.timeSeconds = timeSeconds;
            this.moves = moves;
            this.difficulty = difficulty;
            this.playedAt = playedAt;
        }

        public int getRank() { return rank; }
        public String getUsername() { return username; }
        public int getScore() { return score; }
        public int getTimeSeconds() { return timeSeconds; }
        public int getMoves() { return moves; }
        public String getDifficulty() { return difficulty; }
        public Timestamp getPlayedAt() { return playedAt; }
        
        public String getFormattedTime() {
            int minutes = timeSeconds / 60;
            int seconds = timeSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
