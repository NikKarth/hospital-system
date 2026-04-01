package oop.hospital_order_system.notification;

import oop.hospital_order_system.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notify(Order order, String event) {
        logger.info("Notification: actor={}, orderId={}, type={}, status={}, event={}",
                order.getClinician(), order.getId(), order.getType(), order.getStatus(), event);
    }
}
