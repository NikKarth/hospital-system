package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Priority;

import java.util.Comparator;

public class PriorityFirstTriageStrategy implements TriageStrategy {

    @Override
    public Comparator<Order> getComparator() {
        return Comparator
                .comparing((Order o) -> { 
                    if (o.getPriority() == Priority.STAT) return 0;
                    if (o.getPriority() == Priority.URGENT) return 1;
                    return 2;
                })
                .thenComparing(Order::getSubmittedAt);
    }
}
