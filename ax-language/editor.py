"""Addressable graph slices and optimistic, atomic multi-node edits (POSIX)."""
import fcntl
import os
import tempfile
from pathlib import Path

from graph import Graph, integer, need, read, seq


def view(graph, bid, ident=None, closure=False):
    need(integer(bid) and bid in graph.blocks, "unknown block")
    block = graph.blocks[bid]
    if ident is None:
        return {"rev": graph.digest, "args": graph.obj["args"], "ret": graph.ret,
                "effects": graph.obj["effects"], "pre": graph.obj["pre"],
                "post": graph.obj["post"], "block": block}
    rows = {r[0]: r for r in block[3]}
    need(integer(ident) and ident in rows, "unknown instruction")
    selected, frontier, pending = set(), set(), [ident]
    while pending:
        reg = pending.pop()
        if reg in selected:
            continue
        selected.add(reg)
        row = rows[reg]
        for ref in ([] if row[1] == "k" else row[2:]):
            if closure and ref in rows:
                pending.append(ref)
            else:
                frontier.add(ref)
    return {"rev": graph.digest, "block": bid, "inputs": [[r, graph.types[r]] for r in sorted(frontier)],
            "nodes": [r for r in block[3] if r[0] in selected], "type": graph.types[ident]}


def patch(path, revision, edits):
    """Replace existing instruction bodies. Directory lock survives atomic rename."""
    path = Path(path)
    directory = os.open(path.parent, os.O_RDONLY)
    try:
        fcntl.flock(directory, fcntl.LOCK_EX)
        graph = read(path)
        need(graph.digest == revision, "stale revision")
        touched = set()
        need(seq(edits), "empty patch")
        for edit in edits:
            bid, ident, body = seq(edit, 3)
            need(integer(bid) and bid in graph.blocks and integer(ident), "invalid edit address")
            need((bid, ident) not in touched, "duplicate edit")
            touched.add((bid, ident))
            for row in graph.blocks[bid][3]:
                if row[0] == ident:
                    row[1:] = seq(body)
                    break
            else:
                raise ValueError("unknown instruction")
        result = Graph(graph.obj)
        with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as tmp:
            tmp_path = Path(tmp.name)
        try:
            tmp_path.write_bytes(result.bytes())
            tmp_path.chmod(path.stat().st_mode & 0o777)
            os.replace(tmp_path, path)
        finally:
            tmp_path.unlink(missing_ok=True)
        return result.digest
    finally:
        os.close(directory)
