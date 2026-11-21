package appcreator.ui;

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
    private final Runnable onApplied;

    public SettingsDialog(JFrame owner, Runnable onApplied) {
        super(owner, "设置", true);
        this.onApplied = onApplied;

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(16, 18, 14, 18));
        setContentPane(root);

        JLabel title = new JLabel("设置");
        title.setFont(Theme.ui(15, Font.BOLD));
        title.setForeground(Theme.ACCENT_DARK);
        root.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        JPanel langRow = new JPanel(new BorderLayout(10, 0));
        langRow.setOpaque(false);
        langRow.add(new JLabel("语言"), BorderLayout.WEST);
        combo.setFont(Theme.ui(12));
        combo.setPreferredSize(new Dimension(260, 30));
        langRow.add(combo, BorderLayout.CENTER);
        RoundButton browse = new RoundButton("导入 .japa", Theme.SOFT, Theme.ACCENT_DARK);
        browse.setBorder(new EmptyBorder(6, 12, 6, 12));
        browse.addActionListener(e -> choosePack());
        langRow.add(browse, BorderLayout.EAST);
        body.add(langRow, BorderLayout.NORTH);

        JTextArea tip = new JTextArea();
        tip.setEditable(false);
        tip.setFont(Theme.ui(12));
        tip.setForeground(Theme.MUTED);
        tip.setBackground(Theme.CARD);
        tip.setBorder(new EmptyBorder(10, 12, 10, 12));
        tip.setText("语言包是 UTF-8 文本，扩展名 .japa，一行一条「原文=译文」：\n"
                + "\n"
                + "  #name=English\n"
                + "  组件库=Palette\n"
                + "  新建=New\n"
                + "\n"
                + "文件名随便取，认的是 .japa 后缀；程序目录、用户目录、\n"
                + "当前项目目录里放的语言包会自动列在上面的下拉框里。\n"
                + "译文会替换内置界面文字，没写到的保持原样。");
        tip.setRows(9);
        JScrollPane sp = new JScrollPane(tip);
        sp.setBorder(new RoundBorder(8, Theme.BORDER));
        body.add(sp, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        RoundButton cancel = new RoundButton("取消", Theme.SOFT, Theme.ACCENT_DARK);
        cancel.setBorder(new EmptyBorder(6, 16, 6, 16));
        cancel.addActionListener(e -> dispose());
        RoundButton ok = new RoundButton("确定", Theme.ACCENT, Color.WHITE);
        ok.setBorder(new EmptyBorder(6, 16, 6, 16));
        ok.addActionListener(e -> apply());
        actions.add(cancel);
        actions.add(ok);
        root.add(actions, BorderLayout.SOUTH);

        loadItems();
        pack();
        setSize(Math.max(560, getWidth()), Math.max(430, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void loadItems() {
        combo.removeAllItems();
        combo.addItem(new Item(Lang.DEFAULT_NAME, null));
        List<File> packs = Lang.discover();
        File current = Lang.file();
        Item selected = null;
        for (File f : packs) {
            Item it = new Item(Lang.displayName(f), f);
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
            return file == null ? name + "（内置）" : name + "  ·  " + file.getName();
        }
    }
}
