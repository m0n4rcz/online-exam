package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern stylized button with smooth hover animation, rounded corners,
 * and high-contrast typography.
 */
public class ModernButton extends JButton {

    public enum Style {
        PRIMARY,
        SECONDARY,
        SUCCESS,
        DANGER,
        OUTLINE
    }

    private final Style style;
    private boolean isHovered = false;
    private int cornerRadius = 10;

    public ModernButton(String text) {
        this(text, Style.PRIMARY);
    }

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style;
        init();
    }

    private void init() {
        setFont(Theme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(140, 38));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color bg;
        Color fg = Theme.TEXT_PRIMARY;

        if (!isEnabled()) {
            bg = Theme.BORDER;
            fg = Theme.TEXT_MUTED;
        } else {
            bg = switch (style) {
                case PRIMARY -> isHovered ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
                case SECONDARY -> isHovered ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
                case SUCCESS -> isHovered ? Theme.SUCCESS_HOVER : Theme.SUCCESS;
                case DANGER -> isHovered ? Theme.DANGER_HOVER : Theme.DANGER;
                case OUTLINE -> isHovered ? Theme.BG_CARD_HOVER : Theme.BG_CARD;
            };
        }

        // Background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Border for outline style
        if (style == Style.OUTLINE && isEnabled()) {
            g2.setColor(isHovered ? Theme.PRIMARY : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);
        }

        g2.dispose();

        setForeground(fg);
        super.paintComponent(g);
    }
}
