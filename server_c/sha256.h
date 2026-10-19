#ifndef SHA256_H
#define SHA256_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    uint8_t data[64];
    uint32_t datalen;
    uint64_t bitlen;
    uint32_t state[8];
} SHA256_CTX;

void sha256_init(SHA256_CTX *ctx);
void sha256_update(SHA256_CTX *ctx, const uint8_t data[], size_t len);
void sha256_final(SHA256_CTX *ctx, uint8_t hash[]);

// Base64 helpers
int base64_encode(const unsigned char *src, size_t len, char *out, size_t out_size);
int base64_decode(const char *src, unsigned char *out, size_t out_size);

// Password verification matching Java PasswordUtil
// Hash format: <base64_salt>$<base64_hash>
bool verify_password(const char *raw_password, const char *stored_hash);
char* hash_password(const char *raw_password);

#ifdef __cplusplus
}
#endif

#endif // SHA256_H
