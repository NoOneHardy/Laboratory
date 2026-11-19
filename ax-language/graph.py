"""AXL v1 graph loader, static semantics and contract expressions."""
import hashlib
import json
from pathlib import Path


class Invalid(ValueError):
    pass


def need(condition, message):
    if not condition:
        raise Invalid(message)


TYPES = ("u8", "u64", "b")
MAX = {"u8": 255, "u64": 2**64 - 1, "b": 1}
ARITH = ("add", "sub", "mul", "div", "rem")
CMP = ("lt", "eq")


def integer(value):
    return type(value) is int and 0 <= value <= MAX["u64"]


def seq(value, length=None):
    need(type(value) is list and (length is None or len(value) == length),
         f"expected array of length {length if length is not None else '*'}")
    return value


def unique_object(pairs):
    obj = dict(pairs)
    need(len(obj) == len(pairs), "duplicate JSON key")
    return obj


def read(path):
    return Graph(json.loads(Path(path).read_text(encoding="utf8"), object_pairs_hook=unique_object))


def packed(value):
    return json.dumps(value, separators=(",", ":"), sort_keys=True, allow_nan=False)


def typed_idents(pairs, seen):
    result = {}
    for pair in seq(pairs):
        ident, kind = seq(pair, 2)
        need(integer(ident) and ident not in seen and kind in TYPES, f"invalid declaration {pair}")
        seen.add(ident)
        result[ident] = kind
    return result


def lookup(env, ident):
    need(integer(ident) and ident in env, f"unavailable value {ident}")
    return env[ident]


def contract(expr, env, outputs):
    """Return a natural/boolean sort. Program words embed into naturals."""
    if integer(expr):
        return "b" if lookup(env, expr) == "b" else "n"
    seq(expr)
    need(bool(expr), "empty contract expression")
    op, *children = expr
    if op in ("k", "out", "bool"):
        seq(children, 1)
        value = children[0]
        if op == "bool":
            need(type(value) is bool, "expected boolean literal")
            return "b"
        need(integer(value), "expected natural literal or output index")
        if op == "k":
            return "n"
        need(value < len(outputs), "unavailable output")
        return "b" if outputs[value] == "b" else "n"
    seq(children, 2)
    a, b = (contract(child, env, outputs) for child in children)
    if op == "eq":
        need(a == b, "contract equality sort mismatch")
        return "b"
    if op in ("and", "or"):
        need(a == b == "b", "expected boolean operands")
        return "b"
    need(op in ("+", "*", "lt", "le") and a == b == "n", f"invalid contract operation {op}")
    return "n" if op in ("+", "*") else "b"


def checks(expressions, env, outputs=()):
    for expr in seq(expressions):
        need(contract(expr, env, outputs) == "b", "predicate must be boolean")


def node_type(row, env, effects):
    seq(row)
    need(len(row) >= 2, "short instruction")
    _, op, *args = row
    if op == "k":
        kind, value = seq(args, 2)
        need(kind in TYPES and type(value) is (bool if kind == "b" else int)
             and 0 <= value <= MAX[kind], "constant outside its type")
        return kind
    kinds = [lookup(env, arg) for arg in args]
    if op == "zext":
        need(kinds == ["u8"], "zext expects u8")
        return "u64"
    if op == "emit":
        need("trace" in effects and len(args) == 1, "emit requires trace and one operand")
        return kinds[0]
    if op == "sel":
        need(len(args) == 3 and kinds[0] == "b" and kinds[1] == kinds[2], "invalid select")
        return kinds[1]
    need(len(args) == 2 and kinds[0] == kinds[1], "binary operand mismatch")
    if op == "eq":
        return "b"
    if op in ("and", "or"):
        need(kinds[0] == "b", "expected booleans")
        return "b"
    need(op in (*ARITH, "lt") and kinds[0] != "b", f"invalid word operation {op}")
    return "b" if op == "lt" else kinds[0]


class Graph:
    def __init__(self, obj):
        fields = {"v", "args", "ret", "effects", "pre", "post", "blocks"}
        need(type(obj) is dict and set(obj) == fields and type(obj["v"]) is int
             and obj["v"] == 1, "invalid graph header")
        self.obj = obj
        seen = set()
        self.args = typed_idents(obj["args"], seen)
        self.ret = seq(obj["ret"])
        need(self.ret and all(t in TYPES for t in self.ret), "invalid return types")
        need(obj["effects"] in ([], ["trace"]), "unknown effects")
        checks(obj["pre"], self.args)
        checks(obj["post"], self.args, self.ret)
        self.blocks = {}
        self.params = {}
        self.types = dict(self.args)
        for block in seq(obj["blocks"]):
            bid, params, _, _, _ = seq(block, 5)
            need(integer(bid) and bid not in self.blocks, "duplicate or invalid block")
            self.blocks[bid] = block
            self.params[bid] = typed_idents(params, seen)
        need(0 in self.blocks and not self.params[0], "entry must be block 0 without parameters")
        for bid, (_, _, inv, nodes, term) in self.blocks.items():
            env = self.args | self.params[bid]
            checks(inv, env)
            for row in seq(nodes):
                kind = node_type(row, env, obj["effects"])
                ident = row[0]
                need(integer(ident) and ident not in seen, f"duplicate or invalid value {ident}")
                seen.add(ident)
                env[ident] = kind
            self.types.update(env)
            self._term(term, env)
        # Only block order is semantically irrelevant. Preserve instruction and argument order.
        self.obj = {**obj, "blocks": [self.blocks[b] for b in sorted(self.blocks)]}

    def _edge(self, edge, env):
        seq(edge)
        need(edge and integer(edge[0]) and edge[0] in self.blocks, "unknown target")
        expected = list(self.params[edge[0]].values())
        need([lookup(env, reg) for reg in edge[1:]] == expected, "edge parameter mismatch")

    def _term(self, term, env):
        seq(term)
        need(bool(term), "missing terminator")
        op, *args = term
        if op == "ret":
            need([lookup(env, reg) for reg in args] == self.ret, "return type mismatch")
        elif op == "jmp":
            self._edge(seq(args, 1)[0], env)
        else:
            need(op == "br" and len(args) == 3 and lookup(env, args[0]) == "b", "invalid branch")
            for edge in args[1:]:
                self._edge(edge, env)

    def bytes(self):
        return (packed(self.obj) + "\n").encode("ascii")

    @property
    def digest(self):
        return hashlib.sha256(self.bytes()).hexdigest()
