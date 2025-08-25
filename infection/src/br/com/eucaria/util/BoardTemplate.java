package br.com.eucaria.util;

import br.com.eucaria.model.Board;
import br.com.eucaria.model.Microbe;
import br.com.eucaria.model.StatusEnum;

/**
 * Classe utilitária para gerar tabuleiros com configurações iniciais.
 */
public class BoardTemplate {

    /**
     * Cria um tabuleiro com a configuração padrão do jogo "Infection":
     * dois micróbios de cada cor nos cantos.
     * @return um Board pronto para o início da simulação.
     */
    public static Board createDefaultBoard() {
        Board board = new Board();
        int initialTick = 0; // O estado inicial é no tick 0

        // Micróbios Azuis
        board.placeMicrobe(new Microbe(0, 0, StatusEnum.BLUE), initialTick);
        board.placeMicrobe(new Microbe(6, 6, StatusEnum.BLUE), initialTick);

        // Micróbios Vermelhos
        board.placeMicrobe(new Microbe(6, 0, StatusEnum.RED), initialTick);
        board.placeMicrobe(new Microbe(0, 6, StatusEnum.RED), initialTick);

        return board;
    }
}