package appcreator;

import appcreator.ui.MainFrame;
import appcreator.ui.Theme;

import javax.swing.*;
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

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
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
