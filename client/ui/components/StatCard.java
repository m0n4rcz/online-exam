package client.ui.components;

import client.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Metric card displaying a prominent summary number, title, and descriptive subtitle.
 */
public class StatCard extends ModernCard {

    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtitle;
    private final Color accentColor;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        super(new BorderLayout(0, 4));
        this.accentColor = accentColor;

        setBorder(new EmptyBorder(14, 18, 14, 18));
        setPreferredSize(new Dimension(200, 110));

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(Theme.FONT_BADGE);
        lblTitle.setForeground(Theme.TEXT_MUTED);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(Theme.FONT_BIG_STAT);
        lblValue.setForeground(accentColor);

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(Theme.FONT_SMALL);
        lblSubtitle.setForeground(Theme.TEXT_SECONDARY);

        JPanel pnlCenter = new JPanel(new GridLayout(3, 1, 0, 2));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblTitle);
        pnlCenter.add(lblValue);
        pnlCenter.add(lblSubtitle);

        add(pnlCenter, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        lblValue.setText(value);
        repaint();
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Subtle left accent bar
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(accentColor);
        g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
        g2.dispose();
    }
}
