package appcreator.ui;

import appcreator.io.Lang;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全界面字体统一：英文和字母用 Consolas，非英文（汉字、阿拉伯文、泰文、谚文……）
 * 用一款能显示当前语言字形的字体（默认黑体，缺字形时自动换 Segoe UI / Nirmala UI 等）。
 * 界面搭好后调一次 {@link #apply(Component)}，会按每个控件当前文字自动挑字体。
 */
public final class Fonts {

    /** 组件库里的每个条目是自己画文字的，不需要在这里改字体 */
    private static final List<String> SKIP = Arrays.asList("appcreator.ui.PalettePanel$Item");

    /** 非英文文字的候选字体，按顺序挑第一款能显示当前语言全部字形的 */
    private static final String[] UI_CANDIDATES = {
            Theme.CJK_FAMILY, "Microsoft YaHei", "Yu Gothic UI", "Meiryo UI", "Malgun Gothic",
            "Nirmala UI", "Leelawadee UI", "Segoe UI", "Arial Unicode MS", "Dialog"
    };

    private static final Map<String, Font> PROBES = new HashMap<String, Font>();
    private static final Map<String, String> TEXT_FAMILY = new HashMap<String, String>();

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

    /** 非 ASCII 字符（阿拉伯文、泰文、天城文、谚文、假名之类都算） */
    public static boolean hasNonAscii(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x80) {
                return true;
            }
        }
        return false;
    }

    /** 递归把控件字体换成 Consolas / 当前语言字体，字号和粗细保持原样 */
    public static void apply(Component root) {
        syncLanguage();
        applyTree(root);
    }

    /** 按当前界面语言重挑「非英文」字体，并清掉按文字算出来的缓存 */
    public static void syncLanguage() {
        StringBuilder sample = new StringBuilder();
        sample.append('中').append(Lang.name());
        for (String key : new String[]{"组件库", "新建", "设置", "窗口", "标签", "进度条", "超链接"}) {
            sample.append(Lang.t(key));
        }
        String family = familyForText(sample.toString());
        Theme.setUiFamily(family);
        TEXT_FAMILY.clear();
    }

    private static void applyTree(Component root) {
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
                applyTree(child);
            }
        }
    }

    private static Font pick(Font base, String text) {
        return new Font(familyForText(text), base.getStyle(), base.getSize());
    }

    /**
     * 纯英文 / 数字 / 符号 → Consolas；带非 ASCII 字符 → 挑一款能显示这些字形的字体。
     */
    public static String familyForText(String text) {
        if (text == null || text.length() == 0) {
            return Theme.MONO_FAMILY;
        }
        String cached = TEXT_FAMILY.get(text);
        if (cached != null) {
            return cached;
        }
        String family;
        if (!hasNonAscii(text)) {
            family = Theme.MONO_FAMILY;
        } else {
            family = firstDisplayable(text);
        }
        if (TEXT_FAMILY.size() > 4000) {
            TEXT_FAMILY.clear();
        }
        TEXT_FAMILY.put(text, family);
        return family;
    }

    /** 按顺序找第一款能显示 text 里所有非 ASCII 字符的字体 */
    private static String firstDisplayable(String text) {
        for (String family : UI_CANDIDATES) {
            if (canDisplayAll(family, text)) {
                return family;
            }
        }
        return "Dialog";
    }

    private static boolean canDisplayAll(String family, String text) {
        Font probe = PROBES.get(family);
        if (probe == null) {
            probe = new Font(family, Font.PLAIN, 14);
            PROBES.put(family, probe);
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x80 || Character.isISOControl(c)) {
                continue;
            }
            if (!probe.canDisplay(c)) {
                return false;
            }
        }
        return true;
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
