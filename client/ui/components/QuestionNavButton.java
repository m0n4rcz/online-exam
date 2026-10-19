package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Palette button representing a single exam question in the navigation grid.
 * Displays number, indicates answered state, flagged state, and highlights currently viewed question.
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
        if (isFlagged) {
            bg = Theme.FLAGGED;
        } else if (isAnswered) {
            bg = Theme.PRIMARY;
        } else {
            bg = Theme.BG_SURFACE;
        }

        g2.setColor(bg);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

        // Border highlighting current active question
        if (isCurrent) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        } else {
            g2.setColor(isAnswered || isFlagged ? bg.brighter() : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        }

        // Flag badge in top right corner if flagged
        if (isFlagged) {
            g2.setColor(Theme.WARNING);
            g2.fillOval(w - 11, 4, 7, 7);
        }

        g2.dispose();

        setForeground(isAnswered || isFlagged ? Color.WHITE : Theme.TEXT_SECONDARY);
        super.paintComponent(g);
    }
}
