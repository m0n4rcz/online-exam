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

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitle.setOpaque(false);
        JLabel lblHeading = new JLabel("TEST ROOMS LOBBY (PHÒNG THI ĐẤU)");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Browse available test rooms, join live competitions, or review finished results");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlTopActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlTopActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Back to Home", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadRooms());

        btnCreate = new ModernButton("+ Create Room", ModernButton.Style.PRIMARY);
        btnCreate.setPreferredSize(new Dimension(140, 38));
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

        String[] cols = {"Room Code", "Room Title", "Host", "Topic", "Difficulty", "Questions", "Duration", "Players", "Status"};
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
                String display = status;

                if (RoomStatus.NOT_STARTED.equalsIgnoreCase(status)) {
                    bg = Theme.SUCCESS;
                    display = "WAITING";
                } else if (RoomStatus.ONGOING.equalsIgnoreCase(status)) {
                    bg = Theme.WARNING;
                    display = "ONGOING";
                } else {
                    bg = Theme.TEXT_MUTED;
                    display = "FINISHED";
                }

                return Theme.createBadge(display, bg, fg);
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblRooms);
        scrollPane.getViewport().setBackground(Theme.BG_SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);

        lblStatus = new JLabel("Click Refresh to load active rooms");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);

        JPanel pnlRoomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlRoomActions.setOpaque(false);

        btnViewResults = new ModernButton("View Room Results", ModernButton.Style.OUTLINE);
        btnViewResults.setPreferredSize(new Dimension(160, 38));
        btnViewResults.addActionListener(e -> viewSelectedRoomResults());

        btnJoin = new ModernButton("Join Test Room", ModernButton.Style.SUCCESS);
        btnJoin.setPreferredSize(new Dimension(150, 38));
        btnJoin.addActionListener(e -> joinSelectedRoom());

        pnlRoomActions.add(btnViewResults);
        pnlRoomActions.add(btnJoin);

        pnlBottom.add(lblStatus, BorderLayout.WEST);
        pnlBottom.add(pnlRoomActions, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadRooms() {
        lblStatus.setText("Loading rooms...");
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
                    lblStatus.setText("Failed to load rooms: " + error.getMessage());
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
                                r.getQuestionCount() + " Qs",
                                r.getDurationMinutes() + " mins",
                                r.getCurrentParticipants() + " / " + r.getMaxParticipants(),
                                r.getStatus()
                        });
                    }
                    lblStatus.setForeground(Theme.TEXT_SECONDARY);
                    lblStatus.setText(String.format("Found %d rooms. Select a row to Join or View Results.", currentRooms.size()));
                } else {
                    lblStatus.setText("No active rooms found.");
                }
            }
        };
        worker.execute();
    }

    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog(app, "Create New Test Room", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 520);
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
        JLabel lblT = new JLabel("Room Title / Tên phòng thi:");
        lblT.setFont(Theme.FONT_BODY_BOLD);
        lblT.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblT, gbc);

        gbc.gridy++;
        ModernTextField txtTitle = new ModernTextField("Midterm Exam 2026");
        txtTitle.setText("Networking Quiz Room");
        pnl.add(txtTitle, gbc);

        // Topic
        gbc.gridy++;
        JLabel lblTopic = new JLabel("Topic / Chủ đề:");
        lblTopic.setFont(Theme.FONT_BODY_BOLD);
        lblTopic.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblTopic, gbc);

        gbc.gridy++;
        JComboBox<String> cmbTopic = new JComboBox<>(QuestionCategory.ALL_CATEGORIES);
        cmbTopic.setFont(Theme.FONT_BODY);
        pnl.add(cmbTopic, gbc);

        // Difficulty
        gbc.gridy++;
        JLabel lblDiff = new JLabel("Difficulty / Độ khó:");
        lblDiff.setFont(Theme.FONT_BODY_BOLD);
        lblDiff.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblDiff, gbc);

        gbc.gridy++;
        JComboBox<String> cmbDiff = new JComboBox<>(new String[]{
                QuestionDifficulty.ALL, QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD
        });
        pnl.add(cmbDiff, gbc);

        // Questions & Duration
        gbc.gridy++;
        JPanel pnlNums = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlNums.setOpaque(false);

        JPanel pnlQ = new JPanel(new BorderLayout(0, 4));
        pnlQ.setOpaque(false);
        pnlQ.add(new JLabel("Questions:"), BorderLayout.NORTH);
        JSpinner spnQ = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        pnlQ.add(spnQ, BorderLayout.CENTER);

        JPanel pnlD = new JPanel(new BorderLayout(0, 4));
        pnlD.setOpaque(false);
        pnlD.add(new JLabel("Duration (mins):"), BorderLayout.NORTH);
        JSpinner spnD = new JSpinner(new SpinnerNumberModel(5, 1, 120, 1));
        pnlD.add(spnD, BorderLayout.CENTER);

        pnlNums.add(pnlQ);
        pnlNums.add(pnlD);
        pnl.add(pnlNums, gbc);

        // Optional Password
        gbc.gridy++;
        JLabel lblPass = new JLabel("Room Password (Optional / Bỏ trống nếu không khóa):");
        lblPass.setFont(Theme.FONT_BODY_BOLD);
        lblPass.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblPass, gbc);

        gbc.gridy++;
        ModernTextField.PasswordField txtPass = new ModernTextField.PasswordField("Optional password");
        pnl.add(txtPass, gbc);

        dialog.add(pnl, BorderLayout.CENTER);

        // Bottom Dialog Actions
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        pnlBtn.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.Style.OUTLINE);
        btnCancel.addActionListener(e -> dialog.dispose());

        ModernButton btnSubmit = new ModernButton("Create & Enter Lobby", ModernButton.Style.PRIMARY);
        btnSubmit.setPreferredSize(new Dimension(170, 38));
        btnSubmit.addActionListener(e -> {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter room title!");
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
                            throw new Exception(r != null ? r.getPayloadAsString() : "Creation failed");
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
                        JOptionPane.showMessageDialog(dialog, "Error: " + err.getMessage(), "Create Failed", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Please select a room to join!");
            return;
        }

        RoomInfoDTO room = currentRooms.get(selRow);
        if (!RoomStatus.isJoinable(room.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Cannot join room: The exam is already " + room.getStatus() + "!\n" +
                            "Only rooms in 'NOT_STARTED' state can be joined.",
                    "Cannot Join Room", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String password = null;
        if (room.isHasPassword()) {
            password = JOptionPane.showInputDialog(this, "This room is password protected. Enter password:");
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
                        throw new Exception(r != null ? r.getPayloadAsString() : "Join failed");
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
                            "Failed to join room: " + err.getMessage(), "Join Failed", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Please select a completed room to view results!");
            return;
        }

        RoomInfoDTO room = currentRooms.get(selRow);
        if (!RoomStatus.FINISHED.equalsIgnoreCase(room.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Results are only available for rooms that have FINISHED!\n" +
                            "Current room status is: " + room.getStatus(),
                    "Room Not Finished", JOptionPane.INFORMATION_MESSAGE);
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
                            "No recorded results found for room " + room.getRoomCode());
                    return;
                }
                showCompletedRoomDialog(room, results);
            }
        };
        worker.execute();
    }

    private void showCompletedRoomDialog(RoomInfoDTO room, List<UserExamRecordDTO> results) {
        JDialog dialog = new JDialog(app, "Exam Results - Room " + room.getRoomCode(), true);
        dialog.setSize(620, 450);
        dialog.setLocationRelativeTo(app);
        dialog.getContentPane().setBackground(Theme.BG_DARK);
        dialog.setLayout(new BorderLayout(0, 12));

        JPanel pnlTop = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTop.setOpaque(false);
        pnlTop.setBorder(new EmptyBorder(16, 20, 0, 20));

        JLabel lblTitle = new JLabel("COMPLETED ROOM RESULTS: " + room.getTitle());
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(String.format("Room Code: %s | Topic: %s | Questions: %d",
                room.getRoomCode(), room.getTopic(), room.getQuestionCount()));
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        pnlTop.add(lblTitle);
        pnlTop.add(lblSub);
        dialog.add(pnlTop, BorderLayout.NORTH);

        String[] cols = {"Rank", "Student Username", "Score (/10.0)", "Correct Answers", "Total Questions"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
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
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        pnlClose.setOpaque(false);
        ModernButton btnClose = new ModernButton("Close", ModernButton.Style.PRIMARY);
        btnClose.addActionListener(e -> dialog.dispose());
        pnlClose.add(btnClose);
        dialog.add(pnlClose, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
