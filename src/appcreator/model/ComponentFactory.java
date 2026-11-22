package appcreator.model;

import appcreator.io.ComponentData;
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
        ComponentData.Entry e = ComponentData.get(type);
        if (e != null) {
            return extraClass(e.kind);
        }
        return CLASSES[indexOf(type)];
    }

    public static String prefix(String type) {
        ComponentData.Entry e = ComponentData.get(type);
        if (e != null) {
            return extraPrefix(e.kind);
        }
        return PREFIXES[indexOf(type)];
    }

    /** 下载数据里的组件：生成的代码用的 Swing 类名 */
    public static String extraClass(String kind) {
        if ("button".equals(kind)) {
            return "JButton";
        }
        if ("label".equals(kind) || "linklabel".equals(kind) || "menubar".equals(kind)
                || "statusbar".equals(kind) || "nonvisual".equals(kind) || "folder".equals(kind)
                || "color".equals(kind)) {
            return "JLabel";
        }
        if ("textfield".equals(kind) || "datetime".equals(kind)) {
            return "JTextField";
        }
        if ("textarea".equals(kind)) {
            return "JTextArea";
        }
        if ("checkbox".equals(kind)) {
            return "JCheckBox";
        }
        if ("radio".equals(kind)) {
            return "JRadioButton";
        }
        if ("combo".equals(kind)) {
            return "JComboBox";
        }
        if ("list".equals(kind) || "checkedlist".equals(kind)) {
            return "JList";
        }
        if ("spinner".equals(kind)) {
            return "JSpinner";
        }
        if ("progress".equals(kind)) {
            return "JProgressBar";
        }
        if ("slider".equals(kind)) {
            return "JSlider";
        }
        if ("scrollbar-h".equals(kind) || "scrollbar-v".equals(kind)) {
            return "JScrollBar";
        }
        if ("table".equals(kind)) {
            return "JTable";
        }
        if ("tree".equals(kind)) {
            return "JTree";
        }
        if ("tabs".equals(kind)) {
            return "JTabbedPane";
        }
        if ("split".equals(kind)) {
            return "JSplitPane";
        }
        if ("group".equals(kind) || "panel".equals(kind) || "toolbar".equals(kind)
                || "calendar".equals(kind)) {
            return "JPanel";
        }
        return "JLabel";
    }

    /** 下载数据里的组件：变量名前缀 */
    public static String extraPrefix(String kind) {
        if ("button".equals(kind)) {
            return "button";
        }
        if ("textfield".equals(kind) || "datetime".equals(kind)) {
            return "textField";
        }
        if ("textarea".equals(kind)) {
            return "richText";
        }
        if ("checkbox".equals(kind)) {
            return "checkBox";
        }
        if ("radio".equals(kind)) {
            return "radioButton";
        }
        if ("combo".equals(kind)) {
            return "comboBox";
        }
        if ("list".equals(kind)) {
            return "list";
        }
        if ("checkedlist".equals(kind)) {
            return "checkedList";
        }
        if ("spinner".equals(kind)) {
            return "numeric";
        }
        if ("progress".equals(kind)) {
            return "progressBar";
        }
        if ("slider".equals(kind)) {
            return "trackBar";
        }
        if ("scrollbar-h".equals(kind)) {
            return "hScrollBar";
        }
        if ("scrollbar-v".equals(kind)) {
            return "vScrollBar";
        }
        if ("table".equals(kind)) {
            return "dataGrid";
        }
        if ("tree".equals(kind)) {
            return "treeView";
        }
        if ("tabs".equals(kind)) {
            return "tabControl";
        }
        if ("split".equals(kind)) {
            return "splitContainer";
        }
        if ("group".equals(kind)) {
            return "groupBox";
        }
        if ("panel".equals(kind)) {
            return "panel";
        }
        if ("toolbar".equals(kind)) {
            return "toolStrip";
        }
        if ("statusbar".equals(kind)) {
            return "statusStrip";
        }
        if ("menubar".equals(kind)) {
            return "menuStrip";
        }
        if ("calendar".equals(kind)) {
            return "monthCalendar";
        }
        if ("linklabel".equals(kind)) {
            return "linkLabel";
        }
        if ("nonvisual".equals(kind) || "folder".equals(kind) || "color".equals(kind)) {
            return "component";
        }
        return "label";
    }

    /** 新建组件时的默认尺寸 */
    public static Dimension defaultSize(String type) {
        ComponentData.Entry e = ComponentData.get(type);
        if (e != null) {
            return new Dimension(e.w, e.h);
        }
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
        ComponentData.Entry e = ComponentData.get(type);
        if (e != null) {
            return e.text;
        }
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
        ComponentData.Entry entry = ComponentData.get(w.type);
        if (entry != null) {
            c = createExtra(entry, w);
        } else if ("标签".equals(w.type)) {
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

    /** 输入框：不要边框，只用内边距保证文字不贴边（和生成的代码保持一致） */
    public static javax.swing.border.Border inputBorder(int topBottom, int leftRight) {
        return new EmptyBorder(topBottom, leftRight, topBottom, leftRight);
    }

    // ------------------------------------------------------------------
    // 下载数据里的 .NET 工具箱组件（Panel、DataGridView、TreeView、MenuStrip…）
    // 设计器和生成的代码用同一套 Swing 对应实现
    // ------------------------------------------------------------------

    private static JComponent createExtra(ComponentData.Entry e, Widget w) {
        String k = e.kind;
        String text = w.text == null ? "" : w.text;
        if ("button".equals(k)) {
            return new JButton();
        }
        if ("label".equals(k)) {
            return new JLabel();
        }
        if ("linklabel".equals(k)) {
            JLabel l = new JLabel();
            l.setForeground(new Color(0x1A73E8));
            return l;
        }
        if ("textfield".equals(k) || "datetime".equals(k)) {
            JTextField f = new JTextField();
            f.setBorder(inputBorder(4, 8));
            return f;
        }
        if ("textarea".equals(k)) {
            JTextArea a = new JTextArea();
            a.setBorder(inputBorder(6, 9));
            return a;
        }
        if ("checkbox".equals(k)) {
            return new JCheckBox();
        }
        if ("radio".equals(k)) {
            return new JRadioButton();
        }
        if ("combo".equals(k)) {
            return new JComboBox<String>();
        }
        if ("list".equals(k)) {
            return wrapScroll(new JList<String>(new String[]{"项目 1", "项目 2", "项目 3"}));
        }
        if ("checkedlist".equals(k)) {
            return wrapScroll(new JList<String>(new String[]{"选项 1", "选项 2", "选项 3"}));
        }
        if ("spinner".equals(k)) {
            return new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        }
        if ("progress".equals(k)) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(60);
            return bar;
        }
        if ("slider".equals(k)) {
            return new JSlider(0, 100, 50);
        }
        if ("scrollbar-h".equals(k)) {
            return new JScrollBar(Adjustable.HORIZONTAL);
        }
        if ("scrollbar-v".equals(k)) {
            return new JScrollBar(Adjustable.VERTICAL);
        }
        if ("table".equals(k)) {
            JTable t = new JTable(new Object[][]{{"行 1", 1}, {"行 2", 2}},
                    new String[]{"名称", "数值"});
            t.setRowHeight(24);
            return wrapScroll(t);
        }
        if ("tree".equals(k)) {
            javax.swing.tree.DefaultMutableTreeNode root =
                    new javax.swing.tree.DefaultMutableTreeNode("根节点");
            root.add(new javax.swing.tree.DefaultMutableTreeNode("节点 1"));
            root.add(new javax.swing.tree.DefaultMutableTreeNode("节点 2"));
            JTree t = new JTree(root);
            t.setRowHeight(22);
            for (int i = 0; i < t.getRowCount(); i++) {
                t.expandRow(i);
            }
            return wrapScroll(t);
        }
        if ("tabs".equals(k)) {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("选项卡 1", new JPanel());
            tabs.addTab("选项卡 2", new JPanel());
            return tabs;
        }
        if ("split".equals(k)) {
            JPanel left = new JPanel();
            left.setBackground(new Color(0xF7FBFA));
            JPanel right = new JPanel();
            right.setBackground(new Color(0xFFFFFF));
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setDividerLocation(0.45);
            return split;
        }
        if ("panel".equals(k)) {
            JPanel p = new JPanel();
            p.setBackground(new Color(0xF4FAF8));
            p.setBorder(BorderFactory.createEmptyBorder());
            return p;
        }
        if ("group".equals(k)) {
            JPanel p = new JPanel();
            p.setBackground(Color.WHITE);
            // 只要标题，不画外框（TitledBorder 传 null 会退回默认线框，所以给个空边框）
            p.setBorder(BorderFactory.createTitledBorder(new EmptyBorder(0, 0, 0, 0),
                    text.isEmpty() ? "分组框" : text));
            return p;
        }
        if ("toolbar".equals(k)) {
            JToolBar bar = new JToolBar();
            bar.setFloatable(false);
            bar.add(new JButton("新建"));
            bar.add(new JButton("保存"));
            return bar;
        }
        if ("statusbar".equals(k)) {
            JLabel l = new JLabel(text.isEmpty() ? "就绪" : text);
            l.setOpaque(true);
            l.setBackground(new Color(0xF0F4F3));
            l.setBorder(new EmptyBorder(0, 8, 0, 0));
            return l;
        }
        if ("menubar".equals(k)) {
            JLabel l = new JLabel(text.isEmpty() ? "文件  编辑  视图" : text);
            l.setOpaque(true);
            l.setBackground(new Color(0xF0F4F3));
            l.setBorder(new EmptyBorder(0, 10, 0, 0));
            return l;
        }
        if ("calendar".equals(k)) {
            return new CalendarPanel();
        }
        // 颜色 / 文件夹 / 计时器这类没有可视化外观的，画成一个小标签块
        JLabel chip = new JLabel(text.isEmpty() ? e.name : text, SwingConstants.CENTER);
        chip.setOpaque(true);
        chip.setBackground(new Color(0xEEF4F2));
        chip.setForeground(new Color(0x2C7A6B));
        chip.setBorder(new EmptyBorder(3, 9, 3, 9));
        chip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        return chip;
    }

    private static JComponent wrapScroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** 月历：表头 + 星期行 + 6 行日期格子（和生成的代码画法一致） */
    public static class CalendarPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(0x22312E));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 13));
            g2.drawString("2026 年 1 月", 12, 22);
            g2.setColor(new Color(0x8A9B97));
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            String[] week = {"日", "一", "二", "三", "四", "五", "六"};
            for (int i = 0; i < 7; i++) {
                g2.drawString(week[i], 12 + i * (w - 24) / 7, 44);
            }
            g2.setColor(new Color(0xDCE7E4));
            g2.drawLine(8, 50, w - 8, 50);
            g2.setColor(new Color(0x3A4A46));
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            int startX = 12;
            int startY = 72;
            int cellW = Math.max(12, (w - 24) / 7);
            int cellH = Math.max(12, (h - 84) / 5);
            int day = 1;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 7; col++) {
                    if (day > 31) {
                        break;
                    }
                    g2.drawString(String.valueOf(day), startX + col * cellW,
                            startY + row * cellH);
                    day++;
                }
            }
            g2.dispose();
        }
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
