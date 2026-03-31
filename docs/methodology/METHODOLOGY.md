# Benchmark Methodology

> Status: Draft — defining how we want to measure things before we start measuring.

## What we want to learn

Benchmarks exist to answer practical questions:

- How much headroom does heuristic scheduling actually buy under realistic loads?
- Where does it help most? Where does it help least?
- What's the overhead of the optimizer itself?
- Does the game feel different to players when optimization is active?

We're not trying to prove a point — we're trying to understand the tradeoffs. Results showing the approach doesn't help in a particular scenario are just as publishable and useful as results showing that it does.

## What we measure

| Metric | Unit | Why it matters |
|---|---|---|
| TPS (sustained) | ticks/sec | Can the server hold its target tick rate? |
| MSPT (P50, P95, P99) | milliseconds | Not just the average — how bad are the worst ticks? |
| Entity throughput | entities simulated / ms | How efficient is simulation with optimization active? |
| Scheduling overhead | μs / tick | How much does the optimizer itself cost? |
| Correctness delta | qualitative | Does the game behave differently with optimization active? |
| Memory overhead | MB | Additional memory used by the scheduler |

"Correctness delta" is intentionally qualitative for now. Measuring exact game state divergence is difficult and game-specific. For early benchmarks, "does it feel wrong to a player?" is a reasonable starting point. We can get more rigorous as the methodology matures.

## Test scenarios

Standardized situations that anyone can set up and run:

### Scenario A: Steady state
- N players, static, distributed evenly.
- M entities, distributed evenly.
- The baseline: how much does the optimizer help when nothing dramatic is happening?

### Scenario B: Clustering
- N players grouped in one area.
- M entities distributed evenly.
- Tests spatial locality — the optimizer should be able to aggressively defer simulation far from the group.

### Scenario C: Migration
- N players moving together through the world over time.
- Tests how quickly the optimizer adapts when the "important region" shifts.

### Scenario D: Gradual stress
- Start comfortable, progressively increase entity count.
- How long does each approach hold target TPS? How does it degrade?

### Scenario E: Sudden spike
- A sudden event spawns many entities at once.
- How does the pressure controller respond? How quickly does it stabilize?

Parameters (N, M, specific entity types) should be chosen to reflect realistic loads for the target game. What's realistic for a 50-player Hytale server is very different from a 2000-player MMO.

### Scenario F: Area event

- A sudden large-scale effect (explosion, spawner burst, area spell) hits a region near a player, many entities affected at once.
- Does the optimizer treat affected entities as critical during the event? Does pressure recover cleanly after, or does it stay elevated?

### Scenario G: AFK player

- A player connected and stationary, not interacting with anything, for several minutes.
- Ambient AI, environmental physics, and anything with ongoing state still needs to tick near the player. Does the optimizer over-defer just because the player stopped moving?

### Scenario H: Teleportation

- A player teleports a large distance. The relevant region shifts instantly.
- How long before the new region is fully active? How long before the old one winds down?

### Scenario I: Autonomous mechanism

- A redstone circuit, spawner, or automated farm running in a chunk with no nearby players.
- Does the optimizer kill it? It shouldn't — these have observable effects with no one watching. If they break, the staleness limit is too long or criticality is missing a case.

## Reproducibility

For results to be useful to others:

- Document the hardware (CPU, RAM, OS, runtime version).
- Use fixed random seeds where possible.
- Provide enough detail for someone with similar hardware to approximately replicate the run, or simulate the state if results are disputed.
- Run each scenario multiple times (minimum 5, ideally 10) and report both mean and spread.
- Share raw data alongside the summary when possible.

Perfection isn't the goal here — a well-documented informal benchmark is more valuable than no benchmark at all. If you run something on your laptop with 3 trials and document it honestly, that's a contribution.

## How results are organized

```
docs/benchmarks/
├── METHODOLOGY.md          # This file
├── results/
│   ├── hytale-v0.1.md      # Results for reference implementation v0.1
│   ├── minecraft-paper-v0.1.md  # Community results for Paper reference server v0.1
│   └── ...
└── raw/                    # Raw data from all runs (CSV, JSON, as much as can be extracted)
```

## Comparison baselines

Every benchmark must compare at minimum:

1. No optimization — the server running as-is, no scheduling, every SU ticks every tick.
2. OrbisOptimizer (heuristic) — the optimizer under test.

If an existing optimization solution exists for the target game (a plugin, a built-in feature, anything), comparing against it makes the results much more interesting. But if none exists, that's fine — the no-optimization baseline is always available.

For Hytale specifically, three-way comparisons are of interest:
1. Vanilla server (no optimization)
2. Server Optimizer or HyFine (threshold-based, reactive)
3. OrbisOptimizer (AIMD scheduling, adaptive)

*Note: we're not trying to directly compete with these existing approaches. Worth remembering that this is just one implementation among many that could exist under our methodology. I'd invite the maintainers of those projects to contribute here — they already have valuable hands-on experience with Hytale. Down the line, once the Hytale implementation matures, the idea is for there to be a shared API that everyone can build on — with a layer in front so others can develop implementations with different approaches, on top of the technical foundation we're building together.*

---

*The methodology will evolve as we learn what's actually worth measuring. If you think we're measuring the wrong things or something important is missing, open an issue.*
