package oop.hospital_order_system.command;

import oop.hospital_order_system.manager.OrderManager;

public interface Command {
    void execute(OrderManager manager);
    String getType();
    String getOrderId();
    String getActor();
}
