package client.ui.components;

import client.ui.Theme;
import shared.dtos.ScoreTrendPointDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom Java2D Graphical Chart Component.
 * Supports rendering:
 *   1. LINE CHART: Score progression trendline with HUST Red & Gold gradient fill and grid
 *   2. BAR CHART: Topic / Category performance comparison with clean academic styling
 */
public class ChartPanel extends ModernCard {

    public enum ChartType {
        LINE,
        BAR
    }

    private ChartType chartType = ChartType.LINE;
    private String chartTitle = "Phân Tích Tiến Độ Học Tập";
    private List<ScoreTrendPointDTO> trendPoints = new ArrayList<>();
    private Map<String, Double> barData = null;

    public ChartPanel(String title, ChartType type) {
        super(new BorderLayout());
        this.chartTitle = title;
        this.chartType = type;
        setPreferredSize(new Dimension(480, 260));
    }

    public void setLineData(List<ScoreTrendPointDTO> points) {
        this.chartType = ChartType.LINE;
        this.trendPoints = (points != null) ? points : new ArrayList<>();
        repaint();
    }

    public void setBarData(Map<String, Double> data) {
        this.chartType = ChartType.BAR;
        this.barData = data;
        repaint();
    }

    public void setChartTitle(String title) {
        this.chartTitle = title;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chart Header
        g2.setFont(Theme.FONT_SUBTITLE);
        g2.setColor(Theme.TEXT_GOLD);
        g2.drawString(chartTitle, 20, 28);

        int paddingLeft = 50;
        int paddingRight = 30;
        int paddingTop = 45;
        int paddingBottom = 40;

        int plotW = w - paddingLeft - paddingRight;
        int plotH = h - paddingTop - paddingBottom;

        if (plotW <= 0 || plotH <= 0) {
            g2.dispose();
            return;
        }

        if (chartType == ChartType.LINE) {
            paintLineChart(g2, paddingLeft, paddingTop, plotW, plotH);
        } else {
            paintBarChart(g2, paddingLeft, paddingTop, plotW, plotH);
        }

        g2.dispose();
    }

    private void paintLineChart(Graphics2D g2, int x0, int y0, int w, int h) {
        // Draw Y Grid lines (0, 2.5, 5.0, 7.5, 10.0)
        g2.setFont(Theme.FONT_SMALL);
        g2.setStroke(new BasicStroke(1.0f));

        double[] yVals = {0.0, 2.5, 5.0, 7.5, 10.0};
        for (double v : yVals) {
            int y = y0 + h - (int) ((v / 10.0) * h);
            g2.setColor(new Color(48, 56, 76, 120)); // Soft clean grid
            g2.drawLine(x0, y, x0 + w, y);

            g2.setColor(Theme.TEXT_MUTED);
            String label = String.format("%.1f", v);
            g2.drawString(label, x0 - 32, y + 4);
        }

        if (trendPoints.isEmpty()) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.FONT_BODY);
            String empty = "Chưa có dữ liệu lịch sử thi. Hãy tham gia tự luyện hoặc thi đấu!";
            int strW = g2.getFontMetrics().stringWidth(empty);
            g2.drawString(empty, x0 + (w - strW) / 2, y0 + h / 2);
            return;
        }

        // Calculate points
        int n = trendPoints.size();
        int[] xs = new int[n];
        int[] ys = new int[n];

        for (int i = 0; i < n; i++) {
            xs[i] = (n == 1) ? x0 + w / 2 : x0 + (int) ((double) i / (n - 1) * w);
            double score = Math.max(0.0, Math.min(10.0, trendPoints.get(i).getScore()));
            ys[i] = y0 + h - (int) ((score / 10.0) * h);
        }

        // Draw Gradient Fill beneath curve (HUST Red to Gold)
        GeneralPath areaPath = new GeneralPath();
        areaPath.moveTo(xs[0], y0 + h);
        for (int i = 0; i < n; i++) {
            areaPath.lineTo(xs[i], ys[i]);
        }
        areaPath.lineTo(xs[n - 1], y0 + h);
        areaPath.closePath();

        GradientPaint gp = new GradientPaint(
                0, y0, new Color(206, 24, 30, 85),
                0, y0 + h, new Color(245, 158, 11, 10)
        );
        g2.setPaint(gp);
        g2.fill(areaPath);

        // Draw Line (HUST Red)
        g2.setColor(Theme.HUST_RED);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }

        // Draw Data dots and labels
        for (int i = 0; i < n; i++) {
            // Gold halo dot with white core
            g2.setColor(Theme.HUST_GOLD);
            g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);

            // Score tag
            g2.setColor(Theme.TEXT_GOLD);
            g2.setFont(Theme.FONT_BADGE);
            String scoreStr = String.format("%.1f", trendPoints.get(i).getScore());
            int tagW = g2.getFontMetrics().stringWidth(scoreStr);
            g2.drawString(scoreStr, xs[i] - tagW / 2, ys[i] - 8);

            // X label (Test index)
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.FONT_SMALL);
            String xLbl = "#" + (i + 1);
            int xLblW = g2.getFontMetrics().stringWidth(xLbl);
            g2.drawString(xLbl, xs[i] - xLblW / 2, y0 + h + 18);
        }
    }

    private void paintBarChart(Graphics2D g2, int x0, int y0, int w, int h) {
        if (barData == null || barData.isEmpty()) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.FONT_BODY);
            String empty = "Chưa có dữ liệu thống kê chủ đề học phần.";
            int strW = g2.getFontMetrics().stringWidth(empty);
            g2.drawString(empty, x0 + (w - strW) / 2, y0 + h / 2);
            return;
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(barData.entrySet());
        int count = entries.size();
        int barHeight = Math.min(26, (h - (count * 6)) / count);

        g2.setFont(Theme.FONT_SMALL);

        int currY = y0 + 10;
        for (Map.Entry<String, Double> entry : entries) {
            String topic = entry.getKey();
            double pct = Math.max(0.0, Math.min(100.0, entry.getValue()));

            // Topic name
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(topic, x0, currY - 2);

            // Background bar track
            g2.setColor(Theme.BG_INPUT);
            g2.fillRoundRect(x0, currY, w - 60, barHeight, 6, 6);

            // Filled bar with gradient
            int fillW = (int) ((pct / 100.0) * (w - 60));
            Color c1 = pct >= 75 ? Theme.SUCCESS : (pct >= 50 ? Theme.HUST_GOLD : Theme.HUST_RED);
            Color c2 = pct >= 75 ? Theme.SUCCESS_HOVER : (pct >= 50 ? Theme.HUST_GOLD_HOVER : Theme.HUST_RED_HOVER);

            GradientPaint barGp = new GradientPaint(x0, currY, c1, x0 + fillW, currY, c2);
            g2.setPaint(barGp);
            g2.fillRoundRect(x0, currY, fillW, barHeight, 6, 6);

            // Percentage label
            g2.setColor(Theme.TEXT_GOLD);
            g2.setFont(Theme.FONT_BADGE);
            String pctStr = String.format("%.0f%%", pct);
            g2.drawString(pctStr, x0 + w - 45, currY + barHeight - 4);

            currY += barHeight + 18;
        }
    }
}
