"""Conventional Python kernels for the benchmark's typed input domain."""


def div64(n, d, fuel):
    if d == 0:
        raise ValueError("pre")
    if fuel == 0:
        raise ValueError("fuel")
    q, r = divmod(n, d)
    if q * d + r != n or r >= d:
        raise ValueError("post")
    return [q, r]


def triangle(n, fuel):
    if not 0 <= n <= 255:
        raise ValueError("input")
    if fuel == 0:
        raise ValueError("fuel")
    fuel -= 1
    i = acc = 0
    while True:
        if fuel == 0:
            raise ValueError("fuel")
        fuel -= 1
        if i > n or 2 * acc != i * (i + 1):
            raise ValueError("invariant")
        if i == n:
            break
        if fuel == 0:
            raise ValueError("fuel")
        fuel -= 1
        i += 1
        acc += i
    if fuel == 0:
        raise ValueError("fuel")
    if 2 * acc != n * (n + 1):
        raise ValueError("post")
    return [acc]
