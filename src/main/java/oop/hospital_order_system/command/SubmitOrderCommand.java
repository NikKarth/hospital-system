package oop.hospital_order_system.command;

import oop.hospital_order_system.domain.OrderType;
import oop.hospital_order_system.domain.Priority;
import oop.hospital_order_system.manager.OrderManager;

public class SubmitOrderCommand implements Command {
    private final OrderType type;
    private final String patientName;
    private final String clinician;
    private final String description;
    private final Priority priority;
    private String orderId;

    public SubmitOrderCommand(OrderType type, String patientName, String clinician, String description, Priority priority) {
        this.type = type;
        this.patientName = patientName;
        this.clinician = clinician;
        this.description = description;
        this.priority = priority;
    }

    @Override
    public void execute(OrderManager manager) {
        this.orderId = manager.submitOrder(type, patientName, clinician, description, priority);
    }

    @Override
    public String getType() {
        return "SUBMIT";
    }

    @Override
    public String getOrderId() {
        return orderId;
    }

    @Override
    public String getActor() {
        return clinician;
    }
}
