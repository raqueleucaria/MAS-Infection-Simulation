package view;
import java.awt.Color;

public enum MicrobeColor {
    BLUE(Color.BLUE), 
    RED(Color.RED);
    
    private final Color color;
    
    MicrobeColor(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
}