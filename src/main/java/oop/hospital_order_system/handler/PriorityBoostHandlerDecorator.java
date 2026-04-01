package oop.hospital_order_system.handler;

import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Priority;

public class PriorityBoostHandlerDecorator extends OrderHandlerDecorator {
    public PriorityBoostHandlerDecorator(OrderHandler inner) {
        super(inner);
    }

    @Override
    public void handle(Order order) {
        if (order.getPriority() == Priority.ROUTINE && order.getDescription() != null && order.getDescription().toLowerCase().contains("urgent")) {
            order.setPriority(Priority.URGENT);
        }
        inner.handle(order);
    }
}
