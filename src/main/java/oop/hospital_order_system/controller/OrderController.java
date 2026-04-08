package oop.hospital_order_system.controller;

import oop.hospital_order_system.command.CancelOrderCommand;
import oop.hospital_order_system.command.ClaimOrderCommand;
import oop.hospital_order_system.command.Command;
import oop.hospital_order_system.command.CompleteOrderCommand;
import oop.hospital_order_system.command.SubmitOrderCommand;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.domain.Status;
import oop.hospital_order_system.manager.OrderManager;
import oop.hospital_order_system.notification.NotificationChannel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderManager orderManager;

    public OrderController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @PostMapping
    public ResponseEntity<?> submitOrder(@RequestBody SubmitOrderRequest req) {
        try {
            SubmitOrderCommand command = new SubmitOrderCommand(req.getType(), req.getPatientName(), req.getClinician(), req.getDescription(), req.getPriority());
            orderManager.dispatchCommand(command);
            return ResponseEntity.status(HttpStatus.CREATED).body(new SubmitOrderResponse(command.getOrderId()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claim(@PathVariable("id") String id, @RequestParam("actor") String actor) {
        try {
            Command command = new ClaimOrderCommand(id, actor);
            orderManager.dispatchCommand(command);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable("id") String id, @RequestParam("actor") String actor) {
        try {
            Command command = new CompleteOrderCommand(id, actor);
            orderManager.dispatchCommand(command);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable("id") String id, @RequestParam("actor") String actor) {
        try {
            Command command = new CancelOrderCommand(id, actor);
            orderManager.dispatchCommand(command);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping
    public List<Order> listOrders() {
        return orderManager.listAllOrders();
    }

    @GetMapping("/pending")
    public List<Order> listPendingOrders(@RequestParam(value = "department", required = false) String department) {
        if (department == null || department.isBlank()) {
            return orderManager.listPendingOrders();
        }
        return orderManager.listPendingOrdersByDepartment(department);
    }

    @GetMapping("/queue")
    public List<QueueOrderResponse> listQueueOrders(@RequestParam(value = "department", required = false) String department) {
        if (department == null || department.isBlank()) {
            department = "ALL";
        }
        return orderManager.listQueueOrdersByDepartment(department).stream()
                .map(order -> new QueueOrderResponse(order, orderManager.timeRemainingToDeadlineMillis(order).orElse(null)))
                .toList();
    }

    @PostMapping("/settings/triage")
    public ResponseEntity<?> setTriageStrategy(@RequestBody TriageSelectionRequest request) {
        try {
            orderManager.setDepartmentStrategy(request.getDepartment(), request.getStrategy());
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/settings/triage")
    public Map<String, Object> getTriageSettings() {
        return Map.of(
                "availableStrategies", orderManager.getAvailableStrategies(),
                "selectedByDepartment", orderManager.getDepartmentStrategySelections()
        );
    }

    @PostMapping("/settings/notifications")
    public ResponseEntity<?> setNotificationChannels(@RequestBody NotificationSettingsRequest request) {
        try {
            orderManager.updateNotificationChannels(request.getRole(), request.getChannels());
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/settings/notifications")
    public Map<String, Set<NotificationChannel>> getNotificationSettings() {
        return orderManager.getNotificationChannelsByRole();
    }

    @GetMapping("/notifications/in-app-count")
    public Map<String, Integer> getInAppAlertCount() {
        return Map.of("count", orderManager.getInAppAlertCount());
    }

    @PostMapping("/admin/undo")
    public ResponseEntity<?> undoLastCommand() {
        try {
            orderManager.undoLastCommand();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/admin/replay/{index}")
    public ResponseEntity<?> replayCommand(@PathVariable("index") int index) {
        try {
            orderManager.replayCommandByLogIndex(index);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/admin/replay/count")
    public Map<String, Integer> replayableCount() {
        return Map.of("count", orderManager.replayableCommandCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable("id") String id) {
        return orderManager.getOrderById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Order not found")));
    }

    public static class SubmitOrderRequest {
        private OrderType type;
        private String patientName;
        private String clinician;
        private String description;
        private Priority priority;

        public OrderType getType() {
            return type;
        }

        public void setType(OrderType type) {
            this.type = type;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getClinician() {
            return clinician;
        }

        public void setClinician(String clinician) {
            this.clinician = clinician;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }
    }

    public static class SubmitOrderResponse {
        private final String orderId;

        public SubmitOrderResponse(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderId() {
            return orderId;
        }
    }

    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    public static class QueueOrderResponse {
        private final String id;
        private final OrderType type;
        private final String patientName;
        private final String clinician;
        private final String description;
        private final Priority priority;
        private final String claimedBy;
        private final Instant submittedAt;
        private final Status status;
        private final Long timeRemainingMillis;

        public QueueOrderResponse(Order order, Long timeRemainingMillis) {
            this.id = order.getId();
            this.type = order.getType();
            this.patientName = order.getPatientName();
            this.clinician = order.getClinician();
            this.description = order.getDescription();
            this.priority = order.getPriority();
            this.claimedBy = order.getClaimedBy();
            this.submittedAt = order.getSubmittedAt();
            this.status = order.getStatus();
            this.timeRemainingMillis = timeRemainingMillis;
        }

        public String getId() {
            return id;
        }

        public OrderType getType() {
            return type;
        }

        public String getPatientName() {
            return patientName;
        }

        public String getClinician() {
            return clinician;
        }

        public String getDescription() {
            return description;
        }

        public Priority getPriority() {
            return priority;
        }

        public String getClaimedBy() {
            return claimedBy;
        }

        public Instant getSubmittedAt() {
            return submittedAt;
        }

        public Status getStatus() {
            return status;
        }

        public Long getTimeRemainingMillis() {
            return timeRemainingMillis;
        }
    }

    public static class TriageSelectionRequest {
        private String department;
        private String strategy;

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }

    public static class NotificationSettingsRequest {
        private String role;
        private Set<String> channels = Collections.emptySet();

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Set<String> getChannels() {
            return channels;
        }

        public void setChannels(Set<String> channels) {
            this.channels = channels;
        }
    }
}
