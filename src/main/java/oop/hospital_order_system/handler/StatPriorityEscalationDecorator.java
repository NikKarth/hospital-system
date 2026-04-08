package oop.hospital_order_system.handler;

import oop.hospital_order_system.access.OrderAccess;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Priority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class StatPriorityEscalationDecorator extends OrderHandlerDecorator {
    private final OrderAccess orderAccess;
    private final Clock clock;
    private final Duration escalationWindow;

    public StatPriorityEscalationDecorator(OrderHandler inner, OrderAccess orderAccess, Clock clock, Duration escalationWindow) {
        super(inner);
        this.orderAccess = orderAccess;
        this.clock = clock;
        this.escalationWindow = escalationWindow;
    }

    @Override
    public void handle(Order order) {
        if (order.getPriority() == Priority.URGENT) {
            Instant now = Instant.now(clock);
            Instant cutoff = now.minus(escalationWindow);
            String description = normalizeDescription(order.getDescription());
            for (Order candidate : orderAccess.listAllOrders()) {
                if (candidate.getType() != order.getType()) {
                    continue;
                }
                if (candidate.getPriority() != Priority.STAT) {
                    continue;
                }
                if (!normalizeDescription(candidate.getDescription()).equals(description)) {
                    continue;
                }
                if (candidate.getSubmittedAt().isBefore(cutoff) || candidate.getSubmittedAt().isAfter(now)) {
                    continue;
                }

                order.setPriority(Priority.STAT);
                break;
            }
        }
        inner.handle(order);
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim().toLowerCase(Locale.ROOT);
    }
}
