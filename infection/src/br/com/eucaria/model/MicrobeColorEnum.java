package br.com.eucaria.model;

public enum MicrobeColorEnum {
    RED("R"),
    BLUE("B"),
    EMPTY("-");

    private final String representation;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    MicrobeColorEnum(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return switch (this) {
            case RED -> ANSI_RED + this.representation + ANSI_RESET;
            case BLUE -> ANSI_BLUE + this.representation + ANSI_RESET;
            default -> this.representation;
        };
    }

    public static MicrobeColorEnum getOpponent(MicrobeColorEnum color) {
        return (color == RED) ? BLUE : RED;
    }
}