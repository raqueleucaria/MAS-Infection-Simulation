package br.com.eucaria.model;

import br.com.eucaria.agent.MicrobeAgent;
import java.util.ArrayList;
import java.util.List;

public class Board {
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

    public void placeMicrobe(MicrobeAgent agent, int x, int y, int tick) {
        Space space = getSpaceAt(x, y);
        if (space != null) {
            space.setMicrobe(agent, tick);
        }
    }

    public void executeMove(MicrobeAgent microbe, MicrobeAgent.Move move, int tick) {
        if (move.type() == MicrobeAgent.MoveType.JUMP) {
            getSpaceAt(microbe.getX(), microbe.getY()).clear(tick);
        }
        placeMicrobe(microbe, move.toX(), move.toY(), tick);
    }

    /**
     * MÉTODO CORRIGIDO: Implementa a lógica de duas fases para evitar reação em cadeia.
     */
    public void applyInfection(int x, int y, StatusEnum attackerColor, int tick) {
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);

        // Fase 1: Identificar todos os vizinhos a serem convertidos e guardá-los em uma lista.
        List<MicrobeAgent> agentsToConvert = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                int neighborX = x + j;
                int neighborY = y + i;

                Space neighborSpace = getSpaceAt(neighborX, neighborY);
                if (neighborSpace != null && neighborSpace.isOccupied() && neighborSpace.getStatus() == opponentColor) {
                    agentsToConvert.add(neighborSpace.getMicrobe());
                }
            }
        }

        // Fase 2: Converter todos os agentes que foram identificados.
        for (MicrobeAgent neighbor : agentsToConvert) {
            neighbor.beConverted(attackerColor);
            // Atualiza o Space do agente convertido para registrar a mudança no histórico.
            getSpaceAt(neighbor.getX(), neighbor.getY()).setMicrobe(neighbor, tick);
        }
    }

    public int countPotentialInfections(int x, int y, StatusEnum attackerColor) {
        int count = 0;
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) { // y-offset
            for (int j = -1; j <= 1; j++) { // x-offset
                if (i == 0 && j == 0) continue;
                Space neighborSpace = getSpaceAt(x + j, y + i);
                if (neighborSpace != null && neighborSpace.getStatus() == opponentColor) {
                    count++;
                }
            }
        }
        return count;
    }

    // O restante da classe (getters, toString, etc.) permanece o mesmo...
    public Space getSpaceAt(int x, int y) {
        if (isOutOfBounds(x, y)) return null;
        return grid[y][x];
    }

    public MicrobeAgent getMicrobeAt(int x, int y) {
        Space space = getSpaceAt(x, y);
        return (space != null) ? space.getMicrobe() : null;
    }

    public void removeMicrobe(int x, int y) {
        // Este método não precisa ser alterado.
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
                StatusEnum status = grid[i][j].getStatus();
                sb.append(status.getColorCode())
                        .append(status.getRepresentation())
                        .append(StatusEnum.ANSI_RESET)
                        .append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}