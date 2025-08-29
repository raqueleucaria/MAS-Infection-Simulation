package br.com.eucaria.ui.custom;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class MainFrame extends JFrame {

    public static final Color RED_MICROBE_COLOR = new Color(227, 61, 51);
    public static final Color BLUE_MICROBE_COLOR = new Color(52, 152, 219);
    public static final Color EMPTY_SPACE_BACKGROUND = new Color(230, 230, 230);
    public static final Color GRID_BORDER_COLOR = new Color(200, 200, 200);

    private final Vector<MicrobePanel> microbePanels;

    public MainFrame(int gridSize, int runNumber) {
        super("Infection Simulation " + runNumber);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLayout(new GridLayout(gridSize, gridSize));

        this.microbePanels = new Vector<>();
        mountPanel(gridSize);
    }

    private void mountPanel(int gridSize) {
        for (int i = 0; i < gridSize * gridSize; i++) {
            MicrobePanel p = new MicrobePanel(EMPTY_SPACE_BACKGROUND, GRID_BORDER_COLOR);

            microbePanels.add(p);
            this.add(p);
        }
    }

    public void updatePanel(Vector<Color> microbeColors) {
        if (microbePanels.size() == microbeColors.size()) {
            for (int i = 0; i < microbePanels.size(); i++) {
                MicrobePanel panel = microbePanels.elementAt(i);
                Color currentMicrobeColor = microbeColors.get(i);

                if (currentMicrobeColor.equals(EMPTY_SPACE_BACKGROUND)) {
                    panel.setMicrobeColor(null);
                } else {
                    panel.setMicrobeColor(currentMicrobeColor);
                }
            }
        }
        repaint();
    }
}