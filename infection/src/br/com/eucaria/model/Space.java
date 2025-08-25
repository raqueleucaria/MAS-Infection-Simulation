package br.com.eucaria.model;

import br.com.eucaria.agent.MicrobeAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Space {

    public record StateChange(int tick, StatusEnum status) {
        @Override
        public String toString() {
            return String.format("Tick %d: %s", tick, status);
        }
    }

    private MicrobeAgent microbeAgent;
    private final List<StateChange> history;

    public Space() {
        this.microbeAgent = null;
        this.history = Collections.synchronizedList(new ArrayList<>());
        recordStateChange(0, StatusEnum.EMPTY);
    }

    public List<StateChange> getHistory() {
        return history;
    }

    public void setMicrobe(MicrobeAgent microbeAgent, int tick) {
        this.microbeAgent = microbeAgent;
        recordStateChange(tick, getStatus());
    }

    public void clear(int tick) {
        this.microbeAgent = null;
        recordStateChange(tick, StatusEnum.EMPTY);
    }

    private void recordStateChange(int tick, StatusEnum newStatus) {
        if (history.isEmpty() || history.get(history.size() - 1).status() != newStatus) {
            history.add(new StateChange(tick, newStatus));
        }
    }

    public MicrobeAgent getMicrobe() {
        return this.microbeAgent;
    }

    public boolean isOccupied() {
        return this.microbeAgent != null;
    }

    public StatusEnum getStatus() {
        if (!isOccupied()) {
            return StatusEnum.EMPTY;
        }
        return microbeAgent.getColor();
    }
}