package dev.orbisoptimizer.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public final class OrbisEarlyPlugin extends JavaPlugin {

    private PassiveProfiler profiler;

    public OrbisEarlyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        profiler = new PassiveProfiler(getLogger());
    }

    @Override
    protected void start() {
        profiler.start();
        getLogger().at(Level.INFO).log("[OrbisOptimizer] Passive profiler started. Logging every ~1 second.");
    }

    @Override
    protected void shutdown() {
        profiler.stop();
        getLogger().at(Level.INFO).log("[OrbisOptimizer] Passive profiler stopped.");
    }
}
