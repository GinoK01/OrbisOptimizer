# OrbisOptimizer — Hytale reference implementation

> Status: Phase 1 in progress — passive profiler verified, per-system timing pending
> Implements: OrbisOptimizer Model Spec v0.1.0-draft
> Target: Hytale Early Access (January 2026+)

## Overview

This is the reference implementation of OrbisOptimizer for Hytale. It has two purposes:

1. Demonstrate that the model works in practice — real optimization, real benchmarks, measurable results.
2. Show how to implement the adapter contract for a specific game, so others can follow the pattern in their own games.

## Architecture

Three layers, distributed as two JARs:

### Layer 1 — Scheduling Engine (`orbisoptimizer-engine`)
Pure Java, no Hytale dependencies. Contains:
- Relevance scoring with configurable weights
- Pressure controller (AIMD, swappable)
- Tick budget management
- Staleness tracking and criticality override
- Clean interfaces (the organic API)

### Layer 2 — Hytale Adapter (`orbisoptimizer-hytale`, early plugin)
Connects the engine to Hytale via bytecode injection. Contains:
- Tick loop hooks via Hyxin `@Inject` / `@Redirect` in `World extends TickingThread`
- ECS system enumeration via injection in `ComponentRegistry` dispatch loop
- Player position tracking (`Universe.get().getPlayers()` — no injection required)
- Interaction event capture (`EventRegistry` — no injection required)
- Per-phase tick timings via injection around each `ISystem.tick()` call

### Layer 3 — Observability (`orbisoptimizer-hytale`, standard plugin)

Scope is open. What Hyxin and Hytale's ECS actually expose at runtime determines what this layer can do — that's the exploration in spec/model.md section 5. Once the baseline engine signals are verified (pressure level, defer counts, score distribution, staleness hits, budget utilization), Layer 3 gets scoped around what's actually reachable.

Working direction:
- In-game commands: `/orbis status`, `/orbis map`
- Real-time visual dashboard (simulation activity heatmap)
- Metrics export (JSON/CSV)
- Config file (optional, auto-detected)

## Distribution

```
server/
└── mods/
    └── orbisoptimizer-early.jar    # passive profiler (Phase 1)
```

Drop the JAR into `mods/`. No configuration required. The optimizer starts active with default settings — AIMD begins at zero pressure and only ramps up if the server actually needs it. On a healthy server, it does nothing visible.

## Adapter contract status

| Capability | Status | Notes |
|---|---|---|
| R1: Enumerate SUs | ⚠️ Partial | Reflection on `ComponentRegistry.systemSize` via `EntityStore.REGISTRY` — confirmed working (`systems=425` on idle server). OQ-13 closed. |
| R2: Tick timings | ⚠️ Partial | `TickingThread.getBufferedTickLengthMetricSet()` — confirmed working (`load_factor=0.008` on idle server). |
| R3: Deferral control | Not started | Via `@Inject(cancellable=true)` on system dispatch loop |
| R4: Player positions | ✅ API confirmed | `Universe.get().getPlayers()` → `PlayerRef` → `TransformComponent` → `Vector3d`. No injection needed. |
| O1: Tick hooks | Not started | Inject in `World.tick()` or `TickingThread` run loop (target: `World extends TickingThread`) |
| O2: Per-phase timings | Next — OQ-14 | Injection in `ComponentRegistry` dispatch loop; wrap each `ISystem.tick()` with `System.nanoTime()`. Class: `EcsSystemProfiler`. |
| O3: Interaction events | ✅ API confirmed | `PlaceBlockEvent`, `BreakBlockEvent`, `DamageBlockEvent`, `UseBlockEvent` via `getEventRegistry().register()`. Combat via `DamageDataComponent.lastCombatAction`. |
| O4: State notifications | Not started | Position delta polling + `MovementStatesComponent` change detection |

## Known challenges

ECS architecture: Hytale entities aren't independently schedulable. Work happens at the ECS system level (classes: `TickingSystem`, `EntityTickingSystem`, `ArchetypeTickingSystem`). For now, SU = ECS system — the scheduler decides whether a whole system runs each tick. Per-entity deferral via archetype tags (`commandBuffer.addComponent/removeComponent`) is the alternative but has a real migration cost. The list of active systems is internal to `ComponentRegistry` and requires injection to enumerate. See OQ-13 and model.md section 6.

Criticality in Hytale's interaction system: The interaction model (combos, charging, wielding, forks) is stateful. The good news is there are four concrete signals available without injection — `DamageDataComponent.lastCombatAction`, `FlockMembership`+`Flock.currentDamageData`, `MountedComponent`, and active `InteractionChain` on a player. Starting conservative: any direct player involvement is critical. See ARQUITECTURA_FASE1.md for the full list.

Injection stability: Early access means breaking changes are likely. Every injection point needs automated tests. Strict versioning per release.

## Benchmarks

Results will be published in `benchmarks/` following the [standard methodology](../../docs/methodology/METHODOLOGY.md).

Planned baseline comparisons:
1. Vanilla Hytale server (no optimization)
2. Server Optimizer (threshold-based, most popular existing solution)
3. OrbisOptimizer (AIMD scheduling, this implementation)

## Development environment

*To be documented once the project skeleton is in place.*

Expected requirements:
- Java 21 (toolchain; Java 25 target when Gradle 8.12+ is stable)
- Hytale Early Access server JAR
- Hyxin framework (Phase 2+)
- Gradle build system (Kotlin DSL)
