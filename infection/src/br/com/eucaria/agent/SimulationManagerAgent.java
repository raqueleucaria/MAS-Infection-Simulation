package br.com.eucaria.agent;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.MicrobeColorEnum;
import br.com.eucaria.model.MicrobeInfo;
import br.com.eucaria.model.MicrobeStatusEnum;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.StaleProxyException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

public class SimulationManagerAgent extends Agent {
    private static final System.Logger LOGGER = System.getLogger(SimulationManagerAgent.class.getName());
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String HISTORY_FILE_NAME = "simulation_history.json";

    private final Board board = new Board();
    private final Vector<MicrobeInfo> eventLog = new Vector<>();
    private int tickCount = 0;
    private boolean isGameOver = false;

    @Override
    protected void setup() {
        LOGGER.log(System.Logger.Level.INFO, "Ambiente ({0}) iniciado.", getLocalName());
        registerService();
        addBehaviour(new HandleMicrobeMessagesBehaviour());
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                tickCount++;
                if (isGameOver) {
                    stop();
                }
            }
        });
    }

    private class HandleMicrobeMessagesBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                try {
                    switch (msg.getPerformative()) {
                        case ACLMessage.INFORM: // Usado para registro
                            if (msg.getContentObject() instanceof MicrobeInfo) {
                                handleRegistration((MicrobeInfo) msg.getContentObject());
                            }
                            break;
                        case ACLMessage.QUERY_REF: // Pedido de percepção
                            handlePerceptionRequest(msg);
                            break;
                        case ACLMessage.PROPOSE: // Proposta de ação
                            if (msg.getContentObject() instanceof MicrobeAgent.Move) {
                                handleActionProposal(msg);
                            }
                            break;
                    }
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.ERROR, "Erro ao processar mensagem.", e);
                }
            } else {
                block();
            }
        }
    }

    private void handleRegistration(MicrobeInfo info) {
        board.placeMicrobe(info.aid(), info.x(), info.y(), info.color());
        eventLog.add(info);
        LOGGER.log(System.Logger.Level.INFO, "AMBIENTE: Agente {0} registrado.", info.aid().getLocalName());
    }

    private void handlePerceptionRequest(ACLMessage msg) throws IOException {
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        // Envia uma cópia do board para o agente ter sua percepção
        reply.setContentObject(new Board(this.board));
        send(reply);
    }

    private synchronized void handleActionProposal(ACLMessage msg) throws UnreadableException, StaleProxyException {
        if (isGameOver) return;

        AID microbeAID = msg.getSender();
        MicrobeAgent.Move move = (MicrobeAgent.Move) msg.getContentObject();

        // Valida se a célula de destino está livre
        boolean success = board.getMicrobeAt(move.toX(), move.toY()) == null;
        ACLMessage reply = msg.createReply();

        if (success) {
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            reply.setContent(move.toX() + ":" + move.toY() + ":" + move.type());

            MicrobeInfo currentState = board.getMicrobeInfo(microbeAID);
            if (currentState == null) return; // Agente fantasma

            if (move.type() == MicrobeAgent.MoveType.COPY) {
                // A célula de origem não muda, apenas a nova é criada
                createNewMicrobe(move.toX(), move.toY(), currentState.color());
            } else { // JUMP
                board.removeMicrobe(move.fromX(), move.fromY());
                board.placeMicrobe(microbeAID, move.toX(), move.toY(), currentState.color());
                MicrobeInfo jumpEvent = new MicrobeInfo(microbeAID, MicrobeStatusEnum.JUMPED, currentState.color(), move.toX(), move.toY(), Instant.now());
                eventLog.add(jumpEvent);
            }

            List<AID> infectedAIDs = board.applyInfection(move.toX(), move.toY(), currentState.color());
            for (AID infectedAID : infectedAIDs) {
                MicrobeInfo oldState = board.getMicrobeInfo(infectedAID);
                MicrobeInfo infectedEvent = new MicrobeInfo(infectedAID, MicrobeStatusEnum.INFECTED, currentState.color(), oldState.x(), oldState.y(), Instant.now());
                eventLog.add(infectedEvent);

                ACLMessage infectionOrder = new ACLMessage(ACLMessage.INFORM);
                infectionOrder.addReceiver(infectedAID);
                infectionOrder.setContent(currentState.color().name());
                send(infectionOrder);
            }

            LOGGER.log(System.Logger.Level.INFO, "----------------------------------------");
            LOGGER.log(System.Logger.Level.INFO, "Tick {0}: {1} realizou {2} para ({3},{4})", new Object[]{tickCount, microbeAID.getLocalName(), move.type(), move.toX(), move.toY()});
            System.out.println(board);
        } else {
            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
            reply.setContent("MOVE_FAILED");
        }
        send(reply);
        checkGameOver();
    }

    private void createNewMicrobe(int x, int y, MicrobeColorEnum color) throws StaleProxyException {
        String agentName = UUID.randomUUID().toString();
        Object[] args = {x, y, color};
        getContainerController().createNewAgent(agentName, "br.com.eucaria.agent.MicrobeAgent", args).start();
    }

    private void checkGameOver() {
        int redCount = board.countMicrobes(MicrobeColorEnum.RED);
        int blueCount = board.countMicrobes(MicrobeColorEnum.BLUE);
        int totalCount = redCount + blueCount;

        if (!isGameOver && ((redCount > 0 && blueCount == 0) || (blueCount > 0 && redCount == 0) || (totalCount >= Board.SIZE * Board.SIZE))) {
            isGameOver = true;
            LOGGER.log(System.Logger.Level.INFO, "### Fim da Simulação (Detectado pelo Manager) ###");
            LOGGER.log(System.Logger.Level.INFO, "Placar Final: VERMELHO ({0}) x AZUL ({1})", redCount, blueCount);

            if (redCount > blueCount) LOGGER.log(System.Logger.Level.INFO, "VENCEDOR: VERMELHO");
            else if (blueCount > redCount) LOGGER.log(System.Logger.Level.INFO, "VENCEDOR: AZUL");
            else LOGGER.log(System.Logger.Level.INFO, "EMPATE");

            ACLMessage gameOverMsg = new ACLMessage(ACLMessage.INFORM);
            gameOverMsg.setContent("GAME_OVER");
            List<AID> allMicrobes = board.getAllMicrobeAIDs();
            for (AID aid : allMicrobes) {
                gameOverMsg.addReceiver(aid);
            }
            send(gameOverMsg);
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        LOGGER.log(System.Logger.Level.INFO, "Desligando SimulationManager. Salvando histórico...");
        saveHistoryToJson(OUTPUT_DIRECTORY + HISTORY_FILE_NAME);
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }


    private void registerService() {
        // 1. Crie uma "descrição" do seu agente
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID()); // Diz que o agente que oferece o serviço é ele mesmo

        // 2. Crie uma "descrição" do serviço oferecido
        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment-manager"); // Um "tipo" único para que outros possam encontrar
        sd.setName("Infection-World-Service"); // Um nome legível para o serviço

        // 3. Adicione o serviço à descrição do agente
        dfd.addServices(sd);

        try {
            // 4. Efetivamente, registre tudo nas Páginas Amarelas
            DFService.register(this, dfd);
            LOGGER.log(System.Logger.Level.INFO, "Serviço de ambiente registrado com sucesso no DF.");
        } catch (FIPAException fe) {
            LOGGER.log(System.Logger.Level.ERROR, "Erro ao registrar serviço no DF.", fe);
        }
    }

    private void saveHistoryToJson(String filePath) {
        // ... (código para criar diretório)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("[\n");
            var iterator = eventLog.iterator();
            while (iterator.hasNext()) {
                writer.write("  " + iterator.next().toString());
                if (iterator.hasNext()) {
                    writer.write(",\n");
                }
            }
            writer.write("\n]");
            LOGGER.log(System.Logger.Level.INFO, "Histórico da simulação salvo com sucesso em: {0}", filePath);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Erro ao salvar o arquivo de histórico JSON.", e);
        }
    }
}