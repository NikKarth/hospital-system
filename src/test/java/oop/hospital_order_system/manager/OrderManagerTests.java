package oop.hospital_order_system.manager;

import oop.hospital_order_system.command.CommandLogEntry;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagerTests {

    @Test
    void submitAndClaimCompleteAndCancel() {
        OrderManager manager = new OrderManager();

        String orderId = manager.submitOrder(OrderType.LAB, "Alice", "Dr Bob", "Stat test", Priority.STAT);
        assertNotNull(orderId);

        Order created = manager.getOrderById(orderId).orElseThrow();
        assertEquals(Status.PENDING, created.getStatus());

        // claim
        manager.claimOrder(orderId, "Tech1");
        Order claimed = manager.getOrderById(orderId).orElseThrow();
        assertEquals(Status.IN_PROGRESS, claimed.getStatus());
        assertEquals("Tech1", claimed.getClaimedBy());

        // complete
        manager.completeOrder(orderId, "Tech1");
        Order completed = manager.getOrderById(orderId).orElseThrow();
        assertEquals(Status.COMPLETED, completed.getStatus());

        // a second order cancel
        String order2 = manager.submitOrder(OrderType.IMAGING, "Bob", "Dr Jane", "Routine scan", Priority.ROUTINE);
        manager.cancelOrder(order2, "Dr Jane");
        Order cancelled = manager.getOrderById(order2).orElseThrow();
        assertEquals(Status.CANCELLED, cancelled.getStatus());

        List<CommandLogEntry> log = manager.getCommandLog();
        assertEquals(5, log.size());
    }

    @Test
    void cannotClaimAlreadyClaimedOrder() {
        OrderManager manager = new OrderManager();
        String orderId = manager.submitOrder(OrderType.MEDICATION, "Tim", "Dr Sam", "Dose", Priority.URGENT);
        manager.claimOrder(orderId, "TechA");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> manager.claimOrder(orderId, "TechB"));
        assertTrue(ex.getMessage().contains("pending"));
    }

    @Test
    void cannotCancelNonPendingOrder() {
        OrderManager manager = new OrderManager();
        String orderId = manager.submitOrder(OrderType.LAB, "Pat", "Dr Saul", "Test", Priority.ROUTINE);
        manager.claimOrder(orderId, "Tech1");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> manager.cancelOrder(orderId, "Dr Saul"));
        assertTrue(ex.getMessage().contains("pending"));
    }

    @Test
    void pendingOrdersSortedByPriority() {
        OrderManager manager = new OrderManager();

        manager.submitOrder(OrderType.LAB, "A", "Dr1", "1", Priority.ROUTINE);
        manager.submitOrder(OrderType.IMAGING, "B", "Dr2", "2", Priority.STAT);
        manager.submitOrder(OrderType.MEDICATION, "C", "Dr3", "3", Priority.URGENT);

        List<Order> pending = manager.listPendingOrders();
        assertEquals(3, pending.size());
        assertEquals(Priority.STAT, pending.get(0).getPriority());
        assertEquals(Priority.URGENT, pending.get(1).getPriority());
        assertEquals(Priority.ROUTINE, pending.get(2).getPriority());
    }
}
