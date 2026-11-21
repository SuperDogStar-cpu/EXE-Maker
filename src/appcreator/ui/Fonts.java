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
            Theme.CJK_FAMILY, "Microsoft YaHei", "Yu Gothic UI", "Meiryo UI", "MS Gothic",
            "Malgun Gothic", "SimSun", "Nirmala UI", "Leelawadee UI",
            "Segoe UI Emoji", "Segoe UI Symbol", "Segoe UI", "Arial Unicode MS", "Dialog"
    };

    /**
     * 实际候选链：已经下载并注册了韩文字体时，把它排在系统韩文字体（Malgun Gothic）后面当兜底，
     * 系统有韩文字体就用系统的，没有（或显示不了某个谚文字）才用下载来的 NanumGothic。
     */
    private static String[] candidates() {
        String kr = appcreator.io.KoreanFont.family();
        if (kr == null) {
            return UI_CANDIDATES;
        }
        String[] out = new String[UI_CANDIDATES.length + 1];
        int i = 0;
        for (String s : UI_CANDIDATES) {
            out[i++] = s;
            if ("Malgun Gothic".equals(s)) {
                out[i++] = kr;
            }
        }
        return out;
    }

    /** 文字里有没有谚文（韩文） */
    public static boolean hasHangul(String s) {
        if (s == null) {
            return false;
        }
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if ((cp >= 0x1100 && cp <= 0x11FF) || (cp >= 0x3130 && cp <= 0x318F)
                    || (cp >= 0xA960 && cp <= 0xA97F) || (cp >= 0xAC00 && cp <= 0xD7FF)) {
                return true;
            }
        }
        return false;
    }

    /** 当前界面是不是需要韩文字体（系统已经能显示谚文就不需要下载） */
    public static boolean needsKoreanFont() {
        if (appcreator.io.KoreanFont.family() != null) {
            return false;
        }
        String sample = hangulSample();
        if (sample.length() == 0) {
            return false;
        }
        for (String family : UI_CANDIDATES) {
            if (canDisplayAll(family, sample)) {
                return false;
            }
        }
        return true;
    }

    /** 从当前界面语言里挑出谚文字符，用来判断本机字体够不够用 */
    private static String hangulSample() {
        StringBuilder all = new StringBuilder(Lang.name());
        for (String key : new String[]{"组件库", "新建", "设置", "窗口", "属性", "代码", "取消", "确定"}) {
            all.append(Lang.t(key));
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < all.length()) {
            int cp = all.codePointAt(i);
            i += Character.charCount(cp);
            if ((cp >= 0x1100 && cp <= 0x11FF) || (cp >= 0x3130 && cp <= 0x318F)
                    || (cp >= 0xA960 && cp <= 0xA97F) || (cp >= 0xAC00 && cp <= 0xD7FF)) {
                out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    /**
     * 确保韩文字体可用（会阻塞，启动画面里用）：本地有就注册，没有就下载。
     * 成功后重算字体并刷新界面。
     */
    public static boolean ensureKoreanFont() {
        if (!needsKoreanFont()) {
            return appcreator.io.KoreanFont.family() != null;
        }
        boolean ok = appcreator.io.KoreanFont.ensure();
        if (ok) {
            refresh();
        }
        return ok;
    }

    /** 不阻塞界面：后台下载韩文字体，装好后自动刷新成韩文字体 */
    public static void prepareKoreanFontAsync() {
        if (!needsKoreanFont() || appcreator.io.KoreanFont.downloading()) {
            return;
        }
        appcreator.io.KoreanFont.ensureAsync(new Runnable() {
            public void run() {
                refresh();
            }
        });
    }

    /** 字体集合变了（比如刚注册了韩文字体）：清缓存、重挑当前语言字体、把已有窗口重刷一遍 */
    public static void refresh() {
        PROBES.clear();
        TEXT_FAMILY.clear();
        CHAR_FAMILY.clear();
        syncLanguage();
        repaintAll();
    }

    /** 把所有已显示窗口的字体重新按当前文字挑一遍（下载完韩文字体后用） */
    public static void repaintAll() {
        Runnable r = new Runnable() {
            public void run() {
                for (Window w : Window.getWindows()) {
                    if (w instanceof javax.swing.RootPaneContainer) {
                        javax.swing.RootPaneContainer rpc = (javax.swing.RootPaneContainer) w;
                        if (rpc.getContentPane() != null) {
                            applyTree(rpc.getContentPane());
                        }
                    }
                    w.repaint();
                }
            }
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(r);
        }
    }

    private static final Map<String, Font> PROBES = new HashMap<String, Font>();
    private static final Map<String, String> TEXT_FAMILY = new HashMap<String, String>();
    /** 单个字符 → 能显示它的字体家族（换字体兜底用） */
    private static final Map<Integer, String> CHAR_FAMILY = new HashMap<Integer, String>();

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
        CHAR_FAMILY.clear();
        // 界面里有谚文但本机还没注册韩文字体：后台下一份 NanumGothic，装好后自动换上去
        if (hasHangul(sample.toString()) && appcreator.io.KoreanFont.family() == null) {
            appcreator.io.KoreanFont.ensureAsync(new Runnable() {
                public void run() {
                    refresh();
                }
            });
        }
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
            family = bestDisplayable(text);
        }
        if (TEXT_FAMILY.size() > 4000) {
            TEXT_FAMILY.clear();
        }
        TEXT_FAMILY.put(text, family);
        return family;
    }

    /**
     * 单个字符用哪款字体：ASCII 走 Consolas，其余挑第一款能显示这个字形的字体。
     * 属于某个字体显示不了的字，就换成别的能显示的字体——这样不会出现空白方框。
     */
    public static String familyForChar(int codePoint) {
        if (codePoint < 0x80) {
            return Theme.MONO_FAMILY;
        }
        if (Character.isISOControl(codePoint)) {
            return Theme.MONO_FAMILY;
        }
        String cached = CHAR_FAMILY.get(codePoint);
        if (cached != null) {
            return cached;
        }
        String family = null;
        for (String cand : candidates()) {
            if (canDisplay(cand, codePoint)) {
                family = cand;
                break;
            }
        }
        if (family == null) {
            family = "Dialog";
        }
        if (CHAR_FAMILY.size() > 20000) {
            CHAR_FAMILY.clear();
        }
        CHAR_FAMILY.put(codePoint, family);
        return family;
    }

    /** 按字体把文字分段（每段一个字体系列），逐段换字体绘制 */
    private static void drawRuns(Graphics2D g2, String text, int x, int y, int style, int size) {
        if (text == null || text.length() == 0) {
            return;
        }
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            String family = familyForChar(cp);
            Font f = new Font(family, style, size);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            int start = i;
            i += len;
            // 继续吃同一字体的后续字符
            while (i < text.length()) {
                int cp2 = text.codePointAt(i);
                if (!family.equals(familyForChar(cp2))) {
                    break;
                }
                i += Character.charCount(cp2);
            }
            String piece = text.substring(start, i);
            g2.drawString(piece, x, y);
            x += fm.stringWidth(piece);
        }
    }

    /** 用当前的字体粗细、字号逐段绘制文字（缺字形自动换字体） */
    public static void draw(Graphics2D g2, String text, int x, int y) {
        Font cur = g2.getFont();
        drawRuns(g2, text, x, y, cur.getStyle(), cur.getSize());
    }

    /** 指定粗细和字号逐段绘制文字（缺字形自动换字体） */
    public static void draw(Graphics2D g2, String text, int x, int y, int style, int size) {
        drawRuns(g2, text, x, y, style, size);
    }

    /** 逐段测量文字宽度（和 {@link #draw} 的排版一致） */
    public static int width(Graphics2D g2, String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        Font cur = g2.getFont();
        int w = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            String family = familyForChar(cp);
            int start = i;
            i += Character.charCount(cp);
            while (i < text.length()) {
                int cp2 = text.codePointAt(i);
                if (!family.equals(familyForChar(cp2))) {
                    break;
                }
                i += Character.charCount(cp2);
            }
            Font f = new Font(family, cur.getStyle(), cur.getSize());
            w += g2.getFontMetrics(f).stringWidth(text.substring(start, i));
        }
        return w;
    }

    /** 按顺序找第一款能显示 text 里所有非 ASCII 字符的字体，都不行就挑覆盖最多的那款 */
    private static String bestDisplayable(String text) {
        for (String family : candidates()) {
            if (canDisplayAll(family, text)) {
                return family;
            }
        }
        String best = "Dialog";
        int bestScore = -1;
        for (String family : candidates()) {
            int score = countDisplayable(family, text);
            if (score > bestScore) {
                bestScore = score;
                best = family;
            }
        }
        return best;
    }

    private static boolean canDisplayAll(String family, String text) {
        Font probe = probe(family);
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp < 0x80 || Character.isISOControl(cp)) {
                continue;
            }
            if (!probe.canDisplay(cp)) {
                return false;
            }
        }
        return true;
    }

    private static int countDisplayable(String family, String text) {
        Font probe = probe(family);
        int n = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp < 0x80 || Character.isISOControl(cp)) {
                continue;
            }
            if (probe.canDisplay(cp)) {
                n++;
            }
        }
        return n;
    }

    private static boolean canDisplay(String family, int codePoint) {
        return probe(family).canDisplay(codePoint);
    }

    private static Font probe(String family) {
        Font probe = PROBES.get(family);
        if (probe == null) {
            probe = new Font(family, Font.PLAIN, 14);
            PROBES.put(family, probe);
        }
        return probe;
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
