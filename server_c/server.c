#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <pthread.h>

#ifdef _WIN32
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #define close_socket(s) closesocket(s)
#else
    #include <sys/types.h>
    #include <sys/socket.h>
    #include <netinet/in.h>
    #include <netinet/tcp.h>
    #include <arpa/inet.h>
    #include <unistd.h>
    #define close_socket(s) close(s)
#endif

#include "protocol.h"
#include "codec.h"
#include "db.h"
#include "session.h"
#include "room.h"
#include "dispatcher.h"

#define DEFAULT_PORT 8888

static int server_socket = -1;
static volatile bool running = true;

static void handle_signal(int sig) {
    (void)sig;
    printf("\n[SERVER] Shutdown signal received. Closing server...\n");
    running = false;
    if (server_socket != -1) {
        close_socket(server_socket);
        server_socket = -1;
    }
}

typedef struct {
    int client_fd;
    char client_ip[64];
} ClientThreadArgs;

static void* client_handler_thread(void *arg) {
    ClientThreadArgs *args = (ClientThreadArgs *)arg;
    int client_fd = args->client_fd;
    char client_ip[64];
    strncpy(client_ip, args->client_ip, sizeof(client_ip) - 1);
    free(args);

    ClientSession *session = session_add(client_fd, client_ip);
    if (!session) {
        fprintf(stderr, "[ERROR] Cannot allocate session for %s\n", client_ip);
        close_socket(client_fd);
        return NULL;
    }

    printf("[CONNECT] Client connected from %s (Socket FD: %d)\n", client_ip, client_fd);

    PacketHeader header;
    char *payload = NULL;

    while (running && recv_packet(client_fd, &header, &payload)) {
        dispatch_packet(session, &header, payload);
        if (payload) {
            free(payload);
            payload = NULL;
        }
    }

    printf("[DISCONNECT] Client disconnected: %s (Socket FD: %d)\n", client_ip, client_fd);

    // Clean up room if in room
    if (strlen(session->current_room_code) > 0 && session->is_authenticated) {
        room_remove_participant(session->current_room_code, session->user.username);
    }

    session_remove(client_fd);
    close_socket(client_fd);
    return NULL;
}

static void print_banner(int port) {
    printf("=================================================================\n");
    printf("    HUST ONLINE EXAMINATION SYSTEM - C TCP SOCKET SERVER        \n");
    printf("  School of Information and Communication Technology (SoICT)    \n");
    printf("            Hanoi University of Science and Technology          \n");
    printf("=================================================================\n");
    printf("[*] Platform: Pure C Sockets (POSIX/BSD API)\n");
    printf("[*] Protocol: 12-Byte Binary Header + UTF-8 JSON Payload\n");
    printf("[*] Database: Embedded SQLite 3 (WAL Mode)\n");
    printf("[*] Listening on: 0.0.0.0:%d\n", port);
    printf("[*] Server started successfully! Ready for incoming client connections...\n");
    printf("=================================================================\n\n");
}

int main(int argc, char *argv[]) {
    int port = DEFAULT_PORT;
    if (argc >= 2) {
        port = atoi(argv[1]);
        if (port <= 0 || port > 65535) port = DEFAULT_PORT;
    }

    const char *db_path = (argc >= 3) ? argv[2] : "exam_system.db";

#ifdef _WIN32
    WSADATA wsa;
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        fprintf(stderr, "[FATAL] WSAStartup failed.\n");
        return 1;
    }
#endif

    signal(SIGINT, handle_signal);
    signal(SIGTERM, handle_signal);

    // 1. Initialize DB, Sessions, Rooms
    if (!db_init(db_path)) {
        fprintf(stderr, "[FATAL] Failed to initialize database.\n");
        return 1;
    }
    session_manager_init();
    room_manager_init();

    // 2. Create Server Socket
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket < 0) {
        perror("[FATAL] socket() failed");
        db_close();
        return 1;
    }

    // Set SO_REUSEADDR
    int opt = 1;
#ifdef _WIN32
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, (const char *)&opt, sizeof(opt));
#else
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif

    // Bind
    struct sockaddr_in server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);

    if (bind(server_socket, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("[FATAL] bind() failed");
        close_socket(server_socket);
        db_close();
        return 1;
    }

    // Listen
    if (listen(server_socket, 128) < 0) {
        perror("[FATAL] listen() failed");
        close_socket(server_socket);
        db_close();
        return 1;
    }

    print_banner(port);

    // Accept loop
    while (running) {
        struct sockaddr_in client_addr;
        socklen_t client_len = sizeof(client_addr);
        int client_fd = accept(server_socket, (struct sockaddr *)&client_addr, &client_len);

        if (client_fd < 0) {
            if (!running) break;
            perror("[WARN] accept() failed");
            continue;
        }

        // Disable Nagle's algorithm for low latency real-time packets
        int flag = 1;
#ifdef _WIN32
        setsockopt(client_fd, IPPROTO_TCP, TCP_NODELAY, (const char *)&flag, sizeof(flag));
#else
        setsockopt(client_fd, IPPROTO_TCP, TCP_NODELAY, &flag, sizeof(flag));
#endif

        ClientThreadArgs *args = (ClientThreadArgs *)malloc(sizeof(ClientThreadArgs));
        args->client_fd = client_fd;
        strncpy(args->client_ip, inet_ntoa(client_addr.sin_addr), sizeof(args->client_ip) - 1);

        pthread_t tid;
        if (pthread_create(&tid, NULL, client_handler_thread, args) == 0) {
            pthread_detach(tid);
        } else {
            perror("[ERROR] pthread_create() failed");
            close_socket(client_fd);
            free(args);
        }
    }

    printf("[SERVER] Cleaning up resources...\n");
    db_close();
#ifdef _WIN32
    WSACleanup();
#endif
    printf("[SERVER] Bye.\n");
    return 0;
}
