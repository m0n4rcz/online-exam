package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Palette button representing a single exam question in the navigation grid.
 * Displays number, indicates answered state, flagged state, and highlights currently viewed question.
 * Styled with HUST Red & Academic Gold visual indicators.
 */
public class QuestionNavButton extends JButton {

    private final int questionIndex;
    private boolean isAnswered = false;
    private boolean isCurrent = false;
    private boolean isFlagged = false;

    public QuestionNavButton(int questionNumber, int questionIndex) {
        super(String.valueOf(questionNumber));
        this.questionIndex = questionIndex;

        setFont(Theme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(44, 44));
    }

    public int getQuestionIndex() {
        return questionIndex;
    }

    public void setAnswered(boolean answered) {
        this.isAnswered = answered;
        repaint();
    }

    public void setCurrent(boolean current) {
        this.isCurrent = current;
        repaint();
    }

    public void setFlagged(boolean flagged) {
        this.isFlagged = flagged;
        repaint();
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background color
        Color bg;
        Color fg = Theme.TEXT_SECONDARY;

        if (isFlagged) {
            bg = Theme.HUST_GOLD;
            fg = new Color(15, 23, 42); // Crisp dark text on gold button
        } else if (isAnswered) {
            bg = Theme.HUST_RED; // HUST Crimson for answered question
            fg = Color.WHITE;
        } else {
            bg = Theme.BG_CARD;
            fg = Theme.TEXT_MUTED;
        }

        g2.setColor(bg);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

        // Border highlighting current active question
        if (isCurrent) {
            g2.setColor(Theme.HUST_GOLD_HOVER);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        } else {
            g2.setColor(isAnswered || isFlagged ? bg.brighter() : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        }

        // Flag dot badge in top right corner if flagged
        if (isFlagged) {
            g2.setColor(new Color(15, 23, 42));
            g2.fillOval(w - 11, 4, 7, 7);
            g2.setColor(Color.WHITE);
            g2.fillOval(w - 10, 5, 5, 5);
        }

        g2.dispose();

        setForeground(fg);
        super.paintComponent(g);
    }
}
