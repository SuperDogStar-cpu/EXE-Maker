package appcreator.gen;

import appcreator.model.ComponentFactory;
import appcreator.io.ComponentData;
import appcreator.model.Project;
import appcreator.model.Widget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把项目模型翻译成一份可以直接编译运行的 AppMain.java。
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String generate(Project p) {
        List<String> names = uniqueNames(p);
        StringBuilder sb = new StringBuilder();
        sb.append("import javax.swing.*;\n");
        sb.append("import java.awt.*;\n");
        sb.append("import java.awt.event.*;\n");
        sb.append("import java.io.File;\n");
        sb.append("import java.net.URI;\n\n");
        sb.append("/**\n");
        sb.append(" * 由「应用创作器」生成。\n");
        sb.append(" */\n");
        sb.append("public class AppMain {\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        SwingUtilities.invokeLater(new Runnable() {\n");
        sb.append("            public void run() {\n");
        sb.append("                new AppMain().buildUI();\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("    }\n\n");
        sb.append("    private void buildUI() {\n");
        sb.append("        JFrame frame = new JFrame(").append(quote(p.title)).append(");\n");
        sb.append("        // 预览时只关闭当前窗口（避免把创作器一起关掉）；独立运行时关闭窗口即退出程序\n");
        sb.append("        if (Boolean.getBoolean(\"appcreator.preview\")) {\n");
        sb.append("            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n");
        sb.append("        } else {\n");
        sb.append("            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");
        sb.append("        }\n");
        sb.append("        frame.setSize(").append(p.width).append(", ").append(p.height).append(");\n");
        appendIcon(sb, p);
        sb.append("        JPanel root = new JPanel(null);\n");
        sb.append("        root.setBackground(").append(color(p.bg)).append(");\n");
        sb.append("        frame.setContentPane(root);\n\n");
        sb.append("        // ----- 组件 -----\n");

        for (int i = 0; i < p.widgets.size(); i++) {
            appendWidget(sb, p.widgets.get(i), names.get(i));
        }

        sb.append("\n        // ----- 逻辑 -----\n");
        appendLogic(sb, p.logic);
        sb.append("\n        frame.setLocationRelativeTo(null);\n");
        sb.append("        frame.setVisible(true);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 窗口图标：图标以资源文件 appicon.png 的形式和程序一起打包（不进源码，避免"常量字符串过长"），
     * 有资源就用，没有就跳过；导出 EXE 时另有 .ico 设置 exe 文件图标。
     */
    private static void appendIcon(StringBuilder sb, Project p) {
        if (p.iconPng == null || p.iconPng.length == 0) {
            return;
        }
        sb.append("        // ----- 窗口图标（随程序打包的 appicon.png，没有则用默认图标）-----\n");
        sb.append("        try {\n");
        sb.append("            java.io.InputStream appIconIn = AppMain.class.getResourceAsStream(\"appicon.png\");\n");
        sb.append("            if (appIconIn != null) {\n");
        sb.append("                java.awt.image.BufferedImage appIconImg = javax.imageio.ImageIO.read(appIconIn);\n");
        sb.append("                appIconIn.close();\n");
        sb.append("                if (appIconImg != null) {\n");
        sb.append("                    frame.setIconImage(appIconImg);\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        } catch (Exception ignored) {\n");
        sb.append("        }\n\n");
    }

    /** 预览/导出真正使用的源码：手改过就用手的，否则按设计器生成 */
    public static String source(Project p) {
        if (p != null && p.customCode != null && p.customCode.trim().length() > 0) {
            return p.customCode;
        }
        return generate(p);
    }

    private static void appendWidget(StringBuilder sb, Widget w, String var) {
        ComponentData.Entry extra = ComponentData.get(w.type);
        if (extra != null) {
            appendExtra(sb, w, var, extra);
            return;
        }
        if ("图片".equals(w.type)) {
            appendImage(sb, w, var);
            return;
        }
        if ("视频".equals(w.type)) {
            appendVideo(sb, w, var);
            return;
        }
        if ("超链接".equals(w.type)) {
            appendLink(sb, w, var);
            return;
        }
        String cls = ComponentFactory.className(w.type);
        String generic = "JComboBox".equals(cls) ? "<String>" : "";

        if ("JSlider".equals(cls)) {
            sb.append("        JSlider ").append(var).append(" = new JSlider(0, 100, 50);\n");
        } else if ("JProgressBar".equals(cls)) {
            sb.append("        JProgressBar ").append(var).append(" = new JProgressBar(0, 100);\n");
            sb.append("        ").append(var).append(".setValue(60);\n");
        } else {
            sb.append("        ").append(cls).append(generic).append(" ").append(var)
                    .append(" = new ").append(cls).append(generic).append("();\n");
        }

        if ("JLabel".equals(cls) || "JButton".equals(cls)
                || "JCheckBox".equals(cls) || "JRadioButton".equals(cls)) {
            sb.append("        ").append(var).append(".setText(").append(quote(w.text)).append(");\n");
        } else if ("JTextField".equals(cls)) {
            sb.append("        ").append(var).append(".setText(").append(quote(w.text)).append(");\n");
            sb.append("        ").append(var).append(".setBorder(javax.swing.BorderFactory.createCompoundBorder(")
                    .append("new javax.swing.border.LineBorder(new Color(0xE3EBE8), 1, true), ")
                    .append("new javax.swing.border.EmptyBorder(4, 8, 4, 8)));\n");
        } else if ("JTextArea".equals(cls)) {
            sb.append("        ").append(var).append(".setText(").append(quote(w.text)).append(");\n");
            sb.append("        ").append(var).append(".setLineWrap(true);\n");
            sb.append("        ").append(var).append(".setWrapStyleWord(true);\n");
            sb.append("        ").append(var).append(".setBorder(javax.swing.BorderFactory.createCompoundBorder(")
                    .append("new javax.swing.border.LineBorder(new Color(0xE3EBE8), 1, true), ")
                    .append("new javax.swing.border.EmptyBorder(6, 9, 6, 9)));\n");
        } else if ("JComboBox".equals(cls)) {
            for (String item : ComponentFactory.splitItems(w.items)) {
                sb.append("        ").append(var).append(".addItem(").append(quote(item)).append(");\n");
            }
        }

        sb.append("        ").append(var).append(".setFont(new Font(\"微软雅黑\", ")
                .append(w.bold ? "Font.BOLD" : "Font.PLAIN").append(", ").append(w.fontSize).append("));\n");
        if ("JButton".equals(cls) || "JCheckBox".equals(cls) || "JRadioButton".equals(cls)) {
            // 去掉文字外面的虚线焦点框
            sb.append("        ").append(var).append(".setFocusPainted(false);\n");
        }
        if (!"JProgressBar".equals(cls) && !"JSlider".equals(cls)) {
            sb.append("        ").append(var).append(".setForeground(").append(color(w.fg)).append(");\n");
        }

        if (w.bg != null) {
            sb.append("        ").append(var).append(".setOpaque(true);\n");
            sb.append("        ").append(var).append(".setBackground(").append(color(w.bg)).append(");\n");
        } else if ("JTextArea".equals(cls)) {
            sb.append("        ").append(var).append(".setOpaque(true);\n");
            sb.append("        ").append(var).append(".setBackground(Color.WHITE);\n");
        } else if ("JCheckBox".equals(cls) || "JRadioButton".equals(cls)) {
            sb.append("        ").append(var).append(".setOpaque(false);\n");
        }

        sb.append("        ").append(var).append(".setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append(");\n\n");
    }

    /** 图片控件：按控件尺寸等比缩放（保持原图宽高比，居中留白），放大控件图片一起放大 */
    private static void appendImage(StringBuilder sb, Widget w, String var) {
        sb.append("        JLabel ").append(var).append(" = new JLabel();\n");
        if (w.path == null || w.path.trim().isEmpty()) {
            sb.append("        ").append(var).append(".setText(\"图片（未选择）\");\n");
            sb.append("        ").append(var).append(".setHorizontalAlignment(SwingConstants.CENTER);\n");
        } else {
            int tw = Math.max(1, w.w);
            int th = Math.max(1, w.h);
            sb.append("        ImageIcon ").append(var).append("Icon = new ImageIcon(")
                    .append(quote(w.path)).append(");\n");
            sb.append("        int ").append(var).append("W = ").append(var).append("Icon.getIconWidth();\n");
            sb.append("        int ").append(var).append("H = ").append(var).append("Icon.getIconHeight();\n");
            sb.append("        if (").append(var).append("W > 0 && ").append(var).append("H > 0) {\n");
            sb.append("            double ").append(var).append("Scale = Math.min(")
                    .append(tw).append(" / (double) ").append(var).append("W, ")
                    .append(th).append(" / (double) ").append(var).append("H);\n");
            sb.append("            ").append(var).append(".setIcon(new ImageIcon(").append(var)
                    .append("Icon.getImage().getScaledInstance((int) Math.round(").append(var).append("W * ")
                    .append(var).append("Scale), (int) Math.round(").append(var).append("H * ")
                    .append(var).append("Scale), Image.SCALE_SMOOTH)));\n");
            sb.append("        } else {\n");
            sb.append("            ").append(var).append(".setIcon(").append(var).append("Icon);\n");
            sb.append("        }\n");
            sb.append("        ").append(var).append(".setHorizontalAlignment(SwingConstants.CENTER);\n");
            sb.append("        ").append(var).append(".setVerticalAlignment(SwingConstants.CENTER);\n");
        }
        sb.append("        ").append(var).append(".setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append(");\n\n");
    }

    /** 视频控件：显示文件名，双击用系统默认播放器打开 */
    private static void appendVideo(StringBuilder sb, Widget w, String var) {
        String name = ComponentFactory.fileName(w.path);
        sb.append("        JLabel ").append(var).append(" = new JLabel(")
                .append(quote("视频 · " + (name.isEmpty() ? "未选择文件" : name)))
                .append(", SwingConstants.CENTER);\n");
        // 播放徽标：尺寸 = 控件短边 / 3，控件放大徽标也跟着放大
        int badge = Math.min(Math.max(1, w.w), Math.max(1, w.h)) / 3;
        if (badge >= 14) {
            sb.append("        {\n");
            sb.append("            int badgeSize = Math.min(").append(Math.max(1, w.w)).append(", ")
                    .append(Math.max(1, w.h)).append(") / 3;\n");
            sb.append("            java.awt.image.BufferedImage badgeImg = new java.awt.image.BufferedImage(")
                    .append("badgeSize, badgeSize, java.awt.image.BufferedImage.TYPE_INT_ARGB);\n");
            sb.append("            Graphics2D bg = badgeImg.createGraphics();\n");
            sb.append("            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n");
            sb.append("            bg.setColor(new Color(0x39, 0xB5, 0x9E));\n");
            sb.append("            int pad = badgeSize / 10;\n");
            sb.append("            bg.fillOval(pad, pad, badgeSize - 2 * pad, badgeSize - 2 * pad);\n");
            sb.append("            bg.setColor(Color.WHITE);\n");
            sb.append("            bg.fillPolygon(new int[]{badgeSize * 4 / 10, badgeSize * 4 / 10, badgeSize * 68 / 100},\n");
            sb.append("                    new int[]{badgeSize * 30 / 100, badgeSize * 70 / 100, badgeSize / 2}, 3);\n");
            sb.append("            bg.dispose();\n");
            sb.append("            ").append(var).append(".setIcon(new ImageIcon(badgeImg));\n");
            sb.append("        }\n");
        }
        sb.append("        ").append(var).append(".setOpaque(true);\n");
        sb.append("        ").append(var).append(".setBackground(new Color(0xEEF4F2));\n");
        sb.append("        ").append(var).append(".setFont(new Font(\"微软雅黑\", Font.PLAIN, ")
                .append(w.fontSize).append("));\n");
        sb.append("        ").append(var).append(".setForeground(").append(color(w.fg)).append(");\n");
        sb.append("        ").append(var).append(".setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));\n");
        sb.append("        ").append(var).append(".setToolTipText(\"双击用系统播放器打开视频\");\n");
        sb.append("        ").append(var).append(".addMouseListener(new MouseAdapter() {\n");
        sb.append("            public void mouseClicked(MouseEvent e) {\n");
        sb.append("                if (e.getClickCount() == 2) {\n");
        sb.append("                    try {\n");
        sb.append("                        Desktop.getDesktop().open(new File(").append(quote(w.path)).append("));\n");
        sb.append("                    } catch (Exception ex) {\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("        ").append(var).append(".setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append(");\n\n");
    }

    /** 超链接控件：带下划线文字，双击用系统浏览器打开网址 */
    private static void appendLink(StringBuilder sb, Widget w, String var) {
        String text = w.text == null ? "" : w.text;
        sb.append("        JLabel ").append(var).append(" = new JLabel(")
                .append(quote("<html><u>" + ComponentFactory.escapeHtml(text) + "</u></html>")).append(");\n");
        sb.append("        ").append(var).append(".setFont(new Font(\"微软雅黑\", Font.PLAIN, ")
                .append(w.fontSize).append("));\n");
        sb.append("        ").append(var).append(".setForeground(").append(color(w.fg)).append(");\n");
        sb.append("        ").append(var).append(".setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));\n");
        sb.append("        ").append(var).append(".setToolTipText(").append(quote("双击打开：" + w.url)).append(");\n");
        sb.append("        ").append(var).append(".addMouseListener(new MouseAdapter() {\n");
        sb.append("            public void mouseClicked(MouseEvent e) {\n");
        sb.append("                if (e.getClickCount() == 2) {\n");
        sb.append("                    try {\n");
        sb.append("                        Desktop.getDesktop().browse(new URI(").append(quote(w.url)).append("));\n");
        sb.append("                    } catch (Exception ex) {\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        });\n");
        if (w.bg != null) {
            sb.append("        ").append(var).append(".setOpaque(true);\n");
            sb.append("        ").append(var).append(".setBackground(").append(color(w.bg)).append(");\n");
        }
        sb.append("        ").append(var).append(".setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append(");\n\n");
    }

    /** 下载数据里的 .NET 工具箱组件：按内部映射生成与设计器一致的 Swing 代码 */
    private static void appendExtra(StringBuilder sb, Widget w, String var, ComponentData.Entry e) {
        String k = e.kind;
        String text = w.text == null ? "" : w.text;

        if ("list".equals(k) || "checkedlist".equals(k)) {
            String[] items = {"list".equals(k) ? "项目 1" : "选项 1", "项目 2", "项目 3"};
            if ("checkedlist".equals(k)) {
                items = new String[]{"选项 1", "选项 2", "选项 3"};
            }
            sb.append("        JList<String> ").append(var).append(" = new JList<String>(new String[]{");
            for (int i = 0; i < items.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(quote(items[i]));
            }
            sb.append("});\n");
            appendScrollWrap(sb, w, var);
            return;
        }
        if ("table".equals(k)) {
            sb.append("        JTable ").append(var).append(" = new JTable(")
                    .append("new Object[][]{{\"行 1\", 1}, {\"行 2\", 2}}, ")
                    .append("new String[]{\"名称\", \"数值\"});\n");
            sb.append("        ").append(var).append(".setRowHeight(24);\n");
            appendScrollWrap(sb, w, var);
            return;
        }
        if ("tree".equals(k)) {
            sb.append("        javax.swing.tree.DefaultMutableTreeNode ").append(var)
                    .append("Root = new javax.swing.tree.DefaultMutableTreeNode(\"根节点\");\n");
            sb.append("        ").append(var).append("Root.add(new javax.swing.tree.DefaultMutableTreeNode(\"节点 1\"));\n");
            sb.append("        ").append(var).append("Root.add(new javax.swing.tree.DefaultMutableTreeNode(\"节点 2\"));\n");
            sb.append("        JTree ").append(var).append(" = new JTree(").append(var).append("Root);\n");
            sb.append("        ").append(var).append(".setRowHeight(22);\n");
            sb.append("        for (int i = 0; i < ").append(var).append(".getRowCount(); i++) {\n");
            sb.append("            ").append(var).append(".expandRow(i);\n");
            sb.append("        }\n");
            appendScrollWrap(sb, w, var);
            return;
        }
        if ("tabs".equals(k)) {
            sb.append("        JTabbedPane ").append(var).append(" = new JTabbedPane();\n");
            sb.append("        JPanel ").append(var).append("Tab1 = new JPanel();\n");
            sb.append("        JPanel ").append(var).append("Tab2 = new JPanel();\n");
            sb.append("        ").append(var).append(".addTab(\"选项卡 1\", ").append(var).append("Tab1);\n");
            sb.append("        ").append(var).append(".addTab(\"选项卡 2\", ").append(var).append("Tab2);\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("split".equals(k)) {
            sb.append("        JPanel ").append(var).append("Left = new JPanel();\n");
            sb.append("        ").append(var).append("Left.setBackground(new Color(0xF7FBFA));\n");
            sb.append("        JPanel ").append(var).append("Right = new JPanel();\n");
            sb.append("        ").append(var).append("Right.setBackground(Color.WHITE);\n");
            sb.append("        JSplitPane ").append(var).append(" = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ")
                    .append(var).append("Left, ").append(var).append("Right);\n");
            sb.append("        ").append(var).append(".setDividerLocation(0.45);\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("panel".equals(k)) {
            sb.append("        JPanel ").append(var).append(" = new JPanel();\n");
            sb.append("        ").append(var).append(".setBackground(new Color(0xF4FAF8));\n");
            sb.append("        ").append(var).append(".setBorder(new javax.swing.border.LineBorder(new Color(0xDCE7E4), 1));\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("group".equals(k)) {
            sb.append("        JPanel ").append(var).append(" = new JPanel();\n");
            sb.append("        ").append(var).append(".setBackground(Color.WHITE);\n");
            sb.append("        ").append(var).append(".setBorder(javax.swing.BorderFactory.createTitledBorder(")
                    .append("new javax.swing.border.LineBorder(new Color(0xDCE7E4), 1), ")
                    .append(quote(text.isEmpty() ? "分组框" : text)).append("));\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("toolbar".equals(k)) {
            sb.append("        JToolBar ").append(var).append(" = new JToolBar();\n");
            sb.append("        ").append(var).append(".setFloatable(false);\n");
            sb.append("        ").append(var).append(".add(new JButton(\"新建\"));\n");
            sb.append("        ").append(var).append(".add(new JButton(\"保存\"));\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("statusbar".equals(k) || "menubar".equals(k)) {
            String t = text.isEmpty() ? ("statusbar".equals(k) ? "就绪" : "文件  编辑  视图") : text;
            sb.append("        JLabel ").append(var).append(" = new JLabel(").append(quote(t)).append(");\n");
            sb.append("        ").append(var).append(".setOpaque(true);\n");
            sb.append("        ").append(var).append(".setBackground(new Color(0xF0F4F3));\n");
            sb.append("        ").append(var).append(".setBorder(new javax.swing.border.EmptyBorder(0, ")
                    .append("statusbar".equals(k) ? 8 : 10).append(", 0, 0));\n");
            appendBounds(sb, w, var);
            return;
        }
        if ("calendar".equals(k)) {
            int tw = Math.max(1, w.w);
            int th = Math.max(1, w.h);
            sb.append("        JPanel ").append(var).append(" = new JPanel() {\n");
            sb.append("            protected void paintComponent(Graphics g) {\n");
            sb.append("                super.paintComponent(g);\n");
            sb.append("                Graphics2D g2 = (Graphics2D) g.create();\n");
            sb.append("                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n");
            sb.append("                int w = getWidth();\n");
            sb.append("                int h = getHeight();\n");
            sb.append("                g2.setColor(Color.WHITE);\n");
            sb.append("                g2.fillRect(0, 0, w, h);\n");
            sb.append("                g2.setColor(new Color(0x22312E));\n");
            sb.append("                g2.setFont(new Font(\"微软雅黑\", Font.BOLD, 13));\n");
            sb.append("                g2.drawString(\"2026 年 1 月\", 12, 22);\n");
            sb.append("                g2.setColor(new Color(0x8A9B97));\n");
            sb.append("                g2.setFont(new Font(\"微软雅黑\", Font.PLAIN, 11));\n");
            sb.append("                String[] week = {\"日\", \"一\", \"二\", \"三\", \"四\", \"五\", \"六\"};\n");
            sb.append("                for (int i = 0; i < 7; i++) {\n");
            sb.append("                    g2.drawString(week[i], 12 + i * (w - 24) / 7, 44);\n");
            sb.append("                }\n");
            sb.append("                g2.setColor(new Color(0xDCE7E4));\n");
            sb.append("                g2.drawLine(8, 50, w - 8, 50);\n");
            sb.append("                g2.setColor(new Color(0x3A4A46));\n");
            sb.append("                g2.setFont(new Font(\"微软雅黑\", Font.PLAIN, 11));\n");
            sb.append("                int startX = 12;\n");
            sb.append("                int startY = 72;\n");
            sb.append("                int cellW = Math.max(12, (w - 24) / 7);\n");
            sb.append("                int cellH = Math.max(12, (h - 84) / 5);\n");
            sb.append("                int day = 1;\n");
            sb.append("                for (int row = 0; row < 5; row++) {\n");
            sb.append("                    for (int col = 0; col < 7; col++) {\n");
            sb.append("                        if (day > 31) {\n");
            sb.append("                            break;\n");
            sb.append("                        }\n");
            sb.append("                        g2.drawString(String.valueOf(day), startX + col * cellW, startY + row * cellH);\n");
            sb.append("                        day++;\n");
            sb.append("                    }\n");
            sb.append("                }\n");
            sb.append("                g2.dispose();\n");
            sb.append("            }\n");
            sb.append("        };\n");
            sb.append("        ").append(var).append(".setBackground(Color.WHITE);\n");
            appendBounds(sb, w, var);
            return;
        }

        // 其余：单行文字类 / 按钮类 / 输入类 / 数值类
        String cls = ComponentFactory.extraClass(k);
        String generic = "JComboBox".equals(cls) ? "<String>" : "";
        if ("JSpinner".equals(cls)) {
            sb.append("        JSpinner ").append(var).append(" = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));\n");
        } else if ("JProgressBar".equals(cls)) {
            sb.append("        JProgressBar ").append(var).append(" = new JProgressBar(0, 100);\n");
            sb.append("        ").append(var).append(".setValue(60);\n");
        } else if ("JSlider".equals(cls)) {
            sb.append("        JSlider ").append(var).append(" = new JSlider(0, 100, 50);\n");
        } else if ("JScrollBar".equals(cls)) {
            sb.append("        JScrollBar ").append(var).append(" = new JScrollBar(Adjustable.")
                    .append("scrollbar-h".equals(k) ? "HORIZONTAL" : "VERTICAL").append(");\n");
        } else {
            sb.append("        ").append(cls).append(generic).append(" ").append(var)
                    .append(" = new ").append(cls).append(generic).append("();\n");
        }

        if ("JLabel".equals(cls) || "JButton".equals(cls)
                || "JCheckBox".equals(cls) || "JRadioButton".equals(cls)) {
            String label = text;
            if ("linklabel".equals(k)) {
                label = "<html><u>" + ComponentFactory.escapeHtml(text) + "</u></html>";
            }
            sb.append("        ").append(var).append(".setText(").append(quote(label)).append(");\n");
        } else if ("JTextField".equals(cls)) {
            sb.append("        ").append(var).append(".setText(").append(quote(text)).append(");\n");
            sb.append("        ").append(var).append(".setBorder(javax.swing.BorderFactory.createCompoundBorder(")
                    .append("new javax.swing.border.LineBorder(new Color(0xE3EBE8), 1, true), ")
                    .append("new javax.swing.border.EmptyBorder(4, 8, 4, 8)));\n");
        } else if ("JTextArea".equals(cls)) {
            sb.append("        ").append(var).append(".setText(").append(quote(text)).append(");\n");
            sb.append("        ").append(var).append(".setLineWrap(true);\n");
            sb.append("        ").append(var).append(".setWrapStyleWord(true);\n");
            sb.append("        ").append(var).append(".setBorder(javax.swing.BorderFactory.createCompoundBorder(")
                    .append("new javax.swing.border.LineBorder(new Color(0xE3EBE8), 1, true), ")
                    .append("new javax.swing.border.EmptyBorder(6, 9, 6, 9)));\n");
        } else if ("JComboBox".equals(cls)) {
            String[] items = ComponentFactory.splitItems(w.items);
            for (String item : items) {
                sb.append("        ").append(var).append(".addItem(").append(quote(item)).append(");\n");
            }
        }

        sb.append("        ").append(var).append(".setFont(new Font(\"微软雅黑\", ")
                .append(w.bold ? "Font.BOLD" : "Font.PLAIN").append(", ").append(w.fontSize).append("));\n");
        if ("JButton".equals(cls) || "JCheckBox".equals(cls) || "JRadioButton".equals(cls)) {
            sb.append("        ").append(var).append(".setFocusPainted(false);\n");
        }
        if (!"JProgressBar".equals(cls) && !"JSlider".equals(cls)) {
            sb.append("        ").append(var).append(".setForeground(").append(color(w.fg)).append(");\n");
        }
        if ("linklabel".equals(k)) {
            sb.append("        ").append(var).append(".setForeground(new Color(0x1A73E8));\n");
            sb.append("        ").append(var).append(".setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));\n");
        }
        if (w.bg != null) {
            sb.append("        ").append(var).append(".setOpaque(true);\n");
            sb.append("        ").append(var).append(".setBackground(").append(color(w.bg)).append(");\n");
        }
        appendBounds(sb, w, var);
    }

    private static void appendScrollWrap(StringBuilder sb, Widget w, String var) {
        sb.append("        JScrollPane ").append(var).append("Scroll = new JScrollPane(").append(var).append(");\n");
        sb.append("        ").append(var).append("Scroll.setBorder(new javax.swing.border.LineBorder(new Color(0xDCE7E4), 1));\n");
        sb.append("        ").append(var).append("Scroll.getVerticalScrollBar().setUnitIncrement(16);\n");
        sb.append("        ").append(var).append("Scroll.setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append("Scroll);\n\n");
    }

    private static void appendBounds(StringBuilder sb, Widget w, String var) {
        sb.append("        ").append(var).append(".setBounds(").append(w.x).append(", ")
                .append(w.y).append(", ").append(w.w).append(", ").append(w.h).append(");\n");
        sb.append("        root.add(").append(var).append(");\n\n");
    }

    private static void appendLogic(StringBuilder sb, String logic) {
        if (logic == null || logic.trim().length() == 0) {
            return;
        }
        String[] lines = logic.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        for (String line : lines) {
            if (line.trim().length() == 0) {
                sb.append("\n");
            } else {
                sb.append("        ").append(line).append("\n");
            }
        }
        sb.append("\n");
    }

    private static List<String> uniqueNames(Project p) {
        Set<String> used = new HashSet<String>();
        List<String> out = new ArrayList<String>();
        for (Widget w : p.widgets) {
            String base = sanitize(w.name, ComponentFactory.prefix(w.type));
            String name = base;
            int i = 2;
            while (!used.add(name)) {
                name = base + i;
                i++;
            }
            out.add(name);
        }
        return out;
    }

    private static String sanitize(String raw, String fallback) {
        String s = raw == null ? "" : raw.trim().replaceAll("[^A-Za-z0-9_$]", "");
        if (s.length() == 0) {
            s = fallback;
        }
        if (Character.isDigit(s.charAt(0))) {
            s = "c" + s;
        }
        return s;
    }

    public static String quote(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    public static String color(Color c) {
        if (c == null) {
            c = Color.WHITE;
        }
        return String.format("new Color(0x%06X)", c.getRGB() & 0xFFFFFF);
    }
}
