package dev.orbisoptimizer.hytale;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.util.thread.TickingThread;

public final class TickTimingAccumulator {

    private TickTimingAccumulator() {}

    public static long getLastGameTickNanos(TickingThread world) {
        HistoricMetric metric = world.getBufferedTickLengthMetricSet();
        if (metric == null) {
            return 0L;
        }
        return metric.getLastValue();
    }
}
