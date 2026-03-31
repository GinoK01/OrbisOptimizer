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
- Is it something that must run regardless? (an entity in combat with a player)

Initial formula:

```
R(su, ctx) = w_d × proximity(su, nearest_player)
           + w_i × interaction_recency(su)
           + w_v × state_velocity(su)
           + w_c × criticality(su)
```

The weights (w_d, w_i, w_v, w_c) are configurable and will likely differ per game type. Finding good values is part of the exploration.

What we believe matters for the relevance function:
- It must be deterministic; same inputs, same score.
- It must be cheap; if computing relevance costs more than the time it saves, it doesn't work.
- It must correlate with player impact; high scores should correspond to things players actually notice.

Whether this specific formula is correct, or whether a single scalar score is the right abstraction at all, is an open question.

### 1.3 Tick Budget

The Tick Budget is how much time the server has per tick for simulation work:

```
B = (1000ms / target_TPS) - fixed_overhead
```

At 20 TPS with 8ms of network and I/O overhead, that leaves roughly 42ms for simulation.

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
4. Skip any SU whose score is below the current pressure threshold.
5. Execute the remaining SUs in relevance order until the budget runs out.
6. Record which SUs were skipped and for how long.

This is deliberately straightforward. The complexity is in tuning the relevance function and the controller, not in the scheduling logic itself.

## 3. Safety nets

Heuristics can fail. These mechanisms limit the damage:

### 3.1 Staleness limit

No SU should be skipped for more than a configurable number of consecutive ticks. If it hits the limit, it's forced to simulate regardless of its relevance score. This prevents a low-relevance entity from accumulating so much stale state that simulating it later causes a performance spike.

Initial value: 10 seconds of ticks (200 ticks at 20 TPS). It's an estimate; benchmarks will tell us if it's reasonable.

### 3.2 Criticality override

Some SUs should never be deferred — entities in combat with a player, blocks a player is interacting with, physics directly affecting a player. These bypass the scheduler entirely.

Defining what's critical depends on the game and will likely be the most subtle part of each implementation.

### 3.3 Pressure ceiling

The controller should never reach P = 1.0 (skip everything). A ceiling of 0.9 ensures something non-critical always runs, even under extreme load.

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

Any implementation must expose enough information to understand what it's doing:

- Current pressure level.
- How many SUs were simulated vs. deferred this tick.
- Distribution of relevance scores.
- How often the staleness limit is hit.
- Actual tick budget utilization.

This isn't about building a dashboard — it's about being able to answer "why did the server stutter at timestamp X?" without guessing.

## Open questions

Things we don't have answers to yet:

- Is a single relevance score enough, or do some games need multidimensional scoring?
- How do we handle SUs that are cheap individually but numerous? (10,000 particles at 0.01ms each)
- What's the right staleness limit? Is a fixed number enough or should it adapt?
- Does the four-concept model hold for zone-scale MMO servers?
- How much does the spatial data structure choice matter for relevance scoring at scale?
- How do Simulation Units map to ECS archetypes and systems? (relevant for Hytale/Flecs)

If you have experience or ideas on any of these, open an issue on the repository.

---

*This spec is a living draft. It will change as we build, measure, and learn.*
