#include "session.h"
#include <string.h>
#include <pthread.h>
#include <stdio.h>

static ClientSession sessions[MAX_SESSIONS];
static pthread_mutex_t session_mutex = PTHREAD_MUTEX_INITIALIZER;

void session_manager_init(void) {
    pthread_mutex_lock(&session_mutex);
    memset(sessions, 0, sizeof(sessions));
    for (int i = 0; i < MAX_SESSIONS; i++) {
        sessions[i].socket_fd = -1;
    }
    pthread_mutex_unlock(&session_mutex);
}

ClientSession* session_add(int socket_fd, const char *client_ip) {
    pthread_mutex_lock(&session_mutex);
    ClientSession *slot = NULL;
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd == -1) {
            slot = &sessions[i];
            memset(slot, 0, sizeof(ClientSession));
            slot->socket_fd = socket_fd;
            strncpy(slot->client_ip, client_ip ? client_ip : "127.0.0.1", sizeof(slot->client_ip) - 1);
            slot->is_authenticated = false;
            break;
        }
    }
    pthread_mutex_unlock(&session_mutex);
    return slot;
}

void session_remove(int socket_fd) {
    pthread_mutex_lock(&session_mutex);
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd == socket_fd) {
            sessions[i].socket_fd = -1;
            sessions[i].is_authenticated = false;
            sessions[i].current_room_code[0] = '\0';
            break;
        }
    }
    pthread_mutex_unlock(&session_mutex);
}

ClientSession* session_get(int socket_fd) {
    pthread_mutex_lock(&session_mutex);
    ClientSession *found = NULL;
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd == socket_fd) {
            found = &sessions[i];
            break;
        }
    }
    pthread_mutex_unlock(&session_mutex);
    return found;
}

ClientSession* session_get_by_username(const char *username) {
    if (!username) return NULL;
    pthread_mutex_lock(&session_mutex);
    ClientSession *found = NULL;
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd != -1 && sessions[i].is_authenticated) {
            if (strcasecmp(sessions[i].user.username, username) == 0) {
                found = &sessions[i];
                break;
            }
        }
    }
    pthread_mutex_unlock(&session_mutex);
    return found;
}

void session_set_user(int socket_fd, const UserProfile *user) {
    pthread_mutex_lock(&session_mutex);
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd == socket_fd) {
            if (user) {
                memcpy(&sessions[i].user, user, sizeof(UserProfile));
                sessions[i].is_authenticated = true;
            } else {
                sessions[i].is_authenticated = false;
            }
            break;
        }
    }
    pthread_mutex_unlock(&session_mutex);
}

void session_set_room(int socket_fd, const char *room_code) {
    pthread_mutex_lock(&session_mutex);
    for (int i = 0; i < MAX_SESSIONS; i++) {
        if (sessions[i].socket_fd == socket_fd) {
            if (room_code) {
                strncpy(sessions[i].current_room_code, room_code, sizeof(sessions[i].current_room_code) - 1);
            } else {
                sessions[i].current_room_code[0] = '\0';
            }
            break;
        }
    }
    pthread_mutex_unlock(&session_mutex);
}
