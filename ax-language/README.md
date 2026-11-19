# AXL — Agent Execution Language

AXL is an experimental programming language for AI agents as sole source authors and maintainers. Humans specify desired behavior and acceptance criteria. The prototype makes one typed instruction graph the unit of execution, verification, retrieval and revision.

A function lives in one `.axl` file. An agent can request a single instruction with typed dependencies, replace it by stable ID, and check the resulting function against its contracts. A Python interpreter defines execution. A C backend produces native functions with the same checked arithmetic, control flow and trace behavior.

## Run an example

Run these commands from `ax-language`. The core needs Python 3.10+ on POSIX. The native examples and tests need a C11 compiler with unsigned 128-bit integers. The benchmark runner also uses Linux procfs.

```sh
python3 axl.py check examples/div64.axl
python3 axl.py run examples/div64.axl '[18446744073709551615,2]'
python3 axl.py verify examples/div8.axl
python3 axl.py run examples/sum.axl '[255]'
python3 axl.py run examples/trace.axl '[true,42,99]'
```

The first division returns `[9223372036854775807,1]`. A zero divisor fails the precondition. The postcondition checks the actual quotient and remainder in mathematical integers. The summation returns `[32640]` and checks its invariant on every loop visit.

## Inspect, edit and verify

This example injects a subtraction into a disposable copy of the summation function. The graph passes static typing. Verification finds the first failing input, `[1]`, and identifies the loop invariant.

```sh
mkdir -p .work
cp examples/sum.axl .work/edit.axl
python3 axl.py view .work/edit.axl 2 11 --deps
rev=$(python3 axl.py check .work/edit.axl | python3 -c 'import json,sys; print(json.load(sys.stdin)["rev"])')
python3 axl.py patch .work/edit.axl "$rev" '[[2,11,["sub",7,10]]]'
python3 axl.py verify .work/edit.axl
```

The last command exits with status 1 for the counterexample. Each successful patch publishes one new revision atomically. Stale revisions and ill-typed batches are rejected.

## Compile and exchange

```sh
mkdir -p .work
python3 axl.py emit-c examples/div64.axl > .work/div.c
cc -std=c11 -O3 -shared -fPIC .work/div.c -o .work/div.so
python3 axl.py pack examples/div64.axl > .work/div.wire
python3 axl.py unpack .work/div.wire | cmp - examples/div64.axl
```

The shared object exports `axl`. The native ABI and the wire grammar are in [SPEC.md](SPEC.md).

## Test, prove and benchmark

```sh
mkdir -p .work
python3 -m venv .work/venv
. .work/venv/bin/activate
python -m pip install -r requirements-experiments.txt
python -m unittest discover -s tests -v
python axl.py prove examples/div64.axl
python bench/run.py > .work/results.json
```

The optional dependencies enable symbolic proofs and exact model-token counts. The tokenizer downloads its vocabulary on first use. Core execution, exhaustive verification and native lowering use the Python standard library. The test suite skips three symbolic checks when Z3 is unavailable.

## Evidence

- The symbolic checker proves partial correctness of quotient and remainder for all `u64` inputs satisfying the precondition. The finite verifier separately checks all 65,280 admitted `u8` pairs and all 256 summation inputs, including termination within the fuel budget.
- The final wire encoding costs 73 and 156 `o200k_base` tokens for division and summation. The checked Python baselines cost 76 and 184. The benchmark also includes canonical JSON, hand-written C, single-node retrieval and a guarded edit.
- Native AXL ran within 7% of the hand-written C median on both kernels in the recorded run. The interpreter had substantially higher time and memory costs. See [experimental results and limitations](RESULTS.md) and [raw measurements](bench/results.json).

The [formal specification](SPEC.md) defines validation, instruction behavior, contracts, proof obligations and the graph-edit protocol. Examples include exact division at two widths, a loop with carried state, and an observable conditional value.
