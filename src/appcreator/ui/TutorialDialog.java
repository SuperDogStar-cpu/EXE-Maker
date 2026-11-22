package appcreator.ui;

import appcreator.io.Lang;
import appcreator.io.TutorialSync;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「教程」窗口：左侧目录 + 右侧正文（Markdown 转 HTML 渲染），顶部带搜索。
 *
 * 内容优先顺序：程序目录里的同名 .md（方便自己改） → 启动时下载到
 * 用户目录 .appcreator/tutorial 的那份 → jar 里内置的一份。
 */
public final class TutorialDialog extends JDialog {

    /** 目录名, 文件名 */
    private static final String[][] DOCS = {
            {"快速上手", "快速上手.md"},
            {"逻辑代码", "控件逻辑代码.md"},
            {"对话框", "对话框使用.md"},
            {"Java 基础", "Java基础语法.md"},
            {"延迟与内置库", "Java延迟与内置库.md"}
    };

    private static final Color HIT = new Color(0xFFF1B0);
    private static final Color HIT_CUR = new Color(0xFFC94D);

    private final JEditorPane pane = new JEditorPane();
    private final JList<String> list;
    private final JTextField search = new JTextField();
    private final JLabel status = new JLabel("0 / 0");
    private final Map<String, String> cache = new HashMap<String, String>();
    private final List<long[]> hits = new ArrayList<long[]>();
    private int hitIndex = -1;
    private int fontSize = 13;
    private int docIndex = 0;

    public TutorialDialog(Window owner) {
        super(owner, Lang.t("教程"), ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DefaultListModel<String> model = new DefaultListModel<String>();
        for (String[] d : DOCS) {
            model.addElement(Lang.t(d[0]));
        }
        list = new JList<String>(model);
        list.setSelectedIndex(0);
        list.setFont(new Font(Fonts.familyForText("教程"), Font.PLAIN, 13));
        list.setFixedCellHeight(42);
        list.setBackground(Color.WHITE);
        list.setCellRenderer(new SideRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedIndex() >= 0) {
                docIndex = list.getSelectedIndex();
                loadDoc();
            }
        });

        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(178, 10));
        JLabel sideTitle = new JLabel("  " + Lang.t("教程"));
        sideTitle.setFont(new Font(Fonts.familyForText("教程"), Font.BOLD, 12));
        sideTitle.setForeground(Theme.ACCENT_DARK);
        sideTitle.setBorder(new EmptyBorder(14, 8, 10, 8));
        side.add(sideTitle, BorderLayout.NORTH);
        JScrollPane sideScroll = new JScrollPane(list);
        sideScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        side.add(sideScroll, BorderLayout.CENTER);

        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setBackground(Color.WHITE);
        pane.setBorder(new EmptyBorder(4, 6, 6, 6));
        pane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    openLink(e.getURL());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(Theme.BG);
        bar.setBorder(new EmptyBorder(8, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        search.setPreferredSize(new Dimension(230, 26));
        search.setFont(new Font(Fonts.familyForText("搜索"), Font.PLAIN, 12));
        search.setToolTipText("搜索当前教程，回车跳到下一处");
        search.addActionListener(e -> nextHit(1));
        search.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    search.setText("");
                }
            }
        });
        JButton prev = tool("▲", "上一处");
        prev.addActionListener(e -> nextHit(-1));
        JButton next = tool("▼", "下一处");
        next.addActionListener(e -> nextHit(1));
        status.setFont(new Font(Fonts.familyForText("0"), Font.PLAIN, 11));
        status.setForeground(Theme.MUTED);
        left.add(search);
        left.add(prev);
        left.add(next);
        left.add(status);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        JButton minus = tool("A-", "字号减小");
        minus.addActionListener(e -> zoom(-1));
        JButton plus = tool("A+", "字号增大");
        plus.addActionListener(e -> zoom(1));
        right.add(minus);
        right.add(plus);
        bar.add(right, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.WHITE);
        center.add(bar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("  Ctrl+F 搜索　·　双击链接用浏览器打开　·　Esc 关闭");
        hint.setFont(new Font(Fonts.familyForText("提"), Font.PLAIN, 11));
        hint.setForeground(Theme.MUTED);
        hint.setOpaque(true);
        hint.setBackground(Color.WHITE);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), new EmptyBorder(5, 8, 6, 8)));
        center.add(hint, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(side, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDlg");
        root.getActionMap().put("closeDlg", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        setContentPane(root);

        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        pane.getActionMap().put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        });
        search.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        search.getActionMap().put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        });

        setSize(980, 720);
        setMinimumSize(new Dimension(620, 400));
        setLocationRelativeTo(owner);
        loadDoc();
    }

    // ---------------- 文档加载 ----------------

    private JButton tool(String text, String tip) {
        JButton b = new JButton(text);
        b.setFont(new Font(Fonts.familyForText(text), Font.PLAIN, 11));
        b.setFocusable(false);
        b.setMargin(new Insets(2, 8, 2, 8));
        b.setToolTipText(tip);
        b.setBackground(Color.WHITE);
        b.setForeground(Theme.TEXT);
        return b;
    }

    private void loadDoc() {
        String md = cache.get(DOCS[docIndex][1]);
        if (md == null) {
            md = load(DOCS[docIndex][1]);
            cache.put(DOCS[docIndex][1], md);
        }
        String family = Fonts.familyForText("中文");
        pane.setText(Markdown.toHtml(md, fontSize, family));
        pane.setCaretPosition(0);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pane.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                search.setText(search.getText());
                refreshHits();
            }
        });
    }

    private String load(String fileName) {
        // 1) 程序目录里的同名 md（用户自己改过的）
        try {
            File f = new File(fileName);
            if (f.isFile()) {
                return read(new FileInputStream(f));
            }
        } catch (Exception ignored) {
        }
        // 2) 启动时下载到用户目录的
        try {
            File f = TutorialSync.local(fileName);
            if (f != null) {
                return read(new FileInputStream(f));
            }
        } catch (Exception ignored) {
        }
        // 3) jar 里内置的
        InputStream in = null;
        try {
            in = TutorialDialog.class.getResourceAsStream("/appcreator/tutorial/" + fileName);
            if (in != null) {
                return read(in);
            }
        } catch (Exception ignored) {
            // 走兜底提示
        } finally {
            close(in);
        }
        return "# " + fileName + "\n\n教程内容没找到。检查网络后重启，程序会自动下载到\n`~/.appcreator/tutorial`。";
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

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void openLink(java.net.URL url) {
        try {
            if (url != null && (url.getProtocol().equals("http") || url.getProtocol().equals("https"))) {
                Desktop.getDesktop().browse(url.toURI());
            }
        } catch (Exception ignored) {
            // 打开失败就什么也不做
        }
    }

    // ---------------- 搜索 ----------------

    private void nextHit(int dir) {
        refreshHits();
        if (hits.isEmpty()) {
            return;
        }
        hitIndex = (hitIndex + dir + hits.size()) % hits.size();
        showHit(hitIndex);
    }

    private void refreshHits() {
        Highlighter hl = pane.getHighlighter();
        hl.removeAllHighlights();
        hits.clear();
        hitIndex = -1;
        String key = search.getText();
        if (key == null || key.trim().length() == 0) {
            status.setText("0 / 0");
            return;
        }
        key = key.trim().toLowerCase();
        String plain;
        try {
            Document doc = pane.getDocument();
            plain = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            status.setText("0 / 0");
            return;
        }
        String lower = plain.toLowerCase();
        int from = 0;
        while (true) {
            int p = lower.indexOf(key, from);
            if (p < 0 || hits.size() >= 400) {
                break;
            }
            hits.add(new long[]{p, p + key.length()});
            from = p + key.length();
        }
        try {
            for (int i = 0; i < hits.size(); i++) {
                hl.addHighlight((int) hits.get(i)[0], (int) hits.get(i)[1],
                        new DefaultHighlighter.DefaultHighlightPainter(HIT));
            }
        } catch (Exception ignored) {
        }
        status.setText(hits.isEmpty() ? "没有找到" : ("1 / " + hits.size()));
        if (!hits.isEmpty()) {
            hitIndex = 0;
            showHit(0);
        }
    }

    private void showHit(int idx) {
        if (idx < 0 || idx >= hits.size()) {
            return;
        }
        int start = (int) hits.get(idx)[0];
        int end = (int) hits.get(idx)[1];
        try {
            Highlighter hl = pane.getHighlighter();
            hl.removeAllHighlights();
            for (int i = 0; i < hits.size(); i++) {
                Color c = i == idx ? HIT_CUR : HIT;
                hl.addHighlight((int) hits.get(i)[0], (int) hits.get(i)[1],
                        new DefaultHighlighter.DefaultHighlightPainter(c));
            }
            java.awt.Rectangle r = pane.modelToView(start);
            if (r != null) {
                java.awt.Rectangle vis = new java.awt.Rectangle(r.x, Math.max(0, r.y - 60), r.width, r.height + 120);
                pane.scrollRectToVisible(vis);
            }
        } catch (Exception ignored) {
        }
        status.setText((idx + 1) + " / " + hits.size());
        hitIndex = idx;
    }

    // ---------------- 字号 ----------------

    private void zoom(int delta) {
        int next = Math.max(9, Math.min(24, fontSize + delta));
        if (next == fontSize) {
            return;
        }
        fontSize = next;
        loadDoc();
    }

    // ---------------- 左侧目录渲染 ----------------

    private static final class SideRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                     boolean selected, boolean focus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            c.setBorder(new EmptyBorder(0, 14, 0, 8));
            c.setOpaque(true);
            if (selected) {
                c.setBackground(Theme.ACCENT);
                c.setForeground(Color.WHITE);
                c.setFont(new Font(Fonts.familyForText(String.valueOf(value)), Font.BOLD, 13));
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Theme.TEXT);
                c.setFont(new Font(Fonts.familyForText(String.valueOf(value)), Font.PLAIN, 13));
            }
            return c;
        }
    }
}
