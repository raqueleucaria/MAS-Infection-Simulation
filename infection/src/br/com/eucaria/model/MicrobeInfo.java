package br.com.eucaria.model;

import jade.core.AID;

import java.io.Serializable;
import java.time.Instant;

public record MicrobeInfo(
        AID aid,
        MicrobeStatusEnum status,
        MicrobeColorEnum color,
        int x,
        int y,
        Instant timestamp
) implements Serializable {
    @Override
    public String toString() {
        String formattedTimestamp = timestamp.toString();

        return String.format(
                "{ " +
                        "\"aid\": \"%s\", " +
                        "\"status\": \"%s\", " +
                        "\"color\": \"%s\", " +
                        "\"position\": { \"x\": %d, \"y\": %d }, " +
                        "\"timestamp\": \"%s\"" +
                        " }",
                aid.getLocalName(),
                status,
                color,
                x,
                y,
                formattedTimestamp
        );
    }
}
