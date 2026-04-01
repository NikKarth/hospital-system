package oop.hospital_order_system.handler;

import oop.hospital_order_system.domain.Order;

public class ValidationOrderHandlerDecorator extends OrderHandlerDecorator {

    public ValidationOrderHandlerDecorator(OrderHandler inner) {
        super(inner);
    }

    @Override
    public void handle(Order order) {
        if (order.getPatientName() == null || order.getPatientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Patient name is required");
        }
        if (order.getClinician() == null || order.getClinician().trim().isEmpty()) {
            throw new IllegalArgumentException("Clinician is required");
        }
        inner.handle(order);
    }
}
