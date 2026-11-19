"""One fresh Python timing/RSS sample."""
import json
import sys
import time
from pathlib import Path

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root))
engine, kernel, amount = sys.argv[1:]
count = int(amount)
if engine == "axl":
    from graph import read
    from machine import execute
    graph = read(root / "examples" / f"{kernel}.axl")

    def run(args):
        return execute(graph, args)
else:
    from baseline import div64, triangle
    function = div64 if kernel == "div64" else triangle

    def run(args):
        return function(*args, 10000)

mask = 2**64 - 1
checksum = 0
start = time.perf_counter()
for i in range(count):
    args = [(i * 11400714819323198485) & mask, i % 251 + 1] if kernel == "div64" else [i & 255]
    checksum = (checksum + sum(run(args))) & mask
elapsed = time.perf_counter() - start
rss = next(line for line in Path("/proc/self/status").read_text().splitlines() if line.startswith("VmHWM:"))
print(json.dumps([elapsed, checksum, int(rss.split()[1])]))
