import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.painter.Painter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class MapPanel extends JPanel {

    private JXMapViewer map;
    private GeoPosition fireLocation = null;
    private GeoPosition nearestStation = null;

    private final List<GeoPosition> stations = new ArrayList<>();

    public MapPanel() {
        setLayout(new BorderLayout());

        map = new JXMapViewer();

        // ✅ CORRECT TILE FACTORY (NO OSMTileFactoryInfo)
        TileFactoryInfo info = new TileFactoryInfo(
                1, 17, 18,
                256, true, true,
                "https://tile.openstreetmap.org",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int z = 18 - zoom;
                return this.baseURL + "/" + z + "/" + x + "/" + y + ".png";
            }
        };

        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        map.setTileFactory(tileFactory);

        // Islamabad center
        GeoPosition islamabad = new GeoPosition(33.6844, 73.0479);
        map.setAddressLocation(islamabad);
        map.setZoom(6);

        // 🚒 FIRE STATIONS
        stations.add(new GeoPosition(33.6849, 73.0479)); // Blue Area
        stations.add(new GeoPosition(33.7008, 73.0408)); // G-10
        stations.add(new GeoPosition(33.6692, 73.0596)); // I-8
        stations.add(new GeoPosition(33.6938, 73.0652)); // F-10
        stations.add(new GeoPosition(33.6412, 73.0705)); // IJP

        // 🖱 CLICK = FIRE
        map.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fireLocation = map.convertPointToGeoPosition(e.getPoint());
                nearestStation = findNearestStation(fireLocation);
                map.repaint();
            }
        });

        // 🎨 DRAW EVERYTHING
        map.setOverlayPainter((Painter<JXMapViewer>) (g, map, w, h) -> {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle viewport = map.getViewportBounds();

            // Stations
            for (GeoPosition s : stations) {
                Point2D pt = map.getTileFactory().geoToPixel(s, map.getZoom());
                int x = (int) (pt.getX() - viewport.getX());
                int y = (int) (pt.getY() - viewport.getY());

                if (s.equals(nearestStation))
                    g2.setColor(Color.GREEN);
                else
                    g2.setColor(Color.BLUE);

                g2.fillOval(x - 6, y - 6, 12, 12);
            }

            // Fire
            if (fireLocation != null) {
                Point2D pt = map.getTileFactory().geoToPixel(fireLocation, map.getZoom());
                int x = (int) (pt.getX() - viewport.getX());
                int y = (int) (pt.getY() - viewport.getY());

                g2.setColor(Color.RED);
                g2.fillOval(x - 8, y - 8, 16, 16);
            }

            // Route
            if (fireLocation != null && nearestStation != null) {
                Point2D f = map.getTileFactory().geoToPixel(fireLocation, map.getZoom());
                Point2D s = map.getTileFactory().geoToPixel(nearestStation, map.getZoom());

                int fx = (int) (f.getX() - viewport.getX());
                int fy = (int) (f.getY() - viewport.getY());
                int sx = (int) (s.getX() - viewport.getX());
                int sy = (int) (s.getY() - viewport.getY());

                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(sx, sy, fx, fy);
            }
        });

        add(map, BorderLayout.CENTER);
    }

    // 🔍 NEAREST STATION
    private GeoPosition findNearestStation(GeoPosition fire) {
        GeoPosition nearest = null;
        double min = Double.MAX_VALUE;

        for (GeoPosition s : stations) {
            double d = distance(
                    fire.getLatitude(), fire.getLongitude(),
                    s.getLatitude(), s.getLongitude());

            if (d < min) {
                min = d;
                nearest = s;
            }
        }
        return nearest;
    }

    // 📏 DISTANCE (KM)
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
