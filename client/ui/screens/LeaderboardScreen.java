package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.LeaderboardEntryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screen displaying global student leaderboard rankings.
 */
public class LeaderboardScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    private JTable tblLeaderboard;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;
    private ModernButton btnRefresh;

    public LeaderboardScreen(ClientApp app) {
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
        JLabel lblHeading = new JLabel("GLOBAL HALL OF FAME (BẢNG XẾP HẠNG)");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Top scoring students ranked by performance and exam activity");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Back to Home", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Refresh", ModernButton.Style.PRIMARY);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadLeaderboard());

        pnlActions.add(btnBack);
        pnlActions.add(btnRefresh);

        pnlTop.add(pnlTitle, BorderLayout.WEST);
        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // Center Table Card
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Rank", "Student Username", "Exams Completed", "Average Score (/10.0)", "Highest Score", "Accuracy %"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblLeaderboard = new JTable(tableModel);
        Theme.styleTable(tblLeaderboard);

        // Medals renderer for rank
        tblLeaderboard.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                String val = value != null ? value.toString() : "";
                Color bg;
                if ("#1".equals(val)) {
                    bg = new Color(234, 179, 8); // Gold
                } else if ("#2".equals(val)) {
                    bg = new Color(148, 163, 184); // Silver
                } else if ("#3".equals(val)) {
                    bg = new Color(180, 83, 9); // Bronze
                } else {
                    bg = Theme.BORDER;
                }
                return Theme.createBadge(val, bg, Color.WHITE);
            }
        });

        JScrollPane scroll = new JScrollPane(tblLeaderboard);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        lblStatus = new JLabel("Click Refresh to load top students");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);
        add(lblStatus, BorderLayout.SOUTH);
    }

    public void loadLeaderboard() {
        lblStatus.setText("Loading leaderboard data from server...");
        btnRefresh.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<LeaderboardEntryDTO> entries;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.LEADERBOARD_REQ);
                    Packet res = client.sendRequest(p, OpCodes.LEADERBOARD_RES).get();
                    if (res != null && res.isSuccess()) {
                        entries = res.getPayloadAsList(LeaderboardEntryDTO.class);
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

                tableModel.setRowCount(0);
                if (entries != null) {
                    int rank = 1;
                    for (LeaderboardEntryDTO e : entries) {
                        tableModel.addRow(new Object[]{
                                "#" + (rank++),
                                e.getUsername(),
                                e.getTotalExams(),
                                String.format("%.1f", e.getAverageScore()),
                                String.format("%.1f", e.getHighestScore()),
                                String.format("%.1f%%", e.getAccuracyPercentage())
                        });
                    }
                    lblStatus.setForeground(Theme.TEXT_SECONDARY);
                    lblStatus.setText(String.format("Loaded top %d students.", entries.size()));
                }
            }
        };
        worker.execute();
    }
}
