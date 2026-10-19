#ifndef DB_H
#define DB_H

#include <stdbool.h>
#include <stdint.h>
#include "lib/cJSON.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    int id;
    char username[64];
    char display_name[128];
    char email[128];
    char role[32];
} UserProfile;

typedef struct {
    int id;
    char content[512];
    char option_a[256];
    char option_b[256];
    char option_c[256];
    char option_d[256];
    char correct_option[8];
    char explanation[512];
    char category[64];
    char difficulty[32];
} Question;

// Initialize SQLite database, creates tables & seeds data if needed
bool db_init(const char *db_path);
void db_close(void);

// Authentication & Users
bool db_authenticate_user(const char *username, const char *password, UserProfile *out_user);
bool db_register_user(const char *username, const char *password, const char *display_name, const char *email, char **out_error);
bool db_get_user_profile(const char *username, UserProfile *out_user);

// Questions
int db_get_questions(const char *category, const char *difficulty, int count, Question **out_questions);
bool db_get_question_by_id(int question_id, Question *out_q);

// Exam Records & History
bool db_save_exam_record(const char *username, const char *test_type, const char *title,
                         const char *room_code, const char *topic, const char *difficulty,
                         double score, int correct_count, int total_questions, int time_spent_seconds);

cJSON* db_get_user_history(const char *username);
cJSON* db_get_user_stats(const char *username);
cJSON* db_get_leaderboard(const char *topic);

// Activity Logs
void db_log_activity(const char *username, const char *action, const char *details, const char *ip_address);
cJSON* db_get_activity_logs(int limit);

// Admin Question CRUD
cJSON* db_get_all_questions_json(void);
bool db_add_question(const Question *q);
bool db_update_question(const Question *q);
bool db_delete_question(int id);

#ifdef __cplusplus
}
#endif

#endif // DB_H
