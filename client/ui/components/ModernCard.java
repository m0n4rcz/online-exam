package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Clean card container panel with rounded corners, subtle border,
 * and standard internal padding.
 */
public class ModernCard extends JPanel {

    private int cornerRadius = 14;
    private Color cardColor = Theme.BG_CARD;
    private Color borderColor = Theme.BORDER;
    private boolean showBorder = true;

    public ModernCard() {
        this(new BorderLayout());
    }

    public ModernCard(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
    }

    public void setCardColor(Color color) {
        this.cardColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setShowBorder(boolean show) {
        this.showBorder = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Card background
        g2.setColor(cardColor);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Card border
        if (showBorder) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
