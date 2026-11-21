package appcreator.ui;

import appcreator.gen.CodeGenerator;
import appcreator.model.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.UndoManager;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 逻辑编辑器：Java 语法高亮 + 行号 + 当前行标记 + 实时语法检查 + 撤销重做。
 */
public class CodeEditor extends JPanel {

    // VS Code Dark+ 取色
    private static final Color BG = new Color(0x1E1E1E);
    private static final Color PANEL = new Color(0x252526);
    private static final Color BORDER = new Color(0x3C3C3C);
    private static final Color TEXT_COLOR = new Color(0xD4D4D4);
    private static final Color KEYWORD = new Color(0x569CD6);
    private static final Color CONTROL = new Color(0xC586C0);
    private static final Color COMMENT = new Color(0x6A9955);
    private static final Color STRING = new Color(0xCE9178);
    private static final Color NUMBER = new Color(0xB5CEA8);
    private static final Color TYPE = new Color(0x4EC9B0);
    private static final Color ANNOTATION = new Color(0xDCDCAA);
    private static final Color CURRENT_LINE = new Color(0x2A2D2E);
    private static final Color ERROR_LINE = new Color(0x3B1E1E);
    private static final Color ERROR_COLOR = new Color(0xF14C4C);
    private static final Color GUTTER_TEXT = new Color(0x858585);
    private static final Color GUTTER_ACTIVE = new Color(0xC6C6C6);

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    };

    private final JTextPane text = new JTextPane();
    private final LineGutter gutter = new LineGutter(text);
    private final DefaultListModel<String> problemModel = new DefaultListModel<String>();
    private final JList<String> problemList = new JList<String>(problemModel);
    private final JLabel problemTitle = Theme.label("语法检查", 11, Theme.MUTED);
    private static final Highlighter.HighlightPainter WAVE = new WavePainter();
    private final JLabel caretLabel = new JLabel("第 1 行 · 第 1 列");
    private final JPanel problemBody = new JPanel(new CardLayout());
    private final JLabel cleanLabel = Theme.label("没有语法错误", 11, Theme.ACCENT_DARK);
    private final UndoManager undo = new UndoManager();
    private final Timer highlightTimer;
    private final Timer checkTimer;

    private Project project;
    private Runnable onDirty;
    private Object currentLineTag;
    private final List<Object> errorTags = new ArrayList<Object>();
    private List<Problem> problems = new ArrayList<Problem>();
    private String checkedCode = "";
    private boolean checking;
    private boolean suppressDirty;

    // 自动补全
    private static final int MAX_COMPLETIONS = 9;
    private static final String[] API_WORDS = {
            "System.out.println()", "System.out.print()", "System.err.println()",
            "System.currentTimeMillis()", "System.exit()", "System.getProperty()",
            "Math.abs()", "Math.max()", "Math.min()", "Math.round()", "Math.random()",
            "Math.sqrt()", "Math.pow()",
            "Integer.parseInt()", "Integer.toString()", "Double.parseDouble()",
            "String.valueOf()", "String.format()",
            "Thread.sleep()", "Thread.currentThread()",
            "JOptionPane.showMessageDialog()", "JOptionPane.showInputDialog()",
            "JOptionPane.showConfirmDialog()",
            "JButton", "JLabel", "JTextField", "JTextArea", "JCheckBox", "JRadioButton",
            "JComboBox", "JProgressBar", "JSlider", "JPanel", "JFrame", "JDialog",
            "JScrollPane", "BorderLayout", "FlowLayout", "GridLayout", "BoxLayout",
            "ActionListener", "MouseAdapter", "MouseEvent", "ActionEvent", "Timer",
            "ArrayList", "HashMap", "LinkedHashMap", "List", "Map", "Collections",
            "getText()", "setText()", "setEnabled()", "setVisible()", "setBackground()",
            "setForeground()", "setFont()", "setTitle()", "setSize()", "setBounds()",
            "setLocationRelativeTo()", "setDefaultCloseOperation()", "setEditable()",
            "addActionListener()", "addMouseListener()", "actionPerformed()", "mouseClicked()",
            "repaint()", "revalidate()", "dispose()", "pack()", "requestFocus()",
            "length()", "substring()", "equals()", "equalsIgnoreCase()", "trim()",
            "toLowerCase()", "toUpperCase()", "charAt()", "indexOf()", "replace()",
            "split()", "isEmpty()", "contains()", "startsWith()", "endsWith()",
            "if ()", "else if ()", "for (int i = 0; i < n; i++)", "while ()",
            "try {", "catch (Exception e) {", "public void run()",
            "private void onClick()", "import javax.swing.*", "import java.awt.*"
    };
    private final DefaultListModel<Candidate> completionModel = new DefaultListModel<Candidate>();
    private final JList<Candidate> completionList = new JList<Candidate>(completionModel);
    private JWindow completionWindow;
    private JLabel completionTitle;
    private Timer completionTimer;
    private boolean completionVisible;
    private boolean suppressCompletion;
    private int completionStart = -1;

    private Style plain;
    private Style keyword;
    private Style control;
    private Style comment;
    private Style string;
    private Style number;
    private Style type;
    private Style annotation;

    /** true 表示编辑的是一份独立的完整源码（不再套用设计器生成的框架） */
    private final boolean standalone;

    public CodeEditor() {
        this(false);
    }

    public CodeEditor(boolean standalone) {
        super(new BorderLayout());
        this.standalone = standalone;
        setOpaque(false);

        text.setFont(Theme.mono(14));
        text.setBackground(BG);
        text.setForeground(TEXT_COLOR);
        text.setCaretColor(new Color(0xAEAFAD));
        text.setSelectionColor(new Color(0x264F78));
        text.setSelectedTextColor(Color.WHITE);
        text.setBorder(new EmptyBorder(0, 0, 0, 0));
        text.setMargin(new Insets(8, 10, 12, 10));

        setupStyles();
        setupKeys();
        setupCompletion();
        installBracketFilter();

        JScrollPane sp = new JScrollPane(text);
        sp.setBorder(new javax.swing.border.LineBorder(BORDER, 1));
        sp.setRowHeaderView(gutter);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(BG);

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setOpaque(true);
        card.setBackground(BG);
        card.add(sp, BorderLayout.CENTER);
        card.add(buildProblems(), BorderLayout.SOUTH);
        card.add(buildStatusBar(), BorderLayout.NORTH);

        add(card, BorderLayout.CENTER);

        highlightTimer = new Timer(120, e -> highlight());
        highlightTimer.setRepeats(false);
        checkTimer = new Timer(900, e -> startCheck());
        checkTimer.setRepeats(false);

        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
                if (!suppressCompletion) {
                    scheduleCompletion();
                }
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
                hideCompletion();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
        text.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                undo.addEdit(e.getEdit());
            }
        });
        text.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                gutter.setCaretLine(caretLine());
                updateCaretLabel();
                paintCurrentLine();
            }
        });

        text.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hideCompletion();
            }
        });
        sp.getViewport().addChangeListener(e -> hideCompletion());

        highlight();
        paintCurrentLine();
        showClean();
    }

    // ---------------- 对外接口 ----------------

    public void setProject(Project project) {
        this.project = project;
        checkedCode = null;
        checkTimer.restart();
    }

    /** 立即重新检查（界面刷新后用） */
    public void recheck() {
        checkedCode = null;
        checkTimer.restart();
    }

    public void setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String code) {
        suppressDirty = true;
        undo.discardAllEdits();
        text.setText(code == null ? "" : code);
        text.setCaretPosition(0);
        suppressDirty = false;
        highlight();
        checkTimer.restart();
        revalidate();
        repaint();
    }

    public void setCaretPosition(int pos) {
        text.setCaretPosition(pos);
    }

    public void focusEditor() {
        text.requestFocusInWindow();
    }

    /** 立即检查一次语法，返回问题文本（供自检/测试用，不依赖界面线程） */
    public String checkNow() {
        List<Problem> list = compile(getText());
        StringBuilder sb = new StringBuilder();
        for (Problem p : list) {
            sb.append(p.line > 0 ? "第 " + p.line + " 行：" : "").append(p.message).append("\n");
        }
        return sb.toString();
    }

    // ---------------- 界面 ----------------

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(true);
        bar.setBackground(PANEL);
        bar.setBorder(new EmptyBorder(4, 12, 4, 12));

        caretLabel.setFont(Theme.mono(11));
        caretLabel.setForeground(new Color(0xCCCCCC));

        JLabel hint = new JLabel("Java");
        hint.setFont(Theme.mono(11));
        hint.setForeground(GUTTER_TEXT);

        bar.add(caretLabel, BorderLayout.WEST);
        bar.add(hint, BorderLayout.EAST);
        return bar;
    }

    private void updateCaretLabel() {
        try {
            int pos = text.getCaretPosition();
            Element root = text.getDocument().getDefaultRootElement();
            int line = root.getElementIndex(pos) + 1;
            int lineStart = root.getElement(line - 1).getStartOffset();
            caretLabel.setText("第 " + line + " 行 · 第 " + (pos - lineStart + 1) + " 列");
        } catch (Exception ignored) {
            // 忽略
        }
    }

    private JComponent buildProblems() {
        JPanel wrap = new JPanel(new BorderLayout(0, 2));
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(2, 10, 4, 10));

        problemTitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        wrap.add(problemTitle, BorderLayout.NORTH);

        problemList.setFont(Theme.mono(11));
        problemList.setFixedCellHeight(18);
        problemList.setBorder(null);
        problemList.setBackground(BG);
        problemList.setForeground(new Color(0xF1A7A7));
        problemList.setSelectionBackground(new Color(0x264F78));
        problemList.setSelectionForeground(Color.WHITE);
        problemList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = problemList.locationToIndex(e.getPoint());
                if (i >= 0 && i < problems.size()) {
                    jumpToLine(problems.get(i).line);
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(problemList);
        listScroll.setBorder(null);
        listScroll.setPreferredSize(new Dimension(10, 58));
        listScroll.getVerticalScrollBar().setUnitIncrement(12);

        JPanel clean = new JPanel(new BorderLayout());
        clean.setOpaque(false);
        clean.setBorder(new EmptyBorder(4, 0, 4, 0));
        cleanLabel.setFont(Theme.ui(11));
        clean.add(cleanLabel, BorderLayout.WEST);

        problemBody.setOpaque(false);
        problemBody.add(listScroll, "list");
        problemBody.add(clean, "clean");

        wrap.add(problemBody, BorderLayout.CENTER);
        return wrap;
    }

    private void showClean() {
        ((CardLayout) problemBody.getLayout()).show(problemBody, "clean");
        problemTitle.setText("语法检查 · 自动");
        problemTitle.setForeground(new Color(0x9DA3A8));
        cleanLabel.setText("没有语法错误");
        cleanLabel.setForeground(TYPE);
    }

    private void showProblems(List<Problem> list) {
        problemModel.clear();
        for (Problem p : list) {
            problemModel.addElement(p.line > 0
                    ? "第 " + p.line + " 行 · " + p.message
                    : p.message);
        }
        ((CardLayout) problemBody.getLayout()).show(problemBody, "list");
        problemTitle.setText("语法检查 · " + list.size() + " 个问题（点击跳转）");
        problemTitle.setForeground(ERROR_COLOR);
    }

    // ---------------- 高亮 ----------------

    private void setupStyles() {
        StyledDocument doc = text.getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style def = sc.getStyle(StyleContext.DEFAULT_STYLE);
        Font f = Theme.mono(14);
        StyleConstants.setFontFamily(def, f.getFamily());
        StyleConstants.setFontSize(def, 14);
        doc.setParagraphAttributes(0, doc.getLength(), def, false);

        plain = addStyle("plain", TEXT_COLOR, false, false);
        keyword = addStyle("keyword", KEYWORD, false, false);
        control = addStyle("control", CONTROL, false, false);
        comment = addStyle("comment", COMMENT, false, true);
        string = addStyle("string", STRING, false, false);
        number = addStyle("number", NUMBER, false, false);
        type = addStyle("type", TYPE, false, false);
        annotation = addStyle("annotation", ANNOTATION, false, false);
    }

    private Style addStyle(String name, Color color, boolean bold, boolean italic) {
        StyledDocument doc = text.getStyledDocument();
        Style s = doc.addStyle(name, plain == null ? null : plain);
        StyleConstants.setForeground(s, color);
        StyleConstants.setBold(s, bold);
        StyleConstants.setItalic(s, italic);
        StyleConstants.setFontFamily(s, Theme.mono(14).getFamily());
        StyleConstants.setFontSize(s, 14);
        return s;
    }

    private void highlight() {
        highlightTimer.stop();
        StyledDocument doc = text.getStyledDocument();
        String src = safeText();
        int len = src.length();
        if (len > 60000) {
            return; // 太长就不再着色，保证输入流畅
        }
        doc.setCharacterAttributes(0, len, plain, true);

        int i = 0;
        while (i < len) {
            char c = src.charAt(i);

            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                int end = src.indexOf('\n', i);
                if (end < 0) {
                    end = len;
                }
                style(i, end - i, comment);
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                int end = src.indexOf("*/", i + 2);
                end = end < 0 ? len : end + 2;
                style(i, end - i, comment);
                i = end;
                continue;
            }
            if (c == '"' || c == '\'') {
                int end = scanQuoted(src, i, c);
                style(i, end - i, string);
                i = end;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int end = i + 1;
                while (end < len && Character.isJavaIdentifierPart(src.charAt(end))) {
                    end++;
                }
                String word = src.substring(i, end);
                if (isKeyword(word)) {
                    style(i, end - i, isControl(word) ? control : keyword);
                } else if (word.charAt(0) >= 'A' && word.charAt(0) <= 'Z') {
                    style(i, end - i, type);
                }
                i = end;
                continue;
            }
            if (c == '@' && i + 1 < len && Character.isJavaIdentifierStart(src.charAt(i + 1))) {
                int end = i + 2;
                while (end < len && Character.isJavaIdentifierPart(src.charAt(end))) {
                    end++;
                }
                style(i, end - i, annotation);
                i = end;
                continue;
            }
            if (Character.isDigit(c)) {
                int end = i + 1;
                while (end < len) {
                    char d = src.charAt(end);
                    if (Character.isLetterOrDigit(d) || d == '.' || d == '_') {
                        end++;
                    } else {
                        break;
                    }
                }
                style(i, end - i, number);
                i = end;
                continue;
            }
            i++;
        }
    }

    private String safeText() {
        try {
            return text.getDocument().getText(0, text.getDocument().getLength());
        } catch (BadLocationException e) {
            return text.getText();
        }
    }

    private int scanQuoted(String src, int start, char quote) {
        int i = start + 1;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote || c == '\n') {
                return Math.min(i + 1, src.length());
            }
            i++;
        }
        return src.length();
    }

    private boolean isKeyword(String w) {
        for (String k : KEYWORDS) {
            if (k.equals(w)) {
                return true;
            }
        }
        return false;
    }

    private boolean isControl(String w) {
        String[] controlWords = {"if", "else", "for", "while", "do", "switch", "case", "default",
                "break", "continue", "return", "try", "catch", "finally", "throw", "throws",
                "new", "this", "super", "instanceof", "synchronized", "assert"};
        for (String k : controlWords) {
            if (k.equals(w)) {
                return true;
            }
        }
        return false;
    }

    private void style(int offset, int length, Style s) {
        if (length <= 0) {
            return;
        }
        text.getStyledDocument().setCharacterAttributes(offset, length, s, false);
    }

    // ---------------- 行号与当前行 ----------------

    private int caretLine() {
        Element root = text.getDocument().getDefaultRootElement();
        int line = root.getElementIndex(text.getCaretPosition()) + 1;
        return line;
    }

    private void paintCurrentLine() {
        Highlighter h = text.getHighlighter();
        if (currentLineTag != null) {
            h.removeHighlight(currentLineTag);
            currentLineTag = null;
        }
        Element root = text.getDocument().getDefaultRootElement();
        int idx = root.getElementIndex(text.getCaretPosition());
        if (idx < 0 || idx >= root.getElementCount()) {
            return;
        }
        Element line = root.getElement(idx);
        int start = line.getStartOffset();
        int end = Math.min(line.getEndOffset(), text.getDocument().getLength());
        if (end < start) {
            end = start;
        }
        try {
            currentLineTag = h.addHighlight(start, end, new LinePainter(CURRENT_LINE));
        } catch (BadLocationException ignored) {
            // 忽略
        }
        paintErrorWaves();
    }

    private void jumpToLine(int line) {
        if (line <= 0) {
            return;
        }
        Element root = text.getDocument().getDefaultRootElement();
        int idx = Math.min(line - 1, root.getElementCount() - 1);
        int pos = root.getElement(idx).getStartOffset();
        text.setCaretPosition(pos);
        text.requestFocusInWindow();
    }

    // ---------------- 语法检查 ----------------

    private void changed() {
        if (!suppressDirty && onDirty != null) {
            onDirty.run();
        }
        highlightTimer.restart();
        checkTimer.restart();
        gutter.revalidate();
        gutter.repaint();
        paintCurrentLine();
    }

    private void startCheck() {
        checkTimer.stop();
        final String code = getText();
        if (checking || code.equals(checkedCode)) {
            return;
        }
        checking = true;
        problemTitle.setText("语法检查 · 检查中…");
        problemTitle.setForeground(Theme.MUTED);

        new SwingWorker<List<Problem>, Void>() {
            @Override
            protected List<Problem> doInBackground() {
                return compile(code);
            }

            @Override
            protected void done() {
                checking = false;
                List<Problem> list;
                try {
                    list = get();
                } catch (Exception ex) {
                    list = new ArrayList<Problem>();
                    Problem p = new Problem(0, "检查失败：" + ex);
                    list.add(p);
                }
                if (!getText().equals(code)) {
                    checkTimer.restart();
                    return;
                }
                checkedCode = code;
                applyProblems(list);
            }
        }.execute();
    }

    private void applyProblems(List<Problem> list) {
        problems = list;
        int[] lines = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            lines[i] = list.get(i).line;
        }
        gutter.setErrorLines(lines);
        paintErrorWaves();

        if (list.isEmpty()) {
            showClean();
        } else {
            showProblems(list);
        }
        repaint();
    }

    /** 错误行画红色波浪线（先清掉旧标记再重画，保证显示在其它高亮之上） */
    private void paintErrorWaves() {
        Highlighter h = text.getHighlighter();
        for (Object tag : errorTags) {
            h.removeHighlight(tag);
        }
        errorTags.clear();
        if (problems == null || problems.isEmpty()) {
            return;
        }
        Element root = text.getDocument().getDefaultRootElement();
        for (Problem p : problems) {
            if (p.line <= 0) {
                continue;
            }
            int idx = Math.min(p.line - 1, root.getElementCount() - 1);
            Element line = root.getElement(idx);
            int start = line.getStartOffset();
            int end = Math.min(line.getEndOffset(), text.getDocument().getLength());
            try {
                errorTags.add(h.addHighlight(start, end, WAVE));
            } catch (BadLocationException ignored) {
                // 忽略
            }
        }
    }

    private List<Problem> compile(String logic) {
        List<Problem> list = new ArrayList<Problem>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            list.add(new Problem(0, "当前运行环境没有 javac，无法检查语法"));
            return list;
        }
        Path dir = null;
        try {
            Project copy = new Project();
            if (project != null) {
                copy.title = project.title;
                copy.width = project.width;
                copy.height = project.height;
                copy.bg = project.bg;
                copy.widgets.addAll(project.widgets);
            }
            copy.logic = logic;
            String src = standalone ? logic : CodeGenerator.generate(copy);

            dir = Files.createTempDirectory("appcreator-check-");
            Path file = dir.resolve("AppMain.java");
            Files.write(file, src.getBytes(StandardCharsets.UTF_8));

            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8);
            Iterable<? extends JavaFileObject> units =
                    fm.getJavaFileObjectsFromFiles(Collections.singletonList(file.toFile()));
            StringWriter log = new StringWriter();
            compiler.getTask(log, fm, diags,
                    Arrays.asList("-encoding", "UTF-8", "-g:none", "-d", dir.toString()),
                    null, units).call();
            fm.close();

            int marker = -1;
            String[] lines = src.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("// ----- 逻辑 -----")) {
                    marker = i;
                    break;
                }
            }
            int logicLines = Math.max(1, logic.split("\n", -1).length);
            for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR) {
                    continue;
                }
                long genLine = d.getLineNumber();
                int logicLine = marker >= 0 ? (int) (genLine - marker - 1) : 0;
                // 编译器可能把错误报到生成的收尾代码上，收拢到逻辑代码的最后一行
                if (logicLine > logicLines) {
                    logicLine = logicLines;
                }
                String msg = d.getMessage(Locale.CHINA);
                if (msg != null) {
                    msg = msg.replace("AppMain.java:", "").replaceAll("\\s+", " ").trim();
                }
                list.add(new Problem(logicLine > 0 ? logicLine : 0,
                        msg == null ? "语法错误" : msg));
            }
        } catch (Exception ex) {
            list.add(new Problem(0, "检查失败：" + ex.getMessage()));
        } finally {
            deleteDir(dir);
        }
        return list;
    }

    private void deleteDir(Path dir) {
        if (dir == null) {
            return;
        }
        try {
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            Files.deleteIfExists(dir);
        } catch (Exception ignored) {
            // 清理失败不影响使用
        }
    }

    // ---------------- 括号自动配对 ----------------

    private void installBracketFilter() {
        Document doc = text.getDocument();
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument) doc).setDocumentFilter(new BracketFilter());
        }
    }

    /** 输入左括号自动补右括号；输入已经补好的右括号时直接跳过 */
    private final class BracketFilter extends DocumentFilter {
        private final String open = "([{<";
        private final String close = ")]}>";

        @Override
        public void insertString(FilterBypass fb, int offset, String s, AttributeSet attr)
                throws BadLocationException {
            if (s != null && s.length() == 1 && handle(fb, offset, s.charAt(0), attr)) {
                return;
            }
            super.insertString(fb, offset, s, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String s, AttributeSet attr)
                throws BadLocationException {
            if (length == 0 && s != null && s.length() == 1 && handle(fb, offset, s.charAt(0), attr)) {
                return;
            }
            super.replace(fb, offset, length, s, attr);
        }

        private boolean handle(FilterBypass fb, int offset, char c, AttributeSet attr)
                throws BadLocationException {
            Document doc = fb.getDocument();
            if (doc.getLength() > 60000) {
                return false;
            }
            if (inCommentOrString(offset)) {
                return false;
            }
            int openAt = open.indexOf(c);
            if (openAt >= 0) {
                if (c == '<') {
                    // 尖括号既可能是泛型也可能是比较，只在紧跟在标识符/点后面时才补
                    String all = doc.getText(0, doc.getLength());
                    char prev = offset > 0 ? all.charAt(offset - 1) : ' ';
                    if (!(Character.isJavaIdentifierPart(prev) || prev == '.' || prev == '>')) {
                        return false;
                    }
                }
                // 先插入右括号，再在它前面插入左括号，光标正好落在中间
                fb.insertString(offset, String.valueOf(close.charAt(openAt)), attr);
                fb.insertString(offset, String.valueOf(c), attr);
                moveCaret(offset + 1);
                return true;
            }
            int closeAt = close.indexOf(c);
            if (closeAt >= 0 && offset < doc.getLength()) {
                String all = doc.getText(0, doc.getLength());
                if (all.charAt(offset) == c) {
                    // 已经自动补过：删掉再插回来，光标就走到它后面
                    fb.remove(offset, 1);
                    fb.insertString(offset, String.valueOf(c), attr);
                    moveCaret(offset + 1);
                    return true;
                }
            }
            return false;
        }

        /** 文档过滤器里改不了光标，排到 EDT 上再摆正 */
        private void moveCaret(final int pos) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int len = text.getDocument().getLength();
                    text.setCaretPosition(Math.max(0, Math.min(pos, len)));
                }
            });
        }
    }

    // ---------------- 键盘 ----------------

    private void setupKeys() {
        text.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "insert-tab");
        text.getActionMap().put("insert-tab", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                text.replaceSelection("    ");
            }
        });

        text.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "insert-enter");
        text.getActionMap().put("insert-enter", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                insertNewLine();
            }
        });

        text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK),
                "toggle-comment");
        text.getActionMap().put("toggle-comment", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                toggleComment();
            }
        });

        text.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo-edit");
        text.getActionMap().put("undo-edit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undo.canUndo()) {
                    undo.undo();
                }
            }
        });
        text.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo-edit");
        text.getActionMap().put("redo-edit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undo.canRedo()) {
                    undo.redo();
                }
            }
        });
    }

    private void insertNewLine() {
        String code = getText();
        int pos = text.getCaretPosition();
        int lineStart = code.lastIndexOf('\n', Math.max(0, pos - 1)) + 1;
        String line = code.substring(lineStart, pos);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        String trimmed = line.trim();
        if (trimmed.endsWith("{") || trimmed.endsWith("(")) {
            indent.append("    ");
        }
        text.replaceSelection("\n" + indent.toString());
    }

    private void toggleComment() {
        try {
            Document doc = text.getDocument();
            Element root = doc.getDefaultRootElement();
            int selStart = text.getSelectionStart();
            int selEnd = text.getSelectionEnd();
            int first = root.getElementIndex(selStart);
            int last = root.getElementIndex(Math.max(selStart, selEnd - 1));
            boolean allCommented = true;
            for (int i = first; i <= last; i++) {
                String line = lineText(i);
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    allCommented = false;
                    break;
                }
            }
            if (allCommented) {
                for (int i = last; i >= first; i--) {
                    int start = root.getElement(i).getStartOffset();
                    String line = lineText(i);
                    int at = 0;
                    while (at < line.length() && line.charAt(at) == ' ') {
                        at++;
                    }
                    if (line.startsWith("// ", at)) {
                        doc.remove(start + at, 3);
                    } else if (line.startsWith("//", at)) {
                        doc.remove(start + at, 2);
                    }
                }
            } else {
                for (int i = last; i >= first; i--) {
                    String line = lineText(i);
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    int start = root.getElement(i).getStartOffset();
                    int at = 0;
                    while (at < line.length() && line.charAt(at) == ' ') {
                        at++;
                    }
                    doc.insertString(start + at, "// ", null);
                }
            }
        } catch (Exception ignored) {
            // 忽略编辑异常
        }
    }

    private String lineText(int index) {
        Element root = text.getDocument().getDefaultRootElement();
        Element line = root.getElement(index);
        int start = line.getStartOffset();
        int end = Math.min(line.getEndOffset(), text.getDocument().getLength());
        try {
            return text.getDocument().getText(start, end - start);
        } catch (BadLocationException e) {
            return "";
        }
    }

    // ---------------- 自动补全 ----------------

    private void setupCompletion() {
        completionList.setFont(Theme.mono(13));
        completionList.setFixedCellHeight(22);
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completionList.setFocusable(false);
        completionList.setBackground(new Color(0x252526));
        completionList.setSelectionBackground(new Color(0x094771));
        completionList.setSelectionForeground(Color.WHITE);
        completionList.setCellRenderer(new CandidateRenderer());

        completionTimer = new Timer(80, e -> showCompletion(false));
        completionTimer.setRepeats(false);

        text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK),
                "completion-show");
        text.getActionMap().put("completion-show", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showCompletion(true);
            }
        });

        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!completionVisible) {
                    return;
                }
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    hideCompletion();
                    e.consume();
                } else if (code == KeyEvent.VK_UP) {
                    moveSelection(-1);
                    e.consume();
                } else if (code == KeyEvent.VK_DOWN) {
                    moveSelection(1);
                    e.consume();
                } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_TAB) {
                    acceptCompletion();
                    e.consume();
                }
            }
        });
    }

    /** 打字后稍等一下再弹提示，避免每敲一个字符都刷新 */
    private void scheduleCompletion() {
        if (completionTimer == null) {
            return;
        }
        completionTimer.restart();
    }

    private void showCompletion(boolean manual) {
        if (completionTimer != null) {
            completionTimer.stop();
        }
        int pos = text.getCaretPosition();
        if (text.getSelectionStart() != text.getSelectionEnd()) {
            hideCompletion();
            return;
        }
        int start = wordStart(pos);
        String prefix = safeText().substring(start, pos);
        if (prefix.isEmpty() && !manual) {
            hideCompletion();
            return;
        }
        // 注释和字符串里不自动弹提示（Ctrl+空格 仍可手动打开）
        if (!manual && inCommentOrString(pos)) {
            hideCompletion();
            return;
        }

        List<Candidate> all = collectCandidates();
        List<Candidate> matched = filterCandidates(all, prefix);
        if (matched.isEmpty()) {
            hideCompletion();
            return;
        }
        if (matched.size() > MAX_COMPLETIONS) {
            matched = new ArrayList<Candidate>(matched.subList(0, MAX_COMPLETIONS));
        }

        completionStart = start;
        completionModel.clear();
        for (Candidate c : matched) {
            completionModel.addElement(c);
        }
        completionList.setSelectedIndex(0);
        showCompletionWindow(pos);
        completionVisible = true;
    }

    private void showCompletionWindow(int caretPos) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (completionWindow == null) {
            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(new Color(0x252526));
            body.setBorder(new javax.swing.border.LineBorder(new Color(0x454545), 1));

            completionTitle = new JLabel("补全");
            completionTitle.setFont(Theme.ui(11));
            completionTitle.setForeground(new Color(0x9DA3A8));
            completionTitle.setBorder(new EmptyBorder(4, 10, 4, 10));

            body.add(completionTitle, BorderLayout.NORTH);
            body.add(completionList, BorderLayout.CENTER);

            completionWindow = new JWindow(owner);
            completionWindow.setFocusableWindowState(false);
            completionWindow.setContentPane(body);
        } else if (owner != null && completionWindow.getOwner() != owner) {
            completionWindow.dispose();
            completionWindow = null;
            showCompletionWindow(caretPos);
            return;
        }

        Rectangle r = null;
        try {
            r = text.modelToView(caretPos);
        } catch (BadLocationException ignored) {
            // 忽略
        }
        if (r == null) {
            hideCompletion();
            return;
        }

        int width = 220;
        FontMetrics fm = completionList.getFontMetrics(Theme.mono(13));
        for (int i = 0; i < completionModel.size(); i++) {
            width = Math.max(width, fm.stringWidth(completionModel.get(i).label) + 32);
        }
        width = Math.min(width, 420);
        int height = completionModel.size() * 22 + 26;

        if (completionTitle != null) {
            completionTitle.setText("补全 · " + completionModel.size() + " 项 · Enter 插入 / Esc 关闭");
        }
        width = Math.max(width, completionTitle == null ? 0
                : completionTitle.getPreferredSize().width + 24);

        completionWindow.getContentPane().setPreferredSize(new Dimension(width, height));
        completionWindow.pack();
        try {
            Point screen = text.getLocationOnScreen();
            completionWindow.setLocation(screen.x + r.x, screen.y + r.y + r.height + 2);
        } catch (Exception ignored) {
            // 取不到屏幕坐标时保持默认位置
        }
        completionWindow.setVisible(true);
    }

    private void hideCompletion() {
        completionVisible = false;
        completionStart = -1;
        if (completionWindow != null && completionWindow.isVisible()) {
            completionWindow.setVisible(false);
        }
    }

    private void moveSelection(int delta) {
        int size = completionModel.size();
        if (size == 0) {
            return;
        }
        int index = completionList.getSelectedIndex();
        index = (index + delta + size) % size;
        completionList.setSelectedIndex(index);
        completionList.ensureIndexIsVisible(index);
    }

    private void acceptCompletion() {
        if (!completionVisible || completionStart < 0) {
            hideCompletion();
            return;
        }
        int pos = text.getCaretPosition();
        int index = completionList.getSelectedIndex();
        if (index < 0 || index >= completionModel.size() || pos < completionStart) {
            hideCompletion();
            return;
        }
        int start = completionStart;
        String typed = safeText().substring(start, pos);
        Candidate picked = completionModel.get(index);
        if (!typed.isEmpty() && !picked.label.regionMatches(true, 0, typed, 0, typed.length())) {
            hideCompletion();
            return;
        }

        String insert = picked.text;
        int caretAfter;
        int paren = insert.indexOf("()");
        if (paren >= 0) {
            caretAfter = start + paren + 1;
        } else {
            caretAfter = start + insert.length();
        }
        hideCompletion();
        suppressCompletion = true;
        try {
            Document doc = text.getDocument();
            doc.remove(start, pos - start);
            doc.insertString(start, insert, null);
        } catch (BadLocationException ignored) {
            // 忽略
        } finally {
            suppressCompletion = false;
        }
        text.setCaretPosition(Math.min(caretAfter, text.getDocument().getLength()));
        text.requestFocusInWindow();
    }

    /** 光标是否落在注释或字符串里 */
    private boolean inCommentOrString(int pos) {
        String src = safeText();
        int len = Math.min(pos, src.length());
        int i = 0;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                int end = src.indexOf('\n', i);
                if (end < 0 || end > len) {
                    return true;
                }
                i = end + 1;
                continue;
            }
            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                int end = src.indexOf("*/", i + 2);
                if (end < 0 || end + 2 > len) {
                    return true;
                }
                i = end + 2;
                continue;
            }
            if (c == '"' || c == '\'') {
                int end = scanQuoted(src, i, c);
                if (end > len) {
                    return true;
                }
                i = end;
                continue;
            }
            i++;
        }
        return false;
    }

    /** 光标所在单词的起点 */
    private int wordStart(int pos) {
        String src = safeText();
        int start = Math.min(pos, src.length());
        while (start > 0) {
            char c = src.charAt(start - 1);
            if (Character.isJavaIdentifierPart(c) && c != '$') {
                start--;
            } else {
                break;
            }
        }
        return start;
    }

    private List<Candidate> collectCandidates() {
        LinkedHashSet<Candidate> set = new LinkedHashSet<Candidate>();
        if (project != null && project.widgets != null) {
            for (appcreator.model.Widget w : project.widgets) {
                if (w != null && w.name != null && !w.name.trim().isEmpty()) {
                    set.add(new Candidate(w.name.trim(), w.name.trim()));
                }
            }
        }
        for (String k : KEYWORDS) {
            set.add(new Candidate(k, k));
        }
        for (String w : API_WORDS) {
            set.add(new Candidate(w, w));
        }
        return new ArrayList<Candidate>(set);
    }

    /** 前缀匹配：先字节相同大小写、再忽略大小写开头、最后包含 */
    private List<Candidate> filterCandidates(List<Candidate> all, String prefix) {
        List<Candidate> rank0 = new ArrayList<Candidate>();
        List<Candidate> rank1 = new ArrayList<Candidate>();
        List<Candidate> rank2 = new ArrayList<Candidate>();
        if (prefix.isEmpty()) {
            rank1.addAll(all);
        } else {
            for (Candidate c : all) {
                if (c.label.startsWith(prefix)) {
                    rank0.add(c);
                } else if (c.label.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    rank1.add(c);
                } else if (c.label.toLowerCase(Locale.ROOT).contains(prefix.toLowerCase(Locale.ROOT))) {
                    rank2.add(c);
                }
            }
        }
        List<Candidate> out = new ArrayList<Candidate>(rank0);
        out.addAll(rank1);
        if (!out.isEmpty()) {
            return out;
        }
        return rank2;
    }

    /** 补全项：label 展示，text 插入 */
    private static final class Candidate {
        final String label;
        final String text;

        Candidate(String label, String text) {
            this.label = label;
            this.text = text;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class CandidateRenderer extends JLabel implements ListCellRenderer<Candidate> {
        CandidateRenderer() {
            setOpaque(true);
            setFont(Theme.mono(13));
            setBorder(new EmptyBorder(2, 10, 2, 10));
        }

        public Component getListCellRendererComponent(JList<? extends Candidate> list, Candidate value,
                                                     int index, boolean isSelected, boolean cellHasFocus) {
            setText(value == null ? "" : value.label);
            setBackground(isSelected ? new Color(0x094771) : new Color(0x252526));
            setForeground(isSelected ? Color.WHITE : new Color(0xD4D4D4));
            return this;
        }
    }

    // ---------------- 内部类 ----------------

    private static final class Problem {
        final int line;
        final String message;

        Problem(int line, String message) {
            this.line = line;
            this.message = message;
        }
    }

    /** 当前行整行底色（VS Code 风格，铺满编辑区宽度） */
    private static final class LinePainter implements Highlighter.HighlightPainter {
        private final Color color;

        LinePainter(Color color) {
            this.color = color;
        }

        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                Rectangle r = c.modelToView(p0);
                if (r == null) {
                    return;
                }
                g.setColor(color);
                g.fillRect(0, r.y, c.getWidth(), r.height);
            } catch (BadLocationException ignored) {
                // 忽略
            }
        }
    }

    /** 错误行的红色波浪线 */
    private static final class WavePainter implements Highlighter.HighlightPainter {
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                Rectangle r0 = c.modelToView(p0);
                if (r0 == null) {
                    return;
                }
                Rectangle r1 = c.modelToView(Math.max(p0, p1 - 1));
                int x0 = r0.x;
                int x1 = r1 == null ? x0 + 18 : r1.x + r1.width;
                if (x1 - x0 < 12) {
                    x1 = x0 + 12;
                }
                int y = r0.y + r0.height - 2;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ERROR_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = x0;
                boolean up = true;
                while (x < x1) {
                    int nx = Math.min(x + 2, x1);
                    g2.drawLine(x, up ? y : y - 2, nx, up ? y - 2 : y);
                    up = !up;
                    x = nx;
                }
                g2.dispose();
            } catch (BadLocationException ignored) {
                // 忽略
            }
        }
    }

    /** 行号栏：跟随滚动，当前行高亮，错误行标红 */
    private static final class LineGutter extends JComponent {        private final JTextPane text;
        private int caretLine = 0;
        private int[] errorLines = new int[0];

        LineGutter(JTextPane text) {
            this.text = text;
            setOpaque(true);
            setBackground(BG);
        }

        void setCaretLine(int line) {
            if (caretLine != line) {
                caretLine = line;
                repaint();
            }
        }

        void setErrorLines(int[] lines) {
            errorLines = lines == null ? new int[0] : lines;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int count = text.getDocument().getDefaultRootElement().getElementCount();
            int digits = Math.max(2, String.valueOf(count).length());
            return new Dimension(16 + digits * 9, text.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(BORDER);
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

            Element root = text.getDocument().getDefaultRootElement();
            int count = root.getElementCount();
            for (int i = 0; i < count; i++) {
                int start = root.getElement(i).getStartOffset();
                Rectangle r;
                try {
                    r = text.modelToView(start);
                } catch (BadLocationException e) {
                    continue;
                }
                if (r == null || r.y > getHeight() || r.y + r.height < 0) {
                    continue;
                }
                int line = i + 1;
                boolean error = contains(line);
                boolean active = line == caretLine;

                if (active) {
                    g2.setColor(CURRENT_LINE);
                    g2.fillRect(0, r.y, getWidth() - 1, r.height);
                }

                if (error) {
                    g2.setColor(ERROR_COLOR);
                    g2.fillOval(4, r.y + r.height / 2 - 3, 5, 5);
                }

                Font f = active
                        ? Theme.mono(11).deriveFont(Font.BOLD)
                        : Theme.mono(11);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                String label = String.valueOf(line);
                int x = getWidth() - 8 - fm.stringWidth(label);
                int y = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(active ? GUTTER_ACTIVE : (error ? ERROR_COLOR : GUTTER_TEXT));
                g2.drawString(label, x, y);
            }
            g2.dispose();
        }

        private boolean contains(int line) {
            for (int l : errorLines) {
                if (l == line) {
                    return true;
                }
            }
            return false;
        }
    }
}

