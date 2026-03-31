# Open questions

> Status: Living document — questions get added when they're discovered, removed when they're resolved.

These are the things we don't have answers to yet. Some will get resolved through implementation and benchmarking. Some might turn out not to matter. If you have experience or data on any of them, open an issue.

---

## Model

**OQ-1: Is a single relevance score enough?**
Or do some games need multidimensional scoring — e.g., separating visual relevance from gameplay relevance, or short-term urgency from long-term importance? A scalar is simpler to reason about, but it might collapse distinctions that matter.

**OQ-2: What's the right staleness limit?**
The current default (200 ticks / 10 seconds at 20 TPS) is a guess. Is a fixed limit even the right abstraction, or should it adapt based on SU type, current pressure, or something else? What's the actual worst case when a low-relevance entity accumulates stale state?

**OQ-3: Does the four-concept model hold for zone-scale MMO servers?**
The model was designed with voxel sandboxes in mind. A 2000-player MMO with zone-based ticking and global event systems might need something structurally different — or the same model with very different granularity choices.

---

## Relevance function

**OQ-4: What's the minimum viable relevance function?**
Does proximity alone get 80% of the benefit with 20% of the complexity? Would starting with just `R(su) = proximity(su, nearest_player)` and adding signals only when benchmarks show a gap be better than the full four-term formula from day one?

**OQ-5: How should the relevance function handle SUs with long-range effects?**
An explosion affects things far from the player who triggered it. Proximity scoring gives those things low relevance right up until they matter. Is criticality the right override, or does this need its own mechanism?

**OQ-6: How much does the spatial data structure choice matter at scale?**
At 10,000+ SUs, proximity scoring needs some form of spatial indexing. Grid vs. k-d tree vs. chunk-based lookup — does it actually matter for overall scheduler overhead, or does the scoring logic dominate regardless?

---

## ECS / Flecs

**OQ-7: How do Simulation Units map to Flecs archetypes and systems?**
*Provisionally answered — see model.md section 6.*
SU = ECS system for the initial implementation. System-level deferral, system-level scoring. This is the starting point for benchmarks; per-entity granularity (OQ-8) gets explored if system-level turns out too coarse.

**OQ-8: What's the overhead of per-entity tags for fine-grained deferral in Flecs?**
Adding or removing a `Deferred` component tag moves an entity between archetypes, which has a real cost in Flecs. Is per-entity deferral practical, or does it need to be batched to avoid creating more overhead than it saves?

---

## Pressure controller

**OQ-9: Are 0.85 / 0.60 the right thresholds for game servers?**
These are adapted from TCP, which operates in a very different environment. Game server tick loads have different variability characteristics than network traffic. Do these thresholds need to be calibrated per game, or is there a reasonable universal starting point?

**OQ-10: Is multiplicative_factor = 0.5 too aggressive for recovery?**
It halves pressure every comfortable tick — from ceiling to near-zero in about 5 ticks (250ms). That might be too fast if the comfortable period is just a brief lull between spikes. Or it might be fine. Benchmarks will tell.

---

## Implementation

**OQ-11: Can OrbisOptimizer coexist cleanly with Catalyst?**
Catalyst does ASM bytecode transformation at startup. OrbisOptimizer uses Hyxin (Mixin-based) injection at runtime. Are there known conflict patterns between these two approaches in Hytale's early plugin layer?

**OQ-12: How should criticality be defined for Hytale's interaction system?**
Hytale's interaction model includes combos, charging attacks, and fork states. At what point does an entity enter a state that should be treated as critical? The safe answer is "any direct player involvement," but that might be too conservative.

---

*When a question is resolved, move the answer to the relevant spec document and remove it from here. Leave a one-line note: "OQ-X resolved — see [file]."*
