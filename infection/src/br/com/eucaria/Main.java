package br.com.eucaria;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.Microbe;
import br.com.eucaria.model.Space;
import br.com.eucaria.model.StatusEnum;
import br.com.eucaria.util.BoardTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final int MAX_TICKS = 100;
    private static final int TICK_DELAY_MS = 500; // Meio segundo por tick
    private static final String OUTPUT_DIRECTORY = "output/";

    public static void main(String[] args) {
        Board board = BoardTemplate.createDefaultBoard();
        System.out.println("### INFECTION-SMA: Início da Simulação ###");
        System.out.println("Tick 0:");
        System.out.println(board);

        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            System.out.println("----------------------------------------");
            System.out.println("Tick " + tick + ":");

            // Coleta todos os agentes do tabuleiro
            List<Microbe> allMicrobes = getAllMicrobes(board);
            Collections.shuffle(allMicrobes); // Aleatoriza a ordem de decisão

            // --- Fase 1: Percepção e Decisão ---
            Map<Microbe, Microbe.Move> plannedMoves = new LinkedHashMap<>();
            for (Microbe microbe : allMicrobes) {
                Microbe.Move move = microbe.decideAction(board);
                if (move != null) {
                    plannedMoves.put(microbe, move);
                }
            }

            // --- Fase 2: Execução e Resolução de Conflitos ---
            Set<String> targetCells = new HashSet<>();
            List<Microbe> movedMicrobes = new ArrayList<>();

            for (Map.Entry<Microbe, Microbe.Move> entry : plannedMoves.entrySet()) {
                Microbe microbe = entry.getKey();
                Microbe.Move move = entry.getValue();
                String target = move.toX() + "," + move.toY();

                if (!targetCells.contains(target)) {
                    targetCells.add(target);
                    board.executeMove(microbe, move, tick);
                    movedMicrobes.add(new Microbe(move.toX(), move.toY(), microbe.getColor()));
                }
            }

            // --- Fase 3: Infecção ---
            for (Microbe movedMicrobe : movedMicrobes) {
                board.applyInfection(movedMicrobe.getX(), movedMicrobe.getY(), movedMicrobe.getColor(), tick);
            }

            // Imprime o estado atual
            System.out.println(board);

            // Contagem para análise
            int redCount = 0;
            int blueCount = 0;
            for (Microbe m : getAllMicrobes(board)) {
                if (m.getColor() == StatusEnum.RED) redCount++;
                else blueCount++;
            }
            System.out.printf("Placar: Vermelhos (R) = %d | Azuis (B) = %d%n", redCount, blueCount);

            // --- Condição de Término ---
            if (redCount == 0 || blueCount == 0 || (redCount + blueCount == Board.SIZE * Board.SIZE)) {
                System.out.println("\n### Fim da Simulação ###");
                if (redCount > blueCount) System.out.println("VENCEDOR: VERMELHO");
                else if (blueCount > redCount) System.out.println("VENCEDOR: AZUL");
                else System.out.println("EMPATE");
                break;
            }

            if (tick == MAX_TICKS) {
                System.out.println("\n### Fim da Simulação (Limite de Ticks Atingido) ###");
            }

            // Pausa para visualização
            try {
                Thread.sleep(TICK_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

//        // --- Análise dos Dados no Final da Simulação (Console) ---
//        System.out.println("\n### Análise Histórica dos Espaços ###");
//        for (int y = 0; y < Board.SIZE; y++) {
//            for (int x = 0; x < Board.SIZE; x++) {
//                Space space = board.getSpaceAt(x, y);
//                if (space.getHistory().size() > 1) {
//                    System.out.printf("Histórico para a célula (%d, %d): %s\n", x, y, space.getHistory());
//                }
//            }
//        }

        // --- Salvar dados em arquivo CSV ---
        saveHistoryToCSV(board, OUTPUT_DIRECTORY + "historico_simulacao.csv");
    }

    /**
     * Varre o tabuleiro e retorna uma lista com todos os micróbios existentes.
     */
    private static List<Microbe> getAllMicrobes(Board board) {
        List<Microbe> microbes = new ArrayList<>();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (board.getMicrobeAt(x, y) != null) {
                    microbes.add(board.getMicrobeAt(x, y));
                }
            }
        }
        return microbes;
    }

    /**
     * Salva o histórico completo da simulação em um arquivo CSV.
     * O arquivo terá as colunas: tick,x,y,status
     *
     * @param board    O tabuleiro contendo todos os espaços e seus históricos.
     * @param filePath O caminho completo do arquivo a ser criado (ex: "output/historico_simulacao.csv").
     */
    private static void saveHistoryToCSV(Board board, String filePath) {
        try {
            // Garante que o diretório de saída exista
            File outputDir = new File(OUTPUT_DIRECTORY);
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // mkdirs() cria diretórios pais se necessário
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                // Escreve o cabeçalho do CSV
                writer.write("tick,x,y,status\n");

                // Itera por cada célula do tabuleiro
                for (int y = 0; y < Board.SIZE; y++) {
                    for (int x = 0; x < Board.SIZE; x++) {
                        Space space = board.getSpaceAt(x, y);
                        // Itera por cada mudança de estado no histórico da célula
                        for (Space.StateChange change : space.getHistory()) {
                            String line = String.format("%d,%d,%d,%s\n",
                                    change.tick(), x, y, change.status().name());
                            writer.write(line);
                        }
                    }
                }
                // Adicionado código de cor verde para a mensagem de sucesso
                System.out.println("\n" + StatusEnum.ANSI_GREEN + "Histórico da simulação salvo com sucesso em: " + filePath + StatusEnum.ANSI_RESET);

            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo de histórico: " + e.getMessage());
            e.printStackTrace();
        }
    }
}