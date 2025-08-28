package br.com.eucaria.model;

import br.com.eucaria.agent.MicrobeAgent;
import jade.core.AID;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Board implements Serializable {
    public static final int SIZE = 7;
    private final Space[][] grid;

    public Board() {
        this.grid = new Space[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Space();
            }
        }
    }

    // Construtor de cópia para a percepção do agente
    public Board(Board other) {
        this.grid = new Space[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                this.grid[i][j] = new Space(other.grid[i][j]);
            }
        }
    }

    public void placeMicrobe(AID agentID, int x, int y, MicrobeColorEnum color) {
        if (!isOutOfBounds(x, y)) {
            grid[y][x].setMicrobe(agentID, color);
        }
    }

    public void removeMicrobe(int x, int y) {
        if (!isOutOfBounds(x, y)) {
            grid[y][x].clear();
        }
    }

    public List<AID> applyInfection(int x, int y, MicrobeColorEnum attackerColor) {
        List<AID> infectedAIDs = new ArrayList<>();
        MicrobeColorEnum opponentColor = MicrobeColorEnum.getOpponent(attackerColor);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int neighborX = x + j;
                int neighborY = y + i;

                if (!isOutOfBounds(neighborX, neighborY)) {
                    Space neighborSpace = grid[neighborY][neighborX];
                    if (neighborSpace.isOccupied() && neighborSpace.getColor() == opponentColor) {
                        neighborSpace.setColor(attackerColor);
                        infectedAIDs.add(neighborSpace.getMicrobeAID());
                    }
                }
            }
        }
        return infectedAIDs;
    }

    public int countPotentialInfections(int x, int y, MicrobeColorEnum attackerColor) {
        int count = 0;
        MicrobeColorEnum opponentColor = MicrobeColorEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                if (!isOutOfBounds(x + j, y + i) && grid[y + i][x + j].getColor() == opponentColor) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countMicrobes(MicrobeColorEnum color) {
        int count = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j].isOccupied() && grid[i][j].getColor() == color) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<AID> getAllMicrobeAIDs() {
        List<AID> aids = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j].isOccupied()) {
                    aids.add(grid[i][j].getMicrobeAID());
                }
            }
        }
        return aids;
    }

    public MicrobeInfo getMicrobeInfo(AID agentAID) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j].isOccupied() && grid[i][j].getMicrobeAID().equals(agentAID)) {
                    return new MicrobeInfo(agentAID, null, grid[i][j].getColor(), j, i, null);
                }
            }
        }
        return null;
    }

    public AID getMicrobeAt(int x, int y) {
        if (isOutOfBounds(x, y)) return null;
        return grid[y][x].getMicrobeAID();
    }

    public Board getLocalPerception(AID agentAID) {
        MicrobeInfo agentInfo = getMicrobeInfo(agentAID);
        if (agentInfo == null) {
            return new Board(); // Retorna um board vazio se o agente não for encontrado
        }

        int centerX = agentInfo.x();
        int centerY = agentInfo.y();
        int perceptionRadius = 2; // O agente pode "ver" 2 células em cada direção

        Board perceptionBoard = new Board(); // Cria um novo board vazio

        for (int i = -perceptionRadius; i <= perceptionRadius; i++) {
            for (int j = -perceptionRadius; j <= perceptionRadius; j++) {
                int targetX = centerX + j;
                int targetY = centerY + i;

                if (!isOutOfBounds(targetX, targetY)) {
                    Space originalSpace = this.grid[targetY][targetX];
                    if (originalSpace.isOccupied()) {
                        // Copia a informação do micróbio para o novo board de percepção
                        perceptionBoard.placeMicrobe(
                                originalSpace.getMicrobeAID(),
                                targetX,
                                targetY,
                                originalSpace.getColor()
                        );
                    }
                }
            }
        }
        return perceptionBoard;
    }

    public boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= SIZE || y < 0 || y >= SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  0 1 2 3 4 5 6\n");
        for (int i = 0; i < SIZE; i++) {
            sb.append(i).append(" ");
            for (int j = 0; j < SIZE; j++) {
                sb.append(grid[i][j].getColor().getRepresentation()).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}