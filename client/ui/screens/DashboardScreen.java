package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.StatCard;
import shared.OpCodes;
import shared.Packet;
import shared.UserRole;
import shared.dtos.UserProfileDTO;
import shared.dtos.UserStatsDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main dashboard screen presenting student profile summary, academic KPIs,
 * and clear navigation to Practice, Multiplayer Rooms, Visual Analytics, and Leaderboards.
 * Designed with HUST Red & Academic Gold visual identity.
 * Role-gated: Admin tools are strictly restricted to Teachers and Admins.
 */
public class DashboardScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    // Header labels
    private JLabel lblUserGreeting;
    private JLabel lblUserSub;
    private JLabel lblUserBadge;
    private JPanel pnlAvatarContainer;

    // Academic KPI Summary
    private StatCard cardTotalTests;
    private StatCard cardAvgScore;
    private StatCard cardHighScore;
    private StatCard cardAccuracy;

    // Admin Tools Button (role gated)
    private ModernButton btnAdmin;

    public DashboardScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // 1. Top Bar: Student profile banner & Logout
        ModernCard topBar = new ModernCard(new BorderLayout(16, 0));
        topBar.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel pnlUser = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        pnlUser.setOpaque(false);

        pnlAvatarContainer = new JPanel(new BorderLayout());
        pnlAvatarContainer.setOpaque(false);
        JLabel defaultAvatar = Theme.createAvatar("H", 42, Theme.HUST_RED);
        pnlAvatarContainer.add(defaultAvatar, BorderLayout.CENTER);

        JPanel pnlNames = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlNames.setOpaque(false);

        JPanel pnlTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitleRow.setOpaque(false);

        lblUserGreeting = new JLabel("Xin chào, Sinh viên Bách Khoa!");
        lblUserGreeting.setFont(Theme.FONT_TITLE);
        lblUserGreeting.setForeground(Theme.TEXT_PRIMARY);

        lblUserBadge = Theme.createBadge("SINH VIÊN HUST", Theme.HUST_RED, Color.WHITE);
        pnlTitleRow.add(lblUserGreeting);
        pnlTitleRow.add(lblUserBadge);

        lblUserSub = new JLabel("Cổng Khảo Thí & Đánh Giá Năng Lực Trực Tuyến - Đại học Bách Khoa Hà Nội");
        lblUserSub.setFont(Theme.FONT_SMALL);
        lblUserSub.setForeground(Theme.TEXT_SECONDARY);

        pnlNames.add(pnlTitleRow);
        pnlNames.add(lblUserSub);

        pnlUser.add(pnlAvatarContainer);
        pnlUser.add(pnlNames);
        topBar.add(pnlUser, BorderLayout.WEST);

        ModernButton btnLogout = new ModernButton("Đăng xuất", ModernButton.Style.OUTLINE);
        btnLogout.setPreferredSize(new Dimension(120, 38));
        btnLogout.addActionListener(e -> {
            client.sendPacket(Packet.ok(OpCodes.LOGOUT_REQ));
            app.showScreen("LOGIN");
        });
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 2. Center Content: KPIs + 4 Main Feature Cards
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 16));
        pnlCenter.setOpaque(false);

        // Academic KPI Summary Cards
        JPanel pnlKPIs = new JPanel(new GridLayout(1, 4, 16, 0));
        pnlKPIs.setOpaque(false);

        cardTotalTests = new StatCard("Bài thi đã làm", "0", "Tổng số lần thi", Theme.HUST_RED);
        cardAvgScore   = new StatCard("Điểm trung bình", "0.0", "Thang điểm 10.0", Theme.HUST_GOLD);
        cardHighScore  = new StatCard("Kỷ lục điểm cao", "0.0", "Thành tích cao nhất", Theme.HUST_GOLD_DARK);
        cardAccuracy   = new StatCard("Độ chính xác", "0%", "Tỷ lệ câu đúng", Theme.SUCCESS);

        pnlKPIs.add(cardTotalTests);
        pnlKPIs.add(cardAvgScore);
        pnlKPIs.add(cardHighScore);
        pnlKPIs.add(cardAccuracy);

        pnlCenter.add(pnlKPIs, BorderLayout.NORTH);

        // Feature Action Cards (2x2 Grid)
        JPanel pnlActionsGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        pnlActionsGrid.setOpaque(false);

        // Card 1: Practice Mode
        pnlActionsGrid.add(createFeatureCard(
                "LUYỆN TẬP CÁ NHÂN (PRACTICE)",
                "Tự do tùy chọn chủ đề chuyên sâu (Mạng máy tính, Java Core, CSDL, HĐH), mức độ khó và thời gian để tự rèn luyện kỹ năng.",
                "Bắt đầu Ôn tập Ngay",
                Theme.HUST_GOLD,
                ModernButton.Style.HUST,
                () -> app.showScreen("PRACTICE_SETUP")
        ));

        // Card 2: Test Rooms Lobby
        pnlActionsGrid.add(createFeatureCard(
                "ĐẤU TRƯỜNG PHÒNG THI (ROOMS)",
                "Tham gia phòng thi chung với các bạn cùng khóa hoặc tự tạo phòng thi đấu. Đồng bộ thời gian thực, bảng điểm tức thì.",
                "Vào Sảnh Phòng Thi",
                Theme.HUST_RED,
                ModernButton.Style.HUST_GOLD,
                () -> app.enterRoomLobby()
        ));

        // Card 3: Visual Analytics & History
        pnlActionsGrid.add(createFeatureCard(
                "ĐỒ THỊ NĂNG LỰC & LỊCH SỬ",
                "Theo dõi tiến độ học tập qua biểu đồ xu hướng điểm số, phân tích mức độ thành thạo các chủ đề kiến thức và nhật ký làm bài.",
                "Xem Biểu đồ Năng lực",
                Theme.HUST_GOLD,
                ModernButton.Style.SECONDARY,
                () -> app.enterHistoryStats()
        ));

        // Card 4: Leaderboard
        pnlActionsGrid.add(createFeatureCard(
                "BẢNG VÀNG THÀNH TÍCH (TOP HUST)",
                "Vinh danh các sinh viên xuất sắc nhất toàn trường về điểm số và độ chuyên cần trên đấu trường trực tuyến HUST.",
                "Xem Bảng Vinh Danh",
                Theme.HUST_GOLD,
                ModernButton.Style.HUST,
                () -> app.enterLeaderboard()
        ));

        pnlCenter.add(pnlActionsGrid, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        // Bottom Bar: Teacher / Admin Management Tool (Hidden for students!)
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlBottom.setOpaque(false);

        btnAdmin = new ModernButton("Quản lý Ngân hàng Đề & Nhật ký Giám sát", ModernButton.Style.OUTLINE);
        btnAdmin.setPreferredSize(new Dimension(320, 38));
        btnAdmin.setVisible(false); // Default hidden until role is verified
        btnAdmin.addActionListener(e -> app.enterAdminQuestions());
        pnlBottom.add(btnAdmin);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    private ModernCard createFeatureCard(String title, String description, String buttonText, Color accentColor, ModernButton.Style btnStyle, Runnable action) {
        ModernCard card = new ModernCard(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 8));
        pnlText.setOpaque(false);

        JLabel lblT = new JLabel(title);
        lblT.setFont(Theme.FONT_SUBTITLE);
        lblT.setForeground(accentColor);

        JLabel lblD = new JLabel("<html><body style='width: 320px; color: #D1D5DB; font-family: Segoe UI; font-size: 10pt; line-height: 1.45;'>"
                + description + "</body></html>");

        pnlText.add(lblT);
        pnlText.add(lblD);
        card.add(pnlText, BorderLayout.CENTER);

        ModernButton btn = new ModernButton(buttonText, btnStyle);
        btn.setPreferredSize(new Dimension(200, 40));
        btn.addActionListener(e -> action.run());

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlBtn.setOpaque(false);
        pnlBtn.add(btn);
        card.add(pnlBtn, BorderLayout.SOUTH);

        return card;
    }

    public void updateUserData(UserProfileDTO user) {
        if (user != null) {
            lblUserGreeting.setText(String.format("Xin chào, %s!", user.getDisplayName()));

            pnlAvatarContainer.removeAll();
            pnlAvatarContainer.add(Theme.createAvatar(user.getDisplayName(), 42, Theme.HUST_RED), BorderLayout.CENTER);
            pnlAvatarContainer.revalidate();
            pnlAvatarContainer.repaint();

            String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";
            if (UserRole.isAdmin(role)) {
                lblUserBadge.setText("QUẢN TRỊ VIÊN HUST");
                lblUserBadge.setForeground(Color.WHITE);
                btnAdmin.setVisible(true);
            } else if (UserRole.isTeacher(role)) {
                lblUserBadge.setText("GIẢNG VIÊN HUST");
                lblUserBadge.setForeground(Color.WHITE);
                btnAdmin.setVisible(true);
            } else {
                lblUserBadge.setText("SINH VIÊN HUST");
                lblUserBadge.setForeground(Color.WHITE);
                btnAdmin.setVisible(false); // Hide admin button for students
            }
        }

        // Fetch user stats to update KPI cards
        String username = user != null ? user.getUsername() : "student01";
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private UserStatsDTO stats;
            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.USER_STATS_REQ, username);
                    Packet r = client.sendRequest(p, OpCodes.USER_STATS_RES).get();
                    if (r != null && r.isSuccess()) {
                        stats = r.getPayloadAs(UserStatsDTO.class);
                    }
                } catch (Exception ignored) {}
                return null;
            }
            @Override
            protected void done() {
                if (stats != null) {
                    cardTotalTests.setValue(String.valueOf(stats.getTotalTests()));
                    cardAvgScore.setValue(String.format("%.1f", stats.getAverageScore()));
                    cardHighScore.setValue(String.format("%.1f", stats.getHighestScore()));
                    cardAccuracy.setValue(String.format("%.1f%%", stats.getOverallAccuracyPercentage()));
                }
            }
        };
        worker.execute();
    }
}
