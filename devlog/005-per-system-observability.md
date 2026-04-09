# Devlog #005 - The number was 425

> Date: 2026-04-09
> Phase: Phase 1 extended — per-system observability

Phase 1 was a passive profiler: read global tick timing, count active ECS systems, log it. Nothing deferred, nothing scored. Just watching. It ran against a real server and both signals came back real.

That's the good part.

We have `systems=425`. Count of ECS systems in `ComponentRegistry`, retrieved via reflection — field name was right, OQ-13 is closed.

The problem: 425 is a fact, not information. It tells me the server has 425 ECS systems. It tells me nothing about which ones are expensive, which ones spike, or which ones I'd actually want to defer. Building a scheduler on top of what Phase 1 produced would mean scheduling blind. "425 systems, global load factor 0.8%" is not enough to make a deferral decision that isn't arbitrary.

So Phase 1 isn't done yet.

---

## What the profiler doesn't know

The obvious things: how long each system takes — not the total tick, each of the 425 individually (`ISystem.tick()` is the right granularity) — and what each one is called (`system.getClass().getSimpleName()` at the same injection point, free since we're already there).

The less obvious one is stability. A system that consistently costs 5ms is a scheduling target. One that costs 0.1ms most ticks but spikes to 40ms every 50 is a different problem entirely. Mean and standard deviation over a rolling window are enough to tell them apart. A scheduler that ignores variance is going to surprise you.

---

## The plan

`ComponentRegistry` runs a dispatch loop, something like `for (system : systems) system.tick(dt)`. That's the injection target. Wrap each `tick()` call with `System.nanoTime()` before and after.

Two classes, not one, because the costs are different. `EcsSystemReader` reads a field via reflection at startup — zero ongoing overhead. `EcsSystemProfiler` injects into the hot path of every tick. Merging them would mean paying injection overhead even when all you need is the count.

The overhead estimate for `EcsSystemProfiler` is 425 pairs of `System.nanoTime()` per tick, around 4–8 µs. That assumes sequential access without synchronization, which holds now with a single profiler thread. Phase 2 changes that: when the scheduler runs on a separate thread, the ring buffer will need synchronization and the number goes up. Worth tracking before that happens, not after.

Expected output:

```
[OrbisOptimizer|default] top_systems: FlockSystems.Ticking=12.4ms(σ=3.1) EntityMovement=8.7ms(σ=0.2) SpawnControllerSystem=4.2ms(σ=8.9)
```

In that example, `SpawnControllerSystem` is the interesting one — not the most expensive by average, but σ=8.9 on a 4.2ms mean is a ratio above 2. `EntityMovement` costs twice as much and barely moves. A scheduler that only looks at averages would deprioritize spawn control; a scheduler that looks at variance would flag it first.

---

## OQ-14

The open question is whether Hyxin can inject in that dispatch loop. `ComponentRegistry` isn't public API. OQ-13 confirmed `systemSize` is readable via reflection, but that's a field read. Method body injection is a different surface.

If `@Inject` works: straightforward. If not: `@Redirect` on the `ISystem.tick()` call is the fallback — same effect, different annotation. If neither works because the class is final or obfuscated, it's a blocker.

I'll find out when the injection attempt runs against a real server.

---

*This devlog is part of the [OrbisOptimizer project](../README.md). Updates come when there's something worth sharing, not on a schedule.*
