package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.Order;

import java.util.Comparator;

public class TriagingEngine {
    private final TriageStrategy triageStrategy;

    public TriagingEngine(TriageStrategy triageStrategy) {
        this.triageStrategy = triageStrategy;
    }

    public Comparator<Order> getComparator() {
        return triageStrategy.getComparator();
    }
}
