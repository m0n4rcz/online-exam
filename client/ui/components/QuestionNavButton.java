package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Palette button representing a single exam question in the navigation grid.
 * Displays number, indicates answered state, and highlights currently viewed question.
 */
public class QuestionNavButton extends JButton {

    private final int questionIndex;
    private boolean isAnswered = false;
    private boolean isCurrent = false;

    public QuestionNavButton(int questionNumber, int questionIndex) {
        super(String.valueOf(questionNumber));
        this.questionIndex = questionIndex;

        setFont(Theme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(42, 42));
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

    public boolean isAnswered() {
        return isAnswered;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background color based on answered state
        Color bg = isAnswered ? Theme.PRIMARY : Theme.BG_SURFACE;
        g2.setColor(bg);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

        // Border highlighting current active question
        if (isCurrent) {
            g2.setColor(Theme.WARNING);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        } else {
            g2.setColor(isAnswered ? Theme.PRIMARY_HOVER : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        }

        g2.dispose();

        setForeground(isAnswered ? Color.WHITE : Theme.TEXT_SECONDARY);
        super.paintComponent(g);
    }
}
