# Devlog #003 — What the Hytale docs actually say

> Date: 2026-04-01
> Phase: API research

I spent time going through the Hytale ECS documentation properly. Several things I'd assumed turned out to be wrong. This devlog is about those things, what the correct picture looks like, and what changed in the spec because of it.

---

## What was wrong

### TPS is 30, not 20

I'd been assuming 20 TPS because that's the Bukkit/Spigot default. Hytale runs at 30 by default. `TickingThread.TPS = 30`. That changes the tick window from 50ms to 33.3ms, and the staleness limit I'd been using (200 ticks for a 10-second target) is actually 300.

More importantly: `world.setTps(int)` exists, takes values between 1 and 2048, and can be called while the server is running. So the budget isn't a compile-time constant. `TickBudget` has to read `world.getTickStepNanos()` on every tick, and the staleness limit in ticks needs to be computed as `targetSeconds × currentTPS` rather than hardcoded anywhere.

### The ECS is not Flecs

I'd been saying "Flecs" throughout the model and architecture docs. That was wrong — Hytale has its own ECS, not a Flecs integration.

The class hierarchy:
- `TickingSystem` — base; `tick(float dt)`
- `EntityTickingSystem` — entity-level; `getQuery()` returns `Query<EntityStore>`
- `ArchetypeTickingSystem` — archetype-level

Systems register via `getEntityStoreRegistry().registerSystem()` in `plugin.setup()`.

`ComponentRegistry` manages the list of running systems internally. It's not in the public API, which means the list of active systems is not enumerable without bytecode injection. That's the main constraint for the profiler's signal 2. Once you have a reference to a system, `EntityTickingSystem.getQuery()` works fine without injection — but getting those references requires injection in the first place.

### `FlecsSystemReader` and `ServerTickMixin` needed fixing

`FlecsSystemReader` assumed a Flecs query API. The actual problem is getting at `ComponentRegistry`'s dispatch loop — renamed to `EcsSystemReader` in the Fase 1 architecture. The Flecs-prefixed names in the architecture objective (`FlecsEnumerator`, `FlecsSuWrapper`) are flagged for review when those classes actually get built.

`ServerTickMixin` had no concrete target. It does now: `World extends TickingThread`. The main tick loop lives in `TickingThread`, `World` is the concrete class. Renamed to `WorldTickMixin`. Also worth noting: `World.execute(Runnable)` is how you run code on the tick thread from anywhere else — relevant if any plugin logic needs world state access from a background thread.

---

## What the standard API covers

More than I expected. No injection needed for:

**Player positions (R4):**
```java
Universe.get().getPlayers()           // → List<PlayerRef>
playerRef.getTransform()              // → TransformComponent directly
store.getComponent(ref, TransformComponent.getComponentType())  // → Vector3d
```
`ChunkTracker` on `PlayerRef` also exposes loaded chunks — could be a useful proximity signal.

**Interaction events (O3):**
```java
getEventRegistry().register(PlaceBlockEvent.class, handler);
getEventRegistry().register(BreakBlockEvent.class, handler);
getEventRegistry().register(DamageBlockEvent.class, handler);
getEventRegistry().register(UseBlockEvent.class, handler);
```
All cancellable. `DamageEventSystem` gives full combat info with typed sources.

**Criticality signals (OQ-12 — resolved):**
- `DamageDataComponent.lastCombatAction` — entity in active combat if within N ticks
- `FlockMembership` + `Flock.currentDamageData` — if the flock is in combat, all members are critical
- `MountedComponent` — mount shares criticality with rider
- `InteractionChain` active on a player → target entity is critical

All four accessible through the public API. Criticality detection doesn't require injection.

Other things that don't need injection: `WorldTimeResource.isGameTimePaused()`, `BlockSection.getTickingBlocksCountCopy()`, `EntityStatMap` + `DefaultEntityStatTypes`, `SpawnBeaconReference`.

---

## What still requires injection

| Hook | Why | Target |
|---|---|---|
| System enumeration | `ComponentRegistry` is internal | `ComponentRegistry` or dispatch loop |
| Per-system timing | Nothing exposed by API | Wrap each `ISystem.tick()` call |
| Tick loop pre/post | Plugin systems run within the tick | `World.tick()` or `TickingThread` run loop |
| System deferral (future) | No API to skip system execution | Dispatch loop, `@Inject(cancellable=true)` |

For the profiler specifically: enumeration and per-system timing both need injection into the dispatch loop. That's the unresolved question that's now OQ-13.

---

## What changed in the spec

- Tick budget example: 33.3ms at 30 TPS, not 50ms at 20. Budget reads `world.getTickStepNanos()` per tick.
- Staleness default: 300 ticks for a 10-second target, computed dynamically.
- Log interval: 30 ticks (not 20) for a ~1-second interval at 30 TPS.
- `FlecsSystemReader` → `EcsSystemReader`, `ServerTickMixin` → `WorldTickMixin`.
- All "Flecs" references in the current-phase architecture updated to use actual class names.
- OQ-2 closed (staleness numbers were wrong), OQ-7 closed (ECS mapping confirmed), OQ-12 closed (criticality signals identified).
- OQ-13 opened: can Hyxin inject into the `ComponentRegistry` dispatch loop?

---

## Systems that are probably worth deferring

Not Fase 1 scope, but useful to have written down before building the scheduler:

- `FlockSystems.Ticking` — NPC flocks far from players
- `SpawnControllerSystem`, `SpawnJobSystem` — background spawning
- `FluidSystems.Ticking` — deferring this defers all fluid simulation in a chunk
- NPC instruction execution systems — safe for non-critical NPCs at distance

Not in scope at all: `ChunkLightingManager` (daemon thread, outside the tick loop) and world generation (async `CompletableFuture`, also outside).

---

## Threading

World state access must happen on the tick thread. The engine's scoring can run anywhere — snapshots are immutable records. Hyxin injections run inline, so actual deferral control is automatically on the right thread. `World.execute(Runnable)` handles everything else.

---

*Next: build the profiler skeleton. OQ-13 gets answered when `EcsSystemReader` tries to actually inject.*
