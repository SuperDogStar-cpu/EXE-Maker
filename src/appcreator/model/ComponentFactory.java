package appcreator.model;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责在“设计画布”和“生成的代码”之间保持一致的组件工厂。
 */
public final class ComponentFactory {

    /** 供调色板显示的组件类型（顺序即显示顺序） */
    public static final String[] TYPES = {
            "标签", "按钮", "文本框", "文本域", "复选框",
            "单选框", "下拉框", "进度条", "滑块",
            "图片", "视频", "超链接"
    };

    /** 组件库分类：每组第一项是分类名，后面是该分类下的组件 */
    public static final String[][] GROUPS = {
            {"基础", "标签", "按钮", "文本框", "文本域", "复选框", "单选框", "下拉框", "进度条", "滑块"},
            {"图文", "图片", "视频"},
            {"链接", "超链接"}
    };

    /** 调色板小图标上的单字 */
    public static final char[] GLYPHS = {
            '标', '钮', '框', '域', '复', '单', '选', '进', '滑', '图', '视', '链'
    };

    private static final String[] CLASSES = {
            "JLabel", "JButton", "JTextField", "JTextArea", "JCheckBox",
            "JRadioButton", "JComboBox", "JProgressBar", "JSlider",
            "JLabel", "JLabel", "JLabel"
    };

    private static final String[] PREFIXES = {
            "label", "button", "textField", "textArea", "checkBox",
            "radioButton", "comboBox", "progressBar", "slider",
            "image", "video", "link"
    };

    private ComponentFactory() {
    }

    private static int indexOf(String type) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i;
            }
        }
        return 1;
    }

    public static String className(String type) {
        return CLASSES[indexOf(type)];
    }

    public static String prefix(String type) {
        return PREFIXES[indexOf(type)];
    }

    /** 新建组件时的默认尺寸 */
    public static Dimension defaultSize(String type) {
        if ("标签".equals(type)) {
            return new Dimension(96, 26);
        }
        if ("按钮".equals(type)) {
            return new Dimension(104, 34);
        }
        if ("文本框".equals(type)) {
            return new Dimension(160, 32);
        }
        if ("文本域".equals(type)) {
            return new Dimension(180, 90);
        }
        if ("复选框".equals(type) || "单选框".equals(type)) {
            return new Dimension(104, 26);
        }
        if ("下拉框".equals(type)) {
            return new Dimension(140, 32);
        }
        if ("进度条".equals(type)) {
            return new Dimension(160, 24);
        }
        if ("图片".equals(type)) {
            return new Dimension(200, 140);
        }
        if ("视频".equals(type)) {
            return new Dimension(240, 150);
        }
        if ("超链接".equals(type)) {
            return new Dimension(150, 26);
        }
        return new Dimension(180, 40); // 滑块
    }

    /** 新建组件时的默认文本 */
    public static String defaultText(String type) {
        if ("标签".equals(type)) {
            return "标签文本";
        }
        if ("按钮".equals(type)) {
            return "按钮";
        }
        if ("复选框".equals(type)) {
            return "复选框";
        }
        if ("单选框".equals(type)) {
            return "单选框";
        }
        if ("图片".equals(type)) {
            return "";
        }
        if ("视频".equals(type)) {
            return "视频";
        }
        if ("超链接".equals(type)) {
            return "打开网站";
        }
        return "";
    }

    /** 根据模型创建真实 Swing 组件（与生成的代码完全一致的控件类型） */
    public static JComponent create(Widget w) {
        JComponent c;
        if ("标签".equals(w.type)) {
            c = new JLabel();
        } else if ("按钮".equals(w.type)) {
            c = new JButton();
        } else if ("文本框".equals(w.type)) {
            JTextField f = new JTextField();
            f.setBorder(inputBorder(4, 8));
            c = f;
        } else if ("文本域".equals(w.type)) {
            JTextArea a = new JTextArea();
            a.setBorder(inputBorder(6, 9));
            c = a;
        } else if ("复选框".equals(w.type)) {
            c = new JCheckBox();
        } else if ("单选框".equals(w.type)) {
            c = new JRadioButton();
        } else if ("下拉框".equals(w.type)) {
            c = new JComboBox<String>();
        } else if ("进度条".equals(w.type)) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(60);
            c = bar;
        } else if ("滑块".equals(w.type)) {
            c = new JSlider(0, 100, 50);
        } else if ("图片".equals(w.type)) {
            JLabel img = new JLabel();
            img.setHorizontalAlignment(SwingConstants.CENTER);
            img.setVerticalAlignment(SwingConstants.CENTER);
            c = img;
        } else if ("视频".equals(w.type)) {
            JLabel video = new JLabel("", SwingConstants.CENTER);
            video.setOpaque(true);
            video.setBackground(new Color(0xEEF4F2));
            c = video;
        } else if ("超链接".equals(w.type)) {
            c = new JLabel();
        } else {
            c = new JLabel();
        }
        if (c instanceof JComponent) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        if ("视频".equals(w.type) || "超链接".equals(w.type)) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Widget widget = w;
            c.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() != 2) {
                        return;
                    }
                    if ("视频".equals(widget.type)) {
                        openFile(widget.path);
                    } else {
                        openUrl(widget.url);
                    }
                    e.consume();
                }
            });
        }
        // 设计态不参与焦点，避免文字外面出现虚线焦点框
        c.setFocusable(false);
        c.setRequestFocusEnabled(false);
        if (c instanceof AbstractButton) {
            ((AbstractButton) c).setFocusPainted(false);
        }
        apply(w, c);
        return c;
    }

    /** 输入框的浅色圆角边框，和生成的代码保持一致 */
    public static javax.swing.border.Border inputBorder(int topBottom, int leftRight) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xE3EBE8), 1, true),
                new EmptyBorder(topBottom, leftRight, topBottom, leftRight));
    }

    /** 把模型属性写回已有组件 */
    public static void apply(Widget w, JComponent c) {
        c.setBounds(w.x, w.y, w.w, w.h);
        c.setFont(new Font("微软雅黑", w.bold ? Font.BOLD : Font.PLAIN, w.fontSize));
        if (!(c instanceof JProgressBar) && !(c instanceof JSlider)) {
            c.setForeground(w.fg == null ? new Color(0x22312E) : w.fg);
        }

        if ("图片".equals(w.type) && c instanceof JLabel) {
            JLabel img = (JLabel) c;
            ImageIcon icon = loadScaled(w.path, w.w, w.h);
            img.setIcon(icon);
            img.setText(icon == null ? "图片（未选择）" : "");
            img.setFont(new Font("微软雅黑", Font.PLAIN, Math.min(14, w.fontSize)));
            img.setToolTipText(w.path == null || w.path.isEmpty() ? "在属性面板选择图片文件"
                    : w.path);
        } else if ("视频".equals(w.type) && c instanceof JLabel) {
            String name = fileName(w.path);
            JLabel video = (JLabel) c;
            video.setText("视频 · " + (name.isEmpty() ? "未选择文件" : name));
            // 播放徽标跟控件高度一起放大缩小
            int badge = Math.min(w.w, w.h) / 3;
            video.setIcon(badge >= 14 ? videoBadge(badge) : null);
            video.setToolTipText(name.isEmpty() ? "在属性面板选择视频文件"
                    : "双击用系统播放器打开：" + w.path);
        } else if ("超链接".equals(w.type) && c instanceof JLabel) {
            JLabel link = (JLabel) c;
            link.setText("<html><u>" + escapeHtml(w.text == null ? "" : w.text) + "</u></html>");
            link.setToolTipText(w.url == null || w.url.isEmpty() ? "在属性面板填写网址"
                    : "双击打开：" + w.url);
        } else if (c instanceof JLabel) {
            ((JLabel) c).setText(w.text);
        } else if (c instanceof AbstractButton) {
            ((AbstractButton) c).setText(w.text);
        } else if (c instanceof JTextField) {
            JTextField f = (JTextField) c;
            if (!f.getText().equals(w.text)) {
                f.setText(w.text);
            }
        } else if (c instanceof JTextArea) {
            JTextArea a = (JTextArea) c;
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
            if (!a.getText().equals(w.text)) {
                a.setText(w.text);
            }
        } else if (c instanceof JComboBox) {
            @SuppressWarnings("rawtypes")
            JComboBox box = (JComboBox) c;
            String[] items = splitItems(w.items);
            if (box.getItemCount() != items.length) {
                box.removeAllItems();
                for (String s : items) {
                    box.addItem(s);
                }
            }
        }

        if (w.bg != null) {
            c.setOpaque(true);
            c.setBackground(w.bg);
        } else if (c instanceof JTextArea) {
            ((JTextArea) c).setBackground(Color.WHITE);
        } else if (c instanceof JTextField) {
            ((JTextField) c).setBackground(Color.WHITE);
        } else if (c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setOpaque(false);
        }
    }

    /** 图片原图缓存：换尺寸时不用每次重新读盘（最多留 6 张） */
    private static final java.util.Map<String, java.awt.image.BufferedImage> IMG_CACHE =
            new java.util.LinkedHashMap<String, java.awt.image.BufferedImage>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, java.awt.image.BufferedImage> e) {
                    return size() > 6;
                }
            };

    /** 读原图（缓存） */
    private static java.awt.image.BufferedImage original(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        java.io.File f = new java.io.File(path);
        String key = f.getAbsolutePath() + "|" + f.lastModified() + "|" + f.length();
        synchronized (IMG_CACHE) {
            java.awt.image.BufferedImage cached = IMG_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        try {
            java.awt.image.BufferedImage img = ImageIO.read(f);
            if (img == null) {
                return null;
            }
            synchronized (IMG_CACHE) {
                IMG_CACHE.put(key, img);
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按控件尺寸等比缩放图片：保持原图宽高比，装满控件后在控件里居中（放大缩小都按同一比例）。
     * 读不到就返回 null。
     */
    public static ImageIcon loadScaled(String path, int w, int h) {
        java.awt.image.BufferedImage src = original(path);
        if (src == null) {
            return null;
        }
        int iw = src.getWidth();
        int ih = src.getHeight();
        if (iw <= 0 || ih <= 0) {
            return null;
        }
        int tw = Math.max(1, w);
        int th = Math.max(1, h);
        double scale = Math.min((double) tw / iw, (double) th / ih);
        int dw = Math.max(1, (int) Math.round(iw * scale));
        int dh = Math.max(1, (int) Math.round(ih * scale));
        if (dw == iw && dh == ih) {
            return new ImageIcon(src);
        }
        java.awt.image.BufferedImage out =
                new java.awt.image.BufferedImage(dw, dh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, dw, dh, null);
        g.dispose();
        return new ImageIcon(out);
    }

    /** 视频控件的播放徽标：尺寸跟着控件走，放大控件徽标也一起放大 */
    private static final java.util.Map<Integer, ImageIcon> BADGES =
            new java.util.HashMap<Integer, ImageIcon>();

    public static ImageIcon videoBadge(int size) {
        int s = Math.max(10, Math.min(96, size));
        synchronized (BADGES) {
            ImageIcon cached = BADGES.get(s);
            if (cached != null) {
                return cached;
            }
        }
        java.awt.image.BufferedImage out =
                new java.awt.image.BufferedImage(s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x39, 0xB5, 0x9E));
        int pad = s / 10;
        g.fillOval(pad, pad, s - 2 * pad, s - 2 * pad);
        g.setColor(Color.WHITE);
        int[] xs = {s * 4 / 10, s * 4 / 10, s * 68 / 100};
        int[] ys = {s * 30 / 100, s * 70 / 100, s / 2};
        g.fillPolygon(xs, ys, 3);
        g.dispose();
        ImageIcon icon = new ImageIcon(out);
        synchronized (BADGES) {
            BADGES.put(s, icon);
        }
        return icon;
    }


    public static String fileName(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        java.io.File f = new java.io.File(path);
        return f.getName();
    }

    public static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 双击用系统默认程序打开文件（视频等） */
    public static void openFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new java.io.File(path));
            }
        } catch (Exception ignored) {
            // 打不开就算了，不弹窗打断设计
        }
    }

    /** 双击用系统浏览器打开网址 */
    public static void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            String u = url.trim();
            if (!u.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
                u = "https://" + u;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new java.net.URI(u));
            }
        } catch (Exception ignored) {
            // 网址不合法时忽略
        }
    }

    public static String[] splitItems(String s) {
        if (s == null || s.trim().length() == 0) {
            return new String[]{"选项1"};
        }
        String[] raw = s.split("[,，]");
        List<String> list = new ArrayList<String>();
        for (String r : raw) {
            if (r.trim().length() > 0) {
                list.add(r.trim());
            }
        }
        if (list.isEmpty()) {
            list.add("选项1");
        }
        return list.toArray(new String[0]);
    }
}
