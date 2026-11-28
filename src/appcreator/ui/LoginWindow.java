package appcreator.ui;

import appcreator.io.GitHubAccount;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

/**
 * 启动后的 GitHub 登录窗口：所有文件下载完成后停在这里，登录成功才进主界面。
 *
 * 上次登录过的话（本机存了令牌）会先校验一遍，校验通过就只留一个「进入创作器」按钮，
 * 不用重新输令牌；校验失败或第一次用，就要求填一个 GitHub 个人访问令牌。
 */
public class LoginWindow extends JFrame {

    private static final String TOKENS_URL = "https://github.com/settings/tokens";

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JPasswordField tokenField = new JPasswordField();
    private final JLabel status = new JLabel(" ");
    private final RoundButton loginBtn = new RoundButton("登录", Theme.ACCENT, Color.WHITE);
    private final Runnable onSuccess;

    /**
     * @param verifiedLogin 上次的令牌已经校验通过的账号名；null 表示需要重新登录
     * @param onSuccess     登录成功（或已登录点「进入」）后执行
     */
    public static void open(String verifiedLogin, final Runnable onSuccess) {
        LoginWindow w = new LoginWindow(verifiedLogin, onSuccess);
        w.setVisible(true);
    }

    private LoginWindow(String verifiedLogin, Runnable onSuccess) {
        super("应用创作器 · 登录");
        this.onSuccess = onSuccess;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(24, 26, 22, 26));

        RoundPanel card = new RoundPanel(new BorderLayout());
        card.setFill(Color.WHITE);
        card.setRadius(14);
        card.setBorder(new EmptyBorder(24, 26, 22, 26));

        body.setOpaque(false);
        body.add(buildForm(), "form");
        body.add(buildDone(verifiedLogin), "done");
        card.add(body, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
        setResizable(false);
        pack();
        setSize(470, verifiedLogin == null ? getHeight() : 306);
        setLocationRelativeTo(null);

        cards.show(body, verifiedLogin == null ? "form" : "done");
        if (verifiedLogin != null) {
            getRootPane().setDefaultButton(null);
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (verifiedLogin == null) {
                    tokenField.requestFocusInWindow();
                }
            }
        });
    }

    // ---------------------------------------------------------------- 表单态

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("登录 GitHub");
        title.setFont(Theme.ui(19, Font.BOLD));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = wrapped("所有文件已下载完成。<br>请使用 GitHub 账号登录后继续，令牌只保存在本机。",
                12, Theme.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        JLabel fieldTitle = new JLabel("个人访问令牌");
        fieldTitle.setFont(Theme.ui(12, Font.BOLD));
        fieldTitle.setForeground(Theme.TEXT);
        fieldTitle.setAlignmentX(LEFT_ALIGNMENT);

        tokenField.setFont(Theme.mono(12));
        tokenField.setForeground(Theme.TEXT);
        tokenField.setBackground(new Color(0xF7FBFA));
        tokenField.setCaretColor(Theme.ACCENT_DARK);
        tokenField.setEchoChar('\u2022');
        tokenField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new RoundBorder(8, Theme.BORDER), new EmptyBorder(7, 10, 7, 10)));
        tokenField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tokenField.setAlignmentX(LEFT_ALIGNMENT);
        tokenField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        JLabel help = link("打开 GitHub 令牌页");
        help.setAlignmentX(LEFT_ALIGNMENT);

        status.setFont(Theme.ui(11));
        status.setForeground(Theme.MUTED);
        status.setAlignmentX(LEFT_ALIGNMENT);

        loginBtn.setAlignmentX(LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        JLabel tip = wrapped("提示：令牌形如 ghp_ 开头，只读取公开账号信息，<br>不会上传到任何服务器。",
                11, Theme.MUTED);
        tip.setAlignmentX(LEFT_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(8));
        form.add(sub);
        form.add(Box.createVerticalStrut(18));
        form.add(fieldTitle);
        form.add(Box.createVerticalStrut(6));
        form.add(tokenField);
        form.add(Box.createVerticalStrut(6));
        form.add(help);
        form.add(Box.createVerticalStrut(14));
        form.add(status);
        form.add(Box.createVerticalStrut(8));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(10));
        form.add(tip);
        return form;
    }

    private void doLogin() {
        final String token = new String(tokenField.getPassword()).trim();
        if (token.length() == 0) {
            status.setForeground(Theme.DANGER);
            status.setText("请先填令牌");
            return;
        }
        loginBtn.setEnabled(false);
        tokenField.setEnabled(false);
        status.setForeground(Theme.MUTED);
        status.setText("正在验证令牌…");
        new Thread(new Runnable() {
            public void run() {
                final GitHubAccount.Result r = GitHubAccount.verify(token);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        loginBtn.setEnabled(true);
                        tokenField.setEnabled(true);
                        if (r.ok) {
                            GitHubAccount.save(r.login, token);
                            status.setForeground(Theme.ACCENT_DARK);
                            status.setText("登录成功，欢迎 " + r.login);
                            enter();
                        } else {
                            status.setForeground(Theme.DANGER);
                            status.setText(r.message);
                            tokenField.requestFocusInWindow();
                        }
                    }
                });
            }
        }, "github-login").start();
    }

    // ------------------------------------------------------------ 已登录态

    private JPanel buildDone(String login) {
        JPanel done = new JPanel();
        done.setOpaque(false);
        done.setLayout(new BoxLayout(done, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("已登录 GitHub");
        title.setFont(Theme.ui(19, Font.BOLD));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel who = new JLabel("账号：" + (login == null ? "" : login));
        who.setFont(Theme.ui(13, Font.BOLD));
        who.setForeground(Theme.ACCENT_DARK);
        who.setAlignmentX(LEFT_ALIGNMENT);
        who.setPreferredSize(new Dimension(360, 22));
        who.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel sub = wrapped("所有文件已下载完成，账号校验通过。<br>点下面的按钮进入创作器。",
                12, Theme.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        RoundButton enterBtn = new RoundButton("进入创作器", Theme.ACCENT, Color.WHITE);
        enterBtn.setAlignmentX(LEFT_ALIGNMENT);
        enterBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        enterBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enter();
            }
        });
        getRootPane().setDefaultButton(enterBtn);

        JLabel switchUser = link("换个账号");
        switchUser.setAlignmentX(LEFT_ALIGNMENT);
        switchUser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                GitHubAccount.clear();
                cards.show(body, "form");
                tokenField.setText("");
                status.setForeground(Theme.MUTED);
                status.setText("请填新的令牌");
                tokenField.requestFocusInWindow();
            }
        });

        done.add(title);
        done.add(Box.createVerticalStrut(6));
        done.add(who);
        done.add(Box.createVerticalStrut(12));
        done.add(sub);
        done.add(Box.createVerticalStrut(22));
        done.add(enterBtn);
        done.add(Box.createVerticalStrut(12));
        done.add(switchUser);
        return done;
    }

    private void enter() {
        setVisible(false);
        dispose();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    // ---------------------------------------------------------------- 小工具

    private static JLabel wrapped(String text, int size, Color color) {
        JLabel l = new JLabel("<html><body style=\"width:340px\">" + text + "</body></html>");
        l.setFont(Theme.ui(size));
        l.setForeground(color);
        return l;
    }

    private static JLabel link(String text) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setFont(Theme.ui(11));
        l.setForeground(Theme.ACCENT_DARK);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                browse(TOKENS_URL);
            }
        });
        return l;
    }

    private static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
            // 打不开浏览器就退回命令行
        }
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception ignored) {
            // 都没成功就由用户自己复制地址
        }
    }
}
