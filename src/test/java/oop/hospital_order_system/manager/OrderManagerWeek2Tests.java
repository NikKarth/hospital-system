package oop.hospital_order_system.manager;

import oop.hospital_order_system.command.SubmitOrderCommand;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderManagerWeek2Tests {

    @Test
    void inAppNotificationChannelIncrementsBadge() {
        // Arrange
        OrderManager manager = new OrderManager();
        manager.updateNotificationChannels("CLINICIAN", Set.of("CONSOLE", "IN_APP"));

        // Act
        manager.submitOrder(OrderType.LAB, "P1", "Dr A", "Panel", Priority.ROUTINE);

        // Assert
        assertEquals(1, manager.getInAppAlertCount());
    }

    @Test
    void undoRevertsLastExecutedCommand() {
        // Arrange
        OrderManager manager = new OrderManager();
        SubmitOrderCommand command = new SubmitOrderCommand(OrderType.IMAGING, "P2", "Dr B", "Scan", Priority.URGENT);

        // Act
        manager.dispatchCommand(command);
        manager.undoLastCommand();

        // Assert
        assertEquals(0, manager.listAllOrders().size());
    }

    @Test
    void replayReExecutesPastCommand() {
        // Arrange
        OrderManager manager = new OrderManager();
        SubmitOrderCommand command = new SubmitOrderCommand(OrderType.MEDICATION, "P3", "Dr C", "Dose", Priority.ROUTINE);
        manager.dispatchCommand(command);

        // Act
        manager.replayCommand(0);

        // Assert
        assertEquals(2, manager.listAllOrders().size());
    }

    @Test
    void loadBalancingAutoClaimsSubmittedOrder() {
        OrderManager manager = new OrderManager();
        manager.setDepartmentStrategy("LAB", "LOAD_BALANCING");

        String orderId = manager.submitOrder(OrderType.LAB, "P4", "Dr D", "Panel", Priority.STAT);
        Order order = manager.getOrderById(orderId).orElseThrow();

        assertEquals(Status.IN_PROGRESS, order.getStatus());
        assertNotNull(order.getClaimedBy());
        assertTrue(Set.of("Tech1", "Tech2", "Tech3").contains(order.getClaimedBy()));
    }

    @Test
    void loadBalancingPrefersLeastLoadedStaff() {
        OrderManager manager = new OrderManager();

        String m1 = manager.submitOrder(OrderType.MEDICATION, "P5", "Dr E", "Dose1", Priority.ROUTINE);
        manager.claimOrder(m1, "Tech1");
        String m2 = manager.submitOrder(OrderType.IMAGING, "P6", "Dr F", "Scan1", Priority.URGENT);
        manager.claimOrder(m2, "Tech1");

        manager.setDepartmentStrategy("LAB", "LOAD_BALANCING");
        String lab = manager.submitOrder(OrderType.LAB, "P7", "Dr G", "Panel2", Priority.ROUTINE);
        Order labOrder = manager.getOrderById(lab).orElseThrow();

        assertEquals(Status.IN_PROGRESS, labOrder.getStatus());
        assertNotEquals("Tech1", labOrder.getClaimedBy());
    }

    @Test
    void deadlineRemainingVisibleOnlyWhenDeadlineFirstEnabled() {
        OrderManager manager = new OrderManager();
        String labOrderId = manager.submitOrder(OrderType.LAB, "P8", "Dr H", "Panel", Priority.STAT);
        Order order = manager.getOrderById(labOrderId).orElseThrow();

        assertFalse(manager.timeRemainingToDeadlineMillis(order).isPresent());

        manager.setDepartmentStrategy("LAB", "DEADLINE_FIRST");
        assertTrue(manager.timeRemainingToDeadlineMillis(order).isPresent());
    }

    @Test
    void claimNotifiesClinicianChannelsForInAppAndEmail() {
        OrderManager manager = new OrderManager();
        manager.updateNotificationChannels("CLINICIAN", Set.of("CONSOLE", "IN_APP", "EMAIL"));

        String orderId = manager.submitOrder(OrderType.LAB, "P9", "Dr I", "Panel", Priority.ROUTINE);
        int beforeClaim = manager.getInAppAlertCount();

        manager.claimOrder(orderId, "Tech1");

        assertEquals(beforeClaim + 1, manager.getInAppAlertCount());
    }

    @Test
    void urgentEscalatesToStatWhenRecentMatchingStatExists() {
        OrderManager manager = new OrderManager();

        manager.submitOrder(OrderType.LAB, "P10", "Dr J", "CBC panel", Priority.STAT);
        String urgentId = manager.submitOrder(OrderType.LAB, "P11", "Dr K", "CBC panel", Priority.URGENT);
        Order urgent = manager.getOrderById(urgentId).orElseThrow();

        assertEquals(Priority.STAT, urgent.getPriority());
    }

    @Test
    void urgentDoesNotEscalateWhenDescriptionDoesNotMatch() {
        OrderManager manager = new OrderManager();

        manager.submitOrder(OrderType.LAB, "P12", "Dr L", "Troponin", Priority.STAT);
        String urgentId = manager.submitOrder(OrderType.LAB, "P13", "Dr M", "CBC panel", Priority.URGENT);
        Order urgent = manager.getOrderById(urgentId).orElseThrow();

        assertEquals(Priority.URGENT, urgent.getPriority());
    }
}
