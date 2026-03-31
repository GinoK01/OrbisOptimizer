# Adapter Contract

> Status: Draft — defined from theory. Will be revised once the Hytale reference implementation is built.

## Overview

The Adapter Contract defines what a game engine (or mod layer) must expose for OrbisOptimizer to function. It's the boundary between the generic scheduling model and the game-specific implementation.

Not all engines will expose everything listed here. That's fine — the interesting question is how much optimization is achievable with partial hooks, and where the diminishing returns are. A partial adapter is better than no adapter.

---

## Required capabilities

These are the minimum capabilities needed for a functional (if limited) implementation.

### R1: Enumerate simulation units

The engine must provide a way to list what's currently available to schedule — entities, chunks, systems, or whatever SU equivalent the game uses.

```
interface SimulationUnitProvider {
    List<SimulationUnit> getSchedulableUnits();
}
```

Each `SimulationUnit` must expose:
- A stable identifier (used for staleness tracking, interaction history, etc.)
- A position or spatial key (used for proximity scoring)
- An estimated execution cost (optional but useful — can be approximated from type)

Partial fallback: If the engine can't enumerate all SUs, enumerate a meaningful subset (e.g., entities only, ignoring chunk ticks). Document what's missing.

---

### R2: Tick timings

The engine must provide timing information from the previous tick.

```
interface TickTimingProvider {
    long getLastTickDurationNanos();
}
```

Ideally broken down by phase (entity ticks, chunk ticks, networking) — but total duration is the minimum needed to compute the `load_factor`.

Why it matters: Without timings, the pressure controller is blind. Every other capability depends on this one working accurately.

---

### R3: Tick frequency control

The engine must allow the adapter to skip or defer a simulation unit for the current tick.

```
interface SchedulerControl {
    void deferUnit(SimulationUnitId id);      // skip this tick
    void forceUnit(SimulationUnitId id);      // force simulation this tick (staleness override)
}
```

This is the central control surface. Without it, OrbisOptimizer can observe and report, but not optimize.

Partial fallback: If per-unit control isn't available, control at a coarser level (e.g., defer all entities in a chunk, or throttle an entire ECS system). Lower optimization potential but still useful.

---

### R4: Player positions

The engine must expose where players currently are.

```
interface PlayerTracker {
    List<PlayerPosition> getPlayerPositions();
}
```

Each `PlayerPosition` includes at minimum: player ID, world position. Optionally: view direction, interaction target.

Why it matters: Proximity is the strongest relevance signal for most games. Without player positions, the proximity term is unusable.

---

## Optional capabilities

These unlock additional optimization potential but aren't strictly required.

### O1: Tick hooks

The ability to run code before and after the main tick cycle.

```
interface TickHooks {
    void registerPreTickListener(Runnable hook);
    void registerPostTickListener(Runnable hook);
}
```

Used for: collecting per-tick measurements, updating pressure, and publishing observability data at tick boundaries.

Without this: The adapter must find another way to hook into the tick cycle (e.g., a scheduled task, a separate thread). Possible but less precise.

---

### O2: Per-phase timings

Tick duration broken down by phase.

```
interface PhaseTimingProvider {
    Map<String, Long> getLastTickPhaseTimings(); // phase name → duration in nanoseconds
}
```

Used for: identifying which phase (entity ticks vs. chunk ticks vs. networking) consumes the most budget, and focusing optimization effort accordingly.

Without this: MSPT breakdown in benchmarks is less precise. Optimization is applied uniformly rather than per phase.

---

### O3: Interaction events

A stream of player-SU interaction events (hit, use, place, look).

```
interface InteractionEventStream {
    void subscribe(InteractionEventListener listener);
}

interface InteractionEventListener {
    void onInteraction(SimulationUnitId su, InteractionType type, long tickTimestamp);
}
```

Used for: maintaining the `interaction_recency` signal in the relevance function.

Without this: Interaction recency can't be tracked. The relevance function falls back to proximity + state velocity only.

---

### O4: State change notifications

Notifications when an SU's state changes significantly (entity starts moving, enters combat, etc.).

```
interface StateChangeNotifier {
    void subscribe(StateChangeListener listener);
}
```

Used for: efficiently maintaining the `state_velocity` signal without polling every SU every tick.

Without this: State velocity is approximated through position delta polling or omitted entirely.

---

## ECS-specific notes

For engines using Entity Component Systems (like Hytale with Flecs), the adapter contract maps differently. The full treatment is in [model.md section 6](model.md#6-sus-in-ecs-engines). Short version for the adapter:

| Standard concept | ECS equivalent (initial implementation) |
|---|---|
| Simulation Unit | ECS system |
| Enumerate SUs | List active systems with matched entity counts |
| Defer unit | Skip system execution this tick |
| Cost per unit | Rolling average execution time per system |
| SU position | Centroid of entities matched by the system's query |

SU = whole system for now, not per-entity or per-archetype. It's the coarsest option and the easiest to hook. Whether that granularity is fine enough for meaningful optimization is what the first benchmarks will show.

---

## Minimum viable adapter

The smallest adapter that can produce meaningful optimization:

R1 (enumerate SUs) + R2 (tick timings) + R3 (deferral control) + R4 (player positions)

With these four, OrbisOptimizer can:
- Score SUs by proximity.
- Defer low-scoring SUs.
- Adjust pressure based on load.
- Apply the staleness limit.

"Meaningful" has a concrete target: the MVP adapter should show measurable improvement in P95 MSPT under scenario B (clustering) compared to no optimization. If R1-R4 alone can't do that, the optional capabilities are worth looking at.

That's a bar, not a guarantee. Load profiles vary. But it's something to aim at when evaluating a partial implementation.

---

## Capability checklist for new implementations

When documenting a new implementation, include a table like this:

| Capability | Status | Notes |
|---|---|---|
| R1: Enumerate SUs | ✅ / ⚠️ / ❌ | What's included / excluded |
| R2: Tick timings | ✅ / ⚠️ / ❌ | Total only, or per phase |
| R3: Deferral control | ✅ / ⚠️ / ❌ | Per unit, per type, or per chunk |
| R4: Player positions | ✅ / ⚠️ / ❌ | |
| O1: Tick hooks | ✅ / ⚠️ / ❌ | |
| O2: Per-phase timings | ✅ / ⚠️ / ❌ | |
| O3: Interaction events | ✅ / ⚠️ / ❌ | |
| O4: State notifications | ✅ / ⚠️ / ❌ | |

✅ = fully available, ⚠️ = partial / approximate, ❌ = not available

---

*The contract will be revised as we learn what Hytale's API actually exposes and what requires bytecode injection via early plugins.*
