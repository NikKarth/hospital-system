package oop.hospital_order_system.handler;

import oop.hospital_order_system.access.OrderAccess;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
        if (order.getPriority() == Priority.STAT) {
            Instant cutoff = Instant.now(clock).minus(escalationWindow);
            List<Order> candidates = orderAccess.listAllOrders();
            for (Order candidate : candidates) {
                if (candidate.getType() == order.getType()
                        && candidate.getPriority() == Priority.URGENT
                        && candidate.getStatus() == Status.PENDING
                        && !candidate.getSubmittedAt().isBefore(cutoff)) {
                    candidate.setPriority(Priority.STAT);
                    orderAccess.saveOrder(candidate);
                }
            }
        }
        inner.handle(order);
    }
}
