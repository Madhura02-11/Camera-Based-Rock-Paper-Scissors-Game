import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GameDatabase {

    // SQLite database file
    private static final String DATABASE_URL = "jdbc:sqlite:game_records.db";

    // Constructor
    public GameDatabase() {
        loadSQLiteDriver();
        createTable();
    }

    // Load SQLite JDBC driver
    private void loadSQLiteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
            System.out.println("Make sure sqlite-jdbc JAR is added to the classpath.");
        }
    }

    // Create database table
    private void createTable() {

        String sql = "CREATE TABLE IF NOT EXISTS game_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "round INTEGER, " +
                "player_choice TEXT, " +
                "computer_choice TEXT, " +
                "result TEXT, " +
                "score INTEGER" +
                ")";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.executeUpdate();

            System.out.println("Game database table created successfully.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Save one completed game record
    public void saveGameRecord(
            int round,
            String playerChoice,
            String computerChoice,
            String result,
            int score) {

        String sql = "INSERT INTO game_records " +
                "(round, player_choice, computer_choice, result, score) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, round);
            statement.setString(2, playerChoice);
            statement.setString(3, computerChoice);
            statement.setString(4, result);
            statement.setInt(5, score);

            statement.executeUpdate();

            System.out.println("Game record saved successfully.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Get all previous game records
    public ResultSet getAllGameRecords() throws SQLException {

        String sql = "SELECT id, round, player_choice, computer_choice, " +
                "result, score FROM game_records ORDER BY id ASC";

        Connection connection = DriverManager.getConnection(DATABASE_URL);

        PreparedStatement statement =
                connection.prepareStatement(sql);

        return statement.executeQuery();
    }
}