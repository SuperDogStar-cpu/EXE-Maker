package appcreator.ui;

import appcreator.gen.CodeGenerator;
import appcreator.gen.Exporter;
import appcreator.gen.Runner;
import appcreator.io.ProjectIO;
import appcreator.model.ComponentFactory;
import appcreator.model.Project;
import appcreator.model.Widget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 主窗口：顶部工具栏 + 左侧组件库 + 中间画布 + 右侧属性/逻辑 + 底部状态栏。
 */
public class MainFrame extends JFrame {

    private Project project = new Project();
    private final DesignCanvas canvas = new DesignCanvas();
    private final WindowMockPanel windowMock;
    private final LogicEditor logicArea = new LogicEditor();
    private final JLabel statusLabel = Theme.label("就绪 · 把左侧组件拖到画布上开始设计", 12, Theme.MUTED);
    private final JLabel sizeLabel = Theme.label("", 12, Theme.MUTED);

    private final PropertiesPane props;
    private File currentFile;
    private int counter;
    private boolean running;
    private boolean exporting;
    private boolean dirty;
    private RoundButton mockButton;
    private LogicWindow logicWindow;
    private JPanel rightBody;
    private RoundButton propBtn;
    private RoundButton logicBtn;
    private Timer autoSaveTimer;
    private final File autoSaveFile = new File(System.getProperty("user.home"), "应用创作器-自动保存.apcj");

    public MainFrame() {
        super("应用创作器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1040, 660));
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        windowMock = new WindowMockPanel(canvas, project, true);

        props = new PropertiesPane(new Runnable() {
            public void run() {
                onWindowChanged();
            }
        }, new java.util.function.Consumer<Widget>() {
            public void accept(Widget w) {
                canvas.refresh(w);
                markDirty();
            }
        });

        canvas.setProject(project);
        canvas.setOnSelect(new java.util.function.Consumer<Widget>() {
            public void accept(Widget w) {
                props.bind(project, w);
            }
        });
        canvas.setOnChange(new Runnable() {
            public void run() {
                props.refreshGeometry();
                markDirty();
            }
        });
        canvas.setOnDrop(new java.util.function.BiConsumer<String, Point>() {
            public void accept(String type, Point p) {
                createWidget(type, p.x, p.y);
            }
        });

        add(buildTop(), BorderLayout.NORTH);
        add(buildPalette(), BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildRight(), BorderLayout.EAST);
        add(buildBottom(), BorderLayout.SOUTH);

        props.bind(project, null);
        logicArea.setProject(project);
        logicArea.setOnDirty(new Runnable() {
            public void run() {
                markDirty();
            }
        });
        logicArea.setText(project.logic);
        updateSizeLabel();
        updateTitle();
        installShortcuts();

        autoSaveTimer = new Timer(15000, e -> autoSave());
        autoSaveTimer.start();
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            updateTitle();
        }
    }

    /** 每 15 秒自动保存：有项目文件就存项目文件，没有就存到用户目录的备份 */
    private void autoSave() {
        if (!dirty) {
            return;
        }
        project.logic = logicArea.getText();
        File target = currentFile != null ? currentFile : autoSaveFile;
        try {
            saveAtomically(project, target);
            dirty = false;
            updateTitle();
            String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            setStatus("已自动保存 " + time + " · " + target.getName());
            if (logicWindow != null) {
                logicWindow.setSaved(time + " · " + target.getName());
            }
        } catch (Exception ex) {
            setStatus("自动保存失败：" + ex.getMessage());
        }
    }

    private void saveAtomically(Project p, File target) throws Exception {
        File tmp = new File(target.getAbsolutePath() + ".tmp");
        appcreator.io.ProjectIO.save(p, tmp);
        try {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---------------- 界面 ----------------

    private JComponent buildTop() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(true);
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(11, 20, 11, 20)));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t = Theme.label("应用创作器", 19, Theme.TEXT);
        t.setFont(Theme.ui(19, Font.BOLD));
        JLabel s = Theme.label("拖拽搭界面 · Java 写逻辑 · 一键导出 EXE", 11, Theme.MUTED);
        titles.add(t);
        titles.add(Box.createVerticalStrut(2));
        titles.add(s);
        bar.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        actions.add(button("新建", Theme.SOFT, Theme.ACCENT_DARK, new Runnable() {
            public void run() {
                newProject();
            }
        }));
        actions.add(button("打开", Theme.SOFT, Theme.ACCENT_DARK, new Runnable() {
            public void run() {
                openProject();
            }
        }));
        actions.add(button("保存", Theme.SOFT, Theme.ACCENT_DARK, new Runnable() {
            public void run() {
                saveProject();
            }
        }));
        actions.add(button("导出 Java", Theme.SOFT, Theme.ACCENT_DARK, new Runnable() {
            public void run() {
                exportJava();
            }
        }));
        actions.add(button("导出 EXE", Theme.SOFT, Theme.ACCENT_DARK, new Runnable() {
            public void run() {
                exportExe();
            }
        }));
        actions.add(button("删除组件", Theme.SOFT, Theme.DANGER, new Runnable() {
            public void run() {
                deleteSelected();
            }
        }));

        mockButton = new RoundButton("模拟窗口", Theme.ACCENT, Color.WHITE);
        mockButton.setToolTipText("在画布外套一层模拟窗口，方便观察窗口大小");
        mockButton.addActionListener(e -> toggleMock());
        actions.add(mockButton);

        actions.add(button("运行预览", Theme.ACCENT, Color.WHITE, new Runnable() {
            public void run() {
                runPreview();
            }
        }));
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void toggleMock() {
        boolean on = !windowMock.isMockEnabled();
        windowMock.setMockEnabled(on);
        if (on) {
            mockButton.setColors(Theme.ACCENT, Color.WHITE);
            setStatus("已显示模拟窗口");
        } else {
            mockButton.setColors(Theme.SOFT, Theme.ACCENT_DARK);
            setStatus("已隐藏模拟窗口");
        }
        windowMock.revalidate();
        windowMock.repaint();
    }

    private RoundButton button(String text, Color bg, Color fg, final Runnable action) {
        RoundButton b = new RoundButton(text, bg, fg);
        b.setBorder(new EmptyBorder(7, 12, 7, 12));
        b.addActionListener(e -> action.run());
        return b;
    }

    private JComponent buildPalette() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(14, 16, 14, 6));

        RoundPanel card = new RoundPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 12, 12, 12));
        card.setPreferredSize(new Dimension(198, 10));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel title = Theme.label("组件库", 13, Theme.ACCENT_DARK);
        title.setFont(Theme.ui(13, Font.BOLD));
        JLabel hint = Theme.label("拖到画布中放置", 11, Theme.MUTED);
        head.add(title);
        head.add(Box.createVerticalStrut(2));
        head.add(hint);
        head.setBorder(new EmptyBorder(0, 4, 10, 4));

        card.add(head, BorderLayout.NORTH);
        card.add(new PalettePanel(new java.util.function.Consumer<String>() {
            public void accept(String type) {
                createWidget(type, project.width / 2, project.height / 2);
            }
        }), BorderLayout.CENTER);

        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildCenter() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 12, 16, 12));
        wrap.add(windowMock);

        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getHorizontalScrollBar().setUnitIncrement(18);
        return sp;
    }

    private JComponent buildRight() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(12, 4, 14, 16));
        wrap.setPreferredSize(new Dimension(336, 10));

        final JPanel body = new JPanel(new CardLayout());
        rightBody = body;
        body.setOpaque(false);
        body.add(props, "props");
        body.add(buildLogicTab(), "logic");

        final RoundButton propButton = new RoundButton("属性", Theme.ACCENT, Color.WHITE);
        final RoundButton logicButton = new RoundButton("逻辑", Theme.SOFT, Theme.ACCENT_DARK);
        propBtn = propButton;
        logicBtn = logicButton;
        propButton.setToolTipText("窗口与组件属性");
        logicButton.setToolTipText("点击打开深色代码编辑器");

        JPanel switchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        switchBar.setOpaque(false);
        switchBar.setBorder(new EmptyBorder(0, 4, 8, 0));
        switchBar.add(propButton);
        switchBar.add(Box.createHorizontalStrut(6));
        switchBar.add(logicButton);

        propButton.addActionListener(e -> {
            ((CardLayout) body.getLayout()).show(body, "props");
            propButton.setColors(Theme.ACCENT, Color.WHITE);
            logicButton.setColors(Theme.SOFT, Theme.ACCENT_DARK);
        });
        logicButton.addActionListener(e -> {
            ((CardLayout) body.getLayout()).show(body, "logic");
            logicButton.setColors(Theme.ACCENT, Color.WHITE);
            propButton.setColors(Theme.SOFT, Theme.ACCENT_DARK);
            openLogicWindow();
        });

        wrap.add(switchBar, BorderLayout.NORTH);
        wrap.add(body, BorderLayout.CENTER);
        return wrap;
    }

    /** 打开（必要时创建）独立的逻辑编辑窗口 */
    private void openLogicWindow() {
        if (logicWindow == null) {
            logicWindow = new LogicWindow(this, logicArea,
                    new Runnable() {
                        public void run() {
                            saveProject();
                        }
                    }, new Runnable() {
                        public void run() {
                            runPreview();
                        }
                    });
            logicWindow.setOnClose(new Runnable() {
                public void run() {
                    autoSave();
                    if (rightBody != null) {
                        ((CardLayout) rightBody.getLayout()).show(rightBody, "props");
                    }
                    if (propBtn != null) {
                        propBtn.setColors(Theme.ACCENT, Color.WHITE);
                    }
                    if (logicBtn != null) {
                        logicBtn.setColors(Theme.SOFT, Theme.ACCENT_DARK);
                    }
                }
            });
        }
        logicWindow.open(project);
    }

    private JComponent buildLogicTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 4, 0, 4));

        RoundPanel card = new RoundPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 16, 18, 16));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel title = Theme.label("深色代码编辑器", 14, Theme.TEXT);
        title.setFont(Theme.ui(14, Font.BOLD));
        JLabel l1 = Theme.label("· Java 语法高亮，关键字 / 字符串 / 注释分色", 11, Theme.MUTED);
        JLabel l2 = Theme.label("· 智能行号：当前行高亮，错误行标红点", 11, Theme.MUTED);
        JLabel l3 = Theme.label("· 停止输入后自动编译，错误画红色波浪线", 11, Theme.MUTED);
        JLabel l4 = Theme.label("· 每 15 秒自动保存，Ctrl+S 立即保存", 11, Theme.MUTED);
        JLabel l5 = Theme.label("· 独立窗口，可拖动、可最大化", 11, Theme.MUTED);

        texts.add(title);
        texts.add(Box.createVerticalStrut(10));
        texts.add(l1);
        texts.add(Box.createVerticalStrut(4));
        texts.add(l2);
        texts.add(Box.createVerticalStrut(4));
        texts.add(l3);
        texts.add(Box.createVerticalStrut(4));
        texts.add(l4);
        texts.add(Box.createVerticalStrut(4));
        texts.add(l5);
        texts.add(Box.createVerticalStrut(16));

        RoundButton open = new RoundButton("打开编辑器", Theme.ACCENT, Color.WHITE);
        open.addActionListener(e -> openLogicWindow());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(open);
        texts.add(btnRow);

        card.add(texts, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildBottom() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 24, 8, 24));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(sizeLabel, BorderLayout.EAST);
        return bar;
    }

    // ---------------- 行为 ----------------

    private void onWindowChanged() {
        canvas.setCanvasSize(project.width, project.height);
        canvas.refreshBackground();
        windowMock.revalidate();
        windowMock.repaint();
        updateSizeLabel();
        updateTitle();
        markDirty();
    }

    private Widget createWidget(String type, int cx, int cy) {
        Dimension d = ComponentFactory.defaultSize(type);
        Widget w = new Widget();
        w.type = type;
        w.name = nextName(type);
        w.w = d.width;
        w.h = d.height;
        w.x = Math.max(0, cx - d.width / 2);
        w.y = Math.max(0, cy - d.height / 2);
        w.text = ComponentFactory.defaultText(type);
        w.fg = Theme.TEXT;
        project.widgets.add(w);
        canvas.addWidget(w);
        props.bind(project, w);
        markDirty();
        setStatus("已添加 " + type + "：" + w.name);
        return w;
    }

    private String nextName(String type) {
        String prefix = ComponentFactory.prefix(type);
        while (true) {
            counter++;
            String candidate = prefix + counter;
            boolean used = false;
            for (Widget w : project.widgets) {
                if (candidate.equals(w.name)) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return candidate;
            }
        }
    }

    private void deleteSelected() {
        Widget sel = canvas.getSelected();
        if (sel == null) {
            setStatus("请先在画布中选中一个组件");
            return;
        }
        project.widgets.remove(sel);
        canvas.removeWidget(sel);
        props.bind(project, null);
        markDirty();
        setStatus("已删除组件：" + sel.name);
    }

    private void newProject() {
        project.reset();
        canvas.setProject(project);
        windowMock.setProject(project);
        props.bind(project, null);
        logicArea.setProject(project);
        logicArea.setText(project.logic);
        if (logicWindow != null) {
            logicWindow.refreshProject(project);
        }
        currentFile = null;
        counter = 0;
        dirty = false;
        updateSizeLabel();
        updateTitle();
        setStatus("已新建空白项目");
    }

    private void openProject() {
        JFileChooser fc = chooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        try {
            Project p = ProjectIO.load(f);
            project = p;
            canvas.setProject(project);
            windowMock.setProject(project);
            props.bind(project, null);
            logicArea.setProject(project);
            logicArea.setText(project.logic);
            logicArea.setCaretPosition(0);
            if (logicWindow != null) {
                logicWindow.refreshProject(project);
            }
            currentFile = f;
            counter = 0;
            dirty = false;
            updateSizeLabel();
            updateTitle();
            setStatus("已打开：" + f.getName());
        } catch (Exception ex) {
            showError("打开失败", String.valueOf(ex.getMessage()));
        }
    }

    private void saveProject() {
        project.logic = logicArea.getText();
        if (currentFile == null) {
            JFileChooser fc = chooser();
            fc.setSelectedFile(new File("我的应用.apcj"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".apcj")) {
                f = new File(f.getParentFile(), f.getName() + ".apcj");
            }
            currentFile = f;
        }
        try {
            ProjectIO.save(project, currentFile);
            dirty = false;
            updateTitle();
            setStatus("已保存：" + currentFile.getName());
        } catch (Exception ex) {
            showError("保存失败", String.valueOf(ex.getMessage()));
        }
    }

    private void exportJava() {
        project.logic = logicArea.getText();
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("导出 Java 源码");
        fc.setSelectedFile(new File("AppMain.java"));
        fc.setFileFilter(new FileNameExtensionFilter("Java 源文件 (*.java)", "java"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".java")) {
            f = new File(f.getParentFile(), f.getName() + ".java");
        }
        try {
            Files.write(f.toPath(), CodeGenerator.generate(project).getBytes(StandardCharsets.UTF_8));
            setStatus("已导出源码：" + f.getAbsolutePath());
        } catch (Exception ex) {
            showError("导出失败", String.valueOf(ex.getMessage()));
        }
    }

    private void exportExe() {
        if (exporting) {
            setStatus("EXE 正在导出，请稍候…");
            return;
        }
        project.logic = logicArea.getText();
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择 EXE 输出文件夹");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setApproveButtonText("导出到此处");
        if (fc.showDialog(this, "导出到此处") != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File dir = fc.getSelectedFile();
        if (dir == null) {
            return;
        }
        final Project snapshot = project;
        exporting = true;
        setStatus("正在生成 EXE（打包 Java 运行时，约需 10~40 秒）…");
        new SwingWorker<Exporter.Result, Void>() {
            @Override
            protected Exporter.Result doInBackground() {
                return Exporter.exportExe(snapshot, dir);
            }

            @Override
            protected void done() {
                exporting = false;
                Exporter.Result r;
                try {
                    r = get();
                } catch (Exception ex) {
                    setStatus("导出 EXE 失败");
                    showError("导出 EXE 失败", String.valueOf(ex));
                    return;
                }
                if (r.error != null) {
                    setStatus("导出 EXE 失败");
                    showError("导出 EXE 失败", r.error);
                    return;
                }
                setStatus("已导出 EXE：" + r.exe.getAbsolutePath());
                Object[] options = {"打开所在文件夹", "完成"};
                int choice = JOptionPane.showOptionDialog(MainFrame.this,
                        "导出成功\n\n" + r.exe.getAbsolutePath()
                                + "\n\n整个文件夹需要一起保留（里面带 Java 运行时）。",
                        "导出 EXE 完成", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (choice == 0) {
                    try {
                        Desktop.getDesktop().open(r.folder);
                    } catch (Exception ex) {
                        showError("打开文件夹失败", String.valueOf(ex));
                    }
                }
            }
        }.execute();
    }

    private void runPreview() {
        if (running) {
            return;
        }
        project.logic = logicArea.getText();
        running = true;
        setStatus("正在编译并启动预览窗口…");
        final Project snapshot = project;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return Runner.run(snapshot);
            }

            @Override
            protected void done() {
                running = false;
                try {
                    String err = get();
                    if (err == null) {
                        setStatus("预览窗口已启动");
                    } else {
                        setStatus("运行失败，请检查逻辑代码");
                        showError("运行失败", err);
                    }
                } catch (Exception ex) {
                    setStatus("运行失败");
                    showError("运行失败", String.valueOf(ex));
                }
            }
        }.execute();
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("创作器项目");
        fc.setFileFilter(new FileNameExtensionFilter("创作器项目 (*.apcj)", "apcj"));
        return fc;
    }

    private void showError(String title, String message) {
        JTextArea ta = new JTextArea(message == null ? "" : message);
        ta.setFont(Theme.mono(12));
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(660, 260));

        Object[] options = {"关闭", "查看生成的代码"};
        int choice = JOptionPane.showOptionDialog(this, sp, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE, null, options, options[0]);
        if (choice == 1) {
            showGeneratedCode();
        }
    }

    private void showGeneratedCode() {
        JTextArea ta = new JTextArea(CodeGenerator.generate(project));
        ta.setFont(Theme.mono(12));
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(820, 560));
        JOptionPane.showMessageDialog(this, sp, "生成的 AppMain.java", JOptionPane.PLAIN_MESSAGE);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void updateSizeLabel() {
        sizeLabel.setText("画布 " + project.width + " × " + project.height);
    }

    private void updateTitle() {
        setTitle("应用创作器 · " + (currentFile == null ? project.title : currentFile.getName()));
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save", new Runnable() {
            public void run() {
                saveProject();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "open", new Runnable() {
            public void run() {
                openProject();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "new", new Runnable() {
            public void run() {
                newProject();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "run", new Runnable() {
            public void run() {
                runPreview();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", new Runnable() {
            public void run() {
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner instanceof javax.swing.text.JTextComponent) {
                    return;
                }
                deleteSelected();
            }
        });
    }

    private void bind(JComponent c, KeyStroke key, String name, final Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
}
