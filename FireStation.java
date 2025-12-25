import java.awt.Color;

public class FireStation {

    private String name;
    private int x, y;
    private Color color = Color.BLUE;

    public FireStation(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public double distanceTo(int fx, int fy) {
        return Math.sqrt(Math.pow(x - fx, 2) + Math.pow(y - fy, 2));
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}