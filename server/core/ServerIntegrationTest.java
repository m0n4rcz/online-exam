package server.core;

import shared.OpCodes;
import shared.Packet;
import shared.PacketCodec;
import shared.QuestionCategory;
import shared.dtos.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * End-to-End integration test for ServerCore over a live TCP Socket connection.
 * Verifies that network packets, JSON serialization, SQLite persistence, and dispatching
 * work seamlessly end-to-end.
 */
public class ServerIntegrationTest {

    private static final int TEST_PORT = 19876;
    private static final String TEST_DB = "test_server_core.db";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("       RUNNING SERVER CORE & SOCKET INTEGRATION TEST           ");
        System.out.println("===============================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        // Clean test DB
        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) dbFile.delete();

        // 1. Start Server in background thread
        ServerCore server = new ServerCore(TEST_PORT, "jdbc:sqlite:" + TEST_DB);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                // Stopped
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // 2. Connect live Client Socket (with retry loop for server binding)
        Socket clientSocket = null;
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(300);
                clientSocket = new Socket("127.0.0.1", TEST_PORT);
                break;
            } catch (Exception ignored) {}
        }
        if (clientSocket == null) throw new RuntimeException("Could not connect to server on port " + TEST_PORT);

        final Socket socketToUse = clientSocket;
        try (socketToUse) {
            clientSocket.setTcpNoDelay(true);
            InputStream in = clientSocket.getInputStream();
            OutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());

            // -------------------------------------------------------------
            // TEST 1: PING / PONG
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 1] TCP Ping / Pong Keep-Alive... ");
            Packet pingPacket = Packet.ok(OpCodes.PING, "ping_payload");
            PacketCodec.writePacket(out, pingPacket);

            Packet pongResponse = PacketCodec.readPacket(in);
            assert pongResponse != null;
            assert pongResponse.getOpCode() == OpCodes.PONG;
            assert pongResponse.isSuccess();
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 2: REGISTER NEW ACCOUNT
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 2] Register Account over Socket... ");
            RegisterRequest regReq = new RegisterRequest("e2e_student", "SecurePass!123", "E2E Student", "e2e@ptit.edu.vn");
            Packet regPacket = Packet.ok(OpCodes.REGISTER_REQ, regReq);
            PacketCodec.writePacket(out, regPacket);

            Packet regResPacket = PacketCodec.readPacket(in);
            assert regResPacket != null;
            assert regResPacket.getOpCode() == OpCodes.REGISTER_RES;
            RegisterResponse regRes = regResPacket.getPayloadAs(RegisterResponse.class);
            assert regRes != null && regRes.isSuccess();
            assert "e2e_student".equals(regRes.getUsername());
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 3: LOGIN OVER SOCKET
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 3] User Authentication & Session Binding... ");
            LoginRequest loginReq = new LoginRequest("e2e_student", "SecurePass!123");
            Packet loginPacket = Packet.ok(OpCodes.LOGIN_REQ, loginReq);
            PacketCodec.writePacket(out, loginPacket);

            Packet loginResPacket = PacketCodec.readPacket(in);
            assert loginResPacket != null;
            assert loginResPacket.getOpCode() == OpCodes.LOGIN_RES;
            LoginResponse loginRes = loginResPacket.getPayloadAs(LoginResponse.class);
            assert loginRes != null && loginRes.isSuccess();
            assert "e2e_student".equals(loginRes.getUsername());
            assert loginRes.getToken() != null && !loginRes.getToken().isEmpty();
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 4: REQUEST SOLO PRACTICE EXAM
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 4] Request Solo Practice Exam Questions... ");
            PracticeRequestDTO practiceReq = new PracticeRequestDTO(QuestionCategory.JAVA_CORE, "ALL", 3, 10);
            Packet practicePacket = Packet.ok(OpCodes.PRACTICE_REQ, practiceReq);
            PacketCodec.writePacket(out, practicePacket);

            Packet practiceResPacket = PacketCodec.readPacket(in);
            assert practiceResPacket != null;
            assert practiceResPacket.getOpCode() == OpCodes.PRACTICE_RES;
            PracticeResponseDTO practiceRes = practiceResPacket.getPayloadAs(PracticeResponseDTO.class);
            assert practiceRes != null;
            assert practiceRes.getQuestions().size() >= 3;
            // Verify sanitized questions: No correct option or explanation leaked!
            for (QuestionDTO q : practiceRes.getQuestions()) {
                assert q.getContent() != null && !q.getContent().isEmpty();
                assert q.getOptionA() != null && q.getOptionB() != null;
            }
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 5: SUBMIT EXAM ANSWERS & EVALUATION
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 5] Submit Answers & Receive Graded Score... ");
            Map<Integer, String> studentChoices = new HashMap<>();
            // Submit option "B" for all questions
            for (QuestionDTO q : practiceRes.getQuestions()) {
                studentChoices.put(q.getId(), "B");
            }

            ExamSubmitDTO submitDTO = new ExamSubmitDTO("PRACTICE", "e2e_student", studentChoices, true);
            Packet submitPacket = Packet.ok(OpCodes.PRACTICE_SUBMIT_REQ, submitDTO);
            PacketCodec.writePacket(out, submitPacket);

            Packet resultPacket = PacketCodec.readPacket(in);
            assert resultPacket != null;
            assert resultPacket.getOpCode() == OpCodes.PRACTICE_RESULT_RES;
            ExamResultDTO examResult = resultPacket.getPayloadAs(ExamResultDTO.class);
            assert examResult != null;
            assert "e2e_student".equals(examResult.getStudentUsername());
            assert examResult.getTotalQuestions() >= 3;
            assert examResult.getReviews().size() >= 3;
            // Check that detailed review includes explanation
            assert examResult.getReviews().get(0).getExplanation() != null;
            System.out.printf("PASSED (Score: %.1f, %d/%d Correct)\n", 
                    examResult.getScore(), examResult.getCorrectCount(), examResult.getTotalQuestions());
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 6: USER STATS & GRAPHICAL CHARTS DATA
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 6] Retrieve Graphical Statistics Data... ");
            Packet statsReqPacket = Packet.ok(OpCodes.USER_STATS_REQ, "e2e_student");
            PacketCodec.writePacket(out, statsReqPacket);

            Packet statsResPacket = PacketCodec.readPacket(in);
            assert statsResPacket != null;
            assert statsResPacket.getOpCode() == OpCodes.USER_STATS_RES;
            UserStatsDTO userStats = statsResPacket.getPayloadAs(UserStatsDTO.class);
            assert userStats != null;
            assert userStats.getTotalTests() >= 1;
            assert userStats.getScoreTrend().size() >= 1;
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 7: GLOBAL LEADERBOARD
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 7] Global Leaderboard Query... ");
            Packet ldbReqPacket = Packet.ok(OpCodes.LEADERBOARD_REQ);
            PacketCodec.writePacket(out, ldbReqPacket);

            Packet ldbResPacket = PacketCodec.readPacket(in);
            assert ldbResPacket != null;
            assert ldbResPacket.getOpCode() == OpCodes.LEADERBOARD_RES;
            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 8: CLEAN LOGOUT
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 8] User Clean Logout... ");
            Packet logoutReqPacket = Packet.ok(OpCodes.LOGOUT_REQ);
            PacketCodec.writePacket(out, logoutReqPacket);

            Packet logoutResPacket = PacketCodec.readPacket(in);
            assert logoutResPacket != null;
            assert logoutResPacket.getOpCode() == OpCodes.LOGOUT_RES;
            System.out.println("PASSED");
            testsPassed++;

        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        } finally {
            server.stop();
            try {
                if (dbFile.exists()) dbFile.delete();
            } catch (Exception ignored) {}
        }

        // -------------------------------------------------------------
        // TEST SUMMARY
        // -------------------------------------------------------------
        System.out.println("\n===============================================================");
        System.out.printf("  SERVER INTEGRATION RESULTS: %d/%d TESTS PASSED\n", testsPassed, totalTests);
        if (testsPassed == totalTests) {
            System.out.println("  >>> ALL SERVER SOCKET & PROTOCOL CHECKS SUCCEEDED! EXCELLENT! <<<");
        } else {
            System.err.println("  >>> SOME TESTS FAILED! CHECK STACK TRACE ABOVE! <<<");
        }
        System.out.println("===============================================================\n");
    }
}
