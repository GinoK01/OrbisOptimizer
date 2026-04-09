# Changelog

All notable changes to the Hytale implementation will be documented here.

Format: [Version] — [OrbisOptimizer Model Spec Version] — [Date]

---

## Unreleased

### Phase 1 — passive profiler

- `OrbisEarlyPlugin`: plugin entry point wiring `PassiveProfiler` lifecycle.
- `PassiveProfiler`: background poller (1 s interval) collecting signals S1–S5 from all active worlds and emitting them as structured log lines (`key=value`).
- `TickTimingAccumulator`: reads `bufferedTickLengthMetricSet` from `TickingThread` — clean tick timing without bytecode injection.
- `EcsSystemReader`: attempts reflection on `ComponentRegistry.systemSize` via `EntityStore.REGISTRY`. Returns -1 and logs a warning if inaccessible (OQ-13 pending).
- Signals S3 (rel_dist) and S4 (staleness_hits) are logged as `PLACEHOLDER` — scoring and staleness tracking are Phase 2.

---

*Each release references the OrbisOptimizer Model Spec version it implements.*
