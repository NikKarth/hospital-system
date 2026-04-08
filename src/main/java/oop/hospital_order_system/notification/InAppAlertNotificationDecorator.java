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
        String claimedBy = order.getClaimedBy();
        if (claimedBy == null || claimedBy.isBlank()) {
            logger.info("InAppAlert: badgeCount={}, patient={}, clinician={}, orderId={}, event={}",
                    updated, order.getPatientName(), order.getClinician(), order.getId(), event);
            return;
        }

        logger.info("InAppAlert: badgeCount={}, patient={}, clinician={}, claimedBy={}, orderId={}, event={}",
                updated, order.getPatientName(), order.getClinician(), claimedBy, order.getId(), event);
    }
}
