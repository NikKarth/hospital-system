package oop.hospital_order_system.controller;

import oop.hospital_order_system.command.CommandLogEntry;
import oop.hospital_order_system.manager.OrderManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final OrderManager orderManager;

    public AuditController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @GetMapping("/commands")
    public List<CommandLogEntry> getCommandLog() {
        return orderManager.getCommandLog();
    }
}
