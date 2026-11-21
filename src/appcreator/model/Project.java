package appcreator.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个完整的应用项目：窗口信息 + 组件列表 + 逻辑代码。
 */
public class Project {

    public static final String DEFAULT_LOGIC =
            "// 在这里编写 Java 逻辑，可直接使用组件变量名（如 button1、label1）。\n"
            + "// 这段代码会插入到窗口组件创建之后、窗口显示之前执行。\n"
            + "//\n"
            + "// 示例：点击按钮弹出提示\n"
            + "// button1.addActionListener(new ActionListener() {\n"
            + "//     public void actionPerformed(ActionEvent e) {\n"
            + "//         JOptionPane.showMessageDialog(null, \"你好，世界！\");\n"
            + "//     }\n"
            + "// });\n";

    public String title = "我的应用";
    public int width = 900;
    public int height = 620;
    public Color bg = new Color(0xFFFFFF);
    public String logic = DEFAULT_LOGIC;

    /** 自定义窗口图标（PNG 字节，最长边不超过 256）；为 null 表示用默认图标 */
    public byte[] iconPng;
    /** 图标文件名，仅用于界面显示 */
    public String iconName = "";

    public final List<Widget> widgets = new ArrayList<Widget>();

    public void reset() {
        title = "我的应用";
        width = 900;
        height = 620;
        bg = new Color(0xFFFFFF);
        logic = DEFAULT_LOGIC;
        iconPng = null;
        iconName = "";
        widgets.clear();
    }
}
