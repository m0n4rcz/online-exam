package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ChartPanel;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.StatCard;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.UserExamRecordDTO;
import shared.dtos.UserStatsDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Screen displaying both persistent exam records and rich graphical visual analytics
 * (Score progression line charts, Topic accuracy bar charts, summary KPIs).
 */
public class HistoryStatsScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    // Stat Cards
    private StatCard cardTotalTests;
    private StatCard cardAvgScore;
    private StatCard cardHighScore;
    private StatCard cardAccuracy;

    // Charts
    private ChartPanel chartScoreTrend;
    private ChartPanel chartTopicAccuracy;

    // History Table
    private JTable tblHistory;
    private DefaultTableModel historyModel;
    private JLabel lblStatus;
    private ModernButton btnRefresh;

    public HistoryStatsScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // Top Header
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitle.setOpaque(false);
        JLabel lblHeading = new JLabel("TEST HISTORY & GRAPHICAL ANALYTICS");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Visual progress charts and persistent record history for your exams");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Back to Home", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Refresh Data", ModernButton.Style.PRIMARY);
        btnRefresh.setPreferredSize(new Dimension(130, 38));
        btnRefresh.addActionListener(e -> loadData());

        pnlActions.add(btnBack);
        pnlActions.add(btnRefresh);

        pnlTop.add(pnlTitle, BorderLayout.WEST);
        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // Center Tabs: Tab 1 = Visual Charts & KPIs, Tab 2 = Records Table
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_BODY_BOLD);
        tabbedPane.setBackground(Theme.BG_SURFACE);
        tabbedPane.setForeground(Theme.TEXT_PRIMARY);

        tabbedPane.addTab("Visual Graphical Statistics", createAnalyticsPanel());
        tabbedPane.addTab("Exam History Records", createHistoryTablePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        lblStatus = new JLabel("Ready");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel createAnalyticsPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 16));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(14, 4, 4, 4));

        // 1. KPI Cards Row
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 16, 0));
        pnlCards.setOpaque(false);

        cardTotalTests = new StatCard("Total Tests Taken", "0", "Completed exams", Theme.PRIMARY);
        cardAvgScore   = new StatCard("Average Score", "0.0", "Out of 10.0 scale", Theme.SUCCESS);
        cardHighScore  = new StatCard("Highest Score", "0.0", "Personal record", Theme.WARNING);
        cardAccuracy   = new StatCard("Overall Accuracy", "0%", "Correct answers ratio", Theme.SECONDARY);

        pnlCards.add(cardTotalTests);
        pnlCards.add(cardAvgScore);
        pnlCards.add(cardHighScore);
        pnlCards.add(cardAccuracy);

        pnl.add(pnlCards, BorderLayout.NORTH);

        // 2. Charts Row (Score Trend Line Chart + Topic Accuracy Bar Chart)
        JPanel pnlCharts = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlCharts.setOpaque(false);

        chartScoreTrend = new ChartPanel("Score Progression Trend (Line Chart)", ChartPanel.ChartType.LINE);
        chartTopicAccuracy = new ChartPanel("Topic Mastery & Accuracy (Bar Chart)", ChartPanel.ChartType.BAR);

        pnlCharts.add(chartScoreTrend);
        pnlCharts.add(chartTopicAccuracy);

        pnl.add(pnlCharts, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel createHistoryTablePanel() {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Date & Time", "Mode", "Title / Room", "Topic", "Difficulty", "Score (/10.0)", "Correct", "Questions"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblHistory = new JTable(historyModel);
        Theme.styleTable(tblHistory);

        JScrollPane scroll = new JScrollPane(tblHistory);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void loadData() {
        lblStatus.setText("Loading analytics and test history...");
        btnRefresh.setEnabled(false);

        String username = client.getCurrentUser() != null ? client.getCurrentUser().getUsername() : "student01";

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private UserStatsDTO stats;
            private List<UserExamRecordDTO> history;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    // 1. Fetch Stats
                    Packet p1 = Packet.ok(OpCodes.USER_STATS_REQ, username);
                    Packet r1 = client.sendRequest(p1, OpCodes.USER_STATS_RES).get();
                    if (r1 != null && r1.isSuccess()) {
                        stats = r1.getPayloadAs(UserStatsDTO.class);
                    }

                    // 2. Fetch History
                    Packet p2 = Packet.ok(OpCodes.USER_HISTORY_REQ, username);
                    Packet r2 = client.sendRequest(p2, OpCodes.USER_HISTORY_RES).get();
                    if (r2 != null && r2.isSuccess()) {
                        history = r2.getPayloadAsList(UserExamRecordDTO.class);
                    }
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                if (error != null) {
                    lblStatus.setForeground(Theme.DANGER);
                    lblStatus.setText("Failed to load: " + error.getMessage());
                    return;
                }

                // Update Stat Cards & Charts
                if (stats != null) {
                    cardTotalTests.setValue(String.valueOf(stats.getTotalTests()));
                    cardAvgScore.setValue(String.format("%.1f", stats.getAverageScore()));
                    cardHighScore.setValue(String.format("%.1f", stats.getHighestScore()));
                    cardAccuracy.setValue(String.format("%.1f%%", stats.getOverallAccuracyPercentage()));

                    chartScoreTrend.setLineData(stats.getScoreTrend());
                    chartTopicAccuracy.setBarData(stats.getTopicAccuracy());
                }

                // Update Table
                historyModel.setRowCount(0);
                if (history != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    for (UserExamRecordDTO rec : history) {
                        String dateStr = sdf.format(new Date(rec.getTakenAt()));
                        historyModel.addRow(new Object[]{
                                dateStr,
                                rec.getTestType(),
                                rec.getTitle(),
                                rec.getTopic(),
                                rec.getDifficulty(),
                                String.format("%.1f", rec.getScore()),
                                rec.getCorrectCount(),
                                rec.getTotalQuestions()
                        });
                    }
                }

                lblStatus.setForeground(Theme.TEXT_SECONDARY);
                lblStatus.setText("Updated analytics successfully.");
            }
        };
        worker.execute();
    }
}
