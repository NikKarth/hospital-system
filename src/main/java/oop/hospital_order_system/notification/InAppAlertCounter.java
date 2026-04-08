package oop.hospital_order_system.notification;

import java.util.concurrent.atomic.AtomicInteger;

public class InAppAlertCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    public int get() {
        return count.get();
    }

    public void set(int value) {
        count.set(Math.max(0, value));
    }
}
