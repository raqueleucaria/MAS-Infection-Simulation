package br.com.eucaria.ui.custom;
import javax.swing.*;
import java.awt.*;

public class MicrobePanel extends JPanel {

    private Color microbeColor;
    private Color backgroundColor;

    public MicrobePanel(Color defaultBackgroundColor, Color borderColor) {
        this.backgroundColor = defaultBackgroundColor;
        this.microbeColor = null;
        this.setBorder(BorderFactory.createLineBorder(borderColor));
        this.setBackground(backgroundColor);
    }

    public void setMicrobeColor(Color color) {
        this.microbeColor = color;
        repaint();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        this.setBackground(color);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (microbeColor != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int panelWidth = getWidth();
            int panelHeight = getHeight();

            int circleDiameter = Math.min(panelWidth, panelHeight) - 10;
            int x = (panelWidth - circleDiameter) / 2;
            int y = (panelHeight - circleDiameter) / 2;

            g2d.setColor(microbeColor);
            g2d.fillOval(x, y, circleDiameter, circleDiameter);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(x, y, circleDiameter, circleDiameter);
        }
    }
}