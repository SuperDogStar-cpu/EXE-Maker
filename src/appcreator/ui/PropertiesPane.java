package appcreator.ui;

import appcreator.gen.IconUtil;
import appcreator.model.Project;
import appcreator.model.Widget;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 右侧属性面板：窗口属性 + 选中组件属性。
 */
public class PropertiesPane extends JPanel {

    private final Runnable onWindowChanged;
    private final Consumer<Widget> onWidgetChanged;

    private Project project;
    private Widget current;
    private boolean updating;

    private final JTextField fTitle = Theme.field();
    private final JTextField fWinW = Theme.field();
    private final JTextField fWinH = Theme.field();
    private final ColorWell winBg = new ColorWell(true);
    private final JLabel iconPreview = new JLabel();
    private final RoundButton iconChoose = new RoundButton("选择", Theme.SOFT, Theme.ACCENT_DARK);
    private final RoundButton iconClear = new RoundButton("清除", new Color(0xFDF0F0), new Color(0xC0504D));

    private final JLabel typeLabel = Theme.label("—", 12, Theme.MUTED);
    private final JTextField fName = Theme.field();
    private final JTextField fText = Theme.field();
    private final JTextField fX = Theme.field();
    private final JTextField fY = Theme.field();
    private final JTextField fW = Theme.field();
    private final JTextField fH = Theme.field();
    private final JTextField fSize = Theme.field();
    private final JCheckBox cBold = new JCheckBox("加粗");
    private final ColorWell fgWell = new ColorWell(false);
    private final ColorWell bgWell = new ColorWell(true);
    private final JTextField fItems = Theme.field();
    private final JTextField fPath = Theme.field();
    private final RoundButton fPathChoose = new RoundButton("选择", Theme.SOFT, Theme.ACCENT_DARK);
    private final RoundButton fPathClear = new RoundButton("清除", new Color(0xFDF0F0), new Color(0xC0504D));
    private final JTextField fUrl = Theme.field();
    private JComponent pathRow;
    private JComponent urlRow;

    private final List<JComponent> widgetInputs = new ArrayList<JComponent>();
    private JLabel emptyHint;

    public PropertiesPane(Runnable onWindowChanged, Consumer<Widget> onWidgetChanged) {
        this.onWindowChanged = onWindowChanged;
        this.onWidgetChanged = onWidgetChanged;
        setOpaque(true);
        setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // 面板宽度上限：德语、法语这类长标签不会把整个属性面板撑出窗口
        JPanel content = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.min(d.width, 296), d.height);
            }
        };
        content.setOpaque(true);
        content.setBackground(Theme.BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 长文本（网址 / 文件路径 / 下拉项）不撑宽面板，超长内容用鼠标悬停提示
        Dimension fieldSize = new Dimension(150, 28);
        fTitle.setPreferredSize(fieldSize);
        fName.setPreferredSize(fieldSize);
        fText.setPreferredSize(fieldSize);
        fItems.setPreferredSize(fieldSize);
        fUrl.setPreferredSize(fieldSize);
        fPath.setPreferredSize(new Dimension(130, 28));

        content.add(buildWindowCard());
        content.add(Box.createVerticalStrut(12));
        content.add(buildWidgetCard());
        content.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        wireWindow();
        wireWidget();
        bind(null, null);
    }

    // ---------------- 构建界面 ----------------

    private JComponent buildWindowCard() {
        RoundPanel card = new RoundPanel(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 14, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = Theme.label("窗口", 13, Theme.ACCENT_DARK);
        title.setFont(Theme.ui(13, Font.BOLD));
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        card.add(row("标题", fTitle));
        card.add(pairRow("尺寸", fWinW, fWinH));
        card.add(row("背景色", winBg));
        winBg.setToolTipText("窗口背景色，点击选择");

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        iconRow.setOpaque(false);
        iconPreview.setPreferredSize(new Dimension(26, 26));
        iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        iconPreview.setBorder(BorderFactory.createLineBorder(new Color(0xE3EBE8), 1, true));
        iconPreview.setOpaque(true);
        iconPreview.setBackground(Color.WHITE);
        iconPreview.setToolTipText("窗口图标（也会写进导出的 EXE）");
        iconChoose.setPreferredSize(new Dimension(52, 26));
        iconClear.setPreferredSize(new Dimension(46, 26));
        iconChoose.setBorder(new EmptyBorder(2, 6, 2, 6));
        iconClear.setBorder(new EmptyBorder(2, 6, 2, 6));
        iconChoose.setFont(Theme.ui(11));
        iconClear.setFont(Theme.ui(11));
        iconRow.add(iconPreview);
        iconRow.add(iconChoose);
        iconRow.add(iconClear);
        card.add(row("图标", iconRow));
        return card;
    }

    private JComponent buildWidgetCard() {
        RoundPanel card = new RoundPanel(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 14, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = Theme.label("组件属性", 13, Theme.ACCENT_DARK);
        title.setFont(Theme.ui(13, Font.BOLD));
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        card.add(row("类型", typeLabel));
        card.add(row("名称", fName));
        card.add(row("文本", fText));
        card.add(pairRow("位置", fX, fY));
        card.add(pairRow("尺寸", fW, fH));
        card.add(pairRow("字号", fSize, cBold));
        card.add(row("前景色", fgWell));
        card.add(row("背景色", bgWell));
        card.add(row("下拉项", fItems));

        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        pathPanel.setOpaque(false);
        fPathChoose.setPreferredSize(new Dimension(46, 26));
        fPathChoose.setFont(Theme.ui(11));
        fPathChoose.setBorder(new EmptyBorder(2, 6, 2, 6));
        fPathClear.setPreferredSize(new Dimension(46, 26));
        fPathClear.setFont(Theme.ui(11));
        fPathClear.setBorder(new EmptyBorder(2, 6, 2, 6));
        pathPanel.add(fPath, BorderLayout.CENTER);
        JPanel pathButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pathButtons.setOpaque(false);
        pathButtons.add(fPathChoose);
        pathButtons.add(fPathClear);
        pathPanel.add(pathButtons, BorderLayout.EAST);
        fPath.setEditable(false);
        fPath.setToolTipText("图片 / 视频文件路径，点「选择」挑一个");
        pathRow = row("文件", pathPanel);
        card.add(pathRow);

        fUrl.setToolTipText("例：https://www.example.com");
        urlRow = row("网址", fUrl);
        card.add(urlRow);
        pathRow.setVisible(false);
        urlRow.setVisible(false);

        cBold.setOpaque(false);
        cBold.setFont(Theme.ui(12));
        cBold.setForeground(Theme.MUTED);
        fgWell.setToolTipText("文字颜色");
        bgWell.setToolTipText("背景色（右键恢复默认 / 透明）");
        fItems.setToolTipText("用逗号分隔，例如：选项1, 选项2, 选项3");

        emptyHint = Theme.label("在画布中选中一个组件，即可在此编辑属性", 11, Theme.MUTED);
        emptyHint.setBorder(new EmptyBorder(4, 0, 0, 0));
        card.add(emptyHint);

        widgetInputs.add(fName);
        widgetInputs.add(fText);
        widgetInputs.add(fX);
        widgetInputs.add(fY);
        widgetInputs.add(fW);
        widgetInputs.add(fH);
        widgetInputs.add(fSize);
        widgetInputs.add(cBold);
        widgetInputs.add(fgWell);
        widgetInputs.add(bgWell);
        widgetInputs.add(fItems);
        widgetInputs.add(fPath);
        widgetInputs.add(fUrl);
        return card;
    }

    private JComponent row(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = Theme.autoLabel(label, 12, Theme.MUTED);
        p.add(l, BorderLayout.WEST);
        p.add(comp, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.setBorder(new EmptyBorder(2, 0, 2, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JComponent pairRow(String label, JComponent a, JComponent b) {
        JPanel inner = new JPanel(new GridLayout(1, 2, 6, 0));
        inner.setOpaque(false);
        inner.add(a);
        inner.add(b);
        return row(label, inner);
    }

    // ---------------- 事件绑定 ----------------

    private void wireWindow() {
        doc(fTitle, new Runnable() {
            public void run() {
                if (project != null) {
                    project.title = fTitle.getText();
                    if (onWindowChanged != null) {
                        onWindowChanged.run();
                    }
                }
            }
        });
        doc(fWinW, new Runnable() {
            public void run() {
                int v = num(fWinW.getText(), -1);
                if (project != null && v >= 320 && v <= 3000) {
                    project.width = v;
                    if (onWindowChanged != null) {
                        onWindowChanged.run();
                    }
                }
            }
        });
        doc(fWinH, new Runnable() {
            public void run() {
                int v = num(fWinH.getText(), -1);
                if (project != null && v >= 240 && v <= 3000) {
                    project.height = v;
                    if (onWindowChanged != null) {
                        onWindowChanged.run();
                    }
                }
            }
        });
        winBg.setOnChange(new Runnable() {
            public void run() {
                if (project != null && winBg.getColor() != null) {
                    project.bg = winBg.getColor();
                    if (onWindowChanged != null) {
                        onWindowChanged.run();
                    }
                }
            }
        });

        iconChoose.addActionListener(e -> chooseIcon());
        iconClear.addActionListener(e -> {
            if (project != null && project.iconPng != null) {
                project.iconPng = null;
                project.iconName = "";
                refreshIcon();
                if (onWindowChanged != null) {
                    onWindowChanged.run();
                }
            }
        });
        refreshIcon();
    }

    // ---------------- 自定义图标 ----------------

    /** 选择图片作为窗口图标 / EXE 图标 */
    private void chooseIcon() {
        if (project == null) {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择窗口图标（建议 256×256 的 PNG）");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String n = f.getName().toLowerCase();
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".gif") || n.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "图片文件（PNG / JPG / GIF / BMP）";
            }
        });
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File f = fc.getSelectedFile();
        try {
            byte[] png = IconUtil.fromFile(f);
            project.iconPng = png;
            project.iconName = f.getName();
            refreshIcon();
            if (onWindowChanged != null) {
                onWindowChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "读取图标失败：\n" + ex.getMessage(),
                    "自定义图标", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** 刷新左上角的图标预览 */
    private void refreshIcon() {
        byte[] data = project == null ? null : project.iconPng;
        if (data == null || data.length == 0) {
            iconPreview.setIcon(null);
            iconPreview.setText("无");
            iconPreview.setFont(Theme.ui(10));
            iconPreview.setForeground(Theme.MUTED);
            iconPreview.setToolTipText("窗口图标（也会写进导出的 EXE）");
        } else {
            Image img = IconUtil.image(data, 24);
            iconPreview.setText("");
            iconPreview.setIcon(img == null ? null : new ImageIcon(img));
            iconPreview.setToolTipText("当前图标：" + (project.iconName == null ? "" : project.iconName)
                    + "\n也会写进导出的 EXE；点击「清除」恢复默认");
        }
    }

    private void wireWidget() {
        Runnable apply = new Runnable() {
            public void run() {
                applyWidget();
            }
        };
        JTextField[] fields = {fName, fText, fX, fY, fW, fH, fSize, fItems, fUrl};
        for (JTextField f : fields) {
            doc(f, apply);
        }
        fPathChoose.addActionListener(e -> chooseWidgetFile());
        fPathClear.addActionListener(e -> {
            if (current != null) {
                current.path = "";
                updating = true;
                fPath.setText("");
                updating = false;
                fire();
            }
        });
        cBold.addActionListener(e -> apply.run());
        fgWell.setOnChange(new Runnable() {
            public void run() {
                if (current != null) {
                    current.fg = fgWell.getColor() == null ? new Color(0x22312E) : fgWell.getColor();
                    fire();
                }
            }
        });
        bgWell.setOnChange(new Runnable() {
            public void run() {
                if (current != null) {
                    current.bg = bgWell.getColor();
                    fire();
                }
            }
        });
    }

    private void fire() {
        if (onWidgetChanged != null && current != null) {
            onWidgetChanged.accept(current);
        }
    }

    private void doc(final JTextField field, final Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!updating) {
                    action.run();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!updating) {
                    action.run();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!updating) {
                    action.run();
                }
            }
        });
    }

    private void applyWidget() {
        if (current == null || updating) {
            return;
        }
        String nm = sanitizeName(fName.getText(), current.name);
        current.name = nm;
        if (!fName.getText().equals(nm)) {
            updating = true;
            fName.setText(nm);
            updating = false;
        }
        current.text = fText.getText();
        current.x = Math.max(0, num(fX.getText(), current.x));
        current.y = Math.max(0, num(fY.getText(), current.y));
        current.w = Math.max(20, num(fW.getText(), current.w));
        current.h = Math.max(16, num(fH.getText(), current.h));
        int fs = num(fSize.getText(), current.fontSize);
        current.fontSize = Math.max(8, Math.min(72, fs));
        current.bold = cBold.isSelected();
        current.items = fItems.getText();
        current.url = fUrl.getText().trim();
        fire();
    }

    /** 选择图片 / 视频文件（双击在新窗口里用系统程序打开） */
    private void chooseWidgetFile() {
        if (current == null) {
            return;
        }
        final boolean video = "视频".equals(current.type);
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(video ? "选择视频文件" : "选择图片文件");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String n = f.getName().toLowerCase();
                if (video) {
                    return n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".wmv")
                            || n.endsWith(".mov") || n.endsWith(".mkv") || n.endsWith(".flv");
                }
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".gif") || n.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return video ? "视频文件（MP4 / AVI / WMV / MOV / MKV / FLV）"
                        : "图片文件（PNG / JPG / GIF / BMP）";
            }
        });
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        current.path = fc.getSelectedFile().getAbsolutePath();
        updating = true;
        fPath.setText(current.path);
        updating = false;
        fire();
    }

    private static String sanitizeName(String raw, String fallback) {
        String s = raw.trim().replaceAll("[^A-Za-z0-9_$]", "");
        if (s.length() == 0) {
            return fallback;
        }
        if (Character.isDigit(s.charAt(0))) {
            s = "c" + s;
        }
        return s;
    }

    private static int num(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // ---------------- 绑定数据 ----------------

    public void bind(Project p, Widget w) {
        project = p;
        current = w;
        updating = true;

        if (p != null) {
            fTitle.setText(p.title == null ? "" : p.title);
            fWinW.setText(String.valueOf(p.width));
            fWinH.setText(String.valueOf(p.height));
            winBg.setColor(p.bg);
            refreshIcon();
        }

        boolean has = w != null;
        for (JComponent c : widgetInputs) {
            c.setEnabled(has);
        }
        if (emptyHint != null) {
            emptyHint.setVisible(!has);
        }

        if (has) {
            typeLabel.setText(w.type);
            fName.setText(w.name);
            fText.setText(w.text == null ? "" : w.text);
            fX.setText(String.valueOf(w.x));
            fY.setText(String.valueOf(w.y));
            fW.setText(String.valueOf(w.w));
            fH.setText(String.valueOf(w.h));
            fSize.setText(String.valueOf(w.fontSize));
            cBold.setSelected(w.bold);
            fgWell.setColor(w.fg);
            bgWell.setColor(w.bg);
            fItems.setText(w.items == null ? "" : w.items);
            fItems.setEnabled("下拉框".equals(w.type));
            fPath.setText(w.path == null ? "" : w.path);
            fUrl.setText(w.url == null ? "" : w.url);
            boolean media = "图片".equals(w.type) || "视频".equals(w.type);
            if (pathRow != null) {
                pathRow.setVisible(media);
            }
            if (urlRow != null) {
                urlRow.setVisible("超链接".equals(w.type));
            }
        } else {
            typeLabel.setText("—");
            fName.setText("");
            fText.setText("");
            fX.setText("");
            fY.setText("");
            fW.setText("");
            fH.setText("");
            fSize.setText("");
            cBold.setSelected(false);
            fgWell.setColor(null);
            bgWell.setColor(null);
            fItems.setText("");
            fPath.setText("");
            fUrl.setText("");
            if (pathRow != null) {
                pathRow.setVisible(false);
            }
            if (urlRow != null) {
                urlRow.setVisible(false);
            }
        }
        updating = false;
        if (pathRow != null) {
            pathRow.getParent().revalidate();
            pathRow.getParent().repaint();
        }
    }

    /** 画布拖动后，只刷新几何字段 */
    public void refreshGeometry() {
        if (current == null) {
            return;
        }
        updating = true;
        fX.setText(String.valueOf(current.x));
        fY.setText(String.valueOf(current.y));
        fW.setText(String.valueOf(current.w));
        fH.setText(String.valueOf(current.h));
        updating = false;
    }

    // ---------------- 颜色选择控件 ----------------

    private static class ColorWell extends JComponent {

        private Color color;
        private final boolean nullable;
        private boolean hover;
        private Runnable onChange;

        ColorWell(boolean nullable) {
            this.nullable = nullable;
            setPreferredSize(new Dimension(118, 26));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) {
                        return;
                    }
                    if (e.getButton() == MouseEvent.BUTTON3 && nullable) {
                        color = null;
                        if (onChange != null) {
                            onChange.run();
                        }
                        repaint();
                        return;
                    }
                    Color c = JColorChooser.showDialog(ColorWell.this, "选择颜色",
                            color == null ? Color.WHITE : color);
                    if (c != null) {
                        color = c;
                        if (onChange != null) {
                            onChange.run();
                        }
                        repaint();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setOnChange(Runnable r) {
            this.onChange = r;
        }

        Color getColor() {
            return color;
        }

        void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // 浅底卡片：左边一个颜色小方块，右边写色值，避免文字压在深色上像被划掉
            g2.setColor(isEnabled() ? Color.WHITE : new Color(0xF7FAF9));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g2.setColor(hover && isEnabled() ? Theme.ACCENT : Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            int sw = 20;
            int sh = 14;
            int sx = 7;
            int sy = (h - sh) / 2;
            g2.setColor(color == null ? new Color(0xF1F5F4) : color);
            g2.fillRoundRect(sx, sy, sw, sh, 5, 5);
            g2.setColor(new Color(0xD8E3DF));
            g2.drawRoundRect(sx, sy, sw, sh, 5, 5);
            if (color == null && nullable) {
                // 半透明斜线表示"默认/透明"
                g2.setColor(new Color(0xC7D3CF));
                g2.drawLine(sx + 3, sy + sh - 3, sx + sw - 3, sy + 3);
            }

            String txt = color == null ? "默认" : String.format("#%06X", color.getRGB() & 0xFFFFFF);
            g2.setColor(isEnabled() ? Theme.TEXT : Theme.MUTED);
            g2.setFont(new Font(Fonts.familyForText(txt), Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            Fonts.draw(g2, txt, sx + sw + 7, (h - fm.getHeight()) / 2 + fm.getAscent(), Font.PLAIN, 11);
            g2.dispose();
        }
    }
}
