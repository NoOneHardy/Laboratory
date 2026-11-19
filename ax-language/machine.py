"""Reference machine and exhaustive finite-domain contract checker."""
import itertools
import math
import operator

from graph import MAX, integer, need


class Trap(Exception):
    def __init__(self, kind, site):
        self.kind, self.site = kind, site
        super().__init__(f"{kind} at {site}")


PRED = {"+": operator.add, "*": operator.mul, "lt": operator.lt, "le": operator.le,
        "eq": operator.eq, "and": operator.and_, "or": operator.or_}


def evaluate(expr, env, outputs):
    if type(expr) is int:
        return env[expr]
    op, *parts = expr
    if op in ("k", "bool"):
        return parts[0]
    if op == "out":
        return outputs[parts[0]]
    return PRED[op](*(evaluate(part, env, outputs) for part in parts))


def check(predicates, env, outputs, kind, site):
    for index, expr in enumerate(predicates):
        if not evaluate(expr, env, outputs):
            raise Trap(kind, [site, index])


def execute(graph, args, fuel=10000, trace=None):
    need(integer(fuel), "invalid fuel")
    need(len(args) == len(graph.args), "argument count")
    for value, kind in zip(args, graph.args.values()):
        need(type(value) is (bool if kind == "b" else int) and 0 <= value <= MAX[kind],
             f"expected {kind}")
    frame = dict(zip(graph.args, args))
    check(graph.obj["pre"], frame, (), "pre", 0)
    target, incoming = 0, []
    while fuel:
        fuel -= 1
        bid, _, inv, nodes, term = graph.blocks[target]
        env = frame | dict(zip(graph.params[bid], incoming))
        check(inv, env, (), "invariant", bid)
        for ident, op, *operands in nodes:
            if op == "k":
                value = operands[1]
            elif op == "zext":
                value = env[operands[0]]
            elif op == "emit":
                value = env[operands[0]]
                if trace is not None:
                    trace(value)
            elif op == "sel":
                cond, yes, no = operands
                value = env[yes if env[cond] else no]
            else:
                left, right = (env[i] for i in operands)
                if op in ("div", "rem") and right == 0:
                    raise Trap("zero", [bid, ident])
                value = BINARY[op](left, right)
                if op in ("add", "sub", "mul"):
                    value &= MAX[graph.types[ident]]
            env[ident] = value
        if term[0] == "ret":
            result = [env[i] for i in term[1:]]
            check(graph.obj["post"], frame, result, "post", bid)
            return result
        edge = term[1] if term[0] == "jmp" else term[2 if env[term[1]] else 3]
        target, *refs = edge
        incoming = [env[i] for i in refs]
    raise Trap("fuel", target)


BINARY = {"add": operator.add, "sub": operator.sub, "mul": operator.mul,
          "div": operator.floordiv, "rem": operator.mod, "eq": operator.eq,
          "lt": operator.lt, "and": operator.and_, "or": operator.or_}


def verify(graph, limit=100000, fuel=10000):
    need(integer(fuel) and integer(limit), "invalid verifier budget")
    kinds = list(graph.args.values())
    size = math.prod(MAX[k] + 1 for k in kinds)
    if size > limit:
        return {"status": "unknown", "reason": "domain limit", "domain": size}
    domains = [(False, True) if k == "b" else range(MAX[k] + 1) for k in kinds]
    accepted = 0
    for args in itertools.product(*domains):
        try:
            execute(graph, args, fuel)
        except Trap as error:
            if error.kind == "pre":
                continue
            return {"status": "unknown" if error.kind == "fuel" else "counterexample",
                    "args": list(args), "reason": error.kind, "site": error.site}
        accepted += 1
    return {"status": "verified" if accepted else "vacuous", "admitted": accepted,
            "domain": size, "fuel": fuel}
