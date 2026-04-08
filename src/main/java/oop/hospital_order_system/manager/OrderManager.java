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
import oop.hospital_order_system.notification.EmailSummaryNotificationDecorator;
import oop.hospital_order_system.notification.InAppAlertCounter;
import oop.hospital_order_system.notification.InAppAlertNotificationDecorator;
import oop.hospital_order_system.notification.NotificationChannel;
import oop.hospital_order_system.notification.NotificationService;
import oop.hospital_order_system.notification.LoggingNotificationService;
import oop.hospital_order_system.triage.DeadlineFirstTriageStrategy;
import oop.hospital_order_system.triage.TriageStrategy;
import oop.hospital_order_system.triage.TriageStrategyCatalog;
import oop.hospital_order_system.triage.TriagingEngine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

@Service
public class OrderManager {

    private static final List<String> DEFAULT_FULFILMENT_STAFF = List.of("Tech1", "Tech2", "Tech3");

    private final OrderAccess orderAccess;
    private final Clock clock;
    private final Map<String, TriageStrategy> availableStrategies;
    private final Map<String, TriagingEngine> triagingByDepartment = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> triageSelectionByDepartment = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Set<NotificationChannel>> notificationChannelsByRole = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<NotificationService> additionalNotificationServices = Collections.synchronizedList(new ArrayList<>());
    private final InAppAlertCounter inAppAlertCounter = new InAppAlertCounter();
    private final Deque<SystemSnapshot> undoStack = new ArrayDeque<>();
    private final List<Command> executedCommands = Collections.synchronizedList(new ArrayList<>());
    private final List<CommandLogEntry> commandLog = Collections.synchronizedList(new ArrayList<>());
    private final OrderHandler orderHandler;

    public OrderManager() {
        this.orderAccess = new OrderAccess();
        this.clock = Clock.systemUTC();
        TriageStrategyCatalog catalog = new TriageStrategyCatalog(clock, this::inProgressCountsByType);
        this.availableStrategies = catalog.createAll();

        triagingByDepartment.put("LAB", new TriagingEngine(availableStrategies.get(TriageStrategyCatalog.PRIORITY_FIRST)));
        triagingByDepartment.put("PHARMACY", new TriagingEngine(availableStrategies.get(TriageStrategyCatalog.PRIORITY_FIRST)));
        triagingByDepartment.put("RADIOLOGY", new TriagingEngine(availableStrategies.get(TriageStrategyCatalog.PRIORITY_FIRST)));
        triagingByDepartment.put("ALL", new TriagingEngine(availableStrategies.get(TriageStrategyCatalog.PRIORITY_FIRST)));

        triageSelectionByDepartment.put("LAB", TriageStrategyCatalog.PRIORITY_FIRST);
        triageSelectionByDepartment.put("PHARMACY", TriageStrategyCatalog.PRIORITY_FIRST);
        triageSelectionByDepartment.put("RADIOLOGY", TriageStrategyCatalog.PRIORITY_FIRST);
        triageSelectionByDepartment.put("ALL", TriageStrategyCatalog.PRIORITY_FIRST);

        notificationChannelsByRole.put("CLINICIAN", EnumSet.of(NotificationChannel.CONSOLE));
        notificationChannelsByRole.put("FULFILMENT", EnumSet.of(NotificationChannel.CONSOLE));
        notificationChannelsByRole.put("ADMIN", EnumSet.of(NotificationChannel.CONSOLE));

        this.orderHandler = new AuditLoggingOrderHandlerDecorator(
                new StatAuditOrderHandlerDecorator(
                        new StatPriorityEscalationDecorator(
                                new PriorityBoostHandlerDecorator(
                                        new ValidationOrderHandlerDecorator(
                                                new BasicOrderHandler(orderAccess)
                                        )
                                ),
                                orderAccess,
                        clock,
                        Duration.ofMinutes(5)
                        ),
                        orderAccess,
                        this::recordStatAudit
                )
        );
    }

    public void setDepartmentStrategy(String department, String strategyName) {
        String normalizedDepartment = normalizeDepartment(department);
        String normalizedStrategy = strategyName == null ? TriageStrategyCatalog.PRIORITY_FIRST : strategyName.trim().toUpperCase(Locale.ROOT);
        TriageStrategy strategy = availableStrategies.get(normalizedStrategy);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported triage strategy: " + strategyName);
        }
        triagingByDepartment.put(normalizedDepartment, new TriagingEngine(strategy));
        triageSelectionByDepartment.put(normalizedDepartment, normalizedStrategy);
    }

    public Map<String, String> getDepartmentStrategySelections() {
        return new LinkedHashMap<>(triageSelectionByDepartment);
    }

    public List<String> getAvailableStrategies() {
        return new ArrayList<>(availableStrategies.keySet());
    }

    public void updateNotificationChannels(String role, Set<String> channels) {
        String normalizedRole = normalizeRole(role);
        EnumSet<NotificationChannel> selected = EnumSet.noneOf(NotificationChannel.class);
        for (String channel : channels == null ? Set.<String>of() : channels) {
            selected.add(NotificationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT)));
        }
        if (selected.isEmpty()) {
            selected.add(NotificationChannel.CONSOLE);
        }
        notificationChannelsByRole.put(normalizedRole, selected);
    }

    public Map<String, Set<NotificationChannel>> getNotificationChannelsByRole() {
        Map<String, Set<NotificationChannel>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<NotificationChannel>> entry : notificationChannelsByRole.entrySet()) {
            copy.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }
        return copy;
    }

    public int getInAppAlertCount() {
        return inAppAlertCounter.get();
    }

    public void undoLastCommand() {
        SystemSnapshot snapshot = undoStack.pollFirst();
        if (snapshot == null) {
            throw new IllegalStateException("No command available to undo");
        }
        restoreSnapshot(snapshot);
        if (!executedCommands.isEmpty()) {
            executedCommands.remove(executedCommands.size() - 1);
        }
    }

    public void replayCommand(int commandIndex) {
        if (commandIndex < 0 || commandIndex >= executedCommands.size()) {
            throw new IllegalArgumentException("Replay index out of bounds: " + commandIndex);
        }
        dispatchCommand(executedCommands.get(commandIndex));
    }

    public void replayCommandByLogIndex(int logIndex) {
        List<CommandLogEntry> logs = getCommandLog();
        if (logIndex < 0 || logIndex >= logs.size()) {
            throw new IllegalArgumentException("Log index out of bounds: " + logIndex);
        }

        int commandIndex = -1;
        for (int i = 0; i <= logIndex; i++) {
            if (isReplayableType(logs.get(i).getType())) {
                commandIndex++;
            }
        }
        if (commandIndex < 0 || !isReplayableType(logs.get(logIndex).getType())) {
            throw new IllegalArgumentException("Selected log entry is not replayable");
        }
        replayCommand(commandIndex);
    }

    public int replayableCommandCount() {
        return executedCommands.size();
    }

    public void registerNotificationService(NotificationService service) {
        additionalNotificationServices.add(service);
    }

    public String submitOrder(OrderType type, String patientName, String clinician, String description, Priority priority) {
        Order order = OrderFactory.create(type, patientName, clinician, description, priority);
        orderHandler.handle(order);

        if (isLoadBalancingEnabledForType(type)) {
            autoClaimByLeastLoadedStaff(order.getId());
        }

        notifyByRole(order, "SUBMITTED", "CLINICIAN");
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
            notifyByRole(order, "CLAIMED_BY_" + actor, "FULFILMENT");
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
            notifyByRole(order, "COMPLETED_BY_" + actor, "FULFILMENT");
            recordCommand(new CommandLogEntry("COMPLETE", orderId, actor));
        }
    }

    public void cancelOrder(String orderId, String actor) {
        Order order = findOrderOrThrow(orderId);
        synchronized (order) {
            if (order.getStatus() != Status.PENDING && order.getStatus() != Status.IN_PROGRESS) {
                throw new IllegalStateException("Only pending or in-progress orders can be cancelled");
            }
            order.setStatus(Status.CANCELLED);
            orderAccess.saveOrder(order);
            notifyByRole(order, "CANCELLED_BY_" + actor, "CLINICIAN");
            recordCommand(new CommandLogEntry("CANCEL", orderId, actor));
        }
    }

    public void dispatchCommand(Command command) {
        SystemSnapshot snapshot = captureSnapshot();
        command.execute(this);
        undoStack.addFirst(snapshot);
        executedCommands.add(command);
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderAccess.findOrderById(orderId);
    }

    public List<Order> listAllOrders() {
        return orderAccess.listOrdersSorted(triagingByDepartment.get("ALL").getComparator());
    }

    public List<Order> listPendingOrders() {
        return listPendingOrdersByDepartment("ALL");
    }

    public List<Order> listPendingOrdersByDepartment(String department) {
        String normalizedDepartment = normalizeDepartment(department);
        TriagingEngine engine = triagingByDepartment.getOrDefault(normalizedDepartment, triagingByDepartment.get("ALL"));

        List<Order> pending = orderAccess.listPendingOrders();
        if (!"ALL".equals(normalizedDepartment)) {
            OrderType type = mapDepartmentToOrderType(normalizedDepartment);
            pending = pending.stream().filter(order -> order.getType() == type).toList();
        }
        pending.sort(engine.getComparator());
        return pending;
    }

    public List<Order> listQueueOrdersByDepartment(String department) {
        String normalizedDepartment = normalizeDepartment(department);
        TriagingEngine engine = triagingByDepartment.getOrDefault(normalizedDepartment, triagingByDepartment.get("ALL"));

        List<Order> active = orderAccess.listAllOrders().stream()
                .filter(order -> order.getStatus() == Status.PENDING || order.getStatus() == Status.IN_PROGRESS)
                .toList();
        if (!"ALL".equals(normalizedDepartment)) {
            OrderType type = mapDepartmentToOrderType(normalizedDepartment);
            active = active.stream().filter(order -> order.getType() == type).toList();
        }
        active = new ArrayList<>(active);
        active.sort(engine.getComparator());
        return active;
    }

    public List<CommandLogEntry> getCommandLog() {
        return new ArrayList<>(commandLog);
    }

    public Optional<Long> timeRemainingToDeadlineMillis(Order order) {
        if (!isStrategyEnabledForType(order.getType(), TriageStrategyCatalog.DEADLINE_FIRST)) {
            return Optional.empty();
        }
        Instant deadline = order.getSubmittedAt().plus(DeadlineFirstTriageStrategy.resolveDeadline(order.getType(), order.getPriority()));
        return Optional.of(Duration.between(Instant.now(clock), deadline).toMillis());
    }

    private void recordCommand(CommandLogEntry entry) {
        commandLog.add(entry);
    }

    private boolean isReplayableType(String type) {
        return "SUBMIT".equals(type) || "CLAIM".equals(type) || "COMPLETE".equals(type) || "CANCEL".equals(type);
    }

    private void recordStatAudit(String details) {
        commandLog.add(new CommandLogEntry("STAT_AUDIT", details, "system"));
    }

    private Order findOrderOrThrow(String orderId) {
        return orderAccess.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private String normalizeDepartment(String department) {
        if (department == null || department.isBlank()) {
            return "ALL";
        }
        String normalized = department.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ALL", "LAB", "PHARMACY", "RADIOLOGY").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported department: " + department);
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "CLINICIAN";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private OrderType mapDepartmentToOrderType(String department) {
        if ("LAB".equals(department)) {
            return OrderType.LAB;
        }
        if ("PHARMACY".equals(department)) {
            return OrderType.MEDICATION;
        }
        if ("RADIOLOGY".equals(department)) {
            return OrderType.IMAGING;
        }
        throw new IllegalArgumentException("No specific order type for department: " + department);
    }

    private void notifyByRole(Order order, String event, String role) {
        NotificationService composed = new LoggingNotificationService();
        Set<NotificationChannel> channels = notificationChannelsByRole.getOrDefault(role, EnumSet.of(NotificationChannel.CONSOLE));

        if (channels.contains(NotificationChannel.IN_APP)) {
            composed = new InAppAlertNotificationDecorator(composed, inAppAlertCounter);
        }
        if (channels.contains(NotificationChannel.EMAIL)) {
            composed = new EmailSummaryNotificationDecorator(composed);
        }

        composed.notify(order, event);
        for (NotificationService observer : new ArrayList<>(additionalNotificationServices)) {
            observer.notify(order, event);
        }
    }

    private Map<OrderType, Long> inProgressCountsByType() {
        Map<OrderType, Long> counts = new HashMap<>();
        for (Order order : orderAccess.listInProgressOrders()) {
            counts.merge(order.getType(), 1L, Long::sum);
        }
        return counts;
    }

    private boolean isStrategyEnabledForType(OrderType type, String strategy) {
        String department = mapOrderTypeToDepartment(type);
        String selected = triageSelectionByDepartment.getOrDefault(department, TriageStrategyCatalog.PRIORITY_FIRST);
        if (strategy.equals(selected)) {
            return true;
        }
        return strategy.equals(triageSelectionByDepartment.getOrDefault("ALL", TriageStrategyCatalog.PRIORITY_FIRST));
    }

    private boolean isLoadBalancingEnabledForType(OrderType type) {
        return isStrategyEnabledForType(type, TriageStrategyCatalog.LOAD_BALANCING);
    }

    private void autoClaimByLeastLoadedStaff(String orderId) {
        String assignee = pickLeastLoadedFulfilmentStaff();
        claimOrder(orderId, assignee);
    }

    private String pickLeastLoadedFulfilmentStaff() {
        Set<String> available = new HashSet<>(DEFAULT_FULFILMENT_STAFF);
        for (Order order : orderAccess.listAllOrders()) {
            String claimedBy = order.getClaimedBy();
            if (claimedBy != null && !claimedBy.isBlank()) {
                available.add(claimedBy);
            }
        }

        Map<String, Long> inProgressByStaff = new HashMap<>();
        for (Order order : orderAccess.listInProgressOrders()) {
            String claimedBy = order.getClaimedBy();
            if (claimedBy != null && !claimedBy.isBlank()) {
                inProgressByStaff.merge(claimedBy, 1L, Long::sum);
            }
        }

        long min = Long.MAX_VALUE;
        List<String> candidates = new ArrayList<>();
        for (String staff : available) {
            long load = inProgressByStaff.getOrDefault(staff, 0L);
            if (load < min) {
                min = load;
                candidates.clear();
                candidates.add(staff);
            } else if (load == min) {
                candidates.add(staff);
            }
        }

        if (candidates.isEmpty()) {
            return DEFAULT_FULFILMENT_STAFF.get(0);
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private String mapOrderTypeToDepartment(OrderType type) {
        if (type == OrderType.LAB) {
            return "LAB";
        }
        if (type == OrderType.MEDICATION) {
            return "PHARMACY";
        }
        if (type == OrderType.IMAGING) {
            return "RADIOLOGY";
        }
        return "ALL";
    }

    private SystemSnapshot captureSnapshot() {
        Map<String, OrderState> states = new HashMap<>();
        for (Order order : orderAccess.listAllOrders()) {
            states.put(order.getId(), new OrderState(order.getPriority(), order.getStatus(), order.getClaimedBy()));
        }
        return new SystemSnapshot(states, commandLog.size(), inAppAlertCounter.get());
    }

    private void restoreSnapshot(SystemSnapshot snapshot) {
        List<Order> currentOrders = orderAccess.listAllOrders();
        for (Order order : currentOrders) {
            if (!snapshot.orderStates.containsKey(order.getId())) {
                orderAccess.deleteOrder(order.getId());
            }
        }

        for (Map.Entry<String, OrderState> entry : snapshot.orderStates.entrySet()) {
            Order order = orderAccess.findOrderById(entry.getKey()).orElse(null);
            if (order != null) {
                order.setPriority(entry.getValue().priority);
                order.setStatus(entry.getValue().status);
                order.setClaimedBy(entry.getValue().claimedBy);
                orderAccess.saveOrder(order);
            }
        }

        while (commandLog.size() > snapshot.commandLogSize) {
            commandLog.remove(commandLog.size() - 1);
        }
        inAppAlertCounter.set(snapshot.inAppAlertCount);
    }

    private static class OrderState {
        private final Priority priority;
        private final Status status;
        private final String claimedBy;

        private OrderState(Priority priority, Status status, String claimedBy) {
            this.priority = priority;
            this.status = status;
            this.claimedBy = claimedBy;
        }
    }

    private static class SystemSnapshot {
        private final Map<String, OrderState> orderStates;
        private final int commandLogSize;
        private final int inAppAlertCount;

        private SystemSnapshot(Map<String, OrderState> orderStates, int commandLogSize, int inAppAlertCount) {
            this.orderStates = orderStates;
            this.commandLogSize = commandLogSize;
            this.inAppAlertCount = inAppAlertCount;
        }
    }
}
