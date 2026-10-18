package client.ui.screens;

import client.net.NetworkClient;
import client.net.ServerEventListener;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.QuestionNavButton;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Live Exam Execution Engine screen.
 * Handles question presentation, real-time synchronized timer countdown,
 * in-flight answer modification, early submission, and host exam termination.
 */
public class ExamScreen extends JPanel implements ServerEventListener {

    private final ClientApp app;
    private final NetworkClient client;

    // Exam Mode & Data
    private boolean isMultiplayer = false;
    private boolean isHost = false;
    private String roomCode;
    private String examTitle = "Exam";
    private List<QuestionDTO> questions = new ArrayList<>();
    private final Map<Integer, String> studentAnswers = new HashMap<>();

    // State
    private int currentQuestionIndex = 0;
    private int remainingSeconds = 300;
    private javax.swing.Timer localTimer;
    private boolean isSubmitted = false;

    // UI Components
    private JLabel lblTitle;
    private JLabel lblMeta;
    private JLabel lblTimer;
    private JLabel lblParticipantNotice;

    // Question Viewer
    private JLabel lblQuestionHeader;
    private JLabel lblQuestionContent;
    private JRadioButton radA;
    private JRadioButton radB;
    private JRadioButton radC;
    private JRadioButton radD;
    private ButtonGroup optionGroup;

    // Navigation Palette
    private JPanel pnlQuestionGrid;
    private final List<QuestionNavButton> navButtons = new ArrayList<>();

    // Action Buttons
    private ModernButton btnPrev;
    private ModernButton btnNext;
    private ModernButton btnSubmit;
    private ModernButton btnHostEnd;

    public ExamScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        initUI();
    }

    private void initUI() {
        // 1. Top Bar: Title, Topic, Timer, Host Controls
        ModernCard topCard = new ModernCard(new BorderLayout(16, 0));
        topCard.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitle.setOpaque(false);

        lblTitle = new JLabel("ONLINE EXAM");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        lblMeta = new JLabel("Question 1 of 10 | Computer Networking");
        lblMeta.setFont(Theme.FONT_SMALL);
        lblMeta.setForeground(Theme.TEXT_SECONDARY);

        pnlTitle.add(lblTitle);
        pnlTitle.add(lblMeta);
        topCard.add(pnlTitle, BorderLayout.WEST);

        // Center notification
        lblParticipantNotice = new JLabel("", SwingConstants.CENTER);
        lblParticipantNotice.setFont(Theme.FONT_BADGE);
        lblParticipantNotice.setForeground(Theme.WARNING);
        topCard.add(lblParticipantNotice, BorderLayout.CENTER);

        // Right side: Host End Exam button + Countdown Timer
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        pnlRight.setOpaque(false);

        btnHostEnd = new ModernButton("End Exam (Host)", ModernButton.Style.DANGER);
        btnHostEnd.setPreferredSize(new Dimension(150, 36));
        btnHostEnd.setVisible(false);
        btnHostEnd.addActionListener(e -> hostEndExam());
        pnlRight.add(btnHostEnd);

        lblTimer = new JLabel("10:00", SwingConstants.CENTER);
        lblTimer.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 26));
        lblTimer.setForeground(Theme.TEXT_PRIMARY);
        lblTimer.setPreferredSize(new Dimension(100, 36));
        pnlRight.add(lblTimer);

        topCard.add(pnlRight, BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        // 2. Center & Side Layout
        JPanel pnlCenter = new JPanel(new BorderLayout(16, 0));
        pnlCenter.setOpaque(false);

        // Main Question Card (Left/Center)
        ModernCard questionCard = new ModernCard(new BorderLayout(0, 16));
        questionCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        lblQuestionHeader = new JLabel("Question 1");
        lblQuestionHeader.setFont(Theme.FONT_SUBTITLE);
        lblQuestionHeader.setForeground(Theme.TEXT_ACCENT);

        lblQuestionContent = new JLabel("<html><body>Loading question...</body></html>");
        lblQuestionContent.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 15));
        lblQuestionContent.setForeground(Theme.TEXT_PRIMARY);
        lblQuestionContent.setVerticalAlignment(SwingConstants.TOP);

        JPanel pnlQTop = new JPanel(new BorderLayout(0, 10));
        pnlQTop.setOpaque(false);
        pnlQTop.add(lblQuestionHeader, BorderLayout.NORTH);
        pnlQTop.add(lblQuestionContent, BorderLayout.CENTER);
        questionCard.add(pnlQTop, BorderLayout.NORTH);

        // 4 Multiple Choice Radio Buttons
        JPanel pnlOptions = new JPanel(new GridLayout(4, 1, 0, 10));
        pnlOptions.setOpaque(false);
        pnlOptions.setBorder(new EmptyBorder(10, 0, 10, 0));

        optionGroup = new ButtonGroup();
        radA = createOptionRadioButton("A");
        radB = createOptionRadioButton("B");
        radC = createOptionRadioButton("C");
        radD = createOptionRadioButton("D");

        pnlOptions.add(radA);
        pnlOptions.add(radB);
        pnlOptions.add(radC);
        pnlOptions.add(radD);

        questionCard.add(pnlOptions, BorderLayout.CENTER);

        // Navigation Prev / Next
        JPanel pnlNav = new JPanel(new BorderLayout());
        pnlNav.setOpaque(false);

        btnPrev = new ModernButton("← Previous Question", ModernButton.Style.OUTLINE);
        btnPrev.setPreferredSize(new Dimension(170, 40));
        btnPrev.addActionListener(e -> navigateQuestion(currentQuestionIndex - 1));

        btnNext = new ModernButton("Next Question →", ModernButton.Style.OUTLINE);
        btnNext.setPreferredSize(new Dimension(170, 40));
        btnNext.addActionListener(e -> navigateQuestion(currentQuestionIndex + 1));

        pnlNav.add(btnPrev, BorderLayout.WEST);
        pnlNav.add(btnNext, BorderLayout.EAST);
        questionCard.add(pnlNav, BorderLayout.SOUTH);

        pnlCenter.add(questionCard, BorderLayout.CENTER);

        // Right Sidebar: Question Palette Grid & Submit Button
        ModernCard sideCard = new ModernCard(new BorderLayout(0, 12));
        sideCard.setPreferredSize(new Dimension(260, 0));
        sideCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lblPalette = new JLabel("QUESTION PALETTE", SwingConstants.CENTER);
        lblPalette.setFont(Theme.FONT_HEADER);
        lblPalette.setForeground(Theme.TEXT_PRIMARY);
        sideCard.add(lblPalette, BorderLayout.NORTH);

        pnlQuestionGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        pnlQuestionGrid.setOpaque(false);
        JScrollPane scrollGrid = new JScrollPane(pnlQuestionGrid);
        scrollGrid.getViewport().setBackground(Theme.BG_CARD);
        scrollGrid.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        sideCard.add(scrollGrid, BorderLayout.CENTER);

        // Submit Button at bottom of sidebar
        btnSubmit = new ModernButton("Submit Exam Early", ModernButton.Style.SUCCESS);
        btnSubmit.setPreferredSize(new Dimension(220, 44));
        btnSubmit.addActionListener(e -> confirmAndSubmitExam());
        sideCard.add(btnSubmit, BorderLayout.SOUTH);

        pnlCenter.add(sideCard, BorderLayout.EAST);
        add(pnlCenter, BorderLayout.CENTER);
    }

    private JRadioButton createOptionRadioButton(String optionKey) {
        JRadioButton rad = new JRadioButton();
        rad.setFont(Theme.FONT_BODY);
        rad.setForeground(Theme.TEXT_PRIMARY);
        rad.setBackground(Theme.BG_SURFACE);
        rad.setFocusPainted(false);
        rad.setOpaque(true);
        rad.setBorder(new EmptyBorder(10, 14, 10, 14));
        rad.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rad.addActionListener(e -> {
            if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
                QuestionDTO q = questions.get(currentQuestionIndex);
                studentAnswers.put(q.getId(), optionKey);

                // Update palette button
                if (currentQuestionIndex < navButtons.size()) {
                    navButtons.get(currentQuestionIndex).setAnswered(true);
                }

                // If multiplayer, immediately synchronize answer to server
                if (isMultiplayer && roomCode != null) {
                    client.sendPacket(Packet.ok(OpCodes.SUBMIT_ANSWER_REQ,
                            new AnswerSubmissionDTO(roomCode, q.getId(), optionKey)));
                }
            }
        });

        optionGroup.add(rad);
        return rad;
    }

    // ==========================================
    // EXAM INITIALIZATION & LIFECYCLE
    // ==========================================

    public void startPracticeExam(PracticeResponseDTO practiceData) {
        this.isMultiplayer = false;
        this.isHost = false;
        this.roomCode = "PRACTICE";
        this.examTitle = "Solo Practice (" + practiceData.getTopic() + ")";
        this.questions = practiceData.getQuestions();
        this.remainingSeconds = practiceData.getDurationSeconds();

        btnHostEnd.setVisible(false);
        btnSubmit.setText("Submit Practice Exam");

        initExamSession();
    }

    public void startMultiplayerExam(String roomCode, RoomInfoDTO info, ExamInitDataDTO examData, boolean isHost) {
        this.isMultiplayer = true;
        this.isHost = isHost;
        this.roomCode = roomCode;
        this.examTitle = info != null ? info.getTitle() : "Room " + roomCode;
        this.questions = examData.getQuestions();
        this.remainingSeconds = examData.getDurationSeconds();

        btnHostEnd.setVisible(isHost);
        btnSubmit.setText("Submit Exam Early");

        initExamSession();
    }

    private void initExamSession() {
        this.isSubmitted = false;
        this.studentAnswers.clear();
        this.currentQuestionIndex = 0;
        lblParticipantNotice.setText("");

        lblTitle.setText(examTitle);

        // Register server push event listener
        client.addListener(this);

        // Build Question Navigation Grid
        pnlQuestionGrid.removeAll();
        navButtons.clear();

        for (int i = 0; i < questions.size(); i++) {
            QuestionNavButton btn = new QuestionNavButton(i + 1, i);
            btn.addActionListener(e -> navigateQuestion(btn.getQuestionIndex()));
            navButtons.add(btn);
            pnlQuestionGrid.add(btn);
        }
        pnlQuestionGrid.revalidate();
        pnlQuestionGrid.repaint();

        // Load First Question
        loadQuestion(0);

        // Start Countdown Timer
        startTimer();
    }

    private void startTimer() {
        if (localTimer != null && localTimer.isRunning()) {
            localTimer.stop();
        }

        updateTimerDisplay();

        localTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            updateTimerDisplay();

            if (remainingSeconds <= 0) {
                localTimer.stop();
                lblTimer.setText("00:00");
                lblTimer.setForeground(Theme.DANGER);
                if (!isSubmitted) {
                    JOptionPane.showMessageDialog(this,
                            "Time is up! Your answers are being submitted automatically.",
                            "Time Expired", JOptionPane.INFORMATION_MESSAGE);
                    submitExam(false);
                }
            }
        });
        localTimer.start();
    }

    private void updateTimerDisplay() {
        int m = Math.max(0, remainingSeconds / 60);
        int s = Math.max(0, remainingSeconds % 60);
        lblTimer.setText(String.format("%02d:%02d", m, s));

        if (remainingSeconds < 60) {
            lblTimer.setForeground(Theme.DANGER);
        } else if (remainingSeconds < 180) {
            lblTimer.setForeground(Theme.WARNING);
        } else {
            lblTimer.setForeground(Theme.TEXT_PRIMARY);
        }
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        this.currentQuestionIndex = index;
        QuestionDTO q = questions.get(index);

        lblQuestionHeader.setText(String.format("Question %d of %d", index + 1, questions.size()));
        lblMeta.setText(String.format("Category: %s | Difficulty: %s", q.getCategory(), q.getDifficulty()));

        lblQuestionContent.setText("<html><body style='width: 580px;'>" + q.getContent() + "</body></html>");

        radA.setText("A.  " + q.getOptionA());
        radB.setText("B.  " + q.getOptionB());
        radC.setText("C.  " + q.getOptionC());
        radD.setText("D.  " + q.getOptionD());

        // Restore previous answer if any
        optionGroup.clearSelection();
        String selected = studentAnswers.get(q.getId());
        if (selected != null) {
            switch (selected.toUpperCase()) {
                case "A" -> radA.setSelected(true);
                case "B" -> radB.setSelected(true);
                case "C" -> radC.setSelected(true);
                case "D" -> radD.setSelected(true);
            }
        }

        // Update palette highlight
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).setCurrent(i == index);
        }

        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < questions.size() - 1);
    }

    private void navigateQuestion(int newIndex) {
        if (newIndex >= 0 && newIndex < questions.size()) {
            loadQuestion(newIndex);
        }
    }

    // ==========================================
    // EXAM SUBMISSION
    // ==========================================

    private void confirmAndSubmitExam() {
        if (isSubmitted) return;

        int answeredCount = studentAnswers.size();
        int total = questions.size();
        String msg = String.format("You have answered %d of %d questions.\nAre you sure you want to submit your exam now?",
                answeredCount, total);

        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Early Submission", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            submitExam(true);
        }
    }

    private void submitExam(boolean isEarly) {
        if (isSubmitted) return;
        this.isSubmitted = true;

        if (localTimer != null) {
            localTimer.stop();
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Grading...");

        String user = client.getCurrentUser() != null ? client.getCurrentUser().getUsername() : "student";
        ExamSubmitDTO submitDTO = new ExamSubmitDTO(roomCode, user, studentAnswers, isEarly);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private ExamResultDTO result;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    if (isMultiplayer) {
                        Packet p = Packet.ok(OpCodes.SUBMIT_EXAM_REQ, submitDTO);
                        Packet res = client.sendRequest(p, OpCodes.SUBMIT_EXAM_RES).get();
                        if (res != null && res.isSuccess()) {
                            result = res.getPayloadAs(ExamResultDTO.class);
                        }
                    } else {
                        // Solo practice
                        Packet p = Packet.ok(OpCodes.PRACTICE_SUBMIT_REQ, submitDTO);
                        Packet res = client.sendRequest(p, OpCodes.PRACTICE_RESULT_RES).get();
                        if (res != null && res.isSuccess()) {
                            result = res.getPayloadAs(ExamResultDTO.class);
                        }
                    }
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (result != null) {
                    showResultDialog(result);
                } else if (isMultiplayer) {
                    // In multiplayer, result might be broadcast when all participants finish
                    lblParticipantNotice.setText("Exam submitted! Waiting for other participants to finish...");
                } else {
                    JOptionPane.showMessageDialog(ExamScreen.this,
                            "Failed to get score: " + (error != null ? error.getMessage() : "Unknown error"));
                }
            }
        };
        worker.execute();
    }

    private void hostEndExam() {
        if (!isHost) return;
        int conf = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to terminate the test for ALL participants immediately?",
                "Host End Exam", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            client.sendPacket(Packet.ok(OpCodes.HOST_END_EXAM_REQ));
        }
    }

    private void showResultDialog(ExamResultDTO result) {
        client.removeListener(this);
        SwingUtilities.invokeLater(() -> {
            ExamResultDialog dialog = new ExamResultDialog(app, result, examTitle, isMultiplayer);
            dialog.setVisible(true);
        });
    }

    // ==========================================
    // SERVER PUSH EVENT HANDLERS
    // ==========================================

    @Override
    public void onExamTick(int serverSecondsRemaining) {
        // Synchronize local countdown clock with authoritative server clock
        this.remainingSeconds = serverSecondsRemaining;
        updateTimerDisplay();
    }

    @Override
    public void onParticipantSubmitted(String username, int submittedCount, int totalCount) {
        lblParticipantNotice.setText(String.format("%s submitted early (%d/%d submitted)", username, submittedCount, totalCount));
    }

    @Override
    public void onExamEnded(String reason) {
        if (!isSubmitted) {
            JOptionPane.showMessageDialog(this, "The exam was concluded by the host or server: " + reason);
            submitExam(false);
        }
    }

    @Override
    public void onExamResult(ExamResultDTO result) {
        showResultDialog(result);
    }

    @Override
    public void onConnectionLost(String reason) {
        if (localTimer != null) localTimer.stop();
        JOptionPane.showMessageDialog(this, "Network connection lost during exam: " + reason);
        app.showScreen("LOGIN");
    }
}
