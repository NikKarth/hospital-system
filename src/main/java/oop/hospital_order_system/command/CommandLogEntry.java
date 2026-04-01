package oop.hospital_order_system.command;

import java.time.Instant;

public class CommandLogEntry {
    private final Instant when;
    private final String type;
    private final String orderId;
    private final String actor;

    public CommandLogEntry(String type, String orderId, String actor) {
        this.when = Instant.now();
        this.type = type;
        this.orderId = orderId;
        this.actor = actor;
    }

    public Instant getWhen() {
        return when;
    }

    public String getType() {
        return type;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getActor() {
        return actor;
    }
}
