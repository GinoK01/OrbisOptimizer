# Devlog #002 — Pinning down observability

> Date: 2026-03-31
> Phase: Spec cleanup and first hard decisions

## Context

Devlog 001 said observability was the differentiator: real-time dashboard, nobody does this. Still true. But I'd been writing about Layer 3 like it was a spec when it's really just a direction. The actual scope depends on what Hyxin and Flecs expose at runtime, and I don't know that yet. Can't know it without running code first.

So I needed to stop treating it as a known thing and write it down as the open question it actually is.

## What I did today

Cleaned up the spec anywhere it was pretending observability was settled.

Main target: spec/model.md section 5. It said "any implementation must expose enough information to understand what it's doing," listed five signals, and ended with "this isn't about building a dashboard." That's not wrong, but it implies the scope is known. It isn't. Rewrote it as an explicit exploration — those five signals (pressure level, SUs simulated vs. deferred, score distribution, staleness hits, tick budget utilization) are a floor, and what sits above that floor gets defined after the exploration, not before.

Updated Layer 3 in implementations/hytale/README.md to match. The feature list is still there but now labeled as working direction with a note on why the scope is open.

Also wrote down three decisions that had been quietly made but never documented: criticality is a pre-scoring bypass, not a weighted signal in R(). SU = ECS system for Flecs, provisional. Zero-config confirmed as option A — starts active, pressure at zero. These were answered questions I'd been carrying in my head. They needed to be in the spec.

## Decisions made

D6: Observability is an open exploration first, Layer 3 second. Five baseline signals get verified against what Hyxin and Flecs actually expose. Full surface gets defined after that. The exploration will probably surface observable contexts that aren't in the current spec — that's expected, that's why it's an exploration.

D7: Everything that depends on D6 stays undefined until D6 is done. Dashboard design, export format, commands — all of it. No point specifying what to show before knowing what data is available.

## What's next

- Start the Layer 1+2 skeleton. Passive profiler: hook the tick loop, collect the five baseline signals, log them somewhere readable. That's it.
- See what Hyxin actually gives for tick timing and system enumeration.
- Layer 3 scope gets drafted once something is running.

---

*This devlog is part of the [OrbisOptimizer project](../README.md). Updates come when there's something worth sharing, not on a schedule.*
