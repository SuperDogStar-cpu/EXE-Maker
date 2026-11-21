package appcreator.ui;

import appcreator.gen.JavaEnv;
import appcreator.io.Lang;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * 设置窗口：选界面语言。内置简体中文，也可以用 .japa 语言包替换内置文字。
 */
public class SettingsDialog extends JDialog {

    private final JComboBox<Item> combo = new JComboBox<Item>();
    private final JComboBox<Object> javaCombo = new JComboBox<Object>();
    private final JLabel javaHint = new JLabel(" ");
    private final Runnable onApplied;

    public SettingsDialog(JFrame owner, Runnable onApplied) {
        super(owner, Lang.t("设置"), true);
        this.onApplied = onApplied;
        // 先按当前语言把字体挑好，下面所有控件（含自绘文字）才能用对字体
        Fonts.syncLanguage();

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(16, 18, 14, 18));
        setContentPane(root);

        JLabel title = new JLabel(Lang.t("设置"));
        title.setFont(Theme.ui(15, Font.BOLD));
        title.setForeground(Theme.ACCENT_DARK);
        root.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        JPanel langRow = new JPanel(new BorderLayout(10, 0));
        langRow.setOpaque(false);
        langRow.add(new JLabel(Lang.t("语言")), BorderLayout.WEST);
        combo.setFont(Theme.ui(12));
        combo.setPreferredSize(new Dimension(260, 30));
        // 下拉项里是各语言的自称名（日本語 / 한국어 / Français …），按名称挑一款能显示它的字体
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                String text = value == null ? "" : value.toString();
                setText(text);
                setFont(new Font(Fonts.familyForText(text), Font.PLAIN, 12));
                return this;
            }
        });
        langRow.add(combo, BorderLayout.CENTER);
        RoundButton browse = new RoundButton(Lang.t("导入 .japa"), Theme.SOFT, Theme.ACCENT_DARK);
        browse.setBorder(new EmptyBorder(6, 12, 6, 12));
        browse.addActionListener(e -> choosePack());
        langRow.add(browse, BorderLayout.EAST);

        // Java 选择：下拉框列出本机检测到并能用的所有 Java
        JPanel javaRow = new JPanel(new BorderLayout(10, 0));
        javaRow.setOpaque(false);
        javaRow.setBorder(new EmptyBorder(8, 0, 0, 0));
        javaRow.add(new JLabel("Java"), BorderLayout.WEST);
        javaCombo.setFont(Theme.ui(12));
        javaCombo.setPreferredSize(new Dimension(260, 30));
        javaCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof JavaEnv.Jdk) {
                    JavaEnv.Jdk j = (JavaEnv.Jdk) value;
                    setText(j.title());
                    setToolTipText(j.detail());
                } else {
                    setText(Lang.t("自动（跟随当前运行环境）"));
                    setToolTipText(Lang.t("不指定，直接用启动创作器的那个 Java"));
                }
                setFont(new Font(Fonts.familyForText(getText()), Font.PLAIN, 12));
                return this;
            }
        });
        javaRow.add(javaCombo, BorderLayout.CENTER);
        RoundButton redetect = new RoundButton(Lang.t("重新检测"), Theme.SOFT, Theme.ACCENT_DARK);
        redetect.setBorder(new EmptyBorder(6, 12, 6, 12));
        redetect.addActionListener(e -> redetect(redetect));
        javaRow.add(redetect, BorderLayout.EAST);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(langRow);
        rows.add(javaRow);
        javaHint.setFont(Theme.ui(11));
        javaHint.setForeground(Theme.MUTED);
        javaHint.setBorder(new EmptyBorder(4, 34, 0, 0));
        javaHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        rows.add(javaHint);
        body.add(rows, BorderLayout.NORTH);

        JTextArea tip = new JTextArea();
        tip.setEditable(false);
        tip.setFont(Theme.ui(12));
        tip.setForeground(Theme.MUTED);
        tip.setBackground(Theme.CARD);
        tip.setBorder(new EmptyBorder(10, 12, 10, 12));
        tip.setText(Lang.t("语言包是 UTF-8 文本，扩展名 .japa，一行一条「原文 → 译文」：")
                + "\n"
                + "\n"
                + "  #name=English\n"
                + "  Palette=Components\n"
                + "  Button=Button\n"
                + "\n"
                + Lang.t("文件名随便取，认的是 .japa 后缀。") + "\n"
                + Lang.t("程序目录、用户目录、当前项目目录以及它们下面的 Language 文件夹，") + "\n"
                + Lang.t("里面放的语言包都会自动列在上面的下拉框里。") + "\n"
                + Lang.t("译文会替换内置界面文字，没写到的保持原样。"));
        tip.setLineWrap(true);
        tip.setWrapStyleWord(true);
        tip.setFont(new Font(Fonts.familyForText(tip.getText()), Font.PLAIN, 12));
        tip.setRows(9);
        JScrollPane sp = new JScrollPane(tip);
        sp.setBorder(new RoundBorder(8, Theme.BORDER));
        body.add(sp, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        RoundButton cancel = new RoundButton(Lang.t("取消"), Theme.SOFT, Theme.ACCENT_DARK);
        cancel.setBorder(new EmptyBorder(6, 16, 6, 16));
        cancel.addActionListener(e -> dispose());
        RoundButton ok = new RoundButton(Lang.t("确定"), Theme.ACCENT, Color.WHITE);
        ok.setBorder(new EmptyBorder(6, 16, 6, 16));
        ok.addActionListener(e -> apply());
        actions.add(cancel);
        actions.add(ok);
        root.add(actions, BorderLayout.SOUTH);

        loadItems();
        fillJava();
        // 和主界面同步：按当前语言重挑字体，再按每个控件文字逐字兜底
        Lang.applyTree(getContentPane());
        Fonts.apply(getContentPane());
        pack();
        setSize(Math.max(560, getWidth()), Math.max(470, getHeight()));
        setLocationRelativeTo(owner);
    }

    /** 把检测到的 Java 填进下拉框，保留当前选择 */
    private void fillJava() {        javaCombo.removeAllItems();
        javaCombo.addItem("自动");
        JavaEnv.Jdk sel = JavaEnv.selected();
        JavaEnv.Jdk selItem = null;
        for (JavaEnv.Jdk j : JavaEnv.list()) {
            javaCombo.addItem(j);
            if (sel != null && sel.home.getAbsolutePath().equals(j.home.getAbsolutePath())) {
                selItem = j;
            }
        }
        if (selItem != null) {
            javaCombo.setSelectedItem(selItem);
        } else {
            javaCombo.setSelectedIndex(0);
        }
        javaHint.setText(JavaEnv.list().isEmpty()
                ? Lang.t("还没检测到可用的 Java，点「重新检测」再试一次")
                : Lang.t("已检测到 %d 个可用 Java（已用 java -version 验证）")
                        .replace("%d", String.valueOf(JavaEnv.list().size())));
    }

    /** 重新扫描本机 Java（要跑多次 java -version，放到后台线程） */
    private void redetect(final RoundButton button) {
        final String oldText = button.getText();
        button.setText("检测中…");
        button.setEnabled(false);
        new Thread(new Runnable() {
            public void run() {
                JavaEnv.detect();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fillJava();
                        button.setText(oldText);
                        button.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private void loadItems() {
        combo.removeAllItems();
        combo.addItem(new Item(Lang.t("简体中文（内置）"), null));
        List<File> packs = Lang.discover();
        File current = Lang.file();
        Item selected = null;
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (File f : packs) {
            String name = Lang.displayName(f);
            // 项目目录和用户目录里可能各有一份同名语言包（启动时会自动下载），下拉框只留一份
            if (!seen.add(name)) {
                continue;
            }
            Item it = new Item(name, f);
            combo.addItem(it);
            if (current != null && current.getAbsoluteFile().equals(f.getAbsoluteFile())) {
                selected = it;
            }
        }
        if (selected != null) {
            combo.setSelectedItem(selected);
        }
    }

    private void choosePack() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择语言包（.japa）");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".japa");
            }

            @Override
            public String getDescription() {
                return "语言包（.japa）";
            }
        });
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        Item it = new Item(Lang.displayName(f), f);
        combo.addItem(it);
        combo.setSelectedItem(it);
    }

    private void apply() {
        Object jv = javaCombo.getSelectedItem();
        JavaEnv.select(jv instanceof JavaEnv.Jdk ? (JavaEnv.Jdk) jv : null);

        Object sel = combo.getSelectedItem();
        File file = sel instanceof Item ? ((Item) sel).file : null;
        try {
            if (file == null) {
                Lang.useDefault();
            } else {
                Lang.use(file);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "语言包读取失败：" + e.getMessage(), "设置", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (onApplied != null) {
            onApplied.run();
        }
        dispose();
    }

    private static class Item {
        final String name;
        final File file;

        Item(String name, File file) {
            this.name = name;
            this.file = file;
        }

        @Override
        public String toString() {
            return file == null ? name : name + "  ·  " + file.getName();
        }
    }
}
