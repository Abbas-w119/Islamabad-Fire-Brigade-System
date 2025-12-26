import org.jxmapviewer.viewer.GeoPosition;

public class FireMarker {
    private GeoPosition position;

    public FireMarker(double lat, double lon) {
        position = new GeoPosition(lat, lon);
    }

    public GeoPosition getPosition() {
        return position;
    }
}
