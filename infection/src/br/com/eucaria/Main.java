package br.com.eucaria;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.Microbe;
import br.com.eucaria.model.StatusEnum;
import br.com.eucaria.util.BoardTemplate;

import java.util.*;

public class Main {
    private static final int MAX_TICKS = 100;
    private static final int TICK_DELAY_MS = 500; // Meio segundo por tick

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
            // Cada agente decide sua ação com base no estado atual do tabuleiro.
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

                // Conflito: se a célula alvo já foi reivindicada neste tick, a ação falha.
                if (!targetCells.contains(target)) {
                    targetCells.add(target);
                    board.executeMove(microbe, move);
                    // Guarda o micróbio na sua nova posição para a fase de infecção
                    movedMicrobes.add(new Microbe(move.toX(), move.toY(), microbe.getColor()));
                }
            }

            // --- Fase 3: Infecção ---
            // A infecção é aplicada com base no estado do tabuleiro APÓS todos se moverem.
            for (Microbe movedMicrobe : movedMicrobes) {
                board.applyInfection(movedMicrobe.getX(), movedMicrobe.getY(), movedMicrobe.getColor());
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
}