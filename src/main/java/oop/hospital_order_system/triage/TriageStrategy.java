package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.Order;

import java.util.Comparator;

public interface TriageStrategy {
    Comparator<Order> getComparator();
}
