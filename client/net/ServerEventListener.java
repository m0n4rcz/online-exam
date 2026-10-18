package client.net;

import shared.dtos.*;

/**
 * Event listener interface for asynchronous notifications pushed from the server.
 */
public interface ServerEventListener {

    /** Fired when participant joins, leaves, or updates ready state in room lobby */
    default void onRoomUpdate(RoomUpdateEventDTO event) {}

    /** Fired when the host starts the exam countdown */
    default void onRoomStart(int countdownSeconds) {}

    /** Fired when server sends exam questions to participants */
    default void onExamInit(ExamInitDataDTO examData) {}

    /** Fired on periodic time sync ticks during live exam */
    default void onExamTick(int secondsRemaining) {}

    /** Fired when another participant submits their exam early */
    default void onParticipantSubmitted(String username, int submittedCount, int totalCount) {}

    /** Fired when test time ends or host terminates exam */
    default void onExamEnded(String reason) {}

    /** Fired when exam grading is finalized and results are sent to participant */
    default void onExamResult(ExamResultDTO result) {}

    /** Fired when a chat message is broadcast in the room lobby */
    default void onRoomChat(ChatMessageDTO chat) {}

    /** Fired when network connection drops or error occurs */
    default void onConnectionLost(String reason) {}
}
