import org.jxmapviewer.viewer.GeoPosition;

public class StationMarker {
    private GeoPosition position;
    private String name;

    public StationMarker(String name, double lat, double lon) {
        this.name = name;
        this.position = new GeoPosition(lat, lon);
    }

    public GeoPosition getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }
}
public class StationMarker {
    private String name;
    private int x;
    private int y;

    public StationMarker(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
}