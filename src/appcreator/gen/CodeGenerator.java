package appcreator.gen;

import appcreator.model.ComponentFactory;
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
        sb.append("import java.awt.event.*;\n\n");
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

    private static void appendWidget(StringBuilder sb, Widget w, String var) {
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
