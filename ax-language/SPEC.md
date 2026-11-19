# AXL v0 prototype · graph format 1

## Function object

One object denotes one independently callable function. A path selects the function. Its semantic value is the following record. Brackets below denote ordered JSON arrays. An ellipsis repeats the preceding element zero or more times.

```text
Function = {v: 1, args: Decls, ret: [Type, ...], effects: Effects,
            pre: Preds, post: Preds, blocks: [Block, ...]}
Type     = "u8" | "u64" | "b"
Decls    = [[ValueID, Type], ...]
Effects  = [] | ["trace"]
Preds    = [Predicate, ...]
Block    = [BlockID, Decls, Preds, [Instruction, ...], Terminator]
Edge     = [BlockID, ValueID, ...]
Terminator = ["ret", ValueID, ...] | ["jmp", Edge] | ["br", ValueID, Edge, Edge]
```

Every field is required. Unknown and duplicate fields are invalid. IDs are unsigned 64-bit integer literals. JSON booleans, floating-point numbers and decimal strings are distinct from integer literals. Block IDs are unique. Function arguments, block parameters and instruction destinations share a second, function-wide namespace of unique value IDs. Block ID 0 is the entry and has no parameters. A function has at least one result. Unreachable blocks are permitted and undergo the same validation as reachable blocks.

Storage uses UTF-8 JSON. The canonical byte sequence sorts object keys lexicographically and blocks by ascending ID, preserves every other array order, uses decimal integers and lowercase JSON booleans, omits insignificant whitespace, and ends with one LF. The revision is the lowercase hexadecimal SHA-256 of these bytes. Reordering declarations, renumbering IDs or commuting arithmetic operands can produce a new revision even when results coincide.

## Typing and scope

Let Γ₀ map argument IDs to types. On entry to block B, Γ = Γ₀ ∪ parameters(B). For each instruction [x,op,…], in array order, all references must belong to Γ. The instruction establishes Γ[x ↦ result type] for subsequent instructions and the terminator. A value crossing a block boundary travels as an explicit edge argument or as a function argument. Every edge has exactly the destination's parameter types in the declared order. Every return has exactly the function's result types in order.

| Instruction body after destination | Operand constraints | Result |
|---|---|---|
| `"k", T, literal` | A JSON boolean for b, otherwise an integer in the unsigned range of T | T |
| `"add"/"sub"/"mul", a, b` | Two values of the same word type uW | uW, reduced modulo 2^W |
| `"div"/"rem", a, b` | Two values of the same word type uW | uW, Euclidean quotient or remainder |
| `"lt", a, b` | Two values of the same word type | b, unsigned less-than |
| `"eq", a, b` | Two values of the same type | b, value equality |
| `"and"/"or", a, b` | Two booleans | b |
| `"sel", c, x, y` | c is b, x and y have the same type T | T, x if c else y |
| `"zext", x` | x is u8 | u64 with the same magnitude |
| `"emit", x` | Any type T, declared trace capability | Appends x to the trace and returns x as T |

Instructions execute strictly in array order, including unused instructions. A select reads already computed values. A branch enters only the selected edge. Bindings on an edge are simultaneous, including self-edges. When b > 0, division satisfies a = q·b + r and 0 ≤ r < b over the natural numbers. Both division operations trap on a zero divisor. There is no implicit coercion at a graph boundary.

## State transitions and observations

A call takes typed arguments and a u64 fuel budget. Its observable behavior is an ordered sequence of trace values followed by a result tuple or a trap. The reference CLI records the trace even when the call traps.

1. Validate the call's arity and argument domains, then check preconditions in order against Γ₀.
2. Before entering each block, trap on empty fuel, otherwise consume one unit. Install incoming parameters and check block invariants in order.
3. Evaluate instructions in order. A trap stops execution immediately. Earlier trace events persist.
4. At a jump or branch, evaluate the selected edge using the current environment and continue at step 2. At a return, collect the result tuple, evaluate postconditions in order, and publish the tuple only on success.

A failed predicate traps with kind `pre`, `invariant` or `post`. Arithmetic failure uses `zero`. Exhaustion uses `fuel`. The reference interpreter also identifies the block and predicate index, instruction, or pending block. Invalid programs and invalid host arguments are errors before execution. Within a call the first failing operation wins. Loops have no implicit bound other than fuel. No global mutable state or host input is available.

## Contract expressions

```text
e ::= id | ["k", u64-literal] | ["bool", boolean] | ["out", result-index]
    | ["+", e, e] | ["*", e, e] | ["lt", e, e] | ["le", e, e]
    | ["eq", e, e] | ["and", e, e] | ["or", e, e]
```

Word references embed into mathematical naturals. Boolean references retain boolean sort. Addition and multiplication operate on unbounded naturals. Order accepts naturals, equality accepts equal sorts, and logical connectives require booleans. Each predicate root is boolean. Preconditions see function arguments. Invariants see arguments and the current block's parameters at entry. Postconditions see arguments and the actual return tuple via zero-based `out` indices. Contract expressions are pure and total. The return binding makes it possible for a verifier to detect an implementation that computes a correct quotient but accidentally returns the remainder in its place.

## Verification judgments

**Static graph validation** checks syntax, uniqueness, scopes, effects, contracts, branch conditions, edge bindings and result types before interpretation or lowering.

**Finite verification** enumerates the Cartesian product of the declared input domains when it fits the configured limit. Every admitted input must terminate within the selected fuel and satisfy every dynamic check. The result is `verified`, `vacuous` for an empty admitted set, `counterexample` with a replayable input for a failed check, or `unknown` for domain or fuel exhaustion. Enumeration order is signature order with the last argument varying fastest. This verifier checks execution using the reference interpreter.

**Inductive verification** constructs verification conditions over Z3 integers and booleans. It first checks that the typed input domain and the precondition are satisfiable. For each block it assumes the precondition, well-typed arbitrary parameters and that block's invariants. It symbolically evaluates the instructions and proves these obligations:

- The precondition establishes the entry invariant.
- Every executed divisor is nonzero.
- Under each selected edge's guard, simultaneous argument transfer establishes the destination invariants.
- Every return establishes all postconditions for its actual outputs.

Word arithmetic is reduced modulo its width. For a positive divisor the translation introduces q and r constrained by the Euclidean division law. Repeated div/rem of the same SSA operand pair share these witnesses. The negation of every obligation must be unsatisfiable to report `proved`. This establishes partial correctness and arithmetic safety by induction on block entries. Termination, available fuel and trace-content properties are outside that theorem. A satisfiable failed obligation gives a block-local model. It may describe an unreachable state when the supplied invariants are too weak. Solver timeouts also yield an unproved obligation. No runtime checks are removed after a proof.

## Agent transfer and edits

`pack` projects a validated graph to a deterministic line-framed encoding. `unpack` reconstructs and validates the canonical graph. Records begin with `AXL1`, then `A` with alternating ID/type declarations, `R` with result types and `E` with effect names. `P` and `Q` carry prefix pre/post predicates. Each `B` starts a block with ID and parameter declarations. `I` records precede instructions. Instructions use an opcode followed by destination and operands. Terminators use ret, jmp or br. A slash separates the two target-first edge lists of br. The br condition precedes both lists.

An ID's spelling is its bijective base-26 representation of ID + 1, using a through z for digits 1 through 26. Predicates use bare IDs, colon-prefixed operators of fixed arity, and decimal literal/index operands for :k, :bool and :out. Boolean literal values in this projection are 0 and 1. Binary operators consume exactly two expressions. The encoder emits one ASCII space between fields and LF after each record. JSON graph bytes remain the basis for identity.

`view FILE BLOCK` returns that block and the function interface and contracts. Adding a node ID returns one instruction and its typed operand boundary. `--deps` also follows transitive instruction operands in that block, stopping at function arguments and block parameters. A node view describes the value calculation only. It omits control-flow predecessors, neighboring effects and contracts. Retrieve the enclosing block when those matter.

`patch FILE REV EDITS` accepts a nonempty JSON array of [block ID, destination ID, replacement instruction body] records. A batch may address each existing instruction once. The editor locks the parent directory, checks the canonical revision, applies every replacement, validates the entire candidate function, then atomically replaces the file. A rejected batch preserves the old bytes. POSIX cooperative writers share this lock. Concurrent external editors bypassing it, crash durability after power loss, and multi-function transactions are outside the guarantee. A successful structural edit can violate a contract.

## Native boundary

`emit-c` lowers the graph to C11 with the GCC/Clang unsigned 128-bit integer extension. The caller supplies a correctly sized argument array and output buffer and may supply a synchronous trace callback. The entry point is:

```c
int axl(const uint64_t *args, uint64_t *out, uint64_t fuel,
        void (*trace)(uint64_t));
```

Values travel as unsigned 64-bit words. Boolean values encode as 0 or 1. A null callback discards trace events. Return codes 0 through 6 mean success, precondition, invariant, divide-by-zero, postcondition, fuel and invalid argument value respectively. Output slots are written only on success. Native diagnostics expose the trap class. The caller owns allocation, arity and pointer validity. Unsigned word operations and parallel assignments preserve graph semantics. Before lowering, a conservative arithmetic bound must fit every contract intermediate into 128 bits. An unsupported graph remains executable in the reference interpreter. Generated functions use local scalar storage and perform no heap allocation.
