package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;

public interface OrderStatusObserver {
    void onStatusChanged(Order order, String event);
}
