package client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Professional design system and visual theme for HUST Online Examination Platform.
 * Built with the official Hanoi University of Science and Technology (ĐHBK Hà Nội)
 * Red & Gold visual identity, prioritizing accessibility, high legibility, and distraction-free testing.
 */
public final class Theme {

    private Theme() {}

    // ==========================================
    // COLOR PALETTE (HUST Red & Academic Gold)
    // ==========================================

    // Backgrounds & Surfaces (Warm deep slate/charcoal)
    public static final Color BG_DARK         = new Color(15, 18, 25);     // #0F1219 (Deep canvas)
    public static final Color BG_SURFACE      = new Color(20, 24, 34);     // #141822 (Elevated surface)
    public static final Color BG_CARD         = new Color(26, 31, 44);     // #1A1F2C (Card container)
    public static final Color BG_CARD_HOVER   = new Color(36, 43, 60);     // #242B3C (Card hover)
    public static final Color BG_INPUT        = new Color(18, 21, 30);     // #12151E (Input background)
    public static final Color BORDER          = new Color(48, 56, 76);     // #30384C (Clean subtle border)
    public static final Color BORDER_LIGHT    = new Color(65, 75, 100);    // #414B64 (Hover border)
    public static final Color BORDER_FOCUS    = new Color(245, 158, 11);   // #F59E0B (HUST Gold focus ring)

    // HUST Brand Core Colors
    public static final Color HUST_RED        = new Color(206, 24, 30);    // #CE181E (HUST Traditional Crimson)
    public static final Color HUST_RED_HOVER  = new Color(225, 29, 72);    // #E11D48 (Vibrant hover)
    public static final Color HUST_RED_DARK   = new Color(153, 27, 27);    // #991B1B (Pressed state)
    public static final Color HUST_RED_TINT   = new Color(206, 24, 30, 28); // Subtle background tint

    public static final Color HUST_GOLD       = new Color(245, 158, 11);   // #F59E0B (HUST Academic Gold)
    public static final Color HUST_GOLD_HOVER = new Color(251, 191, 36);   // #FBBF24 (Bright Gold)
    public static final Color HUST_GOLD_DARK  = new Color(217, 119, 6);    // #D97706 (Deep Amber)
    public static final Color HUST_GOLD_TINT  = new Color(245, 158, 11, 26);// Subtle gold tint

    // Functional Semantic Colors
    public static final Color PRIMARY         = HUST_RED;
    public static final Color PRIMARY_HOVER   = HUST_RED_HOVER;
    public static final Color SECONDARY       = HUST_GOLD;
    public static final Color SECONDARY_HOVER = HUST_GOLD_HOVER;

    public static final Color SUCCESS         = new Color(16, 185, 129);   // #10B981 (Emerald 500 - Correct answers)
    public static final Color SUCCESS_HOVER   = new Color(5, 150, 105);
    public static final Color WARNING         = HUST_GOLD;                 // #F59E0B (Amber Gold)
    public static final Color FLAGGED         = HUST_GOLD_DARK;            // #D97706 (Question review flag)
    public static final Color DANGER          = new Color(239, 68, 68);    // #EF4444 (Rose Red)
    public static final Color DANGER_HOVER    = new Color(220, 38, 38);

    // High-Contrast Accessible Typography
    public static final Color TEXT_PRIMARY    = new Color(255, 255, 255);  // #FFFFFF (Crisp 100% white)
    public static final Color TEXT_SECONDARY  = new Color(209, 213, 219);  // #D1D5DB (High-contrast gray, 8.5:1)
    public static final Color TEXT_MUTED      = new Color(148, 163, 184);  // #94A3B8 (Accessible caption)
    public static final Color TEXT_GOLD       = new Color(251, 191, 36);   // #FBBF24 (Gold text highlight)
    public static final Color TEXT_RED        = new Color(248, 113, 113);  // #F87171 (Crimson highlight)

    // ==========================================
    // TYPOGRAPHY (Clear Academic Scale)
    // ==========================================
    public static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_HERO      = new Font(FONT_FAMILY, Font.BOLD, 26);
    public static final Font FONT_TITLE     = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font FONT_SUBTITLE  = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_QUESTION  = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_HEADER    = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY      = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL     = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_BADGE     = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_TIMER     = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_BIG_STAT  = new Font(FONT_FAMILY, Font.BOLD, 30);

    // ==========================================
    // REUSABLE UI STYLING UTILITIES
    // ==========================================

    /**
     * Styles a standard JTable with modern dark theme headers, borders, and rows.
     */
    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(40);
        table.setFont(FONT_BODY);
        table.setSelectionBackground(new Color(153, 27, 27, 200)); // Dark HUST Red selection
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_DARK);
        header.setForeground(TEXT_GOLD);
        header.setFont(FONT_HEADER);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBackground(BG_CARD);
        centerRenderer.setForeground(TEXT_PRIMARY);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftRenderer.setBorder(new EmptyBorder(0, 14, 0, 14));
        leftRenderer.setBackground(BG_CARD);
        leftRenderer.setForeground(TEXT_PRIMARY);

        table.setDefaultRenderer(Object.class, leftRenderer);
    }

    /**
     * Styles a JScrollPane with a sleek, minimalist dark scrollbar with gold/crimson hints.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollPane.getViewport().setBackground(BG_CARD);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(65, 75, 100);
                this.trackColor = BG_SURFACE;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
    }

    /**
     * Creates a styled chip/pill label with rounded background.
     */
    public static JLabel createBadge(String text, Color bgColor, Color textColor) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setForeground(textColor);
        badge.setFont(FONT_BADGE);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        return badge;
    }

    /**
     * Creates circular student avatar icon with their initials and subtle golden border ring.
     */
    public static JLabel createAvatar(String name, int size, Color bgColor) {
        String initial = (name != null && !name.trim().isEmpty())
                ? name.trim().substring(0, 1).toUpperCase() : "U";
        JLabel avatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());

                // Subtle gold ring
                g2.setColor(HUST_GOLD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(1, 1, getWidth() - 2, getHeight() - 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setFont(new Font(FONT_FAMILY, Font.BOLD, size / 2));
        avatar.setForeground(Color.WHITE);
        avatar.setOpaque(false);
        return avatar;
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }
}
