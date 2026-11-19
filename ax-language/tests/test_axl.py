import copy
import ctypes as ct
import json
import multiprocessing
import os
import random
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from editor import patch, view
from graph import Graph, Invalid, MAX, read
from machine import Trap, execute, verify
from native import lower
from wire import decode, encode, ident, symbol

ROOT = Path(__file__).resolve().parents[1]
EX = ROOT / "examples"
WORK = ROOT / ".work"
WORK.mkdir(exist_ok=True)
U = MAX["u64"]


def sample(name):
    return read(EX / f"{name}.axl")


def function(nodes, result, *, args=None, ret=None, pre=None, post=None, effects=None):
    return Graph({"v": 1, "args": [] if args is None else args, "ret": ret or ["u8"],
                  "pre": pre or [], "post": post or [], "effects": effects or [],
                  "blocks": [[0, [], [], nodes, ["ret", *result]]]})


def compete(path, revision, op, start, queue):
    start.wait()
    try:
        patch(path, revision, [[0, 2, [op, 0, 1]]])
        queue.put("saved")
    except Invalid as error:
        queue.put(str(error))


class Language(unittest.TestCase):
    def test_canonical_and_wire(self):
        for path in EX.glob("*.axl"):
            graph = read(path)
            self.assertEqual(path.read_bytes(), graph.bytes())
            self.assertEqual(graph.bytes(), decode(encode(graph)).bytes())
            obj = copy.deepcopy(graph.obj)
            obj["blocks"].reverse()
            self.assertEqual(graph.digest, Graph(obj).digest)
        for number in (0, 25, 26, 701, 702, U):
            self.assertEqual(ident(symbol(number)), number)

    def test_ill_typed_graphs(self):
        changes = [
            (["v"], True), (["args", 1, 0], 0), (["args", 0, 0], True), (["args", 0, 1], "u32"),
            (["blocks", 0, 3, 0, 2], 9), (["blocks", 0, 3, 0, 2], 3),
            (["blocks", 0, 3, 0, 0], 0), (["blocks", 0, 4], ["ret", 2]),
            (["blocks", 0, 4], ["br", 1, [0], [0]]), (["blocks", 0, 4], ["jmp", [8]]),
            (["blocks", 0, 4], ["jmp", [0, 0]]), (["pre"], [0]),
            (["post"], [["eq", ["out", 2], 1]]), (["effects"], ["file"]),
            (["blocks", 0, 3, 0], [2, "emit", 0]), (["blocks", 0, 3, 0], [2, "k", "u8", 256]),
            (["blocks", 0, 3, 0], [2, "k", "b", 1]), (["pre"], [["lt", True, 0]]),
            (["pre"], [["eq", ["out", 0], 0]]), (["blocks", 0, 3, 0], [2, "mystery", 0, 1])]
        for keys, value in changes:
            with self.subTest(keys=keys, value=value):
                obj = copy.deepcopy(sample("div64").obj)
                current = obj
                for key in keys[:-1]:
                    current = current[key]
                current[keys[-1]] = value
                with self.assertRaises(Invalid):
                    Graph(obj)
        obj = sample("sum").obj
        obj["blocks"][2][3][-1][2] = 4
        with self.assertRaises(Invalid):
            Graph(obj)

    def test_failure_precedence_and_fuel(self):
        with self.assertRaisesRegex(Trap, "pre"):
            execute(sample("div64"), [1, 0], 0)
        with self.assertRaisesRegex(Trap, "fuel"):
            execute(sample("div64"), [1, 1], 0)
        graph = sample("sum")
        self.assertEqual(execute(graph, [2], 7), [3])
        with self.assertRaisesRegex(Trap, "fuel"):
            execute(graph, [2], 6)
        self.assertEqual(verify(graph, fuel=1)["status"], "unknown")

    def test_input_types(self):
        for args in ([True, 2], [-1, 1], [U + 1, 1], [1], [0.0, 1]):
            with self.assertRaises(Invalid):
                execute(sample("div64"), args)
        with self.assertRaises(Invalid):
            execute(sample("trace"), [1, 2, 3])

    def test_verifier(self):
        result = verify(sample("div8"))
        self.assertEqual((result["status"], result["admitted"]), ("verified", 65280))
        self.assertEqual(verify(sample("div64"))["domain"], 2**128)
        self.assertEqual(verify(sample("sum"))["admitted"], 256)
        obj = sample("div8").obj
        obj["blocks"][0][4] = ["ret", 3, 2]
        fault = verify(Graph(obj))
        self.assertEqual(fault["status"], "counterexample")
        with self.assertRaisesRegex(Trap, "post"):
            execute(Graph(obj), fault["args"])
        obj["pre"] = [["bool", False]]
        self.assertEqual(verify(Graph(obj))["status"], "vacuous")

    def test_loop_invariant_counterexample(self):
        obj = sample("sum").obj
        obj["blocks"][2][3][-1][1] = "sub"
        result = verify(Graph(obj))
        self.assertEqual((result["args"], result["reason"]), ([1], "invariant"))

    def test_trace_survives_trap(self):
        g = function([[1,"emit",0],[2,"k","u8",0],[3,"div",1,2]], [3],
                     args=[[0,"u8"]], effects=["trace"])
        seen = []
        with self.assertRaisesRegex(Trap, "zero"):
            execute(g, [5], trace=seen.append)
        self.assertEqual(seen, [5])

    def test_wire_rejects_malformed(self):
        wire = encode(sample("div64"))
        for source in [wire[:-8], wire.replace("div c", "I :bool 3\ndiv c"),
                       wire.replace(":k 0", ":k"), wire + "N e add a b\n",
                       wire.replace("b u64", "a u64"), wire.replace("div c", "div 999")]:
            with self.assertRaises(Invalid):
                decode(source)

    def test_cli(self):
        def cli(*args):
            return subprocess.run([sys.executable, ROOT / "axl.py", *args], capture_output=True, text=True)
        p = cli("run", EX/"trace.axl", "[false,3,9]")
        self.assertEqual(json.loads(p.stdout), {"trace": [9], "result": [9]})
        p = cli("run", EX/"div64.axl", "[7,0]")
        self.assertEqual((p.returncode, json.loads(p.stdout)["trap"]), (1, "pre"))
        self.assertEqual(cli("run", EX/"div64.axl", "{}").returncode, 2)
        self.assertEqual(cli("verify", EX/"div64.axl").returncode, 1)


class Edits(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(dir=WORK)
        self.addCleanup(temp.cleanup)
        self.path = Path(temp.name) / "test.axl"
        shutil.copyfile(EX / "div8.axl", self.path)
        self.graph = read(self.path)

    def test_slice(self):
        graph = sample("sum")
        self.assertEqual(view(graph, 2, 11)["inputs"], [[7,"u64"],[10,"u64"]])
        full = view(graph, 2, 11, True)
        self.assertEqual([r[0] for r in full["nodes"]], [8,9,10,11])
        self.assertEqual(full["inputs"], [[6,"u8"],[7,"u64"]])
        self.assertEqual(view(graph, 1)["block"][2], graph.blocks[1][2])

    def test_atomic_edits(self):
        rev = self.graph.digest
        with self.assertRaises(Invalid):
            patch(self.path, rev, [[0,2,["rem",0,1]], [0,3,["div",0,88]]])
        self.assertEqual(self.path.read_bytes(), self.graph.bytes())
        new = patch(self.path, rev, [[0,2,["rem",0,1]], [0,3,["div",0,1]]])
        self.assertNotEqual(rev, new)
        self.assertEqual(new, read(self.path).digest)
        with self.assertRaisesRegex(Invalid, "stale"):
            patch(self.path, rev, [[0,2,["div",0,1]]])

    def test_concurrent_writers(self):
        ctx = multiprocessing.get_context("fork")
        start, queue = ctx.Event(), ctx.Queue()
        jobs = [ctx.Process(target=compete, args=(self.path,self.graph.digest,op,start,queue))
                for op in ("rem","add")]
        for job in jobs:
            job.start()
        start.set()
        for job in jobs:
            job.join(5)
            self.assertEqual(job.exitcode, 0)
        self.assertEqual(sorted([queue.get(timeout=1),queue.get(timeout=1)]), ["saved","stale revision"])
        queue.close()


class Native(unittest.TestCase):
    U64 = ct.c_uint64
    Callback = ct.CFUNCTYPE(None, ct.c_uint64)

    def binding(self, graph):
        temp = tempfile.TemporaryDirectory(dir=WORK)
        self.addCleanup(temp.cleanup)
        so = Path(temp.name) / "axl.so"
        subprocess.run([os.environ.get("CC", "cc"), "-O2", "-std=c11", "-shared", "-fPIC", "-x", "c",
                        "-", "-o", so], input=lower(graph), text=True, check=True)
        fn = ct.CDLL(str(so)).axl
        fn.argtypes = [ct.POINTER(self.U64), ct.POINTER(self.U64), self.U64, self.Callback]
        fn.restype = ct.c_int

        def call(args, fuel=10000):
            inputs = (self.U64 * len(args))(*args)
            outputs = (self.U64 * len(graph.ret))(*([888] * len(graph.ret)))
            events = []
            status = fn(inputs, outputs, fuel, self.Callback(events.append))
            return status, list(outputs), events
        return call

    def test_div64_boundary_and_random(self):
        graph = sample("div64")
        call = self.binding(graph)
        rng = random.Random(617)
        cases = [(n,d) for n in (0,1,2,2**63,U-1,U) for d in (1,2,7,U-1,U)]
        cases += [(rng.getrandbits(64), rng.randrange(1,U+1)) for _ in range(1000)]
        for n,d in cases:
            expected = list(divmod(n,d))
            self.assertEqual(execute(graph, [n,d]), expected)
            self.assertEqual(call([n,d]), (0,expected,[]))
        self.assertEqual(call([0,0],0), (1,[888,888],[]))
        self.assertEqual(call([1,1],0)[0], 5)

    def test_native_loop_and_fuel(self):
        graph = sample("sum")
        call = self.binding(graph)
        for n in range(256):
            self.assertEqual(call([n]), (0,execute(graph,[n]),[]))
            self.assertEqual(call([n],2*n+2)[0], 5)
        self.assertEqual(call([256])[0], 6)

    def test_wide_contract_and_rejection(self):
        obj = sample("div64").obj
        obj["blocks"][0][3] = [[2,"k","u64",U],[3,"k","u64",0]]
        graph = Graph(obj)
        self.assertEqual(self.binding(graph)([1,U]), (4,[888,888],[]))
        with self.assertRaisesRegex(Trap, "post"):
            execute(graph, [1,U])
        obj["post"] = [["eq", ["*", ["*", 1, 1], 1], ["k",1]]]
        with self.assertRaisesRegex(Invalid, "128 bits"):
            lower(Graph(obj))

    def test_effects_and_native_traps(self):
        g = sample("trace")
        call = self.binding(g)
        for flag in (False,True):
            trace = []
            out = execute(g,[flag,13,21],trace=trace.append)
            self.assertEqual(call([flag,13,21]), (0,out,trace))
        self.assertEqual(call([2,13,21])[0], 6)
        graph = function([[1,"emit",0],[2,"k","u8",0],[3,"rem",1,2]], [3],
                         args=[[0,"u8"]], effects=["trace"])
        self.assertEqual(self.binding(graph)([6]), (3,[888],[6]))
        obj = sample("sum").obj
        obj["blocks"][2][3][-1][1] = "sub"
        self.assertEqual(self.binding(Graph(obj))([2])[0], 2)

    def test_parallel_edge_assignment(self):
        graph = Graph({"v":1,"args":[[0,"u8"],[1,"u8"],[2,"b"]],"ret":["u8","u8"],
                       "effects":[],"pre":[],"post":[],"blocks":[
                           [0,[],[],[],["jmp",[1,0,1,2]]],
                           [1,[[3,"u8"],[4,"u8"],[5,"b"]],[],[[6,"k","b",False]],
                            ["br",5,[1,4,3,6],[2,3,4]]],
                           [2,[[7,"u8"],[8,"u8"]],[],[],["ret",7,8]]]})
        self.assertEqual(execute(graph,[21,37,True]), [37,21])
        self.assertEqual(self.binding(graph)([21,37,True]), (0,[37,21],[]))

    def test_generated_straight_line_programs(self):
        rng = random.Random(20261119)
        for kind in ("u8","u64"):
            rows = []
            for reg in range(2,30):
                rows.append([reg,rng.choice(("add","mul","sub")),rng.randrange(reg),rng.randrange(reg)])
            refs = list(range(2,30))
            g = function(rows, refs, args=[[0,kind],[1,kind]], ret=[kind]*len(refs))
            call = self.binding(g)
            for _ in range(100):
                args = [rng.randrange(MAX[kind]+1),rng.randrange(MAX[kind]+1)]
                self.assertEqual(call(args)[1], execute(g,args))

    def test_boolean_ops(self):
        g = function([[2,"and",0,1],[3,"or",0,1],[4,"eq",2,3]], [2,3,4],
                     args=[[0,"b"],[1,"b"]], ret=["b"]*3)
        call = self.binding(g)
        for a in (True,False):
            for b in (True,False):
                expected = [a and b, a or b, a == b]
                self.assertEqual(execute(g,[a,b]), expected)
                self.assertEqual(call([a,b])[1], expected)


class Proof(unittest.TestCase):
    def setUp(self):
        try:
            import z3
        except ImportError:
            self.skipTest("optional z3-solver package unavailable")

    def test_full_width_division(self):
        from proof import prove
        self.assertEqual(prove(sample("div64"))["status"], "proved")
        obj = sample("div64").obj
        obj["pre"] = []
        self.assertEqual(prove(Graph(obj))["status"], "unproved")

    def test_postcondition_and_wraparound(self):
        from proof import prove
        g = function([[1,"k","u8",1],[2,"add",0,1]], [2], args=[[0,"u8"]],
                     post=[["lt",0,["out",0]]])
        result = prove(g)
        self.assertEqual(result["block_inputs"], {"0":"255"})
        g.obj["pre"] = [["lt",0,["k",255]]]
        self.assertEqual(prove(Graph(g.obj))["status"], "proved")

    def test_induction(self):
        from proof import prove
        obj = {"v":1,"args":[],"ret":["u8"],"pre":[],"post":[],"effects":[],"blocks":[
            [0,[],[],[[0,"k","u8",1]],["jmp",[1,0]]],
            [1,[[1,"u8"]],[["eq",1,["k",1]]],[],["jmp",[1,1]]]]}
        self.assertEqual(prove(Graph(obj))["status"], "proved")
        self.assertEqual(verify(Graph(obj), fuel=5)["status"], "unknown")
        obj["blocks"][0][3][0][3] = 2
        self.assertEqual(prove(Graph(obj))["status"], "unproved")
