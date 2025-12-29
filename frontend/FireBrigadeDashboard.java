import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;

public class FireBrigadeDashboard extends JFrame {
    private TabbedMainPanel tabbedPanel;

    public FireBrigadeDashboard() {
        setTitle("🚒 Islamabad Fire Brigade System - Real-time Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        tabbedPanel = new TabbedMainPanel();
        add(tabbedPanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FireBrigadeDashboard());
    }
}

class TabbedMainPanel extends JPanel {
    private JTabbedPane tabbedPane;
    private AdvancedMapPanel mapPanel;
    private StatsPanel statsPanel;
    private IncidentsPanel incidentsPanel;
    private StationsPanel stationsPanel;
    private AnalyticsPanel analyticsPanel;

    public TabbedMainPanel() {
        setLayout(new BorderLayout());
        
        mapPanel = new AdvancedMapPanel();
        statsPanel = new StatsPanel(mapPanel);
        incidentsPanel = new IncidentsPanel(mapPanel);
        stationsPanel = new StationsPanel(mapPanel);
        analyticsPanel = new AnalyticsPanel(mapPanel);
        
        mapPanel.setCallback(statsPanel, incidentsPanel);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(245, 245, 245));
        tabbedPane.setForeground(new Color(40, 40, 40));
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, 
                                             int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isSelected) {
                    g2.setColor(new Color(0, 100, 200));
                } else {
                    g2.setColor(new Color(200, 200, 200));
                }
                g2.fillRect(x, y, w, h);
            }
        });

        tabbedPane.addTab("🗺️  Real-time Map", mapPanel);
        tabbedPane.addTab("📊 Dashboard", statsPanel);
        tabbedPane.addTab("🚨 Incidents", incidentsPanel);
        tabbedPane.addTab("🏢 Stations", stationsPanel);
        tabbedPane.addTab("📈 Analytics", analyticsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }
}

class AdvancedMapPanel extends JPanel {
    private ArrayList<FireStation> stations = new ArrayList<>();
    private ArrayList<Incident> incidents = new ArrayList<>();
    private ArrayList<RoadNetwork> roadNetwork = new ArrayList<>();
    private GraphDS graph;
    private StatsPanel statsCallback;
    private IncidentsPanel incidentsCallback;
    private double zoomLevel = 13.0;
    private double centerLat = 33.6844;
    private double centerLon = 73.0479;
    private int animationFrame = 0;
    private ArrayList<Integer> currentRoute = new ArrayList<>();
    private ArrayList<double[]> routeCoordinates = new ArrayList<>();
    private int respondingStationId = -1;
    private Point lastMousePos;

    public AdvancedMapPanel() {
        setBackground(new Color(10, 15, 25));
        setBorder(new LineBorder(new Color(30, 50, 80), 3));

        initializeStations();
        initializeRoads();
        graph = new GraphDS(stations.size());
        buildGraph();
        startAnimation();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    double[] coords = screenToLatLon(e.getX(), e.getY());
                    handleMapClick(coords[0], coords[1]);
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePos != null && SwingUtilities.isMiddleMouseButton(e)) {
                    double dx = (e.getX() - lastMousePos.x) / (256.0 * Math.pow(2, zoomLevel) / getWidth());
                    double dy = (e.getY() - lastMousePos.y) / (256.0 * Math.pow(2, zoomLevel) / getHeight());
                    centerLon -= dx;
                    centerLat += dy;
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            zoomLevel -= (e.getWheelRotation() * 0.5);
            zoomLevel = Math.max(11, Math.min(18, zoomLevel));
            repaint();
        });
    }

    private void initializeStations() {
        // Central Core
        stations.add(new FireStation(0, "Main Station", 33.6844, 73.0479));
        stations.add(new FireStation(1, "Blue Area", 33.7182, 73.0605));
        stations.add(new FireStation(2, "G-6 Sector", 33.7100, 73.0800));
        stations.add(new FireStation(3, "Margalla Road", 33.7400, 73.0900));
        stations.add(new FireStation(4, "Airport Road", 33.6167, 73.0992));
        
        // Northern Zone
        stations.add(new FireStation(5, "Saidpur", 33.7650, 73.0850));
        stations.add(new FireStation(6, "Pir Sohawa", 33.7750, 73.1050));
        stations.add(new FireStation(7, "Fatima Jinnah Park", 33.7900, 73.0950));
        stations.add(new FireStation(8, "Aabpara", 33.8050, 73.0850));
        stations.add(new FireStation(9, "Kashmir Highway", 33.8150, 73.1150));
        
        // Eastern Zone
        stations.add(new FireStation(10, "Shifa Hospital", 33.6300, 73.1150));
        stations.add(new FireStation(11, "Research Center", 33.6450, 73.1350));
        stations.add(new FireStation(12, "Chak Shahzad", 33.6600, 73.1550));
        stations.add(new FireStation(13, "Koral", 33.6750, 73.1700));
        stations.add(new FireStation(14, "Behlwal", 33.6550, 73.1950));
        
        // Western Zone
        stations.add(new FireStation(15, "Rawalpindi", 33.5895, 74.3055));
        stations.add(new FireStation(16, "Pirwadhai", 33.5750, 74.2850));
        stations.add(new FireStation(17, "Potters", 33.6100, 74.2650));
        stations.add(new FireStation(18, "Sector F", 33.5600, 74.3150));
        stations.add(new FireStation(19, "Westridge", 33.5800, 74.3350));
    }

    private void initializeRoads() {
        // Central Core Roads
        roadNetwork.add(new RoadNetwork(0, 1, "Constitution Avenue", 33.6844, 73.0479, 33.7182, 73.0605, 5, 18));
        roadNetwork.add(new RoadNetwork(1, 2, "Jinnah Avenue", 33.7182, 73.0605, 33.7100, 73.0800, 4, 14));
        roadNetwork.add(new RoadNetwork(2, 3, "G-6 Link Road", 33.7100, 73.0800, 33.7400, 73.0900, 6, 12));
        roadNetwork.add(new RoadNetwork(3, 4, "Margalla Road", 33.7400, 73.0900, 33.6167, 73.0992, 7, 16));
        roadNetwork.add(new RoadNetwork(0, 2, "Main Expressway", 33.6844, 73.0479, 33.7100, 73.0800, 8, 22));
        roadNetwork.add(new RoadNetwork(1, 3, "Blue Area Link", 33.7182, 73.0605, 33.7400, 73.0900, 9, 14));
        roadNetwork.add(new RoadNetwork(0, 4, "Airport Highway", 33.6844, 73.0479, 33.6167, 73.0992, 14, 26));
        
        // Northern Zone Roads
        roadNetwork.add(new RoadNetwork(2, 5, "Saidpur Road", 33.7100, 73.0800, 33.7650, 73.0850, 8, 18));
        roadNetwork.add(new RoadNetwork(3, 6, "Pir Sohawa Road", 33.7400, 73.0900, 33.7750, 73.1050, 7, 16));
        roadNetwork.add(new RoadNetwork(5, 6, "Saidpur-Pir Link", 33.7650, 73.0850, 33.7750, 73.1050, 6, 12));
        roadNetwork.add(new RoadNetwork(5, 7, "Park Access Road", 33.7650, 73.0850, 33.7900, 73.0950, 5, 10));
        roadNetwork.add(new RoadNetwork(6, 8, "Aabpara Road", 33.7750, 73.1050, 33.8050, 73.0850, 8, 16));
        roadNetwork.add(new RoadNetwork(7, 8, "Fatima-Aab Link", 33.7900, 73.0950, 33.8050, 73.0850, 7, 14));
        roadNetwork.add(new RoadNetwork(8, 9, "Kashmir Highway", 33.8050, 73.0850, 33.8150, 73.1150, 4, 12));
        
        // Eastern Zone Roads
        roadNetwork.add(new RoadNetwork(4, 10, "Airport-Shifa", 33.6167, 73.0992, 33.6300, 73.1150, 9, 18));
        roadNetwork.add(new RoadNetwork(10, 11, "Shifa-Research", 33.6300, 73.1150, 33.6450, 73.1350, 6, 14));
        roadNetwork.add(new RoadNetwork(11, 12, "Research-Chak", 33.6450, 73.1350, 33.6600, 73.1550, 5, 12));
        roadNetwork.add(new RoadNetwork(12, 13, "Chak-Koral", 33.6600, 73.1550, 33.6750, 73.1700, 7, 16));
        roadNetwork.add(new RoadNetwork(13, 14, "Koral-Behlwal", 33.6750, 73.1700, 33.6550, 73.1950, 6, 14));
        
        // Western Zone Roads
        roadNetwork.add(new RoadNetwork(0, 15, "Rawalpindi Road", 33.6844, 73.0479, 33.5895, 74.3055, 10, 20));
        roadNetwork.add(new RoadNetwork(15, 16, "Rawalpindi-Pirwadhai", 33.5895, 74.3055, 33.5750, 74.2850, 8, 16));
        roadNetwork.add(new RoadNetwork(16, 17, "Pirwadhai-Potters", 33.5750, 74.2850, 33.6100, 74.2650, 7, 14));
        roadNetwork.add(new RoadNetwork(17, 1, "Potters-Blue Area", 33.6100, 74.2650, 33.7182, 73.0605, 9, 18));
        roadNetwork.add(new RoadNetwork(15, 18, "Sector F Road", 33.5895, 74.3055, 33.5600, 74.3150, 6, 12));
        roadNetwork.add(new RoadNetwork(18, 19, "Sector-Westridge", 33.5600, 74.3150, 33.5800, 74.3350, 5, 10));
        
        // Cross-city Connections
        roadNetwork.add(new RoadNetwork(9, 14, "Kashmir-Behlwal", 33.8150, 73.1150, 33.6550, 73.1950, 12, 24));
        roadNetwork.add(new RoadNetwork(8, 11, "Aabpara-Research", 33.8050, 73.0850, 33.6450, 73.1350, 10, 20));
        roadNetwork.add(new RoadNetwork(7, 10, "Park-Shifa", 33.7900, 73.0950, 33.6300, 73.1150, 11, 22));
        roadNetwork.add(new RoadNetwork(4, 13, "Airport-Koral", 33.6167, 73.0992, 33.6750, 73.1700, 15, 28));
        roadNetwork.add(new RoadNetwork(1, 19, "Blue-Westridge", 33.7182, 73.0605, 33.5800, 74.3350, 13, 26));
        roadNetwork.add(new RoadNetwork(19, 14, "Westridge-Behlwal", 33.5800, 74.3350, 33.6550, 73.1950, 14, 28));
        roadNetwork.add(new RoadNetwork(16, 10, "Pirwadhai-Shifa", 33.5750, 74.2850, 33.6300, 73.1150, 11, 22));
        roadNetwork.add(new RoadNetwork(12, 9, "Chak-Kashmir", 33.6600, 73.1550, 33.8150, 73.1150, 10, 20));
    }

    private void buildGraph() {
        for (RoadNetwork road : roadNetwork) {
            graph.addEdge(road.from, road.to, road.distance);
            graph.addEdge(road.to, road.from, road.distance);
        }
    }

    private double[] screenToLatLon(int screenX, int screenY) {
        double centerX = lngToPixel(centerLon, zoomLevel);
        double centerY = latToPixel(centerLat, zoomLevel);
        double pixelX = centerX + (screenX - getWidth() / 2.0);
        double pixelY = centerY + (screenY - getHeight() / 2.0);
        double lon = pixelToLng(pixelX, zoomLevel);
        double lat = pixelToLat(pixelY, zoomLevel);
        return new double[]{lat, lon};
    }

    private double lngToPixel(double lon, double zoom) {
        return (lon + 180.0) / 360.0 * Math.pow(2, zoom) * 256;
    }

    private double latToPixel(double lat, double zoom) {
        double sin = Math.sin(Math.toRadians(lat));
        double y2 = Math.log((1 + sin) / (1 - sin)) / 2;
        return (1 - y2 / Math.PI) / 2.0 * Math.pow(2, zoom) * 256;
    }

    private double pixelToLng(double pixel, double zoom) {
        return pixel / (Math.pow(2, zoom) * 256) * 360.0 - 180.0;
    }

    private double pixelToLat(double pixel, double zoom) {
        double n = Math.PI - 2.0 * Math.PI * pixel / (Math.pow(2, zoom) * 256);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private int[] latLonToScreen(double lat, double lon) {
        double centerX = lngToPixel(centerLon, zoomLevel);
        double centerY = latToPixel(centerLat, zoomLevel);
        double pixelX = lngToPixel(lon, zoomLevel);
        double pixelY = latToPixel(lat, zoomLevel);
        int screenX = (int)(getWidth() / 2.0 + (pixelX - centerX));
        int screenY = (int)(getHeight() / 2.0 + (pixelY - centerY));
        return new int[]{screenX, screenY};
    }

    private void handleMapClick(double lat, double lon) {
        int response = JOptionPane.showConfirmDialog(this,
            String.format("Report fire incident here?\nLat: %.4f | Lon: %.4f", lat, lon),
            "🔥 Fire Report", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            String[] severities = {"🟡 Low", "🟠 Medium", "🔴 Critical"};
            String severity = (String) JOptionPane.showInputDialog(this,
                "Select Incident Severity:", "Severity Level",
                JOptionPane.QUESTION_MESSAGE, null, severities, severities[2]);

            if (severity != null) {
                int sevLevel = severity.contains("Low") ? 1 : (severity.contains("Medium") ? 2 : 3);
                int nearestStation = findNearestStation(lat, lon);

                Incident incident = new Incident(nearestStation, sevLevel, lat, lon);
                incidents.add(incident);

                respondingStationId = nearestStation;
                currentRoute = graph.dijkstra(nearestStation);
                buildRouteCoordinates();

                if (statsCallback != null) statsCallback.updateData(incidents, stations);
                if (incidentsCallback != null) incidentsCallback.updateIncidents(incidents);

                repaint();
            }
        }
    }

    private void buildRouteCoordinates() {
        routeCoordinates.clear();
        for (int stationId : currentRoute) {
            FireStation station = stations.get(stationId);
            routeCoordinates.add(new double[]{station.latitude, station.longitude});
        }
    }

    private int findNearestStation(double lat, double lon) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (FireStation s : stations) {
            double dist = Math.hypot(s.latitude - lat, s.longitude - lon) * 111;
            if (dist < minDist) {
                minDist = dist;
                nearest = s.id;
            }
        }
        return nearest;
    }

    private void startAnimation() {
        Timer timer = new Timer(80, e -> {
            animationFrame++;
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawMapBackground(g2);
        drawRoads(g2);
        drawIncidentHeatmap(g2);

        if (!routeCoordinates.isEmpty()) {
            drawRoute(g2);
        }

        for (FireStation station : stations) {
            drawStation(g2, station);
        }

        for (Incident incident : incidents) {
            drawIncident(g2, incident);
        }

        drawHUD(g2);
    }

    private void drawMapBackground(Graphics2D g2) {
        g2.setColor(new Color(8, 12, 28));
        g2.fillRect(0, 0, getWidth(), getHeight());

        GradientPaint gp = new GradientPaint(0, 0, new Color(12, 18, 35),
                                             0, getHeight(), new Color(5, 8, 20));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawIncidentHeatmap(Graphics2D g2) {
        for (Incident incident : incidents) {
            int[] screen = latLonToScreen(incident.lat, incident.lon);
            int x = screen[0], y = screen[1];
            if (!isPointVisible(screen)) continue;

            Color base = incident.severity == 3 ? new Color(255, 60, 0) :
                         incident.severity == 2 ? new Color(255, 120, 0) : new Color(255, 180, 50);

            for (int r = 180; r > 0; r -= 30) {
                int alpha = (int)(40 * (r / 180.0));
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                g2.fillOval(x - r, y - r, r*2, r*2);
            }
        }
    }

    private void drawRoads(Graphics2D g2) {
        // Draw road shadows first for depth
        for (RoadNetwork road : roadNetwork) {
            int[] p1 = latLonToScreen(road.lat1, road.lon1);
            int[] p2 = latLonToScreen(road.lat2, road.lon2);
            if (!isPointVisible(p1) && !isPointVisible(p2)) continue;

            // Shadow effect
            g2.setColor(new Color(0, 0, 0, 60));
            g2.setStroke(new BasicStroke(road.width + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1[0] + 3, p1[1] + 3, p2[0] + 3, p2[1] + 3);
        }

        // Draw actual roads with glow
        for (RoadNetwork road : roadNetwork) {
            int[] p1 = latLonToScreen(road.lat1, road.lon1);
            int[] p2 = latLonToScreen(road.lat2, road.lon2);
            if (!isPointVisible(p1) && !isPointVisible(p2)) continue;

            // Outer glow effect
            g2.setColor(new Color(100, 180, 255, 40));
            g2.setStroke(new BasicStroke(road.width + 20, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Mid glow
            g2.setColor(new Color(80, 160, 255, 80));
            g2.setStroke(new BasicStroke(road.width + 12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Main road asphalt
            g2.setColor(new Color(45, 55, 75));
            g2.setStroke(new BasicStroke(road.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Road highlights
            g2.setColor(new Color(70, 90, 110));
            g2.setStroke(new BasicStroke(road.width - 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Center line markings
            g2.setColor(new Color(255, 255, 150, 150));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{15, 10}, 0));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Road name label
            int midX = (p1[0] + p2[0]) / 2;
            int midY = (p1[1] + p2[1]) / 2;
            g2.setColor(new Color(255, 255, 200, 180));
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(road.name);
            
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(midX - textWidth/2 - 5, midY - 12, textWidth + 10, 18, 4, 4);
            
            g2.setColor(new Color(255, 255, 200));
            g2.drawString(road.name, midX - textWidth/2, midY + 3);
        }
    }

    private void drawRoute(Graphics2D g2) {
        if (routeCoordinates.size() < 2) return;

        // Route glow effect
        g2.setColor(new Color(255, 100, 0, 80));
        g2.setStroke(new BasicStroke(28, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }

        // Mid route glow
        g2.setColor(new Color(255, 120, 0, 120));
        g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }

        // Main route line - bright orange
        g2.setColor(new Color(255, 100, 0, 220));
        g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }

        // Route direction arrows
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            
            int midX = (p1[0] + p2[0]) / 2;
            int midY = (p1[1] + p2[1]) / 2;
            
            // Arrow background
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillOval(midX - 18, midY - 18, 36, 36);
            
            // Arrow
            g2.setColor(new Color(255, 150, 0));
            g2.drawString("→", midX - 8, midY + 8);
        }

        // Animated emergency vehicle
        float progress = (animationFrame % 120) / 120.0f;
        
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            double[] c1 = routeCoordinates.get(i);
            double[] c2 = routeCoordinates.get(i + 1);
            int[] p1 = latLonToScreen(c1[0], c1[1]);
            int[] p2 = latLonToScreen(c2[0], c2[1]);

            int px = (int)(p1[0] + (p2[0] - p1[0]) * progress);
            int py = (int)(p1[1] + (p2[1] - p1[1]) * progress);

            // Vehicle glow
            g2.setColor(new Color(255, 150, 0, 100));
            g2.fillOval(px - 20, py - 20, 40, 40);

            // Vehicle body
            g2.setColor(new Color(255, 100, 0));
            g2.fillRect(px - 14, py - 10, 28, 20);

            // Vehicle lights
            g2.setColor(new Color(255, 0, 0));
            g2.fillOval(px - 12, py - 8, 8, 8);
            g2.fillOval(px + 4, py - 8, 8, 8);

            // Vehicle siren effect
            int sirenSize = (int)(20 + Math.sin(animationFrame * 0.2) * 8);
            g2.setColor(new Color(255, 0, 0, 80 - (sirenSize - 20) * 5));
            g2.drawOval(px - sirenSize/2, py - sirenSize/2, sirenSize, sirenSize);

            // Vehicle label
            g2.setColor(new Color(255, 255, 100));
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("🚒", px - 10, py + 18);
        }

        // Route distance and time info
        g2.setColor(new Color(255, 150, 0, 200));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        
        int totalDistance = routeCoordinates.stream().mapToInt(c -> 0).sum();
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            
            int distX = p1[0] + (p2[0] - p1[0]) / 4;
            int distY = p1[1] + (p2[1] - p1[1]) / 4;
            
            g2.setColor(new Color(255, 200, 0, 220));
            g2.drawString("⚡", distX, distY);
        }
    }

    private void drawStation(Graphics2D g2, FireStation station) {
        int[] screen = latLonToScreen(station.latitude, station.longitude);
        int x = screen[0], y = screen[1];
        if (!isPointVisible(screen)) return;

        // Responding station - intense pulsing glow
        if (station.id == respondingStationId) {
            float pulse = (float)(0.3 + 0.7 * Math.sin(animationFrame * 0.12));
            
            // Multiple glow rings
            for (int ring = 4; ring >= 1; ring--) {
                int glowSize = (int)(100 + ring * 25);
                g2.setColor(new Color(0, 255, 150, (int)(120 * pulse / ring)));
                g2.fillOval(x - glowSize/2, y - glowSize/2, glowSize, glowSize);
            }
            
            // Pulsing border
            g2.setColor(new Color(0, 255, 150, (int)(200 * pulse)));
            g2.setStroke(new BasicStroke(5));
            g2.drawOval(x - 80, y - 80, 160, 160);
        }

        // Station outer ring shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(x - 38, y - 38, 76, 76);

        // Station outer ring - dark blue
        g2.setColor(new Color(10, 60, 150));
        g2.fillOval(x - 36, y - 36, 72, 72);

        // Station middle ring - bright blue
        g2.setColor(new Color(30, 130, 255));
        g2.fillOval(x - 30, y - 30, 60, 60);

        // Station inner ring - lighter blue
        g2.setColor(new Color(60, 160, 255));
        g2.fillOval(x - 24, y - 24, 48, 48);

        // Station highlight/gloss
        g2.setColor(new Color(120, 200, 255));
        g2.fillOval(x - 18, y - 22, 24, 20);

        // Station ID number
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        FontMetrics fm = g2.getFontMetrics();
        String id = String.valueOf(station.id);
        g2.drawString(id, x - fm.stringWidth(id)/2, y + 10);

        // Station name background
        g2.setColor(new Color(10, 20, 40, 200));
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        int nameWidth = fm.stringWidth(station.name);
        g2.fillRoundRect(x - nameWidth/2 - 8, y - 55, nameWidth + 16, 20, 5, 5);

        // Station name text
        g2.setColor(new Color(100, 200, 255));
        g2.drawString(station.name, x - nameWidth/2, y - 40);

        // Station status indicator
        g2.setColor(new Color(0, 255, 100));
        g2.fillOval(x + 28, y - 28, 12, 12);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(1));
        g2.drawOval(x + 28, y - 28, 12, 12);
    }

    private void drawIncident(Graphics2D g2, Incident incident) {
        int[] screen = latLonToScreen(incident.lat, incident.lon);
        int x = screen[0], y = screen[1];
        if (!isPointVisible(screen)) return;

        Color base = incident.severity == 3 ? new Color(255, 30, 30) :
                     incident.severity == 2 ? new Color(255, 100, 0) : new Color(255, 180, 50);

        // Multiple pulsing rings with wave effect
        for (int ring = 5; ring >= 1; ring--) {
            float wavePhase = animationFrame * 0.15f + ring * 0.5f;
            float pulse = (float) Math.abs(Math.sin(wavePhase));
            int baseSize = 50 + ring * 30;
            int size = baseSize + (int)(pulse * 20);
            
            int alpha = (int)(120 * (1 - ring / 5.0f) * pulse);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(30, alpha)));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(x - size/2, y - size/2, size, size);
        }

        // Core fire circle
        g2.setColor(base);
        g2.fillOval(x - 25, y - 25, 50, 50);

        // Inner glow
        g2.setColor(new Color(255, 150, 0, 180));
        g2.fillOval(x - 18, y - 18, 36, 36);

        // Flame center
        g2.setColor(new Color(255, 255, 100));
        g2.fillOval(x - 10, y - 10, 20, 20);

        // Fire emoji
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 32));
        g2.drawString("🔥", x - 16, y + 12);

        // Severity indicator
        String severityLabel = incident.severity == 3 ? "CRITICAL" :
                              incident.severity == 2 ? "MEDIUM" : "LOW";
        g2.setColor(base);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(severityLabel, x - fm.stringWidth(severityLabel)/2, y + 50);
    }

    private void drawHUD(Graphics2D g2) {
        int margin = 25;
        int hudW = 480;
        int hudH = 260;
        int hudX = margin;
        int hudY = getHeight() - hudH - margin;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(hudX, hudY, hudW, hudH, 20, 20);
        g2.setColor(new Color(60, 120, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(hudX, hudY, hudW, hudH, 20, 20);

        g2.setColor(new Color(80, 180, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.drawString("🚒 ISLAMABAD FIRE BRIGADE", hudX + 25, hudY + 45);

        g2.setColor(new Color(200, 240, 255));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        g2.drawString("🕐 System Time: " + time, hudX + 25, hudY + 80);
        g2.drawString("🔴 Active Incidents: " + incidents.size(), hudX + 25, hudY + 110);
        g2.drawString("🟦 Stations Online: " + stations.size(), hudX + 25, hudY + 140);
        g2.drawString("📍 Zoom: " + String.format("%.1f", zoomLevel), hudX + 25, hudY + 170);

        g2.setColor(new Color(120, 255, 180));
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.drawString("Click = Report Fire | Scroll = Zoom | Middle Drag = Pan", hudX + 25, hudY + 210);
    }

    private boolean isPointVisible(int[] point) {
        return point[0] >= -150 && point[0] <= getWidth() + 150 &&
               point[1] >= -150 && point[1] <= getHeight() + 150;
    }

    public void setCallback(StatsPanel stats, IncidentsPanel incidents) {
        this.statsCallback = stats;
        this.incidentsCallback = incidents;
    }

    public ArrayList<FireStation> getStations() {
        return stations;
    }

    public ArrayList<Incident> getIncidents() {
        return incidents;
    }
}

class StatsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;
    private JLabel totalIncidentsLabel, activeIncidentsLabel, criticalLabel, mediumLabel, lowLabel;
    private JLabel responseTotalLabel, avgResponseLabel;

    public StatsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(0, 100, 200));
        JLabel title = new JLabel("📊 SYSTEM DASHBOARD");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        JPanel statsGrid = createStatsGrid();
        JPanel chartsPanel = createChartsPanel();

        add(header, BorderLayout.NORTH);
        add(statsGrid, BorderLayout.WEST);
        add(chartsPanel, BorderLayout.CENTER);
    }

    private JPanel createStatsGrid() {
        JPanel grid = new JPanel(new GridLayout(3, 2, 20, 20));
        grid.setBackground(new Color(245, 248, 255));
        grid.setPreferredSize(new Dimension(350, 400));

        totalIncidentsLabel = createStatCard("📋 Total Incidents", "0", new Color(70, 130, 180));
        activeIncidentsLabel = createStatCard("🔴 Active Now", "0", new Color(220, 50, 50));
        criticalLabel = createStatCard("🔴 Critical", "0", new Color(255, 0, 0));
        mediumLabel = createStatCard("🟠 Medium", "0", new Color(255, 140, 0));
        lowLabel = createStatCard("🟡 Low", "0", new Color(255, 200, 0));
        responseTotalLabel = createStatCard("⚡ Response Time", "0s", new Color(50, 150, 50));

        grid.add(totalIncidentsLabel);
        grid.add(activeIncidentsLabel);
        grid.add(criticalLabel);
        grid.add(mediumLabel);
        grid.add(lowLabel);
        grid.add(responseTotalLabel);

        return grid;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 248, 255));
        panel.setBorder(new TitledBorder("📈 Real-time Statistics"));

        JTextArea stats = new JTextArea();
        stats.setEditable(false);
        stats.setFont(new Font("Arial", Font.PLAIN, 13));
        stats.setBackground(new Color(240, 245, 255));
        stats.setText("INCIDENT ANALYSIS\n\n" +
            "System Status: OPERATIONAL ✅\n" +
            "Total Stations: 5\n" +
            "Coverage Area: 1200+ km²\n" +
            "Average Response Time: 8.5 minutes\n" +
            "Success Rate: 98.7%\n\n" +
            "MONTHLY STATISTICS\n" +
            "Total Incidents: 47\n" +
            "Critical: 12\n" +
            "Medium: 23\n" +
            "Low: 12\n\n" +
            "TOP INCIDENT ZONES\n" +
            "1. Blue Area - 14 incidents\n" +
            "2. G-6 Sector - 11 incidents\n" +
            "3. Margalla Road - 8 incidents");

        JScrollPane scroll = new JScrollPane(stats);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JLabel label = new JLabel("<html><center>" + title + "<br><font size='5'>" + value + "</font></center></html>");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createRaisedBevelBorder());
        label.setPreferredSize(new Dimension(150, 100));
        return label;
    }

    public void updateData(ArrayList<Incident> incidents, ArrayList<FireStation> stations) {
        int total = incidents.size();
        int critical = (int) incidents.stream().filter(i -> i.severity == 3).count();
        int medium = (int) incidents.stream().filter(i -> i.severity == 2).count();
        int low = (int) incidents.stream().filter(i -> i.severity == 1).count();

        totalIncidentsLabel.setText("<html><center>📋 Total Incidents<br><font size='5'>" + total + "</font></center></html>");
        activeIncidentsLabel.setText("<html><center>🔴 Active Now<br><font size='5'>" + total + "</font></center></html>");
        criticalLabel.setText("<html><center>🔴 Critical<br><font size='5'>" + critical + "</font></center></html>");
        mediumLabel.setText("<html><center>🟠 Medium<br><font size='5'>" + medium + "</font></center></html>");
        lowLabel.setText("<html><center>🟡 Low<br><font size='5'>" + low + "</font></center></html>");
    }
}

class IncidentsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;
    private JTextArea incidentsArea;

    public IncidentsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(220, 50, 50));
        JLabel title = new JLabel("🚨 INCIDENT MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        incidentsArea = new JTextArea();
        incidentsArea.setEditable(false);
        incidentsArea.setFont(new Font("Courier", Font.PLAIN, 12));
        incidentsArea.setBackground(new Color(20, 25, 35));
        incidentsArea.setForeground(new Color(100, 255, 100));

        JScrollPane scroll = new JScrollPane(incidentsArea);
        scroll.setBorder(new LineBorder(new Color(0, 100, 200), 2));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void updateIncidents(ArrayList<Incident> incidents) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ACTIVE INCIDENTS REPORT ===\n\n");
        
        for (int i = 0; i < incidents.size(); i++) {
            Incident inc = incidents.get(i);
            String severity = inc.severity == 3 ? "🔴 CRITICAL" : inc.severity == 2 ? "🟠 MEDIUM" : "🟡 LOW";
            sb.append("[").append(i+1).append("] ").append(severity).append("\n");
            sb.append("Location: ").append(String.format("%.4f, %.4f", inc.lat, inc.lon)).append("\n");
            sb.append("Status: ").append(inc.respondingStation >= 0 ? "RESPONDING" : "PENDING").append("\n");
            sb.append("Time: ").append(new SimpleDateFormat("HH:mm:ss").format(new Date())).append("\n\n");
        }
        
        if (incidents.isEmpty()) {
            sb.append("No active incidents. System operational.\n");
        }
        
        incidentsArea.setText(sb.toString());
    }
}

class StationsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;
    private JPanel stationsContainer;

    public StationsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(0, 120, 200));
        JLabel title = new JLabel("🏢 FIRE STATIONS");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        stationsContainer = new JPanel();
        stationsContainer.setLayout(new BoxLayout(stationsContainer, BoxLayout.Y_AXIS));
        stationsContainer.setBackground(new Color(245, 248, 255));

        ArrayList<FireStation> stations = mapPanel.getStations();
        for (FireStation station : stations) {
            stationsContainer.add(createStationCard(station));
            stationsContainer.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(stationsContainer);
        scroll.setBackground(new Color(245, 248, 255));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createStationCard(FireStation station) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(new Color(220, 235, 255));
        card.setBorder(new LineBorder(new Color(0, 100, 200), 2));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel nameLabel = new JLabel("🚒 " + station.name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel locLabel = new JLabel("📍 Lat: " + String.format("%.4f", station.latitude) + " | Lon: " + String.format("%.4f", station.longitude));
        locLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel statusLabel = new JLabel("✅ Status: ONLINE | Vehicles: 4 | Personnel: 12");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(0, 150, 0));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setBackground(new Color(220, 235, 255));
        info.add(nameLabel);
        info.add(locLabel);
        info.add(statusLabel);

        card.add(info, BorderLayout.CENTER);
        return card;
    }
}

class AnalyticsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;

    public AnalyticsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(70, 130, 180));
        JLabel title = new JLabel("📈 ANALYTICS & REPORTS");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        JTextArea analytics = new JTextArea();
        analytics.setEditable(false);
        analytics.setFont(new Font("Arial", Font.PLAIN, 13));
        analytics.setBackground(new Color(240, 245, 255));
        analytics.setText("PERFORMANCE METRICS\n\n" +
            "═══════════════════════════════════════════════════\n" +
            "Response Time Analysis:\n" +
            "  • Average: 8.5 minutes\n" +
            "  • Fastest: 4.2 minutes (Blue Area)\n" +
            "  • Slowest: 15.3 minutes (Airport Road)\n\n" +
            "Incident Distribution:\n" +
            "  • Critical (33%): 15 incidents\n" +
            "  • Medium (48%): 22 incidents\n" +
            "  • Low (19%): 9 incidents\n\n" +
            "Station Efficiency:\n" +
            "  1. Main Station: 92% availability\n" +
            "  2. Blue Area: 88% availability\n" +
            "  3. G-6 Sector: 95% availability\n" +
            "  4. Margalla Road: 87% availability\n" +
            "  5. Airport Road: 91% availability\n\n" +
            "System Health: 90.6% EXCELLENT ✅\n");

        JScrollPane scroll = new JScrollPane(analytics);
        scroll.setBorder(new LineBorder(new Color(0, 100, 200), 2));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }
}

class RoadNetwork {
    int from, to, distance, width;
    double lat1, lon1, lat2, lon2;
    String name;
    
    RoadNetwork(int from, int to, String name, double lat1, double lon1, 
                double lat2, double lon2, int distance, int width) {
        this.from = from;
        this.to = to;
        this.name = name;
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.distance = distance;
        this.width = width;
    }
}

class FireStation {
    int id;
    String name;
    double latitude, longitude;
    
    FireStation(int id, String name, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.latitude = lat;
        this.longitude = lon;
    }
}

class Incident {
    int respondingStation;
    int severity;
    double lat, lon;
    
    Incident(int station, int severity, double lat, double lon) {
        this.respondingStation = station;
        this.severity = severity;
        this.lat = lat;
        this.lon = lon;
    }
}

class GraphDS {
    private ArrayList<ArrayList<EdgeDS>> adjacencyList;
    
    GraphDS(int vertices) {
        adjacencyList = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }
    
    void addEdge(int u, int v, int weight) {
        adjacencyList.get(u).add(new EdgeDS(v, weight));
    }
    
    ArrayList<Integer> dijkstra(int start) {
        int vertices = adjacencyList.size();
        int[] dist = new int[vertices];
        int[] parent = new int[vertices];
        boolean[] visited = new boolean[vertices];
        
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        dist[start] = 0;
        
        for (int i = 0; i < vertices - 1; i++) {
            int u = -1;
            for (int j = 0; j < vertices; j++) {
                if (!visited[j] && (u == -1 || dist[j] < dist[u])) {
                    u = j;
                }
            }
            
            if (dist[u] == Integer.MAX_VALUE) break;
            visited[u] = true;
            
            for (EdgeDS edge : adjacencyList.get(u)) {
                int v = edge.to;
                if (!visited[v] && dist[u] + edge.weight < dist[v]) {
                    dist[v] = dist[u] + edge.weight;
                    parent[v] = u;
                }
            }
        }
        
        ArrayList<Integer> route = new ArrayList<>();
        int farthest = 0;
        for (int i = 1; i < vertices; i++) {
            if (dist[i] < Integer.MAX_VALUE && dist[i] > dist[farthest]) {
                farthest = i;
            }
        }
        
        int current = farthest;
        while (current != -1) {
            route.add(0, current);
            current = parent[current];
        }
        
        return route;
    }
}

class EdgeDS {
    int to, weight;
    EdgeDS(int to, int weight) {
        this.to = to;
        this.weight = weight;
    }
}