package appcreator.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 白色圆角卡片容器。
 */
public class RoundPanel extends JPanel {

    private int radius = 12;
    private Color fill = Color.WHITE;
    private Color line = null;

    public RoundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public RoundPanel(LayoutManager layout, int radius, Color fill) {
        this(layout);
        this.radius = radius;
        this.fill = fill;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setLine(Color line) {
        this.line = line;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (line != null) {
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
