# AXL: agent-only authoring, framework-owned compilation

## Architectural rule

AI coding agents exclusively read, generate, retrieve, patch and verify canonical AXL instruction graphs. They must not generate, inspect or edit Python/C output during ordinary development. Compilation, backend selection, caching, build execution and deployment belong to the harness or orchestrating framework, not to the AI model's task. Human-readable generated code is an optional diagnostic artifact for humans, not agent context.

Pipeline: human requirements → AI agent → canonical typed AXL graph → framework validation/verification → framework compilation → Python or C target → tests/deployment → structured diagnostics returned to agent as compact AXL node IDs and contract failures.

## Existing baseline

The prototype already has a Python-based interpreter, C emitter, contracts, graph edits and benchmarks. A Python interpreter is distinct from a Python source-code backend. Preserve working capabilities.

## Codex implementation tasks

1. Inspect README, SPEC, CLI, tests and benchmarks before modifying existing code.
2. Define a machine-oriented agent API for scoped graph retrieval, transactional patching, static validation and compact structured diagnostics. Stable IDs and revisions are mandatory; avoid whole-program context unless needed.
3. Define a framework/harness build API that accepts a validated AXL revision, target backend, build options and artifact destination, and returns machine-readable status, artifact identity and mapped diagnostics. The agent-facing API should not require the agent to choose or invoke compiler commands.
4. Implement standalone Python source emission (`emit-python`) alongside the existing C backend (`emit-c`). Keep both behind the framework build API. Preserve AXL fixed-width integer, overflow, control-flow, effect and contract semantics.
5. Implement deterministic output, content-addressed caching where practical, and source maps from generated backend locations to AXL node IDs. Backend error details must be translated into compact AXL-native diagnostics before reaching an agent.
6. Add differential tests across AXL interpreter, generated Python and compiled C, including boundary cases, contract failures and traces. Add end-to-end tests in which an agent-facing graph patch triggers framework-owned validation/build/test without exposing emitted source to the agent.
7. Benchmark AXL agent-context tokens (graph retrieval, patch and diagnostic loops) separately from generated artifact size, execution speed and peak memory. Compare against equivalent conventional workflows and report measured results without unsupported claims.
8. Update README, SPEC and RESULTS with the separation of responsibilities and implementation status. Consider LLVM later, not as a prerequisite.

## Non-goals

Do not make agents write Python/C, parse compiler output directly, or maintain generated code. Do not optimize only for compact source syntax; the canonical graph and its targeted editing protocol are the product.

## Acceptance criteria

Agent interaction remains AXL-only; the framework can build to Python and C without agent involvement; diagnostics map to AXL IDs; generated code is disposable; existing tests pass and new cross-backend and end-to-end tests demonstrate equivalence.
