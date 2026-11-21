package appcreator.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 全局配色与字体（清新风格：薄荷绿 + 白 + 淡灰绿背景）。
 */
public final class Theme {

    public static final Color BG = new Color(0xF4F8F7);
    public static final Color CARD = Color.WHITE;
    public static final Color ACCENT = new Color(0x2FC2A6);
    public static final Color ACCENT_DARK = new Color(0x1E9E86);
    public static final Color TEXT = new Color(0x22312E);
    public static final Color MUTED = new Color(0x84948F);
    public static final Color BORDER = new Color(0xE3EBE8);
    public static final Color SOFT = new Color(0xEAF7F3);
    public static final Color DANGER = new Color(0xE0685D);
    public static final Color GRID = new Color(0xE9F0ED);

    /** 英文与字母统一 Consolas，汉字统一黑体 */
    public static final String MONO_FAMILY = "Consolas";
    public static final String CJK_FAMILY = "黑体";

    private Theme() {
    }

    public static Font ui(int size) {
        return ui(size, Font.PLAIN);
    }

    public static Font ui(int size, int style) {
        return new Font(CJK_FAMILY, style, size);
    }

    /** 等宽字体：英文 / 代码用 Consolas */
    public static Font mono(int size) {
        return new Font(MONO_FAMILY, Font.PLAIN, size);
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(ui(size));
        l.setForeground(color);
        return l;
    }

    /** 输入框统一样式 */
    public static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(ui(12));
        f.setForeground(TEXT);
        f.setBackground(Color.WHITE);
        f.setCaretColor(ACCENT_DARK);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(8, BORDER), new javax.swing.border.EmptyBorder(4, 8, 4, 8)));
        return f;
    }

    /** 左侧标签列：固定一列宽度（够放英文 Background / Foreground），切语言后英文也不会被截断 */
    public static JLabel autoLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(LABEL_COLUMN, d.width), 24);
            }
        };
        l.setFont(ui(size));
        l.setForeground(color);
        return l;
    }

    private static final int LABEL_COLUMN = 76;

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
