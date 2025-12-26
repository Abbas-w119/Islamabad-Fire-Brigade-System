import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import java.awt.*;

public class MapWindow extends JFrame {

    public MapWindow() {
        setTitle("🔥 Fire Brigade Control System");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        StationManager manager = new StationManager();

        // Default stations
        manager.addStation(new FireStation("Blue Area", new GeoPosition(33.6849, 73.0479)));
        manager.addStation(new FireStation("G-10", new GeoPosition(33.7008, 73.0408)));
        manager.addStation(new FireStation("I-8", new GeoPosition(33.6692, 73.0596)));

        MapPanel mapPanel = new MapPanel(manager);

        DefaultListModel<FireStation> model = new DefaultListModel<>();
        manager.getStations().forEach(model::addElement);

        JList<FireStation> list = new JList<>(model);

        JButton add = new JButton("➕ Add Station");
        JButton remove = new JButton("❌ Remove Station");

        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Station name:");
            if (name == null) return;

            String lat = JOptionPane.showInputDialog("Latitude:");
            String lon = JOptionPane.showInputDialog("Longitude:");

            FireStation s = new FireStation(
                    name,
                    new GeoPosition(Double.parseDouble(lat), Double.parseDouble(lon)));

            manager.addStation(s);
            model.addElement(s);
            mapPanel.repaint();
        });

        remove.addActionListener(e -> {
            FireStation s = list.getSelectedValue();
            if (s != null) {
                manager.removeStation(s);
                model.removeElement(s);
                mapPanel.repaint();
            }
        });

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 1));
        buttons.add(add);
        buttons.add(remove);

        left.add(buttons, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(300, 0));

        add(left, BorderLayout.WEST);
        add(mapPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapWindow::new);
    }
}
