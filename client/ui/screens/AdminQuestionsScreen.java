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
import shared.dtos.ActivityLogDTO;
import shared.dtos.FullQuestionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Administrative and pedagogical management screen.
 * Provides Question Bank customization (topic, difficulty, CRUD) and
 * Activity Logging audit inspection.
 */
public class AdminQuestionsScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    // Question Bank
    private JTable tblQuestions;
    private DefaultTableModel questionModel;
    private List<FullQuestionDTO> currentQuestions;

    // Activity Logs
    private JTable tblLogs;
    private DefaultTableModel logModel;

    private JLabel lblStatus;
    private ModernButton btnRefresh;
    private ModernButton btnAddQuestion;
    private ModernButton btnDeleteQuestion;

    public AdminQuestionsScreen(ClientApp app) {
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
        JLabel lblHeading = new JLabel("QUESTION BANK & SYSTEM AUDIT LOGS");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Manage questions classified by topic & difficulty, and review real-time audit logs");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        pnlTitle.add(lblHeading);
        pnlTitle.add(lblSub);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Back to Home", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 38));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnRefresh = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadData());

        btnAddQuestion = new ModernButton("+ Add Question", ModernButton.Style.PRIMARY);
        btnAddQuestion.setPreferredSize(new Dimension(150, 38));
        btnAddQuestion.addActionListener(e -> showAddQuestionDialog());

        btnDeleteQuestion = new ModernButton("Delete Question", ModernButton.Style.DANGER);
        btnDeleteQuestion.setPreferredSize(new Dimension(150, 38));
        btnDeleteQuestion.addActionListener(e -> deleteSelectedQuestion());

        pnlActions.add(btnBack);
        pnlActions.add(btnRefresh);
        pnlActions.add(btnDeleteQuestion);
        pnlActions.add(btnAddQuestion);

        pnlTop.add(pnlTitle, BorderLayout.WEST);
        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // Center Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_BODY_BOLD);
        tabbedPane.setBackground(Theme.BG_SURFACE);
        tabbedPane.setForeground(Theme.TEXT_PRIMARY);

        tabbedPane.addTab("Question Bank Management", createQuestionsPanel());
        tabbedPane.addTab("Activity Audit Logs", createLogsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        lblStatus = new JLabel("Ready");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_SECONDARY);
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel createQuestionsPanel() {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"ID", "Topic / Category", "Difficulty", "Question Content", "Correct", "Explanation"};
        questionModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblQuestions = new JTable(questionModel);
        Theme.styleTable(tblQuestions);
        tblQuestions.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblQuestions.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblQuestions.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblQuestions.getColumnModel().getColumn(3).setPreferredWidth(340);
        tblQuestions.getColumnModel().getColumn(4).setPreferredWidth(60);

        JScrollPane scroll = new JScrollPane(tblQuestions);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createLogsPanel() {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Timestamp", "Username", "Action", "Details", "IP Address"};
        logModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblLogs = new JTable(logModel);
        Theme.styleTable(tblLogs);
        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(140);
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(320);

        JScrollPane scroll = new JScrollPane(tblLogs);
        scroll.getViewport().setBackground(Theme.BG_SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void loadData() {
        lblStatus.setText("Loading question bank and activity logs...");
        btnRefresh.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<FullQuestionDTO> qList;
            private List<ActivityLogDTO> logList;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    // Fetch Questions
                    Packet p1 = Packet.ok(OpCodes.QUESTION_LIST_REQ);
                    Packet r1 = client.sendRequest(p1, OpCodes.QUESTION_LIST_RES).get();
                    if (r1 != null && r1.isSuccess()) {
                        qList = r1.getPayloadAsList(FullQuestionDTO.class);
                    }

                    // Fetch Logs
                    Packet p2 = Packet.ok(OpCodes.GET_LOGS_REQ);
                    Packet r2 = client.sendRequest(p2, OpCodes.GET_LOGS_RES).get();
                    if (r2 != null && r2.isSuccess()) {
                        logList = r2.getPayloadAsList(ActivityLogDTO.class);
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

                // Update Questions table
                questionModel.setRowCount(0);
                if (qList != null) {
                    currentQuestions = qList;
                    for (FullQuestionDTO q : qList) {
                        questionModel.addRow(new Object[]{
                                q.getId(),
                                q.getCategory(),
                                q.getDifficulty(),
                                q.getContent(),
                                q.getCorrectOption(),
                                q.getExplanation()
                        });
                    }
                }

                // Update Logs table
                logModel.setRowCount(0);
                if (logList != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    for (ActivityLogDTO log : logList) {
                        String timeStr = sdf.format(new Date(log.getTimestamp()));
                        logModel.addRow(new Object[]{
                                timeStr,
                                log.getUsername(),
                                log.getAction(),
                                log.getDetails(),
                                log.getIpAddress()
                        });
                    }
                }

                lblStatus.setForeground(Theme.TEXT_SECONDARY);
                lblStatus.setText(String.format("Loaded %d questions and %d log entries.",
                        qList != null ? qList.size() : 0, logList != null ? logList.size() : 0));
            }
        };
        worker.execute();
    }

    private void showAddQuestionDialog() {
        JDialog dialog = new JDialog(app, "Add New Question to Bank", true);
        dialog.setSize(560, 600);
        dialog.setLocationRelativeTo(app);
        dialog.getContentPane().setBackground(Theme.BG_DARK);
        dialog.setLayout(new BorderLayout());

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 20, 16, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Content
        pnl.add(new JLabel("Question Content:"), gbc);
        gbc.gridy++;
        ModernTextField txtContent = new ModernTextField("What is the primary function of DNS?");
        pnl.add(txtContent, gbc);

        // Options A, B, C, D
        gbc.gridy++;
        pnl.add(new JLabel("Option A:"), gbc);
        gbc.gridy++;
        ModernTextField txtA = new ModernTextField("Resolve domain names to IP addresses");
        pnl.add(txtA, gbc);

        gbc.gridy++;
        pnl.add(new JLabel("Option B:"), gbc);
        gbc.gridy++;
        ModernTextField txtB = new ModernTextField("Encrypt web communication");
        pnl.add(txtB, gbc);

        gbc.gridy++;
        pnl.add(new JLabel("Option C:"), gbc);
        gbc.gridy++;
        ModernTextField txtC = new ModernTextField("Route network packets");
        pnl.add(txtC, gbc);

        gbc.gridy++;
        pnl.add(new JLabel("Option D:"), gbc);
        gbc.gridy++;
        ModernTextField txtD = new ModernTextField("Assign dynamic IP addresses");
        pnl.add(txtD, gbc);

        // Correct Option, Topic & Difficulty
        gbc.gridy++;
        JPanel pnlMeta = new JPanel(new GridLayout(1, 3, 8, 0));
        pnlMeta.setOpaque(false);

        JPanel pnlCorr = new JPanel(new BorderLayout(0, 2));
        pnlCorr.setOpaque(false);
        pnlCorr.add(new JLabel("Correct:"), BorderLayout.NORTH);
        JComboBox<String> cmbCorr = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        pnlCorr.add(cmbCorr, BorderLayout.CENTER);

        JPanel pnlTopic = new JPanel(new BorderLayout(0, 2));
        pnlTopic.setOpaque(false);
        pnlTopic.add(new JLabel("Topic:"), BorderLayout.NORTH);
        JComboBox<String> cmbTopic = new JComboBox<>(new String[]{
                QuestionCategory.NETWORKING, QuestionCategory.JAVA_CORE, QuestionCategory.DATABASE,
                QuestionCategory.OPERATING_SYSTEMS, QuestionCategory.DATA_STRUCTURES, QuestionCategory.GENERAL_IT
        });
        pnlTopic.add(cmbTopic, BorderLayout.CENTER);

        JPanel pnlDiff = new JPanel(new BorderLayout(0, 2));
        pnlDiff.setOpaque(false);
        pnlDiff.add(new JLabel("Difficulty:"), BorderLayout.NORTH);
        JComboBox<String> cmbDiff = new JComboBox<>(new String[]{
                QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD
        });
        pnlDiff.add(cmbDiff, BorderLayout.CENTER);

        pnlMeta.add(pnlCorr);
        pnlMeta.add(pnlTopic);
        pnlMeta.add(pnlDiff);
        pnl.add(pnlMeta, gbc);

        // Explanation
        gbc.gridy++;
        pnl.add(new JLabel("Explanation / Giải thích:"), gbc);
        gbc.gridy++;
        ModernTextField txtExpl = new ModernTextField("DNS converts human-friendly domain names to numeric IP addresses.");
        pnl.add(txtExpl, gbc);

        dialog.add(pnl, BorderLayout.CENTER);

        // Buttons
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlBtn.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.Style.OUTLINE);
        btnCancel.addActionListener(e -> dialog.dispose());

        ModernButton btnSave = new ModernButton("Save Question", ModernButton.Style.PRIMARY);
        btnSave.addActionListener(e -> {
            String content = txtContent.getText().trim();
            String a = txtA.getText().trim();
            String b = txtB.getText().trim();
            String c = txtC.getText().trim();
            String d = txtD.getText().trim();
            String corr = (String) cmbCorr.getSelectedItem();
            String topic = (String) cmbTopic.getSelectedItem();
            String diff = (String) cmbDiff.getSelectedItem();
            String expl = txtExpl.getText().trim();

            if (content.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in question content and all 4 options!");
                return;
            }

            FullQuestionDTO newQ = new FullQuestionDTO(
                    0, content, a, b, c, d, corr, expl, topic, diff);

            btnSave.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                private Packet res;
                private Exception err;
                @Override
                protected Void doInBackground() {
                    try {
                        Packet p = Packet.ok(OpCodes.QUESTION_ADD_REQ, newQ);
                        res = client.sendRequest(p, OpCodes.QUESTION_ADD_RES).get();
                    } catch (Exception ex) {
                        this.err = ex;
                    }
                    return null;
                }
                @Override
                protected void done() {
                    btnSave.setEnabled(true);
                    if (err != null) {
                        JOptionPane.showMessageDialog(dialog, "Error: " + err.getMessage());
                        return;
                    }
                    if (res != null && res.isSuccess()) {
                        dialog.dispose();
                        loadData();
                    }
                }
            };
            worker.execute();
        });

        pnlBtn.add(btnCancel);
        pnlBtn.add(btnSave);
        dialog.add(pnlBtn, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void deleteSelectedQuestion() {
        int sel = tblQuestions.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Please select a question to delete!");
            return;
        }

        int qId = (Integer) tblQuestions.getValueAt(sel, 0);
        int conf = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete question #" + qId + " from question bank?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        Packet p = Packet.ok(OpCodes.QUESTION_DELETE_REQ, String.valueOf(qId));
                        client.sendRequest(p, OpCodes.QUESTION_DELETE_RES).get();
                    } catch (Exception ignored) {}
                    return null;
                }
                @Override
                protected void done() {
                    loadData();
                }
            };
            worker.execute();
        }
    }
}
