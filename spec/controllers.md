# Pressure Controller Strategies

> Status: Draft — strategies described, not yet validated against real game server loads.

## Overview

The Pressure Controller decides how aggressively the scheduler should defer simulation work, based on how much of the tick budget is actually being consumed. This document catalogs the strategies we're considering and what we know (or think we know) about their tradeoffs.

The model spec introduces AIMD as the initial strategy. This document goes deeper into that choice and describes alternatives worth exploring.

---

## What a controller must do

Given:
- `load_factor`: fraction of the tick budget consumed in the last tick (0.0–1.0+)
- `pressure`: current deferral aggressiveness (0.0 = defer nothing, 1.0 = defer everything below the ceiling)

Produces:
- Updated `pressure` for the next tick

The controller must:
1. React quickly to stress; when load rises, increase pressure fast enough to avoid tick overruns.
2. Recover conservatively; when load drops, reduce pressure slowly to avoid oscillations.
3. Avoid hunting; don't oscillate between high and low pressure under stable load.
4. Be predictable; operators should be able to reason about what the controller will do.

---

## Strategy 1: AIMD (Additive Increase, Multiplicative Decrease)

The initial strategy, taken from TCP congestion control.

```
if load_factor > 0.85:
    pressure = min(ceiling, pressure + 0.05)    # additive increase
if load_factor < 0.60:
    pressure = max(0.0, pressure × 0.5)         # multiplicative decrease
else:
    pressure = pressure                         # hold
```

Why AIMD:
- Well-understood behavior from decades of TCP research and deployment.
- Asymmetric response (fast increase, slow recovery) is appropriate when the cost of overshooting (tick overrun) is much greater than undershooting (over-deferring).
- Conservative recovery avoids oscillations.

Parameters:
| Parameter | Default value | Effect |
|---|---|---|
| `high_threshold` | 0.85 | Load fraction that triggers pressure increase |
| `low_threshold` | 0.60 | Load fraction below which pressure decreases |
| `additive_step` | 0.05 | How much pressure increases per stressed tick |
| `multiplicative_factor` | 0.5 | How fast pressure recovers (0.5 = halved each comfortable tick) |
| `ceiling` | 0.9 | Maximum pressure (never defer everything) |

Known tradeoffs:
- Slow recovery means the optimizer stays more aggressive than necessary after a spike. This wastes simulation capacity.
- The hold zone (0.60–0.85) prevents unnecessary pressure changes at borderline loads, but means small load changes don't take effect immediately.
- `multiplicative_factor = 0.5` means pressure halves each comfortable tick — in 10 quiet ticks, it falls to ~0.1% of its peak. That may be faster than desired.

Open questions:
- Is 0.85/0.60 the right band for game servers? TCP uses different thresholds depending on the network profile.
- Should the additive step be proportional to how far the threshold is exceeded?

---

## Strategy 2: Stepped thresholds

A simpler, more predictable approach: define discrete pressure levels with explicit transitions.

```
if load_factor > 0.90:   pressure = 0.8   # critical
if load_factor > 0.80:   pressure = 0.5   # stressed
if load_factor > 0.65:   pressure = 0.2   # moderate
else:                    pressure = 0.0   # comfortable
```

Why consider it:
- Extremely predictable; operators know exactly what the optimizer will do at each load level.
- No gradual drift; pressure jumps to well-defined values.
- Easier to debug: "the server was in stressed mode for 3 minutes" is clearer than "average pressure was 0.47."

Known tradeoffs:
- Jumps can cause visible changes if the server oscillates near a threshold.
- Less sensitive to gradual load increases; pressure doesn't change until a threshold is crossed.
- Requires careful threshold calibration per game/server.

Hysteresis (recommended improvement):
Use separate thresholds for entering and leaving a level:
```
# Enter "stressed" at load > 0.80, exit only when it drops below 0.70
```
This prevents rapid oscillation near boundaries.

---

## Strategy 3: PID controller

A classical control approach: use proportional, integral, and derivative terms to drive load toward a target.

```
error = load_factor - target_load     # e.g., target 0.70
P = Kp × error
I = Ki × integral(error, dt)
D = Kd × derivative(error, dt)
delta_pressure = P + I + D
pressure = clamp(pressure + delta_pressure, 0.0, ceiling)
```

Why consider it:
- Can be tuned to converge quickly with minimal overshoot.
- Handles gradual load drift well.
- Well-studied in control theory; extensive resources on Kp/Ki/Kd tuning.

Known tradeoffs:
- Requires tuning three parameters (Kp, Ki, Kd); getting it wrong can cause oscillations or sluggishness.
- The integral term can "wind up" under sustained overload, causing overshoot on recovery.
- More complex to reason about than AIMD or thresholds.
- Game server tick loads are quite different from the continuous physical systems PID was designed for.

When it might beat AIMD:
- If you want to maintain a specific load target (e.g., "stay at 70% utilization") rather than just responding to thresholds.
- If AIMD oscillates under certain game-specific load patterns.

---

## Strategy 4: Exponential moving average (EMA) smoothing

Not a standalone strategy — it's a modifier for any of the above. Instead of reacting to instantaneous load, react to a smoothed average:

```
smoothed_load = alpha × current_load + (1 - alpha) × smoothed_load
```

Where `alpha` controls responsiveness (higher = faster, lower = smoother).

Why use it:
- Game server loads are noisy. An expensive tick (worldgen, chunk loading) can spike load_factor to 2.0+ for one tick and then return to normal.
- Without smoothing, AIMD or thresholds would react to a single-tick spike and then spend many ticks recovering.
- EMA filters transient spikes while still following sustained trends.

Tradeoff:
- Slower reaction to genuine load increases. Choose `alpha` based on acceptable latency vs. noise tolerance.
- Suggested value: `alpha = 0.2` (20% weight on current tick, 80% on history).

---

## Comparison summary

| Strategy | Reaction speed | Recovery speed | Predictability | Tuning complexity |
|---|---|---|---|---|
| AIMD | Fast | Slow (intentional) | Medium | Low (4 params) |
| Stepped thresholds | Instant at boundary | Instant at boundary | High | Low (threshold values) |
| PID | Adjustable | Adjustable | Low | High (3 params + stability) |
| EMA + any | Smoothed | Smoothed | Same as base | +1 param (alpha) |

---

## Choosing a strategy

For the reference implementation, AIMD is the starting point. It's conservative, well-understood, and prioritizes stability. The goal is to validate it against real Hytale loads before exploring alternatives.

If benchmarks show:
- Oscillation: try EMA or switch to stepped thresholds.
- Slow reaction to spikes: raise the additive step or lower the high threshold.
- Excessive deferral after spikes: lower the multiplicative factor (e.g., 0.7 instead of 0.5).
- Need to maintain a specific utilization target: consider PID.

---

*These strategies are theoretical until validated against real loads. If you have data on any of them, share it.*
