package appcreator.model;

import java.awt.Color;

/**
 * 画布上的一个组件（纯数据模型）。
 */
public class Widget {

    /** 组件类型，取自 ComponentFactory.TYPES */
    public String type = "按钮";

    /** Java 变量名 */
    public String name = "button1";

    public int x = 20;
    public int y = 20;
    public int w = 100;
    public int h = 32;

    public String text = "";
    public int fontSize = 14;
    public boolean bold = false;
    public Color fg = new Color(0x22312E);
    /** null 表示使用控件默认背景 */
    public Color bg = null;

    /** 下拉框的选项，用逗号分隔 */
    public String items = "选项1, 选项2, 选项3";

    public Widget() {
    }

    public Widget(String type, String name, int x, int y, int w, int h, String text) {
        this.type = type;
        this.name = name;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.text = text;
    }

    @Override
    public String toString() {
        return name;
    }
}
