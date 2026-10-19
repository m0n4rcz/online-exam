package client.ui.screens;

import client.net.NetworkClient;
import client.net.ServerEventListener;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.OptionCard;
import client.ui.components.QuestionNavButton;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Live Academic Exam Execution Screen for HUST Examination Platform.
 * Features distraction-free focus mode, interactive OptionCards,
 * full keyboard shortcut answering (A/B/C/D), synchronized server clock,
 * question flagging for review, and 2-step safe submission verification.
 */
public class ExamScreen extends JPanel implements ServerEventListener {

    private final ClientApp app;
    private final NetworkClient client;

    // Exam Mode & Data
    private boolean isMultiplayer = false;
    private boolean isHost = false;
    private String roomCode;
    private String examTitle = "Bài thi HUST";
    private List<QuestionDTO> questions = new ArrayList<>();
    private final Map<Integer, String> studentAnswers = new HashMap<>();
    private final Set<Integer> flaggedQuestions = new HashSet<>();

    // State
    private int currentQuestionIndex = 0;
    private int remainingSeconds = 300;
    private javax.swing.Timer localTimer;
    private boolean isSubmitted = false;

    // UI Components - Top Bar
    private JLabel lblTitle;
    private JLabel lblMeta;
    private JProgressBar progressBar;
    private JLabel lblTimer;
    private JLabel lblParticipantNotice;

    // Question Viewer
    private JLabel lblQuestionHeader;
    private JLabel lblDifficultyBadge;
    private JLabel lblQuestionContent;

    // Interactive Option Cards (A, B, C, D)
    private OptionCard optA;
    private OptionCard optB;
    private OptionCard optC;
    private OptionCard optD;

    // Navigation Palette & Action Buttons
    private JPanel pnlQuestionGrid;
    private final List<QuestionNavButton> navButtons = new ArrayList<>();
    private JLabel lblStatAnswered;
    private JLabel lblStatRemaining;
    private JLabel lblStatFlagged;

    private ModernButton btnPrev;
    private ModernButton btnNext;
    private ModernButton btnFlag;
    private ModernButton btnSubmit;
    private ModernButton btnHostEnd;
    private ModernButton btnExit;

    public ExamScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(16, 22, 16, 22));

        initUI();
        initKeyboardShortcuts();
    }

    private void initUI() {
        // 1. Top Bar: Title, Subject, Progress Bar, Timer, Host Controls, Exit Button
        ModernCard topCard = new ModernCard(new BorderLayout(16, 0));
        topCard.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel pnlTopLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        pnlTopLeft.setOpaque(false);

        btnExit = new ModernButton("← Thoát", ModernButton.Style.OUTLINE);
        btnExit.setPreferredSize(new Dimension(100, 38));
        btnExit.setToolTipText("Thoát khỏi bài thi / Rời phòng thi (Phím tắt: ESC)");
        btnExit.addActionListener(e -> confirmAndExitExam());

        JPanel pnlTitle = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTitle.setOpaque(false);

        lblTitle = new JLabel("HUST ONLINE EXAM");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        lblMeta = new JLabel("Đang tải dữ liệu bài thi...");
        lblMeta.setFont(Theme.FONT_SMALL);
        lblMeta.setForeground(Theme.TEXT_SECONDARY);

        pnlTitle.add(lblTitle);
        pnlTitle.add(lblMeta);

        pnlTopLeft.add(btnExit);
        pnlTopLeft.add(pnlTitle);
        topCard.add(pnlTopLeft, BorderLayout.WEST);

        // Center: Progress Bar & Multiplayer notice
        JPanel pnlCenterTop = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlCenterTop.setOpaque(false);
        pnlCenterTop.setBorder(new EmptyBorder(4, 30, 4, 30));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Tiến độ: 0% (0/0 câu)");
        progressBar.setFont(Theme.FONT_BADGE);
        progressBar.setForeground(Theme.HUST_RED);
        progressBar.setBackground(Theme.BG_INPUT);
        progressBar.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        progressBar.setPreferredSize(new Dimension(280, 20));

        lblParticipantNotice = new JLabel("", SwingConstants.CENTER);
        lblParticipantNotice.setFont(Theme.FONT_BADGE);
        lblParticipantNotice.setForeground(Theme.HUST_GOLD);

        pnlCenterTop.add(progressBar);
        pnlCenterTop.add(lblParticipantNotice);
        topCard.add(pnlCenterTop, BorderLayout.CENTER);

        // Right side: Host End Exam button + Digital Countdown Timer
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        pnlRight.setOpaque(false);

        btnHostEnd = new ModernButton("Kết thúc sớm (Host)", ModernButton.Style.DANGER);
        btnHostEnd.setPreferredSize(new Dimension(160, 38));
        btnHostEnd.setVisible(false);
        btnHostEnd.addActionListener(e -> hostEndExam());
        pnlRight.add(btnHostEnd);

        lblTimer = new JLabel("10:00", SwingConstants.CENTER);
        lblTimer.setFont(Theme.FONT_TIMER);
        lblTimer.setForeground(Theme.HUST_GOLD);
        lblTimer.setPreferredSize(new Dimension(110, 38));
        lblTimer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        pnlRight.add(lblTimer);

        topCard.add(pnlRight, BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        // 2. Center & Side Layout
        JPanel pnlCenter = new JPanel(new BorderLayout(16, 0));
        pnlCenter.setOpaque(false);

        // Main Question Card (Left / Center)
        ModernCard questionCard = new ModernCard(new BorderLayout(0, 14));
        questionCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel pnlQTop = new JPanel(new BorderLayout(0, 10));
        pnlQTop.setOpaque(false);

        JPanel pnlHeaderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlHeaderRow.setOpaque(false);

        lblQuestionHeader = new JLabel("Câu hỏi số 1");
        lblQuestionHeader.setFont(Theme.FONT_SUBTITLE);
        lblQuestionHeader.setForeground(Theme.HUST_GOLD);

        lblDifficultyBadge = Theme.createBadge("MỨC ĐỘ: TRUNG BÌNH", Theme.BG_SURFACE, Theme.TEXT_SECONDARY);

        pnlHeaderRow.add(lblQuestionHeader);
        pnlHeaderRow.add(lblDifficultyBadge);
        pnlQTop.add(pnlHeaderRow, BorderLayout.NORTH);

        lblQuestionContent = new JLabel("<html><body>Đang tải nội dung câu hỏi...</body></html>");
        lblQuestionContent.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 15));
        lblQuestionContent.setForeground(Theme.TEXT_PRIMARY);
        lblQuestionContent.setVerticalAlignment(SwingConstants.TOP);
        pnlQTop.add(lblQuestionContent, BorderLayout.CENTER);

        questionCard.add(pnlQTop, BorderLayout.NORTH);

        // 4 Interactive Option Cards (A, B, C, D)
        JPanel pnlOptions = new JPanel(new GridLayout(4, 1, 0, 10));
        pnlOptions.setOpaque(false);
        pnlOptions.setBorder(new EmptyBorder(8, 0, 8, 0));

        optA = new OptionCard("A");
        optB = new OptionCard("B");
        optC = new OptionCard("C");
        optD = new OptionCard("D");

        optA.setOnSelectListener(() -> selectAnswer("A"));
        optB.setOnSelectListener(() -> selectAnswer("B"));
        optC.setOnSelectListener(() -> selectAnswer("C"));
        optD.setOnSelectListener(() -> selectAnswer("D"));

        pnlOptions.add(optA);
        pnlOptions.add(optB);
        pnlOptions.add(optC);
        pnlOptions.add(optD);

        questionCard.add(pnlOptions, BorderLayout.CENTER);

        // Bottom Navigation Bar
        JPanel pnlNav = new JPanel(new BorderLayout());
        pnlNav.setOpaque(false);

        btnPrev = new ModernButton("← Câu trước (P)", ModernButton.Style.OUTLINE);
        btnPrev.setPreferredSize(new Dimension(160, 42));
        btnPrev.addActionListener(e -> navigateQuestion(currentQuestionIndex - 1));

        btnFlag = new ModernButton("⚑ Đánh dấu xem lại (F)", ModernButton.Style.OUTLINE);
        btnFlag.setPreferredSize(new Dimension(190, 42));
        btnFlag.addActionListener(e -> toggleFlagCurrentQuestion());

        btnNext = new ModernButton("Câu tiếp theo (N) →", ModernButton.Style.OUTLINE);
        btnNext.setPreferredSize(new Dimension(170, 42));
        btnNext.addActionListener(e -> navigateQuestion(currentQuestionIndex + 1));

        pnlNav.add(btnPrev, BorderLayout.WEST);
        pnlNav.add(btnFlag, BorderLayout.CENTER);
        pnlNav.add(btnNext, BorderLayout.EAST);
        questionCard.add(pnlNav, BorderLayout.SOUTH);

        pnlCenter.add(questionCard, BorderLayout.CENTER);

        // Right Sidebar: Question Palette Grid & Submit Button
        ModernCard sideCard = new ModernCard(new BorderLayout(0, 12));
        sideCard.setPreferredSize(new Dimension(280, 0));
        sideCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel pnlSideTop = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlSideTop.setOpaque(false);

        JLabel lblPalette = new JLabel("DANH SÁCH CÂU HỎI", SwingConstants.CENTER);
        lblPalette.setFont(Theme.FONT_HEADER);
        lblPalette.setForeground(Theme.TEXT_GOLD);

        JLabel lblShortcutHint = new JLabel("Phím tắt: A, B, C, D | ← → chuyển câu", SwingConstants.CENTER);
        lblShortcutHint.setFont(Theme.FONT_SMALL);
        lblShortcutHint.setForeground(Theme.TEXT_MUTED);

        pnlSideTop.add(lblPalette);
        pnlSideTop.add(lblShortcutHint);
        sideCard.add(pnlSideTop, BorderLayout.NORTH);

        pnlQuestionGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        pnlQuestionGrid.setOpaque(false);
        JScrollPane scrollGrid = new JScrollPane(pnlQuestionGrid);
        Theme.styleScrollPane(scrollGrid);
        sideCard.add(scrollGrid, BorderLayout.CENTER);

        // Palette Status Summary & Submit Button at Bottom
        JPanel pnlSideBottom = new JPanel(new BorderLayout(0, 10));
        pnlSideBottom.setOpaque(false);

        JPanel pnlStats = new JPanel(new GridLayout(3, 1, 0, 2));
        pnlStats.setOpaque(false);
        lblStatAnswered = new JLabel("● Đã trả lời: 0 câu");
        lblStatAnswered.setFont(Theme.FONT_SMALL);
        lblStatAnswered.setForeground(Theme.HUST_RED);

        lblStatRemaining = new JLabel("○ Chưa trả lời: 0 câu");
        lblStatRemaining.setFont(Theme.FONT_SMALL);
        lblStatRemaining.setForeground(Theme.TEXT_SECONDARY);

        lblStatFlagged = new JLabel("⚑ Cần xem lại: 0 câu");
        lblStatFlagged.setFont(Theme.FONT_SMALL);
        lblStatFlagged.setForeground(Theme.HUST_GOLD);

        pnlStats.add(lblStatAnswered);
        pnlStats.add(lblStatRemaining);
        pnlStats.add(lblStatFlagged);
        pnlSideBottom.add(pnlStats, BorderLayout.NORTH);

        btnSubmit = new ModernButton("Nộp bài thi ngay", ModernButton.Style.HUST);
        btnSubmit.setPreferredSize(new Dimension(240, 44));
        btnSubmit.addActionListener(e -> confirmAndSubmitExam());
        pnlSideBottom.add(btnSubmit, BorderLayout.SOUTH);

        sideCard.add(pnlSideBottom, BorderLayout.SOUTH);
        pnlCenter.add(sideCard, BorderLayout.EAST);

        add(pnlCenter, BorderLayout.CENTER);
    }

    /**
     * Binds keyboard shortcuts (A, B, C, D, Left, Right, F) for lightning-fast academic test taking.
     */
    private void initKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "ANSWER_A");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "ANSWER_A");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, 0), "ANSWER_A");
        am.put("ANSWER_A", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { optA.select(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "ANSWER_B");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "ANSWER_B");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, 0), "ANSWER_B");
        am.put("ANSWER_B", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { optB.select(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "ANSWER_C");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "ANSWER_C");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD3, 0), "ANSWER_C");
        am.put("ANSWER_C", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { optC.select(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "ANSWER_D");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0), "ANSWER_D");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, 0), "ANSWER_D");
        am.put("ANSWER_D", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { optD.select(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "NAV_PREV");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "NAV_PREV");
        am.put("NAV_PREV", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigateQuestion(currentQuestionIndex - 1); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "NAV_NEXT");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "NAV_NEXT");
        am.put("NAV_NEXT", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigateQuestion(currentQuestionIndex + 1); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "FLAG_QUESTION");
        am.put("FLAG_QUESTION", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { toggleFlagCurrentQuestion(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EXIT_EXAM");
        am.put("EXIT_EXAM", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { confirmAndExitExam(); }
        });
    }

    private void selectAnswer(String optionKey) {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            QuestionDTO q = questions.get(currentQuestionIndex);
            studentAnswers.put(q.getId(), optionKey);

            // Update visual states of option cards
            optA.setSelected("A".equalsIgnoreCase(optionKey));
            optB.setSelected("B".equalsIgnoreCase(optionKey));
            optC.setSelected("C".equalsIgnoreCase(optionKey));
            optD.setSelected("D".equalsIgnoreCase(optionKey));

            // Update question palette
            if (currentQuestionIndex < navButtons.size()) {
                navButtons.get(currentQuestionIndex).setAnswered(true);
            }

            updateProgressAndSummary();

            // If multiplayer, immediately synchronize answer to server
            if (isMultiplayer && roomCode != null) {
                client.sendPacket(Packet.ok(OpCodes.SUBMIT_ANSWER_REQ,
                        new AnswerSubmissionDTO(roomCode, q.getId(), optionKey)));
            }
        }
    }

    private void toggleFlagCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            QuestionDTO q = questions.get(currentQuestionIndex);
            if (flaggedQuestions.contains(q.getId())) {
                flaggedQuestions.remove(q.getId());
                btnFlag.setText("⚑ Đánh dấu xem lại (F)");
                btnFlag.setForeground(Theme.TEXT_PRIMARY);
            } else {
                flaggedQuestions.add(q.getId());
                btnFlag.setText("★ Đã đánh dấu xem lại");
                btnFlag.setForeground(Theme.WARNING);
            }

            if (currentQuestionIndex < navButtons.size()) {
                navButtons.get(currentQuestionIndex).setFlagged(flaggedQuestions.contains(q.getId()));
            }
            updateProgressAndSummary();
        }
    }

    private void updateProgressAndSummary() {
        int total = questions.size();
        int answered = studentAnswers.size();
        int remaining = Math.max(0, total - answered);
        int flagged = flaggedQuestions.size();

        int percent = total > 0 ? (answered * 100) / total : 0;
        progressBar.setValue(percent);
        progressBar.setString(String.format("Tiến độ: %d%% (%d/%d câu)", percent, answered, total));

        lblStatAnswered.setText(String.format("● Đã trả lời: %d câu", answered));
        lblStatRemaining.setText(String.format("○ Chưa trả lời: %d câu", remaining));
        lblStatFlagged.setText(String.format("⚑ Cần xem lại: %d câu", flagged));
    }

    // ==========================================
    // EXAM INITIALIZATION & LIFECYCLE
    // ==========================================

    public void startPracticeExam(PracticeResponseDTO practiceData) {
        this.isMultiplayer = false;
        this.isHost = false;
        this.roomCode = "PRACTICE";
        this.examTitle = "Luyện tập: " + practiceData.getTopic();
        this.questions = practiceData.getQuestions() != null ? practiceData.getQuestions() : new ArrayList<>();
        this.remainingSeconds = practiceData.getDurationSeconds();
        this.studentAnswers.clear();
        this.flaggedQuestions.clear();
        this.isSubmitted = false;

        btnExit.setText("← Thoát");
        btnExit.setToolTipText("Thoát khỏi bài luyện tập và trở về Bảng điều khiển (ESC)");
        btnHostEnd.setVisible(false);
        lblTitle.setText(examTitle);
        lblParticipantNotice.setText("CHẾ ĐỘ TỰ LUYỆN TẬP (SOLO PRACTICE)");

        setupExamSession();
    }

    public void startMultiplayerExam(String roomCode, RoomInfoDTO info, ExamInitDataDTO examData, boolean isHost) {
        this.isMultiplayer = true;
        this.isHost = isHost;
        this.roomCode = roomCode;
        this.examTitle = String.format("Phòng %s - %s", roomCode, info != null ? info.getTitle() : "Thi đấu");
        this.questions = examData.getQuestions() != null ? examData.getQuestions() : new ArrayList<>();
        this.remainingSeconds = examData.getDurationSeconds();
        this.studentAnswers.clear();
        this.flaggedQuestions.clear();
        this.isSubmitted = false;

        btnExit.setText("← Rời phòng");
        btnExit.setToolTipText("Rời khỏi phòng thi đấu trực tuyến (ESC)");
        btnHostEnd.setVisible(isHost);
        lblTitle.setText(examTitle);
        lblParticipantNotice.setText("PHÒNG THI ĐẤU TRỰC TUYẾN - MÁY CHỦ HUST");

        client.addListener(this);
        setupExamSession();
    }

    private void setupExamSession() {
        btnExit.setEnabled(true);
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Nộp bài thi ngay");

        // Build question palette buttons
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

        updateProgressAndSummary();
        loadQuestion(0);
        startTimer();
    }

    private void startTimer() {
        if (localTimer != null) localTimer.stop();
        updateTimerDisplay();

        localTimer = new javax.swing.Timer(1000, e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimerDisplay();
            } else {
                localTimer.stop();
                lblTimer.setText("00:00");
                lblTimer.setForeground(Theme.DANGER);
                if (!isSubmitted) {
                    JOptionPane.showMessageDialog(this,
                            "Thời gian làm bài đã kết thúc! Hệ thống đang tự động nộp bài của bạn.",
                            "Hết giờ làm bài", JOptionPane.INFORMATION_MESSAGE);
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
            lblTimer.setForeground(Theme.HUST_RED_HOVER);
        } else if (remainingSeconds < 180) {
            lblTimer.setForeground(Theme.HUST_GOLD_DARK);
        } else {
            lblTimer.setForeground(Theme.HUST_GOLD);
        }
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        this.currentQuestionIndex = index;
        QuestionDTO q = questions.get(index);

        lblQuestionHeader.setText(String.format("Câu hỏi số %d / %d", index + 1, questions.size()));
        lblDifficultyBadge.setText(String.format("CHỦ ĐỀ: %s • MỨC ĐỘ: %s", q.getCategory(), q.getDifficulty()));
        lblMeta.setText(String.format("Chuyên đề: %s | Độ khó: %s", q.getCategory(), q.getDifficulty()));

        lblQuestionContent.setText("<html><body style='width: 580px; font-family: Segoe UI; font-size: 11pt; color: #FFFFFF; line-height: 1.5;'>" 
                + q.getContent() + "</body></html>");

        optA.setOptionText(q.getOptionA());
        optB.setOptionText(q.getOptionB());
        optC.setOptionText(q.getOptionC());
        optD.setOptionText(q.getOptionD());

        // Restore previous answer if any
        String selected = studentAnswers.get(q.getId());
        optA.setSelected("A".equalsIgnoreCase(selected));
        optB.setSelected("B".equalsIgnoreCase(selected));
        optC.setSelected("C".equalsIgnoreCase(selected));
        optD.setSelected("D".equalsIgnoreCase(selected));

        // Update Flag button state
        boolean isFlagged = flaggedQuestions.contains(q.getId());
        btnFlag.setText(isFlagged ? "★ Đã gắn cờ xem lại (F)" : "⚑ Đánh dấu xem lại (F)");
        btnFlag.setStyle(isFlagged ? ModernButton.Style.HUST_GOLD : ModernButton.Style.OUTLINE);

        // Update palette buttons
        for (int i = 0; i < navButtons.size(); i++) {
            QuestionNavButton btn = navButtons.get(i);
            btn.setCurrent(i == index);
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
    // EXAM SUBMISSION (2-STEP SAFE CONFIRMATION)
    // ==========================================

    private void confirmAndSubmitExam() {
        if (isSubmitted) return;

        int answeredCount = studentAnswers.size();
        int total = questions.size();
        int remaining = total - answeredCount;
        int flagged = flaggedQuestions.size();

        StringBuilder msg = new StringBuilder();
        msg.append("BÁO CÁO TIẾN ĐỘ BÀI THI CỦA BẠN:\n\n");
        msg.append("● Số câu đã trả lời: ").append(answeredCount).append(" / ").append(total).append("\n");
        if (remaining > 0) {
            msg.append("⚠ Số câu CHƯA trả lời: ").append(remaining).append(" câu!\n");
        }
        if (flagged > 0) {
            msg.append("⚑ Số câu đang đánh dấu xem lại: ").append(flagged).append(" câu.\n");
        }
        msg.append("\nBạn có chắc chắn muốn nộp bài thi ngay bây giờ không?");

        int confirm = JOptionPane.showConfirmDialog(this, msg.toString(),
                "Xác nhận Nộp bài thi", JOptionPane.YES_NO_OPTION,
                remaining > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

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
        btnSubmit.setText("Đang chấm điểm...");

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
                    lblParticipantNotice.setText("Đã gửi bài thi! Đang chờ các thí sinh khác hoàn thành...");
                } else {
                    JOptionPane.showMessageDialog(ExamScreen.this,
                            "Lỗi khi nhận kết quả chấm: " + (error != null ? error.getMessage() : "Lỗi không xác định"));
                }
            }
        };
        worker.execute();
    }

    private void hostEndExam() {
        if (!isHost) return;
        int conf = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn kết thúc bài thi ngay lập tức cho TẤT CẢ thí sinh trong phòng?",
                "Kết thúc phòng thi (Host)", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            client.sendPacket(Packet.ok(OpCodes.HOST_END_EXAM_REQ));
        }
    }

    // ==========================================
    // SAFE EXAM EXIT & CANCELLATION (QoL)
    // ==========================================

    public void confirmAndExitExam() {
        if (isSubmitted) {
            exitExam();
            return;
        }

        String msg;
        String title;
        if (isMultiplayer) {
            msg = "CẢNH BÁO: Bạn đang tham gia phòng thi đấu trực tuyến (" + roomCode + ")!\n\n"
                + "● Nếu bạn rời phòng thi lúc này, bạn sẽ bị tính là bỏ cuộc hoặc nộp bài sớm.\n"
                + "● Điểm số hiện tại của bạn sẽ được chốt và bạn không thể tiếp tục thi phòng này.\n\n"
                + "Bạn có chắc chắn muốn rời phòng thi không?";
            title = "Xác nhận rời phòng thi";
        } else {
            msg = "BẠN ĐANG TRONG PHIÊN TỰ LUYỆN TẬP:\n\n"
                + "● Tiến độ hiện tại: " + studentAnswers.size() + "/" + questions.size() + " câu đã trả lời.\n"
                + "● Nếu thoát ngay bây giờ, kết quả bài làm này sẽ không được lưu vào lịch sử cá nhân.\n\n"
                + "Bạn có chắc chắn muốn hủy và quay về Bảng điều khiển không?";
            title = "Xác nhận thoát bài luyện tập";
        }

        int choice = JOptionPane.showConfirmDialog(this, msg, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            exitExam();
        }
    }

    private void exitExam() {
        if (localTimer != null) {
            localTimer.stop();
        }
        client.removeListener(this);

        if (isMultiplayer && roomCode != null && !"PRACTICE".equalsIgnoreCase(roomCode)) {
            // Notify server that student is leaving the room
            try {
                client.sendPacket(Packet.ok(OpCodes.LEAVE_ROOM_REQ));
                client.setCurrentRoomCode(null);
            } catch (Exception ignored) {}
        }

        app.showScreen("DASHBOARD");
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
        this.remainingSeconds = serverSecondsRemaining;
        updateTimerDisplay();
    }

    @Override
    public void onParticipantSubmitted(String username, int submittedCount, int totalCount) {
        lblParticipantNotice.setText(String.format("%s đã nộp bài sớm (%d/%d thí sinh đã nộp)", username, submittedCount, totalCount));
    }

    @Override
    public void onExamEnded(String reason) {
        if (!isSubmitted) {
            JOptionPane.showMessageDialog(this, "Bài thi đã được kết thúc bởi giám thị hoặc máy chủ: " + reason);
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
        JOptionPane.showMessageDialog(this, "Mất kết nối mạng trong lúc làm bài: " + reason);
        app.showScreen("LOGIN");
    }
}
