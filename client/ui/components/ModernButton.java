package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern stylized button with smooth hover feedback, rounded corners,
 * and high-contrast HUST Red & Academic Gold aesthetic.
 */
public class ModernButton extends JButton {

    public enum Style {
        PRIMARY,      // HUST Crimson Red
        SECONDARY,    // Clean Slate with Gold accent
        HUST,         // HUST Crimson Red
        HUST_GOLD,    // HUST Academic Gold
        SUCCESS,      // Emerald Green
        WARNING,      // Amber Warning
        DANGER,       // Rose Red
        OUTLINE       // Sleek subtle border
    }

    private Style style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 10;

    public ModernButton(String text) {
        this(text, Style.PRIMARY);
    }

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style;
        init();
    }

    public void setStyle(Style style) {
        this.style = style;
        repaint();
    }

    public Style getStyle() {
        return this.style;
    }

    private void init() {
        setFont(Theme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(140, 40));

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
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = false;
                    repaint();
                }
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
                case PRIMARY, HUST -> isPressed ? Theme.HUST_RED_DARK : (isHovered ? Theme.HUST_RED_HOVER : Theme.HUST_RED);
                case HUST_GOLD -> {
                    fg = new Color(15, 23, 42); // Crisp dark contrast on gold
                    yield isPressed ? Theme.HUST_GOLD_DARK : (isHovered ? Theme.HUST_GOLD_HOVER : Theme.HUST_GOLD);
                }
                case SECONDARY -> {
                    fg = isHovered ? Theme.HUST_GOLD : Theme.TEXT_PRIMARY;
                    yield isHovered ? Theme.BG_CARD_HOVER : Theme.BG_SURFACE;
                }
                case SUCCESS -> isPressed ? Theme.SUCCESS_HOVER.darker() : (isHovered ? Theme.SUCCESS_HOVER : Theme.SUCCESS);
                case WARNING -> isHovered ? Theme.HUST_GOLD_HOVER : Theme.HUST_GOLD;
                case DANGER -> isPressed ? Theme.DANGER_HOVER.darker() : (isHovered ? Theme.DANGER_HOVER : Theme.DANGER);
                case OUTLINE -> {
                    fg = isHovered ? Theme.HUST_GOLD : Theme.TEXT_PRIMARY;
                    yield isHovered ? new Color(245, 158, 11, 20) : Theme.BG_SURFACE;
                }
            };
        }

        // Draw pill background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Border for outline or secondary styles
        if (style == Style.OUTLINE && isEnabled()) {
            g2.setColor(isHovered ? Theme.HUST_GOLD : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);
        } else if (style == Style.SECONDARY && isEnabled()) {
            g2.setColor(isHovered ? Theme.HUST_GOLD : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);
        }

        g2.dispose();

        setForeground(fg);
        super.paintComponent(g);
    }
}
