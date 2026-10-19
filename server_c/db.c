#include "db.h"
#include "sha256.h"
#include "lib/sqlite3.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

static sqlite3 *db = NULL;
static pthread_mutex_t db_mutex = PTHREAD_MUTEX_INITIALIZER;

static void create_tables_if_not_exist(void) {
    const char *sql_users = 
        "CREATE TABLE IF NOT EXISTS users ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        "username TEXT UNIQUE NOT NULL COLLATE NOCASE,"
        "password_hash TEXT NOT NULL,"
        "display_name TEXT NOT NULL,"
        "email TEXT,"
        "role TEXT NOT NULL DEFAULT 'STUDENT',"
        "created_at INTEGER NOT NULL);";

    const char *sql_questions = 
        "CREATE TABLE IF NOT EXISTS questions ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        "content TEXT NOT NULL,"
        "option_a TEXT NOT NULL,"
        "option_b TEXT NOT NULL,"
        "option_c TEXT NOT NULL,"
        "option_d TEXT NOT NULL,"
        "correct_option TEXT NOT NULL,"
        "explanation TEXT,"
        "category TEXT NOT NULL,"
        "difficulty TEXT NOT NULL,"
        "created_at INTEGER NOT NULL);";

    const char *sql_records = 
        "CREATE TABLE IF NOT EXISTS exam_records ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        "username TEXT NOT NULL COLLATE NOCASE,"
        "test_type TEXT NOT NULL,"
        "title TEXT NOT NULL,"
        "room_code TEXT,"
        "topic TEXT NOT NULL,"
        "difficulty TEXT NOT NULL,"
        "score REAL NOT NULL,"
        "correct_count INTEGER NOT NULL,"
        "total_questions INTEGER NOT NULL,"
        "time_spent_seconds INTEGER NOT NULL,"
        "taken_at INTEGER NOT NULL);";

    const char *sql_logs = 
        "CREATE TABLE IF NOT EXISTS activity_logs ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        "timestamp INTEGER NOT NULL,"
        "username TEXT COLLATE NOCASE,"
        "action TEXT NOT NULL,"
        "details TEXT,"
        "ip_address TEXT);";

    char *err_msg = NULL;
    sqlite3_exec(db, sql_users, NULL, NULL, &err_msg);
    sqlite3_exec(db, sql_questions, NULL, NULL, &err_msg);
    sqlite3_exec(db, sql_records, NULL, NULL, &err_msg);
    sqlite3_exec(db, sql_logs, NULL, NULL, &err_msg);

    // Performance optimizations
    sqlite3_exec(db, "PRAGMA journal_mode = WAL;", NULL, NULL, NULL);
    sqlite3_exec(db, "PRAGMA synchronous = NORMAL;", NULL, NULL, NULL);
}

static void seed_default_users(void) {
    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM users;", -1, &stmt, NULL) == SQLITE_OK) {
        if (sqlite3_step(stmt) == SQLITE_ROW && sqlite3_column_int(stmt, 0) == 0) {
            sqlite3_finalize(stmt);

            const char *insert_sql = "INSERT INTO users (username, password_hash, display_name, email, role, created_at) VALUES (?, ?, ?, ?, ?, ?);";
            long long now = (long long)time(NULL) * 1000;

            struct {
                const char *user;
                const char *pass;
                const char *name;
                const char *email;
                const char *role;
            } seed[] = {
                {"admin", "admin123", "System Administrator", "admin@onlineexam.com", "ADMIN"},
                {"teacher01", "teacher123", "Dr. Alice Smith", "alice@exam.edu.vn", "TEACHER"},
                {"student01", "student123", "Nguyen Van A", "nva@sis.hust.edu.vn", "STUDENT"},
                {"student02", "student123", "Tran Thi B", "ttb@sis.hust.edu.vn", "STUDENT"}
            };

            for (int i = 0; i < 4; i++) {
                char *h = hash_password(seed[i].pass);
                sqlite3_stmt *ins;
                if (sqlite3_prepare_v2(db, insert_sql, -1, &ins, NULL) == SQLITE_OK) {
                    sqlite3_bind_text(ins, 1, seed[i].user, -1, SQLITE_STATIC);
                    sqlite3_bind_text(ins, 2, h, -1, SQLITE_STATIC);
                    sqlite3_bind_text(ins, 3, seed[i].name, -1, SQLITE_STATIC);
                    sqlite3_bind_text(ins, 4, seed[i].email, -1, SQLITE_STATIC);
                    sqlite3_bind_text(ins, 5, seed[i].role, -1, SQLITE_STATIC);
                    sqlite3_bind_int64(ins, 6, now);
                    sqlite3_step(ins);
                    sqlite3_finalize(ins);
                }
                free(h);
            }
            printf("[DB] Seeded default users (admin, teacher01, student01, student02)\n");
        } else {
            sqlite3_finalize(stmt);
        }
    }
}

bool db_init(const char *db_path) {
    pthread_mutex_lock(&db_mutex);
    int rc = sqlite3_open(db_path ? db_path : "exam_system.db", &db);
    if (rc != SQLITE_OK) {
        fprintf(stderr, "[DB ERROR] Cannot open database: %s\n", sqlite3_errmsg(db));
        pthread_mutex_unlock(&db_mutex);
        return false;
    }

    create_tables_if_not_exist();
    seed_default_users();
    pthread_mutex_unlock(&db_mutex);
    printf("[DB] SQLite database initialized successfully: %s\n", db_path ? db_path : "exam_system.db");
    return true;
}

void db_close(void) {
    pthread_mutex_lock(&db_mutex);
    if (db) {
        sqlite3_close(db);
        db = NULL;
    }
    pthread_mutex_unlock(&db_mutex);
}

bool db_authenticate_user(const char *username, const char *password, UserProfile *out_user) {
    if (!username || !password || !out_user) return false;

    pthread_mutex_lock(&db_mutex);
    sqlite3_stmt *stmt;
    const char *sql = "SELECT id, username, password_hash, display_name, email, role FROM users WHERE username = ? COLLATE NOCASE;";
    
    bool authenticated = false;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            const char *stored_hash = (const char *)sqlite3_column_text(stmt, 2);
            if (verify_password(password, stored_hash)) {
                out_user->id = sqlite3_column_int(stmt, 0);
                strncpy(out_user->username, (const char *)sqlite3_column_text(stmt, 1), sizeof(out_user->username) - 1);
                strncpy(out_user->display_name, (const char *)sqlite3_column_text(stmt, 3), sizeof(out_user->display_name) - 1);
                const char *email = (const char *)sqlite3_column_text(stmt, 4);
                strncpy(out_user->email, email ? email : "", sizeof(out_user->email) - 1);
                strncpy(out_user->role, (const char *)sqlite3_column_text(stmt, 5), sizeof(out_user->role) - 1);
                authenticated = true;
            }
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return authenticated;
}

bool db_register_user(const char *username, const char *password, const char *display_name, const char *email, char **out_error) {
    if (!username || !password) {
        if (out_error) *out_error = strdup("Username and password are required.");
        return false;
    }

    pthread_mutex_lock(&db_mutex);
    // Check if username already exists
    sqlite3_stmt *check_stmt;
    const char *check_sql = "SELECT id FROM users WHERE username = ? COLLATE NOCASE;";
    if (sqlite3_prepare_v2(db, check_sql, -1, &check_stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(check_stmt, 1, username, -1, SQLITE_STATIC);
        if (sqlite3_step(check_stmt) == SQLITE_ROW) {
            sqlite3_finalize(check_stmt);
            pthread_mutex_unlock(&db_mutex);
            if (out_error) *out_error = strdup("Username already exists!");
            return false;
        }
        sqlite3_finalize(check_stmt);
    }

    char *h = hash_password(password);
    const char *ins_sql = "INSERT INTO users (username, password_hash, display_name, email, role, created_at) VALUES (?, ?, ?, ?, 'STUDENT', ?);";
    sqlite3_stmt *ins_stmt;
    bool success = false;
    if (sqlite3_prepare_v2(db, ins_sql, -1, &ins_stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(ins_stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(ins_stmt, 2, h, -1, SQLITE_STATIC);
        sqlite3_bind_text(ins_stmt, 3, (display_name && strlen(display_name) > 0) ? display_name : username, -1, SQLITE_STATIC);
        sqlite3_bind_text(ins_stmt, 4, email ? email : "", -1, SQLITE_STATIC);
        sqlite3_bind_int64(ins_stmt, 5, (long long)time(NULL) * 1000);
        if (sqlite3_step(ins_stmt) == SQLITE_DONE) {
            success = true;
        }
        sqlite3_finalize(ins_stmt);
    }
    free(h);
    pthread_mutex_unlock(&db_mutex);
    return success;
}

bool db_get_user_profile(const char *username, UserProfile *out_user) {
    if (!username || !out_user) return false;
    pthread_mutex_lock(&db_mutex);
    sqlite3_stmt *stmt;
    const char *sql = "SELECT id, username, display_name, email, role FROM users WHERE username = ? COLLATE NOCASE;";
    bool found = false;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            out_user->id = sqlite3_column_int(stmt, 0);
            strncpy(out_user->username, (const char *)sqlite3_column_text(stmt, 1), sizeof(out_user->username) - 1);
            strncpy(out_user->display_name, (const char *)sqlite3_column_text(stmt, 2), sizeof(out_user->display_name) - 1);
            const char *email = (const char *)sqlite3_column_text(stmt, 3);
            strncpy(out_user->email, email ? email : "", sizeof(out_user->email) - 1);
            strncpy(out_user->role, (const char *)sqlite3_column_text(stmt, 4), sizeof(out_user->role) - 1);
            found = true;
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return found;
}

int db_get_questions(const char *category, const char *difficulty, int count, Question **out_questions) {
    if (count <= 0) count = 5;
    pthread_mutex_lock(&db_mutex);

    char sql[512];
    bool filter_cat = (category && strlen(category) > 0 && strcasecmp(category, "ALL") != 0);
    bool filter_diff = (difficulty && strlen(difficulty) > 0 && strcasecmp(difficulty, "ALL") != 0);

    if (filter_cat && filter_diff) {
        snprintf(sql, sizeof(sql), "SELECT id, content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty FROM questions WHERE category = '%s' AND difficulty = '%s' ORDER BY RANDOM() LIMIT %d;", category, difficulty, count);
    } else if (filter_cat) {
        snprintf(sql, sizeof(sql), "SELECT id, content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty FROM questions WHERE category = '%s' ORDER BY RANDOM() LIMIT %d;", category, count);
    } else if (filter_diff) {
        snprintf(sql, sizeof(sql), "SELECT id, content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty FROM questions WHERE difficulty = '%s' ORDER BY RANDOM() LIMIT %d;", difficulty, count);
    } else {
        snprintf(sql, sizeof(sql), "SELECT id, content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty FROM questions ORDER BY RANDOM() LIMIT %d;", count);
    }

    sqlite3_stmt *stmt;
    int fetched = 0;
    Question *list = NULL;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        list = (Question *)malloc(sizeof(Question) * count);
        while (sqlite3_step(stmt) == SQLITE_ROW && fetched < count) {
            Question *q = &list[fetched];
            q->id = sqlite3_column_int(stmt, 0);
            strncpy(q->content, (const char*)sqlite3_column_text(stmt, 1), sizeof(q->content) - 1);
            strncpy(q->option_a, (const char*)sqlite3_column_text(stmt, 2), sizeof(q->option_a) - 1);
            strncpy(q->option_b, (const char*)sqlite3_column_text(stmt, 3), sizeof(q->option_b) - 1);
            strncpy(q->option_c, (const char*)sqlite3_column_text(stmt, 4), sizeof(q->option_c) - 1);
            strncpy(q->option_d, (const char*)sqlite3_column_text(stmt, 5), sizeof(q->option_d) - 1);
            strncpy(q->correct_option, (const char*)sqlite3_column_text(stmt, 6), sizeof(q->correct_option) - 1);
            const char *exp = (const char*)sqlite3_column_text(stmt, 7);
            strncpy(q->explanation, exp ? exp : "", sizeof(q->explanation) - 1);
            strncpy(q->category, (const char*)sqlite3_column_text(stmt, 8), sizeof(q->category) - 1);
            strncpy(q->difficulty, (const char*)sqlite3_column_text(stmt, 9), sizeof(q->difficulty) - 1);
            fetched++;
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);

    *out_questions = list;
    return fetched;
}

bool db_get_question_by_id(int question_id, Question *out_q) {
    if (!out_q) return false;
    pthread_mutex_lock(&db_mutex);
    sqlite3_stmt *stmt;
    const char *sql = "SELECT id, content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty FROM questions WHERE id = ?;";
    bool found = false;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_int(stmt, 1, question_id);
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            out_q->id = sqlite3_column_int(stmt, 0);
            strncpy(out_q->content, (const char*)sqlite3_column_text(stmt, 1), sizeof(out_q->content) - 1);
            strncpy(out_q->option_a, (const char*)sqlite3_column_text(stmt, 2), sizeof(out_q->option_a) - 1);
            strncpy(out_q->option_b, (const char*)sqlite3_column_text(stmt, 3), sizeof(out_q->option_b) - 1);
            strncpy(out_q->option_c, (const char*)sqlite3_column_text(stmt, 4), sizeof(out_q->option_c) - 1);
            strncpy(out_q->option_d, (const char*)sqlite3_column_text(stmt, 5), sizeof(out_q->option_d) - 1);
            strncpy(out_q->correct_option, (const char*)sqlite3_column_text(stmt, 6), sizeof(out_q->correct_option) - 1);
            const char *exp = (const char*)sqlite3_column_text(stmt, 7);
            strncpy(out_q->explanation, exp ? exp : "", sizeof(out_q->explanation) - 1);
            strncpy(out_q->category, (const char*)sqlite3_column_text(stmt, 8), sizeof(out_q->category) - 1);
            strncpy(out_q->difficulty, (const char*)sqlite3_column_text(stmt, 9), sizeof(out_q->difficulty) - 1);
            found = true;
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return found;
}

bool db_save_exam_record(const char *username, const char *test_type, const char *title,
                         const char *room_code, const char *topic, const char *difficulty,
                         double score, int correct_count, int total_questions, int time_spent_seconds) {
    pthread_mutex_lock(&db_mutex);
    const char *sql = "INSERT INTO exam_records (username, test_type, title, room_code, topic, difficulty, score, correct_count, total_questions, time_spent_seconds, taken_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    sqlite3_stmt *stmt;
    bool success = false;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, test_type ? test_type : "SOLO", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 3, title ? title : "Exam Record", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 4, room_code ? room_code : "", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 5, topic ? topic : "General", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 6, difficulty ? difficulty : "MEDIUM", -1, SQLITE_STATIC);
        sqlite3_bind_double(stmt, 7, score);
        sqlite3_bind_int(stmt, 8, correct_count);
        sqlite3_bind_int(stmt, 9, total_questions);
        sqlite3_bind_int(stmt, 10, time_spent_seconds);
        sqlite3_bind_int64(stmt, 11, (long long)time(NULL) * 1000);
        if (sqlite3_step(stmt) == SQLITE_DONE) {
            success = true;
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return success;
}

cJSON* db_get_user_history(const char *username) {
    pthread_mutex_lock(&db_mutex);
    cJSON *arr = cJSON_CreateArray();
    const char *sql = "SELECT id, username, test_type, title, room_code, topic, difficulty, score, correct_count, total_questions, time_spent_seconds, taken_at FROM exam_records WHERE username = ? COLLATE NOCASE ORDER BY taken_at DESC;";
    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            cJSON *item = cJSON_CreateObject();
            cJSON_AddNumberToObject(item, "id", sqlite3_column_int(stmt, 0));
            cJSON_AddStringToObject(item, "username", (const char*)sqlite3_column_text(stmt, 1));
            cJSON_AddStringToObject(item, "testType", (const char*)sqlite3_column_text(stmt, 2));
            cJSON_AddStringToObject(item, "title", (const char*)sqlite3_column_text(stmt, 3));
            cJSON_AddStringToObject(item, "roomCode", (const char*)sqlite3_column_text(stmt, 4));
            cJSON_AddStringToObject(item, "topic", (const char*)sqlite3_column_text(stmt, 5));
            cJSON_AddStringToObject(item, "difficulty", (const char*)sqlite3_column_text(stmt, 6));
            cJSON_AddNumberToObject(item, "score", sqlite3_column_double(stmt, 7));
            cJSON_AddNumberToObject(item, "correctCount", sqlite3_column_int(stmt, 8));
            cJSON_AddNumberToObject(item, "totalQuestions", sqlite3_column_int(stmt, 9));
            cJSON_AddNumberToObject(item, "timeSpentSeconds", sqlite3_column_int(stmt, 10));
            cJSON_AddNumberToObject(item, "takenAt", (double)sqlite3_column_int64(stmt, 11));
            cJSON_AddItemToArray(arr, item);
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return arr;
}

cJSON* db_get_user_stats(const char *username) {
    pthread_mutex_lock(&db_mutex);
    cJSON *obj = cJSON_CreateObject();
    cJSON_AddStringToObject(obj, "username", username);

    const char *sql = "SELECT COUNT(*), AVG(score), SUM(correct_count), SUM(total_questions), MAX(score) FROM exam_records WHERE username = ? COLLATE NOCASE;";
    sqlite3_stmt *stmt;
    int total_exams = 0;
    double avg_score = 0.0;
    int total_correct = 0;
    int total_q = 0;
    double max_score = 0.0;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            total_exams = sqlite3_column_int(stmt, 0);
            avg_score = sqlite3_column_double(stmt, 1);
            total_correct = sqlite3_column_int(stmt, 2);
            total_q = sqlite3_column_int(stmt, 3);
            max_score = sqlite3_column_double(stmt, 4);
        }
        sqlite3_finalize(stmt);
    }

    cJSON_AddNumberToObject(obj, "totalExamsTaken", total_exams);
    cJSON_AddNumberToObject(obj, "averageScore", avg_score);
    cJSON_AddNumberToObject(obj, "highestScore", max_score);
    cJSON_AddNumberToObject(obj, "totalCorrectAnswers", total_correct);
    cJSON_AddNumberToObject(obj, "totalQuestionsAnswered", total_q);
    double accuracy = (total_q > 0) ? ((double)total_correct / total_q) * 100.0 : 0.0;
    cJSON_AddNumberToObject(obj, "overallAccuracyPercentage", accuracy);

    // Score trend
    cJSON *trend_arr = cJSON_CreateArray();
    const char *trend_sql = "SELECT title, score FROM exam_records WHERE username = ? COLLATE NOCASE ORDER BY taken_at ASC LIMIT 10;";
    if (sqlite3_prepare_v2(db, trend_sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        int idx = 1;
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            cJSON *pt = cJSON_CreateObject();
            cJSON_AddNumberToObject(pt, "testIndex", idx++);
            cJSON_AddStringToObject(pt, "testLabel", (const char*)sqlite3_column_text(stmt, 0));
            double s = sqlite3_column_double(stmt, 1);
            cJSON_AddNumberToObject(pt, "score", s);
            cJSON_AddNumberToObject(pt, "accuracy", s * 10.0);
            cJSON_AddItemToArray(trend_arr, pt);
        }
        sqlite3_finalize(stmt);
    }
    cJSON_AddItemToObject(obj, "scoreTrend", trend_arr);

    pthread_mutex_unlock(&db_mutex);
    return obj;
}

cJSON* db_get_leaderboard(const char *topic) {
    pthread_mutex_lock(&db_mutex);
    cJSON *arr = cJSON_CreateArray();

    char sql[512];
    if (topic && strlen(topic) > 0 && strcasecmp(topic, "ALL") != 0) {
        snprintf(sql, sizeof(sql),
            "SELECT u.username, u.display_name, COUNT(r.id), AVG(r.score), MAX(r.score) "
            "FROM users u JOIN exam_records r ON u.username = r.username "
            "WHERE r.topic = '%s' "
            "GROUP BY u.username ORDER BY MAX(r.score) DESC, AVG(r.score) DESC LIMIT 20;", topic);
    } else {
        snprintf(sql, sizeof(sql),
            "SELECT u.username, u.display_name, COUNT(r.id), AVG(r.score), MAX(r.score) "
            "FROM users u JOIN exam_records r ON u.username = r.username "
            "GROUP BY u.username ORDER BY MAX(r.score) DESC, AVG(r.score) DESC LIMIT 20;");
    }

    sqlite3_stmt *stmt;
    int rank = 1;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            cJSON *item = cJSON_CreateObject();
            cJSON_AddNumberToObject(item, "rank", rank++);
            cJSON_AddStringToObject(item, "username", (const char*)sqlite3_column_text(stmt, 0));
            cJSON_AddStringToObject(item, "displayName", (const char*)sqlite3_column_text(stmt, 1));
            cJSON_AddNumberToObject(item, "examsTaken", sqlite3_column_int(stmt, 2));
            cJSON_AddNumberToObject(item, "averageScore", sqlite3_column_double(stmt, 3));
            cJSON_AddNumberToObject(item, "highestScore", sqlite3_column_double(stmt, 4));
            cJSON_AddItemToArray(arr, item);
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return arr;
}

void db_log_activity(const char *username, const char *action, const char *details, const char *ip_address) {
    pthread_mutex_lock(&db_mutex);
    const char *sql = "INSERT INTO activity_logs (timestamp, username, action, details, ip_address) VALUES (?, ?, ?, ?, ?);";
    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        sqlite3_bind_int64(stmt, 1, (long long)time(NULL) * 1000);
        sqlite3_bind_text(stmt, 2, username ? username : "ANONYMOUS", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 3, action ? action : "UNKNOWN", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 4, details ? details : "", -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 5, ip_address ? ip_address : "127.0.0.1", -1, SQLITE_STATIC);
        sqlite3_step(stmt);
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
}

cJSON* db_get_activity_logs(int limit) {
    pthread_mutex_lock(&db_mutex);
    cJSON *arr = cJSON_CreateArray();
    if (limit <= 0) limit = 50;

    char sql[128];
    snprintf(sql, sizeof(sql), "SELECT id, timestamp, username, action, details, ip_address FROM activity_logs ORDER BY timestamp DESC LIMIT %d;", limit);

    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            cJSON *item = cJSON_CreateObject();
            cJSON_AddNumberToObject(item, "id", sqlite3_column_int(stmt, 0));
            cJSON_AddNumberToObject(item, "timestamp", (double)sqlite3_column_int64(stmt, 1));
            cJSON_AddStringToObject(item, "username", (const char*)sqlite3_column_text(stmt, 2));
            cJSON_AddStringToObject(item, "action", (const char*)sqlite3_column_text(stmt, 3));
            cJSON_AddStringToObject(item, "details", (const char*)sqlite3_column_text(stmt, 4));
            cJSON_AddStringToObject(item, "ipAddress", (const char*)sqlite3_column_text(stmt, 5));
            cJSON_AddItemToArray(arr, item);
        }
        sqlite3_finalize(stmt);
    }
    pthread_mutex_unlock(&db_mutex);
    return arr;
}
