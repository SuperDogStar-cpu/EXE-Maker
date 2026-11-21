package appcreator.ui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * 全界面字体统一：英文和字母用 Consolas，汉字用黑体。
 * 界面搭好后调一次 {@link #apply(Component)}，会按每个控件当前文字自动挑字体。
 */
public final class Fonts {

    private static final List<String> SKIP = Arrays.asList("appcreator.ui.PalettePanel");

    private Fonts() {
    }

    public static boolean hasCjk(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (isCjk(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCjk(char c) {
        if (c < 0x2E80) {
            return false;
        }
        return (c <= 0x9FFF) || (c >= 0xAC00 && c <= 0xD7AF) || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFE30 && c <= 0xFE4F) || (c >= 0xFF00 && c <= 0xFFEF);
    }

    /** 递归把控件字体换成 Consolas / 黑体，字号和粗细保持原样 */
    public static void apply(Component root) {
        if (root == null) {
            return;
        }
        if (root instanceof Component && isSkipped(root)) {
            return;
        }
        if (root instanceof JComponent) {
            JComponent c = (JComponent) root;
            if (!(c instanceof JTextComponent)) {
                String text = textOf(c);
                Font base = c.getFont();
                if (base != null && text != null && text.length() > 0) {
                    c.setFont(pick(base, text));
                }
            }
            hookTextChanges(c);
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                apply(child);
            }
        }
    }

    private static Font pick(Font base, String text) {
        return new Font(hasCjk(text) ? Theme.CJK_FAMILY : Theme.MONO_FAMILY, base.getStyle(), base.getSize());
    }

    /** 文字后面还会变（属性面板的值、状态文字等），变了就重新挑字体 */
    private static void hookTextChanges(final JComponent c) {
        if (c.getClientProperty("appcreator.fontHook") != null) {
            return;
        }
        if (!(c instanceof AbstractButton) && !(c instanceof JLabel)) {
            return;
        }
        c.putClientProperty("appcreator.fontHook", Boolean.TRUE);
        c.addPropertyChangeListener("text", ev -> {
            String t = textOf(c);
            Font base = c.getFont();
            if (t != null && base != null && t.length() > 0) {
                c.setFont(pick(base, t));
            }
        });
    }

    private static boolean isSkipped(Component c) {
        String name = c.getClass().getName();
        for (String s : SKIP) {
            if (name.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private static String textOf(JComponent c) {
        if (c instanceof AbstractButton) {
            return ((AbstractButton) c).getText();
        }
        if (c instanceof JLabel) {
            return ((JLabel) c).getText();
        }
        return null;
    }
}
