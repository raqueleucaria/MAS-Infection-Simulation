package br.com.eucaria.agent;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.Space;
import br.com.eucaria.model.StatusEnum;
import br.com.eucaria.service.EnvironmentService;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.wrapper.ControllerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SimulationManagerAgent extends Agent {

    private static final String OUTPUT_DIRECTORY = "output/";

    @Override
    protected void setup() {
        System.out.println("Agente SimulationManager " + getLocalName() + " iniciado e monitorando o jogo.");

        addBehaviour(new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {
                Board board = EnvironmentService.getInstance().getBoard();
                int redCount = 0;
                int blueCount = 0;

                for (int y = 0; y < Board.SIZE; y++) {
                    for (int x = 0; x < Board.SIZE; x++) {
                        if (board.getMicrobeAt(x, y) != null) {
                            if (board.getMicrobeAt(x, y).getColor() == StatusEnum.RED) redCount++;
                            else blueCount++;
                        }
                    }
                }

                boolean isGameOver = (redCount > 0 && blueCount == 0) ||
                        (blueCount > 0 && redCount == 0) ||
                        (redCount + blueCount == Board.SIZE * Board.SIZE);

                if (isGameOver) {
                    System.out.println("\n" + StatusEnum.ANSI_GREEN + "### Fim da Simulação (Detectado pelo Manager) ###" + StatusEnum.ANSI_RESET);
                    if (redCount > blueCount) System.out.println("VENCEDOR: VERMELHO");
                    else if (blueCount > redCount) System.out.println("VENCEDOR: AZUL");
                    else System.out.println("EMPATE");

                    EnvironmentService.getInstance().setGameOver();
                    saveHistoryToCSV(board, OUTPUT_DIRECTORY + "historico_simulacao.csv");
                    stop();
                    shutdownPlatform();
                }
            }
        });
    }

    private void shutdownPlatform() {
        try {
            getContainerController().getPlatformController().kill();
        } catch (ControllerException e) {
            System.err.println("Erro ao tentar desligar a plataforma JADE:");
            e.printStackTrace();
        }
    }

    private void saveHistoryToCSV(Board board, String filePath) {
        try {
            File outputDir = new File(OUTPUT_DIRECTORY);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("tick,x,y,status\n");
                for (int y = 0; y < Board.SIZE; y++) {
                    for (int x = 0; x < Board.SIZE; x++) {
                        Space space = board.getSpaceAt(x, y);
                        for (Space.StateChange change : space.getHistory()) {
                            String line = String.format("%d,%d,%d,%s\n",
                                    change.tick(), x, y, change.status().name());
                            writer.write(line);
                        }
                    }
                }
                System.out.println("\n" + StatusEnum.ANSI_GREEN + "Histórico da simulação salvo com sucesso em: " + filePath + StatusEnum.ANSI_RESET);
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo de histórico: " + e.getMessage());
            e.printStackTrace();
        }
    }
}