package appcreator.ui;

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
public class LogicWindow extends JDialog {

    private static final Color BG = new Color(0x1E1E1E);
    private static final Color PANEL = new Color(0x252526);
    private static final Color BORDER = new Color(0x3C3C3C);
    private static final Color TEXT = new Color(0xCCCCCC);
    private static final Color MUTED = new Color(0x858585);
    private static final Color BLUE = new Color(0x0E639C);

    private final LogicEditor editor;
    private final JLabel titleLabel = new JLabel("逻辑代码");
    private final JLabel subtitleLabel = new JLabel("AppMain.java · 停止输入后自动检查语法");
    private final JLabel savedLabel = new JLabel("自动保存：等待第一次保存");
    private final Runnable onSave;
    private final Runnable onPreview;
    private Runnable onClose;

    public LogicWindow(JFrame owner, LogicEditor editor, Runnable onSave, Runnable onPreview) {
        super(owner, "逻辑编辑器", false);
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
        root.add(editor, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        installKeys();

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

    public LogicEditor getEditor() {
        return editor;
    }

    /** 每次打开时刷新标题、项目和焦点 */
    public void open(Project project) {
        refreshProject(project);
        editor.focusEditor();
        editor.recheck();
        setVisible(true);
        toFront();
    }

    /** 项目切换（新建/打开）时同步标题和编辑器 */
    public void refreshProject(Project project) {
        titleLabel.setText(project == null ? "逻辑代码" : "逻辑代码 · " + project.title);
        editor.setProject(project);
    }

    public void setSaved(String text) {
        savedLabel.setText("自动保存：" + text);
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
        actions.add(darkButton("立即检查", new Color(0x3C3C3C), Color.WHITE, new Runnable() {
            public void run() {
                editor.recheck();
            }
        }));
        actions.add(darkButton("运行预览", new Color(0x3C3C3C), Color.WHITE, new Runnable() {
            public void run() {
                if (onPreview != null) {
                    onPreview.run();
                }
            }
        }));
        actions.add(darkButton("保存并关闭", BLUE, Color.WHITE, new Runnable() {
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
