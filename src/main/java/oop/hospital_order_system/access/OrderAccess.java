package oop.hospital_order_system.access;

import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderAccess {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public void saveOrder(Order order) {
        store.put(order.getId(), order);
    }

    public Optional<Order> findOrderById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Order> listAllOrders() {
        return new ArrayList<>(store.values());
    }

    public List<Order> listOrdersByStatus(Status status) {
        return store.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> listPendingOrders() {
        return listOrdersByStatus(Status.PENDING);
    }

    public List<Order> listInProgressOrders() {
        return listOrdersByStatus(Status.IN_PROGRESS);
    }

    public void deleteOrder(String id) {
        store.remove(id);
    }

    public boolean orderExists(String id) {
        return store.containsKey(id);
    }

    public List<Order> listOrdersSorted(java.util.Comparator<Order> comparator) {
        List<Order> orders = new ArrayList<>(store.values());
        orders.sort(comparator);
        return orders;
    }

    public List<Order> findPendingForClaim() {
        return store.values().stream()
                .filter(order -> order.getStatus() == Status.PENDING)
                .collect(Collectors.toList());
    }
}
