package oop.hospital_order_system.domain;

public class MedicationOrder extends Order {
    public MedicationOrder(String patientName, String clinician, String description, Priority priority) {
        super(OrderType.MEDICATION, patientName, clinician, description, priority);
    }
}
