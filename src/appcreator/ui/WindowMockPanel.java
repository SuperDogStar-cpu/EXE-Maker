package appcreator.ui;

import appcreator.gen.IconUtil;
import appcreator.model.Project;

import javax.swing.*;
import java.awt.*;

/**
 * 模拟窗口：在画布外面套一层“真实窗口”外观（标题栏 + 边框 + 尺寸提示），
 * 方便直观看到窗口的最终大小。可以关闭，关闭后只显示纯画布。
 */
public class WindowMockPanel extends JPanel {

    private static final int FRAME_X = 2;
    private static final int FRAME_Y = 2;
    private static final int TITLE_H = 32;
    private static final int SHADOW = 8;
    private static final int MARGIN = 20;
    private static final int CAPTION_H = 30;

    private static final Color TITLE_BG = new Color(0xF0F5F3);
    private static final Color TITLE_ACTIVE = new Color(0x22312E);
    private static final Color EDGE = new Color(0xD8E3DF);

    private final DesignCanvas canvas;
    private Project project;
    private boolean mock;

    private Image iconCache;
    private byte[] iconCacheSrc;

    public WindowMockPanel(DesignCanvas canvas, Project project, boolean mock) {
        this.canvas = canvas;
        this.project = project;
        this.mock = mock;
        setLayout(null);
        setOpaque(false);
        add(canvas);
    }

    public void setProject(Project project) {
        this.project = project;
        revalidate();
        repaint();
    }

    public boolean isMockEnabled() {
        return mock;
    }

    public void setMockEnabled(boolean mock) {
        this.mock = mock;
        revalidate();
        repaint();
    }

    /** 标题栏小图标（按字节缓存，避免每帧重新解码） */
    private Image projectIcon() {
        byte[] data = project == null ? null : project.iconPng;
        if (data == null || data.length == 0) {
            iconCache = null;
            iconCacheSrc = null;
            return null;
        }
        if (iconCache == null || iconCacheSrc != data) {
            iconCache = IconUtil.image(data, 16);
            iconCacheSrc = data;
        }
        return iconCache;
    }

    @Override
    public Dimension getPreferredSize() {        Dimension d = canvas.getPreferredSize();
        if (!mock) {
            return new Dimension(d.width, d.height);
        }
        return new Dimension(d.width + FRAME_X * 2 + MARGIN * 2 + SHADOW,
                d.height + FRAME_Y + TITLE_H + 1 + SHADOW + MARGIN + CAPTION_H);
    }

    @Override
    public void doLayout() {
        Dimension d = canvas.getPreferredSize();
        if (mock) {
            canvas.setBounds(MARGIN + FRAME_X + 1, MARGIN + FRAME_Y + TITLE_H + 1, d.width, d.height);
        } else {
            canvas.setBounds(0, 0, d.width, d.height);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!mock || project == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Dimension d = canvas.getPreferredSize();
        int fx = MARGIN + FRAME_X;
        int fy = MARGIN + FRAME_Y;
        int fw = d.width + 2;
        int fh = TITLE_H + d.height + 1;

        // 桌面底板（让窗口边界和大小更直观）
        g2.setPaint(new GradientPaint(0, 0, new Color(0xF7FBFA), 0, getHeight(),
                new Color(0xE3EFEA)));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.setColor(new Color(0xDCE8E3));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

        // 投影
        for (int i = 7; i >= 1; i--) {
            g2.setColor(new Color(0x1E, 0x2B, 0x28, 12));
            g2.fillRoundRect(fx + 4 - i, fy + 5 + i, fw + i * 2, fh + i + 2, 18, 18);
        }

        // 窗口本体
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(fx, fy, fw, fh, 12, 12);

        // 标题栏
        g2.setColor(TITLE_BG);
        g2.fillRoundRect(fx, fy, fw, TITLE_H, 12, 12);
        g2.fillRect(fx, fy + TITLE_H - 12, fw, 12);

        // 标题栏分隔线
        g2.setColor(EDGE);
        g2.fillRect(fx, fy + TITLE_H, fw, 1);

        // 应用小图标：有自定义图标就用自定义的，否则用默认色块
        Image icon = projectIcon();
        if (icon != null) {
            g2.drawImage(icon, fx + 12, fy + (TITLE_H - 16) / 2, 16, 16, null);
        } else {
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(fx + 12, fy + (TITLE_H - 14) / 2, 14, 14, 5, 5);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(fx + 16, fy + (TITLE_H - 14) / 2 + 4, 6, 6, 2, 2);
        }

        // 标题（超出宽度时截断）
        String title = project.title == null ? "" : project.title;
        g2.setFont(new Font(Fonts.familyForText(title), Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int avail = fw - 190;
        String shown = title;
        while (Fonts.width(g2, shown) > avail && shown.length() > 1) {
            shown = shown.substring(0, shown.length() - 1);
        }
        g2.setColor(TITLE_ACTIVE);
        Fonts.draw(g2, shown, fx + 36, fy + (TITLE_H - fm.getHeight()) / 2 + fm.getAscent(), Font.BOLD, 12);

        // 尺寸提示
        String size = project.width + " × " + project.height;
        g2.setFont(Theme.mono(11));
        FontMetrics sm = g2.getFontMetrics();
        g2.setColor(Theme.MUTED);
        g2.drawString(size, fx + fw - sm.stringWidth(size) - 86,
                fy + (TITLE_H - sm.getHeight()) / 2 + sm.getAscent());

        // 窗口按钮（最小化 / 最大化 / 关闭）
        int cy = fy + TITLE_H / 2;
        g2.setColor(Theme.MUTED);
        g2.setStroke(new BasicStroke(1f));
        int c1 = fx + fw - 70;
        int c2 = fx + fw - 44;
        int c3 = fx + fw - 18;
        g2.drawLine(c1 - 4, cy + 4, c1 + 4, cy + 4);
        g2.drawRect(c2 - 4, cy - 4, 8, 8);
        g2.drawLine(c3 - 4, cy - 4, c3 + 4, cy + 4);
        g2.drawLine(c3 - 4, cy + 4, c3 + 4, cy - 4);

        // 外框
        g2.setColor(EDGE);
        g2.drawRoundRect(fx, fy, fw - 1, fh - 1, 12, 12);

        // 底部说明
        String caption = "窗口 " + project.width + " × " + project.height
                + " · 预览时居中显示";
        g2.setFont(new Font(Fonts.familyForText(caption), Font.PLAIN, 11));
        g2.setColor(Theme.MUTED);
        Fonts.draw(g2, caption, fx + 2, fy + fh + 16, Font.PLAIN, 11);

        g2.dispose();
    }
}
