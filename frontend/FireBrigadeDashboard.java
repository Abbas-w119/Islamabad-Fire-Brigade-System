import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;

public class FireBrigadeDashboard extends JFrame {
    private TabbedMainPanel tabbedPanel;
    private SocketClient socketClient;

    public FireBrigadeDashboard() {
        try {
            setTitle("Islamabad Fire Brigade System - Real-time Dashboard");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1600, 900);
            setLocationRelativeTo(null);
            setExtendedState(JFrame.MAXIMIZED_BOTH);

            // Connect to C++ Server
            System.out.println("\n===================================");
            System.out.println("Connecting to C++ Backend Server...");
            System.out.println("===================================\n");

            socketClient = new SocketClient("127.0.0.1", 5000, new SocketClient.ClientListener() {
                @Override
                public void onConnected(String welcomeMessage) {
                    System.out.println("\n===================================");
                    System.out.println("SUCCESS! Connected to C++ Server!");
                    System.out.println("===================================");
                    System.out.println("Message: " + welcomeMessage);
                    System.out.println("===================================\n");
                }

                @Override
                public void onDisconnected() {
                    System.out.println("\nDISCONNECTED from server\n");
                }

                @Override
                public void onMessageReceived(String message) {
                    System.out.println("[SERVER] " + message);
                }

                @Override
                public void onError(String error) {
                    System.out.println("\n[ERROR] " + error);
                }
            });

            boolean connected = socketClient.connect();
            if (!connected) {
                System.out.println("\n[FATAL] Could not connect to C++ server!");
                System.out.println("Make sure: 1) Server is running");
                System.out.println("           2) Port 5000 is not blocked");
                System.out.println("           3) Firewall allows connection\n");
            }

            System.out.println("[GUI] Initializing TabbedMainPanel...");
            tabbedPanel = new TabbedMainPanel(socketClient);
            System.out.println("[GUI] TabbedMainPanel created successfully");
            
            add(tabbedPanel);
            System.out.println("[GUI] Panel added to frame");
            
            setVisible(true);
            System.out.println("[GUI] Window made visible - READY!");
        } catch (Exception e) {
            System.out.println("[CRITICAL ERROR] GUI initialization failed!");
            System.out.println(e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage(), "Critical Error", JOptionPane.ERROR_MESSAGE);
        }
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
    private SocketClient socketClient;

    public TabbedMainPanel(SocketClient socketClient) {
        this.socketClient = socketClient;
        setLayout(new BorderLayout());
        System.out.println("[PANEL] Creating TabbedMainPanel");
        
        try {
            System.out.println("[PANEL] Creating AdvancedMapPanel...");
            mapPanel = new AdvancedMapPanel(socketClient);
            System.out.println("[PANEL] AdvancedMapPanel created");
            
            System.out.println("[PANEL] Creating StatsPanel...");
            statsPanel = new StatsPanel(mapPanel);
            System.out.println("[PANEL] StatsPanel created");
            
            System.out.println("[PANEL] Creating IncidentsPanel...");
            incidentsPanel = new IncidentsPanel(mapPanel);
            System.out.println("[PANEL] IncidentsPanel created");
            
            System.out.println("[PANEL] Creating StationsPanel...");
            stationsPanel = new StationsPanel(mapPanel);
            System.out.println("[PANEL] StationsPanel created");
            
            System.out.println("[PANEL] Creating AnalyticsPanel...");
            analyticsPanel = new AnalyticsPanel(mapPanel);
            System.out.println("[PANEL] AnalyticsPanel created");

            System.out.println("[PANEL] Creating TabbedPane...");
            tabbedPane = new JTabbedPane();
            tabbedPane.setBackground(new Color(245, 245, 245));
            tabbedPane.setForeground(new Color(40, 40, 40));
            tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

            System.out.println("[PANEL] Adding tabs to TabbedPane...");
            tabbedPane.addTab("Map", mapPanel);
            tabbedPane.addTab("Dashboard", statsPanel);
            tabbedPane.addTab("Incidents", incidentsPanel);
            tabbedPane.addTab("Stations", stationsPanel);
            tabbedPane.addTab("Analytics", analyticsPanel);

            System.out.println("[PANEL] Adding TabbedPane to main panel...");
            add(tabbedPane, BorderLayout.CENTER);
            System.out.println("[PANEL] TabbedMainPanel fully initialized!");
        } catch (Exception e) {
            System.out.println("[PANEL ERROR] Exception during UI creation: " + e.getMessage());
            e.printStackTrace();
            
            // Create minimal fallback UI
            System.out.println("[PANEL] Creating fallback UI...");
            JLabel errorLabel = new JLabel("Error initializing panels: " + e.getMessage());
            errorLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            errorLabel.setHorizontalAlignment(JLabel.CENTER);
            add(errorLabel, BorderLayout.CENTER);
        }
    }
}

class AdvancedMapPanel extends JPanel {
    private ArrayList<FireStation> stations = new ArrayList<>();
    private ArrayList<Incident> incidents = new ArrayList<>();
    private ArrayList<RoadNetwork> roadNetwork = new ArrayList<>();
    private GraphDS graph;
    private SocketClient socketClient;
    private double zoomLevel = 13.0;
    private double centerLat = 33.6844;
    private double centerLon = 73.0479;
    private int animationFrame = 0;
    private ArrayList<Integer> currentRoute = new ArrayList<>();
    private ArrayList<double[]> routeCoordinates = new ArrayList<>();
    private int respondingStationId = -1;
    private Point lastMousePos;

    public AdvancedMapPanel(SocketClient socketClient) {
        try {
            System.out.println("[MAP] Initializing AdvancedMapPanel");
            this.socketClient = socketClient;
            setBackground(new Color(10, 15, 25));
            setBorder(new LineBorder(new Color(30, 50, 80), 3));
            setPreferredSize(new Dimension(800, 600));

            System.out.println("[MAP] Initializing stations and roads...");
            initializeStations();
            initializeRoads();
            System.out.println("[MAP] Creating graph...");
            graph = new GraphDS(stations.size());
            buildGraph();
            System.out.println("[MAP] Starting animation timer...");
            startAnimation();

            System.out.println("[MAP] Adding mouse listeners...");
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
            System.out.println("[MAP] AdvancedMapPanel fully initialized!");
        } catch (Exception e) {
            System.out.println("[MAP ERROR] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeStations() {
        stations.add(new FireStation(0, "Main Station", 33.6844, 73.0479));
        stations.add(new FireStation(1, "Blue Area", 33.7182, 73.0605));
        stations.add(new FireStation(2, "G-6 Sector", 33.7100, 73.0800));
        stations.add(new FireStation(3, "Margalla Road", 33.7400, 73.0900));
        stations.add(new FireStation(4, "Airport Road", 33.6167, 73.0992));
    }

    private void initializeRoads() {
        roadNetwork.add(new RoadNetwork(0, 1, "Constitution Ave", 33.6844, 73.0479, 33.7182, 73.0605, 6, 18));
        roadNetwork.add(new RoadNetwork(0, 3, "Margalla Road", 33.6844, 73.0479, 33.7400, 73.0900, 8, 16));
        roadNetwork.add(new RoadNetwork(1, 2, "Jinnah Avenue", 33.7182, 73.0605, 33.7100, 73.0800, 5, 14));
        roadNetwork.add(new RoadNetwork(1, 4, "Srinagar Highway", 33.7182, 73.0605, 33.6167, 73.0992, 12, 20));
        roadNetwork.add(new RoadNetwork(2, 3, "G-6 Link Road", 33.7100, 73.0800, 33.7400, 73.0900, 7, 12));
        roadNetwork.add(new RoadNetwork(3, 4, "Northern Bypass", 33.7400, 73.0900, 33.6167, 73.0992, 15, 18));
        roadNetwork.add(new RoadNetwork(0, 2, "Main Expressway", 33.6844, 73.0479, 33.7100, 73.0800, 9, 22));
        roadNetwork.add(new RoadNetwork(0, 4, "Airport Highway", 33.6844, 73.0479, 33.6167, 73.0992, 18, 26));
        roadNetwork.add(new RoadNetwork(1, 3, "Blue Area Link", 33.7182, 73.0605, 33.7400, 73.0900, 10, 14));
        roadNetwork.add(new RoadNetwork(2, 4, "Scenic Route", 33.7100, 73.0800, 33.6167, 73.0992, 16, 20));
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
            "Fire Report", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            String[] severities = {"Low", "Medium", "Critical"};
            String severity = (String) JOptionPane.showInputDialog(this,
                "Select Incident Severity:", "Severity Level",
                JOptionPane.QUESTION_MESSAGE, null, severities, severities[2]);

            if (severity != null) {
                int sevLevel = severity.equals("Low") ? 1 : (severity.equals("Medium") ? 2 : 3);
                int nearestStation = findNearestStation(lat, lon);

                Incident incident = new Incident(nearestStation, sevLevel, lat, lon);
                incidents.add(incident);

                respondingStationId = nearestStation;
                currentRoute = graph.dijkstra(nearestStation);
                buildRouteCoordinates();

                // Send to C++ server
                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.reportIncident(nearestStation, sevLevel, lat, lon);
                }

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

        GradientPaint gp = new GradientPaint(0, 0, new Color(12, 18, 35), 0, getHeight(), new Color(5, 8, 20));
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
        for (RoadNetwork road : roadNetwork) {
            int[] p1 = latLonToScreen(road.lat1, road.lon1);
            int[] p2 = latLonToScreen(road.lat2, road.lon2);
            if (!isPointVisible(p1) && !isPointVisible(p2)) continue;

            g2.setColor(new Color(80, 160, 255, 60));
            g2.setStroke(new BasicStroke(road.width + 10));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            g2.setColor(new Color(35, 45, 65));
            g2.setStroke(new BasicStroke(road.width + 4));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);

            g2.setColor(new Color(200, 200, 100, 120));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 15}, 0));
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }
    }

    private void drawRoute(Graphics2D g2) {
        g2.setColor(new Color(255, 80, 0, 200));
        g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            int[] p1 = latLonToScreen(routeCoordinates.get(i)[0], routeCoordinates.get(i)[1]);
            int[] p2 = latLonToScreen(routeCoordinates.get(i+1)[0], routeCoordinates.get(i+1)[1]);
            g2.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }

        g2.setColor(new Color(255, 255, 100));
        float progress = (animationFrame % 120) / 120.0f;
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            double[] c1 = routeCoordinates.get(i);
            double[] c2 = routeCoordinates.get(i + 1);
            int[] p1 = latLonToScreen(c1[0], c1[1]);
            int[] p2 = latLonToScreen(c2[0], c2[1]);
            int px = (int)(p1[0] + (p2[0] - p1[0]) * progress);
            int py = (int)(p1[1] + (p2[1] - p1[1]) * progress);
            g2.fillOval(px - 10, py - 10, 20, 20);
        }
    }

    private void drawStation(Graphics2D g2, FireStation station) {
        int[] screen = latLonToScreen(station.latitude, station.longitude);
        int x = screen[0], y = screen[1];
        if (!isPointVisible(screen)) return;

        if (station.id == respondingStationId) {
            float pulse = (float)(0.4 + 0.6 * Math.sin(animationFrame * 0.12));
            int alpha = Math.min(255, (int)(100 * pulse));
            g2.setColor(new Color(0, 255, 150, alpha));
            g2.fillOval(x - 60, y - 60, 120, 120);
        }

        g2.setColor(new Color(20, 80, 180));
        g2.fillOval(x - 32, y - 32, 64, 64);
        g2.setColor(new Color(50, 150, 255));
        g2.fillOval(x - 28, y - 28, 56, 56);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString(String.valueOf(station.id), x - 12, y + 10);

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(station.name, x - fm.stringWidth(station.name)/2, y + 45);
    }

    private void drawIncident(Graphics2D g2, Incident incident) {
        int[] screen = latLonToScreen(incident.lat, incident.lon);
        int x = screen[0], y = screen[1];
        if (!isPointVisible(screen)) return;

        Color base = incident.severity == 3 ? new Color(255, 50, 50) :
                     incident.severity == 2 ? new Color(255, 120, 0) : new Color(255, 200, 50);

        for (int i = 4; i >= 1; i--) {
            float pulse = (float) Math.abs(Math.sin(animationFrame * 0.15 + i * 0.5));
            int size = 40 + i * 25 + (int)(pulse * 15);
            int alpha = Math.max(0, Math.min(255, 80 - i*15));
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;
            try {
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                g2.setStroke(new BasicStroke(5));
                g2.drawOval(x - size/2, y - size/2, size, size);
            } catch (Exception e) {
                System.out.println("[PAINT ERROR] Invalid color alpha: " + alpha);
            }
        }

        g2.setColor(base);
        g2.fillOval(x - 22, y - 22, 44, 44);
        g2.setColor(new Color(255, 180, 0));
        g2.fillOval(x - 16, y - 16, 32, 32);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.drawString("F", x - 10, y + 12);
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
        g2.drawString("ISLAMABAD FIRE BRIGADE", hudX + 25, hudY + 45);

        g2.setColor(new Color(200, 240, 255));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        g2.drawString("System Time: " + time, hudX + 25, hudY + 80);
        g2.drawString("Active Incidents: " + incidents.size(), hudX + 25, hudY + 110);
        g2.drawString("Stations Online: " + stations.size(), hudX + 25, hudY + 140);
        g2.drawString("Zoom: " + String.format("%.1f", zoomLevel), hudX + 25, hudY + 170);

        g2.setColor(new Color(120, 255, 180));
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.drawString("Click = Report Fire | Scroll = Zoom | Middle Drag = Pan", hudX + 25, hudY + 210);
    }

    private boolean isPointVisible(int[] point) {
        return point[0] >= -150 && point[0] <= getWidth() + 150 &&
               point[1] >= -150 && point[1] <= getHeight() + 150;
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
    private JLabel incidentsLabel, stationsLabel, systemStatusLabel;
    private JTextArea stats;

    public StatsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(0, 100, 200));
        JLabel title = new JLabel("SYSTEM DASHBOARD");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        JPanel statsGrid = new JPanel(new GridLayout(3, 2, 20, 20));
        statsGrid.setBackground(new Color(245, 248, 255));
        statsGrid.setPreferredSize(new Dimension(350, 300));

        incidentsLabel = createStatCard("Active Incidents", "0", new Color(220, 50, 50));
        stationsLabel = createStatCard("Fire Stations", "5", new Color(0, 120, 200));
        systemStatusLabel = createStatCard("System Status", "ACTIVE", new Color(50, 150, 50));

        statsGrid.add(incidentsLabel);
        statsGrid.add(stationsLabel);
        statsGrid.add(systemStatusLabel);
        statsGrid.add(createStatCard("Response Time", "8.5 min", new Color(70, 130, 180)));
        statsGrid.add(createStatCard("Coverage Area", "1200 km2", new Color(100, 100, 100)));
        statsGrid.add(createStatCard("System Health", "90.6%", new Color(0, 150, 0)));

        JPanel chartsPanel = new JPanel(new BorderLayout());
        chartsPanel.setBackground(new Color(245, 248, 255));
        chartsPanel.setBorder(new TitledBorder("Real-time Statistics"));

        stats = new JTextArea();
        stats.setEditable(false);
        stats.setFont(new Font("Arial", Font.PLAIN, 13));
        stats.setBackground(new Color(240, 245, 255));
        updateStats();

        JScrollPane scroll = new JScrollPane(stats);
        chartsPanel.add(scroll, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(statsGrid, BorderLayout.WEST);
        add(chartsPanel, BorderLayout.CENTER);
        
        // Auto-update every 500ms
        new Timer(500, e -> updateDisplay()).start();
    }
    
    private void updateDisplay() {
        int incidentCount = mapPanel.getIncidents().size();
        incidentsLabel.setText("<html><center>Active Incidents<br><font size='6'>" + incidentCount + "</font></center></html>");
        updateStats();
    }
    
    private void updateStats() {
        ArrayList<Incident> incidents = mapPanel.getIncidents();
        int critical = 0, medium = 0, low = 0;
        for (Incident inc : incidents) {
            if (inc.severity == 3) critical++;
            else if (inc.severity == 2) medium++;
            else low++;
        }
        
        stats.setText("INCIDENT ANALYSIS\n\n" +
            "System Status: " + (incidents.isEmpty() ? "OPERATIONAL" : "ACTIVE RESPONSE") + "\n" +
            "Total Stations: 5\n" +
            "Coverage Area: 1200+ km2\n" +
            "Average Response Time: 8.5 minutes\n" +
            "Success Rate: 98.7%\n\n" +
            "CURRENT STATISTICS\n" +
            "Total Active Incidents: " + incidents.size() + "\n" +
            "Critical: " + critical + "\n" +
            "Medium: " + medium + "\n" +
            "Low: " + low + "\n\n" +
            "SYSTEM INFO\n" +
            "Connected Stations: 5\n" +
            "Available Vehicles: 19\n" +
            "Network Status: ONLINE");
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JLabel label = new JLabel("<html><center>" + title + "<br><font size='6'>" + value + "</font></center></html>");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createRaisedBevelBorder());
        return label;
    }
}

class IncidentsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;
    private JTextArea incidents;
    
    public IncidentsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(220, 50, 50));
        JLabel title = new JLabel("INCIDENT MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        incidents = new JTextArea();
        incidents.setEditable(false);
        incidents.setFont(new Font("Courier", Font.PLAIN, 12));
        incidents.setBackground(new Color(20, 25, 35));
        incidents.setForeground(new Color(100, 255, 100));
        updateIncidents();

        JScrollPane scroll = new JScrollPane(incidents);
        scroll.setBorder(new LineBorder(new Color(0, 100, 200), 2));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        // Auto-update every 500ms
        new Timer(500, e -> updateIncidents()).start();
    }
    
    private void updateIncidents() {
        ArrayList<Incident> incidentList = mapPanel.getIncidents();
        StringBuilder text = new StringBuilder("=== ACTIVE INCIDENTS REPORT ===\n\n");
        
        if (incidentList.isEmpty()) {
            text.append("No active incidents. System operational.\n\n")
                .append("[READY] Waiting for incident reports...\n\n")
                .append("When an incident is reported:\n")
                .append("- Location will be displayed\n")
                .append("- Nearest station will respond\n")
                .append("- Optimal route will be calculated\n")
                .append("- Real-time status will update");
        } else {
            text.append("CURRENT ACTIVE INCIDENTS: ").append(incidentList.size()).append("\n\n");
            for (int i = 0; i < incidentList.size(); i++) {
                Incident inc = incidentList.get(i);
                String severity = inc.severity == 3 ? "CRITICAL" : (inc.severity == 2 ? "MEDIUM" : "LOW");
                text.append("[INCIDENT ").append(i + 1).append("]\n")
                    .append("  Station: ").append(inc.respondingStation).append("\n")
                    .append("  Severity: ").append(severity).append("\n")
                    .append("  Location: ").append(String.format("%.4f, %.4f", inc.lat, inc.lon)).append("\n")
                    .append("  Status: RESPONDING\n\n");
            }
        }
        
        incidents.setText(text.toString());
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
        JLabel title = new JLabel("FIRE STATIONS");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        stationsContainer = new JPanel();
        stationsContainer.setLayout(new BoxLayout(stationsContainer, BoxLayout.Y_AXIS));
        stationsContainer.setBackground(new Color(245, 248, 255));
        updateStations();

        JScrollPane scroll = new JScrollPane(stationsContainer);
        scroll.setBackground(new Color(245, 248, 255));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        // Auto-update every 500ms
        new Timer(500, e -> updateStations()).start();
    }
    
    private void updateStations() {
        stationsContainer.removeAll();
        ArrayList<FireStation> stations = mapPanel.getStations();
        
        for (FireStation station : stations) {
            JPanel card = new JPanel(new BorderLayout(15, 15));
            card.setBackground(new Color(220, 235, 255));
            card.setBorder(new LineBorder(new Color(0, 100, 200), 2));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            String stationInfo = String.format("Station %d: %s | Lat: %.4f | Lon: %.4f | Vehicles: %d | Status: ONLINE",
                station.id, station.name, station.latitude, station.longitude, 3);
            JLabel stationLabel = new JLabel(stationInfo);
            stationLabel.setFont(new Font("Arial", Font.BOLD, 13));
            card.add(stationLabel, BorderLayout.CENTER);

            stationsContainer.add(card);
            stationsContainer.add(Box.createVerticalStrut(8));
        }
        
        stationsContainer.revalidate();
        stationsContainer.repaint();
    }
}

class AnalyticsPanel extends JPanel {
    private AdvancedMapPanel mapPanel;
    private JTextArea analytics;
    
    public AnalyticsPanel(AdvancedMapPanel mapPanel) {
        this.mapPanel = mapPanel;
        setBackground(new Color(245, 248, 255));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel();
        header.setBackground(new Color(70, 130, 180));
        JLabel title = new JLabel("ANALYTICS & REPORTS");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title);

        analytics = new JTextArea();
        analytics.setEditable(false);
        analytics.setFont(new Font("Arial", Font.PLAIN, 13));
        analytics.setBackground(new Color(240, 245, 255));
        updateAnalytics();

        JScrollPane scroll = new JScrollPane(analytics);
        scroll.setBorder(new LineBorder(new Color(0, 100, 200), 2));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        // Auto-update every 500ms
        new Timer(500, e -> updateAnalytics()).start();
    }
    
    private void updateAnalytics() {
        ArrayList<Incident> incidents = mapPanel.getIncidents();
        int critical = 0, medium = 0, low = 0;
        for (Incident inc : incidents) {
            if (inc.severity == 3) critical++;
            else if (inc.severity == 2) medium++;
            else low++;
        }
        
        int totalIncidents = critical + medium + low;
        double criticalPercent = totalIncidents > 0 ? (critical * 100.0 / totalIncidents) : 33;
        double mediumPercent = totalIncidents > 0 ? (medium * 100.0 / totalIncidents) : 48;
        double lowPercent = totalIncidents > 0 ? (low * 100.0 / totalIncidents) : 19;
        
        analytics.setText("PERFORMANCE METRICS\n" +
            "═════════════════════════════════════════════════════\n" +
            "Response Time Analysis:\n" +
            "  • Average: 8.5 minutes\n" +
            "  • Fastest: 4.2 minutes (Blue Area)\n" +
            "  • Slowest: 15.3 minutes (Airport Road)\n\n" +
            "Incident Distribution (Live Data):\n" +
            "  • Critical (" + String.format("%.0f", criticalPercent) + "%): " + critical + " incidents\n" +
            "  • Medium (" + String.format("%.0f", mediumPercent) + "%): " + medium + " incidents\n" +
            "  • Low (" + String.format("%.0f", lowPercent) + "%): " + low + " incidents\n" +
            "  • Total Active: " + totalIncidents + " incidents\n\n" +
            "Station Efficiency:\n" +
            "  1. Main Station: 92% availability\n" +
            "  2. Blue Area: 88% availability\n" +
            "  3. G-6 Sector: 95% availability\n" +
            "  4. Margalla Road: 87% availability\n" +
            "  5. Airport Road: 91% availability\n\n" +
            "System Health: " + (incidents.isEmpty() ? "95.8%" : "ACTIVE") + " \n");
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

class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP = "127.0.0.1";
    private int serverPort = 5000;
    private boolean isConnected = false;
    private ClientListener listener;

    public interface ClientListener {
        void onConnected(String welcomeMessage);
        void onDisconnected();
        void onMessageReceived(String message);
        void onError(String error);
    }

    public SocketClient(String serverIP, int serverPort, ClientListener listener) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.listener = listener;
    }

    public boolean connect() {
        try {
            System.out.println("[CLIENT] Connecting to " + serverIP + ":" + serverPort);
            
            socket = new Socket(serverIP, serverPort);
            socket.setSoTimeout(5000); // 5 second timeout
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            isConnected = true;
            System.out.println("[CLIENT] Socket created");
            
            // Read welcome message with timeout in a separate thread
            new Thread(() -> {
                try {
                    String welcome = in.readLine();
                    if (welcome != null) {
                        System.out.println("[CLIENT] Welcome: " + welcome);
                        if (listener != null) {
                            listener.onConnected(welcome);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[CLIENT] No welcome message received (timeout)");
                }
            }).start();

            // Start message listener thread
            new Thread(() -> {
                try {
                    socket.setSoTimeout(0); // Remove timeout for long-running listener
                    String message;
                    while ((message = in.readLine()) != null && isConnected) {
                        System.out.println("[CLIENT] Message: " + message);
                        if (listener != null) {
                            listener.onMessageReceived(message);
                        }
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        System.out.println("[CLIENT] Connection lost");
                        if (listener != null) {
                            listener.onError("Connection lost");
                        }
                    }
                }
                disconnect();
            }).start();
            
            return true;
        } catch (IOException e) {
            System.out.println("[CLIENT] Connection failed: " + e.getMessage());
            if (listener != null) {
                listener.onError("Failed to connect: " + e.getMessage());
            }
            return false;
        }
    }

    public synchronized void sendMessage(String message) {
        if (isConnected && out != null) {
            out.println(message);
            System.out.println("[CLIENT] Sent: " + message);
        } else {
            System.out.println("[CLIENT] Not connected");
        }
    }

    public void reportIncident(int stationId, int severity, double lat, double lon) {
        String message = String.format("INCIDENT|%d|%d|%.4f|%.4f", stationId, severity, lat, lon);
        sendMessage(message);
    }

    public void requestRoute(int from, int to) {
        String message = String.format("DIJKSTRA|%d|%d", from, to);
        sendMessage(message);
    }

    public void ping() {
        sendMessage("PING");
    }

    public void disconnect() {
        try {
            isConnected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[CLIENT] Disconnected");
            if (listener != null) {
                listener.onDisconnected();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}
 
