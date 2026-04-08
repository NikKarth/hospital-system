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
        String claimedBy = order.getClaimedBy();
        if (claimedBy == null || claimedBy.isBlank()) {
            logger.info("EmailSummary: toRole={}, subject='Order Update', body='patient={}, clinician={}, orderId={}, type={}, status={}, event={}'",
                deriveRole(event), order.getPatientName(), order.getClinician(), order.getId(), order.getType(), order.getStatus(), event);
            return;
        }

        logger.info("EmailSummary: toRole={}, subject='Order Update', body='patient={}, clinician={}, claimedBy={}, orderId={}, type={}, status={}, event={}'",
            deriveRole(event), order.getPatientName(), order.getClinician(), claimedBy, order.getId(), order.getType(), order.getStatus(), event);
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
