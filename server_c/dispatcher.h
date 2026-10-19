#ifndef DISPATCHER_H
#define DISPATCHER_H

#include "protocol.h"
#include "session.h"
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// Dispatches an incoming request packet and sends the appropriate response over socket
void dispatch_packet(ClientSession *session, const PacketHeader *header, const char *payload);

#ifdef __cplusplus
}
#endif

#endif // DISPATCHER_H
