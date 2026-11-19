#define _POSIX_C_SOURCE 200809L
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <inttypes.h>

int axl(const uint64_t *, uint64_t *, uint64_t, void (*)(uint64_t));

static double seconds(void) {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec + t.tv_nsec * 1e-9;
}

int main(int argc, char **argv) {
    if (argc != 3) return 1;
    int division = strcmp(argv[1], "div64") == 0;
    int count = atoi(argv[2]);
    uint64_t checksum = 0, args[2], out[2] = {0};
    double start = seconds();
    for (int i = 0; i < count; ++i) {
        args[0] = division ? (uint64_t)i * UINT64_C(11400714819323198485) : (i & 255);
        args[1] = (i % 251) + 1;
        if (axl(args, out, 10000, NULL)) return 2;
        checksum += out[0] + out[1];
    }
    double elapsed = seconds() - start;
    FILE *fp = fopen("/proc/self/status", "r");
    if (!fp) return 3;
    char line[256];
    int kb = 0;
    while (fgets(line, sizeof line, fp)) {
        if (sscanf(line, "VmHWM: %d kB", &kb) == 1) break;
    }
    fclose(fp);
    if (!kb) return 4;
    printf("[%.9f,%" PRIu64 ",%d]\n", elapsed, checksum, kb);
    return 0;
}
