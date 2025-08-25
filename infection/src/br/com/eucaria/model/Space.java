package br.com.eucaria.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um espaço ou célula individual no tabuleiro.
 * Cada espaço pode estar vazio ou ser ocupado por um micróbio.
 */
public class Space {

    // 1. Record para armazenar a mudança de estado
    public record StateChange(int tick, StatusEnum status) {
        @Override
        public String toString() {
            return String.format("Tick %d: %s", tick, status);
        }
    }

    private final List<StateChange> history;

    private Microbe microbe;

    public Space() {
        this.microbe = null;
        this.history = Collections.synchronizedList(new ArrayList<>());
        // Registra o estado inicial no tick 0
        recordStateChange(0, StatusEnum.EMPTY);
    }

    public List<StateChange> getHistory() {
        return history;
    }

    public Microbe getMicrobe() {
        return microbe;
    }

    public void setMicrobe(Microbe microbe, int tick) {
        this.microbe = microbe;
        recordStateChange(tick, getStatus());
    }

    public void clear(int tick) {
        this.microbe = null;
        recordStateChange(tick, StatusEnum.EMPTY);
    }

    public boolean isOccupied() {
        return this.microbe != null;
    }

    private void recordStateChange(int tick, StatusEnum newStatus) {
        // Para otimizar, só adiciona se o estado realmente mudou
        if (history.isEmpty() || history.get(history.size() - 1).status() != newStatus) {
            history.add(new StateChange(tick, newStatus));
        }
    }

    /**
     * Retorna o status da célula (VAZIO, VERMELHO ou AZUL) com base
     * na presença e cor do micróbio.
     * @return O StatusEnum correspondente.
     */
    public StatusEnum getStatus() {
        if (!isOccupied()) {
            return StatusEnum.EMPTY;
        }
        return microbe.getColor();
    }

    /**
     * Limpa a célula, removendo qualquer micróbio.
     */
    public void clear() {
        this.microbe = null;
    }
}