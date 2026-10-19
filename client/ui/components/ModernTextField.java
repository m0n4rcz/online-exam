package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Modern text input field featuring placeholder text, rounded corners,
 * active HUST Gold focus ring, and high-contrast typography for student test applications.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private boolean isError = false;
    private int cornerRadius = 8;

    public ModernTextField() {
        this("");
    }

    public ModernTextField(String placeholder) {
        this.placeholder = placeholder;
        init();
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextFieldUI());
    }

    private void init() {
        setUI(new BasicTextFieldUI());
        setOpaque(false);
        setBackground(Theme.BG_INPUT);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_PRIMARY);
        setCaretColor(Theme.HUST_GOLD);
        setSelectedTextColor(Color.WHITE);
        setSelectionColor(new Color(206, 24, 30, 180));
        setBorder(new EmptyBorder(8, 14, 8, 14));
        setPreferredSize(new Dimension(240, 40));

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

    public void setError(boolean error) {
        this.isError = error;
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

        // Border with HUST Gold focus ring
        if (isError) {
            g2.setColor(Theme.DANGER);
            g2.setStroke(new BasicStroke(1.5f));
        } else if (isFocused) {
            g2.setColor(Theme.BORDER_FOCUS);
            g2.setStroke(new BasicStroke(2.0f));
        } else {
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);

        // Placeholder text - visible whenever text is empty
        if (getText().isEmpty() && !placeholder.isEmpty()) {
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
     * Specialized password field with identical styling and HUST focus ring.
     */
    public static class PasswordField extends JPasswordField {
        private String placeholder = "";
        private boolean isFocused = false;
        private boolean isError = false;
        private int cornerRadius = 8;

        public PasswordField(String placeholder) {
            this.placeholder = placeholder;
            setUI(new BasicPasswordFieldUI());
            setOpaque(false);
            setBackground(Theme.BG_INPUT);
            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT_PRIMARY);
            setCaretColor(Theme.HUST_GOLD);
            setSelectedTextColor(Color.WHITE);
            setSelectionColor(new Color(206, 24, 30, 180));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setPreferredSize(new Dimension(240, 40));

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
        public void updateUI() {
            setUI(new BasicPasswordFieldUI());
        }

        public void setError(boolean error) {
            this.isError = error;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(Theme.BG_INPUT);
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

            if (isError) {
                g2.setColor(Theme.DANGER);
                g2.setStroke(new BasicStroke(1.5f));
            } else if (isFocused) {
                g2.setColor(Theme.BORDER_FOCUS);
                g2.setStroke(new BasicStroke(2.0f));
            } else {
                g2.setColor(Theme.BORDER);
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);

            if (getPassword().length == 0 && !placeholder.isEmpty()) {
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
