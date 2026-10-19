package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import shared.OpCodes;
import shared.Packet;
import shared.QuestionCategory;
import shared.QuestionDifficulty;
import shared.dtos.PracticeRequestDTO;
import shared.dtos.PracticeResponseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Configuration screen for Solo Practice Mode.
 * Allows the student to select question topic, difficulty, quantity, and time limit.
 * Styled for HUST academic examination environment.
 */
public class PracticeSetupScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    private JComboBox<String> cmbTopic;
    private JComboBox<String> cmbDifficulty;
    private JSpinner spnQuestions;
    private JSpinner spnDuration;
    private JLabel lblStatus;
    private ModernButton btnStart;

    public PracticeSetupScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new GridBagLayout());
        setBackground(Theme.BG_DARK);

        initUI();
    }

    private void initUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 16, 16, 16);

        ModernCard card = new ModernCard(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(620, 580));
        card.setBorder(new EmptyBorder(26, 32, 26, 32));

        // Header
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);

        JPanel pnlBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlBadge.setOpaque(false);
        pnlBadge.add(Theme.createBadge("LUYỆN TẬP ĐỘC LẬP - HUST", Theme.HUST_RED, Color.WHITE));

        JLabel lblTitle = new JLabel("CẤU HÌNH BÀI THI TỰ LUYỆN", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Tự do lựa chọn chủ đề, cấp độ khó và thời gian để củng cố kiến thức", SwingConstants.CENTER);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlHeader.add(pnlBadge);
        pnlHeader.add(Box.createVerticalStrut(8));
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblSub);
        card.add(pnlHeader, BorderLayout.NORTH);

        // Form Fields
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setOpaque(false);

        // Quick Preset Chips
        JLabel lblPreset = new JLabel("Gợi ý cấu hình nhanh:");
        lblPreset.setFont(Theme.FONT_BADGE);
        lblPreset.setForeground(Theme.TEXT_GOLD);

        JPanel pnlPresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlPresets.setOpaque(false);

        JButton btnPre1 = createPresetChip("⚡ Ôn nhanh (5 câu / 5p)", 5, 5);
        JButton btnPre2 = createPresetChip("📝 Tiêu chuẩn (10 câu / 15p)", 10, 15);
        JButton btnPre3 = createPresetChip("🎯 Chuyên sâu (20 câu / 30p)", 20, 30);
        pnlPresets.add(btnPre1);
        pnlPresets.add(btnPre2);
        pnlPresets.add(btnPre3);

        pnlCenter.add(lblPreset);
        pnlCenter.add(Box.createVerticalStrut(6));
        pnlCenter.add(pnlPresets);
        pnlCenter.add(Box.createVerticalStrut(16));

        // Form Grid
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);

        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        fGbc.insets = new Insets(6, 4, 6, 4);
        fGbc.gridx = 0;
        fGbc.gridy = 0;
        fGbc.weightx = 1.0;

        // 1. Topic
        JLabel lblTopic = new JLabel("Chủ đề học phần:");
        lblTopic.setFont(Theme.FONT_BODY_BOLD);
        lblTopic.setForeground(Theme.TEXT_SECONDARY);
        pnlForm.add(lblTopic, fGbc);

        fGbc.gridy++;
        cmbTopic = new JComboBox<>(QuestionCategory.ALL_CATEGORIES);
        styleComboBox(cmbTopic);
        pnlForm.add(cmbTopic, fGbc);

        // 2. Difficulty
        fGbc.gridy++;
        JLabel lblDiff = new JLabel("Độ khó câu hỏi:");
        lblDiff.setFont(Theme.FONT_BODY_BOLD);
        lblDiff.setForeground(Theme.TEXT_SECONDARY);
        pnlForm.add(lblDiff, fGbc);

        fGbc.gridy++;
        String[] difficulties = {QuestionDifficulty.ALL, QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD};
        cmbDifficulty = new JComboBox<>(difficulties);
        styleComboBox(cmbDifficulty);
        pnlForm.add(cmbDifficulty, fGbc);

        // 3. Question Count & Duration (side-by-side)
        fGbc.gridy++;
        JPanel pnlNumbers = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlNumbers.setOpaque(false);

        JPanel pnlQ = new JPanel(new BorderLayout(0, 4));
        pnlQ.setOpaque(false);
        JLabel lblQ = new JLabel("Số lượng câu hỏi:");
        lblQ.setFont(Theme.FONT_BODY_BOLD);
        lblQ.setForeground(Theme.TEXT_SECONDARY);
        spnQuestions = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        styleSpinner(spnQuestions);
        pnlQ.add(lblQ, BorderLayout.NORTH);
        pnlQ.add(spnQuestions, BorderLayout.CENTER);

        JPanel pnlD = new JPanel(new BorderLayout(0, 4));
        pnlD.setOpaque(false);
        JLabel lblD = new JLabel("Thời gian làm bài (Phút):");
        lblD.setFont(Theme.FONT_BODY_BOLD);
        lblD.setForeground(Theme.TEXT_SECONDARY);
        spnDuration = new JSpinner(new SpinnerNumberModel(5, 1, 120, 1));
        styleSpinner(spnDuration);
        pnlD.add(lblD, BorderLayout.NORTH);
        pnlD.add(spnDuration, BorderLayout.CENTER);

        pnlNumbers.add(pnlQ);
        pnlNumbers.add(pnlD);
        pnlForm.add(pnlNumbers, fGbc);

        pnlCenter.add(pnlForm);
        card.add(pnlCenter, BorderLayout.CENTER);

        // Bottom status & Actions
        JPanel pnlBottom = new JPanel(new BorderLayout(0, 10));
        pnlBottom.setOpaque(false);

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.DANGER);
        pnlBottom.add(lblStatus, BorderLayout.NORTH);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Quay Lại", ModernButton.Style.OUTLINE);
        btnBack.setPreferredSize(new Dimension(130, 42));
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnStart = new ModernButton("Bắt Đầu Làm Bài", ModernButton.Style.HUST);
        btnStart.setPreferredSize(new Dimension(200, 42));
        btnStart.addActionListener(e -> startPracticeExam());

        pnlActions.add(btnBack);
        pnlActions.add(btnStart);
        pnlBottom.add(pnlActions, BorderLayout.SOUTH);

        card.add(pnlBottom, BorderLayout.SOUTH);
        add(card, gbc);
    }

    private JButton createPresetChip(String text, int questions, int minutes) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_SMALL);
        btn.setForeground(Theme.TEXT_GOLD);
        btn.setBackground(Theme.BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            spnQuestions.setValue(questions);
            spnDuration.setValue(minutes);
        });
        return btn;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(Theme.FONT_BODY);
        box.setBackground(Theme.BG_INPUT);
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setPreferredSize(new Dimension(200, 38));
        box.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(Theme.FONT_BODY_BOLD);
        spinner.setPreferredSize(new Dimension(100, 38));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(Theme.BG_INPUT);
            de.getTextField().setForeground(Theme.TEXT_PRIMARY);
        }
        spinner.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
    }

    private void startPracticeExam() {
        String topic = (String) cmbTopic.getSelectedItem();
        String difficulty = (String) cmbDifficulty.getSelectedItem();
        int count = (Integer) spnQuestions.getValue();
        int minutes = (Integer) spnDuration.getValue();

        lblStatus.setForeground(Theme.TEXT_SECONDARY);
        lblStatus.setText("Đang tải dữ liệu đề thi từ máy chủ HUST...");
        btnStart.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private PracticeResponseDTO responseDTO;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    PracticeRequestDTO req = new PracticeRequestDTO(topic, difficulty, count, minutes);
                    Packet p = Packet.ok(OpCodes.PRACTICE_REQ, req);
                    Packet res = client.sendRequest(p, OpCodes.PRACTICE_RES).get();
                    if (res != null && res.isSuccess()) {
                        responseDTO = res.getPayloadAs(PracticeResponseDTO.class);
                    } else {
                        throw new Exception(res != null ? res.getPayloadAsString() : "Không nhận được phản hồi từ máy chủ");
                    }
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                btnStart.setEnabled(true);
                if (error != null) {
                    lblStatus.setForeground(Theme.DANGER);
                    lblStatus.setText("Lỗi khởi tạo bài thi: " + error.getMessage());
                    return;
                }

                if (responseDTO != null && responseDTO.getQuestions() != null && !responseDTO.getQuestions().isEmpty()) {
                    lblStatus.setText("");
                    app.startPracticeExam(responseDTO);
                } else {
                    lblStatus.setForeground(Theme.WARNING);
                    lblStatus.setText("Không tìm thấy câu hỏi phù hợp với tiêu chí đã chọn trong ngân hàng đề.");
                }
            }
        };
        worker.execute();
    }
}
