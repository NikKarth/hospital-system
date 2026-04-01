package oop.hospital_order_system.factory;

import oop.hospital_order_system.domain.*;

public class OrderFactory {

    public static Order create(OrderType type, String patientName, String clinician, String description, Priority priority) {
        switch (type) {
            case LAB:
                return new LabOrder(patientName, clinician, description, priority);
            case MEDICATION:
                return new MedicationOrder(patientName, clinician, description, priority);
            case IMAGING:
                return new ImagingOrder(patientName, clinician, description, priority);
            default:
                throw new IllegalArgumentException("Unsupported order type: " + type);
        }
    }
}
