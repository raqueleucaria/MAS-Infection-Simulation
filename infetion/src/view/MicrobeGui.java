package view;

import javax.swing.*;
import java.awt.*;

public class MicrobeGui extends JComponent {
    private String agentName;
    private MicrobeColor color;
    private Point position;
    
    public MicrobeGui(String agentName, MicrobeColor color, Point position) {
        this.agentName = agentName;
        this.color = color;
        this.position = position;
        setPreferredSize(new Dimension(40, 40));
    }
    
    public String getAgentName() {
        return agentName;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int diameter = Math.min(getWidth(), getHeight()) - 10;
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;
        
        g2d.setColor(color.getColor());
        g2d.fillOval(x, y, diameter, diameter);
        
//        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y, diameter, diameter);
    }
}