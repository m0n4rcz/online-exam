#ifndef SESSION_H
#define SESSION_H

#include <stdbool.h>
#include "db.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_SESSIONS 256

typedef struct {
    int socket_fd;
    char client_ip[64];
    bool is_authenticated;
    UserProfile user;
    char current_room_code[16];
} ClientSession;

void session_manager_init(void);
ClientSession* session_add(int socket_fd, const char *client_ip);
void session_remove(int socket_fd);
ClientSession* session_get(int socket_fd);
ClientSession* session_get_by_username(const char *username);
void session_set_user(int socket_fd, const UserProfile *user);
void session_set_room(int socket_fd, const char *room_code);

#ifdef __cplusplus
}
#endif

#endif // SESSION_H
