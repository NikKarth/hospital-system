package oop.hospital_order_system.handler;

import oop.hospital_order_system.access.OrderAccess;
import oop.hospital_order_system.domain.Order;

public class BasicOrderHandler implements OrderHandler {
    private final OrderAccess orderAccess;

    public BasicOrderHandler(OrderAccess orderAccess) {
        this.orderAccess = orderAccess;
    }

    @Override
    public void handle(Order order) {
        orderAccess.saveOrder(order);
    }
}
