# Devlog #004 - Phase 1: what we built and what changed

> Date: 2026-04-08
> Phase: Implementation, passive profiler

Phase 1 is done. A few things went differently than planned, and I'd rather write them down now than piece it together from git history later.

---

## What changed from the plan

### No WorldTickMixin

I had `WorldTickMixin` down as the first file to write: `@Inject HEAD + RETURN` into `World extends TickingThread` to get clean tick timing without plugin overhead contaminating the measurement.

Didn't need it. `TickingThread` already exposes `getBufferedTickLengthMetricSet()`, which has `getLastValue()` returning the last tick's duration in nanoseconds. The server measures it itself. `TickTimingAccumulator` just reads it. I'm not going to complain about not having to write a mixin.

### PassiveProfiler runs on a background thread

The plan was tick-hook driven: inject into the tick loop, run profiler logic in the post-hook every Nth tick. Instead, `PassiveProfiler` runs a `ScheduledExecutorService` waking every second.

The reads aren't tick-boundary-aligned; the profiler picks up whatever the last completed tick measured, which could be slightly stale. For passive observation with no scheduling decisions being made, that's fine. If Phase 2 needs the pressure controller updating at tick boundaries, the hook goes in then. Right now, figuring out Hyxin injection before I've even run this thing against a real server felt like getting ahead of myself.

### Standard plugin, not early plugin

Original plan had the JAR in `early-plugins/`. Phase 1 doesn't do any bytecode transformation at class-load time, so a standard `JavaPlugin` in `mods/` is enough. Early plugin registration becomes relevant when Hyxin mixins enter the picture. That's Phase 2.

---

## What was built

Four classes. One resources file.

`OrbisEarlyPlugin` wires `PassiveProfiler` into the plugin lifecycle. `TickTimingAccumulator` reads `world.getBufferedTickLengthMetricSet().getLastValue()` and returns 0 before the first tick completes. `PassiveProfiler` iterates all active worlds once per second and emits one log line per world:

```
[OrbisOptimizer|<world>] load_factor=0.412 budget_util=0.412 systems=42 rel_dist=PLACEHOLDER staleness_hits=PLACEHOLDER
```

S1 and S5 are the same value right now: both `lastTickNanos / tickStepNanos`. S3 and S4 are explicit `PLACEHOLDER` strings. S2 is either a real count or `PLACEHOLDER(OQ-13)` depending on whether reflection succeeds.

`EcsSystemReader` attempts reflection on `ComponentRegistry.systemSize` via `EntityStore.REGISTRY`, logs whether it worked at startup, and returns -1 on failure. `PassiveProfiler` renders -1 as `PLACEHOLDER(OQ-13)`.

---

## OQ-13

The original question was about Hyxin injection into the `ComponentRegistry` dispatch loop. Before doing that, I tried something simpler: reflect directly on a private field.

`EcsSystemReader` resolves the field at class-load time. If `ComponentRegistry.systemSize` exists and is accessible, signal 2 is real. If not (wrong field name, obfuscation, SecurityManager, whatever), it falls back without crashing.

`systemSize` is a guess. Reasonable, but a guess. The actual field name in the shipped JAR might be different. I'll know when this runs against a real server. If reflection fails, the fallback is Hyxin injection into the dispatch loop, which is what I was going to do anyway. Reflection is just a shortcut that costs nothing to try.

---

## Verifying Phase 1

Drop `orbisoptimizer-early.jar` into `mods/`. Start the server. At startup you should see one of:

```
[OrbisOptimizer] EcsSystemReader: reflection on ComponentRegistry.systemSize OK — signal 2 available, OQ-13 resolved.
```
```
[OrbisOptimizer] EcsSystemReader: cannot access ComponentRegistry.systemSize (NoSuchFieldException) — signal 2 will report PLACEHOLDER(OQ-13).
```

After that, one line per world per second. If `load_factor` is stuck at 0.000, `getBufferedTickLengthMetricSet()` is returning null. The API assumption was wrong and timing needs a different approach.

---

## What actually happened

Ran it. Both signals came back real.

```
[OrbisOptimizer|default] load_factor=0.008 budget_util=0.008 systems=425 rel_dist=PLACEHOLDER staleness_hits=PLACEHOLDER
```

`systems=425` — reflection on `ComponentRegistry.systemSize` worked. Field name was right. OQ-13 is closed. `load_factor=0.008` makes sense for an idle server: about 0.8% of the tick budget.

One warning logged at startup: the server couldn't recognize `ServerVersion: "*"` as a valid target version specifier and flagged it as a future hard error. The plugin still loaded. That's a manifest format question for later — once I know what the server actually expects (a concrete version, a range, something else), the manifest gets updated.

## What's next

The data is real. But 425 is still a number without context: no names, no per-system timing, no variance data. Phase 1 isn't done. Devlog/005 starts from there.

---

*This devlog is part of the [OrbisOptimizer project](../README.md). Updates come when there's something worth sharing, not on a schedule.*
