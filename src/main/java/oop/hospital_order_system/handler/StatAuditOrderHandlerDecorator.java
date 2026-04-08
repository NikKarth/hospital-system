package oop.hospital_order_system.handler;

import oop.hospital_order_system.access.OrderAccess;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;

import java.util.List;
import java.util.function.Consumer;

public class StatAuditOrderHandlerDecorator extends OrderHandlerDecorator {
    private final OrderAccess orderAccess;
    private final Consumer<String> auditSink;

    public StatAuditOrderHandlerDecorator(OrderHandler inner, OrderAccess orderAccess, Consumer<String> auditSink) {
        super(inner);
        this.orderAccess = orderAccess;
        this.auditSink = auditSink;
    }

    @Override
    public void handle(Order order) {
        int beforeEscalation = countPendingStatOfType(order);
        inner.handle(order);

        if (order.getPriority() == Priority.STAT) {
            int afterEscalation = countPendingStatOfType(order);
            int affected = Math.max(0, afterEscalation - beforeEscalation);
            auditSink.accept("STAT_AUDIT orderId=" + order.getId() + " patient=" + order.getPatientName()
                    + " affectedPatientCount=" + affected + " escalationDecisions=APPLIED");
        }
    }

    private int countPendingStatOfType(Order order) {
        List<Order> all = orderAccess.listAllOrders();
        int count = 0;
        for (Order current : all) {
            if (current.getType() == order.getType()
                    && current.getPriority() == Priority.STAT
                    && current.getStatus() == Status.PENDING) {
                count++;
            }
        }
        return count;
    }
}
