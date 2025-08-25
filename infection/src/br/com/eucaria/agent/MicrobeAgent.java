package br.com.eucaria.agent;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.StatusEnum;
import br.com.eucaria.service.EnvironmentService;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MicrobeAgent extends Agent {

    public record Move(MoveType type, int toX, int toY) {}
    public enum MoveType { COPY, JUMP }

    private int x;
    private int y;
    private StatusEnum color;
    private final Random random = new Random();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            this.x = (int) args[0];
            this.y = (int) args[1];
            this.color = (StatusEnum) args[2];
            EnvironmentService.getInstance().placeMicrobe(this, this.x, this.y, 0);
            System.out.println("Agente " + getLocalName() + " nascido em (" + x + "," + y + ")");
        } else {
            doDelete();
            return;
        }

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (EnvironmentService.getInstance().isGameOver()) {
                    stop();
                    myAgent.doDelete();
                    return;
                }
                Move chosenMove = decideAction();
                if (chosenMove != null) {
                    boolean moved = EnvironmentService.getInstance().tryExecuteMove(myAgent, chosenMove, getTickCount());
                    if (moved) {
                        ((MicrobeAgent) myAgent).updatePosition(chosenMove.toX, chosenMove.toY);
                    }
                }
            }
        });
    }

    /**
     * MÉTODO CORRIGIDO: Agora, apenas o estado interno do agente é alterado.
     * A responsabilidade de atualizar o tabuleiro fica com quem chamou o método.
     */
    public void beConverted(StatusEnum newColor) {
        System.out.println("Agente " + getLocalName() + " foi convertido de " + this.color + " para " + newColor);
        this.color = newColor;
    }

    public void updatePosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    private Move decideAction() {
        Board currentBoard = EnvironmentService.getInstance().getBoard();
        Move bestAttackMove = findBestInfectionMove(currentBoard);
        if (bestAttackMove != null) return bestAttackMove;
        List<Move> copyMoves = findPossibleMoves(currentBoard, MoveType.COPY, 1);
        if (!copyMoves.isEmpty()) return copyMoves.get(random.nextInt(copyMoves.size()));
        List<Move> jumpMoves = findPossibleMoves(currentBoard, MoveType.JUMP, 2);
        if (!jumpMoves.isEmpty()) return jumpMoves.get(random.nextInt(jumpMoves.size()));
        return null;
    }

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
        return maxInfections > 0 ? bestMove : null;
    }

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

    public int getX() { return x; }
    public int getY() { return y; }
    public StatusEnum getColor() { return color; }

    @Override
    protected void takeDown() {
        EnvironmentService.getInstance().removeMicrobe(this);
        System.out.println("Agente " + getLocalName() + " morrendo.");
    }
}