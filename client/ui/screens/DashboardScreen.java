package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.StatCard;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.UserProfileDTO;
import shared.dtos.UserStatsDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main dashboard screen presenting user profile summary, quick statistics,
 * and high-level navigation to Practice, Multiplayer Rooms, Analytics, and Question Bank.
 */
public class DashboardScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    // Header labels
    private JLabel lblUserGreeting;
    private JLabel lblUserBadge;

    // KPI Summary
    private StatCard cardTotalTests;
    private StatCard cardAvgScore;
    private StatCard cardHighScore;
    private StatCard cardAccuracy;

    public DashboardScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // 1. Top Bar: User profile info & Logout
        ModernCard topBar = new ModernCard(new BorderLayout(16, 0));
        topBar.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel pnlUser = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlUser.setOpaque(false);

        lblUserGreeting = new JLabel("Welcome back!");
        lblUserGreeting.setFont(Theme.FONT_TITLE);
        lblUserGreeting.setForeground(Theme.TEXT_PRIMARY);

        lblUserBadge = Theme.createBadge("STUDENT", Theme.PRIMARY, Color.WHITE);

        pnlUser.add(lblUserGreeting);
        pnlUser.add(lblUserBadge);
        topBar.add(pnlUser, BorderLayout.WEST);

        ModernButton btnLogout = new ModernButton("Log Out", ModernButton.Style.OUTLINE);
        btnLogout.setPreferredSize(new Dimension(110, 36));
        btnLogout.addActionListener(e -> {
            client.sendPacket(Packet.ok(OpCodes.LOGOUT_REQ));
            app.showScreen("LOGIN");
        });
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 2. Center Content: KPIs + 4 Main Feature Cards
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 16));
        pnlCenter.setOpaque(false);

        // KPI Summary Cards
        JPanel pnlKPIs = new JPanel(new GridLayout(1, 4, 16, 0));
        pnlKPIs.setOpaque(false);

        cardTotalTests = new StatCard("Total Tests Taken", "0", "Completed exams", Theme.PRIMARY);
        cardAvgScore   = new StatCard("Average Score", "0.0", "Out of 10.0 scale", Theme.SUCCESS);
        cardHighScore  = new StatCard("Highest Score", "0.0", "Personal record", Theme.WARNING);
        cardAccuracy   = new StatCard("Accuracy Rate", "0%", "Correct answers ratio", Theme.SECONDARY);

        pnlKPIs.add(cardTotalTests);
        pnlKPIs.add(cardAvgScore);
        pnlKPIs.add(cardHighScore);
        pnlKPIs.add(cardAccuracy);

        pnlCenter.add(pnlKPIs, BorderLayout.NORTH);

        // Feature Action Cards (Grid 2x2 or 2x3)
        JPanel pnlActionsGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        pnlActionsGrid.setOpaque(false);

        // Card 1: Practice Mode
        pnlActionsGrid.add(createFeatureCard(
                "PRACTICE MODE (LUYỆN TẬP)",
                "Customize question topics (Java Core, Networking, DB, OS), difficulty levels, count, and duration for solo study.",
                "Start Solo Practice",
                Theme.PRIMARY,
                () -> app.showScreen("PRACTICE_SETUP")
        ));

        // Card 2: Test Rooms Lobby
        pnlActionsGrid.add(createFeatureCard(
                "TEST ROOMS LOBBY (PHÒNG THI)",
                "Create or join live multiplayer exam rooms. Host decides when to start or end the test, with live timer & scoring.",
                "Enter Rooms Lobby",
                Theme.SECONDARY,
                () -> app.enterRoomLobby()
        ));

        // Card 3: Visual Analytics & History
        pnlActionsGrid.add(createFeatureCard(
                "ANALYTICS & TEST HISTORY",
                "View visual score progression line charts, topic mastery bar charts, and historical exam records.",
                "View Graphical Stats",
                Theme.SUCCESS,
                () -> app.enterHistoryStats()
        ));

        // Card 4: Leaderboard & Management
        pnlActionsGrid.add(createFeatureCard(
                "LEADERBOARD & QUESTION BANK",
                "Review the global student hall of fame, or manage question bank bank topics and inspect audit activity logs.",
                "Hall of Fame & Tools",
                Theme.WARNING,
                () -> app.enterLeaderboard()
        ));

        pnlCenter.add(pnlActionsGrid, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        // Bottom extra bar
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlBottom.setOpaque(false);

        ModernButton btnAdmin = new ModernButton("Question Bank & Audit Logs", ModernButton.Style.OUTLINE);
        btnAdmin.setPreferredSize(new Dimension(240, 36));
        btnAdmin.addActionListener(e -> app.enterAdminQuestions());
        pnlBottom.add(btnAdmin);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    private ModernCard createFeatureCard(String title, String description, String buttonText, Color accentColor, Runnable action) {
        ModernCard card = new ModernCard(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 8));
        pnlText.setOpaque(false);

        JLabel lblT = new JLabel(title);
        lblT.setFont(Theme.FONT_SUBTITLE);
        lblT.setForeground(accentColor);

        JLabel lblD = new JLabel("<html><body style='width: 280px; color: #94A3B8;'>" + description + "</body></html>");
        lblD.setFont(Theme.FONT_BODY);

        pnlText.add(lblT);
        pnlText.add(lblD);
        card.add(pnlText, BorderLayout.CENTER);

        ModernButton btn = new ModernButton(buttonText, ModernButton.Style.PRIMARY);
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
            lblUserGreeting.setText(String.format("Hello, %s!", user.getDisplayName()));
            lblUserBadge.setText(user.getRole().toUpperCase());
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
