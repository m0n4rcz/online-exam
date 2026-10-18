package shared.dtos;

import java.io.Serializable;

/**
 * Server's response to an authentication or registration attempt.
 */
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String token;
    private String username;
    private String displayName;
    private String role; // "STUDENT" or "ADMIN"

    public LoginResponse() {}

    public LoginResponse(boolean success, String message, String token, String username, String displayName, String role) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public static LoginResponse ok(String token, String username, String displayName, String role) {
        return new LoginResponse(true, "Authentication successful", token, username, displayName, role);
    }

    public static LoginResponse fail(String message) {
        return new LoginResponse(false, message, null, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserProfileDTO toUserProfileDTO() {
        return new UserProfileDTO(username, displayName != null ? displayName : username, "", role != null ? role : "STUDENT", System.currentTimeMillis());
    }
}
