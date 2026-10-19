#include "sha256.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>

#define DBL_INT_ADD(a,b,c) if (a > 0xffffffff - (c)) ++b; a += c;
#define ROTLEFT(a,b) (((a) << (b)) | ((a) >> (32-(b))))
#define ROTRIGHT(a,b) (((a) >> (b)) | ((a) << (32-(b))))

#define CH(x,y,z) (((x) & (y)) ^ (~(x) & (z)))
#define MAJ(x,y,z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define EP0(x) (ROTRIGHT(x,2) ^ ROTRIGHT(x,13) ^ ROTRIGHT(x,22))
#define EP1(x) (ROTRIGHT(x,6) ^ ROTRIGHT(x,11) ^ ROTRIGHT(x,25))
#define SIG0(x) (ROTRIGHT(x,7) ^ ROTRIGHT(x,18) ^ ((x) >> 3))
#define SIG1(x) (ROTRIGHT(x,17) ^ ROTRIGHT(x,19) ^ ((x) >> 10))

static const uint32_t k[64] = {
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};

static void sha256_transform(SHA256_CTX *ctx, const uint8_t data[]) {
    uint32_t a, b, c, d, e, f, g, h, i, j, t1, t2, m[64];

    for (i = 0, j = 0; i < 16; ++i, j += 4)
        m[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | (data[j + 3]);
    for ( ; i < 64; ++i)
        m[i] = SIG1(m[i - 2]) + m[i - 7] + SIG0(m[i - 15]) + m[i - 16];

    a = ctx->state[0];
    b = ctx->state[1];
    c = ctx->state[2];
    d = ctx->state[3];
    e = ctx->state[4];
    f = ctx->state[5];
    g = ctx->state[6];
    h = ctx->state[7];

    for (i = 0; i < 64; ++i) {
        t1 = h + EP1(e) + CH(e,f,g) + k[i] + m[i];
        t2 = EP0(a) + MAJ(a,b,c);
        h = g;
        g = f;
        f = e;
        e = d + t1;
        d = c;
        c = b;
        b = a;
        a = t1 + t2;
    }

    ctx->state[0] += a;
    ctx->state[1] += b;
    ctx->state[2] += c;
    ctx->state[3] += d;
    ctx->state[4] += e;
    ctx->state[5] += f;
    ctx->state[6] += g;
    ctx->state[7] += h;
}

void sha256_init(SHA256_CTX *ctx) {
    ctx->datalen = 0;
    ctx->bitlen = 0;
    ctx->state[0] = 0x6a09e667;
    ctx->state[1] = 0xbb67ae85;
    ctx->state[2] = 0x3c6ef372;
    ctx->state[3] = 0xa54ff53a;
    ctx->state[4] = 0x510e527f;
    ctx->state[5] = 0x9b05688c;
    ctx->state[6] = 0x1f83d9ab;
    ctx->state[7] = 0x5be0cd19;
}

void sha256_update(SHA256_CTX *ctx, const uint8_t data[], size_t len) {
    for (size_t i = 0; i < len; ++i) {
        ctx->data[ctx->datalen] = data[i];
        ctx->datalen++;
        if (ctx->datalen == 64) {
            sha256_transform(ctx, ctx->data);
            ctx->bitlen += 512;
            ctx->datalen = 0;
        }
    }
}

void sha256_final(SHA256_CTX *ctx, uint8_t hash[]) {
    uint32_t i = ctx->datalen;

    if (ctx->datalen < 56) {
        ctx->data[i++] = 0x80;
        while (i < 56)
            ctx->data[i++] = 0x00;
    } else {
        ctx->data[i++] = 0x80;
        while (i < 64)
            ctx->data[i++] = 0x00;
        sha256_transform(ctx, ctx->data);
        memset(ctx->data, 0, 56);
    }

    ctx->bitlen += ctx->datalen * 8;
    ctx->data[63] = ctx->bitlen;
    ctx->data[62] = ctx->bitlen >> 8;
    ctx->data[61] = ctx->bitlen >> 16;
    ctx->data[60] = ctx->bitlen >> 24;
    ctx->data[59] = ctx->bitlen >> 32;
    ctx->data[58] = ctx->bitlen >> 40;
    ctx->data[57] = ctx->bitlen >> 48;
    ctx->data[56] = ctx->bitlen >> 56;
    sha256_transform(ctx, ctx->data);

    for (i = 0; i < 4; ++i) {
        hash[i]      = (ctx->state[0] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 4]  = (ctx->state[1] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 8]  = (ctx->state[2] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 12] = (ctx->state[3] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 16] = (ctx->state[4] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 20] = (ctx->state[5] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 24] = (ctx->state[6] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 28] = (ctx->state[7] >> (24 - i * 8)) & 0x000000ff;
    }
}

// ==========================================
// BASE64 IMPLEMENTATION
// ==========================================
static const char b64_table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

int base64_encode(const unsigned char *src, size_t len, char *out, size_t out_size) {
    size_t olen = 4 * ((len + 2) / 3);
    if (out_size < olen + 1) return -1;

    size_t i = 0, j = 0;
    while (i < len) {
        size_t remain = len - i;
        uint32_t a = src[i++];
        uint32_t b = (remain > 1) ? src[i++] : 0;
        uint32_t c = (remain > 2) ? src[i++] : 0;

        uint32_t triple = (a << 16) | (b << 8) | c;

        out[j++] = b64_table[(triple >> 18) & 0x3F];
        out[j++] = b64_table[(triple >> 12) & 0x3F];
        out[j++] = (remain > 1) ? b64_table[(triple >> 6) & 0x3F] : '=';
        out[j++] = (remain > 2) ? b64_table[triple & 0x3F] : '=';
    }
    out[j] = '\0';
    return (int)j;
}

static int b64_val(char c) {
    if (c >= 'A' && c <= 'Z') return c - 'A';
    if (c >= 'a' && c <= 'z') return c - 'a' + 26;
    if (c >= '0' && c <= '9') return c - '0' + 52;
    if (c == '+') return 62;
    if (c == '/') return 63;
    return -1;
}

int base64_decode(const char *src, unsigned char *out, size_t out_size) {
    size_t len = strlen(src);
    if (len % 4 != 0) return -1;

    size_t out_len = (len / 4) * 3;
    if (len > 0 && src[len - 1] == '=') out_len--;
    if (len > 1 && src[len - 2] == '=') out_len--;
    if (out_size < out_len) return -1;

    size_t i, j;
    for (i = 0, j = 0; i < len; ) {
        int v0 = b64_val(src[i++]);
        int v1 = b64_val(src[i++]);
        int v2 = (src[i] == '=') ? (i++, 0) : b64_val(src[i++]);
        int v3 = (src[i] == '=') ? (i++, 0) : b64_val(src[i++]);

        if (v0 < 0 || v1 < 0 || v2 < 0 || v3 < 0) return -1;

        uint32_t triple = (v0 << 18) | (v1 << 12) | (v2 << 6) | v3;

        if (j < out_len) out[j++] = (triple >> 16) & 0xFF;
        if (j < out_len) out[j++] = (triple >> 8) & 0xFF;
        if (j < out_len) out[j++] = triple & 0xFF;
    }
    return (int)out_len;
}

bool verify_password(const char *raw_password, const char *stored_hash) {
    if (!raw_password || !stored_hash) return false;

    // Check if plain / unsalted backward compatibility
    const char *delim = strchr(stored_hash, '$');
    if (!delim) {
        // Plaintext match
        if (strcmp(raw_password, stored_hash) == 0) return true;
        // Plain SHA-256 hex match
        SHA256_CTX ctx;
        uint8_t hash[32];
        sha256_init(&ctx);
        sha256_update(&ctx, (const uint8_t*)raw_password, strlen(raw_password));
        sha256_final(&ctx, hash);
        char hex[65];
        for (int i = 0; i < 32; i++) sprintf(hex + (i * 2), "%02x", hash[i]);
        hex[64] = '\0';
        return (strcasecmp(hex, stored_hash) == 0);
    }

    // Salted format: <base64_salt>$<base64_hash>
    size_t salt_b64_len = delim - stored_hash;
    char salt_b64[128];
    if (salt_b64_len >= sizeof(salt_b64)) return false;
    strncpy(salt_b64, stored_hash, salt_b64_len);
    salt_b64[salt_b64_len] = '\0';

    const char *hash_b64 = delim + 1;

    unsigned char salt_bytes[64];
    int salt_len = base64_decode(salt_b64, salt_bytes, sizeof(salt_bytes));
    if (salt_len <= 0) return false;

    // Java: md.update(salt); byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
    SHA256_CTX ctx;
    uint8_t hash_bytes[32];
    sha256_init(&ctx);
    sha256_update(&ctx, salt_bytes, salt_len);
    sha256_update(&ctx, (const uint8_t*)raw_password, strlen(raw_password));
    sha256_final(&ctx, hash_bytes);

    char computed_b64[128];
    base64_encode(hash_bytes, 32, computed_b64, sizeof(computed_b64));

    return (strcmp(computed_b64, hash_b64) == 0);
}

char* hash_password(const char *raw_password) {
    if (!raw_password) return NULL;
    unsigned char salt[16];
    for (int i = 0; i < 16; i++) salt[i] = (unsigned char)(rand() % 256);

    char salt_b64[64];
    base64_encode(salt, 16, salt_b64, sizeof(salt_b64));

    SHA256_CTX ctx;
    uint8_t hash_bytes[32];
    sha256_init(&ctx);
    sha256_update(&ctx, salt, 16);
    sha256_update(&ctx, (const uint8_t*)raw_password, strlen(raw_password));
    sha256_final(&ctx, hash_bytes);

    char hash_b64[64];
    base64_encode(hash_bytes, 32, hash_b64, sizeof(hash_b64));

    char *result = (char*)malloc(strlen(salt_b64) + strlen(hash_b64) + 2);
    sprintf(result, "%s$%s", salt_b64, hash_b64);
    return result;
}
