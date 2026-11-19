#include <stdint.h>
typedef __uint128_t wide;

int axl(const uint64_t *args, uint64_t *out, uint64_t fuel, void (*trace)(uint64_t)) {
    uint64_t n = args[0], acc = 0, i = 0;
    if (n > 255) return 6;
    if (fuel == 0) return 5;
    --fuel;
    for (;;) {
        if (fuel == 0) return 5;
        --fuel;
        if (i > n || (wide)2 * acc != (wide)i * (i + 1)) return 2;
        if (i == n) break;
        if (fuel == 0) return 5;
        --fuel;
        ++i;
        acc += i;
    }
    if (fuel == 0) return 5;
    if ((wide)2 * acc != (wide)n * (n + 1)) return 4;
    out[0] = acc;
    return 0;
}
