package oop.hospital_order_system.triage;

import oop.hospital_order_system.domain.OrderType;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class TriageStrategyCatalog {
    public static final String PRIORITY_FIRST = "PRIORITY_FIRST";
    public static final String LOAD_BALANCING = "LOAD_BALANCING";
    public static final String DEADLINE_FIRST = "DEADLINE_FIRST";

    private final Clock clock;
    private final Supplier<Map<OrderType, Long>> inProgressCountSupplier;

    public TriageStrategyCatalog(Clock clock, Supplier<Map<OrderType, Long>> inProgressCountSupplier) {
        this.clock = clock;
        this.inProgressCountSupplier = inProgressCountSupplier;
    }

    public Map<String, TriageStrategy> createAll() {
        Map<String, TriageStrategy> strategies = new LinkedHashMap<>();
        strategies.put(PRIORITY_FIRST, new PriorityFirstTriageStrategy());
        strategies.put(LOAD_BALANCING, new LoadBalancingTriageStrategy(inProgressCountSupplier));
        strategies.put(DEADLINE_FIRST, new DeadlineFirstTriageStrategy(clock));
        return strategies;
    }

    public String normalize(String strategyName) {
        return strategyName == null ? PRIORITY_FIRST : strategyName.trim().toUpperCase(Locale.ROOT);
    }
}
