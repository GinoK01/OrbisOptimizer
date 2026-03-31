# Devlog #001 — Starting from zero

> Date: 2026-03-29
> Phase: Understanding the landscape

## Context

I've spent a good amount of time optimizing Minecraft servers, and the patterns I keep coming back to are always the same: score entities by how relevant they are to players, defer the ones that don't matter right now, and use a feedback loop to decide how aggressive to be about it. It's not complicated — it's the kind of engineering that's been standard in networking and operating systems for decades.

I want to try the same approach for Hytale, but this time I want to do it differently. Instead of just building a plugin, I want to document the entire process — the model, the decisions, the tradeoffs, the things that don't work — so that someone building a server optimizer for a completely different game can follow along and adapt the approach.

That's what this project is.

## What I did today

I wrote the initial project structure and documentation:
- A model spec defining the four core concepts (Simulation Unit, Relevance Function, Tick Budget, Pressure Controller).
- Pattern documents for the relevance function and controller strategies.
- An adapter contract defining what Hytale's mod layer needs to expose.
- A benchmark methodology so we know how we'll measure results before we start measuring.
- A contribution guide, because I want this to be a community project, not a solo one.

No code yet. That's intentional; I want the model to be reviewed and questioned before starting to implement. Writing code is easy. Writing the right code is what requires thinking.

## Research findings on Hytale

Time spent understanding the modding surface:

There are three levels of modding:
- Asset Packs (JSON, textures, no code)
- Plugins (Java, `JavaPlugin`, events/commands — Bukkit-style)
- Early Plugins / Bootstrap Mods (bytecode transformation via ASM before the server starts — Forge/Fabric-style)

The third level is what OrbisOptimizer needs. Two frameworks are available:
- Hyxin — Mixin runtime ported from FabricMC. Maintained by Darkhax and Jaredlll08.
- MixinTale — Harmony-style patching with Prefix/Postfix/Replace semantics.

Hyxin's `@Inject` annotations can hook into the tick loop. `@Redirect` and `@Inject(cancellable=true)` can intercept individual entity ticks.

**Key architectural finding:** Hytale uses Flecs (an ECS framework). This means entities are IDs + components, not objects with update methods. The scheduler needs to operate at the ECS system level, not the entity level. This partially invalidates the current spec's approach and needs to be addressed.

**Competitive landscape:**
| Project | Approach |
|---|---|
| Server Optimizer (32K+ downloads) | Adaptive TPS/memory, view distance, AI tick rates |
| Catalyst | ASM bytecode, static optimizations |
| HyFine (4.9K+) | Presets: LOW/BALANCED/ULTRA |
| HyOptimizer (11.9K+) | Entity cleanup, memory leaks, metrics |
| spark (16.3K+) | Profiler only — diagnoses, doesn't optimize |

Every existing optimizer follows the same pattern: monitor a metric → cross a threshold → apply a predefined action. Static thresholds, reactive responses.

OrbisOptimizer's differentiator: continuous relevance scoring + adaptive AIMD scheduling + real-time observability dashboard. The observability angle is the biggest gap in the market — no existing optimizer for any voxel game shows what it's doing, where, why, and how much it's saving. That's the hook.

That said — "continuous relevance scoring" is a theory until benchmarks show it actually holds target TPS longer than Server Optimizer's threshold approach under real load. The dashboard is genuinely novel, but it's a second differentiator: it makes the tool more trustworthy, not better. The bet is that both hold up. The benchmark is what settles it.

## Decisions made

D1: Full implementation first, not API first. The community adopts things that work. A good implementation will produce an organic API when others want to build on top of it.

D2: Zero required configuration. Install the JAR, it works. The optimizer starts active with sensible defaults — no setup, no calibration step. AIMD starts at zero pressure and ramps up only if the server actually needs it, so on a healthy server it's basically invisible. Configuration exists for those who want it; it's not required for those who don't.

D3: Observability as the central differentiator. Real-time visual dashboard showing what's being optimized, where, and how much. Nobody does this.

D4: Occupy empty space. Not "another optimizer." A scheduling system with observability. The existing 32K-download optimizer owns the "quick preset" space — we're not competing there.

D5: Hytale as first target. Java (compatible with my expertise), new ecosystem in early access since January 2026 (window of opportunity), early plugins give deep access to the tick loop.

## Open questions I'm left with

These are tracked in [spec/open-questions.md](../spec/open-questions.md). The Hytale-specific ones from today: OQ-7 (SU/Flecs mapping), OQ-6 (scoring overhead at scale), OQ-1 (scalar vs multidimensional), OQ-11 (Catalyst coexistence), OQ-12 (criticality for Hytale interactions).

## What's next

- Sketch the Java project structure for the three-layer architecture (engine, adapter, observability).
- Investigate what Hyxin injection points look like for the ECS tick loop.
- Start the reference implementation skeleton — passive profiler first.

---

*This devlog is part of the [OrbisOptimizer project](../README.md). Updates come when there's something worth sharing, not on a schedule.*
