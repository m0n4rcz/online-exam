#include "room.h"
#include "codec.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#ifdef _WIN32
    #include <windows.h>
    #define sleep_ms(ms) Sleep(ms)
#else
    #include <unistd.h>
    #define sleep_ms(ms) usleep((ms) * 1000)
#endif

static ExamRoom rooms[MAX_ROOMS];
static int room_count = 0;
static pthread_mutex_t room_mutex = PTHREAD_MUTEX_INITIALIZER;

static void generate_room_code(char *out_code) {
    const char chars[] = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    for (int i = 0; i < 6; i++) {
        out_code[i] = chars[rand() % (sizeof(chars) - 1)];
    }
    out_code[6] = '\0';
}

void room_manager_init(void) {
    pthread_mutex_lock(&room_mutex);
    memset(rooms, 0, sizeof(rooms));
    room_count = 0;
    pthread_mutex_unlock(&room_mutex);
}

ExamRoom* room_get(const char *room_code) {
    if (!room_code) return NULL;
    for (int i = 0; i < room_count; i++) {
        if (strcasecmp(rooms[i].room_code, room_code) == 0) {
            return &rooms[i];
        }
    }
    return NULL;
}

static cJSON* room_to_json(const ExamRoom *r) {
    cJSON *obj = cJSON_CreateObject();
    cJSON_AddStringToObject(obj, "roomCode", r->room_code);
    cJSON_AddStringToObject(obj, "roomName", r->room_name);
    cJSON_AddStringToObject(obj, "hostUsername", r->host_username);
    cJSON_AddStringToObject(obj, "hostDisplayName", r->host_display_name);
    cJSON_AddStringToObject(obj, "status", r->status);
    cJSON_AddNumberToObject(obj, "currentParticipants", r->participant_count);
    cJSON_AddNumberToObject(obj, "maxParticipants", r->max_participants);
    cJSON_AddNumberToObject(obj, "questionCount", r->question_count);
    cJSON_AddNumberToObject(obj, "durationMinutes", r->duration_minutes);
    cJSON_AddStringToObject(obj, "topic", r->topic);
    cJSON_AddStringToObject(obj, "difficulty", r->difficulty);
    cJSON_AddBoolToObject(obj, "hasPassword", strlen(r->room_password) > 0);
    cJSON_AddNumberToObject(obj, "createdAt", (double)r->created_at);
    return obj;
}

cJSON* room_get_list_json(void) {
    pthread_mutex_lock(&room_mutex);
    cJSON *root = cJSON_CreateObject();
    cJSON *arr = cJSON_CreateArray();
    for (int i = 0; i < room_count; i++) {
        cJSON_AddItemToArray(arr, room_to_json(&rooms[i]));
    }
    cJSON_AddItemToObject(root, "rooms", arr);
    pthread_mutex_unlock(&room_mutex);
    return root;
}

static void broadcast_room_update(ExamRoom *r) {
    cJSON *root = cJSON_CreateObject();
    cJSON_AddStringToObject(root, "roomCode", r->room_code);
    cJSON_AddStringToObject(root, "roomStatus", r->status);
    cJSON *part_arr = cJSON_CreateArray();
    for (int i = 0; i < r->participant_count; i++) {
        cJSON *p = cJSON_CreateObject();
        cJSON_AddStringToObject(p, "username", r->participants[i].username);
        cJSON_AddStringToObject(p, "displayName", r->participants[i].display_name);
        cJSON_AddBoolToObject(p, "host", r->participants[i].is_host);
        cJSON_AddBoolToObject(p, "ready", r->participants[i].is_ready);
        cJSON_AddBoolToObject(p, "submitted", r->participants[i].is_submitted);
        cJSON_AddItemToArray(part_arr, p);
    }
    cJSON_AddItemToObject(root, "participants", part_arr);
    char *json = cJSON_PrintUnformatted(root);
    cJSON_Delete(root);

    for (int i = 0; i < r->participant_count; i++) {
        if (r->participants[i].socket_fd > 0) {
            send_packet(r->participants[i].socket_fd, OP_EVENT_ROOM_UPDATE, STATUS_OK, json);
        }
    }
    free(json);
}

void room_broadcast(const char *room_code, uint16_t opcode, uint16_t status, const char *json) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (r) {
        for (int i = 0; i < r->participant_count; i++) {
            if (r->participants[i].socket_fd > 0) {
                send_packet(r->participants[i].socket_fd, opcode, status, json);
            }
        }
    }
    pthread_mutex_unlock(&room_mutex);
}

void room_broadcast_except(const char *room_code, int except_fd, uint16_t opcode, uint16_t status, const char *json) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (r) {
        for (int i = 0; i < r->participant_count; i++) {
            if (r->participants[i].socket_fd > 0 && r->participants[i].socket_fd != except_fd) {
                send_packet(r->participants[i].socket_fd, opcode, status, json);
            }
        }
    }
    pthread_mutex_unlock(&room_mutex);
}

ExamRoom* room_create(const char *name, int duration_min, int q_count,
                      const char *topic, const char *difficulty,
                      int max_p, const char *password,
                      const char *host_user, const char *host_name, int host_socket) {
    pthread_mutex_lock(&room_mutex);
    if (room_count >= MAX_ROOMS) {
        pthread_mutex_unlock(&room_mutex);
        return NULL;
    }

    ExamRoom *r = &rooms[room_count++];
    memset(r, 0, sizeof(ExamRoom));
    generate_room_code(r->room_code);
    strncpy(r->room_name, (name && strlen(name) > 0) ? name : "Exam Room", sizeof(r->room_name) - 1);
    strncpy(r->host_username, host_user, sizeof(r->host_username) - 1);
    strncpy(r->host_display_name, (host_name && strlen(host_name) > 0) ? host_name : host_user, sizeof(r->host_display_name) - 1);
    strncpy(r->topic, (topic && strlen(topic) > 0) ? topic : "ALL", sizeof(r->topic) - 1);
    strncpy(r->difficulty, (difficulty && strlen(difficulty) > 0) ? difficulty : "ALL", sizeof(r->difficulty) - 1);
    r->duration_minutes = duration_min > 0 ? duration_min : 10;
    r->question_count = q_count > 0 ? q_count : 5;
    r->max_participants = max_p > 0 ? max_p : 30;
    if (password) strncpy(r->room_password, password, sizeof(r->room_password) - 1);
    strcpy(r->status, "NOT_STARTED");
    r->created_at = (long long)time(NULL) * 1000;

    // Add host as participant
    r->participant_count = 1;
    strncpy(r->participants[0].username, host_user, sizeof(r->participants[0].username) - 1);
    strncpy(r->participants[0].display_name, r->host_display_name, sizeof(r->participants[0].display_name) - 1);
    r->participants[0].is_host = true;
    r->participants[0].is_ready = true;
    r->participants[0].socket_fd = host_socket;

    pthread_mutex_unlock(&room_mutex);
    return r;
}

bool room_add_participant(const char *room_code, const char *username, const char *display_name, int socket_fd, const char *password, char **out_error) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (!r) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Room not found!");
        return false;
    }

    if (strcmp(r->status, "NOT_STARTED") != 0) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Exam has already started or finished!");
        return false;
    }

    if (r->max_participants > 0 && r->participant_count >= r->max_participants) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Room is full!");
        return false;
    }

    if (strlen(r->room_password) > 0) {
        if (!password || strcmp(r->room_password, password) != 0) {
            pthread_mutex_unlock(&room_mutex);
            if (out_error) *out_error = strdup("Incorrect room password!");
            return false;
        }
    }

    // Check if already in room
    for (int i = 0; i < r->participant_count; i++) {
        if (strcasecmp(r->participants[i].username, username) == 0) {
            r->participants[i].socket_fd = socket_fd;
            broadcast_room_update(r);
            pthread_mutex_unlock(&room_mutex);
            return true;
        }
    }

    // Add new participant
    Participant *p = &r->participants[r->participant_count++];
    strncpy(p->username, username, sizeof(p->username) - 1);
    strncpy(p->display_name, (display_name && strlen(display_name) > 0) ? display_name : username, sizeof(p->display_name) - 1);
    p->is_host = false;
    p->is_ready = false;
    p->is_submitted = false;
    p->socket_fd = socket_fd;

    broadcast_room_update(r);
    pthread_mutex_unlock(&room_mutex);
    return true;
}

bool room_remove_participant(const char *room_code, const char *username) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (!r) {
        pthread_mutex_unlock(&room_mutex);
        return false;
    }

    int idx = -1;
    for (int i = 0; i < r->participant_count; i++) {
        if (strcasecmp(r->participants[i].username, username) == 0) {
            idx = i;
            break;
        }
    }

    if (idx >= 0) {
        for (int i = idx; i < r->participant_count - 1; i++) {
            r->participants[i] = r->participants[i + 1];
        }
        r->participant_count--;
        broadcast_room_update(r);
    }
    pthread_mutex_unlock(&room_mutex);
    return true;
}

bool room_toggle_ready(const char *room_code, const char *username) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (!r) {
        pthread_mutex_unlock(&room_mutex);
        return false;
    }

    for (int i = 0; i < r->participant_count; i++) {
        if (strcasecmp(r->participants[i].username, username) == 0) {
            if (!r->participants[i].is_host) {
                r->participants[i].is_ready = !r->participants[i].is_ready;
                broadcast_room_update(r);
            }
            break;
        }
    }
    pthread_mutex_unlock(&room_mutex);
    return true;
}

static void* room_timer_worker(void *arg) {
    char room_code[16];
    strncpy(room_code, (const char*)arg, sizeof(room_code) - 1);
    free(arg);

    while (1) {
        sleep_ms(1000);
        pthread_mutex_lock(&room_mutex);
        ExamRoom *r = room_get(room_code);
        if (!r || !r->is_timer_running) {
            pthread_mutex_unlock(&room_mutex);
            break;
        }

        r->seconds_remaining--;
        int sec = r->seconds_remaining;

        // Broadcast tick
        char tick_json[128];
        snprintf(tick_json, sizeof(tick_json), "{\"roomCode\":\"%s\",\"secondsRemaining\":%d}", room_code, sec);
        for (int i = 0; i < r->participant_count; i++) {
            if (r->participants[i].socket_fd > 0 && !r->participants[i].is_submitted) {
                send_packet(r->participants[i].socket_fd, OP_EVENT_EXAM_TICK, STATUS_OK, tick_json);
            }
        }

        if (sec <= 0) {
            strcpy(r->status, "FINISHED");
            r->is_timer_running = false;
            // Broadcast exam ended
            for (int i = 0; i < r->participant_count; i++) {
                if (r->participants[i].socket_fd > 0) {
                    send_packet(r->participants[i].socket_fd, OP_EVENT_EXAM_ENDED, STATUS_OK, "{\"message\":\"Time expired!\"}");
                }
            }
            pthread_mutex_unlock(&room_mutex);
            break;
        }
        pthread_mutex_unlock(&room_mutex);
    }
    return NULL;
}

bool room_start_exam(const char *room_code, const char *host_username, char **out_error) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (!r) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Room not found!");
        return false;
    }

    if (strcasecmp(r->host_username, host_username) != 0) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Only host can start the exam!");
        return false;
    }

    // Sample questions from database
    Question *qs = NULL;
    int fetched = db_get_questions(r->topic, r->difficulty, r->question_count, &qs);
    if (fetched <= 0 || !qs) {
        pthread_mutex_unlock(&room_mutex);
        if (out_error) *out_error = strdup("Not enough questions in database matching criteria!");
        return false;
    }

    r->full_question_count = fetched;
    for (int i = 0; i < fetched; i++) {
        r->full_questions[i] = qs[i];
    }
    free(qs);

    strcpy(r->status, "IN_PROGRESS");
    r->seconds_remaining = r->duration_minutes * 60;
    r->is_timer_running = true;

    // 1. Broadcast EVENT_ROOM_START
    for (int i = 0; i < r->participant_count; i++) {
        if (r->participants[i].socket_fd > 0) {
            send_packet(r->participants[i].socket_fd, OP_EVENT_ROOM_START, STATUS_OK, "{\"message\":\"Exam starting!\"}");
        }
    }

    // 2. Broadcast sanitized EXAM_INIT_DATA
    cJSON *init_obj = cJSON_CreateObject();
    cJSON_AddStringToObject(init_obj, "roomCode", r->room_code);
    cJSON_AddNumberToObject(init_obj, "durationMinutes", r->duration_minutes);
    cJSON_AddNumberToObject(init_obj, "durationSeconds", r->seconds_remaining);
    cJSON *q_arr = cJSON_CreateArray();
    for (int i = 0; i < r->full_question_count; i++) {
        cJSON *q = cJSON_CreateObject();
        cJSON_AddNumberToObject(q, "id", r->full_questions[i].id);
        cJSON_AddNumberToObject(q, "questionNumber", i + 1);
        cJSON_AddStringToObject(q, "content", r->full_questions[i].content);
        cJSON_AddStringToObject(q, "optionA", r->full_questions[i].option_a);
        cJSON_AddStringToObject(q, "optionB", r->full_questions[i].option_b);
        cJSON_AddStringToObject(q, "optionC", r->full_questions[i].option_c);
        cJSON_AddStringToObject(q, "optionD", r->full_questions[i].option_d);
        cJSON_AddStringToObject(q, "category", r->full_questions[i].category);
        cJSON_AddStringToObject(q, "difficulty", r->full_questions[i].difficulty);
        cJSON_AddItemToArray(q_arr, q);
    }
    cJSON_AddItemToObject(init_obj, "questions", q_arr);
    char *init_json = cJSON_PrintUnformatted(init_obj);
    cJSON_Delete(init_obj);

    for (int i = 0; i < r->participant_count; i++) {
        if (r->participants[i].socket_fd > 0) {
            send_packet(r->participants[i].socket_fd, OP_EXAM_INIT_DATA, STATUS_OK, init_json);
        }
    }
    free(init_json);

    // Launch background timer thread
    char *code_arg = strdup(room_code);
    pthread_create(&r->timer_thread, NULL, room_timer_worker, code_arg);
    pthread_detach(r->timer_thread);

    pthread_mutex_unlock(&room_mutex);
    return true;
}

void room_record_answer(const char *room_code, const char *username, int question_id, const char *answer) {
    if (!room_code || !username || !answer) return;
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (r) {
        int p_idx = -1;
        for (int i = 0; i < r->participant_count; i++) {
            if (strcasecmp(r->participants[i].username, username) == 0) {
                p_idx = i;
                break;
            }
        }
        if (p_idx >= 0) {
            ParticipantAnswers *pa = &r->user_answers[p_idx];
            strncpy(pa->username, username, sizeof(pa->username) - 1);
            bool updated = false;
            for (int k = 0; k < pa->answer_count; k++) {
                if (pa->answers[k].question_id == question_id) {
                    strncpy(pa->answers[k].selected_option, answer, sizeof(pa->answers[k].selected_option) - 1);
                    updated = true;
                    break;
                }
            }
            if (!updated && pa->answer_count < MAX_ROOM_QUESTIONS) {
                pa->answers[pa->answer_count].question_id = question_id;
                strncpy(pa->answers[pa->answer_count].selected_option, answer, sizeof(pa->answers[pa->answer_count].selected_option) - 1);
                pa->answer_count++;
            }
        }
    }
    pthread_mutex_unlock(&room_mutex);
}

cJSON* room_grade_student(const char *room_code, const char *username, int time_spent, bool is_early) {
    pthread_mutex_lock(&room_mutex);
    ExamRoom *r = room_get(room_code);
    if (!r) {
        pthread_mutex_unlock(&room_mutex);
        return NULL;
    }

    // Mark submitted
    for (int i = 0; i < r->participant_count; i++) {
        if (strcasecmp(r->participants[i].username, username) == 0) {
            r->participants[i].is_submitted = true;
            break;
        }
    }

    // Find saved answers
    ParticipantAnswers *pa = NULL;
    for (int i = 0; i < r->participant_count; i++) {
        if (strcasecmp(r->user_answers[i].username, username) == 0) {
            pa = &r->user_answers[i];
            break;
        }
    }

    int correct_count = 0;
    cJSON *review_arr = cJSON_CreateArray();

    for (int i = 0; i < r->full_question_count; i++) {
        Question *fq = &r->full_questions[i];
        char student_choice[8] = "";
        if (pa) {
            for (int k = 0; k < pa->answer_count; k++) {
                if (pa->answers[k].question_id == fq->id) {
                    strncpy(student_choice, pa->answers[k].selected_option, sizeof(student_choice) - 1);
                    break;
                }
            }
        }

        bool is_correct = (strcasecmp(student_choice, fq->correct_option) == 0);
        if (is_correct) correct_count++;

        cJSON *rev = cJSON_CreateObject();
        cJSON_AddNumberToObject(rev, "questionId", fq->id);
        cJSON_AddNumberToObject(rev, "questionNumber", i + 1);
        cJSON_AddStringToObject(rev, "prompt", fq->content);
        cJSON_AddStringToObject(rev, "studentChoice", student_choice);
        cJSON_AddStringToObject(rev, "correctChoice", fq->correct_option);
        cJSON_AddBoolToObject(rev, "correct", is_correct);
        cJSON_AddStringToObject(rev, "explanation", fq->explanation);
        cJSON_AddItemToArray(review_arr, rev);
    }

    int total_q = r->full_question_count;
    double score = (total_q > 0) ? ((double)correct_count / total_q) * 10.0 : 0.0;

    // Save exam record
    db_save_exam_record(username, "MULTIPLAYER", r->room_name, r->room_code, r->topic, r->difficulty,
                        score, correct_count, total_q, time_spent);

    // Build ExamResultDTO JSON
    cJSON *res = cJSON_CreateObject();
    cJSON_AddStringToObject(res, "testType", "MULTIPLAYER");
    cJSON_AddStringToObject(res, "title", r->room_name);
    cJSON_AddStringToObject(res, "roomCode", r->room_code);
    cJSON_AddNumberToObject(res, "score", score);
    cJSON_AddNumberToObject(res, "correctCount", correct_count);
    cJSON_AddNumberToObject(res, "totalQuestions", total_q);
    cJSON_AddNumberToObject(res, "timeSpentSeconds", time_spent);
    cJSON_AddBoolToObject(res, "earlySubmission", is_early);
    cJSON_AddItemToObject(res, "reviews", review_arr);

    // Notify other participants of submission
    char notice[128];
    snprintf(notice, sizeof(notice), "{\"username\":\"%s\"}", username);
    for (int i = 0; i < r->participant_count; i++) {
        if (r->participants[i].socket_fd > 0 && strcasecmp(r->participants[i].username, username) != 0) {
            send_packet(r->participants[i].socket_fd, OP_EVENT_PARTICIPANT_SUBMITTED, STATUS_OK, notice);
        }
    }

    pthread_mutex_unlock(&room_mutex);
    return res;
}
