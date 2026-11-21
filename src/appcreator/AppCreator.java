package appcreator;

import appcreator.gen.JavaEnv;
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

        // 先把 Java 检测跑完再进主界面：启动画面停在这里，检测完自动关闭
        final JWindow splash = buildSplash();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                splash.setVisible(true);
            }
        });
        new Thread(new Runnable() {
            public void run() {
                JavaEnv.detect();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        splash.dispose();
                        MainFrame frame = new MainFrame();
                        frame.setVisible(true);
                    }
                });
            }
        }, "detect-java").start();
    }

    /** 启动画面：检测 Java 时停留在这里 */
    private static JWindow buildSplash() {
        JWindow win = new JWindow();
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));

        JLabel title = new JLabel("应用创作器");
        title.setFont(Theme.ui(17, Font.BOLD));
        title.setForeground(Theme.ACCENT_DARK);
        JLabel sub = new JLabel("正在检测本机 Java 版本并验证是否可用…");
        sub.setFont(Theme.ui(12));
        sub.setForeground(Theme.MUTED);

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
