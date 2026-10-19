#include "codec.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
    #include <winsock2.h>
    #include <ws2tcpip.h>
#else
    #include <sys/types.h>
    #include <sys/socket.h>
    #include <netinet/in.h>
    #include <arpa/inet.h>
    #include <unistd.h>
#endif

// Helper: send exactly `len` bytes over socket (anti-fragmentation)
static bool send_all(int socket_fd, const void *buf, size_t len) {
    const char *ptr = (const char *)buf;
    size_t total_sent = 0;
    while (total_sent < len) {
        int n = send(socket_fd, ptr + total_sent, (int)(len - total_sent), 0);
        if (n <= 0) {
            return false;
        }
        total_sent += n;
    }
    return true;
}

// Helper: recv exactly `len` bytes from socket (anti-fragmentation)
static bool recv_all(int socket_fd, void *buf, size_t len) {
    char *ptr = (char *)buf;
    size_t total_read = 0;
    while (total_read < len) {
        int n = recv(socket_fd, ptr + total_read, (int)(len - total_read), 0);
        if (n <= 0) {
            return false; // Connection closed or error
        }
        total_read += n;
    }
    return true;
}

bool send_packet(int socket_fd, uint16_t opcode, uint16_t status, const char *json_payload) {
    uint32_t payload_len = 0;
    if (json_payload != NULL) {
        payload_len = (uint32_t)strlen(json_payload);
    }

    PacketHeader header;
    header.magic = htonl(MAGIC_NUMBER);
    header.opcode = htons(opcode);
    header.status = htons(status);
    header.length = htonl(payload_len);

    // Send 12-byte header
    if (!send_all(socket_fd, &header, sizeof(header))) {
        return false;
    }

    // Send payload if any
    if (payload_len > 0) {
        if (!send_all(socket_fd, json_payload, payload_len)) {
            return false;
        }
    }

    return true;
}

bool recv_packet(int socket_fd, PacketHeader *out_header, char **out_payload) {
    if (!out_header || !out_payload) return false;
    *out_payload = NULL;

    PacketHeader raw_header;
    if (!recv_all(socket_fd, &raw_header, sizeof(raw_header))) {
        return false; // Normal EOF or connection drop
    }

    // Convert network byte order to host byte order
    out_header->magic = ntohl(raw_header.magic);
    out_header->opcode = ntohs(raw_header.opcode);
    out_header->status = ntohs(raw_header.status);
    out_header->length = ntohl(raw_header.length);

    // Validate Magic Number
    if (out_header->magic != MAGIC_NUMBER) {
        fprintf(stderr, "[ERROR] Corrupted packet: invalid magic 0x%08X (expected 0x%08X)\n",
                out_header->magic, MAGIC_NUMBER);
        return false;
    }

    // Validate Payload Size
    if (out_header->length > MAX_PAYLOAD_SIZE) {
        fprintf(stderr, "[ERROR] Packet payload exceeds max size: %u bytes\n", out_header->length);
        return false;
    }

    // Allocate memory and read exact payload bytes
    if (out_header->length > 0) {
        char *payload = (char *)malloc(out_header->length + 1);
        if (!payload) return false;

        if (!recv_all(socket_fd, payload, out_header->length)) {
            free(payload);
            return false;
        }

        payload[out_header->length] = '\0'; // Null-terminate string for JSON parser
        *out_payload = payload;
    } else {
        *out_payload = strdup("");
    }

    return true;
}

bool send_ok(int socket_fd, uint16_t opcode, const char *json_payload) {
    return send_packet(socket_fd, opcode, STATUS_OK, json_payload);
}

bool send_error(int socket_fd, uint16_t opcode, uint16_t status, const char *error_message) {
    char json[512];
    snprintf(json, sizeof(json), "{\"success\":false,\"message\":\"%s\"}", error_message ? error_message : "Error");
    return send_packet(socket_fd, opcode, status, json);
}

const char* opcode_to_string(uint16_t opcode) {
    switch (opcode) {
        case OP_LOGIN_REQ:          return "LOGIN_REQ (0x1001)";
        case OP_LOGIN_RES:          return "LOGIN_RES (0x1002)";
        case OP_REGISTER_REQ:       return "REGISTER_REQ (0x1003)";
        case OP_REGISTER_RES:       return "REGISTER_RES (0x1004)";
        case OP_LOGOUT_REQ:         return "LOGOUT_REQ (0x1005)";
        case OP_LOGOUT_RES:         return "LOGOUT_RES (0x1006)";
        case OP_CREATE_ROOM_REQ:    return "CREATE_ROOM_REQ (0x2001)";
        case OP_CREATE_ROOM_RES:    return "CREATE_ROOM_RES (0x2002)";
        case OP_JOIN_ROOM_REQ:      return "JOIN_ROOM_REQ (0x2003)";
        case OP_JOIN_ROOM_RES:      return "JOIN_ROOM_RES (0x2004)";
        case OP_TOGGLE_READY_REQ:   return "TOGGLE_READY_REQ (0x2005)";
        case OP_TOGGLE_READY_RES:   return "TOGGLE_READY_RES (0x2006)";
        case OP_START_ROOM_REQ:     return "START_ROOM_REQ (0x2007)";
        case OP_START_ROOM_RES:     return "START_ROOM_RES (0x2008)";
        case OP_LEAVE_ROOM_REQ:     return "LEAVE_ROOM_REQ (0x2009)";
        case OP_LEAVE_ROOM_RES:     return "LEAVE_ROOM_RES (0x200A)";
        case OP_EVENT_ROOM_UPDATE:  return "EVENT_ROOM_UPDATE (0x2010)";
        case OP_EVENT_ROOM_START:   return "EVENT_ROOM_START (0x2011)";
        case OP_ROOM_CHAT_REQ:      return "ROOM_CHAT_REQ (0x2012)";
        case OP_EVENT_ROOM_CHAT:    return "EVENT_ROOM_CHAT (0x2013)";
        case OP_ROOM_LIST_REQ:      return "ROOM_LIST_REQ (0x2020)";
        case OP_ROOM_LIST_RES:      return "ROOM_LIST_RES (0x2021)";
        case OP_EXAM_INIT_DATA:     return "EXAM_INIT_DATA (0x3001)";
        case OP_SUBMIT_ANSWER_REQ:  return "SUBMIT_ANSWER_REQ (0x3002)";
        case OP_SUBMIT_ANSWER_RES:  return "SUBMIT_ANSWER_RES (0x3003)";
        case OP_SUBMIT_EXAM_REQ:    return "SUBMIT_EXAM_REQ (0x3004)";
        case OP_SUBMIT_EXAM_RES:    return "SUBMIT_EXAM_RES (0x3005)";
        case OP_EVENT_EXAM_ENDED:   return "EVENT_EXAM_ENDED (0x3006)";
        case OP_EXAM_RESULT_RES:    return "EXAM_RESULT_RES (0x3007)";
        case OP_EVENT_EXAM_TICK:    return "EVENT_EXAM_TICK (0x3010)";
        case OP_PRACTICE_REQ:       return "PRACTICE_REQ (0x4001)";
        case OP_PRACTICE_RES:       return "PRACTICE_RES (0x4002)";
        case OP_PRACTICE_SUBMIT_REQ:return "PRACTICE_SUBMIT_REQ (0x4003)";
        case OP_PRACTICE_RESULT_RES:return "PRACTICE_RESULT_RES (0x4004)";
        case OP_GET_LOGS_REQ:       return "GET_LOGS_REQ (0x5001)";
        case OP_GET_LOGS_RES:       return "GET_LOGS_RES (0x5002)";
        case OP_QUESTION_LIST_REQ:  return "QUESTION_LIST_REQ (0x5010)";
        case OP_QUESTION_LIST_RES:  return "QUESTION_LIST_RES (0x5011)";
        case OP_USER_HISTORY_REQ:   return "USER_HISTORY_REQ (0x6001)";
        case OP_USER_HISTORY_RES:   return "USER_HISTORY_RES (0x6002)";
        case OP_USER_STATS_REQ:     return "USER_STATS_REQ (0x6003)";
        case OP_USER_STATS_RES:     return "USER_STATS_RES (0x6004)";
        case OP_LEADERBOARD_REQ:    return "LEADERBOARD_REQ (0x6005)";
        case OP_LEADERBOARD_RES:    return "LEADERBOARD_RES (0x6006)";
        case OP_PING:               return "PING (0x9001)";
        case OP_PONG:               return "PONG (0x9002)";
        default:                    return "UNKNOWN_OPCODE";
    }
}
