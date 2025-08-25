package br.com.eucaria.model;

/**
 * Enum que representa o estado de uma célula no tabuleiro.
 * Cada estado possui um caractere associado para representação visual.
 */
public enum StatusEnum {
    EMPTY('-'),
    RED('R'),
    BLUE('B');

    private final char representation;

    /**
     * Construtor para associar um caractere a cada estado.
     * @param representation O caractere visual (e.g., 'R' para RED).
     */
    StatusEnum(final char representation) {
        this.representation = representation;
    }

    /**
     * @return O caractere que representa o estado no tabuleiro.
     */
    public char getRepresentation() {
        return representation;
    }

    /**
     * Retorna a cor do oponente.
     * @param color A cor atual.
     * @return A cor oposta.
     */
    public static StatusEnum getOpponent(StatusEnum color) {
        if (color == RED) {
            return BLUE;
        } else if (color == BLUE) {
            return RED;
        }
        return EMPTY; // Não há oponente para uma célula vazia.
    }
}