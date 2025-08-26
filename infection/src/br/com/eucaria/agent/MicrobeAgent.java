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
            // 1. Pedir percepção ao Manager
            ACLMessage request = new ACLMessage(ACLMessage.QUERY_REF);
            request.addReceiver(managerAID);
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
                    } else {
                        // Se não tem movimento, agenda despertar para tentar de novo
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

    private Move decideMove(Board board) {
        List<Move> copyMoves = findPossibleMoves(board, MoveType.COPY);
        List<Move> jumpMoves = findPossibleMoves(board, MoveType.JUMP);

        int bestJumpInfections = 0;
        Move bestJumpMove = null;
        for (Move jump : jumpMoves) {
            int infections = board.countPotentialInfections(jump.toX(), jump.toY(), this.color);
            if (infections > bestJumpInfections) {
                bestJumpInfections = infections;
                bestJumpMove = jump;
            }
        }

        int bestCopyInfections = 0;
        Move bestCopyMove = null;
        if (!copyMoves.isEmpty()) {
            bestCopyMove = copyMoves.get(0);
            bestCopyInfections = board.countPotentialInfections(bestCopyMove.toX(), bestCopyMove.toY(), this.color);
        }

        if (bestJumpMove != null && bestJumpInfections > (bestCopyInfections + 2)) {
            return bestJumpMove;
        }
        return bestCopyMove;
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


    // Dentro da classe MicrobeAgent.java

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
                // Você pode adicionar uma lógica para tentar novamente mais tarde
                // Por exemplo, adicionando um WakerBehaviour para chamar este método de novo.
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Este método vai dentro da classe MicrobeAgent

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