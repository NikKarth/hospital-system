package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notify(Order order, String event) {
        String claimedBy = order.getClaimedBy();
        if (claimedBy == null || claimedBy.isBlank()) {
            logger.info("Notification: patient={}, clinician={}, orderId={}, type={}, status={}, event={}",
                    order.getPatientName(), order.getClinician(), order.getId(), order.getType(), order.getStatus(), event);
            return;
        }

        logger.info("Notification: patient={}, clinician={}, claimedBy={}, orderId={}, type={}, status={}, event={}",
                order.getPatientName(), order.getClinician(), claimedBy, order.getId(), order.getType(), order.getStatus(), event);
    }
}
