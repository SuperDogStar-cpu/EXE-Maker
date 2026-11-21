package appcreator.ui;

import appcreator.gen.CodeGenerator;
import appcreator.io.Lang;
import appcreator.model.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 独立的逻辑代码窗口（VS Code 风格深色界面）。
 * 点主界面「逻辑」直接打开；关闭后回到属性面板。
 */
public class CodeWindow extends JDialog {

    private static final Color BG = new Color(0x1E1E1E);
    private static final Color PANEL = new Color(0x252526);
    private static final Color BORDER = new Color(0x3C3C3C);
    private static final Color TEXT = new Color(0xCCCCCC);
    private static final Color MUTED = new Color(0x858585);
    private static final Color BLUE = new Color(0x0E639C);
    /** 深色按钮：深底、浅字，三个按钮统一 */
    private static final Color BUTTON_BG = new Color(0x333333);
    private static final Color BUTTON_FG = new Color(0xEDEDED);

    private final CodeEditor editor;
    private final CodeEditor fullEditor = new CodeEditor(true);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel titleLabel = new JLabel("代码");
    private final JLabel subtitleLabel = new JLabel("AppMain.java · 停止输入后自动检查语法");
    private final JLabel savedLabel = new JLabel("自动保存：等待第一次保存");
    private final JLabel customHint = new JLabel(" ");
    private final Runnable onSave;
    private final Runnable onPreview;
    private Runnable onClose;
    private Project project;
    private boolean customEdited;
    private boolean refreshing;

    public CodeWindow(JFrame owner, CodeEditor editor, Runnable onSave, Runnable onPreview) {
        super(owner, Lang.t("代码编辑器"), false);
        this.editor = editor;
        this.onSave = onSave;
        this.onPreview = onPreview;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        fullEditor.setOnDirty(new Runnable() {
            public void run() {
                if (refreshing) {
                    return;
                }
                customEdited = true;
                if (project != null) {
                    project.customCode = fullEditor.getText();
                }
                updateHint();
            }
        });

        installKeys();
        Lang.applyTree(getContentPane());
        Fonts.apply(getContentPane());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        });
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public CodeEditor getEditor() {
        return editor;
    }

    /** 完整源码编辑器里的内容（没手改过时返回 null） */
    public String customCode() {
        return customEdited ? fullEditor.getText() : null;
    }

    /** 关闭窗口 / 预览 / 导出前，把两个编辑器的内容同步回项目 */
    public void commit() {
        if (project == null) {
            return;
        }
        project.logic = editor.getText();
        if (customEdited) {
            project.customCode = fullEditor.getText();
        }
    }

    public void discardCustomCode() {
        customEdited = false;
        if (project != null) {
            project.customCode = null;
        }
        refreshFullCode();
        updateHint();
    }

    /** 每次打开时刷新标题、项目和焦点 */
    public void open(Project project) {
        refreshProject(project);
        editor.focusEditor();
        editor.recheck();
        fullEditor.recheck();
        setVisible(true);
        toFront();
    }

    /** 项目切换（新建/打开）时同步标题和编辑器 */
    public void refreshProject(Project project) {
        this.project = project;
        titleLabel.setText(project == null ? Lang.t("逻辑") : Lang.t("逻辑") + " · " + project.title);
        editor.setProject(project);
        fullEditor.setProject(project);
        customEdited = project != null && project.customCode != null
                && project.customCode.trim().length() > 0;
        refreshFullCode();
        updateHint();
    }

    public void setSaved(String text) {
        savedLabel.setText("自动保存：" + text);
    }

    // ---------------- 完整源码 ----------------

    private JComponent buildTabs() {
        JPanel logicWrap = new JPanel(new BorderLayout());
        logicWrap.setBackground(BG);
        logicWrap.add(editor, BorderLayout.CENTER);

        JPanel fullWrap = new JPanel(new BorderLayout());
        fullWrap.setBackground(BG);
        fullWrap.add(buildFullToolbar(), BorderLayout.NORTH);
        fullWrap.add(fullEditor, BorderLayout.CENTER);

        tabs.setFont(Theme.ui(12));
        tabs.addTab("逻辑", logicWrap);
        tabs.addTab("完整源码（可改）", fullWrap);

        JLabel logicTab = new JLabel("逻辑");
        logicTab.setFont(Theme.ui(12));
        logicTab.setBorder(new EmptyBorder(4, 8, 4, 28));
        tabs.setTabComponentAt(0, logicTab);

        JLabel fullTab = new JLabel("完整源码（可改）");
        fullTab.setFont(Theme.ui(12));
        fullTab.setBorder(new EmptyBorder(4, 8, 4, 8));
        tabs.setTabComponentAt(1, fullTab);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                refreshFullCode();
                fullEditor.focusEditor();
            } else {
                editor.focusEditor();
            }
        });
        return tabs;
    }

    private JComponent buildFullToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(PANEL);
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        customHint.setFont(Theme.ui(11));
        customHint.setForeground(MUTED);
        bar.add(customHint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(darkButton("重新生成", BUTTON_BG, BUTTON_FG, new Runnable() {
            public void run() {
                discardCustomCode();
            }
        }));
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    /** 完整源码：手改过就显示手改的，没改过就显示设计器生成的 */
    private void refreshFullCode() {
        if (project == null) {
            return;
        }
        refreshing = true;
        String code;
        if (customEdited) {
            code = project.customCode == null ? "" : project.customCode;
        } else {
            project.logic = editor.getText();
            code = CodeGenerator.generate(project);
        }
        fullEditor.setText(code);
        refreshing = false;
    }

    private void updateHint() {
        customHint.setText(customEdited
                ? Lang.t("已手改：预览 / 导出都按这份代码走（点「重新生成」恢复）")
                : Lang.t("由设计器生成，可直接在这里改组件、改布局，改了就按这份代码跑"));
        customHint.setForeground(customEdited ? new Color(0xE2C08D) : MUTED);
    }

    // ---------------- 界面 ----------------

    private JComponent buildHeader() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(8, 14, 8, 12)));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        titleLabel.setFont(Theme.ui(14, Font.BOLD));
        titleLabel.setForeground(Color.WHITE);
        subtitleLabel.setFont(Theme.ui(11));
        subtitleLabel.setForeground(MUTED);
        texts.add(titleLabel);
        texts.add(Box.createVerticalStrut(2));
        texts.add(subtitleLabel);
        bar.add(texts, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(darkButton("立即检查", BUTTON_BG, BUTTON_FG, new Runnable() {
            public void run() {
                editor.recheck();
                fullEditor.recheck();
            }
        }));
        actions.add(darkButton("运行预览", BUTTON_BG, BUTTON_FG, new Runnable() {
            public void run() {
                if (onPreview != null) {
                    onPreview.run();
                }
            }
        }));
        actions.add(darkButton("保存并关闭", BUTTON_BG, BUTTON_FG, new Runnable() {
            public void run() {
                if (onSave != null) {
                    onSave.run();
                }
                dispose();
            }
        }));
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(4, 14, 4, 14)));

        savedLabel.setFont(Theme.mono(11));
        savedLabel.setForeground(MUTED);
        JLabel keys = new JLabel("Ctrl+S 保存 · Ctrl+Enter 预览 · Esc 关闭");
        keys.setFont(Theme.ui(11));
        keys.setForeground(MUTED);
        bar.add(savedLabel, BorderLayout.WEST);
        bar.add(keys, BorderLayout.EAST);
        return bar;
    }

    private JButton darkButton(String text, Color base, Color fg, final Runnable action) {
        RoundButton b = new RoundButton(text, base, fg);
        b.setFont(Theme.ui(12));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void installKeys() {
        JRootPane pane = getRootPane();
        bind(pane, "escape", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new Runnable() {
            public void run() {
                dispose();
            }
        });
        bind(pane, "save", KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), new Runnable() {
            public void run() {
                if (onSave != null) {
                    onSave.run();
                }
            }
        });
        bind(pane, "preview", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), new Runnable() {
            public void run() {
                if (onPreview != null) {
                    onPreview.run();
                }
            }
        });
    }

    private void bind(JRootPane pane, String name, KeyStroke key, final Runnable action) {
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        pane.getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
}

