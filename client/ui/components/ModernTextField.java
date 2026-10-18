package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Modern text input field featuring placeholder text, rounded corners,
 * and high-contrast focused state borders.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 8;

    public ModernTextField() {
        this("");
    }

    public ModernTextField(String placeholder) {
        this.placeholder = placeholder;
        init();
    }

    private void init() {
        setOpaque(false);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_PRIMARY);
        setCaretColor(Theme.TEXT_PRIMARY);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setPreferredSize(new Dimension(220, 38));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2.setColor(Theme.BG_INPUT);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Border
        g2.setColor(isFocused ? Theme.BORDER_FOCUS : Theme.BORDER);
        g2.setStroke(new BasicStroke(isFocused ? 1.5f : 1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);

        // Placeholder
        if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            gPlaceholder.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gPlaceholder.setFont(Theme.FONT_BODY);
            gPlaceholder.setColor(Theme.TEXT_MUTED);
            Insets insets = getInsets();
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            gPlaceholder.drawString(placeholder, insets.left, y);
            gPlaceholder.dispose();
        }
    }

    /**
     * Specialized password field with identical styling.
     */
    public static class PasswordField extends JPasswordField {
        private String placeholder = "";
        private boolean isFocused = false;
        private int cornerRadius = 8;

        public PasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT_PRIMARY);
            setCaretColor(Theme.TEXT_PRIMARY);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setPreferredSize(new Dimension(220, 38));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(Theme.BG_INPUT);
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

            g2.setColor(isFocused ? Theme.BORDER_FOCUS : Theme.BORDER);
            g2.setStroke(new BasicStroke(isFocused ? 1.5f : 1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);

            if (getPassword().length == 0 && !placeholder.isEmpty() && !isFocused) {
                Graphics2D gPlaceholder = (Graphics2D) g.create();
                gPlaceholder.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gPlaceholder.setFont(Theme.FONT_BODY);
                gPlaceholder.setColor(Theme.TEXT_MUTED);
                Insets insets = getInsets();
                FontMetrics fm = gPlaceholder.getFontMetrics();
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                gPlaceholder.drawString(placeholder, insets.left, y);
                gPlaceholder.dispose();
            }
        }
    }
}
