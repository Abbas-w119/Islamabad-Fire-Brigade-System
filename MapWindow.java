import javax.swing.*;

public class MapWindow extends JFrame {

    public MapWindow() {
        setTitle("Islamabad Fire Brigade Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(new MapPanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        new MapWindow();
    }
}
