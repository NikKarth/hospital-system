package oop.hospital_order_system.handler;

import oop.hospital_order_system.domain.Order;

public interface OrderHandler {
    void handle(Order order);
}
