package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;

public interface NotificationService extends OrderStatusObserver {
    void notify(Order order, String event);

    @Override
    default void onStatusChanged(Order order, String event) {
        notify(order, event);
    }
}
