package oop.hospital_order_system.handler;

import oop.hospital_order_system.domain.Order;

public abstract class OrderHandlerDecorator implements OrderHandler {
    protected final OrderHandler inner;

    protected OrderHandlerDecorator(OrderHandler inner) {
        this.inner = inner;
    }

    @Override
    public void handle(Order order) {
        inner.handle(order);
    }
}
