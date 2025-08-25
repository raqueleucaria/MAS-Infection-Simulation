package br.com.eucaria.model;

/**
 * Enum que representa o estado de uma célula no tabuleiro.
 * Cada estado possui um caractere e um código de cor ANSI associado.
 */
public enum StatusEnum {
    // Adicionamos o código de cor ao construtor
    EMPTY('-', StatusEnum.ANSI_RESET),
    RED('R', StatusEnum.ANSI_RED),
    BLUE('B', StatusEnum.ANSI_BLUE);

    // --- Novas constantes para as cores ANSI ---
    public static final String ANSI_RESET = "\u001b[0m";
    public static final String ANSI_RED = "\u001b[31m";
    public static final String ANSI_BLUE = "\u001b[34m";
    public static final String ANSI_GREEN = "\u001b[32m";
    // -------------------------------------------

    private final char representation;
    private final String colorCode; // Novo atributo

    /**
     * Construtor atualizado para associar um caractere e uma cor.
     * @param representation O caractere visual (e.g., 'R' para RED).
     * @param colorCode O código de escape ANSI para a cor.
     */
    StatusEnum(final char representation, final String colorCode) {
        this.representation = representation;
        this.colorCode = colorCode;
    }

    public char getRepresentation() {
        return representation;
    }

    // Novo getter para o código de cor
    public String getColorCode() {
        return colorCode;
    }

    public static StatusEnum getOpponent(StatusEnum color) {
        if (color == RED) {
            return BLUE;
        } else if (color == BLUE) {
            return RED;
        }
        return EMPTY;
    }
}