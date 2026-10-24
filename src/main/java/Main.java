import dao.DatabaseConnection;
import ui.MainMenu;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();
        new MainMenu().show();
    }
}
