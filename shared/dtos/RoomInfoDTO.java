package shared.dtos;

import shared.RoomStatus;
import java.io.Serializable;

/**
 * Summary of a test room, presented in the room lobby list.
 * Rubric requirement: "View a list of test rooms and the status of each room (not started, ongoing, finished)"
 */
public class RoomInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomCode;            // e.g. "ROOM42" or PIN
    private String roomName;            // e.g. "Java Final Exam 2026"
    private String hostUsername;
    private String hostDisplayName;
    private String status;              // NOT_STARTED, ONGOING, FINISHED (from RoomStatus)
    private int currentParticipants;
    private int maxParticipants;
    private int questionCount;
    private int durationMinutes;
    private String topic;               // e.g. "Java Core"
    private String difficulty;          // e.g. "MEDIUM", "ALL"
    private boolean hasPassword;
    private long createdAt;

    public RoomInfoDTO() {}

    public RoomInfoDTO(String roomCode, String roomName, String hostUsername, String hostDisplayName,
                       String status, int currentParticipants, int maxParticipants,
                       int questionCount, int durationMinutes, String topic, String difficulty,
                       boolean hasPassword, long createdAt) {
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.hostUsername = hostUsername;
        this.hostDisplayName = hostDisplayName;
        this.status = status != null ? status : RoomStatus.NOT_STARTED;
        this.currentParticipants = currentParticipants;
        this.maxParticipants = maxParticipants;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.topic = topic;
        this.difficulty = difficulty;
        this.hasPassword = hasPassword;
        this.createdAt = createdAt;
    }

    public boolean isJoinable() {
        return RoomStatus.isJoinable(status) && (maxParticipants <= 0 || currentParticipants < maxParticipants);
    }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getRoomName() { return roomName; }
    public String getTitle() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getHostUsername() { return hostUsername; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }

    public String getHostDisplayName() { return hostDisplayName; }
    public void setHostDisplayName(String hostDisplayName) { this.hostDisplayName = hostDisplayName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%s] %s (Host: %s, Status: %s, Users: %d/%d, Questions: %d, Time: %dm)",
                roomCode, roomName, hostDisplayName != null ? hostDisplayName : hostUsername,
                status, currentParticipants, maxParticipants, questionCount, durationMinutes);
    }
}
