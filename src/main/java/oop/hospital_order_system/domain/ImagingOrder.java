package oop.hospital_order_system.domain;

public class ImagingOrder extends Order {
    public ImagingOrder(String patientName, String clinician, String description, Priority priority) {
        super(OrderType.IMAGING, patientName, clinician, description, priority);
    }
}
