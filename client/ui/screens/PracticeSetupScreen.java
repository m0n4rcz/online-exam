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
        gbc.insets = new Insets(12, 12, 12, 12);

        ModernCard card = new ModernCard(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(560, 520));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header
        JPanel pnlHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("PRACTICE MODE (CHẾ ĐỘ LUYỆN TẬP)", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Customize topic, difficulty, and duration for your solo study session", SwingConstants.CENTER);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        pnlHeader.add(lblTitle);
        pnlHeader.add(lblSub);
        card.add(pnlHeader, BorderLayout.NORTH);

        // Form Fields
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);

        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        fGbc.insets = new Insets(8, 6, 8, 6);
        fGbc.gridx = 0;
        fGbc.gridy = 0;
        fGbc.weightx = 1.0;

        // 1. Topic
        JLabel lblTopic = new JLabel("Question Topic / Chủ đề kiến thức:");
        lblTopic.setFont(Theme.FONT_BODY_BOLD);
        lblTopic.setForeground(Theme.TEXT_SECONDARY);
        pnlForm.add(lblTopic, fGbc);

        fGbc.gridy++;
        cmbTopic = new JComboBox<>(QuestionCategory.ALL_CATEGORIES);
        styleComboBox(cmbTopic);
        pnlForm.add(cmbTopic, fGbc);

        // 2. Difficulty
        fGbc.gridy++;
        JLabel lblDiff = new JLabel("Difficulty Level / Mức độ khó:");
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
        JLabel lblQ = new JLabel("Number of Questions:");
        lblQ.setFont(Theme.FONT_BODY_BOLD);
        lblQ.setForeground(Theme.TEXT_SECONDARY);
        spnQuestions = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        styleSpinner(spnQuestions);
        pnlQ.add(lblQ, BorderLayout.NORTH);
        pnlQ.add(spnQuestions, BorderLayout.CENTER);

        JPanel pnlD = new JPanel(new BorderLayout(0, 4));
        pnlD.setOpaque(false);
        JLabel lblD = new JLabel("Duration (Minutes):");
        lblD.setFont(Theme.FONT_BODY_BOLD);
        lblD.setForeground(Theme.TEXT_SECONDARY);
        spnDuration = new JSpinner(new SpinnerNumberModel(5, 1, 120, 1));
        styleSpinner(spnDuration);
        pnlD.add(lblD, BorderLayout.NORTH);
        pnlD.add(spnDuration, BorderLayout.CENTER);

        pnlNumbers.add(pnlQ);
        pnlNumbers.add(pnlD);
        pnlForm.add(pnlNumbers, fGbc);

        fGbc.gridy++;
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.DANGER);
        pnlForm.add(lblStatus, fGbc);

        card.add(pnlForm, BorderLayout.CENTER);

        // Action Buttons
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pnlActions.setOpaque(false);

        ModernButton btnBack = new ModernButton("Back to Home", ModernButton.Style.OUTLINE);
        btnBack.addActionListener(e -> app.showScreen("DASHBOARD"));

        btnStart = new ModernButton("Start Practice Test", ModernButton.Style.PRIMARY);
        btnStart.setPreferredSize(new Dimension(180, 40));
        btnStart.addActionListener(e -> startPracticeExam());

        pnlActions.add(btnBack);
        pnlActions.add(btnStart);
        card.add(pnlActions, BorderLayout.SOUTH);

        add(card, gbc);
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(Theme.FONT_BODY);
        box.setBackground(Theme.BG_INPUT);
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setPreferredSize(new Dimension(200, 36));
        box.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(Theme.FONT_BODY_BOLD);
        spinner.setPreferredSize(new Dimension(100, 36));
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
        lblStatus.setText("Requesting practice questions from server...");
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
                        throw new Exception(res != null ? res.getPayloadAsString() : "No response from server");
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
                    lblStatus.setText("Failed to start practice: " + error.getMessage());
                    return;
                }

                if (responseDTO != null && responseDTO.getQuestions() != null && !responseDTO.getQuestions().isEmpty()) {
                    lblStatus.setText("");
                    app.startPracticeExam(responseDTO);
                } else {
                    lblStatus.setForeground(Theme.WARNING);
                    lblStatus.setText("No questions matched the selected criteria in question bank.");
                }
            }
        };
        worker.execute();
    }
}
