package br.com.eucaria.model;

import jade.core.AID;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Space implements Serializable {

    public record StateChange(int tick, MicrobeColorEnum color) implements Serializable {}

    private AID microbeAID;
    private MicrobeColorEnum color;
    private final List<StateChange> history;

    public Space() {
        this.microbeAID = null;
        this.color = MicrobeColorEnum.EMPTY;
        this.history = Collections.synchronizedList(new ArrayList<>());
        recordStateChange(0, MicrobeColorEnum.EMPTY);
    }

    // Construtor de cópia
    public Space(Space other) {
        this.microbeAID = other.microbeAID;
        this.color = other.color;
        this.history = Collections.synchronizedList(new ArrayList<>(other.history));
    }

    public void setMicrobe(AID microbeAID, MicrobeColorEnum color) {
        this.microbeAID = microbeAID;
        this.color = color;
    }

    public void clear() {
        this.microbeAID = null;
        this.color = MicrobeColorEnum.EMPTY;
    }

    private void recordStateChange(int tick, MicrobeColorEnum newStatus) {
        if (history.isEmpty() || history.get(history.size() - 1).color() != newStatus) {
            history.add(new StateChange(tick, newStatus));
        }
    }

    public AID getMicrobeAID() {
        return this.microbeAID;
    }

    public boolean isOccupied() {
        return this.microbeAID != null;
    }

    public MicrobeColorEnum getColor() {
        return this.color;
    }

    public List<StateChange> getHistory() {
        return this.history;
    }

    public void setColor(MicrobeColorEnum color) {
        this.color = color;
    }
}