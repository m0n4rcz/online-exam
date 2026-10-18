package client;

import client.net.NetworkClient;
import server.core.ServerCore;
import shared.OpCodes;
import shared.Packet;
import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.dtos.*;

import java.io.File;

/**
 * Headless end-to-end integration test verifying that NetworkClient,
 * serialization, request-response matching, and data exchange work flawlessly.
 */
public class ClientIntegrationTest {

    private static final int PORT = 19879;
    private static final String TEST_DB = "test_client_integration.db";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("         RUNNING NETWORK CLIENT INTEGRATION TEST               ");
        System.out.println("===============================================================\n");

        int passed = 0;
        int total = 0;

        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) dbFile.delete();

        ServerCore server = new ServerCore(PORT, "jdbc:sqlite:" + TEST_DB);
        Thread sThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception ignored) {}
        });
        sThread.setDaemon(true);
        sThread.start();

        NetworkClient client = new NetworkClient();

        try {
            // Wait for server to bind and connect
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(300);
                    client.connect("127.0.0.1", PORT);
                    break;
                } catch (Exception ignored) {}
            }
            assert client.isConnected() : "Client should be connected";

            // TEST 1: Register
            total++;
            System.out.print("[TEST 1] Register account via NetworkClient... ");
            RegisterRequest regReq = new RegisterRequest("student_gui", "pass123", "GUI Student", "gui@test.com");
            Packet regRes = client.sendRequest(Packet.ok(OpCodes.REGISTER_REQ, regReq), OpCodes.REGISTER_RES).get();
            assert regRes.isSuccess();
            System.out.println("PASSED");
            passed++;

            // TEST 2: Login
            total++;
            System.out.print("[TEST 2] Login via NetworkClient... ");
            LoginRequest loginReq = new LoginRequest("student_gui", "pass123");
            Packet loginRes = client.sendRequest(Packet.ok(OpCodes.LOGIN_REQ, loginReq), OpCodes.LOGIN_RES).get();
            assert loginRes.isSuccess();
            LoginResponse lr = loginRes.getPayloadAs(LoginResponse.class);
            assert "student_gui".equals(lr.getUsername());
            client.setCurrentUser(lr.toUserProfileDTO());
            System.out.println("PASSED");
            passed++;

            // TEST 3: Create Room
            total++;
            System.out.print("[TEST 3] Create Room via NetworkClient... ");
            CreateRoomRequest crReq = new CreateRoomRequest("Networking Final", 10, 5,
                    QuestionCategory.NETWORKING, QuestionDifficulty.ALL, 10, null);
            Packet crRes = client.sendRequest(Packet.ok(OpCodes.CREATE_ROOM_REQ, crReq), OpCodes.CREATE_ROOM_RES).get();
            assert crRes.isSuccess();
            CreateRoomResponse cr = crRes.getPayloadAs(CreateRoomResponse.class);
            String roomCode = cr.getRoomCode();
            assert roomCode != null && !roomCode.isEmpty();
            System.out.println("PASSED (RoomCode: " + roomCode + ")");
            passed++;

            // TEST 4: Room List
            total++;
            System.out.print("[TEST 4] Fetch Room List... ");
            Packet rlRes = client.sendRequest(Packet.ok(OpCodes.ROOM_LIST_REQ), OpCodes.ROOM_LIST_RES).get();
            assert rlRes.isSuccess();
            RoomListResponse rlr = rlRes.getPayloadAs(RoomListResponse.class);
            assert rlr.getRooms().size() >= 1;
            assert roomCode.equals(rlr.getRooms().get(0).getRoomCode());
            System.out.println("PASSED (Active rooms: " + rlr.getRooms().size() + ")");
            passed++;

            // TEST 5: Practice Request
            total++;
            System.out.print("[TEST 5] Solo Practice Request & Response... ");
            PracticeRequestDTO prReq = new PracticeRequestDTO(QuestionCategory.ALL, QuestionDifficulty.ALL, 4, 10);
            Packet prRes = client.sendRequest(Packet.ok(OpCodes.PRACTICE_REQ, prReq), OpCodes.PRACTICE_RES).get();
            assert prRes.isSuccess();
            PracticeResponseDTO pr = prRes.getPayloadAs(PracticeResponseDTO.class);
            assert pr.getQuestions().size() >= 4;
            System.out.println("PASSED (Received " + pr.getQuestions().size() + " questions)");
            passed++;

            // TEST 6: Practice Submission & Grading
            total++;
            System.out.print("[TEST 6] Practice Submit & Instant Score Review... ");
            java.util.Map<Integer, String> answers = new java.util.HashMap<>();
            for (QuestionDTO q : pr.getQuestions()) {
                answers.put(q.getId(), "A");
            }
            ExamSubmitDTO subDTO = new ExamSubmitDTO("PRACTICE", "student_gui", answers, true);
            Packet subRes = client.sendRequest(Packet.ok(OpCodes.PRACTICE_SUBMIT_REQ, subDTO), OpCodes.PRACTICE_RESULT_RES).get();
            assert subRes.isSuccess();
            ExamResultDTO result = subRes.getPayloadAs(ExamResultDTO.class);
            assert result.getTotalQuestions() >= 4;
            assert result.getReviews().size() >= 4;
            System.out.printf("PASSED (Score: %.1f, Correct: %d/%d)\n",
                    result.getScore(), result.getCorrectCount(), result.getTotalQuestions());
            passed++;

            // TEST 7: Graphical User Stats Query
            total++;
            System.out.print("[TEST 7] Fetch User Graphical Stats & Trendlines... ");
            Packet statsRes = client.sendRequest(Packet.ok(OpCodes.USER_STATS_REQ, "student_gui"), OpCodes.USER_STATS_RES).get();
            assert statsRes.isSuccess();
            UserStatsDTO stats = statsRes.getPayloadAs(UserStatsDTO.class);
            assert stats.getTotalTests() >= 1;
            assert !stats.getScoreTrend().isEmpty();
            System.out.println("PASSED (Trend points: " + stats.getScoreTrend().size() + ")");
            passed++;

            // TEST 8: Leaderboard
            total++;
            System.out.print("[TEST 8] Fetch Global Leaderboard... ");
            Packet ldbRes = client.sendRequest(Packet.ok(OpCodes.LEADERBOARD_REQ), OpCodes.LEADERBOARD_RES).get();
            assert ldbRes.isSuccess();
            java.util.List<LeaderboardEntryDTO> ldb = ldbRes.getPayloadAsList(LeaderboardEntryDTO.class);
            assert !ldb.isEmpty();
            System.out.println("PASSED (Rankings: " + ldb.size() + ")");
            passed++;

        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        } finally {
            client.disconnect();
            server.stop();
            if (dbFile.exists()) dbFile.delete();
        }

        System.out.println("\n===============================================================");
        System.out.printf("  CLIENT INTEGRATION RESULTS: %d/%d TESTS PASSED\n", passed, total);
        if (passed == total) {
            System.out.println("  >>> ALL NETWORK CLIENT CHECKS SUCCEEDED! EXCELLENT! <<<");
        } else {
            System.err.println("  >>> SOME TESTS FAILED! <<<");
        }
        System.out.println("===============================================================\n");
    }
}
