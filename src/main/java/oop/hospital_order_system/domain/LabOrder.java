package oop.hospital_order_system.domain;

public class LabOrder extends Order {
    public LabOrder(String patientName, String clinician, String description, Priority priority) {
        super(OrderType.LAB, patientName, clinician, description, priority);
    }
}
