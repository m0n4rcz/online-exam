package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Interactive Multiple-Choice Option Card for Examination Screens.
 * Replaces traditional radio buttons with large, comfortable, clickable cards
 * supporting hover transitions, selected states, and keyboard shortcut triggers.
 */
public class OptionCard extends JPanel {

    private final String optionKey; // "A", "B", "C", "D"
    private String optionText = "";
    private boolean isSelected = false;
    private boolean isHovered = false;
    private Runnable onSelectListener;

    private final JLabel lblBadge;
    private final JLabel lblText;

    public OptionCard(String optionKey) {
        this.optionKey = optionKey;

        setLayout(new BorderLayout(14, 0));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(500, 56));

        // 1. Left Option Letter Badge (A, B, C, D)
        lblBadge = new JLabel(optionKey, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color badgeBg;
                if (isSelected) {
                    badgeBg = Theme.PRIMARY;
                } else if (isHovered) {
                    badgeBg = Theme.BG_CARD_HOVER;
                } else {
                    badgeBg = Theme.BG_SURFACE;
                }

                g2.setColor(badgeBg);
                g2.fillOval(0, 0, getWidth(), getHeight());

                if (!isSelected) {
                    g2.setColor(isHovered ? Theme.PRIMARY : Theme.BORDER);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setPreferredSize(new Dimension(34, 34));
        lblBadge.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        lblBadge.setForeground(Theme.TEXT_PRIMARY);
        lblBadge.setOpaque(false);

        // 2. Right Option Text
        lblText = new JLabel("");
        lblText.setFont(Theme.FONT_BODY);
        lblText.setForeground(Theme.TEXT_PRIMARY);

        add(lblBadge, BorderLayout.WEST);
        add(lblText, BorderLayout.CENTER);

        // Mouse interactions
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
                lblBadge.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
                lblBadge.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                select();
            }
        });
    }

    public void setOptionText(String text) {
        this.optionText = text != null ? text : "";
        lblText.setText("<html><body style='width: 520px; font-family: Segoe UI; font-size: 11pt; color: #F8FAFC;'>" 
                + this.optionText + "</body></html>");
        repaint();
    }

    public String getOptionKey() {
        return optionKey;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
        lblBadge.repaint();
    }

    public void setOnSelectListener(Runnable listener) {
        this.onSelectListener = listener;
    }

    public void select() {
        if (!isSelected) {
            setSelected(true);
            if (onSelectListener != null) {
                onSelectListener.run();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        Color bg;
        if (isSelected) {
            bg = new Color(79, 70, 229, 38); // Subtle Indigo tint
        } else if (isHovered) {
            bg = Theme.BG_CARD_HOVER;
        } else {
            bg = Theme.BG_CARD;
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 12, 12);

        // Border
        if (isSelected) {
            g2.setColor(Theme.PRIMARY);
            g2.setStroke(new BasicStroke(2.0f));
        } else if (isHovered) {
            g2.setColor(Theme.PRIMARY_HOVER);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(1, 1, w - 2, h - 2, 12, 12);

        g2.dispose();
        super.paintComponent(g);
    }
}
