package br.com.eucaria.service;

import br.com.eucaria.agent.MicrobeAgent;
import br.com.eucaria.model.Board;
import jade.core.Agent;

public class EnvironmentService {
    private static EnvironmentService instance;
    private final Board board;
    private volatile boolean isGameOver = false;

    private EnvironmentService() {
        this.board = new Board();
    }

    public static synchronized EnvironmentService getInstance() {
        if (instance == null) {
            instance = new EnvironmentService();
        }
        return instance;
    }

    public void setGameOver() {
        this.isGameOver = true;
    }

    public boolean isGameOver() {
        return this.isGameOver;
    }

    public synchronized void placeMicrobe(MicrobeAgent agent, int x, int y, int tick) {
        board.placeMicrobe(agent, x, y, tick);
    }

    public synchronized void removeMicrobe(MicrobeAgent agent) {
        // O tick de remoção seria o tick atual do agente que o remove,
        // mas para simplificar, a lógica de 'morrer' foi simplificada.
        // Se a remoção precisasse de um tick, ele seria passado aqui.
        board.removeMicrobe(agent.getX(), agent.getY());
    }

    public synchronized boolean tryExecuteMove(Agent agent, MicrobeAgent.Move move, int tick) {
        if (!(agent instanceof MicrobeAgent microbe)) {
            return false;
        }
        if (board.getMicrobeAt(move.toX(), move.toY()) != null) {
            return false;
        }

        board.executeMove(microbe, move, tick);
        board.applyInfection(move.toX(), move.toY(), microbe.getColor(), tick);

        System.out.println("----------------------------------------");
        System.out.println("Tick " + tick + ": Agente " + microbe.getLocalName() + " moveu para (" + move.toX() + "," + move.toY() + ")");
        System.out.println(board);
        return true;
    }

    public Board getBoard() {
        return this.board;
    }
}