package client.ui.screens;

import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import shared.dtos.ExamResultDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * High-impact modal dialog presenting exam score, rank, correct answers,
 * and detailed question-by-question explanation review.
 */
public class ExamResultDialog extends JDialog {

    public ExamResultDialog(ClientApp parent, ExamResultDTO result, String examTitle, boolean isMultiplayer) {
        super(parent, "Exam Results - " + examTitle, true);
        setSize(780, 680);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 16));

        initUI(parent, result, examTitle, isMultiplayer);
    }

    private void initUI(ClientApp app, ExamResultDTO result, String title, boolean isMultiplayer) {
        // 1. Top Summary Banner
        ModernCard bannerCard = new ModernCard(new BorderLayout(16, 0));
        bannerCard.setBorder(new EmptyBorder(16, 24, 16, 24));

        double score = result != null ? result.getScore() : 0.0;
        int correct = result != null ? result.getCorrectCount() : 0;
        int total = result != null ? result.getTotalQuestions() : 0;

        Color scoreColor = score >= 8.0 ? Theme.SUCCESS : (score >= 5.0 ? Theme.PRIMARY : Theme.DANGER);

        JPanel pnlScores = new JPanel(new GridLayout(1, isMultiplayer ? 3 : 2, 20, 0));
        pnlScores.setOpaque(false);

        // Score Card
        JPanel pnlS = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlS.setOpaque(false);
        JLabel lblS1 = new JLabel("FINAL SCORE (/10.0)");
        lblS1.setFont(Theme.FONT_BADGE);
        lblS1.setForeground(Theme.TEXT_MUTED);
        JLabel lblS2 = new JLabel(String.format("%.1f", score));
        lblS2.setFont(Theme.FONT_BIG_STAT);
        lblS2.setForeground(scoreColor);
        pnlS.add(lblS1);
        pnlS.add(lblS2);
        pnlScores.add(pnlS);

        // Correct count
        JPanel pnlC = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlC.setOpaque(false);
        JLabel lblC1 = new JLabel("CORRECT ANSWERS");
        lblC1.setFont(Theme.FONT_BADGE);
        lblC1.setForeground(Theme.TEXT_MUTED);
        JLabel lblC2 = new JLabel(String.format("%d / %d", correct, total));
        lblC2.setFont(Theme.FONT_BIG_STAT);
        lblC2.setForeground(Theme.TEXT_PRIMARY);
        pnlC.add(lblC1);
        pnlC.add(lblC2);
        pnlScores.add(pnlC);

        // Rank (Multiplayer only)
        if (isMultiplayer) {
            JPanel pnlR = new JPanel(new GridLayout(2, 1, 0, 2));
            pnlR.setOpaque(false);
            JLabel lblR1 = new JLabel("ROOM RANK");
            lblR1.setFont(Theme.FONT_BADGE);
            lblR1.setForeground(Theme.TEXT_MUTED);
            JLabel lblR2 = new JLabel("#" + (result != null ? result.getRank() : 1));
            lblR2.setFont(Theme.FONT_BIG_STAT);
            lblR2.setForeground(Theme.WARNING);
            pnlR.add(lblR1);
            pnlR.add(lblR2);
            pnlScores.add(pnlR);
        }

        bannerCard.add(pnlScores, BorderLayout.CENTER);
        add(bannerCard, BorderLayout.NORTH);

        // 2. Center: Question-by-Question Review List
        JPanel pnlReviewList = new JPanel();
        pnlReviewList.setLayout(new BoxLayout(pnlReviewList, BoxLayout.Y_AXIS));
        pnlReviewList.setBackground(Theme.BG_DARK);

        if (result != null && result.getReviews() != null) {
            for (ExamResultDTO.QuestionReview rev : result.getReviews()) {
                pnlReviewList.add(createQuestionReviewCard(rev));
                pnlReviewList.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scrollReview = new JScrollPane(pnlReviewList);
        scrollReview.getViewport().setBackground(Theme.BG_DARK);
        scrollReview.setBorder(new EmptyBorder(0, 20, 0, 20));
        scrollReview.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollReview, BorderLayout.CENTER);

        // 3. Bottom Close Button
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 14));
        pnlBottom.setOpaque(false);

        ModernButton btnDone = new ModernButton("Return to Dashboard", ModernButton.Style.PRIMARY);
        btnDone.setPreferredSize(new Dimension(200, 42));
        btnDone.addActionListener(e -> {
            dispose();
            app.showScreen("DASHBOARD");
        });

        pnlBottom.add(btnDone);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private JPanel createQuestionReviewCard(ExamResultDTO.QuestionReview rev) {
        ModernCard card = new ModernCard(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        boolean isCorrect = rev.isCorrect();
        Color statusColor = isCorrect ? Theme.SUCCESS : Theme.DANGER;
        String statusText = isCorrect ? "CORRECT" : "INCORRECT";

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);

        JLabel lblQNum = new JLabel(String.format("Question #%d", rev.getQuestionNumber()));
        lblQNum.setFont(Theme.FONT_BODY_BOLD);
        lblQNum.setForeground(Theme.TEXT_PRIMARY);

        JLabel badge = Theme.createBadge(statusText, statusColor, Color.WHITE);
        pnlTop.add(lblQNum, BorderLayout.WEST);
        pnlTop.add(badge, BorderLayout.EAST);
        card.add(pnlTop, BorderLayout.NORTH);

        // Question content
        JLabel lblContent = new JLabel("<html><body style='width: 580px;'>" + rev.getPrompt() + "</body></html>");
        lblContent.setFont(Theme.FONT_BODY);
        lblContent.setForeground(Theme.TEXT_SECONDARY);

        // Answer comparison
        String chosen = rev.getStudentChoice() != null ? rev.getStudentChoice() : "(Unanswered)";
        String correct = rev.getCorrectChoice();

        JPanel pnlAnswers = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlAnswers.setOpaque(false);

        JLabel lblAns = new JLabel(String.format("Your Answer: %s  |  Correct Answer: %s", chosen, correct));
        lblAns.setFont(Theme.FONT_BODY_BOLD);
        lblAns.setForeground(isCorrect ? Theme.SUCCESS : Theme.DANGER);

        String expl = rev.getExplanation() != null && !rev.getExplanation().isEmpty()
                ? "Explanation: " + rev.getExplanation()
                : "No explanation provided.";
        JLabel lblExpl = new JLabel("<html><body style='width: 580px; color: #94A3B8; font-style: italic;'>" + expl + "</body></html>");
        lblExpl.setFont(Theme.FONT_SMALL);

        pnlAnswers.add(lblAns);
        pnlAnswers.add(lblExpl);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 8));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblContent, BorderLayout.NORTH);
        pnlCenter.add(pnlAnswers, BorderLayout.CENTER);

        card.add(pnlCenter, BorderLayout.CENTER);
        return card;
    }
}
