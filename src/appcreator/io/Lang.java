package appcreator.io;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 界面语言。内置简体中文；用 .japa 语言包可以替换内置界面文字。
 *
 * .japa 是 UTF-8 纯文本，一行一条「原文=译文」，井号开头是注释：
 * <pre>
 * #name=English
 * 组件库=Palette
 * 新建=New
 * </pre>
 * 文件名不需要改，叫什么都能被识别（认的是 .japa 后缀）。
 */
public final class Lang {

    public static final String DEFAULT_NAME = "简体中文";
    private static final String CONFIG_NAME = "应用创作器.lang.properties";

    private static final Map<String, String> map = new LinkedHashMap<String, String>();
    private static final Map<Component, String> origins = new WeakHashMap<Component, String>();
    private static String currentName = DEFAULT_NAME;
    private static File currentFile;
    private static File projectDir;

    private Lang() {
    }

    public static String name() {
        return currentName;
    }

    public static File file() {
        return currentFile;
    }

    public static boolean isDefault() {
        return currentFile == null;
    }

    /** 翻译一段内置文字；没在语言包里就原样返回 */
    public static String t(String s) {
        if (s == null) {
            return null;
        }
        String v = map.get(s);
        return v == null ? s : v;
    }

    public static void setProjectDir(File dir) {
        projectDir = dir;
    }

    private static File configFileParent;

    public static File configFile() {
        File base = configFileParent != null ? configFileParent
                : new File(System.getProperty("user.home"));
        return new File(base, CONFIG_NAME);
    }

    // ---------------- 语言包 ----------------

    public static void useDefault() {
        map.clear();
        currentName = DEFAULT_NAME;
        currentFile = null;
        saveSettings();
    }

    public static void use(File japa) throws IOException {
        Map<String, String> loaded = parse(japa);
        map.clear();
        map.putAll(loaded);
        currentFile = japa;
        currentName = displayName(japa);
        saveSettings();
    }

    /** 自动恢复上次设置的语言包 */
    public static void loadSaved() {
        try {
            File cfg = configFile();
            if (!cfg.isFile()) {
                return;
            }
            java.util.Properties p = new java.util.Properties();
            Reader r = new InputStreamReader(new FileInputStream(cfg), "UTF-8");
            p.load(r);
            r.close();
            String path = p.getProperty("file");
            if (path == null || path.trim().isEmpty()) {
                return;
            }
            File f = new File(path.trim());
            if (f.isFile()) {
                use(f);
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveSettings() {
        try {
            File cfg = configFile();
            File parent = cfg.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.util.Properties p = new java.util.Properties();
            p.setProperty("file", currentFile == null ? "" : currentFile.getAbsolutePath());
            p.setProperty("name", currentName);
            Writer w = new OutputStreamWriter(new FileOutputStream(cfg), "UTF-8");
            p.store(w, "应用创作器 语言设置");
            w.close();
        } catch (Exception ignored) {
        }
    }

    /** 语言包显示名：优先取 #name=，否则用文件名（文件名保持原样，不改） */
    public static String displayName(File f) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (s.length() == 0) {
                    continue;
                }
                if (s.startsWith("#")) {
                    String low = s.toLowerCase();
                    if (low.startsWith("#name")) {
                        int eq = s.indexOf('=');
                        if (eq > 0) {
                            r.close();
                            return s.substring(eq + 1).trim();
                        }
                    }
                    continue;
                }
                break;
            }
            r.close();
        } catch (Exception ignored) {
        }
        String n = f.getName();
        return n.toLowerCase().endsWith(".japa") ? n.substring(0, n.length() - 5) : n;
    }

    private static Map<String, String> parse(File f) throws IOException {
        Map<String, String> out = new LinkedHashMap<String, String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            String s = line;
            if (s.length() == 0 || s.trim().startsWith("#")) {
                continue;
            }
            int eq = s.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = s.substring(0, eq).trim();
            String val = s.substring(eq + 1).trim();
            if (key.length() > 0) {
                out.put(key, val);
            }
        }
        r.close();
        return out;
    }

    /** 找程序目录、用户目录、当前项目目录下的所有 .japa（认后缀，文件名随意） */
    public static List<File> discover() {
        List<File> out = new ArrayList<File>();
        List<File> dirs = new ArrayList<File>();
        dirs.add(programDir());
        dirs.add(new File(System.getProperty("user.dir")));
        dirs.add(new File(System.getProperty("user.home")));
        if (projectDir != null) {
            dirs.add(projectDir);
        }
        for (File dir : dirs) {
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".japa") && !out.contains(f)) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    public static File programDir() {
        try {
            File jar = new File(Lang.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return jar.isFile() ? jar.getParentFile() : jar;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    // ---------------- 界面文字替换 ----------------

    /** 把整棵界面树上「内置文字」换成语言包里的译文（先记住原文，切回中文能换回来） */
    public static void applyTree(Component root) {
        if (root == null) {
            return;
        }
        if (root instanceof JLabel) {
            JLabel l = (JLabel) root;
            if (l.getText() != null && !l.getText().startsWith("<html>")) {
                l.setText(langText(l, l.getText()));
            }
        } else if (root instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) root;
            if (b.getText() != null) {
                b.setText(langText(b, b.getText()));
            }
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                applyTree(child);
            }
        }
        if (root instanceof JComponent) {
            JComponent c = (JComponent) root;
            String tip = c.getToolTipText();
            if (tip != null && tip.length() > 0) {
                c.setToolTipText(map.containsKey(tip) ? map.get(tip) : tip);
            }
        }
    }

    private static String langText(JComponent c, String now) {
        String orig = origins.get(c);
        if (orig == null) {
            orig = now;
            origins.put(c, orig);
        }
        return map.containsKey(orig) ? map.get(orig) : orig;
    }
}
