"""Reversible, line-framed transfer encoding of the same graph."""
import re
from graph import ARITH, Graph, Invalid, integer, need, seq


def symbol(i):
    s = ""
    while True:
        i, digit = divmod(i, 26)
        s = chr(97 + digit) + s
        if not i:
            return s
        i -= 1


def ident(s):
    need(re.fullmatch("[a-z]{1,14}", s), "invalid identifier")
    i = 0
    for ch in s:
        i = i * 26 + ord(ch) - 96
    need(integer(i - 1), "identifier out of range")
    return i - 1


def pairs(items):
    return [word for reg, kind in items for word in (symbol(reg), kind)]


def predicate(expr):
    if type(expr) is int:
        return [symbol(expr)]
    op, *args = expr
    if op in ("k", "out", "bool"):
        return [":" + op, str(int(args[0]))]
    return [":" + op, *predicate(args[0]), *predicate(args[1])]


def encode(g):
    lines = [["AXL1"], ["A", *pairs(g.args.items())], ["R", *g.ret], ["E", *g.obj["effects"]]]
    for tag, key in [("P", "pre"), ("Q", "post")]:
        lines.extend([tag, *predicate(e)] for e in g.obj[key])
    for bid, params, inv, nodes, term in g.obj["blocks"]:
        lines.append(["B", symbol(bid), *pairs(params)])
        lines.extend(["I", *predicate(e)] for e in inv)
        for reg, op, *args in nodes:
            words = [args[0], str(int(args[1]))] if op == "k" else list(map(symbol, args))
            lines.append([op, symbol(reg), *words])
        op = term[0]
        if op == "br":
            words = [symbol(term[1]), *map(symbol, term[2]), "/", *map(symbol, term[3])]
        else:
            words = list(map(symbol, term[1] if op == "jmp" else term[1:]))
        lines.append([op, *words])
    return "".join(" ".join(row) + "\n" for row in lines)


def number(s):
    need(re.fullmatch("0|[1-9][0-9]{0,19}", s), "invalid unsigned literal")
    n = int(s)
    need(integer(n), "literal out of range")
    return n


def literal(kind, s):
    if kind == "b":
        need(s in ("0", "1"), "invalid boolean literal")
        return s == "1"
    return number(s)


def parse_pred(words):
    stream = iter(words)

    def one():
        token = next(stream)
        if not token.startswith(":"):
            return ident(token)
        op = token[1:]
        if op in ("k", "out", "bool"):
            return [op, literal("b" if op == "bool" else "u64", next(stream))]
        need(op in ("+", "*", "lt", "le", "eq", "and", "or"), "unknown predicate")
        return [op, one(), one()]

    expr = one()
    need(next(stream, None) is None, "trailing predicate tokens")
    return expr


def unpairs(words):
    need(len(words) % 2 == 0, "incomplete declaration")
    return [[ident(reg), kind] for reg, kind in zip(words[::2], words[1::2])]


def decode(source):
    lines = iter(line.split() for line in source.splitlines())
    obj = {"v": 1, "pre": [], "post": [], "blocks": []}
    try:
        need(next(lines) == ["AXL1"], "missing wire version")
        for tag, key in (("A", "args"), ("R", "ret"), ("E", "effects")):
            current, *words = next(lines)
            need(current == tag, f"expected {tag}")
            obj[key] = unpairs(words) if tag == "A" else words
        block = None
        for row in lines:
            tag, *words = row
            if tag in ("P", "Q"):
                need(block is None, "function predicate inside a block")
                obj["pre" if tag == "P" else "post"].append(parse_pred(words))
            elif tag == "B":
                block = [ident(words[0]), unpairs(words[1:]), [], [], []]
                obj["blocks"].append(block)
            else:
                need(block is not None and not block[4], "unexpected block record")
                if tag == "I":
                    need(not block[3], "late invariant")
                    block[2].append(parse_pred(words))
                elif tag in (*ARITH, "k", "lt", "eq", "and", "or", "sel", "emit", "zext"):
                    reg, *args = words
                    op = tag
                    if op == "k":
                        kind, value = seq(args, 2)
                        args = [kind, literal(kind, value)]
                    else:
                        args = list(map(ident, args))
                    block[3].append([ident(reg), op, *args])
                elif tag == "br":
                    i = words.index("/")
                    block[4] = [tag, ident(words[0]), list(map(ident, words[1:i])),
                                list(map(ident, words[i + 1:]))]
                elif tag in ("ret", "jmp"):
                    refs = list(map(ident, words))
                    block[4] = [tag, refs] if tag == "jmp" else [tag, *refs]
                else:
                    raise Invalid(f"unknown record {tag}")
        return Graph(obj)
    except (IndexError, StopIteration, ValueError) as exc:
        raise Invalid(f"invalid wire stream: {exc}") from exc
