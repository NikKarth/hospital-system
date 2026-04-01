package oop.hospital_order_system.command;

import oop.hospital_order_system.manager.OrderManager;

public class CancelOrderCommand implements Command {
    private final String orderId;
    private final String actor;

    public CancelOrderCommand(String orderId, String actor) {
        this.orderId = orderId;
        this.actor = actor;
    }

    @Override
    public void execute(OrderManager manager) {
        manager.cancelOrder(orderId, actor);
    }

    @Override
    public String getType() {
        return "CANCEL";
    }

    @Override
    public String getOrderId() {
        return orderId;
    }

    @Override
    public String getActor() {
        return actor;
    }
}
