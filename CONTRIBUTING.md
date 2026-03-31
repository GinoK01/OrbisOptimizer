# Contributing to OrbisOptimizer

OrbisOptimizer is an exploration, not a finished product. The most valuable contributions right now aren't code — they're ideas, questions, real-world experience, and honest results.

## How you can help

### Share your experience

If you've worked on server optimization for any game — as a modder, server admin, or professional developer — your experience is relevant. What worked? What didn't? What did you try that nobody ever documents? Open a discussion or write it up and send a PR to the devlog.

### Test the approach

Take the model from [spec/model.md](spec/model.md) and try applying it to a project you know. It doesn't have to be a full implementation — even mapping the four concepts (Simulation Unit, Relevance Function, Tick Budget, Pressure Controller) to your game or engine and documenting what fits and what doesn't is an extremely useful contribution.

If you get something working, you can add it under `implementations/community/your-engine/` with a README explaining the context.

### Challenge the model

The model is a draft. If something seems wrong, incomplete, or doesn't apply to your case, it's important to know. Open an issue with:

- What scenario or workload doesn't fit.
- What's missing or incorrect.
- A concrete example, ideally from something real.

### Run benchmarks

The [benchmark methodology](docs/methodology/METHODOLOGY.md) defines how we measure things. If you can run tests against any game server — even informal ones — and share the results with enough detail for someone to understand the context, that's valuable information.

We're interested in results that confirm the approach just as much as results that challenge it.

### Improve the documentation

If something in the spec or docs is confusing, poorly explained, or assumes too much prior knowledge, fixing it is a real contribution. The project is only useful if people can actually understand it.

## Guidelines

- **English** for repository content, but discussions in other languages are accepted in issues.
- **Show your work.** Performance claims need data. Claims about the model require concrete scenarios. "I think X would be faster" is just a starting point for investigation — fine as an initial contribution, but not a conclusion.
- **It's okay not to know.** This is an exploration. Tentative ideas, partial implementations, and open questions are welcome. Use the prefix "Not sure about this, but..." if you want — that's perfectly fine.
- **Be constructive.** If something doesn't work or seems wrong, explain why and suggest an alternative if you have one. The goal is to build a guide together.

## Structure for implementations

If you contribute an implementation for a specific game or engine:

```
implementations/community/your-engine/
├── README.md          # Context: what the engine exposes, what tradeoffs you made
├── src/               # The implementation
└── benchmarks/        # Results, ideally following the standard methodology
```

## Development environment

The reference implementation will use Java (targeting Hytale's modding API). Community implementations use whatever the target engine requires.

Setup instructions specific to each implementation will be in that implementation's directory.

## Opening issues

Labels are set up on the repository. Use whichever fits, and don't worry too much about it — a mismatched label is fine:

- `model` — questions or challenges to the spec
- `implementation` — bugs, improvements, or new implementations
- `benchmark` — data, methodology questions, or results
- `question` — anything you're curious about
- `idea` — something that might be worth exploring

There are a few open issues already tracking the known open questions from the spec. If you want to contribute but aren't sure where to start, those are a reasonable entry point.

## Code of conduct

Be direct, be curious, be kind. Technical disagreement is healthy. We're here to learn.
