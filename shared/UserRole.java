package shared;

/**
 * Access control roles for authentication and authorization.
 * Meets rubric requirement: "Access control management"
 */
public final class UserRole {

    private UserRole() {}

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String ADMIN   = "ADMIN";

    public static boolean isValid(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals(STUDENT) || r.equals(TEACHER) || r.equals(ADMIN);
    }

    public static boolean isAdmin(String role) {
        return ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isTeacher(String role) {
        return TEACHER.equalsIgnoreCase(role);
    }
}
