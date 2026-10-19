#ifndef ROOM_H
#define ROOM_H

#include <stdbool.h>
#include <stdint.h>
#include <pthread.h>
#include "db.h"
#include "lib/cJSON.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_ROOMS 64
#define MAX_PARTICIPANTS_PER_ROOM 32
#define MAX_ROOM_QUESTIONS 50

typedef struct {
    char username[64];
    char display_name[128];
    bool is_host;
    bool is_ready;
    bool is_submitted;
    int socket_fd;
} Participant;

typedef struct {
    int question_id;
    char selected_option[8];
} InFlightAnswer;

typedef struct {
    char username[64];
    int answer_count;
    InFlightAnswer answers[MAX_ROOM_QUESTIONS];
} ParticipantAnswers;

typedef struct {
    char room_code[16];
    char room_name[128];
    char host_username[64];
    char host_display_name[128];
    char topic[64];
    char difficulty[32];
    int question_count;
    int duration_minutes;
    int max_participants;
    char room_password[64];
    char status[32]; // "NOT_STARTED", "IN_PROGRESS", "FINISHED"
    long long created_at;

    int participant_count;
    Participant participants[MAX_PARTICIPANTS_PER_ROOM];

    int full_question_count;
    Question full_questions[MAX_ROOM_QUESTIONS];

    ParticipantAnswers user_answers[MAX_PARTICIPANTS_PER_ROOM];

    int seconds_remaining;
    bool is_timer_running;
    pthread_t timer_thread;
} ExamRoom;

void room_manager_init(void);

ExamRoom* room_create(const char *name, int duration_min, int q_count,
                      const char *topic, const char *difficulty,
                      int max_p, const char *password,
                      const char *host_user, const char *host_name, int host_socket);

ExamRoom* room_get(const char *room_code);
cJSON* room_get_list_json(void);

bool room_add_participant(const char *room_code, const char *username, const char *display_name, int socket_fd, const char *password, char **out_error);
bool room_remove_participant(const char *room_code, const char *username);
bool room_toggle_ready(const char *room_code, const char *username);
bool room_start_exam(const char *room_code, const char *host_username, char **out_error);
bool room_end_exam(const char *room_code);

void room_record_answer(const char *room_code, const char *username, int question_id, const char *answer);
cJSON* room_grade_student(const char *room_code, const char *username, int time_spent, bool is_early);

void room_broadcast(const char *room_code, uint16_t opcode, uint16_t status, const char *json);
void room_broadcast_except(const char *room_code, int except_fd, uint16_t opcode, uint16_t status, const char *json);

#ifdef __cplusplus
}
#endif

#endif // ROOM_H
