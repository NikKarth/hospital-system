package oop.hospital_order_system.command;

import oop.hospital_order_system.manager.OrderManager;

public class CompleteOrderCommand implements Command {
    private final String orderId;
    private final String actor;

    public CompleteOrderCommand(String orderId, String actor) {
        this.orderId = orderId;
        this.actor = actor;
    }

    @Override
    public void execute(OrderManager manager) {
        manager.completeOrder(orderId, actor);
    }

    @Override
    public String getType() {
        return "COMPLETE";
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
