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
