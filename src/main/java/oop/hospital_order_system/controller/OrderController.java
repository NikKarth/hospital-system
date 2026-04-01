package oop.hospital_order_system.controller;

import oop.hospital_order_system.command.CancelOrderCommand;
import oop.hospital_order_system.command.ClaimOrderCommand;
import oop.hospital_order_system.command.Command;
import oop.hospital_order_system.command.CompleteOrderCommand;
import oop.hospital_order_system.command.SubmitOrderCommand;
import oop.hospital_order_system.domain.Order;
import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.manager.OrderManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<Order> listPendingOrders() {
        return orderManager.listPendingOrders();
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
}
