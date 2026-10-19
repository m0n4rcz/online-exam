package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.ModernTextField;
import shared.OpCodes;
import shared.Packet;
import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.RoomStatus;
import shared.dtos.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lobby screen displaying all active and completed exam rooms.
 * Allows creating a new room, joining joinable rooms, and inspecting completed room results.
 * Styled for HUST academic examination environment.
 */
public class RoomLobbyListScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    private JTable tblRooms;
    private DefaultTableModel tableModel;
    private List<RoomInfoDTO> currentRooms = new ArrayList<>();

    private JLabel lblStatus;
    private ModernButton btnRefresh;
    private ModernButton btnCreate;
    private ModernButton btnJoin;
    private ModernButton btnViewResults;

    public RoomLobbyListScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // Top Header
        JPanel pnlTop = new JPanel(new BorderLayout(16, 0));
        pnlTop.setOpaque(false);

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTitle.setOpaque(false);
        JLabel lblHeading = new JLabel("SẢNH PHÒNG THI ĐẤU TRỰC TUYẾN - HUST");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Tham gia phòng thi nhóm học phần hoặc tra cứu kết quả các đợt thi đã hoàn thành");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlTopActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlTopActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Về Trang Chủ", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadRooms());

        btnCreate = new ModernButton("+ Tạo Phòng Thi Mới", ModernButton.Style.HUST);
        btnCreate.setPreferredSize(new Dimension(175, 38));
        btnCreate.addActionListener(e -> showCreateRoomDialog());

        pnlTopActions.add(btnBack);
        pnlTopActions.add(btnRefresh);
        pnlTopActions.add(btnCreate);

        pnlTop.add(pnlTitle, BorderLayout.WEST);
        pnlTop.add(pnlTopActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // Center Table Card
        ModernCard tableCard = new ModernCard(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Mã Phòng", "Tên Phòng Thi", "Chủ Phòng", "Chủ Đề", "Độ Khó", "Số Câu", "Thời Gian", "Thí Sinh", "Trạng Thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblRooms = new JTable(tableModel);
        Theme.styleTable(tblRooms);

        // Custom status badge renderer
        tblRooms.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                String status = value != null ? value.toString() : "";
                Color bg;
                Color fg = Color.WHITE;
                String display;

                if (RoomStatus.NOT_STARTED.equalsIgnoreCase(status)) {
                    bg = Theme.HUST_GOLD;
                    fg = new Color(15, 23, 42);
                    display = "CHỜ BẮT ĐẦU";
                } else if (RoomStatus.ONGOING.equalsIgnoreCase(status)) {
                    bg = Theme.HUST_RED;
                    fg = Color.WHITE;
                    display = "ĐANG THI";
                } else {
                    bg = Theme.BORDER;
                    fg = Theme.TEXT_MUTED;
                    display = "ĐÃ KẾT THÚC";
                }

                return Theme.createBadge(display, bg, fg);
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblRooms);
        scrollPane.getViewport().setBackground(Theme.BG_SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        Theme.styleScrollPane(scrollPane);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);

        lblStatus = new JLabel("Bấm 'Làm Mới' để cập nhật danh sách phòng thi");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);

        JPanel pnlRoomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlRoomActions.setOpaque(false);

        btnViewResults = new ModernButton("Xem Bảng Điểm Phòng", ModernButton.Style.OUTLINE);
        btnViewResults.setPreferredSize(new Dimension(170, 38));
        btnViewResults.addActionListener(e -> viewSelectedRoomResults());

        btnJoin = new ModernButton("Vào Phòng Thi", ModernButton.Style.HUST_GOLD);
        btnJoin.setPreferredSize(new Dimension(150, 38));
        btnJoin.addActionListener(e -> joinSelectedRoom());

        pnlRoomActions.add(btnViewResults);
        pnlRoomActions.add(btnJoin);

        pnlBottom.add(lblStatus, BorderLayout.WEST);
        pnlBottom.add(pnlRoomActions, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadRooms() {
        lblStatus.setText("Đang tải danh sách phòng thi từ máy chủ...");
        btnRefresh.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private RoomListResponse response;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.ROOM_LIST_REQ);
                    Packet res = client.sendRequest(p, OpCodes.ROOM_LIST_RES).get();
                    if (res != null && res.isSuccess()) {
                        response = res.getPayloadAs(RoomListResponse.class);
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
                    lblStatus.setText("Lỗi tải danh sách phòng: " + error.getMessage());
                    return;
                }

                tableModel.setRowCount(0);
                if (response != null && response.getRooms() != null) {
                    currentRooms = response.getRooms();
                    for (RoomInfoDTO r : currentRooms) {
                        tableModel.addRow(new Object[]{
                                r.getRoomCode(),
                                r.getTitle(),
                                r.getHostUsername(),
                                r.getTopic(),
                                r.getDifficulty(),
                                r.getQuestionCount() + " câu",
                                r.getDurationMinutes() + " phút",
                                r.getCurrentParticipants() + " / " + r.getMaxParticipants(),
                                r.getStatus()
                        });
                    }
                    lblStatus.setForeground(Theme.TEXT_SECONDARY);
                    lblStatus.setText(String.format("Tìm thấy %d phòng thi. Chọn phòng để 'Vào Phòng Thi' hoặc 'Xem Bảng Điểm'.", currentRooms.size()));
                } else {
                    lblStatus.setText("Hiện không có phòng thi nào đang hoạt động.");
                }
            }
        };
        worker.execute();
    }

    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog(app, "Tạo Phòng Thi Mới - HUST", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(app);
        dialog.getContentPane().setBackground(Theme.BG_DARK);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Title
        JLabel lblT = new JLabel("Tên phòng thi:");
        lblT.setFont(Theme.FONT_BODY_BOLD);
        lblT.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblT, gbc);

        gbc.gridy++;
        ModernTextField txtTitle = new ModernTextField("VD: Thi Giữa Kỳ Mạng Máy Tính");
        txtTitle.setText("Thi Thử Đánh Giá Năng Lực HUST");
        pnl.add(txtTitle, gbc);

        // Topic
        gbc.gridy++;
        JLabel lblTopic = new JLabel("Chủ đề học phần:");
        lblTopic.setFont(Theme.FONT_BODY_BOLD);
        lblTopic.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblTopic, gbc);

        gbc.gridy++;
        JComboBox<String> cmbTopic = new JComboBox<>(QuestionCategory.ALL_CATEGORIES);
        cmbTopic.setFont(Theme.FONT_BODY);
        cmbTopic.setBackground(Theme.BG_INPUT);
        cmbTopic.setForeground(Theme.TEXT_PRIMARY);
        pnl.add(cmbTopic, gbc);

        // Difficulty
        gbc.gridy++;
        JLabel lblDiff = new JLabel("Độ khó đề thi:");
        lblDiff.setFont(Theme.FONT_BODY_BOLD);
        lblDiff.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblDiff, gbc);

        gbc.gridy++;
        JComboBox<String> cmbDiff = new JComboBox<>(new String[]{
                QuestionDifficulty.ALL, QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD
        });
        cmbDiff.setFont(Theme.FONT_BODY);
        cmbDiff.setBackground(Theme.BG_INPUT);
        cmbDiff.setForeground(Theme.TEXT_PRIMARY);
        pnl.add(cmbDiff, gbc);

        // Questions & Duration
        gbc.gridy++;
        JPanel pnlNums = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlNums.setOpaque(false);

        JPanel pnlQ = new JPanel(new BorderLayout(0, 4));
        pnlQ.setOpaque(false);
        JLabel lblQ = new JLabel("Số câu hỏi:");
        lblQ.setFont(Theme.FONT_BODY_BOLD);
        lblQ.setForeground(Theme.TEXT_SECONDARY);
        pnlQ.add(lblQ, BorderLayout.NORTH);
        JSpinner spnQ = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        spnQ.setFont(Theme.FONT_BODY_BOLD);
        pnlQ.add(spnQ, BorderLayout.CENTER);

        JPanel pnlD = new JPanel(new BorderLayout(0, 4));
        pnlD.setOpaque(false);
        JLabel lblD = new JLabel("Thời gian (phút):");
        lblD.setFont(Theme.FONT_BODY_BOLD);
        lblD.setForeground(Theme.TEXT_SECONDARY);
        pnlD.add(lblD, BorderLayout.NORTH);
        JSpinner spnD = new JSpinner(new SpinnerNumberModel(5, 1, 120, 1));
        spnD.setFont(Theme.FONT_BODY_BOLD);
        pnlD.add(spnD, BorderLayout.CENTER);

        pnlNums.add(pnlQ);
        pnlNums.add(pnlD);
        pnl.add(pnlNums, gbc);

        // Optional Password
        gbc.gridy++;
        JLabel lblPass = new JLabel("Mật khẩu phòng (Để trống nếu không khóa):");
        lblPass.setFont(Theme.FONT_BODY_BOLD);
        lblPass.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblPass, gbc);

        gbc.gridy++;
        ModernTextField.PasswordField txtPass = new ModernTextField.PasswordField("Mật khẩu tùy chọn");
        pnl.add(txtPass, gbc);

        dialog.add(pnl, BorderLayout.CENTER);

        // Bottom Dialog Actions
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        pnlBtn.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Hủy", ModernButton.Style.OUTLINE);
        btnCancel.addActionListener(e -> dialog.dispose());

        ModernButton btnSubmit = new ModernButton("Tạo & Vào Phòng Chờ", ModernButton.Style.HUST);
        btnSubmit.setPreferredSize(new Dimension(190, 38));
        btnSubmit.addActionListener(e -> {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên phòng thi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String topic = (String) cmbTopic.getSelectedItem();
            String diff = (String) cmbDiff.getSelectedItem();
            int qCount = (Integer) spnQ.getValue();
            int duration = (Integer) spnD.getValue();
            String pass = new String(txtPass.getPassword()).trim();
            if (pass.isEmpty()) pass = null;

            CreateRoomRequest req = new CreateRoomRequest(title, duration, qCount, topic, diff, 10, pass);

            btnSubmit.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                private CreateRoomResponse res;
                private Exception err;

                @Override
                protected Void doInBackground() {
                    try {
                        Packet p = Packet.ok(OpCodes.CREATE_ROOM_REQ, req);
                        Packet r = client.sendRequest(p, OpCodes.CREATE_ROOM_RES).get();
                        if (r != null && r.isSuccess()) {
                            res = r.getPayloadAs(CreateRoomResponse.class);
                        } else {
                            throw new Exception(r != null ? r.getPayloadAsString() : "Tạo phòng thất bại");
                        }
                    } catch (Exception ex) {
                        this.err = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnSubmit.setEnabled(true);
                    if (err != null) {
                        JOptionPane.showMessageDialog(dialog, "Lỗi tạo phòng: " + err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (res != null && res.isSuccess()) {
                        client.setCurrentRoomCode(res.getRoomCode());
                        dialog.dispose();
                        app.enterRoomWaitingScreen(res.getRoomCode(), res.getRoomInfo(), true);
                    }
                }
            };
            worker.execute();
        });

        pnlBtn.add(btnCancel);
        pnlBtn.add(btnSubmit);
        dialog.add(pnlBtn, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void joinSelectedRoom() {
        int selRow = tblRooms.getSelectedRow();
        if (selRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng thi trong danh sách để tham gia!", "Chưa chọn phòng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        RoomInfoDTO room = currentRooms.get(selRow);
        if (!RoomStatus.isJoinable(room.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tham gia: Phòng thi đang ở trạng thái '" + room.getStatus() + "'!\n" +
                            "Chỉ các phòng có trạng thái 'NOT_STARTED' mới được phép tham gia.",
                    "Không thể vào phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String password = null;
        if (room.isHasPassword()) {
            password = JOptionPane.showInputDialog(this, "Phòng này có đặt mật khẩu bảo vệ. Vui lòng nhập mật khẩu:", "Yêu cầu mật khẩu", JOptionPane.QUESTION_MESSAGE);
            if (password == null) return;
        }

        final String finalPass = password;
        btnJoin.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private JoinRoomResponse res;
            private Exception err;

            @Override
            protected Void doInBackground() {
                try {
                    JoinRoomRequest req = new JoinRoomRequest(room.getRoomCode(), finalPass);
                    Packet p = Packet.ok(OpCodes.JOIN_ROOM_REQ, req);
                    Packet r = client.sendRequest(p, OpCodes.JOIN_ROOM_RES).get();
                    if (r != null && r.isSuccess()) {
                        res = r.getPayloadAs(JoinRoomResponse.class);
                    } else {
                        throw new Exception(r != null ? r.getPayloadAsString() : "Không thể tham gia phòng");
                    }
                } catch (Exception ex) {
                    this.err = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                btnJoin.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(RoomLobbyListScreen.this,
                            "Lỗi khi vào phòng: " + err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (res != null && res.isSuccess()) {
                    client.setCurrentRoomCode(room.getRoomCode());
                    app.enterRoomWaitingScreen(room.getRoomCode(), room, false);
                }
            }
        };
        worker.execute();
    }

    private void viewSelectedRoomResults() {
        int selRow = tblRooms.getSelectedRow();
        if (selRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng thi đã kết thúc để xem kết quả!", "Chưa chọn phòng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        RoomInfoDTO room = currentRooms.get(selRow);
        if (!RoomStatus.FINISHED.equalsIgnoreCase(room.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Bảng điểm chỉ khả dụng cho các phòng đã KẾT THÚC (FINISHED)!\n" +
                            "Trạng thái phòng hiện tại là: " + room.getStatus(),
                    "Phòng chưa kết thúc", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        btnViewResults.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<UserExamRecordDTO> results;
            private Exception err;

            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.COMPLETED_ROOM_RESULT_REQ, room.getRoomCode());
                    Packet r = client.sendRequest(p, OpCodes.COMPLETED_ROOM_RESULT_RES).get();
                    if (r != null && r.isSuccess()) {
                        results = r.getPayloadAsList(UserExamRecordDTO.class);
                    }
                } catch (Exception ex) {
                    this.err = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                btnViewResults.setEnabled(true);
                if (err != null || results == null || results.isEmpty()) {
                    JOptionPane.showMessageDialog(RoomLobbyListScreen.this,
                            "Không tìm thấy dữ liệu kết quả của phòng " + room.getRoomCode(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                showCompletedRoomDialog(room, results);
            }
        };
        worker.execute();
    }

    private void showCompletedRoomDialog(RoomInfoDTO room, List<UserExamRecordDTO> results) {
        JDialog dialog = new JDialog(app, "Kết Quả Phòng Thi - " + room.getRoomCode(), true);
        dialog.setSize(680, 480);
        dialog.setLocationRelativeTo(app);
        dialog.getContentPane().setBackground(Theme.BG_DARK);
        dialog.setLayout(new BorderLayout(0, 12));

        JPanel pnlTop = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTop.setOpaque(false);
        pnlTop.setBorder(new EmptyBorder(16, 20, 0, 20));

        JLabel lblTitle = new JLabel("KẾT QUẢ BÀI THI: " + room.getTitle());
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(String.format("Mã phòng: %s | Chủ đề: %s | Số câu hỏi: %d",
                room.getRoomCode(), room.getTopic(), room.getQuestionCount()));
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        pnlTop.add(lblTitle);
        pnlTop.add(lblSub);
        dialog.add(pnlTop, BorderLayout.NORTH);

        String[] cols = {"Hạng", "Tài Khoản Thí Sinh", "Điểm Số (/10.0)", "Số Câu Đúng", "Tổng Số Câu"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        int rank = 1;
        for (UserExamRecordDTO rec : results) {
            model.addRow(new Object[]{
                    "#" + (rank++),
                    rec.getUsername(),
                    String.format("%.1f", rec.getScore()),
                    rec.getCorrectCount(),
                    rec.getTotalQuestions()
            });
        }

        JTable table = new JTable(model);
        Theme.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(new EmptyBorder(0, 20, 0, 20));
        Theme.styleScrollPane(scroll);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        pnlClose.setOpaque(false);
        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.HUST);
        btnClose.setPreferredSize(new Dimension(120, 36));
        btnClose.addActionListener(e -> dialog.dispose());
        pnlClose.add(btnClose);
        dialog.add(pnlClose, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
