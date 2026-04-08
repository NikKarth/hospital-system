package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

public class DeadlineFirstTriageStrategy implements TriageStrategy {
    private final Clock clock;

    public DeadlineFirstTriageStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Comparator<Order> getComparator() {
        return Comparator
                .comparing((Order order) -> timeToDeadline(order, Instant.now(clock)))
                .thenComparing(Order::getSubmittedAt);
    }

    private long timeToDeadline(Order order, Instant now) {
        Instant deadline = order.getSubmittedAt().plus(resolveDeadline(order.getType(), order.getPriority()));
        return Duration.between(now, deadline).toMillis();
    }

    public static Duration resolveDeadline(OrderType type, Priority priority) {
        if (type == OrderType.LAB && priority == Priority.STAT) {
            return Duration.ofMinutes(30);
        }
        if (priority == Priority.STAT) {
            return Duration.ofMinutes(45);
        }
        if (priority == Priority.URGENT) {
            return Duration.ofHours(2);
        }
        return Duration.ofHours(6);
    }
}
