# Prototype results

The strongest result is verification across two independent methods. The unsigned division contract holds for all valid 64-bit pairs under the symbolic semantics. Exhaustive execution independently checked the corresponding 8-bit function. The token and runtime results below cover two small kernels on one machine.

## Verification evidence

| Experiment | Observed result | Scope |
|---|---|---|
| Symbolic u64 division | Four obligations discharged | Both denominators safe, exact reconstruction and remainder bound for every admitted pair |
| Exhaustive u8 division | 65,280 admitted out of 65,536 total | Both outputs, pre/post checks and termination within 10,000 block entries |
| Exhaustive summation | All 256 inputs admitted and verified | Loop invariant, postcondition and bounded termination |
| Change loop addition to subtraction | Structural validation succeeded, exhaustive check failed on input 1 | First bad loop visit detected before return |
| Inductive proof of the supplied summation graph | Unproved at the body-to-header edge | The body has no local invariant carrying the predecessor's guard and accumulator relation |
| Interpreter/native differential tests | Passed on all 256 summation inputs and 1,030 boundary/seeded u64 division pairs | Also covers generated arithmetic graphs, parallel edge transfers, trap classes and trace order |
| Concurrent edits from one revision | One commit, one stale-revision rejection | Cooperating POSIX processes editing the same function |

The u64 proof uses mathematical division with nonzero divisor and unbounded arithmetic in the contract. Native differential tests exercise the lowering separately. The solver does not verify the C compiler, the lowering implementation or its own translation. The sum example illustrates the distinction between a reachable concrete counterexample and a solver model from an underconstrained block.

## Tokens

Counts use tiktoken 0.14.0 with the `o200k_base` vocabulary. Each count includes a complete checked kernel as supplied to the tokenizer. The C source includes its ABI, includes and return-code handling. Python counts the function definition with its checks. File paths, prompts, shared runtimes and the specification are excluded.

| Kernel | Canonical JSON | First wire encoding | Final wire encoding | Hand-written C | Python |
|---|---:|---:|---:|---:|---:|
| u64 quotient and remainder | 112 | 75 | 73 | 141 | 76 |
| u8 loop with u64 sum | 244 | 163 | 156 | 223 | 184 |

The first wire format used a redundant instruction tag before each destination and opcode. Using the opcode as the record tag saved a further 2 and 7 tokens. The final wire format saves 35–36% relative to the canonical JSON and 4–15% relative to these Python implementations. The three-token Python difference on division is small enough that source spelling could reverse it. No model was asked to author or debug a program in this experiment.

### Retrieval and edit experiment

The task was to locate the accumulator update and inject a subtraction. Both protocols include a full SHA-256 revision guard. The C comparison uses a hypothetical editor capable of reading or replacing an exact line. The AXL commands were implemented and exercised. File path, shell quoting and CLI boilerplate are excluded from both.

| Payload | AXL tokens | C tokens |
|---|---:|---:|
| Whole kernel, final wire / C source | 156 | 223 |
| Selected instruction with typed direct inputs / exact C line | 73 | 55 |
| Selected instruction with transitive local dependencies | 97 | — |
| Guarded replacement payload | 45 | 48 |

The AXL node view includes operand IDs, types, block, result type and revision. The C line omits surrounding declarations. Both interfaces substantially cut context compared with loading their entire kernel. The typed AXL view costs 18 more tokens than the isolated C line. A three-token patch difference is sensitive to the tokenization of the actual hash. The prototype reads and validates the whole selected function internally, while the agent receives the requested slice.

## Runtime and resources

Median of five fresh processes per engine and kernel, with rotating execution order. A timed batch varies its inputs and accumulates a checksum. An independent Python calculation checks every sample's checksum. Native runs used GCC 14.2.0, separate translation units, C11 and -O3 with no link-time optimization. The host reported Linux x86-64 and an AMD EPYC 9V45. Python was 3.12.14. Raw samples, flags, checksums and sample counts are in [bench/results.json](bench/results.json).

| Kernel | Engine | Calls per sample | Median ns/call | Median peak RSS, KiB |
|---|---|---:|---:|---:|
| Division | AXL native | 2,000,000 | 2.79 | 616 |
| Division | Hand-written C | 2,000,000 | 2.63 | 620 |
| Division | AXL interpreter | 8,192 | 5,226 | 9,716 |
| Division | Python | 8,192 | 392 | 8,708 |
| Summation | AXL native | 150,000 | 137.83 | 620 |
| Summation | Hand-written C | 150,000 | 131.06 | 620 |
| Summation | AXL interpreter | 512 | 634,933 | 9,704 |
| Summation | Python | 512 | 10,039 | 8,696 |

The native medians were 6.1% and 5.2% slower than the C baselines. Sample ranges overlap for both comparisons. The interpreter was about 13× and 63× slower than Python. It allocates dictionaries for activation environments and walks contract expression trees repeatedly. Those are plausible contributors. The experiment did not profile their individual costs.

Timings include input generation, the language call and checksum accumulation. They exclude compilation, process startup and graph loading. Native and Python pairs each use the same host driver and input schedule within their pair. Counts differ across host languages to keep the interpreter run practical. The summation input cycles through 0–255. The C and Python loop baselines retain the contract and fuel checks. The Python harness supplies already-typed valid inputs. RSS uses Linux VmHWM for each entire worker, including its runtime and imports. The 0–4 KiB differences between native medians provide no evidence of a memory advantage. Native initiation costs, code size, energy use and editor latency were not measured.

## Architectural decisions and open questions

| Decision | Why it was made | Cost or open question |
|---|---|---|
| Function objects with explicit edges and block parameters | Stable addresses survive unrelated edits. Simultaneous edge bindings express loops without mutable locals. | One function per file. Calls, linking, heaps, aggregate types and concurrency remain open. |
| Canonical JSON plus reversible wire projection | JSON makes validation and tooling inexpensive. A separate transport can be optimized against an actual tokenizer. | The opcode vocabulary, specification and context from earlier turns also consume tokens in an agent session. |
| Stable numeric IDs | Preserve identities across patches. Content-addressing every node would rename downstream nodes after edits. | Current storage has no persistent node index. Function revision guards cause independent edits within a function to conflict. |
| Python reference engine and C lowering | Python integers give exact contracts. C provides an optimizing native toolchain and a small callable ABI. | Two execution paths require differential tests. Native contracts with conservative intermediate bounds above 128 bits are rejected. |
| Exhaustive execution plus inductive SMT checks | Small domains give replayable faults. Induction handles full-width types and abstract loop states. | Exhaustion scales with the input product. SMT needs adequate block invariants, can time out, and establishes partial correctness. |
| Typed trace as the only effect | Gives observable ordering and trap behavior while keeping execution deterministic. | The proof language does not express trace properties. Storage, clocks and external I/O require additional semantics. |

A next experiment would give agents the same held-out edit tasks in AXL and a conventional language, then measure all input/output tokens, successful repairs, retries and wall time. Larger kernels, additional tokenizers, inter-function graph retrieval and independently checked native output would test whether these small-kernel results generalize.
