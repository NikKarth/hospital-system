package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

public class LoadBalancingTriageStrategy implements TriageStrategy {
    private final Supplier<Map<OrderType, Long>> inProgressCountSupplier;

    public LoadBalancingTriageStrategy(Supplier<Map<OrderType, Long>> inProgressCountSupplier) {
        this.inProgressCountSupplier = inProgressCountSupplier;
    }

    @Override
    public Comparator<Order> getComparator() {
        Map<OrderType, Long> loadByType = inProgressCountSupplier.get();
        return Comparator
                .comparingLong((Order order) -> loadByType.getOrDefault(order.getType(), 0L))
                .thenComparing(Order::getSubmittedAt);
    }
}
