# OrbisOptimizer Model Specification

> Version: 0.1.0-draft
> Status: Work in progress — everything here may change as we learn.

## Overview

This document describes how OrbisOptimizer approaches server-side simulation optimization. It's a starting point — a way to organize the problem so we can discuss it clearly, build implementations, and compare results across different games and engines.

The model is intentionally simple. If it turns out to be too simple for certain scenarios, that's also useful information and we'll adapt.

## 1. The four concepts

### 1.1 Simulation Unit

A Simulation Unit (SU) is the smallest piece of server work that can be independently scheduled — skipped, deferred, or run at a lower frequency without breaking the game.

What counts as an SU depends entirely on the game:

| Game type | Typical SU examples |
|---|---|
| Voxel sandbox (Minecraft, Hytale) | Entity tick, chunk tick, redstone |
| Multiplayer survival (Rust, Valheim) | Entity, structure decay, AI group |
| MMO | Zone, NPC group, global event system |
| Heavy physics (Space Engineers) | Physics grid, block group |

What qualifies:
- Can be deferred without corrupting shared game state.
- Has a measurable execution cost.
- Can be associated with a spatial or logical position relative to players.

What doesn't qualify: Network packet processing, player input handling, state persistence — these are fixed-cost operations that must run every tick without exception.

These boundaries aren't always clear. Part of implementing OrbisOptimizer for a specific game is discovering where the line is between deferrable and non-deferrable, and sometimes that requires trial and error.

### 1.2 Relevance Function

A Relevance Function R(su, ctx) → [0.0, 1.0] estimates how important a simulation unit is right now. Higher score = more important to simulate this tick.

Inputs that typically matter:
- How close is this SU to the nearest player?
- When did a player last interact with it?
- Is its state actively changing? (a moving entity vs. a stationary one)

Initial formula:

```
R(su, ctx) = w_d × proximity(su, nearest_player)
           + w_i × interaction_recency(su)
           + w_v × state_velocity(su)
```

The weights (w_d, w_i, w_v) are configurable and will likely differ per game type. Finding good values is part of the exploration.

What we believe matters for the relevance function:
- It must be deterministic; same inputs, same score.
- It must be cheap; if computing relevance costs more than the time it saves, it doesn't work.
- It must correlate with player impact; high scores should correspond to things players actually notice.

Criticality is intentionally not a term in this formula. Critical SUs bypass the scheduler entirely — they don't get scored, they just run. See section 3.2.

Whether this specific formula is correct, or whether a single scalar score is the right abstraction at all, is an open question.

### 1.3 Tick Budget

The Tick Budget is how much time the server has per tick for simulation work:

```
B = (1000ms / target_TPS) - fixed_overhead
```

At 30 TPS (Hytale's default), each tick window is 33.3ms. `world.getTickStepNanos()` returns 33,333,333 ns and is the canonical source for the budget — not a compile-time constant. `world.setTps(int)` allows runtime TPS changes, so the budget must be recomputed each tick rather than cached at startup.

Fixed overhead must be measured, not guessed. It will vary by server, player count, and engine.

### 1.4 Pressure Controller

The Pressure Controller manages how aggressively the optimizer defers work, based on how much of the tick budget is actually being used.

The idea: when the server is comfortable (using 60% of budget), optimize lightly. When it's stressed (using 90%), optimize aggressively. In between, hold steady.

AIMD (initial strategy):

```
if load_factor > 0.85:
    pressure = min(1.0, pressure + 0.05)       # Increase — defer more work
if load_factor < 0.60:
    pressure = max(0.0, pressure × 0.5)        # Decrease — recover gently
else:
    pressure = pressure                        # Hold
```

AIMD comes from TCP congestion control. It's used here because it's well understood and conservative when recovering (avoids oscillations). Whether it's the best fit for game servers is what benchmarks will tell us.

Other strategies (PID controllers, stepped thresholds, exponential backoff) may work better in different scenarios. That exploration is in [controllers.md](controllers.md).

## 2. How the scheduler works

Each tick:

1. Measure how much of the budget the previous tick used.
2. Update the pressure level.
3. Score each SU with the relevance function.
4. Skip any SU whose score is below the current pressure threshold. (soft filter)
5. Execute the remaining SUs in relevance order until the budget runs out. (hard filter)
6. Record which SUs were skipped and for how long.

Two things worth clarifying about steps 4 and 5:

- They're two separate filters. Step 4 cuts low-relevance work based on pressure. Step 5 cuts whatever didn't fit in the remaining time budget.
- An SU that passes step 4 but doesn't fit in the budget in step 5 still counts as deferred for staleness tracking purposes. It was ready to run, it just didn't get a slot.

Under heavy load, step 5 becomes the binding constraint, not step 4. That's fine — it means the optimizer is working as intended. But it also means the staleness limit needs to account for budget-deferred SUs, not just pressure-deferred ones.

This is deliberately straightforward. The complexity is in tuning the relevance function and the controller, not in the scheduling logic itself.

## 3. Safety nets

Heuristics can fail. These mechanisms limit the damage:

### 3.1 Staleness limit

No SU should be skipped for more than a configurable number of consecutive ticks. If it hits the limit, it's forced to simulate regardless of its relevance score. This prevents a low-relevance entity from accumulating so much stale state that simulating it later causes a performance spike.

Initial value: 10 seconds of ticks (300 ticks at 30 TPS). This is an estimate; benchmarks will tell us if it's reasonable. Because `world.setTps(int)` can change TPS at runtime, the staleness limit in ticks should be computed as `targetSeconds × currentTPS` rather than hardcoded.

### 3.2 Criticality override

Some SUs should never be deferred — entities in combat with a player, blocks a player is interacting with, physics directly affecting a player. These bypass the scheduler entirely and aren't scored by the relevance function.

This is a hard separation by design. Criticality isn't a high relevance score — it's an exit from the scoring path altogether. An SU is either critical (always runs) or it isn't (goes through the scheduler). Mixing the two into a single score would let pressure accidentally defer something that shouldn't be deferrable under any circumstances.

Defining what's critical depends on the game and will likely be the most subtle part of each implementation. Start conservative — when in doubt, treat it as critical.

### 3.3 Pressure ceiling

The controller should never reach P = 1.0 (skip everything). A ceiling of 0.9 ensures something non-critical always runs, even under extreme load.

The 0.9 value is a starting point, not a derived constant. The right ceiling depends on how scores are distributed across your SUs. If most scores cluster near zero (which is typical when few players are online and most entities are idle), a ceiling of 0.9 might defer very little. If scores are spread evenly, it defers about 10% of the queue. Benchmarks will tell you whether this needs adjusting. The important constraint is just that the ceiling stays below 1.0.

## 4. Adapter contract

For this to work with any game, the engine (or mod layer) must expose certain capabilities:

| Capability | What it provides |
|---|---|
| List simulation units | Enumerate what's running, with positions and types |
| Tick timings | How long the last tick took, ideally broken down by phase |
| Tick frequency control | Ability to skip or slow down specific SUs |
| Player positions | Where players are (and optionally where they're looking) |
| Tick hooks | Ability to run code before/after the main tick cycle |

Not all engines will expose all of this. Partial implementations are valid — the interesting question is how much optimization is achievable with limited hooks and where the diminishing returns are.

## 5. Observability

Open exploration. What's actually observable — and what it costs — depends on what Hyxin and Hytale's ECS expose at runtime. Can't answer that without running code.

The floor: any implementation needs to expose enough internal state to answer "why did the server stutter at timestamp X?" without guessing. What comes after that floor is an open question until there's a working prototype.

Minimum signals to aim for:

- Current pressure level.
- SUs simulated vs. deferred this tick.
- Distribution of relevance scores.
- How often the staleness limit fires.
- Actual tick budget utilization.

Those five are the starting point. Once real constraints from Hyxin and Hytale's ECS are understood, the full surface — commands, exports, dashboards, heatmaps — gets defined. That's Layer 3 in implementations/hytale/README.md, and it blocks on this exploration being done first.

## 6. SUs in ECS engines

The model description above assumes something like an OOP entity with a `tick()` method — a unit you can call or skip independently. ECS engines don't work that way.

In Hytale's ECS (and ECS in general), entities are just IDs with components attached. Work happens in systems, each of which processes all entities matching a query in one batch. There's no per-entity `tick()` to call or skip. The natural unit of deferral is the system, not the entity.

For the initial implementation, SU = ECS system.

Each running system is one Simulation Unit. The scheduler decides whether the system runs this tick. It's the coarsest possible granularity and the simplest to implement — the right starting point before adding per-entity complexity.

Hytale's ECS has three ticking system types: `TickingSystem` (base, `tick(float dt)`), `EntityTickingSystem` (exposes `getQuery()` returning `Query<EntityStore>`), and `ArchetypeTickingSystem`. The catch is that `ComponentRegistry` manages the list of active systems internally — it's not in the public API. You can call `EntityTickingSystem.getQuery()` on a system reference you already have, but getting those references in the first place requires injection. See OQ-13.

| Model concept | ECS equivalent |
|---|---|
| Simulation Unit | ECS system |
| SU position | Centroid or bounding box of entities matched by the system's query |
| SU cost | Average execution time of the system over the last N ticks |
| Defer SU | Skip system execution this tick |
| Enumerate SUs | List active systems and their matched entity counts (requires injection — see OQ-13) |

With SU = system, the relevance function scores the system as a whole. Proximity becomes: how close is the nearest entity matched by this system to the nearest player? State velocity: did this system's entities change state recently? Coarser than per-entity scoring, but a system with 500 matched entities costs one score instead of 500.

System-level deferral is all-or-nothing. If `MovementSystem` matches both a mob near a player and one on the other side of the map, deferring the system defers both. That's a real tradeoff. The alternative — per-entity deferral via archetype tags (`commandBuffer.addComponent()` / `removeComponent()`) — moves entities between archetypes every tick, which has its own cost and isn't obviously better.

Whether system-level granularity is enough, or whether the coarseness hurts optimization too much, is what benchmarks will tell us. This decision is provisional and gets revisited once there are actual numbers. See OQ-8 in [open-questions.md](open-questions.md).

## Open questions

See [open-questions.md](open-questions.md) for the current list. Relevant ones for the model: OQ-1, OQ-3, OQ-8, OQ-13.

---

*This spec is a living draft. It will change as we build, measure, and learn.*
