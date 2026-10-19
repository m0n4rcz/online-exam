package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Interactive Multiple-Choice Option Card for Examination Screens.
 * Styled with HUST Crimson Red and Academic Gold identity for clean,
 * distraction-free, and high-accessibility student exams.
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

        setLayout(new BorderLayout(16, 0));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 18, 12, 18));
        setPreferredSize(new Dimension(520, 58));

        // 1. Left Option Letter Badge (A, B, C, D)
        lblBadge = new JLabel(optionKey, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color badgeBg;
                Color badgeBorder;

                if (isSelected) {
                    badgeBg = Theme.HUST_GOLD;
                    badgeBorder = Theme.HUST_GOLD_HOVER;
                } else if (isHovered) {
                    badgeBg = Theme.BG_CARD_HOVER;
                    badgeBorder = Theme.HUST_GOLD;
                } else {
                    badgeBg = Theme.BG_INPUT;
                    badgeBorder = Theme.BORDER;
                }

                g2.setColor(badgeBg);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(badgeBorder);
                g2.setStroke(new BasicStroke(isSelected ? 2.0f : 1.2f));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setPreferredSize(new Dimension(36, 36));
        lblBadge.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 15));
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
                updateBadgeForeground();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                updateBadgeForeground();
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                select();
            }
        });

        updateBadgeForeground();
    }

    private void updateBadgeForeground() {
        if (isSelected) {
            lblBadge.setForeground(new Color(15, 23, 42)); // High contrast dark text on gold badge
        } else if (isHovered) {
            lblBadge.setForeground(Theme.HUST_GOLD);
        } else {
            lblBadge.setForeground(Theme.TEXT_PRIMARY);
        }
        lblBadge.repaint();
    }

    public void setOptionText(String text) {
        this.optionText = text != null ? text : "";
        lblText.setText("<html><body style='width: 530px; font-family: Segoe UI; font-size: 11pt; color: #FFFFFF; line-height: 1.35;'>"
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
        updateBadgeForeground();
        repaint();
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
            bg = Theme.HUST_RED_TINT; // Subtle warm crimson tint
        } else if (isHovered) {
            bg = Theme.BG_CARD_HOVER;
        } else {
            bg = Theme.BG_CARD;
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 12, 12);

        // Border
        if (isSelected) {
            g2.setColor(Theme.HUST_RED);
            g2.setStroke(new BasicStroke(2.2f));
        } else if (isHovered) {
            g2.setColor(Theme.HUST_GOLD);
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
