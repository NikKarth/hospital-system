package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;

public abstract class NotificationDecorator implements NotificationService {
    protected final NotificationService inner;

    protected NotificationDecorator(NotificationService inner) {
        this.inner = inner;
    }

    @Override
    public void notify(Order order, String event) {
        inner.notify(order, event);
    }
}
