package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSummaryNotificationDecorator extends NotificationDecorator {
    private static final Logger logger = LoggerFactory.getLogger(EmailSummaryNotificationDecorator.class);

    public EmailSummaryNotificationDecorator(NotificationService inner) {
        super(inner);
    }

    @Override
    public void notify(Order order, String event) {
        super.notify(order, event);
        logger.info("EmailSummary: toRole={}, subject='Order Update', body='orderId={}, type={}, status={}, event={}'",
                deriveRole(event), order.getId(), order.getType(), order.getStatus(), event);
    }

    private String deriveRole(String event) {
        if (event.startsWith("CLAIMED") || event.startsWith("COMPLETED")) {
            return "FULFILMENT";
        }
        if (event.startsWith("CANCELLED")) {
            return "CLINICIAN";
        }
        return "ADMIN";
    }
}
