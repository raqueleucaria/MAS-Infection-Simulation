package view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Board {
    private JFrame frame;
    private JPanel boardPanel;
    private JButton startButton;
    private Map<Point, MicrobeGui> microbesGui = new HashMap<>();
    private static final int BOARD_SIZE = 7;
    private static final int CELL_SIZE = 60;
    private AgentContainer mainContainer;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Board window = new Board();
                window.initialize();  // Inicializa primeiro
                window.frame.setVisible(true);  // Depois torna visível
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Board() {
        initializeJADE();
    }

    private void initializeJADE() {
        try {
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.GUI, "true");
            mainContainer = jade.core.Runtime.instance().createMainContainer(profile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Erro ao iniciar JADE: " + e.getMessage(), 
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initialize() {
        frame = new JFrame("Infection Game");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                cell.setBackground((row + col) % 2 == 0 ? 
                    new Color(240, 240, 240) : new Color(220, 220, 220));
                boardPanel.add(cell);
            }
        }

        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start Game");
        startButton.addActionListener(this::startGame);
        controlPanel.add(startButton);

        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void startGame(ActionEvent e) {
        microbesGui.clear();
        clearBoard();
        
        try {
            // Cria agentes e passa a referência do boardPanel
            Object[] blue1Args = {this, boardPanel, MicrobeColor.BLUE, new Point(0, 0)};
            createMicrobeAgent("blue1", "agent.Microbe", blue1Args);
            
            Object[] blue2Args = {this, boardPanel, MicrobeColor.BLUE, new Point(6, 6)};
            createMicrobeAgent("blue2", "agent.Microbe", blue2Args);
            
            Object[] red1Args = {this, boardPanel, MicrobeColor.RED, new Point(0, 6)};
            createMicrobeAgent("red1", "agent.Microbe", red1Args);
            
            Object[] red2Args = {this, boardPanel, MicrobeColor.RED, new Point(6, 0)};
            createMicrobeAgent("red2", "agent.Microbe", red2Args);
            
        } catch (StaleProxyException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error creating agents: " + ex.getMessage());
        }
        
        startButton.setEnabled(false);
    }

    private void createMicrobeAgent(String name, String className, Object[] args) 
            throws StaleProxyException {
        AgentController ac = mainContainer.createNewAgent(name, className, args);
        ac.start();
    }
    
    public void updateMicrobePosition(String agentName, MicrobeColor color, Point newPosition) {
        SwingUtilities.invokeLater(() -> {
            // Remove da posição antiga
            microbesGui.entrySet().removeIf(entry -> 
                entry.getValue().getAgentName().equals(agentName));
            
            // Adiciona à nova posição
            JPanel cell = (JPanel) boardPanel.getComponent(
                newPosition.x * BOARD_SIZE + newPosition.y);
            cell.removeAll();
            
            MicrobeGui microbeGui = new MicrobeGui(agentName, color, newPosition);
            microbesGui.put(newPosition, microbeGui);
            cell.add(microbeGui);
            
            cell.revalidate();
            cell.repaint();
        });
    }


    private void clearBoard() {
        for (Component comp : boardPanel.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).removeAll();
                ((JPanel) comp).revalidate();
                ((JPanel) comp).repaint();
            }
        }
    }
}