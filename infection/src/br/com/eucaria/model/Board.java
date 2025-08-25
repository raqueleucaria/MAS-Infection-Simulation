package br.com.eucaria.model;

import br.com.eucaria.agent.MicrobeAgent;

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

    public void applyInfection(int x, int y, StatusEnum attackerColor, int tick) {
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int neighborX = x + i;
                int neighborY = y + j;

                MicrobeAgent neighbor = getMicrobeAt(neighborX, neighborY);
                if (neighbor != null && neighbor.getColor() == opponentColor) {
                    neighbor.beConverted(attackerColor, tick);
                }
            }
        }
    }

    // Demais métodos (getSpaceAt, getMicrobeAt, countPotentialInfections, isOutOfBounds, toString)
    // permanecem como na versão anterior. Adiciono removeMicrobe para consistência.

    public Space getSpaceAt(int x, int y) {
        if (isOutOfBounds(x, y)) return null;
        return grid[y][x];
    }

    public MicrobeAgent getMicrobeAt(int x, int y) {
        Space space = getSpaceAt(x, y);
        return (space != null) ? space.getMicrobe() : null;
    }

    public void removeMicrobe(int x, int y) {
        Space space = getSpaceAt(x, y);
        if (space != null) {
            // O tick idealmente seria passado aqui se a remoção precisasse ser historiada
            // space.clear(tick);
        }
    }

    public int countPotentialInfections(int x, int y, StatusEnum attackerColor) {
        int count = 0;
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                Space neighborSpace = getSpaceAt(x + i, y + j);
                if (neighborSpace != null && neighborSpace.getStatus() == opponentColor) {
                    count++;
                }
            }
        }
        return count;
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