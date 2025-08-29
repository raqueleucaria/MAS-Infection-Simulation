package br.com.eucaria.agent;

import br.com.eucaria.model.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static br.com.eucaria.model.MicrobeStatusEnum.ACTIVE;
import static br.com.eucaria.model.MicrobeStatusEnum.PAUSED;

public class MicrobeAgent extends Agent {

    public record Move(MoveType type, int fromX, int fromY, int toX, int toY) implements Serializable {}
    public enum MoveType { COPY, JUMP }

    private int x, y;
    private MicrobeColorEnum color;
    private MicrobeStatusEnum state = PAUSED;
    private AID managerAID;
    private final Random random = new Random();
    private boolean isAlive = true;

    private double aggressiveness = 0.5; // Inicia neutro (0.0 a 1.0)
    private int energy = 100; // Energia máxima inicial
    private double colonyCohesion = 0.5; // Coesão inicial neutra

    private static final int MAX_ENERGY = 100;
    private static final int ENERGY_REGEN_RATE = 5;
    private static final int COPY_COST = 10;
    private static final int JUMP_COST = 25;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            this.x = (int) args[0];
            this.y = (int) args[1];
            this.color = (MicrobeColorEnum) args[2];
        }

        SequentialBehaviour startupSequence = new SequentialBehaviour(this);
        startupSequence.addSubBehaviour(new WakerBehaviour(this, 500) {
            @Override protected void onWake() {}
        });
        startupSequence.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                findAndRegisterWithManager();
                state = ACTIVE;
            }
        });

        startupSequence.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (!isAlive) {
                    stop();
                    myAgent.doDelete();
                    return;
                }
                if (state == ACTIVE) {
                    myAgent.addBehaviour(new DecideAndActBehaviour());
                    state = PAUSED;
                }
            }
        });
        addBehaviour(startupSequence);
        addBehaviour(new ListenForManagerCommands());
    }

    private class DecideAndActBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            if (managerAID == null) {
                state = ACTIVE;
                return;
            }

            String conversationId = "perception-" + myAgent.getLocalName() + "-" + System.currentTimeMillis();

            ACLMessage request = new ACLMessage(ACLMessage.QUERY_REF);
            request.addReceiver(managerAID);
            request.setContent(this.myAgent.getAID().getLocalName());
            request.setConversationId(conversationId);
            send(request);

            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId(conversationId)
            );
            ACLMessage reply = myAgent.blockingReceive(mt, 200);

            if (reply != null) {
                try {
                    Board currentBoard = (Board) reply.getContentObject();
                    Move chosenMove = decideMove(currentBoard);

                    if (chosenMove != null) {
                        informActionToManager(chosenMove);
                    } else {
                        addBehaviour(new WakerBehaviour(myAgent, 500) {
                            @Override protected void onWake() { state = ACTIVE; }
                        });
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                    state = ACTIVE;
                }
            } else {
                state = ACTIVE;
            }
        }
    }

    private void updateInternalState(Board perception) {
        int allyCount = 0;
        int enemyCount = 0;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                AID neighbor = perception.getMicrobeAt(this.x + j, this.y + i);
                if (neighbor != null) {
                    MicrobeInfo neighborInfo = perception.getMicrobeInfo(neighbor);
                    if (neighborInfo.color() == this.color) {
                        allyCount++;
                    } else {
                        enemyCount++;
                    }
                }
            }
        }

        int totalNeighbors = allyCount + enemyCount;
        if (totalNeighbors > 0) {
            this.colonyCohesion = (double) allyCount / totalNeighbors;
            this.aggressiveness = (double) enemyCount / totalNeighbors;
        } else {
            this.aggressiveness = 0.5;
            this.colonyCohesion = 0.5;
        }

        if (this.energy < MAX_ENERGY) {
            this.energy += ENERGY_REGEN_RATE;
        }
    }

    private Move decideMove(Board perception) {
        updateInternalState(perception);

        if (this.energy < COPY_COST) {
            state = PAUSED;
            return null;
        }

        List<Move> copyMoves = findPossibleMoves(perception, MoveType.COPY);
        List<Move> jumpMoves = findPossibleMoves(perception, MoveType.JUMP);

        if (this.colonyCohesion < 0.3 && this.energy >= JUMP_COST && !jumpMoves.isEmpty()) {
            System.out.println(getLocalName() + " está isolado, tentando SALTAR.");
            this.energy -= JUMP_COST; // <<<--- ADICIONE ESTA LINHA
            return findBestInfectionMove(jumpMoves, perception);
        }

        if (this.aggressiveness > 0.6 && this.energy >= COPY_COST && !copyMoves.isEmpty()) {
            System.out.println(getLocalName() + " está agressivo, tentando COPIAR.");
            this.energy -= COPY_COST;
            return findBestInfectionMove(copyMoves, perception);
        }

        if (this.energy >= COPY_COST && !copyMoves.isEmpty()){
            this.energy -= COPY_COST;
            return findBestInfectionMove(copyMoves, perception);
        }

        return null;
    }

    private Move findBestInfectionMove(List<Move> moves, Board board) {
        Move bestMove = null;
        int maxInfections = -1;

        for (Move move : moves) {
            int potentialInfections = board.countPotentialInfections(move.toX(), move.toY(), this.color);
            if (potentialInfections > maxInfections) {
                maxInfections = potentialInfections;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private List<Move> findPossibleMoves(Board board, MoveType type) {
        List<Move> moves = new ArrayList<>();
        int d = (type == MoveType.COPY) ? 1 : 2;

        for (int i = -d; i <= d; i++) {
            for (int j = -d; j <= d; j++) {
                if (i == 0 && j == 0) continue;
                if (type == MoveType.COPY && (Math.abs(i) > 1 || Math.abs(j) > 1)) continue;
                if (type == MoveType.JUMP && (Math.abs(i) < 2 && Math.abs(j) < 2)) continue;

                int targetX = this.x + j;
                int targetY = this.y + i;
                if (!board.isOutOfBounds(targetX, targetY) && board.getMicrobeAt(targetX, targetY) == null) {
                    moves.add(new Move(type, this.x, this.y, targetX, targetY));
                }
            }
        }
        return moves;
    }

    public void updatePosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public void beConverted(MicrobeColorEnum newColor) {
        System.out.println("Agente " + getLocalName() + " foi convertido de " + this.color + " para " + newColor);
        this.color = newColor;
        this.state = PAUSED;
        addBehaviour(new WakerBehaviour(this, 1000) {
            @Override protected void onWake() { state = ACTIVE; }
        });
    }
    @Override
    protected void takeDown() {
        System.out.println("Agente " + getLocalName() + " morrendo.");
    }


    private void findAndRegisterWithManager() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment-manager");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);

            if (result.length > 0) {
                managerAID = result[0].getName();
                System.out.println("Agente " + getLocalName() + " encontrou o Manager: " + managerAID.getLocalName());

                ACLMessage registerMsg = new ACLMessage(ACLMessage.INFORM);
                registerMsg.addReceiver(managerAID);
                try {
                    MicrobeInfo initialState = new MicrobeInfo(
                            getAID(),
                            MicrobeStatusEnum.CREATED,
                            this.color,
                            this.x,
                            this.y,
                            Instant.now()
                    );
                    registerMsg.setContentObject(initialState);
                    send(registerMsg);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                System.out.println("Agente " + getLocalName() + " não encontrou o Manager. Tentando novamente...");

            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void informActionToManager(Move move) {
        if (managerAID == null) {
            System.out.println("ERRO: Agente " + getLocalName() + " tentou agir sem encontrar o Manager.");
            return;
        }

        ACLMessage proposeMsg = new ACLMessage(ACLMessage.PROPOSE);
        proposeMsg.addReceiver(managerAID);
        proposeMsg.setProtocol("propose-action");
        try {
            proposeMsg.setContentObject(move);
            send(proposeMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ListenForManagerCommands extends CyclicBehaviour {
        @Override
        public void action() {
            if (managerAID == null) {
                block(100);
                return;
            }
            MessageTemplate mt = MessageTemplate.MatchSender(managerAID);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.ACCEPT_PROPOSAL:
                        String[] newPos = msg.getContent().split(":");
                        updatePosition(Integer.parseInt(newPos[0]), Integer.parseInt(newPos[1]));
                        break;

                    case ACLMessage.REJECT_PROPOSAL:
                        break;

                    case ACLMessage.INFORM:
                        if ("GAME_OVER".equals(msg.getContent())) {
                            isAlive = false;
                        } else {
                            MicrobeColorEnum newColor = MicrobeColorEnum.valueOf(msg.getContent());
                            beConverted(newColor);
                        }
                        break;
                }
            } else {
                block();
            }
        }
    }
}