#include "dispatcher.h"
#include "codec.h"
#include "db.h"
#include "room.h"
#include "lib/cJSON.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static void handle_ping(ClientSession *session) {
    send_ok(session->socket_fd, OP_PONG, "{\"message\":\"PONG\"}");
}

static void handle_login(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    if (!json) {
        send_error(session->socket_fd, OP_LOGIN_RES, STATUS_BAD_REQUEST, "Invalid JSON payload");
        return;
    }

    cJSON *user_item = cJSON_GetObjectItem(json, "username");
    cJSON *pass_item = cJSON_GetObjectItem(json, "password");
    if (!user_item || !pass_item || !user_item->valuestring || !pass_item->valuestring) {
        cJSON_Delete(json);
        send_error(session->socket_fd, OP_LOGIN_RES, STATUS_BAD_REQUEST, "Missing username or password");
        return;
    }

    UserProfile profile;
    if (db_authenticate_user(user_item->valuestring, pass_item->valuestring, &profile)) {
        session_set_user(session->socket_fd, &profile);

        cJSON *res = cJSON_CreateObject();
        cJSON_AddBoolToObject(res, "success", true);
        cJSON_AddStringToObject(res, "message", "Authentication successful");
        cJSON_AddStringToObject(res, "token", "session_token_authenticated");
        cJSON_AddStringToObject(res, "username", profile.username);
        cJSON_AddStringToObject(res, "displayName", profile.display_name);
        cJSON_AddStringToObject(res, "role", profile.role);

        char *res_str = cJSON_PrintUnformatted(res);
        send_ok(session->socket_fd, OP_LOGIN_RES, res_str);
        free(res_str);
        cJSON_Delete(res);

        db_log_activity(profile.username, "LOGIN", "User logged in via C Server", session->client_ip);
        printf("[AUTH] User '%s' (%s) logged in successfully.\n", profile.username, profile.role);
    } else {
        send_error(session->socket_fd, OP_LOGIN_RES, STATUS_UNAUTHORIZED, "Invalid username or password!");
        printf("[AUTH] Login failed for username: %s\n", user_item->valuestring);
    }
    cJSON_Delete(json);
}

static void handle_register(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    if (!json) {
        send_error(session->socket_fd, OP_REGISTER_RES, STATUS_BAD_REQUEST, "Invalid JSON payload");
        return;
    }

    cJSON *user_item = cJSON_GetObjectItem(json, "username");
    cJSON *pass_item = cJSON_GetObjectItem(json, "password");
    cJSON *name_item = cJSON_GetObjectItem(json, "displayName");
    cJSON *email_item = cJSON_GetObjectItem(json, "email");

    if (!user_item || !pass_item || !user_item->valuestring || !pass_item->valuestring) {
        cJSON_Delete(json);
        send_error(session->socket_fd, OP_REGISTER_RES, STATUS_BAD_REQUEST, "Username and password required");
        return;
    }

    char *err = NULL;
    const char *name = (name_item && name_item->valuestring) ? name_item->valuestring : user_item->valuestring;
    const char *email = (email_item && email_item->valuestring) ? email_item->valuestring : "";

    if (db_register_user(user_item->valuestring, pass_item->valuestring, name, email, &err)) {
        cJSON *res = cJSON_CreateObject();
        cJSON_AddBoolToObject(res, "success", true);
        cJSON_AddStringToObject(res, "message", "User registered successfully");
        char *res_str = cJSON_PrintUnformatted(res);
        send_ok(session->socket_fd, OP_REGISTER_RES, res_str);
        free(res_str);
        cJSON_Delete(res);

        db_log_activity(user_item->valuestring, "REGISTER", "New user account created", session->client_ip);
        printf("[AUTH] Registered new user '%s'\n", user_item->valuestring);
    } else {
        send_error(session->socket_fd, OP_REGISTER_RES, STATUS_CONFLICT, err ? err : "Registration failed");
        if (err) free(err);
    }
    cJSON_Delete(json);
}

static void handle_logout(ClientSession *session) {
    if (session->is_authenticated) {
        printf("[AUTH] User '%s' logged out.\n", session->user.username);
        db_log_activity(session->user.username, "LOGOUT", "User logged out", session->client_ip);
    }
    session_set_user(session->socket_fd, NULL);
    send_ok(session->socket_fd, OP_LOGOUT_RES, "{\"success\":true,\"message\":\"Logged out successfully\"}");
}

static void handle_practice_request(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    const char *topic = "ALL";
    const char *diff = "ALL";
    int count = 5;
    int duration_min = 5;

    if (json) {
        cJSON *t = cJSON_GetObjectItem(json, "topic");
        cJSON *d = cJSON_GetObjectItem(json, "difficulty");
        cJSON *c = cJSON_GetObjectItem(json, "questionCount");
        cJSON *dur = cJSON_GetObjectItem(json, "durationMinutes");
        if (t && t->valuestring) topic = t->valuestring;
        if (d && d->valuestring) diff = d->valuestring;
        if (c && c->valueint > 0) count = c->valueint;
        if (dur && dur->valueint > 0) duration_min = dur->valueint;
    }

    Question *qs = NULL;
    int fetched = db_get_questions(topic, diff, count, &qs);

    cJSON *res = cJSON_CreateObject();
    cJSON_AddStringToObject(res, "topic", topic);
    cJSON_AddStringToObject(res, "difficulty", diff);
    cJSON_AddNumberToObject(res, "durationSeconds", duration_min * 60);

    cJSON *q_arr = cJSON_CreateArray();
    for (int i = 0; i < fetched; i++) {
        cJSON *q = cJSON_CreateObject();
        cJSON_AddNumberToObject(q, "id", qs[i].id);
        cJSON_AddNumberToObject(q, "questionNumber", i + 1);
        cJSON_AddStringToObject(q, "content", qs[i].content);
        cJSON_AddStringToObject(q, "optionA", qs[i].option_a);
        cJSON_AddStringToObject(q, "optionB", qs[i].option_b);
        cJSON_AddStringToObject(q, "optionC", qs[i].option_c);
        cJSON_AddStringToObject(q, "optionD", qs[i].option_d);
        cJSON_AddStringToObject(q, "category", qs[i].category);
        cJSON_AddStringToObject(q, "difficulty", qs[i].difficulty);
        cJSON_AddItemToArray(q_arr, q);
    }
    cJSON_AddItemToObject(res, "questions", q_arr);

    char *res_str = cJSON_PrintUnformatted(res);
    send_ok(session->socket_fd, OP_PRACTICE_RES, res_str);
    free(res_str);
    cJSON_Delete(res);
    if (qs) free(qs);
    if (json) cJSON_Delete(json);
}

static void handle_practice_submit(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    if (!json) {
        send_error(session->socket_fd, OP_PRACTICE_RESULT_RES, STATUS_BAD_REQUEST, "Invalid submission payload");
        return;
    }

    cJSON *answers = cJSON_GetObjectItem(json, "answers");
    cJSON *topic_item = cJSON_GetObjectItem(json, "topic");
    cJSON *diff_item = cJSON_GetObjectItem(json, "difficulty");
    cJSON *time_item = cJSON_GetObjectItem(json, "timeSpentSeconds");

    const char *topic = (topic_item && topic_item->valuestring) ? topic_item->valuestring : "General IT";
    const char *diff = (diff_item && diff_item->valuestring) ? diff_item->valuestring : "MEDIUM";
    int time_spent = (time_item) ? time_item->valueint : 60;

    int total_q = 0;
    int correct_count = 0;
    cJSON *review_arr = cJSON_CreateArray();

    if (answers && cJSON_IsObject(answers)) {
        cJSON *entry = NULL;
        cJSON_ArrayForEach(entry, answers) {
            int q_id = atoi(entry->string);
            const char *student_choice = entry->valuestring ? entry->valuestring : "";

            Question q;
            if (db_get_question_by_id(q_id, &q)) {
                total_q++;
                bool is_correct = (strcasecmp(student_choice, q.correct_option) == 0);
                if (is_correct) correct_count++;

                cJSON *rev = cJSON_CreateObject();
                cJSON_AddNumberToObject(rev, "questionId", q.id);
                cJSON_AddNumberToObject(rev, "questionNumber", total_q);
                cJSON_AddStringToObject(rev, "prompt", q.content);
                cJSON_AddStringToObject(rev, "studentChoice", student_choice);
                cJSON_AddStringToObject(rev, "correctChoice", q.correct_option);
                cJSON_AddBoolToObject(rev, "correct", is_correct);
                cJSON_AddStringToObject(rev, "explanation", q.explanation);
                cJSON_AddItemToArray(review_arr, rev);
            }
        }
    }

    double score = (total_q > 0) ? ((double)correct_count / total_q) * 10.0 : 0.0;
    const char *user = session->is_authenticated ? session->user.username : "guest";

    db_save_exam_record(user, "SOLO_PRACTICE", "Solo Practice Exam", "", topic, diff, score, correct_count, total_q, time_spent);

    cJSON *res = cJSON_CreateObject();
    cJSON_AddStringToObject(res, "testType", "SOLO_PRACTICE");
    cJSON_AddStringToObject(res, "title", "Solo Practice Exam");
    cJSON_AddNumberToObject(res, "score", score);
    cJSON_AddNumberToObject(res, "correctCount", correct_count);
    cJSON_AddNumberToObject(res, "totalQuestions", total_q);
    cJSON_AddNumberToObject(res, "timeSpentSeconds", time_spent);
    cJSON_AddItemToObject(res, "reviews", review_arr);

    char *res_str = cJSON_PrintUnformatted(res);
    send_ok(session->socket_fd, OP_PRACTICE_RESULT_RES, res_str);
    free(res_str);
    cJSON_Delete(res);
    cJSON_Delete(json);
}

static void handle_room_list(ClientSession *session) {
    cJSON *list = room_get_list_json();
    char *res_str = cJSON_PrintUnformatted(list);
    send_ok(session->socket_fd, OP_ROOM_LIST_RES, res_str);
    free(res_str);
    cJSON_Delete(list);
}

static void handle_create_room(ClientSession *session, const char *payload) {
    if (!session->is_authenticated) {
        send_error(session->socket_fd, OP_CREATE_ROOM_RES, STATUS_UNAUTHORIZED, "Login required to create room");
        return;
    }

    cJSON *json = cJSON_Parse(payload);
    if (!json) {
        send_error(session->socket_fd, OP_CREATE_ROOM_RES, STATUS_BAD_REQUEST, "Invalid JSON payload");
        return;
    }

    cJSON *name_item = cJSON_GetObjectItem(json, "roomName");
    cJSON *dur_item = cJSON_GetObjectItem(json, "durationMinutes");
    cJSON *q_item = cJSON_GetObjectItem(json, "questionCount");
    cJSON *topic_item = cJSON_GetObjectItem(json, "topic");
    cJSON *diff_item = cJSON_GetObjectItem(json, "difficulty");
    cJSON *max_item = cJSON_GetObjectItem(json, "maxParticipants");
    cJSON *pass_item = cJSON_GetObjectItem(json, "roomPassword");

    const char *name = (name_item && name_item->valuestring) ? name_item->valuestring : "Exam Room";
    int dur = (dur_item && dur_item->valueint > 0) ? dur_item->valueint : 10;
    int q_cnt = (q_item && q_item->valueint > 0) ? q_item->valueint : 5;
    const char *topic = (topic_item && topic_item->valuestring) ? topic_item->valuestring : "ALL";
    const char *diff = (diff_item && diff_item->valuestring) ? diff_item->valuestring : "ALL";
    int max_p = (max_item && max_item->valueint > 0) ? max_item->valueint : 30;
    const char *pass = (pass_item && pass_item->valuestring && strlen(pass_item->valuestring) > 0) ? pass_item->valuestring : NULL;

    ExamRoom *r = room_create(name, dur, q_cnt, topic, diff, max_p, pass,
                              session->user.username, session->user.display_name, session->socket_fd);

    if (r) {
        session_set_room(session->socket_fd, r->room_code);

        cJSON *res = cJSON_CreateObject();
        cJSON_AddBoolToObject(res, "success", true);
        cJSON_AddStringToObject(res, "message", "Room created successfully");
        cJSON_AddStringToObject(res, "roomCode", r->room_code);

        cJSON *info = cJSON_CreateObject();
        cJSON_AddStringToObject(info, "roomCode", r->room_code);
        cJSON_AddStringToObject(info, "roomName", r->room_name);
        cJSON_AddStringToObject(info, "hostUsername", r->host_username);
        cJSON_AddStringToObject(info, "hostDisplayName", r->host_display_name);
        cJSON_AddStringToObject(info, "status", r->status);
        cJSON_AddNumberToObject(info, "currentParticipants", r->participant_count);
        cJSON_AddNumberToObject(info, "maxParticipants", r->max_participants);
        cJSON_AddNumberToObject(info, "questionCount", r->question_count);
        cJSON_AddNumberToObject(info, "durationMinutes", r->duration_minutes);
        cJSON_AddStringToObject(info, "topic", r->topic);
        cJSON_AddStringToObject(info, "difficulty", r->difficulty);
        cJSON_AddBoolToObject(info, "hasPassword", strlen(r->room_password) > 0);
        cJSON_AddItemToObject(res, "roomInfo", info);

        char *res_str = cJSON_PrintUnformatted(res);
        send_ok(session->socket_fd, OP_CREATE_ROOM_RES, res_str);
        free(res_str);
        cJSON_Delete(res);

        db_log_activity(session->user.username, "CREATE_ROOM", r->room_code, session->client_ip);
        printf("[ROOM] Room %s created by %s\n", r->room_code, session->user.username);
    } else {
        send_error(session->socket_fd, OP_CREATE_ROOM_RES, STATUS_SERVER_ERROR, "Cannot create room (limit reached)");
    }
    cJSON_Delete(json);
}

static void handle_join_room(ClientSession *session, const char *payload) {
    if (!session->is_authenticated) {
        send_error(session->socket_fd, OP_JOIN_ROOM_RES, STATUS_UNAUTHORIZED, "Login required to join room");
        return;
    }

    cJSON *json = cJSON_Parse(payload);
    if (!json) {
        send_error(session->socket_fd, OP_JOIN_ROOM_RES, STATUS_BAD_REQUEST, "Invalid JSON payload");
        return;
    }

    cJSON *code_item = cJSON_GetObjectItem(json, "roomCode");
    cJSON *pass_item = cJSON_GetObjectItem(json, "password");
    if (!code_item || !code_item->valuestring) {
        cJSON_Delete(json);
        send_error(session->socket_fd, OP_JOIN_ROOM_RES, STATUS_BAD_REQUEST, "Missing roomCode");
        return;
    }

    const char *code = code_item->valuestring;
    const char *pass = (pass_item && pass_item->valuestring) ? pass_item->valuestring : NULL;

    char *err = NULL;
    if (room_add_participant(code, session->user.username, session->user.display_name, session->socket_fd, pass, &err)) {
        session_set_room(session->socket_fd, code);

        cJSON *res = cJSON_CreateObject();
        cJSON_AddBoolToObject(res, "success", true);
        cJSON_AddStringToObject(res, "message", "Joined room successfully");
        cJSON_AddStringToObject(res, "roomCode", code);

        char *res_str = cJSON_PrintUnformatted(res);
        send_ok(session->socket_fd, OP_JOIN_ROOM_RES, res_str);
        free(res_str);
        cJSON_Delete(res);

        db_log_activity(session->user.username, "JOIN_ROOM", code, session->client_ip);
        printf("[ROOM] User %s joined room %s\n", session->user.username, code);
    } else {
        send_error(session->socket_fd, OP_JOIN_ROOM_RES, STATUS_BAD_REQUEST, err ? err : "Failed to join room");
        if (err) free(err);
    }
    cJSON_Delete(json);
}

static void handle_toggle_ready(ClientSession *session) {
    if (strlen(session->current_room_code) > 0) {
        room_toggle_ready(session->current_room_code, session->user.username);
        send_ok(session->socket_fd, OP_TOGGLE_READY_RES, "{\"success\":true}");
    } else {
        send_error(session->socket_fd, OP_TOGGLE_READY_RES, STATUS_BAD_REQUEST, "Not currently in a room");
    }
}

static void handle_start_room(ClientSession *session) {
    if (strlen(session->current_room_code) > 0) {
        char *err = NULL;
        if (room_start_exam(session->current_room_code, session->user.username, &err)) {
            send_ok(session->socket_fd, OP_START_ROOM_RES, "{\"success\":true,\"message\":\"Exam started\"}");
            db_log_activity(session->user.username, "START_EXAM", session->current_room_code, session->client_ip);
            printf("[ROOM] Host %s started exam in room %s\n", session->user.username, session->current_room_code);
        } else {
            send_error(session->socket_fd, OP_START_ROOM_RES, STATUS_FORBIDDEN, err ? err : "Cannot start exam");
            if (err) free(err);
        }
    } else {
        send_error(session->socket_fd, OP_START_ROOM_RES, STATUS_BAD_REQUEST, "Not currently in a room");
    }
}

static void handle_leave_room(ClientSession *session) {
    if (strlen(session->current_room_code) > 0) {
        room_remove_participant(session->current_room_code, session->user.username);
        session_set_room(session->socket_fd, NULL);
        send_ok(session->socket_fd, OP_LEAVE_ROOM_RES, "{\"success\":true,\"message\":\"Left room\"}");
    } else {
        send_ok(session->socket_fd, OP_LEAVE_ROOM_RES, "{\"success\":true}");
    }
}

static void handle_submit_answer(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    if (json) {
        cJSON *q_item = cJSON_GetObjectItem(json, "questionId");
        cJSON *a_item = cJSON_GetObjectItem(json, "selectedOption");
        if (q_item && a_item && a_item->valuestring && strlen(session->current_room_code) > 0) {
            room_record_answer(session->current_room_code, session->user.username, q_item->valueint, a_item->valuestring);
            send_ok(session->socket_fd, OP_SUBMIT_ANSWER_RES, "{\"success\":true}");
        }
        cJSON_Delete(json);
    }
}

static void handle_submit_exam(ClientSession *session, const char *payload) {
    cJSON *json = cJSON_Parse(payload);
    int time_spent = 60;
    bool is_early = true;
    if (json) {
        cJSON *t_item = cJSON_GetObjectItem(json, "timeSpentSeconds");
        cJSON *e_item = cJSON_GetObjectItem(json, "isEarlySubmission");
        if (t_item) time_spent = t_item->valueint;
        if (e_item) is_early = cJSON_IsTrue(e_item);
        cJSON_Delete(json);
    }

    if (strlen(session->current_room_code) > 0) {
        cJSON *graded = room_grade_student(session->current_room_code, session->user.username, time_spent, is_early);
        if (graded) {
            char *res_str = cJSON_PrintUnformatted(graded);
            send_ok(session->socket_fd, OP_EXAM_RESULT_RES, res_str);
            free(res_str);
            cJSON_Delete(graded);
            db_log_activity(session->user.username, "SUBMIT_EXAM", session->current_room_code, session->client_ip);
            printf("[EXAM] User %s submitted exam in room %s\n", session->user.username, session->current_room_code);
        } else {
            send_error(session->socket_fd, OP_EXAM_RESULT_RES, STATUS_SERVER_ERROR, "Grading failed");
        }
    }
}

static void handle_room_chat(ClientSession *session, const char *payload) {
    if (strlen(session->current_room_code) > 0) {
        cJSON *json = cJSON_Parse(payload);
        if (json) {
            cJSON *msg_item = cJSON_GetObjectItem(json, "message");
            if (msg_item && msg_item->valuestring) {
                cJSON *bcast = cJSON_CreateObject();
                cJSON_AddStringToObject(bcast, "roomCode", session->current_room_code);
                cJSON_AddStringToObject(bcast, "senderUsername", session->user.username);
                cJSON_AddStringToObject(bcast, "senderDisplayName", session->user.display_name);
                cJSON_AddStringToObject(bcast, "message", msg_item->valuestring);
                cJSON_AddNumberToObject(bcast, "timestamp", (double)time(NULL) * 1000);

                char *bcast_str = cJSON_PrintUnformatted(bcast);
                room_broadcast(session->current_room_code, OP_EVENT_ROOM_CHAT, STATUS_OK, bcast_str);
                free(bcast_str);
                cJSON_Delete(bcast);
            }
            cJSON_Delete(json);
        }
    }
}

static void handle_user_history(ClientSession *session) {
    if (!session->is_authenticated) {
        send_error(session->socket_fd, OP_USER_HISTORY_RES, STATUS_UNAUTHORIZED, "Login required");
        return;
    }
    cJSON *hist = db_get_user_history(session->user.username);
    char *res_str = cJSON_PrintUnformatted(hist);
    send_ok(session->socket_fd, OP_USER_HISTORY_RES, res_str);
    free(res_str);
    cJSON_Delete(hist);
}

static void handle_user_stats(ClientSession *session) {
    if (!session->is_authenticated) {
        send_error(session->socket_fd, OP_USER_STATS_RES, STATUS_UNAUTHORIZED, "Login required");
        return;
    }
    cJSON *stats = db_get_user_stats(session->user.username);
    char *res_str = cJSON_PrintUnformatted(stats);
    send_ok(session->socket_fd, OP_USER_STATS_RES, res_str);
    free(res_str);
    cJSON_Delete(stats);
}

static void handle_leaderboard(ClientSession *session, const char *payload) {
    const char *topic = "ALL";
    cJSON *json = cJSON_Parse(payload);
    if (json) {
        cJSON *t = cJSON_GetObjectItem(json, "topic");
        if (t && t->valuestring) topic = t->valuestring;
    }

    cJSON *lb = db_get_leaderboard(topic);
    char *res_str = cJSON_PrintUnformatted(lb);
    send_ok(session->socket_fd, OP_LEADERBOARD_RES, res_str);
    free(res_str);
    cJSON_Delete(lb);
    if (json) cJSON_Delete(json);
}

static void handle_get_logs(ClientSession *session) {
    cJSON *logs = db_get_activity_logs(50);
    char *res_str = cJSON_PrintUnformatted(logs);
    send_ok(session->socket_fd, OP_GET_LOGS_RES, res_str);
    free(res_str);
    cJSON_Delete(logs);
}

void dispatch_packet(ClientSession *session, const PacketHeader *header, const char *payload) {
    uint16_t op = header->opcode;
    printf("[DISPATCH] Received %s from %s\n", opcode_to_string(op), session->client_ip);

    switch (op) {
        case OP_PING:                 handle_ping(session); break;
        case OP_LOGIN_REQ:            handle_login(session, payload); break;
        case OP_REGISTER_REQ:         handle_register(session, payload); break;
        case OP_LOGOUT_REQ:           handle_logout(session); break;
        case OP_SESSION_CHECK_REQ:
            if (session->is_authenticated) {
                send_ok(session->socket_fd, OP_SESSION_CHECK_RES, "{\"authenticated\":true}");
            } else {
                send_error(session->socket_fd, OP_SESSION_CHECK_RES, STATUS_UNAUTHORIZED, "Not authenticated");
            }
            break;

        case OP_PRACTICE_REQ:         handle_practice_request(session, payload); break;
        case OP_PRACTICE_SUBMIT_REQ:  handle_practice_submit(session, payload); break;

        case OP_ROOM_LIST_REQ:        handle_room_list(session); break;
        case OP_CREATE_ROOM_REQ:      handle_create_room(session, payload); break;
        case OP_JOIN_ROOM_REQ:        handle_join_room(session, payload); break;
        case OP_TOGGLE_READY_REQ:     handle_toggle_ready(session); break;
        case OP_START_ROOM_REQ:       handle_start_room(session); break;
        case OP_LEAVE_ROOM_REQ:       handle_leave_room(session); break;
        case OP_SUBMIT_ANSWER_REQ:    handle_submit_answer(session, payload); break;
        case OP_SUBMIT_EXAM_REQ:      handle_submit_exam(session, payload); break;
        case OP_ROOM_CHAT_REQ:        handle_room_chat(session, payload); break;

        case OP_USER_HISTORY_REQ:     handle_user_history(session); break;
        case OP_USER_STATS_REQ:       handle_user_stats(session); break;
        case OP_LEADERBOARD_REQ:      handle_leaderboard(session, payload); break;
        case OP_GET_LOGS_REQ:         handle_get_logs(session); break;

        default:
            fprintf(stderr, "[WARN] Unhandled OpCode: 0x%04X\n", op);
            send_error(session->socket_fd, op, STATUS_BAD_REQUEST, "Unknown or unhandled OpCode");
            break;
    }
}
