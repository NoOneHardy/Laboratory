#include <stdint.h>
typedef __uint128_t wide;

int axl(const uint64_t *args, uint64_t *out, uint64_t fuel, void (*trace)(uint64_t)) {
    uint64_t n = args[0], d = args[1];
    if (d == 0) return 1;
    if (fuel == 0) return 5;
    uint64_t q = n / d, r = n % d;
    if ((wide)q * d + r != n || r >= d) return 4;
    out[0] = q;
    out[1] = r;
    return 0;
}
