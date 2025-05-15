package agent;

import view.Board;
import javax.swing.JPanel;
import java.awt.Point;
import jade.core.Agent;
import view.MicrobeColor;

public class Microbe extends Agent {
    private Board board;
    private JPanel boardPanel;
    private MicrobeColor color;
    private Point position;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 4) {
            this.board = (Board) args[0];
            this.boardPanel = (JPanel) args[1];
            this.color = (MicrobeColor) args[2];
            
            if (args[3] instanceof Point) {
                this.position = (Point) args[3];
            }
            board.updateMicrobePosition(getLocalName(), color, position);
            System.out.println(color+" "+position);
        }
 
    }
    
    // Getters
    public Board getBoard() { return board; }
    public JPanel getBoardPanel() { return boardPanel; }
    public MicrobeColor getColor() { return color; }
    public Point getPosition() { return position; }
    
    public void setPosition(Point newPosition) {
        this.position = newPosition;

    }
}