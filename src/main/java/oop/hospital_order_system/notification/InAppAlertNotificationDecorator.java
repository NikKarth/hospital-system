package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InAppAlertNotificationDecorator extends NotificationDecorator {
    private static final Logger logger = LoggerFactory.getLogger(InAppAlertNotificationDecorator.class);
    private final InAppAlertCounter counter;

    public InAppAlertNotificationDecorator(NotificationService inner, InAppAlertCounter counter) {
        super(inner);
        this.counter = counter;
    }

    @Override
    public void notify(Order order, String event) {
        super.notify(order, event);
        int updated = counter.incrementAndGet();
        logger.info("InAppAlert: badgeCount={}, orderId={}, event={}", updated, order.getId(), event);
    }
}
