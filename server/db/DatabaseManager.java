package server.db;

import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.UserRole;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages SQLite database connection, table migrations, and sample data seeding.
 */
public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    public static final String DEFAULT_DB_URL = "jdbc:sqlite:exam_system.db";

    private static String currentDbUrl = DEFAULT_DB_URL;
    private static boolean initialized = false;

    private DatabaseManager() {}

    /**
     * Initializes the database with the default file path.
     */
    public static synchronized void initialize() {
        initialize(DEFAULT_DB_URL);
    }

    /**
     * Initializes the database with a custom JDBC URL (e.g. for testing in-memory or custom path).
     */
    public static synchronized void initialize(String dbUrl) {
        currentDbUrl = dbUrl;
        try {
            // Explicitly load SQLite driver class
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "SQLite JDBC driver not found on classpath!", e);
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        try (Connection conn = getConnection()) {
            // SQLite optimizations for multi-threaded socket servers
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
                // Avoid WAL if running on memory database
                if (!dbUrl.contains(":memory:")) {
                    st.execute("PRAGMA journal_mode = WAL;");
                }
                st.execute("PRAGMA synchronous = NORMAL;");
            }

            createTables(conn);
            seedInitialData(conn);
            initialized = true;
            LOGGER.info("Database initialized successfully at: " + currentDbUrl);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database initialization failed", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Obtains a new connection to the configured database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(currentDbUrl);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getCurrentDbUrl() {
        return currentDbUrl;
    }

    // ==========================================
    // SCHEMA MIGRATION / TABLE CREATION
    // ==========================================

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // 1. Users Table
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL COLLATE NOCASE,
                    password_hash TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    email TEXT,
                    role TEXT NOT NULL DEFAULT 'STUDENT',
                    created_at INTEGER NOT NULL
                );
            """);

            // 2. Questions Table
            st.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT NOT NULL,
                    option_a TEXT NOT NULL,
                    option_b TEXT NOT NULL,
                    option_c TEXT NOT NULL,
                    option_d TEXT NOT NULL,
                    correct_option TEXT NOT NULL,
                    explanation TEXT,
                    category TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
            """);

            // Indices for high-performance question sampling
            st.execute("CREATE INDEX IF NOT EXISTS idx_questions_cat_diff ON questions(category, difficulty);");

            // 3. Exam Records Table (Multiplayer Room & Solo Practice history)
            st.execute("""
                CREATE TABLE IF NOT EXISTS exam_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL COLLATE NOCASE,
                    test_type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    room_code TEXT,
                    topic TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    score REAL NOT NULL,
                    correct_count INTEGER NOT NULL,
                    total_questions INTEGER NOT NULL,
                    time_spent_seconds INTEGER NOT NULL,
                    taken_at INTEGER NOT NULL,
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_exam_records_user ON exam_records(username);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_exam_records_room ON exam_records(room_code);");

            // 4. Activity Logs Table
            st.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    username TEXT COLLATE NOCASE,
                    action TEXT NOT NULL,
                    details TEXT,
                    ip_address TEXT
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_activity_logs_time ON activity_logs(timestamp DESC);");
        }
    }

    // ==========================================
    // INITIAL SEED DATA
    // ==========================================

    private static void seedInitialData(Connection conn) throws SQLException {
        seedUsers(conn);
        seedQuestions(conn);
    }

    private static void seedUsers(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM users;";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        String insertSql = """
            INSERT INTO users (username, password_hash, display_name, email, role, created_at)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        long now = System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            // Admin
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hashPassword("admin123"));
            ps.setString(3, "System Administrator");
            ps.setString(4, "admin@onlineexam.com");
            ps.setString(5, UserRole.ADMIN);
            ps.setLong(6, now);
            ps.addBatch();

            // Teacher
            ps.setString(1, "teacher01");
            ps.setString(2, PasswordUtil.hashPassword("teacher123"));
            ps.setString(3, "Dr. Alice Smith");
            ps.setString(4, "alice@exam.edu.vn");
            ps.setString(5, UserRole.TEACHER);
            ps.setLong(6, now);
            ps.addBatch();

            // Student 1
            ps.setString(1, "student01");
            ps.setString(2, PasswordUtil.hashPassword("student123"));
            ps.setString(3, "Nguyen Van A");
            ps.setString(4, "nva@sis.hust.edu.vn");
            ps.setString(5, UserRole.STUDENT);
            ps.setLong(6, now);
            ps.addBatch();

            // Student 2
            ps.setString(1, "student02");
            ps.setString(2, PasswordUtil.hashPassword("student123"));
            ps.setString(3, "Tran Thi B");
            ps.setString(4, "ttb@sis.hust.edu.vn");
            ps.setString(5, UserRole.STUDENT);
            ps.setLong(6, now);
            ps.addBatch();

            ps.executeBatch();
            LOGGER.info("Seeded default users (admin, teacher01, student01, student02).");
        }
    }

    private static void seedQuestions(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM questions;";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        String insertSql = """
            INSERT INTO questions (content, option_a, option_b, option_c, option_d, correct_option, explanation, category, difficulty, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        long now = System.currentTimeMillis();

        Object[][] questionsData = {
            // === COMPUTER NETWORKING ===
            {
                "Giao thức nào sau đây hoạt động ở tầng Transport và cung cấp truyền dữ liệu tin cậy, có hướng kết nối?",
                "UDP", "TCP", "ICMP", "IP",
                "B",
                "TCP (Transmission Control Protocol) là giao thức hướng kết nối, đảm bảo phân phối tin cậy và có kiểm tra thứ tự gói tin.",
                QuestionCategory.NETWORKING, QuestionDifficulty.EASY
            },
            {
                "Kích thước phần Header tối thiểu của một gói tin TCP chuẩn là bao nhiêu byte?",
                "12 bytes", "16 bytes", "20 bytes", "32 bytes",
                "C",
                "Header chuẩn của TCP không có Options có kích thước 20 bytes (160 bits).",
                QuestionCategory.NETWORKING, QuestionDifficulty.MEDIUM
            },
            {
                "Cổng (port) mặc định của giao thức HTTPS là gì?",
                "80", "8080", "22", "443",
                "D",
                "HTTPS sử dụng cổng 443 làm cổng mặc định (HTTP sử dụng cổng 80).",
                QuestionCategory.NETWORKING, QuestionDifficulty.EASY
            },
            {
                "Trong mô hình TCP/IP, thuật toán nào thường được dùng để tránh nghẽn mạch (Congestion Avoidance)?",
                "Bellman-Ford", "Slow Start & Congestion Avoidance (AIMD)", "Dijkstra", "Round Robin",
                "B",
                "TCP sử dụng Slow Start kết hợp Congestion Avoidance dựa trên cơ chế AIMD (Additive Increase Multiplicative Decrease).",
                QuestionCategory.NETWORKING, QuestionDifficulty.HARD
            },
            {
                "Giao thức nào chịu trách nhiệm phân giải địa chỉ IP thành địa chỉ MAC tương ứng trong mạng LAN?",
                "DNS", "DHCP", "ARP", "NAT",
                "C",
                "ARP (Address Resolution Protocol) dùng để ánh xạ địa chỉ logic IP sang địa chỉ vật lý MAC.",
                QuestionCategory.NETWORKING, QuestionDifficulty.EASY
            },

            // === JAVA CORE ===
            {
                "Trong Java, từ khóa nào được sử dụng để ngăn chặn một lớp không thể bị kế thừa?",
                "static", "final", "abstract", "const",
                "B",
                "Một lớp được khai báo với từ khóa 'final' sẽ không thể bị kế thừa bởi bất kỳ lớp con nào.",
                QuestionCategory.JAVA_CORE, QuestionDifficulty.EASY
            },
            {
                "Lớp nào sau đây trong Java là Thread-Safe khi làm việc trong môi trường đa luồng?",
                "ArrayList", "HashMap", "ConcurrentHashMap", "StringBuilder",
                "C",
                "ConcurrentHashMap hỗ trợ truy cập đa luồng an toàn (Thread-safe) với hiệu năng cao bằng cơ chế phân đoạn khóa.",
                QuestionCategory.JAVA_CORE, QuestionDifficulty.MEDIUM
            },
            {
                "Khối lệnh nào luôn được thực thi trong cấu trúc try-catch-finally, bất kể có ngoại lệ hay không?",
                "try", "catch", "finally", "throw",
                "C",
                "Khối 'finally' luôn luôn được chạy ngay cả khi có ngoại lệ hay có lệnh return trong khối try/catch.",
                QuestionCategory.JAVA_CORE, QuestionDifficulty.EASY
            },
            {
                "Bộ nhớ Heap trong JVM chủ yếu được dùng để lưu trữ đối tượng nào?",
                "Các biến cục bộ nguyên thủy", "Tất cả các đối tượng (objects) được tạo bởi từ khóa new", "Bytecode của các phương thức", "Con trỏ ngăn xếp luồng",
                "B",
                "Heap là vùng nhớ dùng chung chứa toàn bộ các instances của đối tượng và mảng được cấp phát động bằng toán tử 'new'.",
                QuestionCategory.JAVA_CORE, QuestionDifficulty.MEDIUM
            },
            {
                "Cơ chế nào trong Java cho phép kiểm tra và can thiệp cấu trúc lớp, phương thức, trường tại Runtime?",
                "Serialization", "Reflection", "Polymorphism", "Encapsulation",
                "B",
                "Java Reflection API cho phép inspect và invoke các class, method, field tại thời điểm thực thi (runtime).",
                QuestionCategory.JAVA_CORE, QuestionDifficulty.HARD
            },

            // === DATABASE SYSTEMS ===
            {
                "Mệnh đề nào trong SQL được dùng để lọc dữ liệu sau khi đã thực hiện nhóm bằng GROUP BY?",
                "WHERE", "HAVING", "ORDER BY", "DISTINCT",
                "B",
                "HAVING được áp dụng trên các nhóm dữ liệu đã qua GROUP BY, còn WHERE chỉ lọc từng bản ghi trước khi nhóm.",
                QuestionCategory.DATABASE, QuestionDifficulty.EASY
            },
            {
                "Thuộc tính ACID trong hệ quản trị cơ sở dữ liệu bao gồm 4 yếu tố nào?",
                "Accuracy, Consistency, Integrity, Durability", "Atomicity, Consistency, Isolation, Durability", "Availability, Consistency, Isolation, Distributed", "Atomicity, Concurrency, Indexing, Durability",
                "B",
                "ACID là viết tắt của: Atomicity (Tính nguyên tử), Consistency (Tính nhất quán), Isolation (Tính cô lập), Durability (Tính bền vững).",
                QuestionCategory.DATABASE, QuestionDifficulty.EASY
            },
            {
                "Loại chỉ mục (Index) nào sắp xếp trực tiếp thứ tự vật lý của các dòng dữ liệu trên đĩa cứng?",
                "Non-Clustered Index", "Clustered Index", "Bitmap Index", "Full-text Index",
                "B",
                "Clustered Index quyết định thứ tự lưu trữ vật lý của các dòng dữ liệu trong bảng. Một bảng chỉ có tối đa 1 Clustered Index.",
                QuestionCategory.DATABASE, QuestionDifficulty.MEDIUM
            },
            {
                "Hiện tượng 'Phantom Read' xảy ra ở cấp độ cô lập (Isolation Level) nào khi có transaction thêm dòng mới?",
                "Serializable", "Read Uncommitted, Read Committed, Repeatable Read", "Chỉ xảy ra ở Serializable", "Không bao giờ xảy ra trong RDBMS",
                "B",
                "Phantom Read có thể xảy ra ở các mức cô lập thấp hơn Serializable, khi một giao dịch đọc lại cùng điều kiện thì thấy thêm các dòng mới được commit.",
                QuestionCategory.DATABASE, QuestionDifficulty.HARD
            },

            // === OPERATING SYSTEMS ===
            {
                "Điều kiện nào sau đây KHÔNG phải là một trong 4 điều kiện Coffman gây ra Deadlock?",
                "Mutual Exclusion", "Hold and Wait", "Preemption", "Circular Wait",
                "C",
                "Điều kiện thứ 3 là 'No Preemption' (Không thể cưỡng đoạt tài nguyên). Nếu có 'Preemption' thì hệ thống sẽ giải phóng được deadlock.",
                QuestionCategory.OPERATING_SYSTEMS, QuestionDifficulty.MEDIUM
            },
            {
                "Thuật toán lập lịch CPU nào có thể gây ra hiện tượng đói tài nguyên (Starvation) đối với các tiến trình có thời gian thực thi dài?",
                "Round Robin (RR)", "Shortest Job First (SJF)", "First-Come First-Served (FCFS)", "Multilevel Feedback Queue",
                "B",
                "SJF (Shortest Job First) luôn ưu tiên tiến trình ngắn, do đó các tiến trình dài có thể bị bỏ đói nếu liên tục có tiến trình ngắn xuất hiện.",
                QuestionCategory.OPERATING_SYSTEMS, QuestionDifficulty.MEDIUM
            },
            {
                "Bộ nhớ ảo (Virtual Memory) thường được hiện thực thông qua kỹ thuật nào phổ biến nhất?",
                "Paging kết hợp Swapping", "Static Partitioning", "Contiguous Allocation", "Overlays",
                "A",
                "Kỹ thuật phân trang (Paging) theo yêu cầu (Demand Paging) kết hợp hoán đổi (Swapping) là nền tảng của bộ nhớ ảo hiện đại.",
                QuestionCategory.OPERATING_SYSTEMS, QuestionDifficulty.EASY
            },

            // === DATA STRUCTURES & ALGORITHMS ===
            {
                "Độ phức tạp thời gian trung bình (Average Time Complexity) của thuật toán QuickSort là gì?",
                "O(N)", "O(N log N)", "O(N^2)", "O(log N)",
                "B",
                "QuickSort có độ phức tạp trung bình là O(N log N), trường hợp xấu nhất là O(N^2) khi chọn pivot không tối ưu.",
                QuestionCategory.DATA_STRUCTURES, QuestionDifficulty.EASY
            },
            {
                "Cấu trúc dữ liệu nào hoạt động theo nguyên lý LIFO (Last In, First Out)?",
                "Queue", "Stack", "Binary Search Tree", "Linked List",
                "B",
                "Stack (Ngăn xếp) hoạt động theo nguyên tắc LIFO: phần tử đẩy vào sau cùng sẽ được lấy ra đầu tiên.",
                QuestionCategory.DATA_STRUCTURES, QuestionDifficulty.EASY
            },
            {
                "Thuật toán Dijkstra được sử dụng để giải quyết bài toán nào sau đây trên đồ thị có trọng số không âm?",
                "Tìm cây khung nhỏ nhất (MST)", "Tìm đường đi ngắn nhất từ một nguồn", "Duyệt topo đồ thị", "Tìm luồng cực đại trong mạng",
                "B",
                "Thuật toán Dijkstra tìm đường đi ngắn nhất từ một đỉnh nguồn đến tất cả các đỉnh còn lại trên đồ thị có trọng số không âm.",
                QuestionCategory.DATA_STRUCTURES, QuestionDifficulty.MEDIUM
            }
        };

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Object[] row : questionsData) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.setString(5, (String) row[4]);
                ps.setString(6, (String) row[5]);
                ps.setString(7, (String) row[6]);
                ps.setString(8, (String) row[7]);
                ps.setString(9, (String) row[8]);
                ps.setLong(10, now);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Seeded " + questionsData.length + " initial exam questions across multiple topics.");
        }
    }
}
