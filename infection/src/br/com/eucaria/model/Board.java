package br.com.eucaria.model;

/**
 * Representa o ambiente da simulação, um tabuleiro 7x7.
 * Gerencia o estado e as interações dos micróbios.
 */
public class Board {
    public static final int SIZE = 7;
    private final Microbe[][] grid;

    public Board() {
        this.grid = new Microbe[SIZE][SIZE];
    }

    public Microbe getMicrobeAt(int x, int y) {
        if (isOutOfBounds(x, y)) return null;
        return grid[y][x];
    }

    public void placeMicrobe(Microbe microbe) {
        if (!isOutOfBounds(microbe.getX(), microbe.getY())) {
            grid[microbe.getY()][microbe.getX()] = microbe;
        }
    }

    /**
     * Executa um movimento, movendo um micróbio de uma célula para outra.
     * Para movimentos de CÓPIA, o original permanece.
     * Para movimentos de PULO, o original é removido.
     */
    public void executeMove(Microbe microbe, Microbe.Move move) {
        if (move.type() == Microbe.MoveType.JUMP) {
            grid[microbe.getY()][microbe.getX()] = null; // Remove da posição original
        }
        Microbe newMicrobe = new Microbe(move.toX(), move.toY(), microbe.getColor());
        placeMicrobe(newMicrobe);
    }

    /**
     * Após um movimento, converte todos os oponentes adjacentes.
     * Este é o mecanismo de "infecção".
     */
    public void applyInfection(int x, int y, StatusEnum attackerColor) {
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                int neighborX = x + i;
                int neighborY = y + j;

                Microbe neighbor = getMicrobeAt(neighborX, neighborY);
                if (neighbor != null && neighbor.getColor() == opponentColor) {
                    // Converte o oponente
                    Microbe convertedMicrobe = new Microbe(neighborX, neighborY, attackerColor);
                    placeMicrobe(convertedMicrobe);
                }
            }
        }
    }

    /**
     * Usado pela camada de decisão do micróbio para prever o resultado de um ataque.
     * @return O número de oponentes que seriam infectados.
     */
    public int countPotentialInfections(int x, int y, StatusEnum attackerColor) {
        int count = 0;
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                Microbe neighbor = getMicrobeAt(x + i, y + j);
                if (neighbor != null && neighbor.getColor() == opponentColor) {
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
                Microbe microbe = grid[i][j];
                // Lógica simplificada: pede a representação diretamente ao enum
                if (microbe == null) {
                    sb.append(StatusEnum.EMPTY.getRepresentation()).append(" ");
                } else {
                    sb.append(microbe.getColor().getRepresentation()).append(" ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}