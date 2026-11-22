package appcreator.ui;

import appcreator.io.Lang;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 「教程」窗口：内置四份教程，用标签页分开，内容随程序一起打包（也能读程序目录里的同名 .md）。
 */
public final class TutorialDialog extends JDialog {

    /** 标签名, 资源路径, 程序目录里的文件名 */
    private static final String[][] DOCS = {
            {"快速上手", "/appcreator/tutorial/快速上手.md", "快速上手.md"},
            {"逻辑代码", "/appcreator/tutorial/控件逻辑代码.md", "控件逻辑代码.md"},
            {"Java 基础", "/appcreator/tutorial/Java基础语法.md", "Java基础语法.md"},
            {"延迟与内置库", "/appcreator/tutorial/Java延迟与内置库.md", "Java延迟与内置库.md"}
    };

    public TutorialDialog(Window owner) {
        super(owner, Lang.t("教程"), ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Fonts.familyForText("教程"), Font.PLAIN, 13));
        for (String[] doc : DOCS) {
            JComponent page = page(doc[1], doc[2]);
            tabs.addTab(Lang.t(doc[0]), page);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setSize(940, 700);
        setMinimumSize(new Dimension(560, 360));
        setLocationRelativeTo(owner);
    }

    private JComponent page(String resource, String fileName) {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setBackground(Color.WHITE);
        pane.setBorder(new javax.swing.border.EmptyBorder(14, 18, 18, 18));
        render(pane, load(resource, fileName));
        pane.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(pane);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    /** 先读程序内置的那份，其次读程序目录里的 .md */
    private String load(String resource, String fileName) {
        InputStream in = null;
        try {
            in = TutorialDialog.class.getResourceAsStream(resource);
            if (in != null) {
                return read(in);
            }
        } catch (Exception ignored) {
            // 资源读不到就走文件
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
        try {
            File f = new File(fileName);
            if (f.isFile()) {
                return read(new FileInputStream(f));
            }
        } catch (Exception ignored) {
            // 都不行就给提示
        }
        return "# " + fileName + "\n\n教程内容没找到，把 " + fileName + " 放到程序目录下即可。";
    }

    private static String read(InputStream in) throws java.io.IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        r.close();
        return sb.toString();
    }

    // ---------------- 简易 Markdown 渲染 ----------------

    private static void render(JTextPane pane, String md) {
        StyledDocument doc = pane.getStyledDocument();
        for (Object[] s : styles()) {
            Style st = doc.addStyle((String) s[0], null);
            st.addAttributes((AttributeSet) s[1]);
        }
        String[] lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        boolean code = false;
        for (String raw : lines) {
            String line = raw;
            if (line.startsWith("```")) {
                code = !code;
                if (!code) {
                    insert(doc, "\n", "plain");
                }
                continue;
            }
            if (code) {
                insert(doc, line + "\n", "code");
                continue;
            }
            if (line.trim().length() == 0) {
                insert(doc, "\n", "plain");
            } else if (line.startsWith("### ")) {
                insert(doc, line.substring(4) + "\n", "h3");
            } else if (line.startsWith("## ")) {
                insert(doc, line.substring(3) + "\n", "h2");
            } else if (line.startsWith("# ")) {
                insert(doc, line.substring(2) + "\n", "h1");
            } else if (line.startsWith("- ")) {
                insert(doc, "  \u00B7  ", "bullet");
                inline(doc, line.substring(2), "bullet");
                insert(doc, "\n", "bullet");
            } else if (line.matches("^\\d+\\..*")) {
                inline(doc, line, "bullet");
                insert(doc, "\n", "bullet");
            } else {
                inline(doc, line, "plain");
                insert(doc, "\n", "plain");
            }
        }
    }

    private static void inline(StyledDocument doc, String text, String base) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end > 0) {
                    insert(doc, buf.toString(), base);
                    buf.setLength(0);
                    insert(doc, text.substring(i + 2, end), "bold");
                    i = end + 2;
                    continue;
                }
            }
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > 0) {
                    insert(doc, buf.toString(), base);
                    buf.setLength(0);
                    insert(doc, text.substring(i + 1, end), "inline");
                    i = end + 1;
                    continue;
                }
            }
            buf.append(text.charAt(i));
            i++;
        }
        insert(doc, buf.toString(), base);
    }

    private static void insert(StyledDocument doc, String text, String style) {
        if (text.length() == 0) {
            return;
        }
        try {
            doc.insertString(doc.getLength(), text, doc.getStyle(style));
        } catch (BadLocationException ignored) {
        }
    }

    private static List<Object[]> styles() {
        List<Object[]> list = new ArrayList<Object[]>();
        list.add(new Object[]{"plain", style(13, Font.PLAIN, new Color(0x22312E), new Color(0xFFFFFF), 0)});
        list.add(new Object[]{"bold", style(13, Font.BOLD, new Color(0x1E4F46), new Color(0xFFFFFF), 0)});
        list.add(new Object[]{"h1", style(20, Font.BOLD, new Color(0x18876F), new Color(0xFFFFFF), 14)});
        list.add(new Object[]{"h2", style(16, Font.BOLD, new Color(0x2C7A6B), new Color(0xFFFFFF), 14)});
        list.add(new Object[]{"h3", style(14, Font.BOLD, new Color(0x3A4A46), new Color(0xFFFFFF), 10)});
        list.add(new Object[]{"bullet", style(13, Font.PLAIN, new Color(0x22312E), new Color(0xFFFFFF), 0)});
        list.add(new Object[]{"code", codeStyle(new Color(0x1F3D3A), new Color(0xF3F8F7))});
        list.add(new Object[]{"inline", codeStyle(new Color(0x1F6F5F), new Color(0xEEF6F4))});
        return list;
    }

    private static AttributeSet style(int size, int fontStyle, Color fg, Color bg, int spaceAbove) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontFamily(s, Fonts.familyForText("中"));
        StyleConstants.setFontSize(s, size);
        StyleConstants.setBold(s, fontStyle == Font.BOLD);
        StyleConstants.setItalic(s, fontStyle == Font.ITALIC);
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBackground(s, bg);
        StyleConstants.setSpaceAbove(s, spaceAbove);
        StyleConstants.setSpaceBelow(s, spaceAbove >= 6 ? 6 : 2);
        return s;
    }

    private static AttributeSet codeStyle(Color fg, Color bg) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontFamily(s, "Consolas");
        StyleConstants.setFontSize(s, 12);
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBackground(s, bg);
        StyleConstants.setSpaceAbove(s, 1);
        StyleConstants.setSpaceBelow(s, 1);
        StyleConstants.setLeftIndent(s, 12f);
        return s;
    }
}
