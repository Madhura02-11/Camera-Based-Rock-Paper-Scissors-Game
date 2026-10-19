public class GestureDetector {

    private String gesture = "Unknown";

    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;

    public void update(
            String gesture,
            int x,
            int y,
            int width,
            int height
    ) {

        this.gesture = gesture;

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;
    }

    public String getGesture() {
        return gesture;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}