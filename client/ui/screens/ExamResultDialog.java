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
 * Styled with HUST Red & Academic Gold visual identity.
 */
public class ExamResultDialog extends JDialog {

    public ExamResultDialog(ClientApp parent, ExamResultDTO result, String examTitle, boolean isMultiplayer) {
        super(parent, "Kết Quả Đánh Giá Năng Lực - " + examTitle, true);
        setSize(820, 720);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 16));

        initUI(parent, result, examTitle, isMultiplayer);
    }

    private void initUI(ClientApp app, ExamResultDTO result, String title, boolean isMultiplayer) {
        // 1. Top Summary Banner
        ModernCard bannerCard = new ModernCard(new BorderLayout(0, 14));
        bannerCard.setBorder(new EmptyBorder(18, 24, 18, 24));

        double score = result != null ? result.getScore() : 0.0;
        int correct = result != null ? result.getCorrectCount() : 0;
        int total = result != null ? result.getTotalQuestions() : 0;
        double percent = total > 0 ? (correct * 100.0 / total) : 0.0;

        Color scoreColor = score >= 8.5 ? Theme.HUST_GOLD_HOVER : (score >= 7.0 ? Theme.HUST_GOLD : (score >= 5.0 ? Theme.SUCCESS : Theme.HUST_RED));
        String scoreRemark = score >= 8.5 ? "Xuất sắc / Giỏi" : (score >= 7.0 ? "Khá" : (score >= 5.0 ? "Đạt chuẩn" : "Chưa đạt"));

        // Header line
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblHeaderTitle = new JLabel("BÁO CÁO KẾT QUẢ BÀI THI CHÍNH THỨC - HUST");
        lblHeaderTitle.setFont(Theme.FONT_BADGE);
        lblHeaderTitle.setForeground(Theme.TEXT_GOLD);

        JLabel lblTag = Theme.createBadge(isMultiplayer ? "PHÒNG THI ĐẤU" : "BÀI LUYỆN TẬP", Theme.HUST_RED, Color.WHITE);
        pnlHeader.add(lblHeaderTitle, BorderLayout.WEST);
        pnlHeader.add(lblTag, BorderLayout.EAST);
        bannerCard.add(pnlHeader, BorderLayout.NORTH);

        // Stats Row
        JPanel pnlScores = new JPanel(new GridLayout(1, isMultiplayer ? 3 : 2, 24, 0));
        pnlScores.setOpaque(false);

        // Score Card
        JPanel pnlS = new JPanel();
        pnlS.setLayout(new BoxLayout(pnlS, BoxLayout.Y_AXIS));
        pnlS.setOpaque(false);
        JLabel lblS1 = new JLabel("ĐIỂM SỐ (THANG 10)");
        lblS1.setFont(Theme.FONT_BADGE);
        lblS1.setForeground(Theme.TEXT_MUTED);
        JLabel lblS2 = new JLabel(String.format("%.1f", score));
        lblS2.setFont(Theme.FONT_BIG_STAT);
        lblS2.setForeground(scoreColor);
        JLabel lblS3 = new JLabel("Xếp loại: " + scoreRemark);
        lblS3.setFont(Theme.FONT_SMALL);
        lblS3.setForeground(scoreColor);
        pnlS.add(lblS1);
        pnlS.add(Box.createVerticalStrut(2));
        pnlS.add(lblS2);
        pnlS.add(Box.createVerticalStrut(2));
        pnlS.add(lblS3);
        pnlScores.add(pnlS);

        // Correct count
        JPanel pnlC = new JPanel();
        pnlC.setLayout(new BoxLayout(pnlC, BoxLayout.Y_AXIS));
        pnlC.setOpaque(false);
        JLabel lblC1 = new JLabel("SỐ CÂU ĐÚNG");
        lblC1.setFont(Theme.FONT_BADGE);
        lblC1.setForeground(Theme.TEXT_MUTED);
        JLabel lblC2 = new JLabel(String.format("%d / %d", correct, total));
        lblC2.setFont(Theme.FONT_BIG_STAT);
        lblC2.setForeground(Theme.TEXT_PRIMARY);
        JLabel lblC3 = new JLabel(String.format("Tỉ lệ chính xác: %.1f%%", percent));
        lblC3.setFont(Theme.FONT_SMALL);
        lblC3.setForeground(Theme.TEXT_SECONDARY);
        pnlC.add(lblC1);
        pnlC.add(Box.createVerticalStrut(2));
        pnlC.add(lblC2);
        pnlC.add(Box.createVerticalStrut(2));
        pnlC.add(lblC3);
        pnlScores.add(pnlC);

        // Rank (Multiplayer only)
        if (isMultiplayer) {
            JPanel pnlR = new JPanel();
            pnlR.setLayout(new BoxLayout(pnlR, BoxLayout.Y_AXIS));
            pnlR.setOpaque(false);
            JLabel lblR1 = new JLabel("THỨ HẠNG PHÒNG");
            lblR1.setFont(Theme.FONT_BADGE);
            lblR1.setForeground(Theme.TEXT_MUTED);
            int rank = result != null ? result.getRank() : 1;
            JLabel lblR2 = new JLabel("#" + rank);
            lblR2.setFont(Theme.FONT_BIG_STAT);
            lblR2.setForeground(rank == 1 ? Theme.HUST_GOLD : (rank <= 3 ? Theme.HUST_GOLD_HOVER : Theme.TEXT_PRIMARY));
            JLabel lblR3 = new JLabel(rank == 1 ? "Hạng nhất toàn phòng 🥇" : "Top thí sinh");
            lblR3.setFont(Theme.FONT_SMALL);
            lblR3.setForeground(Theme.TEXT_MUTED);
            pnlR.add(lblR1);
            pnlR.add(Box.createVerticalStrut(2));
            pnlR.add(lblR2);
            pnlR.add(Box.createVerticalStrut(2));
            pnlR.add(lblR3);
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
                pnlReviewList.add(Box.createVerticalStrut(12));
            }
        }

        JScrollPane scrollReview = new JScrollPane(pnlReviewList);
        scrollReview.getViewport().setBackground(Theme.BG_DARK);
        scrollReview.setBorder(new EmptyBorder(0, 24, 0, 24));
        scrollReview.getVerticalScrollBar().setUnitIncrement(16);
        Theme.styleScrollPane(scrollReview);
        add(scrollReview, BorderLayout.CENTER);

        // 3. Bottom Close Button
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 14));
        pnlBottom.setOpaque(false);

        ModernButton btnDone = new ModernButton("Về Bảng Điều Khiển", ModernButton.Style.HUST);
        btnDone.setPreferredSize(new Dimension(220, 42));
        btnDone.addActionListener(e -> {
            dispose();
            app.showScreen("DASHBOARD");
        });

        pnlBottom.add(btnDone);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private JPanel createQuestionReviewCard(ExamResultDTO.QuestionReview rev) {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        boolean isCorrect = rev.isCorrect();
        boolean isUnanswered = rev.getStudentChoice() == null || rev.getStudentChoice().trim().isEmpty();

        Color statusColor;
        String statusText;
        if (isCorrect) {
            statusColor = Theme.SUCCESS;
            statusText = "CHÍNH XÁC";
        } else if (isUnanswered) {
            statusColor = Theme.HUST_GOLD;
            statusText = "CHƯA TRẢ LỜI";
        } else {
            statusColor = Theme.HUST_RED;
            statusText = "CHƯA ĐÚNG";
        }

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);

        JLabel lblQNum = new JLabel(String.format("Câu hỏi #%d", rev.getQuestionNumber()));
        lblQNum.setFont(Theme.FONT_BODY_BOLD);
        lblQNum.setForeground(Theme.TEXT_PRIMARY);

        JLabel badge = Theme.createBadge(statusText, statusColor, Color.WHITE);
        pnlTop.add(lblQNum, BorderLayout.WEST);
        pnlTop.add(badge, BorderLayout.EAST);
        card.add(pnlTop, BorderLayout.NORTH);

        // Question content
        JLabel lblContent = new JLabel("<html><body style='width: 680px; line-height: 1.4; color: #FFFFFF; font-size: 11pt;'>" + rev.getPrompt() + "</body></html>");
        lblContent.setFont(Theme.FONT_BODY);

        // Answer comparison
        String chosen = !isUnanswered ? rev.getStudentChoice() : "(Bỏ trống)";
        String correct = rev.getCorrectChoice();

        JPanel pnlAnswers = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlAnswers.setOpaque(false);

        JPanel pnlYourAns = new JPanel(new BorderLayout(6, 0));
        pnlYourAns.setOpaque(false);
        JLabel lblYourTitle = new JLabel("Đáp án của bạn: ");
        lblYourTitle.setFont(Theme.FONT_SMALL);
        lblYourTitle.setForeground(Theme.TEXT_MUTED);
        JLabel lblYourVal = new JLabel(chosen);
        lblYourVal.setFont(Theme.FONT_BODY_BOLD);
        lblYourVal.setForeground(isCorrect ? Theme.SUCCESS : (isUnanswered ? Theme.HUST_GOLD : Theme.HUST_RED));
        pnlYourAns.add(lblYourTitle, BorderLayout.WEST);
        pnlYourAns.add(lblYourVal, BorderLayout.CENTER);

        JPanel pnlCorrectAns = new JPanel(new BorderLayout(6, 0));
        pnlCorrectAns.setOpaque(false);
        JLabel lblCorTitle = new JLabel("Đáp án chuẩn: ");
        lblCorTitle.setFont(Theme.FONT_SMALL);
        lblCorTitle.setForeground(Theme.TEXT_MUTED);
        JLabel lblCorVal = new JLabel(correct);
        lblCorVal.setFont(Theme.FONT_BODY_BOLD);
        lblCorVal.setForeground(Theme.SUCCESS);
        pnlCorrectAns.add(lblCorTitle, BorderLayout.WEST);
        pnlCorrectAns.add(lblCorVal, BorderLayout.CENTER);

        pnlAnswers.add(pnlYourAns);
        pnlAnswers.add(pnlCorrectAns);

        // Explanation callout with HUST Gold accent border
        String expl = rev.getExplanation() != null && !rev.getExplanation().trim().isEmpty()
                ? rev.getExplanation()
                : "Không có lời giải thích kèm theo.";

        JPanel pnlExpl = new JPanel(new BorderLayout());
        pnlExpl.setBackground(Theme.BG_INPUT);
        pnlExpl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.HUST_GOLD),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblExpl = new JLabel("<html><body style='width: 650px; line-height: 1.35; color: #D1D5DB; font-size: 11px;'>"
                + "<strong style='color: #FBBF24;'>Lời giải chi tiết:</strong> " + expl + "</body></html>");
        pnlExpl.add(lblExpl, BorderLayout.CENTER);

        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblContent);
        pnlCenter.add(Box.createVerticalStrut(10));
        pnlCenter.add(pnlAnswers);
        pnlCenter.add(Box.createVerticalStrut(10));
        pnlCenter.add(pnlExpl);

        card.add(pnlCenter, BorderLayout.CENTER);
        return card;
    }
}
