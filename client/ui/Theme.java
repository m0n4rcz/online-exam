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
 * Features a high-contrast dark slate palette, clear typography scale, and ergonomic styling.
 */
public final class Theme {

    private Theme() {}

    // ==========================================
    // COLOR PALETTE (Modern Deep Slate & Accents)
    // ==========================================
    public static final Color BG_DARK        = new Color(11, 17, 32);    // #0B1120 (Deep Slate)
    public static final Color BG_SURFACE     = new Color(17, 24, 39);    // #111827 (Slate 900)
    public static final Color BG_CARD        = new Color(30, 41, 59);    // #1E293B (Slate 800)
    public static final Color BG_CARD_HOVER  = new Color(45, 59, 80);    // Hover state
    public static final Color BG_INPUT       = new Color(15, 23, 42);    // #0F172A (Input field)
    public static final Color BORDER         = new Color(51, 65, 85);    // #334155 (Subtle border)
    public static final Color BORDER_FOCUS   = new Color(99, 102, 241);  // #6366F1 (Indigo focus glow)

    // Academic & Functional Accents
    public static final Color HUST_RED       = new Color(206, 24, 30);   // #CE181E (HUST Traditional Crimson)
    public static final Color PRIMARY        = new Color(79, 70, 229);   // #4F46E5 (Indigo 600 - Primary Action)
    public static final Color PRIMARY_HOVER  = new Color(99, 102, 241);  // #6366F1
    public static final Color SECONDARY      = new Color(14, 165, 233);  // #0EA5E9 (Sky 500)
    public static final Color SECONDARY_HOVER= new Color(2, 132, 199);
    public static final Color SUCCESS        = new Color(16, 185, 129);  // #10B981 (Emerald 500 - Correct/Ready)
    public static final Color SUCCESS_HOVER  = new Color(5, 150, 105);
    public static final Color WARNING        = new Color(245, 158, 11);  // #F59E0B (Amber 500 - Clock/Review)
    public static final Color FLAGGED        = new Color(217, 119, 6);   // #D97706 (Amber - Marked for review)
    public static final Color DANGER         = new Color(239, 68, 68);   // #EF4444 (Rose 500 - Danger/Alert)
    public static final Color DANGER_HOVER   = new Color(220, 38, 38);

    // High Contrast Typography
    public static final Color TEXT_PRIMARY   = new Color(248, 250, 252); // #F8FAFC (99% White)
    public static final Color TEXT_SECONDARY = new Color(156, 163, 175); // #9CA3AF (Clear Muted)
    public static final Color TEXT_MUTED     = new Color(107, 114, 128); // #6B7280 (Hints)
    public static final Color TEXT_ACCENT    = new Color(129, 140, 248); // #818CF8 (Highlight)

    // ==========================================
    // TYPOGRAPHY (Segoe UI Academic Scale)
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
        table.setSelectionBackground(new Color(67, 56, 202)); // Deep indigo
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_DARK);
        header.setForeground(TEXT_SECONDARY);
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
     * Styles a JScrollPane with a sleek, minimalist dark scrollbar.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollPane.getViewport().setBackground(BG_CARD);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(71, 85, 105);
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
     * Creates circular student avatar icon with their initials.
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
