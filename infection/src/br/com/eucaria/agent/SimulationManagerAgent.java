package br.com.eucaria.agent;

import br.com.eucaria.ui.custom.MainFrame;
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
import jade.lang.acl.UnreadableException;
import jade.wrapper.StaleProxyException;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import static br.com.eucaria.ui.custom.MainFrame.BLUE_MICROBE_COLOR;
import static br.com.eucaria.ui.custom.MainFrame.EMPTY_SPACE_BACKGROUND;
import static br.com.eucaria.ui.custom.MainFrame.RED_MICROBE_COLOR;

public class SimulationManagerAgent extends Agent {
    private static final System.Logger LOGGER = System.getLogger(SimulationManagerAgent.class.getName());
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String HISTORY_FILE_NAME = "simulation_history.json";

    private final Board board = new Board();
    private final Vector<MicrobeInfo> eventLog = new Vector<>();
    private int tickCount = 0;
    private boolean isGameOver = false;

    private MainFrame gui;

    @Override
    protected void setup() {
        LOGGER.log(System.Logger.Level.INFO, "Ambiente ({0}) iniciado.", getLocalName());
        registerService();

        gui = new MainFrame(Board.SIZE);
        gui.setVisible(true);

        addBehaviour(new HandleMicrobeMessagesBehaviour());
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                tickCount++;
                updateGUI();
                if (isGameOver) {
                    stop();
                }
            }
        });
    }

    private void updateGUI() {
        if (gui == null) return;

        Vector<Color> colors = new Vector<>();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                MicrobeColorEnum colorEnum = board.getColorAt(j, i);
                switch (colorEnum) {
                    case RED:
                        colors.add(RED_MICROBE_COLOR);
                        break;
                    case BLUE:
                        colors.add(BLUE_MICROBE_COLOR);
                        break;
                    case EMPTY:
                    default:
                        colors.add(EMPTY_SPACE_BACKGROUND);
                        break;
                }
            }
        }
        gui.updatePanel(colors);
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

        reply.setConversationId(msg.getConversationId());

        String agentName = msg.getContent();
        AID agentAID = new AID(agentName, AID.ISLOCALNAME);

        Board localView = this.board.getLocalPerception(agentAID);
        reply.setContentObject(localView);

        send(reply);
    }

    private synchronized void handleActionProposal(ACLMessage msg) throws UnreadableException, StaleProxyException {
        if (isGameOver) return;

        AID microbeAID = msg.getSender();
        MicrobeAgent.Move move = (MicrobeAgent.Move) msg.getContentObject();

        boolean success = board.getMicrobeAt(move.toX(), move.toY()) == null;
        ACLMessage reply = msg.createReply();

        if (success) {
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            reply.setContent(move.toX() + ":" + move.toY() + ":" + move.type());

            MicrobeInfo currentState = board.getMicrobeInfo(microbeAID);
            if (currentState == null) return;

            if (move.type() == MicrobeAgent.MoveType.COPY) {
                createNewMicrobe(move.toX(), move.toY(), currentState.color());
            } else {
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
        updateGUI();
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
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment-manager");
        sd.setName("Infection-World-Service");

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            LOGGER.log(System.Logger.Level.INFO, "Serviço de ambiente registrado com sucesso no DF.");
        } catch (FIPAException fe) {
            LOGGER.log(System.Logger.Level.ERROR, "Erro ao registrar serviço no DF.", fe);
        }
    }

    private void saveHistoryToJson(String filePath) {

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