package oop.hospital_order_system.manager;

import oop.hospital_order_system.access.OrderAccess;
import oop.hospital_order_system.command.Command;
import oop.hospital_order_system.command.CommandLogEntry;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;
import oop.hospital_order_system.factory.OrderFactory;
import oop.hospital_order_system.handler.*;
import oop.hospital_order_system.notification.NotificationService;
import oop.hospital_order_system.notification.OrderStatusSubject;
import oop.hospital_order_system.triage.TriagingEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class OrderManager {

    private final OrderAccess orderAccess;
    private final TriagingEngine triagingEngine;
    private final OrderStatusSubject statusSubject;
    private final List<CommandLogEntry> commandLog = Collections.synchronizedList(new ArrayList<>());
    private final OrderHandler orderHandler;

    public OrderManager() {
        this.orderAccess = new OrderAccess();
        this.triagingEngine = new TriagingEngine(new oop.hospital_order_system.triage.PriorityFirstTriageStrategy());
        this.statusSubject = new OrderStatusSubject();
        NotificationService logger = new oop.hospital_order_system.notification.LoggingNotificationService();
        statusSubject.registerObserver(logger);

        this.orderHandler = new AuditLoggingOrderHandlerDecorator(
                new PriorityBoostHandlerDecorator(
                        new ValidationOrderHandlerDecorator(
                                new BasicOrderHandler(orderAccess)
                        )
                )
        );
    }

    public void registerNotificationService(NotificationService service) {
        statusSubject.registerObserver(service);
    }

    public String submitOrder(OrderType type, String patientName, String clinician, String description, Priority priority) {
        Order order = OrderFactory.create(type, patientName, clinician, description, priority);
        orderHandler.handle(order);
        statusSubject.notifyObservers(order, "SUBMITTED");
        recordCommand(new CommandLogEntry("SUBMIT", order.getId(), clinician));
        return order.getId();
    }

    public void claimOrder(String orderId, String actor) {
        Order order = findOrderOrThrow(orderId);
        synchronized (order) {
            if (order.getStatus() != Status.PENDING) {
                throw new IllegalStateException("Order must be pending to claim");
            }
            order.setStatus(Status.IN_PROGRESS);
            order.setClaimedBy(actor);
            orderAccess.saveOrder(order);
            statusSubject.notifyObservers(order, "CLAIMED_BY_" + actor);
            recordCommand(new CommandLogEntry("CLAIM", orderId, actor));
        }
    }

    public void completeOrder(String orderId, String actor) {
        Order order = findOrderOrThrow(orderId);
        synchronized (order) {
            if (order.getStatus() != Status.IN_PROGRESS) {
                throw new IllegalStateException("Order must be in progress to complete");
            }
            if (!actor.equals(order.getClaimedBy())) {
                throw new IllegalStateException("Only claiming staff can complete this order");
            }
            order.setStatus(Status.COMPLETED);
            orderAccess.saveOrder(order);
            statusSubject.notifyObservers(order, "COMPLETED_BY_" + actor);
            recordCommand(new CommandLogEntry("COMPLETE", orderId, actor));
        }
    }

    public void cancelOrder(String orderId, String actor) {
        Order order = findOrderOrThrow(orderId);
        synchronized (order) {
            if (order.getStatus() != Status.PENDING) {
                throw new IllegalStateException("Only pending orders can be cancelled");
            }
            order.setStatus(Status.CANCELLED);
            orderAccess.saveOrder(order);
            statusSubject.notifyObservers(order, "CANCELLED_BY_" + actor);
            recordCommand(new CommandLogEntry("CANCEL", orderId, actor));
        }
    }

    public void dispatchCommand(Command command) {
        command.execute(this);
        if (command.getType() != null && command.getOrderId() != null) {
            // Submit command is already logged with orderId after ID created; to avoid duplicate, only log other commands here
            if (!"SUBMIT".equals(command.getType())) {
                recordCommand(new CommandLogEntry(command.getType(), command.getOrderId(), command.getActor()));
            }
        }
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderAccess.findOrderById(orderId);
    }

    public List<Order> listAllOrders() {
        return orderAccess.listOrdersSorted(triagingEngine.getComparator());
    }

    public List<Order> listPendingOrders() {
        List<Order> pending = orderAccess.listPendingOrders();
        pending.sort(triagingEngine.getComparator());
        return pending;
    }

    public List<CommandLogEntry> getCommandLog() {
        return new ArrayList<>(commandLog);
    }

    private void recordCommand(CommandLogEntry entry) {
        commandLog.add(entry);
    }

    private Order findOrderOrThrow(String orderId) {
        return orderAccess.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}
