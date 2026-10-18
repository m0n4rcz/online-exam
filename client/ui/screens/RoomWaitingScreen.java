package client.ui.screens;

import client.net.NetworkClient;
import client.net.ServerEventListener;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.ModernTextField;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Multiplayer Room Waiting Lobby before the host begins the exam.
 * Features real-time participant roster updates, ready toggle, in-room chat,
 * and host-controlled exam initiation.
 */
public class RoomWaitingScreen extends JPanel implements ServerEventListener {

    private final ClientApp app;
    private final NetworkClient client;

    private String roomCode;
    private RoomInfoDTO roomInfo;
    private boolean isHost = false;
    private boolean isReady = false;

    // UI Components
    private JLabel lblRoomTitle;
    private JLabel lblRoomMeta;
    private JTable tblParticipants;
    private DefaultTableModel participantModel;

    // Chat
    private JTextArea txtChatArea;
    private ModernTextField txtChatInput;
    private ModernButton btnSendChat;

    // Action buttons
    private ModernButton btnToggleReady;
    private ModernButton btnStartExam;
    private ModernButton btnLeaveRoom;
    private JLabel lblCountdown;

    public RoomWaitingScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // Top Header
        ModernCard topCard = new ModernCard(new BorderLayout(16, 0));
        topCard.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTitle.setOpaque(false);

        lblRoomTitle = new JLabel("ROOM LOBBY: LOADING...");
        lblRoomTitle.setFont(Theme.FONT_TITLE);
        lblRoomTitle.setForeground(Theme.TEXT_PRIMARY);

        lblRoomMeta = new JLabel("Connecting to room lobby...");
        lblRoomMeta.setFont(Theme.FONT_SMALL);
        lblRoomMeta.setForeground(Theme.TEXT_SECONDARY);

        pnlTitle.add(lblRoomTitle);
        pnlTitle.add(lblRoomMeta);
        topCard.add(pnlTitle, BorderLayout.WEST);

        lblCountdown = new JLabel("", SwingConstants.RIGHT);
        lblCountdown.setFont(Theme.FONT_TITLE);
        lblCountdown.setForeground(Theme.WARNING);
        topCard.add(lblCountdown, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        // Center Split: Left = Participants, Right = Live Chat
        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlCenter.setOpaque(false);

        // Left: Participant List
        ModernCard pnlLeft = new ModernCard(new BorderLayout(0, 10));
        JLabel lblPartTitle = new JLabel("PARTICIPANTS IN ROOM");
        lblPartTitle.setFont(Theme.FONT_SUBTITLE);
        lblPartTitle.setForeground(Theme.TEXT_PRIMARY);
        pnlLeft.add(lblPartTitle, BorderLayout.NORTH);

        String[] cols = {"Username", "Display Name", "Role", "Status"};
        participantModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblParticipants = new JTable(participantModel);
        Theme.styleTable(tblParticipants);

        // Status renderer
        tblParticipants.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                String val = value != null ? value.toString() : "";
                boolean ready = "READY".equalsIgnoreCase(val);
                Color bg = ready ? Theme.SUCCESS : Theme.BORDER;
                Color fg = ready ? Color.WHITE : Theme.TEXT_MUTED;
                return Theme.createBadge(val, bg, fg);
            }
        });

        JScrollPane scrollPart = new JScrollPane(tblParticipants);
        scrollPart.getViewport().setBackground(Theme.BG_SURFACE);
        scrollPart.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pnlLeft.add(scrollPart, BorderLayout.CENTER);

        // Right: In-Room Chat
        ModernCard pnlRight = new ModernCard(new BorderLayout(0, 10));
        JLabel lblChatTitle = new JLabel("ROOM LIVE CHAT");
        lblChatTitle.setFont(Theme.FONT_SUBTITLE);
        lblChatTitle.setForeground(Theme.TEXT_PRIMARY);
        pnlRight.add(lblChatTitle, BorderLayout.NORTH);

        txtChatArea = new JTextArea();
        txtChatArea.setEditable(false);
        txtChatArea.setFont(Theme.FONT_BODY);
        txtChatArea.setBackground(Theme.BG_SURFACE);
        txtChatArea.setForeground(Theme.TEXT_PRIMARY);
        txtChatArea.setLineWrap(true);
        txtChatArea.setWrapStyleWord(true);
        txtChatArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollChat = new JScrollPane(txtChatArea);
        scrollChat.getViewport().setBackground(Theme.BG_SURFACE);
        scrollChat.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pnlRight.add(scrollChat, BorderLayout.CENTER);

        JPanel pnlChatInput = new JPanel(new BorderLayout(8, 0));
        pnlChatInput.setOpaque(false);
        txtChatInput = new ModernTextField("Type a message to the room...");
        txtChatInput.addActionListener(e -> sendChatMessage());

        btnSendChat = new ModernButton("Send", ModernButton.Style.SECONDARY);
        btnSendChat.setPreferredSize(new Dimension(80, 36));
        btnSendChat.addActionListener(e -> sendChatMessage());

        pnlChatInput.add(txtChatInput, BorderLayout.CENTER);
        pnlChatInput.add(btnSendChat, BorderLayout.EAST);
        pnlRight.add(pnlChatInput, BorderLayout.SOUTH);

        pnlCenter.add(pnlLeft);
        pnlCenter.add(pnlRight);
        add(pnlCenter, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);

        btnLeaveRoom = new ModernButton("Leave Room", ModernButton.Style.DANGER);
        btnLeaveRoom.setPreferredSize(new Dimension(140, 42));
        btnLeaveRoom.addActionListener(e -> leaveRoom());

        JPanel pnlRightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlRightActions.setOpaque(false);

        btnToggleReady = new ModernButton("Toggle Ready", ModernButton.Style.OUTLINE);
        btnToggleReady.setPreferredSize(new Dimension(160, 42));
        btnToggleReady.addActionListener(e -> toggleReady());

        btnStartExam = new ModernButton("Start Exam Now (Host)", ModernButton.Style.PRIMARY);
        btnStartExam.setPreferredSize(new Dimension(200, 42));
        btnStartExam.addActionListener(e -> startExam());

        pnlRightActions.add(btnToggleReady);
        pnlRightActions.add(btnStartExam);

        pnlBottom.add(btnLeaveRoom, BorderLayout.WEST);
        pnlBottom.add(pnlRightActions, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void setupRoom(String roomCode, RoomInfoDTO info, boolean isHost) {
        this.roomCode = roomCode;
        this.roomInfo = info;
        this.isHost = isHost;
        this.isReady = false;

        lblCountdown.setText("");
        lblRoomTitle.setText(String.format("ROOM %s: %s", roomCode, info != null ? info.getTitle() : "Online Test"));
        if (info != null) {
            lblRoomMeta.setText(String.format("Host: %s | Topic: %s | Difficulty: %s | Questions: %d | Duration: %d mins",
                    info.getHostUsername(), info.getTopic(), info.getDifficulty(), info.getQuestionCount(), info.getDurationMinutes()));
        }

        txtChatArea.setText("--- Welcome to Room " + roomCode + "! Chat with other players here ---\n");

        // Visibility based on host status
        btnStartExam.setVisible(isHost);
        btnToggleReady.setVisible(!isHost);

        // Register listener for server push events
        client.addListener(this);

        // Load participants initial snapshot
        participantModel.setRowCount(0);
        String myUser = client.getCurrentUser() != null ? client.getCurrentUser().getUsername() : "me";
        String myName = client.getCurrentUser() != null ? client.getCurrentUser().getDisplayName() : "Me";
        participantModel.addRow(new Object[]{myUser, myName, isHost ? "HOST" : "STUDENT", isHost ? "READY" : "NOT READY"});
    }

    private void toggleReady() {
        btnToggleReady.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Packet res;
            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.TOGGLE_READY_REQ);
                    res = client.sendRequest(p, OpCodes.TOGGLE_READY_RES).get();
                } catch (Exception ignored) {}
                return null;
            }
            @Override
            protected void done() {
                btnToggleReady.setEnabled(true);
                if (res != null && res.isSuccess()) {
                    isReady = !isReady;
                    btnToggleReady.setText(isReady ? "Cancel Ready (Hủy sẵn sàng)" : "I'm Ready! (Sẵn sàng)");
                }
            }
        };
        worker.execute();
    }

    private void startExam() {
        if (!isHost) return;
        btnStartExam.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Packet res;
            private Exception err;
            @Override
            protected Void doInBackground() {
                try {
                    Packet p = Packet.ok(OpCodes.START_ROOM_REQ);
                    res = client.sendRequest(p, OpCodes.START_ROOM_RES).get();
                } catch (Exception e) {
                    this.err = e;
                }
                return null;
            }
            @Override
            protected void done() {
                if (err != null) {
                    btnStartExam.setEnabled(true);
                    JOptionPane.showMessageDialog(RoomWaitingScreen.this,
                            "Failed to start exam: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void leaveRoom() {
        client.removeListener(this);
        client.sendPacket(Packet.ok(OpCodes.LEAVE_ROOM_REQ));
        client.setCurrentRoomCode(null);
        app.showScreen("ROOM_LOBBY");
    }

    private void sendChatMessage() {
        String msg = txtChatInput.getText().trim();
        if (msg.isEmpty()) return;

        ChatMessageDTO chat = new ChatMessageDTO(roomCode,
                client.getCurrentUser() != null ? client.getCurrentUser().getUsername() : "me",
                client.getCurrentUser() != null ? client.getCurrentUser().getDisplayName() : "Me",
                msg);

        // Display locally immediately
        appendChat(chat.getSenderDisplayName(), msg);
        txtChatInput.setText("");

        // Broadcast to server
        client.sendPacket(Packet.ok(OpCodes.ROOM_CHAT_REQ, chat));
    }

    private void appendChat(String sender, String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        txtChatArea.append(String.format("[%s] %s: %s\n", time, sender, msg));
        txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
    }

    // ==========================================
    // SERVER EVENT LISTENER IMPLEMENTATION
    // ==========================================

    @Override
    public void onRoomUpdate(RoomUpdateEventDTO event) {
        if (event == null) return;
        if (event.getMessage() != null && !event.getMessage().isEmpty()) {
            appendChat("SYSTEM", event.getMessage());
        }

        List<RoomParticipantDTO> participants = event.getParticipants();
        if (participants != null) {
            participantModel.setRowCount(0);
            for (RoomParticipantDTO p : participants) {
                participantModel.addRow(new Object[]{
                        p.getUsername(),
                        p.getDisplayName(),
                        p.isHost() ? "HOST" : "STUDENT",
                        p.isReady() ? "READY" : "NOT READY"
                });
            }
        }
    }

    @Override
    public void onRoomStart(int countdownSeconds) {
        lblCountdown.setText("Exam starting in " + countdownSeconds + "s...");
    }

    @Override
    public void onExamInit(ExamInitDataDTO examData) {
        client.removeListener(this);
        app.startMultiplayerExam(roomCode, roomInfo, examData, isHost);
    }

    @Override
    public void onRoomChat(ChatMessageDTO chat) {
        if (chat != null) {
            appendChat(chat.getSenderDisplayName(), chat.getMessage());
        }
    }

    @Override
    public void onConnectionLost(String reason) {
        JOptionPane.showMessageDialog(this, "Connection lost: " + reason);
        app.showScreen("LOGIN");
    }
}
