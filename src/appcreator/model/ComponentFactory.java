package appcreator.model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责在“设计画布”和“生成的代码”之间保持一致的组件工厂。
 */
public final class ComponentFactory {

    /** 供调色板显示的组件类型（顺序即显示顺序） */
    public static final String[] TYPES = {
            "标签", "按钮", "文本框", "文本域", "复选框",
            "单选框", "下拉框", "进度条", "滑块"
    };

    /** 调色板小图标上的单字 */
    public static final char[] GLYPHS = {
            '标', '钮', '框', '域', '复', '单', '选', '进', '滑'
    };

    private static final String[] CLASSES = {
            "JLabel", "JButton", "JTextField", "JTextArea", "JCheckBox",
            "JRadioButton", "JComboBox", "JProgressBar", "JSlider"
    };

    private static final String[] PREFIXES = {
            "label", "button", "textField", "textArea", "checkBox",
            "radioButton", "comboBox", "progressBar", "slider"
    };

    private ComponentFactory() {
    }

    private static int indexOf(String type) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i;
            }
        }
        return 1;
    }

    public static String className(String type) {
        return CLASSES[indexOf(type)];
    }

    public static String prefix(String type) {
        return PREFIXES[indexOf(type)];
    }

    /** 新建组件时的默认尺寸 */
    public static Dimension defaultSize(String type) {
        if ("标签".equals(type)) {
            return new Dimension(96, 26);
        }
        if ("按钮".equals(type)) {
            return new Dimension(104, 34);
        }
        if ("文本框".equals(type)) {
            return new Dimension(160, 32);
        }
        if ("文本域".equals(type)) {
            return new Dimension(180, 90);
        }
        if ("复选框".equals(type) || "单选框".equals(type)) {
            return new Dimension(104, 26);
        }
        if ("下拉框".equals(type)) {
            return new Dimension(140, 32);
        }
        if ("进度条".equals(type)) {
            return new Dimension(160, 24);
        }
        return new Dimension(180, 40); // 滑块
    }

    /** 新建组件时的默认文本 */
    public static String defaultText(String type) {
        if ("标签".equals(type)) {
            return "标签文本";
        }
        if ("按钮".equals(type)) {
            return "按钮";
        }
        if ("复选框".equals(type)) {
            return "复选框";
        }
        if ("单选框".equals(type)) {
            return "单选框";
        }
        return "";
    }

    /** 根据模型创建真实 Swing 组件（与生成的代码完全一致的控件类型） */
    public static JComponent create(Widget w) {
        JComponent c;
        if ("标签".equals(w.type)) {
            c = new JLabel();
        } else if ("按钮".equals(w.type)) {
            c = new JButton();
        } else if ("文本框".equals(w.type)) {
            JTextField f = new JTextField();
            f.setBorder(inputBorder(4, 8));
            c = f;
        } else if ("文本域".equals(w.type)) {
            JTextArea a = new JTextArea();
            a.setBorder(inputBorder(6, 9));
            c = a;
        } else if ("复选框".equals(w.type)) {
            c = new JCheckBox();
        } else if ("单选框".equals(w.type)) {
            c = new JRadioButton();
        } else if ("下拉框".equals(w.type)) {
            c = new JComboBox<String>();
        } else if ("进度条".equals(w.type)) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(60);
            c = bar;
        } else if ("滑块".equals(w.type)) {
            c = new JSlider(0, 100, 50);
        } else {
            c = new JLabel();
        }
        if (c instanceof JComponent) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        // 设计态不参与焦点，避免文字外面出现虚线焦点框
        c.setFocusable(false);
        c.setRequestFocusEnabled(false);
        if (c instanceof AbstractButton) {
            ((AbstractButton) c).setFocusPainted(false);
        }
        apply(w, c);
        return c;
    }

    /** 输入框的浅色圆角边框，和生成的代码保持一致 */
    public static javax.swing.border.Border inputBorder(int topBottom, int leftRight) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xE3EBE8), 1, true),
                new EmptyBorder(topBottom, leftRight, topBottom, leftRight));
    }

    /** 把模型属性写回已有组件 */
    public static void apply(Widget w, JComponent c) {
        c.setBounds(w.x, w.y, w.w, w.h);
        c.setFont(new Font("微软雅黑", w.bold ? Font.BOLD : Font.PLAIN, w.fontSize));
        if (!(c instanceof JProgressBar) && !(c instanceof JSlider)) {
            c.setForeground(w.fg == null ? new Color(0x22312E) : w.fg);
        }

        if (c instanceof JLabel) {
            ((JLabel) c).setText(w.text);
        } else if (c instanceof AbstractButton) {
            ((AbstractButton) c).setText(w.text);
        } else if (c instanceof JTextField) {
            JTextField f = (JTextField) c;
            if (!f.getText().equals(w.text)) {
                f.setText(w.text);
            }
        } else if (c instanceof JTextArea) {
            JTextArea a = (JTextArea) c;
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
            if (!a.getText().equals(w.text)) {
                a.setText(w.text);
            }
        } else if (c instanceof JComboBox) {
            @SuppressWarnings("rawtypes")
            JComboBox box = (JComboBox) c;
            String[] items = splitItems(w.items);
            if (box.getItemCount() != items.length) {
                box.removeAllItems();
                for (String s : items) {
                    box.addItem(s);
                }
            }
        }

        if (w.bg != null) {
            c.setOpaque(true);
            c.setBackground(w.bg);
        } else if (c instanceof JTextArea) {
            ((JTextArea) c).setBackground(Color.WHITE);
        } else if (c instanceof JTextField) {
            ((JTextField) c).setBackground(Color.WHITE);
        } else if (c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setOpaque(false);
        }
    }

    public static String[] splitItems(String s) {
        if (s == null || s.trim().length() == 0) {
            return new String[]{"选项1"};
        }
        String[] raw = s.split("[,，]");
        List<String> list = new ArrayList<String>();
        for (String r : raw) {
            if (r.trim().length() > 0) {
                list.add(r.trim());
            }
        }
        if (list.isEmpty()) {
            list.add("选项1");
        }
        return list.toArray(new String[0]);
    }
}
