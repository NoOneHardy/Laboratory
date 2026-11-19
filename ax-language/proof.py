"""Optional inductive safety proof with mathematical integer constraints."""
import operator
from graph import MAX, integer, need


def prove(graph, timeout=2000):
    try:
        import z3 as z
    except ImportError as exc:
        raise ValueError("prove requires the optional z3-solver package") from exc
    need(integer(timeout) and 0 < timeout <= 2147483647, "invalid solver timeout in milliseconds")
    solver = z.Solver()
    solver.set(timeout=timeout)
    count = 0

    def symbolic(reg, kind):
        if kind == "b":
            return z.Bool(f"v{reg}")
        var = z.Int(f"v{reg}")
        solver.add(var >= 0, var <= MAX[kind])
        return var

    operations = {"eq": operator.eq, "lt": operator.lt, "le": operator.le, "and": z.And,
                  "or": z.Or, "+": operator.add, "*": operator.mul}

    def expr(e, env, out=()):
        if type(e) is int:
            return env[e]
        op, *args = e
        if op == "k":
            return z.IntVal(args[0])
        if op == "bool":
            return z.BoolVal(args[0])
        if op == "out":
            return out[args[0]]
        return operations[op](*(expr(arg, env, out) for arg in args))

    class Unproved(Exception):
        pass

    def obligation(claim, site, frame):
        nonlocal count
        count += 1
        solver.push()
        solver.add(z.Not(claim))
        answer = solver.check()
        if answer != z.unsat:
            info = {"status": "unproved", "site": site}
            if answer == z.unknown:
                info["reason"] = solver.reason_unknown()
            else:
                model = solver.model()
                info["block_inputs"] = {str(k): str(model.eval(v, model_completion=True)) for k,v in frame.items()}
            raise Unproved(info)
        solver.pop()

    args = {i: symbolic(i, t) for i, t in graph.args.items()}
    solver.add(*(expr(e, args) for e in graph.obj["pre"]))
    possible = solver.check()
    if possible != z.sat:
        return {"status": "vacuous" if possible == z.unsat else "unknown"}
    try:
        for i, inv in enumerate(graph.blocks[0][2]):
            obligation(expr(inv, args), ["entry", i], args)
        for bid, _, inv, rows, term in graph.obj["blocks"]:
            solver.push()
            frame = args | {i: symbolic(i, t) for i, t in graph.params[bid].items()}
            env = frame.copy()
            solver.add(*(expr(e, env) for e in inv))
            divisions = {}
            for reg, op, *refs in rows:
                if op == "k":
                    kind, k = refs
                    env[reg] = z.BoolVal(k) if kind == "b" else z.IntVal(k)
                    continue
                vals = [env[i] for i in refs]
                if op in ("div", "rem"):
                    a, b = vals
                    obligation(b != 0, [bid, reg, "zero"], frame)
                    key = tuple(refs)
                    if key not in divisions:
                        q, r = z.Ints(f"q{reg} r{reg}")
                        solver.add(a == q*b+r, q >= 0, r >= 0, r < b)
                        divisions[key] = (q, r)
                    env[reg] = divisions[key][op == "rem"]
                elif op in ("add", "sub", "mul"):
                    arithmetic = {"add": operator.add, "sub": operator.sub, "mul": operator.mul}
                    env[reg] = arithmetic[op](*vals) % (MAX[graph.types[reg]] + 1)
                elif op in ("zext", "emit"):
                    env[reg] = vals[0]
                elif op == "sel":
                    env[reg] = z.If(*vals)
                else:
                    env[reg] = operations[op](*vals)
            if term[0] == "ret":
                outputs = [env[i] for i in term[1:]]
                for i, e in enumerate(graph.obj["post"]):
                    obligation(expr(e, args, outputs), [bid, "post", i], frame)
            else:
                edges = [(True, term[1])] if term[0] == "jmp" else [
                    (env[term[1]], term[2]), (z.Not(env[term[1]]), term[3])]
                for guard, (dest, *refs) in edges:
                    next_frame = args | dict(zip(graph.params[dest], (env[r] for r in refs)))
                    for i, e in enumerate(graph.blocks[dest][2]):
                        obligation(z.Implies(guard, expr(e, next_frame)), [bid, dest, "invariant", i], frame)
            solver.pop()
    except Unproved as failure:
        return failure.args[0]
    return {"status": "proved", "property": "partial correctness", "obligations": count}
