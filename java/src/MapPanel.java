import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.*;

public class MapPanel extends JPanel {

    private ArrayList<FireStation> stations = new ArrayList<>();
    private Point firePoint = null;
    private FireStation nearestStation = null;

    public MapPanel() {
        setBackground(new Color(230, 230, 230));

        // Islamabad fire stations (approx positions)
        stations.add(new FireStation("G-6", 200, 150));
        stations.add(new FireStation("I-8", 350, 320));
        stations.add(new FireStation("F-10", 500, 180));
        stations.add(new FireStation("Blue Area", 420, 260));
        stations.add(new FireStation("H-11", 650, 120));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                firePoint = e.getPoint();
                findNearestStation();
                repaint();
            }
        });
    }

    private void findNearestStation() {
        nearestStation = null;
        double min = Double.MAX_VALUE;

        for (FireStation s : stations) {
            s.setColor(Color.BLUE); // reset
            double d = s.distanceTo(firePoint.x, firePoint.y);
            if (d < min) {
                min = d;
                nearestStation = s;
            }
        }

        if (nearestStation != null) {
            nearestStation.setColor(Color.GREEN);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // draw stations
        for (FireStation s : stations) {
            g2.setColor(s.getColor());
            g2.fillOval(s.getX() - 8, s.getY() - 8, 16, 16);
            g2.setColor(Color.BLACK);
            g2.drawString(s.getName(), s.getX() - 15, s.getY() - 12);
        }

        // draw fire
        if (firePoint != null) {
            g2.setColor(Color.RED);
            g2.fillOval(firePoint.x - 10, firePoint.y - 10, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("FIRE", firePoint.x - 12, firePoint.y - 15);
        }
    }
}
