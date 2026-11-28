package appcreator;

import appcreator.gen.JavaEnv;
import appcreator.io.LanguageSync;
import appcreator.ui.MainFrame;
import appcreator.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 应用创作器启动类。
 */
public class AppCreator {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 使用默认外观
        }
        tuneDefaults();

        // 启动流程：适配字体 → 下载语言包 → 检测 Java → 进主界面；每一步都显示在启动画面上
        final JWindow splash = buildSplash();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                splash.setVisible(true);
            }
        });
        new Thread(new Runnable() {
            public void run() {
                step("正在适配本机字体…");
                appcreator.ui.Fonts.syncLanguage();
                step("正在下载语言包…");
                LanguageSync.sync(new LanguageSync.Listener() {
                    public void step(String text) {
                        AppCreator.step(text);
                    }
                });
                // 韩文界面：本机没有能显示谚文的字体就后台下载一份 NanumGothic，不拖慢启动
                if (appcreator.ui.Fonts.needsKoreanFont()) {
                    step("正在后台下载韩文字体…");
                    appcreator.ui.Fonts.prepareKoreanFontAsync();
                }
                // 下载教程（4 份 .md），失败就用 jar 里内置的那份
                step("正在下载教程…");
                appcreator.io.TutorialSync.sync(new appcreator.io.TutorialSync.Listener() {
                    public void step(String text) {
                        AppCreator.step(text);
                    }
                });
                // 下载 .NET 工具箱组件数据（容器/菜单/数据/输入/对话框等），失败就用内置那份
                step("正在下载组件数据…");
                appcreator.io.ComponentData.sync();
                step("正在检测本机 Java 版本并验证是否可用…");
                JavaEnv.detect();
                // 所有文件下载完，先过 GitHub 登录：本机存过令牌就校验一次，不通过再让人填
                step("正在确认 GitHub 登录状态…");
                final String verifiedLogin;
                if (appcreator.io.GitHubAccount.savedToken() != null) {
                    appcreator.io.GitHubAccount.Result r =
                            appcreator.io.GitHubAccount.verify(appcreator.io.GitHubAccount.savedToken());
                    verifiedLogin = r.ok ? r.login : null;
                } else {
                    verifiedLogin = null;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        splash.dispose();
                        appcreator.ui.LoginWindow.open(verifiedLogin, new Runnable() {
                            public void run() {
                                MainFrame frame = new MainFrame();
                                frame.setVisible(true);
                            }
                        });
                    }
                });
            }
        }, "startup").start();
    }

    private static JLabel splashSub;

    /** 更新启动画面上的提示文字 */
    private static void step(final String text) {
        final JLabel label = splashSub;
        if (label == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                label.setText(text);
            }
        });
    }

    /** 启动画面：适配字体、下载语言包、检测 Java 时停留在这里 */
    private static JWindow buildSplash() {
        JWindow win = new JWindow();
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));

        JLabel title = new JLabel("应用创作器");
        title.setFont(Theme.ui(17, Font.BOLD));
        title.setForeground(Theme.ACCENT_DARK);
        JLabel sub = new JLabel("正在适配本机字体…");
        sub.setFont(Theme.ui(12));
        sub.setForeground(Theme.MUTED);
        splashSub = sub;

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(title);
        texts.add(Box.createVerticalStrut(6));
        texts.add(sub);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(340, 8));
        bar.setForeground(Theme.ACCENT);

        root.add(texts, BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        win.setContentPane(root);
        win.setSize(420, 132);
        win.setLocationRelativeTo(null);
        return win;
    }

    private static void tuneDefaults() {
        UIManager.put("TabbedPane.font", Theme.ui(13));
        UIManager.put("TabbedPane.background", Color.WHITE);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.foreground", Theme.TEXT);
        UIManager.put("OptionPane.messageFont", Theme.ui(13));
        UIManager.put("OptionPane.buttonFont", Theme.ui(13));
        UIManager.put("ToolTip.font", Theme.ui(11));
        UIManager.put("CheckBox.font", Theme.ui(12));
        UIManager.put("ComboBox.font", Theme.ui(12));
        UIManager.put("Label.font", Theme.ui(12));
        UIManager.put("TextField.font", Theme.ui(12));
        UIManager.put("TextArea.font", Theme.mono(13));
        UIManager.put("Panel.background", Theme.BG);
        UIManager.put("ScrollPane.background", Theme.BG);
    }
}
