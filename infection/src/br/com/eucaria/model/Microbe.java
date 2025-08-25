package br.com.eucaria.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Representa o agente Micróbio.
 * É um agente reativo que segue uma arquitetura de subsunção para decidir suas ações.
 */
public class Microbe {

    // Registra uma ação planejada: tipo (Cópia ou Pulo) e coordenadas de destino.
    public record Move(MoveType type, int toX, int toY) {}
    public enum MoveType { COPY, JUMP }

    private final int x;
    private final int y;
    private final StatusEnum color;
    private final Random random = new Random();

    public Microbe(int x, int y, StatusEnum color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public StatusEnum getColor() { return color; }

    /**
     * Lógica principal de decisão do agente, baseada na Arquitetura de Subsunção.
     * Camada 0: Ataque (infectar oponentes)
     * Camada 1: Expansão (copiar-se)
     * Camada 2: Reposicionamento (pular)
     * Camada 3: Ação nula (ficar parado)
     *
     * @param board O ambiente que o micróbio percebe.
     * @return A ação escolhida (Move) ou null se nenhuma ação for possível.
     */
    public Move decideAction(Board board) {
        // --- Camada 0: Ação de Ataque/Infecção (maior prioridade) ---
        Move bestAttackMove = findBestInfectionMove(board);
        if (bestAttackMove != null) {
            return bestAttackMove;
        }

        // --- Camada 1: Ação de Expansão (Cópia) ---
        List<Move> copyMoves = findPossibleMoves(board, MoveType.COPY, 1);
        if (!copyMoves.isEmpty()) {
            return copyMoves.get(random.nextInt(copyMoves.size())); // Escolhe um movimento de cópia aleatório
        }

        // --- Camada 2: Ação de Reposicionamento (Pulo) ---
        List<Move> jumpMoves = findPossibleMoves(board, MoveType.JUMP, 2);
        if (!jumpMoves.isEmpty()) {
            return jumpMoves.get(random.nextInt(jumpMoves.size())); // Escolhe um pulo aleatório
        }

        // --- Camada 3: Nenhuma ação possível ---
        return null;
    }

    /**
     * Camada 0: Avalia todos os movimentos possíveis (cópia e pulo) para encontrar
     * aquele que converte o maior número de oponentes.
     */
    private Move findBestInfectionMove(Board board) {
        List<Move> allPossibleMoves = new ArrayList<>();
        allPossibleMoves.addAll(findPossibleMoves(board, MoveType.COPY, 1));
        allPossibleMoves.addAll(findPossibleMoves(board, MoveType.JUMP, 2));

        Move bestMove = null;
        int maxInfections = 0;

        for (Move move : allPossibleMoves) {
            int infections = board.countPotentialInfections(move.toX, move.toY, this.color);
            if (infections > maxInfections) {
                maxInfections = infections;
                bestMove = move;
            }
        }

        // Só retorna o movimento se ele de fato infectar alguém.
        return maxInfections > 0 ? bestMove : null;
    }

    /**
     * Encontra todas as células vazias alcançáveis para um determinado tipo de movimento.
     */
    private List<Move> findPossibleMoves(Board board, MoveType type, int distance) {
        List<Move> moves = new ArrayList<>();
        for (int i = -distance; i <= distance; i++) {
            for (int j = -distance; j <= distance; j++) {
                if (i == 0 && j == 0) continue;

                int targetX = this.x + i;
                int targetY = this.y + j;

                if (!board.isOutOfBounds(targetX, targetY) && board.getMicrobeAt(targetX, targetY) == null) {
                    moves.add(new Move(type, targetX, targetY));
                }
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        return "Microbe{" + "color=" + color + ", x=" + x + ", y=" + y + '}';
    }
}