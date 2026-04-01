package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderStatusSubject {
    private final List<OrderStatusObserver> observers = Collections.synchronizedList(new ArrayList<>());

    public void registerObserver(OrderStatusObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(OrderStatusObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Order order, String event) {
        for (OrderStatusObserver observer : new ArrayList<>(observers)) {
            observer.onStatusChanged(order, event);
        }
    }
}
