
# OrbisOptimizer

**Exploring server simulation optimization through deterministic heuristics.**

OrbisOptimizer is an open exploration of how far well-designed heuristic systems can go when optimizing game server performance, starting with Hytale as a real, working project.

This isn't a finished framework. It's a living project where the optimization work, methodology, and lessons learned are documented openly so that anyone — modders, server admins, game developers, or curious engineers — can learn from the process, replicate the approach in their own projects, or contribute directly.

---

## Table of contents

- [What is this?](#what-is-this)
- [Why servers?](#why-servers)
- [Why heuristics?](#why-heuristics)
- [The model](#the-model)
- [What's in the repository?](#whats-in-the-repository)
- [Current status](#current-status)
- [How to get involved](#how-to-get-involved)
- [A note on scope](#a-note-on-scope)

### Suggested reading order

First time here: **README → [spec/model.md](spec/model.md) → [devlog/](devlog/) → [CONTRIBUTING.md](CONTRIBUTING.md)**

Already familiar with the model and want to contribute: **[spec/adapter-contract.md](spec/adapter-contract.md) → [docs/methodology/METHODOLOGY.md](docs/methodology/METHODOLOGY.md) → whichever implementation interests you**

---

## What is this?

At its core, OrbisOptimizer is a practical experiment: can we build a server-side optimizer for Hytale using only classical techniques — spatial partitioning, adaptive tick scheduling, heuristic relevance scoring, feedback-based load control — and document the entire process so others can do the same for their own games?

The project has two sides:

1. A real optimizer for Hytale servers. Working code, real benchmarks, measurable results.
2. An open knowledge base. Every design decision, tradeoff, dead end, and lesson is documented. If you want to build something similar for Minecraft, Rust, Valheim, or your own project, the process should be reproducible from what's written here.

## Why servers?

Game servers are real-time environments with strict execution budgets: every tick must complete in milliseconds, regardless of how many entities are active or how many players are connected. Unlike the client, the server can't just drop graphics quality under load — it has to decide what to simulate and what to defer, with direct consequences for the player experience. That technical challenge is something I've always wanted to tackle, and since I have some background in optimization, it felt worth making public — to sharpen my own thinking and, along the way, help others working on similar problems.

It's also the side that the modder and indie community tends to neglect. There's an enormous amount of literature on rendering, shaders, and client-side optimization; far less on simulation load management, entity prioritization, or pressure control in multiplayer servers. That's the space this project explores.

Beyond the technical challenge and the general neglect of this area — with exceptions like games-as-a-service — I think any project should be optimizable regardless of domain: client, server, games, banking, or a calculator. Being aware of the resources our projects demand from users is something people care less and less about, and it's often just as important as everything else. Fewer resources means more profit, stability, observability, and control; it also expands what we can build and manage as programmers, engineers, or designers.

## Why heuristics?

Not because other approaches are wrong, but because heuristics have proven to work better in this domain than almost anything else, with some exceptions. They also have properties worth investigating seriously:

- They're transparent. When something breaks at 3AM, you can trace exactly why the scheduler made a decision. The logic is in the code, not in a black box.
- They're cheap. A comparison and a branch cost almost nothing. That matters when you're optimizing a 50ms tick budget.
- They're deterministic. Same state, same decision. You can reproduce bugs, write meaningful tests, and reason about behavior.
- They build on solved problems. TCP congestion control, OS process scheduling, garbage collection — all are heuristic systems refined over decades. The patterns transfer.

Whether these properties make heuristics the best option for every game server scenario is exactly what we want to find out, and whether there are currently more efficient methods that could replace them in some contexts. There may be cases where other techniques work better. That's fine — documenting where heuristics fail is just as valuable as showing where they shine.

## The model

OrbisOptimizer organizes the optimization problem around four concepts:

| Concept | What it means | Example |
|---|---|---|
| Simulation Unit | A piece of server work that can be scheduled independently | An entity, a chunk, an ECS system |
| Relevance Function | A heuristic that scores how important a unit is right now | Based on distance to players, recent interaction, state activity |
| Tick Budget | Time available per server tick for simulation | 50ms at 20 TPS, minus fixed overhead |
| Pressure Controller | A feedback loop that adjusts optimization aggressiveness based on actual load | AIMD, stepped thresholds, etc. |

These aren't new ideas — they're shared vocabulary for talking about server optimization, so implementations across different games can compare and share patterns.

The full model is in [spec/model.md](spec/model.md). It's a draft in progress; comments and challenges are welcome.

## What's in the repository?

```
orbisoptimizer/
├── spec/                        # The optimization model — how we think about the problem
│   ├── model.md                 # Core concepts and their relationships
│   ├── relevance.md             # Patterns for scoring simulation importance
│   ├── controllers.md           # Load control strategies
│   └── adapter-contract.md      # What a game/engine must expose
│
├── implementations/
│   ├── hytale/                  # Reference implementation for Hytale (not started)
│   │   ├── README.md
│   │   └── CHANGELOG.md
│   └── community/               # Community implementations for other games
│       └── minecraft/           # Placeholder — see README for context
│
├── docs/
│   ├── methodology/             # How we measure results
│   │   └── METHODOLOGY.md
│   ├── benchmarks/              # Results from all implementations (empty)
│   │   ├── results/
│   │   └── raw/
│   └── research/                # Research and synthesis
│       └── orbit-summary-completo.md
│
├── devlog/                      # Development diary — decisions, mistakes, progress
│   └── 001-starting-from-zero.md
│
└── CONTRIBUTING.md              # How to get involved
```

## Current status

🌱 **Phase: Defining the approach**

The model spec and project structure are in place. The reference implementation hasn't started yet, and there are more open questions than answers. Good time to get involved if you want to influence the direction.

Follow progress in the [devlog](devlog/).

## How to get involved

There are many ways to contribute, and writing code is just one of them:

- Try the approach on your own project and share what worked and what didn't.
- Challenge the model; if something doesn't make sense or doesn't apply to your case, that feedback is valuable and will be addressed as soon as possible.
- Run benchmarks; reproducible data from different games and hardware is incredibly useful.
- Share your experience; if you've optimized game servers before, your knowledge improves this project.

Read the [Contribution Guide](CONTRIBUTING.md) for details.

## A note on scope

This project focuses on what happens on the server. Client-side optimization has its own challenges and techniques, but the server side tends to get less attention in the modder and indie community. That's the gap we're exploring here.

We're not claiming this is the definitive answer to server optimization. It's an approach — one based on patterns that have proven useful in other domains. Whether it works and how well for game servers is what benchmarks, implementations, and community discussion will reveal.

## License

MIT
