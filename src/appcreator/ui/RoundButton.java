package appcreator.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 圆角按钮（扁平、带悬停反馈）。
 */
public class RoundButton extends JButton {

    private Color base;
    private Color hoverColor;
    private Color pressColor;

    public RoundButton(String text, Color base, Color fg) {
        this(text, null, base, fg);
    }

    public RoundButton(String text, Icon icon, Color base, Color fg) {
        super(text);
        if (icon != null) {
            setIcon(icon);
            setIconTextGap(7);
        }
        setColors(base, fg);
        setFont(Theme.ui(13));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 15, 8, 15));
    }

    /** 运行时切换配色（用于选中/未选中状态） */
    public void setColors(Color base, Color fg) {
        this.base = base;
        this.hoverColor = shade(base, 0.95);
        this.pressColor = shade(base, 0.88);
        setForeground(fg);
        repaint();
    }

    private static Color shade(Color c, double f) {
        return new Color(
                Math.max(0, Math.min(255, (int) (c.getRed() * f))),
                Math.max(0, Math.min(255, (int) (c.getGreen() * f))),
                Math.max(0, Math.min(255, (int) (c.getBlue() * f))));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = base;
        if (!isEnabled()) {
            fill = new Color(0xEFF3F2);
        } else if (getModel().isPressed() && getModel().isArmed()) {
            fill = pressColor;
        } else if (getModel().isRollover()) {
            fill = hoverColor;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 60));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
