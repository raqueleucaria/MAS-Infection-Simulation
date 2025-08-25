package br.com.eucaria.model;

/**
 * Representa o ambiente da simulação, um tabuleiro 7x7.
 * Agora gerencia uma grade de objetos 'Space'.
 */
public class Board {
    public static final int SIZE = 7;
    // A grade agora é de objetos Space
    private final Space[][] grid;

    public Board() {
        this.grid = new Space[SIZE][SIZE];
        // Inicializa cada célula com um objeto Space vazio
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Space();
            }
        }
    }

    public Space getSpaceAt(int x, int y) {
        if (isOutOfBounds(x, y)) return null;
        return grid[y][x];
    }

    public Microbe getMicrobeAt(int x, int y) {
        Space space = getSpaceAt(x, y);
        return (space != null) ? space.getMicrobe() : null;
    }

    public void placeMicrobe(Microbe microbe, int tick) {
        Space space = getSpaceAt(microbe.getX(), microbe.getY());
        if (space != null) {
            space.setMicrobe(microbe, tick);
        }
    }

    /**
     * Executa um movimento, interagindo com os objetos Space.
     */
    public void executeMove(Microbe microbe, Microbe.Move move, int tick) {
        if (move.type() == Microbe.MoveType.JUMP) {
            getSpaceAt(microbe.getX(), microbe.getY()).clear(tick);
        }
        Microbe newMicrobe = new Microbe(move.toX(), move.toY(), microbe.getColor());
        placeMicrobe(newMicrobe, tick);
    }

    /**
     * Aplica a infecção, agora verificando o status através do Space.
     */
    public void applyInfection(int x, int y, StatusEnum attackerColor, int tick) {
        StatusEnum opponentColor = StatusEnum.getOpponent(attackerColor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                int neighborX = x + i;
                int neighborY = y + j;

                Space neighborSpace = getSpaceAt(neighborX, neighborY);
                if (neighborSpace != null && neighborSpace.getStatus() == opponentColor) {
                    Microbe convertedMicrobe = new Microbe(neighborX, neighborY, attackerColor);
                    // Passa o tick ao setar o novo micróbio
                    neighborSpace.setMicrobe(convertedMicrobe, tick);
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

    // Dentro da classe br.com.eucaria.model.Board

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  0 1 2 3 4 5 6\n");
        for (int i = 0; i < SIZE; i++) {
            sb.append(i).append(" ");
            for (int j = 0; j < SIZE; j++) {
                // Pega o status da célula
                StatusEnum status = grid[i][j].getStatus();

                // Constrói a string com a cor: CÓDIGO_DA_COR + CARACTERE + RESET_DA_COR
                sb.append(status.getColorCode())
                        .append(status.getRepresentation())
                        .append(StatusEnum.ANSI_RESET) // Reseta a cor para o padrão
                        .append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}