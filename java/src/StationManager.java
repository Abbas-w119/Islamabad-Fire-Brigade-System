import java.util.ArrayList;
import java.util.List;

public class StationManager {

    private final List<FireStation> stations = new ArrayList<>();

    public void addStation(FireStation s) {
        stations.add(s);
    }

    public void removeStation(FireStation s) {
        stations.remove(s);
    }

    public List<FireStation> getStations() {
        return stations;
    }
}
