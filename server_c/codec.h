#ifndef CODEC_H
#define CODEC_H

#include "protocol.h"
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// Sends a complete packet over socket with Big-Endian 12-byte header + UTF-8 payload
// Anti-fragmentation guarantee: loops until all bytes are sent
bool send_packet(int socket_fd, uint16_t opcode, uint16_t status, const char *json_payload);

// Receives a complete packet from socket
// Returns true on success, false on socket disconnect/error
// Dynamically allocates *out_payload (caller must free(*out_payload))
bool recv_packet(int socket_fd, PacketHeader *out_header, char **out_payload);

// Helper to send an OK response packet
bool send_ok(int socket_fd, uint16_t opcode, const char *json_payload);

// Helper to send an ERROR response packet
bool send_error(int socket_fd, uint16_t opcode, uint16_t status, const char *error_message);

#ifdef __cplusplus
}
#endif

#endif // CODEC_H
