package dev.orbisoptimizer.hytale.ecs;

import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EcsSystemReader {

    private static final Logger LOGGER = Logger.getLogger("OrbisOptimizer");
    private static final Field SYSTEM_SIZE_FIELD = resolveField();

    private EcsSystemReader() {}

    private static Field resolveField() {
        try {
            Field f = ComponentRegistry.class.getDeclaredField("systemSize");
            f.setAccessible(true);
            LOGGER.log(Level.INFO,
                "[OrbisOptimizer] EcsSystemReader: reflection on ComponentRegistry.systemSize OK — signal 2 available, OQ-13 resolved.");
            return f;
        } catch (NoSuchFieldException | SecurityException e) {
            LOGGER.log(Level.WARNING,
                "[OrbisOptimizer] EcsSystemReader: cannot access ComponentRegistry.systemSize ({0}) — signal 2 will report PLACEHOLDER(OQ-13).",
                e.getClass().getSimpleName());
            return null;
        }
    }

    public static int countSystems() {
        if (SYSTEM_SIZE_FIELD == null) {
            return -1;
        }
        try {
            return (int) SYSTEM_SIZE_FIELD.get(EntityStore.REGISTRY);
        } catch (IllegalAccessException e) {
            return -1;
        }
    }
}
