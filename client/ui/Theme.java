package client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Modern design system and visual theme for the Online Exam System GUI.
 * Provides a cohesive dark palette, consistent typography, and reusable UI factories.
 */
public final class Theme {

    private Theme() {}

    // ==========================================
    // COLOR PALETTE (Modern Slate & Vibrant Accents)
    // ==========================================
    public static final Color BG_DARK        = new Color(15, 23, 42);    // #0F172A (Slate 900)
    public static final Color BG_SURFACE     = new Color(30, 41, 59);    // #1E293B (Slate 800)
    public static final Color BG_CARD        = new Color(30, 41, 59);    // #1E293B
    public static final Color BG_CARD_HOVER  = new Color(42, 56, 78);
    public static final Color BG_INPUT       = new Color(15, 23, 42);    // #0F172A
    public static final Color BORDER         = new Color(51, 65, 85);    // #334155 (Slate 700)
    public static final Color BORDER_FOCUS   = new Color(99, 102, 241);  // Indigo focus ring

    // Accents
    public static final Color PRIMARY        = new Color(99, 102, 241);  // #6366F1 (Indigo 500)
    public static final Color PRIMARY_HOVER  = new Color(79, 70, 229);   // #4F46E5 (Indigo 600)
    public static final Color SECONDARY      = new Color(6, 182, 212);   // #06B6D4 (Cyan 500)
    public static final Color SECONDARY_HOVER= new Color(8, 145, 178);
    public static final Color SUCCESS        = new Color(16, 185, 129);  // #10B981 (Emerald 500)
    public static final Color SUCCESS_HOVER  = new Color(5, 150, 105);
    public static final Color WARNING        = new Color(245, 158, 11);  // #F59E0B (Amber 500)
    public static final Color DANGER         = new Color(239, 68, 68);   // #EF4444 (Rose 500)
    public static final Color DANGER_HOVER   = new Color(220, 38, 38);

    // Text Colors
    public static final Color TEXT_PRIMARY   = new Color(248, 250, 252); // #F8FAFC
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184); // #94A3B8
    public static final Color TEXT_MUTED     = new Color(100, 116, 139); // #64748B
    public static final Color TEXT_ACCENT    = new Color(129, 140, 248); // #818CF8

    // ==========================================
    // TYPOGRAPHY (Segoe UI or Modern Sans-Serif)
    // ==========================================
    public static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_BADGE = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_BIG_STAT = new Font(FONT_FAMILY, Font.BOLD, 32);

    // ==========================================
    // REUSABLE UI STYLING UTILITIES
    // ==========================================

    /**
     * Styles a standard JTable with modern dark theme headers, borders, and rows.
     */
    public static void styleTable(JTable table) {
        table.setBackground(BG_SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(38);
        table.setFont(FONT_BODY);
        table.setSelectionBackground(new Color(67, 56, 202)); // Deep indigo
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(15, 23, 42));
        header.setForeground(TEXT_SECONDARY);
        header.setFont(FONT_HEADER);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBackground(BG_SURFACE);
        centerRenderer.setForeground(TEXT_PRIMARY);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        leftRenderer.setBackground(BG_SURFACE);
        leftRenderer.setForeground(TEXT_PRIMARY);

        table.setDefaultRenderer(Object.class, leftRenderer);
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
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        return badge;
    }

    /**
     * Creates an empty border with standard padding.
     */
    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /**
     * Creates standard card border with rounded corners.
     */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        );
    }
}
