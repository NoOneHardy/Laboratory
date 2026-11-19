"""Lower a validated AXL function to C11 with the unsigned 128-bit extension."""
from graph import Invalid, MAX

PRED_C = {"+": "+", "*": "*", "lt": "<", "le": "<=", "eq": "==", "and": "&", "or": "|"}
OPS = {"add": "+", "sub": "-", "mul": "*", "div": "/", "rem": "%",
       "lt": "<", "eq": "==", "and": "&", "or": "|"}


def pred_c(expr, graph):
    if type(expr) is int:
        return f"((wide)v{expr})", MAX[graph.types[expr]]
    op, *parts = expr
    if op == "out":
        i = parts[0]
        return f"((wide)r{i})", MAX[graph.ret[i]]
    if op in ("k", "bool"):
        value = int(parts[0])
        return f"((wide)UINT64_C({value}))", value
    (left, a), (right, b) = [pred_c(e, graph) for e in parts]
    bound = a + b if op == "+" else a * b if op == "*" else 1
    if bound >= 2**128:
        raise Invalid("native contract intermediate may exceed 128 bits")
    return f"({left} {PRED_C[op]} {right})", bound


def lower(graph):
    lines = ["#include <stdint.h>", "typedef __uint128_t wide;",
             "int axl(const uint64_t *args, uint64_t *out, uint64_t fuel, void (*trace)(uint64_t)) {"]
    put = lines.append
    for i, (reg, kind) in enumerate(graph.args.items()):
        put(f"  uint64_t v{reg} = args[{i}];")
        if kind != "u64":
            put(f"  if (v{reg} > {MAX[kind]}) return 6;")
    for reg in sorted(graph.types.keys() - graph.args.keys()):
        put(f"  uint64_t v{reg};")

    def assertions(checks, status):
        for expr in checks:
            put(f"  if (!({pred_c(expr, graph)[0]})) return {status};")

    def edge(target):
        bid, *values = target
        put("  {")
        for i, value in enumerate(values):
            put(f"    uint64_t t{i} = v{value};")
        for i, param in enumerate(graph.params[bid]):
            put(f"    v{param} = t{i};")
        put(f"    goto b{bid};")
        put("  }")

    assertions(graph.obj["pre"], 1)
    put("  goto b0;")
    for bid, _, inv, nodes, term in graph.obj["blocks"]:
        put(f"b{bid}:")
        put("  if (fuel == 0) return 5;")
        put("  --fuel;")
        assertions(inv, 2)
        for reg, op, *args in nodes:
            if op == "k":
                expr = f"UINT64_C({int(args[1])})"
            elif op == "zext":
                expr = f"v{args[0]}"
            elif op == "emit":
                expr = f"v{args[0]}"
                put(f"  if (trace) trace({expr});")
            elif op == "sel":
                expr = f"v{args[0]} ? v{args[1]} : v{args[2]}"
            else:
                a, b = args
                if op in ("div", "rem"):
                    put(f"  if (!v{b}) return 3;")
                expr = f"v{a} {OPS[op]} v{b}"
                if graph.types[reg] == "u8" and op in ("add", "sub", "mul"):
                    expr = f"(uint8_t)({expr})"
            put(f"  v{reg} = {expr};")
        if term[0] == "ret":
            put("  {")
            for i, reg in enumerate(term[1:]):
                put(f"  uint64_t r{i} = v{reg};")
            assertions(graph.obj["post"], 4)
            for i in range(len(graph.ret)):
                put(f"  out[{i}] = r{i};")
            put("  return 0;")
            put("  }")
        elif term[0] == "jmp":
            edge(term[1])
        else:
            put(f"  if (v{term[1]})")
            edge(term[2])
            edge(term[3])
    return "\n".join(lines + ["}", ""])
