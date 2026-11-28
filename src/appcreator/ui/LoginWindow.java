package appcreator.ui;

import appcreator.io.Account;
import appcreator.io.License;

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
 * 启动后的登录窗口：所有文件下载完成后停在这里，登录成功才进主界面。
 *
 * 两种登录方式：GitHub 个人访问令牌、微软账号（设备码授权，浏览器里输验证码）。
 * 上次登录过的话会显示「已登录」卡片，点「进入创作器」即可，不用重新登录。
 */
public class LoginWindow extends JFrame {

    private static final String GITHUB_TOKENS = "https://github.com/settings/tokens";

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JPanel switchBar = new JPanel(new GridLayout(1, 2, 8, 0));
    private final RoundButton ghTab = new RoundButton("GitHub 登录", icon("github.png", 20), Theme.ACCENT, Color.WHITE);
    private final RoundButton msTab = new RoundButton("微软账号登录", icon("microsoft.png", 20), Theme.SOFT, Theme.ACCENT_DARK);
    private final Runnable onSuccess;

    private final JPasswordField tokenField = new JPasswordField();
    private final JLabel ghStatus = new JLabel(" ");
    private final RoundButton ghBtn = new RoundButton("登录", Theme.ACCENT, Color.WHITE);

    private final JLabel msCode = new JLabel("");
    private final JLabel msStatus = new JLabel(" ");
    private final RoundButton msBtn = new RoundButton("用微软账号登录", Theme.ACCENT, Color.WHITE);

    private final JTextField subField = new JTextField();
    private final JLabel subStatus = new JLabel(" ");
    private JPanel subPanel;

    private JPanel donePanel;
    private boolean busy;

    /**
     * @param verifiedName 上次登录的账号（令牌已校验通过 / 微软账号本机记录），null 表示需要登录
     * @param onSuccess    登录成功后执行
     */
    public static void open(String verifiedName, final Runnable onSuccess) {
        LoginWindow w = new LoginWindow(verifiedName, onSuccess);
        w.setVisible(true);
    }

    private LoginWindow(String verifiedName, Runnable onSuccess) {
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
        body.add(buildGitHub(), "github");
        body.add(buildMicrosoft(), "ms");
        donePanel = buildDone(null);
        body.add(donePanel, "done");
        subPanel = buildSub();
        body.add(subPanel, "sub");

        switchBar.setOpaque(false);
        switchBar.setBorder(new EmptyBorder(0, 0, 18, 0));
        switchBar.add(ghTab);
        switchBar.add(msTab);
        ghTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                show("github");
            }
        });
        msTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                show("ms");
            }
        });

        card.add(switchBar, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
        setResizable(false);

        if (verifiedName == null) {
            show("github");
        } else {
            donePanel = buildDone(verifiedName);
            body.remove(body.getComponent(2));
            body.add(donePanel, "done");
            show("done");
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (verifiedName == null) {
                    tokenField.requestFocusInWindow();
                }
            }
        });
    }

    /** 切到某张卡片，同时同步两个按钮的选中样式（已登录 / 订阅卡片不显示按钮） */
    private void show(String name) {
        cards.show(body, name);
        boolean login = "github".equals(name) || "ms".equals(name);
        switchBar.setVisible(login);
        boolean github = "github".equals(name);
        ghTab.setColors(github ? Theme.ACCENT : Theme.SOFT, github ? Color.WHITE : Theme.ACCENT_DARK);
        msTab.setColors(github ? Theme.SOFT : Theme.ACCENT, github ? Theme.ACCENT_DARK : Color.WHITE);
        fit();
    }

    /** 高度按当前卡片算：GitHub / 微软 / 已登录三张卡片高度不一样，不留大片空白 */
    private void fit() {
        pack();
        Component visible = null;
        for (Component c : body.getComponents()) {
            if (c.isVisible()) {
                visible = c;
            }
        }
        int extra = getHeight() - body.getPreferredSize().height;
        int cardHeight = visible == null ? body.getPreferredSize().height : visible.getPreferredSize().height;
        setSize(470, Math.max(280, cardHeight + extra));
        setLocationRelativeTo(null);
        validate();
    }

    // ------------------------------------------------------------ GitHub

    private JPanel buildGitHub() {
        JPanel form = column();

        form.add(title("登录 GitHub"));
        form.add(Box.createVerticalStrut(8));
        form.add(wrapped("所有文件已下载完成。<br>填一个个人访问令牌即可登录，令牌只保存在本机。", 12, Theme.MUTED));
        form.add(Box.createVerticalStrut(18));
        form.add(bold("个人访问令牌"));
        form.add(Box.createVerticalStrut(6));

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
                doGitHub();
            }
        });
        form.add(tokenField);
        form.add(Box.createVerticalStrut(6));

        JLabel help = link("打开 GitHub 令牌页", GITHUB_TOKENS);
        form.add(help);
        form.add(Box.createVerticalStrut(14));

        ghStatus.setFont(Theme.ui(11));
        ghStatus.setForeground(Theme.MUTED);
        ghStatus.setAlignmentX(LEFT_ALIGNMENT);
        form.add(ghStatus);
        form.add(Box.createVerticalStrut(8));

        ghBtn.setAlignmentX(LEFT_ALIGNMENT);
        ghBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        ghBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doGitHub();
            }
        });
        form.add(ghBtn);
        form.add(Box.createVerticalStrut(10));

        JLabel tip = wrapped("提示：令牌形如 ghp_ 开头，只读取公开账号信息，<br>不会上传到任何服务器。", 11, Theme.MUTED);
        form.add(tip);
        return form;
    }

    private void doGitHub() {
        if (busy) {
            return;
        }
        final String token = new String(tokenField.getPassword()).trim();
        if (token.length() == 0) {
            ghStatus.setForeground(Theme.DANGER);
            ghStatus.setText("请先填令牌");
            return;
        }
        busy = true;
        ghBtn.setEnabled(false);
        tokenField.setEnabled(false);
        ghStatus.setForeground(Theme.MUTED);
        ghStatus.setText("正在验证令牌…");
        new Thread(new Runnable() {
            public void run() {
                final Account.Result r = Account.loginGitHub(token);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        busy = false;
                        ghBtn.setEnabled(true);
                        tokenField.setEnabled(true);
                        if (r.ok) {
                            ghStatus.setForeground(Theme.ACCENT_DARK);
                            ghStatus.setText("登录成功，欢迎 " + r.name);
                            showDone(r.name);
                        } else {
                            ghStatus.setForeground(Theme.DANGER);
                            ghStatus.setText(r.message);
                            tokenField.requestFocusInWindow();
                        }
                    }
                });
            }
        }, "github-login").start();
    }

    // ------------------------------------------------------------ 微软账号

    private JPanel buildMicrosoft() {
        JPanel form = column();

        form.add(title("登录微软账号"));
        form.add(Box.createVerticalStrut(8));
        form.add(wrapped("点下面的按钮，程序会给出一个验证码。<br>在浏览器打开 microsoft.com/devicelogin 输入验证码，<br>用微软账号登录即可，密码不会经过本程序。", 12, Theme.MUTED));
        form.add(Box.createVerticalStrut(16));

        msCode.setFont(Theme.ui(12));
        msCode.setForeground(Theme.MUTED);
        msCode.setText("点击下面的按钮获取验证码");
        msCode.setAlignmentX(LEFT_ALIGNMENT);
        msCode.setPreferredSize(new Dimension(240, 30));
        msCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        form.add(msCode);
        form.add(Box.createVerticalStrut(12));

        msStatus.setFont(Theme.ui(11));
        msStatus.setForeground(Theme.MUTED);
        msStatus.setAlignmentX(LEFT_ALIGNMENT);
        form.add(msStatus);
        form.add(Box.createVerticalStrut(8));

        msBtn.setAlignmentX(LEFT_ALIGNMENT);
        msBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        msBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doMicrosoft();
            }
        });
        form.add(msBtn);
        return form;
    }

    private void doMicrosoft() {
        if (busy) {
            return;
        }
        busy = true;
        msBtn.setEnabled(false);
        msCode.setFont(Theme.mono(22));
        msCode.setForeground(Theme.ACCENT_DARK);
        msCode.setText("···");
        msStatus.setForeground(Theme.MUTED);
        msStatus.setText("正在获取验证码…");
        new Thread(new Runnable() {
            public void run() {
                final Account.Result r = Account.loginMicrosoft(new Account.Prompt() {
                    public void show(final String code, final String url, final String message) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                msCode.setFont(Theme.mono(22));
                                msCode.setForeground(Theme.ACCENT_DARK);
                                msCode.setText(code);
                                msStatus.setForeground(Theme.TEXT);
                                msStatus.setText(message);
                                browse(url);
                            }
                        });
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        busy = false;
                        msBtn.setEnabled(true);
                        if (r.ok) {
                            msStatus.setForeground(Theme.ACCENT_DARK);
                            msStatus.setText("登录成功");
                            showDone(r.name);
                        } else {
                            msStatus.setForeground(Theme.DANGER);
                            msStatus.setText(r.message);
                        }
                    }
                });
            }
        }, "ms-login").start();
    }

    // ------------------------------------------------------------ 已登录

    private JPanel buildDone(String name) {
        JPanel done = column();
        String who = name == null ? Account.name() : name;
        String platform = Account.isLoggedIn() ? Account.platformName() : "GitHub";

        JLabel avatar = new JLabel(new ImageIcon(Avatars.render(46, who)));
        avatar.setAlignmentX(LEFT_ALIGNMENT);
        done.add(avatar);
        done.add(Box.createVerticalStrut(12));

        done.add(title("已登录 " + platform));
        done.add(Box.createVerticalStrut(4));

        JLabel account = new JLabel(Account.isLoggedIn() ? Account.display() : (who == null ? "" : who));
        account.setFont(Theme.ui(12));
        account.setForeground(Theme.MUTED);
        account.setAlignmentX(LEFT_ALIGNMENT);
        done.add(account);
        done.add(Box.createVerticalStrut(14));

        done.add(wrapped("所有文件已下载完成，账号校验通过。", 12, Theme.MUTED));
        done.add(Box.createVerticalStrut(8));

        boolean active = License.active();
        JLabel lic = new JLabel((active ? "● " : "○ ") + License.statusText());
        lic.setFont(Theme.ui(12));
        lic.setForeground(active ? Theme.ACCENT_DARK : new Color(0xC2410C));
        lic.setAlignmentX(LEFT_ALIGNMENT);
        done.add(lic);
        done.add(Box.createVerticalStrut(16));

        RoundButton enter = new RoundButton(active ? "进入创作器" : "输入订阅码续订", Theme.ACCENT, Color.WHITE);
        enter.setAlignmentX(LEFT_ALIGNMENT);
        enter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        enter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enter();
            }
        });
        getRootPane().setDefaultButton(enter);
        done.add(enter);
        done.add(Box.createVerticalStrut(12));

        JLabel switchUser = link("换个账号", null);
        switchUser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Account.clear();
                tokenField.setText("");
                ghStatus.setForeground(Theme.MUTED);
                ghStatus.setText("请填新的令牌");
                show("github");
                tokenField.requestFocusInWindow();
            }
        });
        done.add(switchUser);
        return done;
    }

    private void showDone(String name) {
        body.remove(donePanel);
        donePanel = buildDone(name);
        body.add(donePanel, "done");
        show("done");
    }

    private void enter() {
        if (!License.active()) {
            show("sub");
            subField.requestFocusInWindow();
            return;
        }
        setVisible(false);
        dispose();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    // ------------------------------------------------------------ 订阅

    private JPanel buildSub() {
        JPanel form = column();

        form.add(title("输入订阅码"));
        form.add(Box.createVerticalStrut(8));
        form.add(wrapped("免费试用已经结束。输入一个订阅码即可再续订 30 天，"
                + "<br>订阅只记录在本机，不联网校验。", 12, Theme.MUTED));
        form.add(Box.createVerticalStrut(16));
        form.add(bold("订阅码"));
        form.add(Box.createVerticalStrut(6));

        subField.setFont(Theme.mono(13));
        subField.setForeground(Theme.TEXT);
        subField.setBackground(new Color(0xF7FBFA));
        subField.setCaretColor(Theme.ACCENT_DARK);
        subField.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(8, Theme.BORDER), new EmptyBorder(7, 10, 7, 10)));
        subField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        subField.setAlignmentX(LEFT_ALIGNMENT);
        subField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSub();
            }
        });
        form.add(subField);
        form.add(Box.createVerticalStrut(6));
        form.add(wrapped("格式形如 " + License.sample() + "，大小写和横线都不计较。", 11, Theme.MUTED));
        form.add(Box.createVerticalStrut(14));

        subStatus.setFont(Theme.ui(11));
        subStatus.setForeground(Theme.MUTED);
        subStatus.setText(" ");
        subStatus.setAlignmentX(LEFT_ALIGNMENT);
        form.add(subStatus);
        form.add(Box.createVerticalStrut(8));

        RoundButton ok = new RoundButton("激活订阅", Theme.ACCENT, Color.WHITE);
        ok.setAlignmentX(LEFT_ALIGNMENT);
        ok.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSub();
            }
        });
        getRootPane().setDefaultButton(ok);
        form.add(ok);
        form.add(Box.createVerticalStrut(12));

        JLabel back = link("稍后再说，退出程序", null);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });
        form.add(back);
        return form;
    }

    private void doSub() {
        String msg = License.redeem(subField.getText());
        boolean ok = msg.startsWith("订阅已续期");
        subStatus.setForeground(ok ? Theme.ACCENT_DARK : new Color(0xC2410C));
        subStatus.setText(msg);
        if (ok) {
            subField.setText("");
            subPanel.setPreferredSize(subPanel.getPreferredSize());
            showDone(Account.isLoggedIn() ? Account.name() : null);
        }
    }

    // ---------------------------------------------------------------- 小工具

    /** 从 jar 里读品牌图标（源图 256×256），逐级减半缩到目标尺寸，边缘更干净；读不到就返回 null（按钮只剩文字） */
    private static Icon icon(String file, final int size) {
        try {
            java.io.InputStream in = LoginWindow.class.getResourceAsStream("/appcreator/ui/icons/" + file);
            if (in == null) {
                return null;
            }
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            in.close();
            if (img == null) {
                return null;
            }
            while (img.getWidth() / 2 >= size) {
                img = scale(img, Math.max(size, img.getWidth() / 2));
            }
            java.awt.image.BufferedImage out = scale(img, size);
            return new ImageIcon(out);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static java.awt.image.BufferedImage scale(java.awt.image.BufferedImage src, int size) {
        java.awt.image.BufferedImage out =
                new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setComposite(java.awt.AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.ui(19, Font.BOLD));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.ui(12, Font.BOLD));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel wrapped(String text, int size, Color color) {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setFont(Theme.ui(size));
        l.setForeground(color);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /** 蓝色下划线链接；url 为 null 时只当可点的文字（由外面挂点击动作） */
    private static JLabel link(final String text, String url) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setFont(Theme.ui(11));
        l.setForeground(Theme.ACCENT_DARK);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setAlignmentX(LEFT_ALIGNMENT);
        if (url != null) {
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    browse(url);
                }
            });
        }
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
