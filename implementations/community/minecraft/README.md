# OrbisOptimizer — Community implementation for Minecraft (Paper)

> Status: Placeholder — no implementation yet.

## Overview

This directory is reserved for a community implementation of OrbisOptimizer targeting Minecraft Paper servers.

If you're interested in building this, read the [contribution guide](../../../CONTRIBUTING.md) and the [adapter contract](../../../spec/adapter-contract.md) to understand what needs to be hooked.

## Why Paper/Folia?

Paper and Folia expose a reasonably complete plugin API with:
- Entity tick interception via `EntityTickEvent` (experimental)
- Chunk tick hooks via `ChunkTickEvent`
- Async profiler integration
- Per-player view distance control

The adapter contract should be mostly satisfiable through Paper's plugin API, without bytecode injection.

## Adapter contract status

| Capability | Status | Notes |
|---|---|---|
| R1: Enumerate SUs | Unknown | Entity list + chunk list available via API |
| R2: Tick timings | Unknown | spark's profiler API may expose this |
| R3: Deferral control | Unknown | EntityTickEvent cancellation (experimental) |
| R4: Player positions | ✅ Available | Standard Bukkit API |
| O1: Tick hooks | Unknown | Scheduler hooks available |
| O2: Per-phase timings | Unknown | |
| O3: Interaction events | ✅ Available | PlayerInteractEntityEvent etc. |
| O4: State notifications | Unknown | |

## HeuristicOptimizer Folia

There's already a plugin for these platforms with a model similar to what we're building here. That plugin was written by me, Gino, though I don't think it applies the model with the depth we're aiming for. I'd like to eventually contribute to something more advanced. If anyone wants help with that — documentation, specific methods, or implementation details — I can provide it so you can build a deeper optimization engine than HO on your own, following the model this repository lays out. Full admin documentation for the plugin is available at https://ginoarzaga.com/en/heuristic-optimizer

---

*Contributions welcome. See [CONTRIBUTING.md](../../../CONTRIBUTING.md).*
