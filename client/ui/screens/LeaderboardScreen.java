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
 * Styled for HUST academic examination environment.
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

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTitle.setOpaque(false);
        JLabel lblHeading = new JLabel("BẢNG VÀNG THÀNH TÍCH HỌC TẬP - HUST");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Vinh danh những sinh viên có kết quả học tập và rèn luyện xuất sắc nhất");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Về Trang Chủ", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.HUST);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadLeaderboard());

        pnlActions.add(btnBack);
        pnlActions.add(btnRefresh);

        pnlTop.add(pnlTitle, BorderLayout.WEST);
        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // Center Table Card
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        String[] cols = {"Thứ Hạng", "Tài Khoản Sinh Viên", "Số Bài Thi Đã Làm", "Điểm Trung Bình (/10.0)", "Điểm Cao Nhất", "Tỉ Lệ Chính Xác"};
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
                String text = val;
                if ("#1".equals(val) || val.contains("1")) {
                    bg = new Color(217, 119, 6); // Gold Amber
                    text = "🥇 Hạng 1";
                } else if ("#2".equals(val) || val.contains("2")) {
                    bg = new Color(100, 116, 139); // Silver
                    text = "🥈 Hạng 2";
                } else if ("#3".equals(val) || val.contains("3")) {
                    bg = new Color(180, 83, 9); // Bronze
                    text = "🥉 Hạng 3";
                } else {
                    bg = Theme.BORDER;
                }
                return Theme.createBadge(text, bg, Color.WHITE);
            }
        });

        JScrollPane scroll = new JScrollPane(tblLeaderboard);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        Theme.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        lblStatus = new JLabel("Bấm 'Làm Mới' để tải bảng xếp hạng sinh viên");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);
        add(lblStatus, BorderLayout.SOUTH);
    }

    public void loadLeaderboard() {
        lblStatus.setText("Đang tải dữ liệu xếp hạng sinh viên từ máy chủ...");
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
                    lblStatus.setText("Lỗi tải bảng xếp hạng: " + error.getMessage());
                    return;
                }

                tableModel.setRowCount(0);
                if (entries != null) {
                    int rank = 1;
                    for (LeaderboardEntryDTO e : entries) {
                        tableModel.addRow(new Object[]{
                                "#" + (rank++),
                                e.getUsername(),
                                e.getTotalExams() + " bài",
                                String.format("%.1f", e.getAverageScore()),
                                String.format("%.1f", e.getHighestScore()),
                                String.format("%.1f%%", e.getAccuracyPercentage())
                        });
                    }
                    lblStatus.setForeground(Theme.TEXT_SECONDARY);
                    lblStatus.setText(String.format("Đã tải thông tin top %d sinh viên xuất sắc.", entries.size()));
                }
            }
        };
        worker.execute();
    }
}
