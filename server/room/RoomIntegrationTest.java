package server.room;

import server.core.ServerCore;
import shared.OpCodes;
import shared.Packet;
import shared.PacketCodec;
import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.dtos.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * End-to-End integration test simulating a 2-player multiplayer room match over live TCP Sockets.
 * Tests: Room creation, Lobby listing, Joining, In-room chat, Ready toggle,
 * Countdown tick, In-flight answer changes, Early submission, Real-time grading,
 * and Final Room Leaderboard.
 */
public class RoomIntegrationTest {

    private static final int TEST_PORT = 19877;
    private static final String TEST_DB = "test_multiplayer_room.db";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("     RUNNING MULTIPLAYER EXAM ROOM & ENGINE INTEGRATION TEST   ");
        System.out.println("===============================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) dbFile.delete();

        ServerCore server = new ServerCore(TEST_PORT, "jdbc:sqlite:" + TEST_DB);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Socket hostSocket = null;
        Socket studentSocket = null;

        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(300);
                if (hostSocket == null) hostSocket = new Socket("127.0.0.1", TEST_PORT);
                if (studentSocket == null) studentSocket = new Socket("127.0.0.1", TEST_PORT);
                break;
            } catch (Exception ignored) {}
        }
        if (hostSocket == null || studentSocket == null) {
            throw new RuntimeException("Could not connect host or student to server on port " + TEST_PORT);
        }

        try {
            hostSocket.setTcpNoDelay(true);
            InputStream hostIn = hostSocket.getInputStream();
            OutputStream hostOut = new BufferedOutputStream(hostSocket.getOutputStream());

            studentSocket.setTcpNoDelay(true);
            InputStream studentIn = studentSocket.getInputStream();
            OutputStream studentOut = new BufferedOutputStream(studentSocket.getOutputStream());

            // -------------------------------------------------------------
            // TEST 1: Register & Login Both Players
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 1] Register & Authenticate Host and Student... ");
            // Host
            PacketCodec.writePacket(hostOut, Packet.ok(OpCodes.REGISTER_REQ, 
                    new RegisterRequest("host_teacher", "Pass123!", "Prof. Host", "host@test.com")));
            Packet hostRegRes = PacketCodec.readPacket(hostIn);
            assert hostRegRes.isSuccess();

            PacketCodec.writePacket(hostOut, Packet.ok(OpCodes.LOGIN_REQ, new LoginRequest("host_teacher", "Pass123!")));
            Packet hostLoginRes = PacketCodec.readPacket(hostIn);
            assert hostLoginRes.isSuccess();

            // Student
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.REGISTER_REQ, 
                    new RegisterRequest("student_player", "Pass123!", "Player One", "player@test.com")));
            Packet studentRegRes = PacketCodec.readPacket(studentIn);
            assert studentRegRes.isSuccess();

            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.LOGIN_REQ, new LoginRequest("student_player", "Pass123!")));
            Packet studentLoginRes = PacketCodec.readPacket(studentIn);
            assert studentLoginRes.isSuccess();

            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 2: Host Creates Multiplayer Room
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 2] Host Creates Multiplayer Room... ");
            CreateRoomRequest createReq = new CreateRoomRequest(
                    "Network Midterm 2026", 3, 5, QuestionCategory.NETWORKING, QuestionDifficulty.ALL, 10, null);
            PacketCodec.writePacket(hostOut, Packet.ok(OpCodes.CREATE_ROOM_REQ, createReq));

            Packet createResPacket = PacketCodec.readPacket(hostIn);
            assert createResPacket.isSuccess();
            CreateRoomResponse createRes = createResPacket.getPayloadAs(CreateRoomResponse.class);
            assert createRes != null && createRes.isSuccess();
            String roomCode = createRes.getRoomCode();
            assert roomCode != null && roomCode.startsWith("ROOM");
            System.out.println("PASSED (RoomCode: " + roomCode + ")");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 3: Student Lists Rooms and Joins
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 3] Student Lists Rooms & Joins Room... ");
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.ROOM_LIST_REQ));
            Packet roomListPacket = PacketCodec.readPacket(studentIn);
            RoomListResponse roomList = roomListPacket.getPayloadAs(RoomListResponse.class);
            assert roomList != null && !roomList.getRooms().isEmpty();
            assert roomCode.equals(roomList.getRooms().get(0).getRoomCode());

            // Join
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.JOIN_ROOM_REQ, new JoinRoomRequest(roomCode)));
            Packet joinResPacket = PacketCodec.readPacket(studentIn);
            assert joinResPacket.isSuccess();
            JoinRoomResponse joinRes = joinResPacket.getPayloadAs(JoinRoomResponse.class);
            assert joinRes.isSuccess();
            assert joinRes.getRoomDetails().getParticipants().size() == 2;

            // Host receives EVENT_ROOM_UPDATE
            Packet hostRoomUpdate = PacketCodec.readPacket(hostIn);
            assert hostRoomUpdate.getOpCode() == OpCodes.EVENT_ROOM_UPDATE;

            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 4: In-Room Chat Broadcast
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 4] In-Room Chat Broadcast... ");
            ChatMessageDTO chatMsg = new ChatMessageDTO(roomCode, "student_player", "Player One", "Hello Teacher!");
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.ROOM_CHAT_REQ, chatMsg));

            // Student receives chat ack
            Packet studentChatAck = PacketCodec.readPacket(studentIn);
            assert studentChatAck.isSuccess();

            // Host receives broadcast EVENT_ROOM_CHAT
            Packet hostBroadcast = PacketCodec.readPacket(hostIn);
            assert hostBroadcast.getOpCode() == OpCodes.EVENT_ROOM_CHAT;
            ChatMessageDTO receivedChat = hostBroadcast.getPayloadAs(ChatMessageDTO.class);
            assert "Hello Teacher!".equals(receivedChat.getMessage());

            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 5: Toggle Ready Status
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 5] Student Toggles Ready Status... ");
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.TOGGLE_READY_REQ));
            Packet toggleRes = PacketCodec.readPacket(studentIn);
            assert toggleRes.isSuccess();

            // Host receives EVENT_ROOM_UPDATE
            Packet hostUpdateReady = PacketCodec.readPacket(hostIn);
            assert hostUpdateReady.getOpCode() == OpCodes.EVENT_ROOM_UPDATE;

            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 6: Host Starts Room & Receives Questions
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 6] Host Starts Room & Exam Questions Broadcast... ");
            PacketCodec.writePacket(hostOut, Packet.ok(OpCodes.START_ROOM_REQ));

            // Student receives EVENT_ROOM_START then EXAM_INIT_DATA
            Packet studentP1 = PacketCodec.readPacket(studentIn);
            assert studentP1.getOpCode() == OpCodes.EVENT_ROOM_START;

            Packet studentInitDataPacket = PacketCodec.readPacket(studentIn);
            assert studentInitDataPacket.getOpCode() == OpCodes.EXAM_INIT_DATA;
            ExamInitDataDTO studentInitData = studentInitDataPacket.getPayloadAs(ExamInitDataDTO.class);
            assert studentInitData != null && studentInitData.getQuestions().size() == 3;

            // Host receives EVENT_ROOM_START, EXAM_INIT_DATA, and START_ROOM_RES
            Packet hostP1 = PacketCodec.readPacket(hostIn);
            Packet hostP2 = PacketCodec.readPacket(hostIn);
            Packet hostP3 = PacketCodec.readPacket(hostIn);
            java.util.Set<Short> hostOpCodes = java.util.Set.of(hostP1.getOpCode(), hostP2.getOpCode(), hostP3.getOpCode());
            assert hostOpCodes.contains(OpCodes.EVENT_ROOM_START);
            assert hostOpCodes.contains(OpCodes.EXAM_INIT_DATA);
            assert hostOpCodes.contains(OpCodes.START_ROOM_RES);

            ExamInitDataDTO hostInitData = hostP1.getOpCode() == OpCodes.EXAM_INIT_DATA 
                    ? hostP1.getPayloadAs(ExamInitDataDTO.class)
                    : (hostP2.getOpCode() == OpCodes.EXAM_INIT_DATA ? hostP2.getPayloadAs(ExamInitDataDTO.class) : hostP3.getPayloadAs(ExamInitDataDTO.class));
            assert hostInitData != null && hostInitData.getQuestions().size() == 3;

            System.out.println("PASSED (Both received 3 sanitized questions)");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 7: In-Flight Answer Change & Tick
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 7] In-Flight Answer Updating & Time Ticks... ");
            // Wait for at least one tick
            Packet tickPacket = PacketCodec.readPacket(studentIn);
            assert tickPacket.getOpCode() == OpCodes.EVENT_EXAM_TICK;

            // Student selects option "B" for question 1
            int firstQId = studentInitData.getQuestions().get(0).getId();
            AnswerSubmissionDTO ansChange = new AnswerSubmissionDTO(roomCode, firstQId, "B");
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.SUBMIT_ANSWER_REQ, ansChange));
            Packet ansChangeRes = PacketCodec.readPacket(studentIn);
            while (ansChangeRes.getOpCode() == OpCodes.EVENT_EXAM_TICK) {
                ansChangeRes = PacketCodec.readPacket(studentIn);
            }
            assert ansChangeRes.isSuccess();

            System.out.println("PASSED");
            testsPassed++;

            // -------------------------------------------------------------
            // TEST 8: Submit Exam & Grading / Leaderboard
            // -------------------------------------------------------------
            totalTests++;
            System.out.print("[TEST 8] Early Exam Submission & Leaderboard Broadcast... ");

            // Host submits
            Map<Integer, String> hostAnswers = new HashMap<>();
            for (QuestionDTO q : hostInitData.getQuestions()) {
                hostAnswers.put(q.getId(), "B"); // Guess B
            }
            ExamSubmitDTO hostSubmit = new ExamSubmitDTO(roomCode, "host_teacher", hostAnswers, true);
            PacketCodec.writePacket(hostOut, Packet.ok(OpCodes.SUBMIT_EXAM_REQ, hostSubmit));

            // Drain any pending ticks from student or host
            Packet hostSubAck = PacketCodec.readPacket(hostIn);
            while (hostSubAck.getOpCode() == OpCodes.EVENT_EXAM_TICK 
                    || hostSubAck.getOpCode() == OpCodes.EVENT_PARTICIPANT_SUBMITTED) {
                hostSubAck = PacketCodec.readPacket(hostIn);
            }
            assert hostSubAck.getOpCode() == OpCodes.SUBMIT_EXAM_RES;

            // Student submits
            Map<Integer, String> studentAnswers = new HashMap<>();
            for (QuestionDTO q : studentInitData.getQuestions()) {
                studentAnswers.put(q.getId(), "C"); // Guess C
            }
            ExamSubmitDTO studentSubmit = new ExamSubmitDTO(roomCode, "student_player", studentAnswers, true);
            PacketCodec.writePacket(studentOut, Packet.ok(OpCodes.SUBMIT_EXAM_REQ, studentSubmit));

            boolean studentGotResult = false;
            boolean studentGotSubmitAck = false;
            Packet p;
            while ((p = PacketCodec.readPacket(studentIn)) != null) {
                if (p.getOpCode() == OpCodes.EXAM_RESULT_RES) studentGotResult = true;
                if (p.getOpCode() == OpCodes.SUBMIT_EXAM_RES) studentGotSubmitAck = true;
                if (studentGotResult && studentGotSubmitAck) break;
            }
            assert studentGotResult : "Student should receive EXAM_RESULT_RES";
            assert studentGotSubmitAck : "Student should receive SUBMIT_EXAM_RES";

            // Host receives final result
            boolean hostGotResult = false;
            ExamResultDTO hostResult = null;
            while ((p = PacketCodec.readPacket(hostIn)) != null) {
                if (p.getOpCode() == OpCodes.EXAM_RESULT_RES) {
                    hostGotResult = true;
                    hostResult = p.getPayloadAs(ExamResultDTO.class);
                    break;
                }
            }
            assert hostGotResult && hostResult != null;
            assert hostResult.getTotalQuestions() == 3;
            assert hostResult.getRank() >= 1;

            System.out.printf("PASSED (Host Rank: #%d, Score: %.1f)\n", hostResult.getRank(), hostResult.getScore());
            testsPassed++;

        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        } finally {
            try {
                if (hostSocket != null) hostSocket.close();
                if (studentSocket != null) studentSocket.close();
            } catch (Exception ignored) {}
            server.stop();
            try {
                if (dbFile.exists()) dbFile.delete();
            } catch (Exception ignored) {}
        }

        // -------------------------------------------------------------
        // TEST SUMMARY
        // -------------------------------------------------------------
        System.out.println("\n===============================================================");
        System.out.printf("  MULTIPLAYER INTEGRATION RESULTS: %d/%d TESTS PASSED\n", testsPassed, totalTests);
        if (testsPassed == totalTests) {
            System.out.println("  >>> ALL MULTIPLAYER ROOM & EXAM ENGINE CHECKS SUCCEEDED! <<<");
        } else {
            System.err.println("  >>> SOME TESTS FAILED! CHECK STACK TRACE ABOVE! <<<");
        }
        System.out.println("===============================================================\n");
    }
}
