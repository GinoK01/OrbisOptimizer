# Relevance Function Patterns

> Status: Draft — theoretical patterns, not yet validated with real benchmarks.

## Overview

The relevance function is the central heuristic in OrbisOptimizer's scheduler. It estimates how important it is to simulate a Simulation Unit (SU) in the current tick. This document catalogs patterns for building and combining relevance signals.

The goal isn't to find a perfect formula — it's to identify a set of composable signals that implementations can mix and weight per game.

## Base formula

From the [model specification](model.md):

```
R(su, ctx) = w_d × proximity(su, nearest_player)
           + w_i × interaction_recency(su)
           + w_v × state_velocity(su)
           + w_c × criticality(su)
```

Each term is a signal in [0.0, 1.0]. The weights (w_d, w_i, w_v, w_c) sum to 1.0 and control how much each signal contributes.

---

## Signal patterns

### Pattern 1: Proximity

What it measures: How close the SU is to the nearest player.

Why it matters: Players notice what's nearby. Distant things rarely affect them in any meaningful way.

Simple implementation:
```
proximity(su, ctx) = max(0.0, 1.0 - (distance(su, nearest_player) / max_distance))
```

Where `max_distance` is the threshold beyond which proximity relevance drops to zero (e.g., server view distance in chunks).

Variants:
- Use the nearest player's view direction to weight what's directly in front of them more heavily.
- Count the number of players within a radius instead of just the nearest one, for multiplayer scenarios.
- Apply a falloff curve (linear, quadratic, exponential); steeper falloffs cut simulation of mid-distance entities more aggressively.

Caveats:
- "Distance to nearest player" is O(players) per SU. For many objects, use spatial indexing (grid, k-d tree) to bring this to O(1) or O(log n).
- In chunk-based games, chunk distance is usually cheaper to compute than Euclidean distance.

---

### Pattern 2: Interaction recency

What it measures: How recently a player interacted with this SU.

Why it matters: If a player just hit, used, or activated something, it's likely to stay relevant for several more ticks even if they've moved away.

Simple implementation:
```
interaction_recency(su, ctx) = max(0.0, 1.0 - (ticks_since_interaction / decay_window))
```

Where `decay_window` is how many ticks the interaction boost lasts (e.g., 100 ticks = 5 seconds at 20 TPS).

Variants:
- Weight by interaction type; a direct hit matters more than just looking at something.
- Use exponential decay instead of linear for a more natural falloff.
- Accumulate multiple interactions, not just the most recent one.

Caveats:
- Requires tracking last interaction state per SU — add a field or an external map.
- For entities that interact with each other (projectiles, explosions), decide whether entity-to-entity interactions count.

---

### Pattern 3: State velocity

What it measures: How actively the SU's state is changing.

Why it matters: A moving, attacking, or spawning entity is more likely to affect gameplay soon than a still one. An entity that hasn't changed in 200 ticks probably doesn't need to be simulated every tick.

Simple implementation:
```
state_velocity(su, ctx) = 1.0 if has_active_change(su) else 0.0
```

A binary version is cheap and usually sufficient. Continuous version:
```
state_velocity(su, ctx) = normalized(state_change_rate(su))
```

Variants:
- For entities: is it moving? attacking? pathfinding? does it have active AI?
- For chunks: did it change recently? are there active redstone circuits or spawners?
- For physics grids: is it moving? accelerating?

Caveats:
- "State change rate" can be expensive to compute. Proxy signals (position delta, health change flag) usually work just as well and are much cheaper.
- An inactive but nearby entity should still simulate sometimes — don't let state_velocity drive the score to zero for close objects.

---

### Pattern 4: Criticality

What it measures: Whether this SU must simulate this tick regardless of other factors.

Why it matters: Some operations are directly visible or safety-critical: an entity attacking a player, a block a player is placing, a physics body a player is riding. These should never be deferred.

Implementation:
```
criticality(su, ctx) = 1.0 if is_critical(su, ctx) else 0.0
```

`is_critical` depends on the game, but typical rules:
- Entity in direct combat with a player (dealing or receiving damage).
- Entity a player is interacting with right now.
- SU part of a physics simulation directly affecting a player.
- SU is a projectile fired by a player that hasn't hit anything yet.

Variants:
- Use `criticality` as an absolute override rather than a weight; critical SUs bypass the scheduler entirely (see [model.md](model.md) safety nets).
- Define "criticality levels": level 1 always runs, level 2 has high weight but can be deferred under extreme pressure.

Caveats:
- Defining criticality conservatively (more things = critical) is safer but limits optimization. Start conservative and loosen with data.
- In ECS architectures (like Hytale/Flecs), it may be easier to define criticality at the system level rather than per entity.

---

## Combining signals

Weighted sum is the simplest strategy. Alternatives:

### Maximum
```
R(su) = max(proximity, interaction_recency, state_velocity, criticality)
```
Useful when any high signal should guarantee simulation. Avoids averaging away important signals.

### Multiplicative
```
R(su) = proximity × (1 + interaction_boost) × (1 + velocity_boost)
```
Useful when signals should reinforce each other: something nearby and recently interacted with and actively moving is far more relevant than any single factor alone.

### Tiered
Partition SUs into tiers by a primary signal (e.g., distance), then score secondarily within each tier. Reduces the scoring space.

---

## Weight tuning

Starting point for a voxel sandbox game:

| Weight | Initial value | Notes |
|---|---|---|
| w_d (proximity) | 0.5 | Usually the strongest signal |
| w_i (interaction) | 0.3 | Strong signal in the short term |
| w_v (velocity) | 0.15 | Useful but not dominant |
| w_c (criticality) | 0.05 | Used as override, not primary signal |

These are estimates. The right values depend on the game, the load profile, and what "feels right" to players. Benchmarks should drive the tuning.

---

## Open questions

- Is a single scalar enough, or do some games need multidimensional scoring (e.g., visual relevance vs. gameplay relevance)?
- How should the relevance function handle SUs with long-range effects (explosions, area spells)?
- What's the minimum viable relevance function? Does proximity alone get 80% of the benefit with 20% of the complexity?

---

*Patterns will be refined as they get implemented and measured. If you tried something that worked or didn't, share it in the issues.*
