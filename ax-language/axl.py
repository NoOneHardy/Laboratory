"""Command-line interface for one AXL function object."""
import argparse
import json
import sys

from editor import patch, view
from pathlib import Path
from graph import packed, read, seq
from wire import encode, decode
from machine import Trap, execute, verify
from native import lower


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="cmd", required=True)
    for name in ("check", "canon", "run", "verify", "view", "patch", "emit-c", "pack", "unpack", "prove"):
        p = commands.add_parser(name)
        p.add_argument("file")
        if name == "run":
            p.add_argument("args", help="JSON array of typed arguments", type=json.loads)
        if name in ("run", "verify"):
            p.add_argument("--fuel", type=int, default=10000)
        if name == "prove":
            p.add_argument("--timeout", type=int, default=2000, help="milliseconds per solver query")
        if name == "verify":
            p.add_argument("--limit", type=int, default=100000)
        if name == "view":
            p.add_argument("block", type=int)
            p.add_argument("node", nargs="?", type=int)
            p.add_argument("--deps", action="store_true")
        if name == "patch":
            p.add_argument("revision")
            p.add_argument("edits", type=json.loads)
    a = parser.parse_args()
    try:
        if a.cmd == "patch":
            result = {"rev": patch(a.file, a.revision, a.edits)}
        else:
            g = decode(Path(a.file).read_text()) if a.cmd == "unpack" else read(a.file)
            if a.cmd == "check":
                result = {"rev": g.digest}
            elif a.cmd == "pack":
                sys.stdout.write(encode(g))
                return 0
            elif a.cmd in ("canon", "unpack"):
                sys.stdout.buffer.write(g.bytes())
                return 0
            elif a.cmd == "emit-c":
                sys.stdout.write(lower(g))
                return 0
            elif a.cmd == "view":
                result = view(g, a.block, a.node, a.deps)
            elif a.cmd == "prove":
                from proof import prove
                result = prove(g, a.timeout)
                print(packed(result))
                return 0 if result["status"] == "proved" else 1
            elif a.cmd == "verify":
                result = verify(g, a.limit, a.fuel)
                print(packed(result))
                return 0 if result["status"] == "verified" else 1
            else:
                events = []
                try:
                    result = {"result": execute(g, seq(a.args), a.fuel, events.append), "trace": events}
                except Trap as error:
                    print(packed({"trap": error.kind, "site": error.site, "trace": events}))
                    return 1
        print(packed(result))
        return 0
    except (ValueError, OSError, RecursionError) as error:
        print(packed({"error": str(error)}), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
