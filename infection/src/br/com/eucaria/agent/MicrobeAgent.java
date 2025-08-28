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
                    // Adiciona um comportamento de tiro único para decidir e agir
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
                state = ACTIVE; // Tenta de novo no próximo tick se o manager não foi encontrado
                return;
            }

            ACLMessage request = new ACLMessage(ACLMessage.QUERY_REF);
            request.addReceiver(managerAID);

            request.setContent(this.myAgent.getAID().getLocalName());
            send(request);

            // 2. Esperar pela resposta com o estado do board
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage reply = myAgent.blockingReceive(mt, 200);

            if (reply != null) {
                try {
                    Board currentBoard = (Board) reply.getContentObject();
                    Move chosenMove = decideMove(currentBoard);

                    if (chosenMove != null) {
                        informActionToManager(chosenMove);
                        // A energia já foi deduzida no decideMove
                    } else {
                        // Se não tem movimento, agenda despertar para tentar de novo
                        // O estado pode ter sido mudado para PAUSED dentro de decideMove
                        addBehaviour(new WakerBehaviour(myAgent, 500) {
                            @Override protected void onWake() { state = ACTIVE; }
                        });
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                    state = ACTIVE; // Reativa em caso de erro
                }
            } else {
                // Não obteve percepção, reativa para tentar de novo
                state = ACTIVE;
            }
        }
    }

    private void updateInternalState(Board perception) {
        int allyCount = 0;
        int enemyCount = 0;

        // O agente "olha" ao seu redor (raio de 1) para sentir o ambiente
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                AID neighbor = perception.getMicrobeAt(this.x + j, this.y + i);
                if (neighbor != null) {
                    // Precisamos saber a cor do vizinho. A percepção nos dá isso.
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
            // Se estiver isolado, a agressividade e coesão tendem a um valor neutro
            this.aggressiveness = 0.5;
            this.colonyCohesion = 0.5;
        }

        // Regenera energia se não estiver no máximo
        if (this.energy < MAX_ENERGY) {
            this.energy += ENERGY_REGEN_RATE;
        }
    }

    private Move decideMove(Board perception) {
        // 1. Atualiza o estado interno com base no que vê
        updateInternalState(perception);

        // 2. Lógica de decisão baseada no estado interno
        if (this.energy < COPY_COST) {
            // Se não tem energia para o mais barato, espera.
            state = PAUSED; // Pausa para regenerar
            return null;
        }

        List<Move> copyMoves = findPossibleMoves(perception, MoveType.COPY);
        List<Move> jumpMoves = findPossibleMoves(perception, MoveType.JUMP);

        // Decisão estratégica:
        // Se está muito isolado (baixa coesão), a prioridade é JUMP para perto de aliados
        if (this.colonyCohesion < 0.3 && this.energy >= JUMP_COST && !jumpMoves.isEmpty()) {
            System.out.println(getLocalName() + " está isolado, tentando SALTAR.");
            this.energy -= JUMP_COST; // <<<--- ADICIONE ESTA LINHA
            return findBestInfectionMove(jumpMoves, perception);
        }

        // Se está em uma zona de conflito (alta agressividade), a prioridade é COPY para expandir
        if (this.aggressiveness > 0.6 && this.energy >= COPY_COST && !copyMoves.isEmpty()) {
            System.out.println(getLocalName() + " está agressivo, tentando COPIAR.");
            this.energy -= COPY_COST; // Deduz a energia
            return findBestInfectionMove(copyMoves, perception);
        }

        // Comportamento padrão: se tiver energia, tenta o melhor movimento de cópia
        if (this.energy >= COPY_COST && !copyMoves.isEmpty()){
            this.energy -= COPY_COST;
            return findBestInfectionMove(copyMoves, perception);
        }

        // Se nada mais for possível, fica parado
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
        // 1. Crie um "template" do serviço que você está procurando
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment-manager"); // Procura por qualquer agente que ofereça este tipo de serviço
        template.addServices(sd);

        try {
            // 2. Peça ao DF para procurar por agentes que correspondam ao template
            DFAgentDescription[] result = DFService.search(this, template);

            if (result.length > 0) {
                // 3. Encontrou! Guarda o AID do primeiro agente encontrado
                managerAID = result[0].getName();
                System.out.println("Agente " + getLocalName() + " encontrou o Manager: " + managerAID.getLocalName());

                // 4. Agora que encontrou, envia a mensagem de registro correta
                ACLMessage registerMsg = new ACLMessage(ACLMessage.INFORM);
                registerMsg.addReceiver(managerAID);
                try {
                    // Cria o objeto MicrobeInfo com o estado inicial do agente
                    MicrobeInfo initialState = new MicrobeInfo(
                            getAID(),
                            MicrobeStatusEnum.CREATED, // Ou o status inicial que preferir
                            this.color,
                            this.x,
                            this.y,
                            Instant.now()
                    );
                    // Define o objeto como conteúdo da mensagem
                    registerMsg.setContentObject(initialState);
                    send(registerMsg); // Efetivamente envia a mensagem

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
        // Usamos um protocolo para que o Manager possa filtrar facilmente essas mensagens
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
                        // O movimento foi bem-sucedido, o Manager nos envia a nova posição
                        String[] newPos = msg.getContent().split(":");
                        updatePosition(Integer.parseInt(newPos[0]), Integer.parseInt(newPos[1]));
                        break;

                    case ACLMessage.REJECT_PROPOSAL:
                        // Ação falhou, o agente tentará de novo no próximo tick ativo.
                        // Não precisamos fazer nada, o estado continua PAUSED até o WakerBehaviour o ativar.
                        break;

                    case ACLMessage.INFORM:
                        if ("GAME_OVER".equals(msg.getContent())) {
                            isAlive = false; // Sinaliza para o TickerBehaviour parar e o agente morrer
                        } else {
                            // Se não for fim de jogo, assume que é uma notificação de infecção
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