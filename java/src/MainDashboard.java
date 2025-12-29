import javax.swing.*;

public class MainDashboard extends JFrame {

    public MainDashboard() {
        setTitle("Fire Brigade Dispatch System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new MapPanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        new MainDashboard();
    }
}
