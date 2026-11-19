"""Reproduce the measurements on Linux. Output a machine-readable report."""
import argparse
import inspect
import json
import os
import platform
import statistics
import subprocess
import sys
import tempfile
from pathlib import Path

import tiktoken
from baseline import div64, triangle

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from graph import packed, read
from native import lower
from editor import view
from wire import encode

BENCH = ROOT / "bench"
MASK = 2**64 - 1


def call(*args):
    return subprocess.check_output(list(map(str, args)), text=True)


def oracle(kernel, count):
    total = 0
    for i in range(count):
        if kernel == "div64":
            total += sum(divmod((i * 11400714819323198485) & MASK, i % 251 + 1))
        else:
            n = i & 255
            total += n * (n + 1) // 2
    return total & MASK


def tokens(graph, kernel, codec):
    def length(s):
        return len(codec.encode_ordinary(s))
    wire = encode(graph)
    tagged = []
    for line in wire.splitlines():
        op, *words = line.split()
        if op in ("k", "add", "sub", "mul", "div", "rem", "lt", "eq", "and", "or", "sel", "emit", "zext"):
            line = " ".join(["N", words[0], op, *words[1:]])
        tagged.append(line)
    docs = {"tagged_wire": "\n".join(tagged) + "\n", "canonical": graph.bytes().decode(), "wire": wire,
            "C": (BENCH / f"{kernel}.c").read_text(),
            "Python": inspect.getsource(div64 if kernel == "div64" else triangle)}
    return {k: length(v) for k, v in docs.items()}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--samples", type=int, default=5)
    p.add_argument("--encoding", default="o200k_base")
    args = p.parse_args()
    if args.samples <= 0:
        p.error("positive sample count required")
    codec = tiktoken.get_encoding(args.encoding)
    cc = os.getenv("CC", "cc")
    flags = ["-std=c11", "-O3"]
    cpu = next((s.split(":", 1)[1].strip() for s in Path("/proc/cpuinfo").read_text().splitlines()
                if s.startswith("model name")), platform.machine())
    report = {"environment": {"python": platform.python_version(), "compiler": call(cc, "--version").splitlines()[0],
              "cpu": cpu, "platform": platform.platform(), "flags": flags, "samples": args.samples,
              "tokenizer": f"tiktoken {tiktoken.__version__} {args.encoding}"}, "kernels": {}}
    work = ROOT / ".work"
    work.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(dir=work) as tmp:
        folder = Path(tmp)
        for kernel in ("div64", "sum"):
            graph = read(ROOT / "examples" / f"{kernel}.axl")
            native_count, py_count = (2000000, 8192) if kernel == "div64" else (150000, 512)
            expected = {n: oracle(kernel, n) for n in (native_count, py_count)}
            data = {"tokens": tokens(graph, kernel, codec), "runs": {}}
            commands = {}
            for name, path in [("native", folder / "lowered.c"), ("C", BENCH / f"{kernel}.c")]:
                if name == "native":
                    path.write_text(lower(graph))
                exe = folder / name
                call(cc, *flags, path, BENCH / "driver.c", "-o", exe)
                commands[name] = ([exe, kernel, native_count], native_count)
            for name, mode in (("interpreter", "axl"), ("Python", "python")):
                commands[name] = ([sys.executable, BENCH / "worker.py", mode, kernel, py_count], py_count)
            # Rotate order across repetitions to spread clock/load drift.
            names = list(commands)
            for sample in range(args.samples):
                for name in names[sample % 4:] + names[:sample % 4]:
                    command, count = commands[name]
                    elapsed, checksum, kb = json.loads(call(*command))
                    if checksum != expected[count]:
                        raise RuntimeError(f"{name}/{kernel}: checksum {checksum}, expected {expected[count]}")
                    row = data["runs"].setdefault(name, {"calls": count, "checksum": checksum,
                                                         "ns_per_call": [], "peak_rss_kib": []})
                    row["ns_per_call"].append(round(elapsed * 1e9 / count, 2))
                    row["peak_rss_kib"].append(kb)
            report["kernels"][kernel] = data
    graph = read(ROOT / "examples/sum.axl")
    rev = graph.digest
    line = next(s for s in (BENCH / "sum.c").read_text().splitlines() if "acc +=" in s)
    # Both hypothetical editor protocols include a SHA-256 guard; charge the actual AXL view/patch payload.
    import hashlib
    c_digest = hashlib.sha256((BENCH / "sum.c").read_bytes()).hexdigest()
    line_number = (BENCH / "sum.c").read_text().splitlines().index(line) + 1
    payloads = {"axl_node": packed(view(graph, 2, 11)), "axl_dependencies": packed(view(graph, 2, 11, True)),
                "axl_patch": rev + " " + packed([[2, 11, ["sub", 7, 10]]]),
                "c_line": packed({"rev": c_digest, "line": line_number, "text": line}),
                "c_patch": c_digest + " " + packed([[line_number, line.replace("+=", "-=")]])}
    report["edit_tokens"] = {k: len(codec.encode_ordinary(v)) for k, v in payloads.items()}
    for data in report["kernels"].values():
        for row in data["runs"].values():
            row["median_ns"] = statistics.median(row["ns_per_call"])
            row["median_kib"] = statistics.median(row["peak_rss_kib"])
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
