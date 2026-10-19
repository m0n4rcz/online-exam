#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// ==========================================
// PROTOCOL CONSTANTS
// ==========================================
#define MAGIC_NUMBER      0x4558414D  // "EXAM" in ASCII
#define HEADER_SIZE       12
#define MAX_PAYLOAD_SIZE  (1024 * 1024) // 1 MB safety limit

// ==========================================
// 12-BYTE FIXED PACKET HEADER (Network Byte Order)
// ==========================================
#pragma pack(push, 1)
typedef struct {
    uint32_t magic;    // 0x4558414D ("EXAM")
    uint16_t opcode;   // Operation Code
    uint16_t status;   // Status Code (0 = OK)
    uint32_t length;   // Length of trailing JSON payload
} PacketHeader;
#pragma pack(pop)

// ==========================================
// STATUS CODES
// ==========================================
#define STATUS_OK                     0
#define STATUS_BAD_REQUEST            400
#define STATUS_UNAUTHORIZED           401
#define STATUS_FORBIDDEN              403
#define STATUS_NOT_FOUND              404
#define STATUS_CONFLICT               409
#define STATUS_ROOM_FULL              410
#define STATUS_ROOM_ALREADY_STARTED   411
#define STATUS_ROOM_FINISHED          412
#define STATUS_NOT_HOST               413
#define STATUS_ALREADY_IN_ROOM        414
#define STATUS_EXAM_ALREADY_SUBMITTED 415
#define STATUS_EXAM_TIME_EXPIRED      416
#define STATUS_SERVER_ERROR           500

// ==========================================
// OPCODES (Application-Layer Protocol)
// ==========================================

// 0x1000: Authentication & User Management
#define OP_LOGIN_REQ          0x1001
#define OP_LOGIN_RES          0x1002
#define OP_REGISTER_REQ       0x1003
#define OP_REGISTER_RES       0x1004
#define OP_LOGOUT_REQ         0x1005
#define OP_LOGOUT_RES         0x1006
#define OP_UPDATE_PROFILE_REQ 0x1007
#define OP_UPDATE_PROFILE_RES 0x1008
#define OP_SESSION_CHECK_REQ  0x1009
#define OP_SESSION_CHECK_RES  0x100A

// 0x2000: Room & Lobby Coordination
#define OP_CREATE_ROOM_REQ    0x2001
#define OP_CREATE_ROOM_RES    0x2002
#define OP_JOIN_ROOM_REQ      0x2003
#define OP_JOIN_ROOM_RES      0x2004
#define OP_TOGGLE_READY_REQ   0x2005
#define OP_TOGGLE_READY_RES   0x2006
#define OP_START_ROOM_REQ     0x2007
#define OP_START_ROOM_RES     0x2008
#define OP_LEAVE_ROOM_REQ     0x2009
#define OP_LEAVE_ROOM_RES     0x200A
#define OP_HOST_END_EXAM_REQ  0x200B
#define OP_HOST_END_EXAM_RES  0x200C

#define OP_EVENT_ROOM_UPDATE  0x2010
#define OP_EVENT_ROOM_START   0x2011
#define OP_ROOM_CHAT_REQ      0x2012
#define OP_EVENT_ROOM_CHAT    0x2013

#define OP_ROOM_LIST_REQ      0x2020
#define OP_ROOM_LIST_RES      0x2021
#define OP_COMPLETED_ROOM_RESULT_REQ 0x2030
#define OP_COMPLETED_ROOM_RESULT_RES 0x2031

// 0x3000: Multiplayer Exam Engine
#define OP_EXAM_INIT_DATA     0x3001
#define OP_SUBMIT_ANSWER_REQ  0x3002
#define OP_SUBMIT_ANSWER_RES  0x3003
#define OP_SUBMIT_EXAM_REQ    0x3004
#define OP_SUBMIT_EXAM_RES    0x3005
#define OP_EVENT_EXAM_ENDED   0x3006
#define OP_EXAM_RESULT_RES    0x3007
#define OP_EVENT_EXAM_TICK    0x3010
#define OP_EVENT_PARTICIPANT_SUBMITTED 0x3011

// 0x4000: Practice Mode (Solo)
#define OP_PRACTICE_REQ       0x4001
#define OP_PRACTICE_RES       0x4002
#define OP_PRACTICE_SUBMIT_REQ 0x4003
#define OP_PRACTICE_RESULT_RES 0x4004

// 0x5000: Activity Logging & Question Bank Management
#define OP_GET_LOGS_REQ       0x5001
#define OP_GET_LOGS_RES       0x5002
#define OP_QUESTION_LIST_REQ  0x5010
#define OP_QUESTION_LIST_RES  0x5011
#define OP_QUESTION_ADD_REQ   0x5012
#define OP_QUESTION_ADD_RES   0x5013
#define OP_QUESTION_UPDATE_REQ 0x5014
#define OP_QUESTION_UPDATE_RES 0x5015
#define OP_QUESTION_DELETE_REQ 0x5016
#define OP_QUESTION_DELETE_RES 0x5017

// 0x6000: History, Graphical Statistics & Leaderboard
#define OP_USER_HISTORY_REQ   0x6001
#define OP_USER_HISTORY_RES   0x6002
#define OP_USER_STATS_REQ     0x6003
#define OP_USER_STATS_RES     0x6004
#define OP_LEADERBOARD_REQ    0x6005
#define OP_LEADERBOARD_RES    0x6006

// 0x9000: System & Keep-Alive
#define OP_PING               0x9001
#define OP_PONG               0x9002
#define OP_RECONNECT_REQ      0x9003
#define OP_RECONNECT_RES      0x9004

const char* opcode_to_string(uint16_t opcode);

#ifdef __cplusplus
}
#endif

#endif // PROTOCOL_H
