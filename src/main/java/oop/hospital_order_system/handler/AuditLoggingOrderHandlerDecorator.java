package oop.hospital_order_system.handler;

import oop.hospital_order_system.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLoggingOrderHandlerDecorator extends OrderHandlerDecorator {
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingOrderHandlerDecorator.class);

    public AuditLoggingOrderHandlerDecorator(OrderHandler inner) {
        super(inner);
    }

    @Override
    public void handle(Order order) {
        logger.info("Order submit request: {} ({})", order.getType(), order.getId());
        inner.handle(order);
        logger.info("Order persisted: {} with status {}", order.getId(), order.getStatus());
    }
}
