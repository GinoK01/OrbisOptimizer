---
name: Benchmark Results
about: Share benchmark data — confirm or challenge the approach with real numbers
title: "[BENCH] "
labels: benchmark
assignees: ''
---

**Game / engine**
(e.g. Hytale Early Access build XXXX, Minecraft Paper 1.21)

**OrbisOptimizer version**
(e.g. v0.1.0, main@abc1234, custom fork)

**Hardware & environment**
- CPU: 
- RAM: (total / allocated to server)
- OS: 
- Java: 
- Other plugins: 

**Scenarios tested**
Check which standard scenarios you ran (see [METHODOLOGY.md](docs/methodology/METHODOLOGY.md)):

- [ ] A — Steady state
- [ ] B — Clustering
- [ ] C — Migration
- [ ] D — Gradual stress
- [ ] E — Sudden spike
- [ ] F — Area event
- [ ] G — AFK player
- [ ] H — Teleportation
- [ ] I — Autonomous mechanism
- [ ] Custom scenario (describe below)

**Test parameters**
Player count, entity count, number of runs, controller settings, etc.

**Results**

| Metric | No optimization | OrbisOptimizer | Other (if applicable) |
|---|---|---|---|
| TPS (sustained) | | | |
| MSPT P50 | | | |
| MSPT P95 | | | |
| MSPT P99 | | | |
| Scheduling overhead | — | | |
| Memory overhead | — | | |

**Observations & interpretation**
What did you notice? Anything surprising? Did the game feel different to players?

**Raw data**
Paste CSV/JSON data or link to files. If you can PR it into `docs/benchmarks/raw/`, even better.
