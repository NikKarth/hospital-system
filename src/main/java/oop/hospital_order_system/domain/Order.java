package oop.hospital_order_system.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public abstract class Order {
    private final String id;
    private final OrderType type;
    private final String patientName;
    private final String clinician;
    private final String description;
    private final Instant submittedAt;
    private Priority priority;
    private Status status;
    private String claimedBy;

    protected Order(OrderType type, String patientName, String clinician, String description, Priority priority) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.patientName = patientName;
        this.clinician = clinician;
        this.description = description;
        this.priority = priority;
        this.status = Status.PENDING;
        this.submittedAt = Instant.now();
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

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", patientName='" + patientName + '\'' +
                ", clinician='" + clinician + '\'' +
                ", description='" + description + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", submittedAt=" + submittedAt +
                ", claimedBy='" + claimedBy + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
