package server.db;

import server.db.dao.ActivityLogDAO;
import server.db.dao.ExamRecordDAO;
import server.db.dao.QuestionDAO;
import server.db.dao.UserDAO;
import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.UserRole;
import shared.dtos.*;

import java.io.File;
import java.util.List;

/**
 * Self-test harness verifying SQLite schema creation, data seeding,
 * and all DAO operations (Users, Questions, Exam Records, Statistics, Logs).
 */
public class DatabaseSelfTest {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("     RUNNING ONLINE EXAM SYSTEM DATABASE SELF-TEST SUITE      ");
        System.out.println("===============================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        // Use a dedicated test database file so we don't pollute production data
        String testDbFile = "test_exam_system.db";
        File f = new File(testDbFile);
        if (f.exists()) {
            f.delete();
        }

        String dbUrl = "jdbc:sqlite:" + testDbFile;

        // -------------------------------------------------------------
        // TEST 1: Database Initialization & Seeding
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 1] Database Initialization & Seed Data... ");
        try {
            DatabaseManager.initialize(dbUrl);
            assert DatabaseManager.isInitialized() : "Database should be marked initialized";
            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        UserDAO userDAO = new UserDAO();
        QuestionDAO questionDAO = new QuestionDAO();
        ExamRecordDAO examRecordDAO = new ExamRecordDAO();
        ActivityLogDAO logDAO = new ActivityLogDAO();

        // -------------------------------------------------------------
        // TEST 2: Seeded Users Verification & Default Login
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 2] Default Users & Password Hashing Verification... ");
        try {
            // Admin check
            UserProfileDTO adminProfile = userDAO.login("admin", "admin123");
            assert adminProfile != null : "Admin login should succeed";
            assert UserRole.ADMIN.equals(adminProfile.getRole());

            // Wrong password check
            UserProfileDTO failedLogin = userDAO.login("admin", "wrong_password");
            assert failedLogin == null : "Login with incorrect password must fail";

            // Student login check
            UserProfileDTO studentProfile = userDAO.login("student01", "student123");
            assert studentProfile != null : "student01 login should succeed";
            assert UserRole.STUDENT.equals(studentProfile.getRole());

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 3: User Registration & Profile Update
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 3] User Registration, Duplicate Prevention & Profile Update... ");
        try {
            // Register new user
            UserProfileDTO newUser = userDAO.register("newbie99", "secretPass99", "Newbie Learner", "newbie@sis.hust.edu.vn", UserRole.STUDENT);
            assert newUser != null;
            assert "newbie99".equals(newUser.getUsername());

            // Verify login with new user
            UserProfileDTO loggedIn = userDAO.login("newbie99", "secretPass99");
            assert loggedIn != null;

            // Attempt duplicate username
            boolean duplicateFailed = false;
            try {
                userDAO.register("newbie99", "otherPass", "Duplicate", "dup@test.com", UserRole.STUDENT);
            } catch (IllegalStateException e) {
                duplicateFailed = true;
            }
            assert duplicateFailed : "Registering duplicate username must throw IllegalStateException";

            // Update profile
            boolean updated = userDAO.updateProfile("newbie99", "Newbie Pro Gamer", "newbie_pro@sis.hust.edu.vn");
            assert updated;
            UserProfileDTO refreshed = userDAO.findByUsername("newbie99");
            assert "Newbie Pro Gamer".equals(refreshed.getDisplayName());
            assert "newbie_pro@sis.hust.edu.vn".equals(refreshed.getEmail());

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 4: Seeded Questions & Filtered Random Sampling
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 4] Seeded Questions & Random Exam Paper Generation... ");
        try {
            List<FullQuestionDTO> allQ = questionDAO.getAllQuestions();
            assert allQ.size() >= 15 : "Should have at least 15 seeded questions, got: " + allQ.size();

            // Random sampling by category: Computer Networking
            List<FullQuestionDTO> netQuestions = questionDAO.getRandomQuestions(QuestionCategory.NETWORKING, "ALL", 3);
            assert netQuestions.size() >= 3 : "Should retrieve requested question count";
            for (FullQuestionDTO q : netQuestions) {
                assert QuestionCategory.NETWORKING.equals(q.getCategory()) : "Question must belong to NETWORKING";
                assert q.getOptionA() != null && q.getOptionB() != null;
                assert q.getCorrectOption() != null;
            }

            // Random sampling across ALL categories
            List<FullQuestionDTO> generalExam = questionDAO.getRandomQuestions("ALL", "ALL", 5);
            assert generalExam.size() == 5 : "Should retrieve exactly 5 questions";

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 5: Question Management CRUD
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 5] Question CRUD (Add, Update, Delete)... ");
        try {
            FullQuestionDTO newQ = new FullQuestionDTO(
                    0,
                    "What does HTTP stand for?",
                    "HyperText Transfer Protocol",
                    "HyperText Transmission Program",
                    "High Traffic Test Process",
                    "Host Token Transfer Protocol",
                    "A",
                    "HTTP stands for HyperText Transfer Protocol.",
                    QuestionCategory.NETWORKING,
                    QuestionDifficulty.EASY
            );

            int qId = questionDAO.addQuestion(newQ);
            assert qId > 0 : "Generated question ID must be positive";

            FullQuestionDTO fetched = questionDAO.getQuestionById(qId);
            assert fetched != null;
            assert "A".equals(fetched.getCorrectOption());
            assert "What does HTTP stand for?".equals(fetched.getContent());

            // Update
            fetched.setContent("What does HTTPS stand for?");
            fetched.setExplanation("HTTPS is HyperText Transfer Protocol Secure.");
            boolean updOk = questionDAO.updateQuestion(fetched);
            assert updOk;

            FullQuestionDTO updatedQ = questionDAO.getQuestionById(qId);
            assert "What does HTTPS stand for?".equals(updatedQ.getContent());

            // Delete
            boolean delOk = questionDAO.deleteQuestion(qId);
            assert delOk;
            assert questionDAO.getQuestionById(qId) == null;

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 6: Exam Records, History & Multiplayer Room Ranking
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 6] Exam Records & Multiplayer Room Ranking... ");
        try {
            // Save test 1 for student01
            UserExamRecordDTO rec1 = new UserExamRecordDTO(
                    0, "student01", "PRACTICE", "Solo Java Practice", null,
                    QuestionCategory.JAVA_CORE, QuestionDifficulty.EASY,
                    8.0, 8, 10, 300, System.currentTimeMillis() - 3600000);
            long id1 = examRecordDAO.saveRecord(rec1);
            assert id1 > 0;

            // Save test 2 for student01 in Room
            UserExamRecordDTO rec2 = new UserExamRecordDTO(
                    0, "student01", "ROOM", "Room NET101 Exam", "NET101",
                    QuestionCategory.NETWORKING, QuestionDifficulty.MEDIUM,
                    9.0, 9, 10, 450, System.currentTimeMillis() - 1800000);
            long id2 = examRecordDAO.saveRecord(rec2);
            assert id2 > 0;

            // Save competitor test for student02 in same Room
            UserExamRecordDTO rec3 = new UserExamRecordDTO(
                    0, "student02", "ROOM", "Room NET101 Exam", "NET101",
                    QuestionCategory.NETWORKING, QuestionDifficulty.MEDIUM,
                    10.0, 10, 10, 420, System.currentTimeMillis() - 1800000);
            examRecordDAO.saveRecord(rec3);

            // History for student01
            List<UserExamRecordDTO> history = examRecordDAO.getHistoryByUsername("student01", 10);
            assert history.size() == 2;
            assert history.get(0).getRecordId() == id2 : "Newer record should be first";

            // Room leaderboard
            List<UserExamRecordDTO> roomResults = examRecordDAO.getRoomResults("NET101");
            assert roomResults.size() == 2;
            assert "student02".equals(roomResults.get(0).getUsername()) : "Higher score must be first";

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 7: Graphical Statistics Aggregation (UserStatsDTO)
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 7] Graphical Statistics Dataset Calculation... ");
        try {
            UserStatsDTO stats = examRecordDAO.getUserStats("student01");
            assert stats.getTotalTests() == 2;
            assert stats.getPracticeTests() == 1;
            assert stats.getRoomTests() == 1;
            assert stats.getHighestScore() == 9.0;
            assert stats.getLowestScore() == 8.0;
            assert stats.getAverageScore() == 8.5;
            assert stats.getTotalQuestionsAnswered() == 20;
            assert stats.getTotalCorrectAnswers() == 17;
            assert stats.getOverallAccuracyPercentage() == 85.0;

            // Topic Performance Chart Data
            assert stats.getTopicAccuracy().containsKey(QuestionCategory.JAVA_CORE);
            assert stats.getTopicAccuracy().containsKey(QuestionCategory.NETWORKING);
            assert stats.getTopicAccuracy().get(QuestionCategory.JAVA_CORE) == 80.0;
            assert stats.getTopicAccuracy().get(QuestionCategory.NETWORKING) == 90.0;

            // Score trend points for Line Chart
            assert stats.getScoreTrend().size() == 2;
            assert stats.getScoreTrend().get(0).getScore() == 8.0;
            assert stats.getScoreTrend().get(1).getScore() == 9.0;

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 8: Global Leaderboard Ranking
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 8] Global Leaderboard Calculation... ");
        try {
            List<LeaderboardEntryDTO> leaderboard = examRecordDAO.getLeaderboard(10);
            assert leaderboard.size() >= 2;
            assert leaderboard.get(0).getRank() == 1;
            assert leaderboard.get(0).getHighestScore() >= leaderboard.get(1).getHighestScore();

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // TEST 9: Activity Logging Audit Trail
        // -------------------------------------------------------------
        totalTests++;
        System.out.print("[TEST 9] Audit Activity Logging & Retrieval... ");
        try {
            long logId1 = logDAO.log("student01", "LOGIN", "Successful login from 192.168.1.10", "192.168.1.10");
            long logId2 = logDAO.log("student01", "SUBMIT_EXAM", "Submitted room NET101 with score 9.0", "192.168.1.10");
            assert logId1 > 0 && logId2 > 0;

            List<ActivityLogDTO> userLogs = logDAO.getLogsByUsername("student01", 10);
            assert userLogs.size() >= 2;
            assert "SUBMIT_EXAM".equals(userLogs.get(0).getAction());
            assert userLogs.get(0).getFormattedTime() != null;

            List<ActivityLogDTO> allLogs = logDAO.getRecentLogs(10);
            assert allLogs.size() >= 2;

            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // -------------------------------------------------------------
        // CLEANUP
        // -------------------------------------------------------------
        try {
            new File(testDbFile).deleteOnExit();
        } catch (Exception ignored) {}

        // -------------------------------------------------------------
        // SUMMARY
        // -------------------------------------------------------------
        System.out.println("\n===============================================================");
        System.out.printf("  TEST RESULTS: %d/%d TESTS PASSED\n", testsPassed, totalTests);
        if (testsPassed == totalTests) {
            System.out.println("  >>> ALL DATABASE LAYER & DAO CHECKS SUCCEEDED! EXCELLENT! <<<");
        } else {
            System.err.println("  >>> SOME TESTS FAILED! CHECK STACK TRACE ABOVE! <<<");
        }
        System.out.println("===============================================================\n");
    }
}
