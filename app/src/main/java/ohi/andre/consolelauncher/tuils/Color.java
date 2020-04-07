package ohi.andre.consolelauncher.tuils;

// I needed to create this class to remove the binding between Integer and color
public class Color {
    public final int intValue;

    public Color(int intValue) {
        this.intValue = intValue;
    }

    public static final Color nullColor = new Color(Integer.MIN_VALUE);
}
