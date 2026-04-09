package dev.orbisoptimizer.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import dev.orbisoptimizer.hytale.ecs.EcsSystemReader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class PassiveProfiler {

    private static final long INTERVAL_SECONDS = 1L;

    private final HytaleLogger logger;
    private ScheduledExecutorService scheduler;

    public PassiveProfiler(HytaleLogger logger) {
        this.logger = logger;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "orbisoptimizer-profiler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::observeAllWorlds, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void observeAllWorlds() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }
            for (World world : universe.getWorlds().values()) {
                observeWorld(world);
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("[OrbisOptimizer] Observation error: %s", e.getMessage());
        }
    }

    private void observeWorld(World world) {
        long lastTickNanos = TickTimingAccumulator.getLastGameTickNanos(world);
        long tickStepNanos = world.getTickStepNanos();

        double loadFactor = tickStepNanos > 0 ? (double) lastTickNanos / tickStepNanos : 0.0;
        double budgetUtil = loadFactor;

        int systemCount = EcsSystemReader.countSystems();
        String systemsStr = systemCount >= 0 ? String.valueOf(systemCount) : "PLACEHOLDER(OQ-13)";

        logger.at(Level.INFO).log(
            "[OrbisOptimizer|%s] load_factor=%.3f budget_util=%.3f systems=%s rel_dist=PLACEHOLDER staleness_hits=PLACEHOLDER",
            world.getName(),
            loadFactor,
            budgetUtil,
            systemsStr
        );
    }
}
