# OrbisOptimizer — Hytale reference implementation

> Status: Not started
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
- Tick loop hooks via Hyxin `@Inject` / `@Redirect`
- ECS system enumeration using Flecs query API
- Player position tracking
- Interaction event capture
- Per-phase tick timings

### Layer 3 — Observability (`orbisoptimizer-hytale`, standard plugin)
- In-game commands: `/orbis status`, `/orbis map`
- Real-time visual dashboard (simulation activity heatmap)
- Metrics export (JSON/CSV)
- Config file (optional, auto-detected)

## Distribution

```
server/
├── early-plugins/
│   └── orbisoptimizer-early.jar    # bytecode transformers (Layer 1 + Layer 2)
└── mods/
    └── orbisoptimizer.jar          # commands, dashboard, public API (Layer 3)
```

Install both JARs. No configuration required to get started.

## Adapter contract status

| Capability | Status | Notes |
|---|---|---|
| R1: Enumerate SUs | Not started | Will enumerate ECS systems/archetypes via Flecs |
| R2: Tick timings | Not started | Via Hyxin hook on the tick loop |
| R3: Deferral control | Not started | Via `@Inject(cancellable=true)` on system execution |
| R4: Player positions | Not started | Standard plugin API |
| O1: Tick hooks | Not started | Hyxin pre/post tick inject |
| O2: Per-phase timings | Not started | Per-system timings via Hyxin |
| O3: Interaction events | Not started | Plugin event API |
| O4: State notifications | Not started | TBD — may require polling |

## Known challenges

ECS architecture (Flecs): Hytale entities aren't independently schedulable. Work happens at the ECS system level. OrbisOptimizer's SU definition needs to map to Flecs archetypes or system queries, not individual entities. This is the biggest open design question.

Criticality in Hytale's interaction system: The interaction system (combos, charging, wielding, forks) is stateful. Correctly identifying "this entity is in an active interaction chain with a player" requires understanding that state at scheduling time. Starting conservative (any direct player involvement = critical).

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
- Java 21+
- Hytale Early Access server JAR
- Hyxin framework
- Gradle build system
