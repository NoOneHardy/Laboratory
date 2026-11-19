# AX — Agent Execution Language

Experimental programming language intended for AI agents as sole source authors and maintainers. Humans specify desired behavior and acceptance criteria, not source syntax.

## Objectives
- Minimize actual model tokens needed to inspect, generate and modify programs; measure using real tokenizers rather than character counts.
- Precise, deterministic semantics, explicit types and effects.
- Machine-verifiable contracts and invariants; distinguish proven properties from runtime checks and tests.
- Efficient native execution, predictable memory usage and optimization opportunities.
- Canonical structured instruction graph as source of truth; stable node IDs, dependency-based retrieval and atomic graph edits. Text is an optional interchange encoding, not a human-readability requirement.

## Codex implementation task
Implement a runnable v0 prototype entirely within this directory. Inspect existing repository instructions first. Choose a practical host language and document the choice. Start with a versioned typed SSA-like graph representation (JSON is acceptable initially), integer and boolean operations, functions, control flow and explicit return values. Specify integer overflow, division, evaluation order and error semantics. Implement structural/type validation, an interpreter, and pre/postcondition checking. Reject invalid graphs, type errors and invalid references. Use an exact unsigned integer division example with nonzero divisor and quotient/remainder correctness; avoid overflow in specification arithmetic. Add deterministic tests and commands to run them.

Next, if feasible, add a compact canonical encoding, a graph-patch interface and a native backend (e.g. via LLVM or C lowering); keep interpreter as reference semantics. Benchmark token counts against equivalent conventional code, including context and edit costs, and runtime against equivalent compiled code. Do not claim benefits without measurements. Clearly separate static proofs, dynamic checks and unimplemented ideas. Do not optimize for human source readability or modify unrelated files. Deliver a tested prototype and a short report of limitations, design choices and next steps.

## Initial illustrative notation (not normative)
```
F0(u64,u64)>u64
?ne($1,0)
%0=div($0,$1)
^%0
```
Numeric references are illustrative; choose encoding empirically. The graph, not this syntax, is authoritative.
