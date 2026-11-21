# 应用创作器（Java 版）

一个用 Java/Swing 写的 Windows 桌面「应用创作器」：**拖拽搭界面，Java 写逻辑，一键预览真实窗口**。界面走清爽风格（薄荷绿 + 白 + 淡灰绿背景、圆角卡片、扁平按钮），不依赖任何第三方库。

## 运行

双击 `run.bat` 即可（自动编译 + 启动）。

也可以直接双击 `应用创作器.jar`（已打包好，需 JDK/JRE 8 以上）。

若系统 PATH 没有 JDK，请打开 `run.bat`，把 `set "JDK=..."` 改成你自己的 JDK 目录（例如 `C:\Program Files\Java\jdk-21`）。

## 功能

| 区域      | 能做什么                                                               |
| ------- | ------------------------------------------------------------------ |
| 左侧「组件库」 | 9 类组件：标签、按钮、文本框、文本域、复选框、单选框、下拉框、进度条、滑块。**拖到画布放置**，或双击直接添加到画布中央     |
| 中间画布    | 点击选中、拖动移动、8 个手柄拖拽缩放；顶部显示组件变量名；Delete 删除；虚线网格 + 白纸页效果               |
| 右侧「属性」  | 窗口标题 / 尺寸 / 背景色 / **窗口图标**；组件变量名、文本、位置、尺寸、字号、加粗、前景色、背景色、下拉项，全部实时生效 |
| 右侧「逻辑」  | 点一下即弹出**独立的深色代码编辑器**，写 Java 代码可引用画布里的组件变量名（如 `button1`）            |
| 顶部工具栏   | 新建、打开、保存、导出 Java、导出 EXE、删除组件、模拟窗口、运行预览                             |
| 底部状态栏   | 操作提示 + 当前画布尺寸                                                      |

### 深色逻辑编辑器（VS Code 风格）

点右侧「逻辑」标签，会弹出独立窗口 `LogicWindow`，里面是跟着 VS Code Dark+ 配色走的代码编辑器：

- **语法高亮**：关键字蓝 `#569CD6`、`if/for/while` 等控制流紫 `#C586C0`、字符串橙 `#CE9178`、数字浅绿 `#B5CEA8`、类型青 `#4EC9B0`、注释绿斜体 `#6A9955`、注解黄 `#DCDCAA`。
- **智能行号**：当前行整行铺灰底 + 行号加粗高亮；出错的行左侧一个红点、行号变红；行号宽度随位数自动加宽，不会挤在一起。
- **自动查错**：停止输入约 0.6 秒后用 JDK 内置 `JavaCompiler` 在后台编译，错误处画红色波浪线，底部列出「第 N 行 · 原因」，点一下就跳到那一行；行号会自动换算回你在编辑器里看到的行号。
- **自动保存**：编辑过程中每 15 秒自动保存（有项目文件就写项目文件，没有就存到 `用户目录\应用创作器-自动保存.apcj`），窗口底部显示最后一次保存时间；`Ctrl+S` 立即保存，关闭窗口时也会再保存一次。
- **编辑体验**：等宽字体、Tab 插入 4 空格、回车自动缩进、`Ctrl+/` 注释/取消注释、`Ctrl+Z / Ctrl+Y` 撤销重做、`Ctrl+A` 全选，垂直滚动条 + 当前行列坐标。
- **窗口操作**：Esc 关闭、`Ctrl+S` 保存、`Ctrl+Enter` 运行预览；「立即检查 / 运行预览 / 保存并关闭」三个按钮。

由于一个 Swing 组件只能有一个父容器，编辑器整体搬进了独立窗口，主界面「逻辑」页只留一张功能介绍卡（含「打开编辑器」按钮）。

### 自定义窗口图标

右侧「属性」→ 窗口 → 「图标」这一行：点「选择」挑一张图片（PNG / JPG / GIF / BMP 都行），点「清除」恢复默认。选好后：

- 左侧**模拟窗口的标题栏**立刻显示新图标，所见即所得；
- 「运行预览」弹出的真实窗口用这个图标：图标作为资源文件 `appicon.png` 放在临时编译目录，生成的 `AppMain` 用 `getResourceAsStream("appicon.png")` 读取，**不写进源码常量**（所以再大的图标也不会报「常量字符串过长」）；
- 「导出 EXE」时自动把图标转成 `.ico` 交给 `jpackage --icon`，**资源管理器里看到的 exe 图标也是自定义的**，同时 `appicon.png` 一并打进 jar，程序窗口也带图标；
- 「导出 Java」只产出单个 `.java` 文件，外部没有 `appicon.png`，所以那种情况下窗口用的是 Java 默认图标；
- 图标按最长边缩放到 256px 后存进项目文件（`.apcj`），所以原图再大也不会把项目撑大。

### 模拟窗口

点工具栏的「模拟窗口」可以给画布套一层**真实窗口外观**：标题栏、应用图标、尺寸文字（如 `900 × 620`）、最小化/最大化/关闭按钮、投影和浅色桌面底板，底部还有一行说明。这样不用运行预览，也能直观看出窗口最终的大小比例和居中位置。再点一次即可关闭，回到纯画布编辑。

### 关闭预览不再闪退

生成的 `AppMain` 会区分运行方式：预览时（创作器会设置 `appcreator.preview`）使用 `JFrame.DISPOSE_ON_CLOSE`，关闭预览窗口只释放这个窗口，不会 `System.exit`，创作器继续正常工作；导出成独立应用后不带该标记，使用 `EXIT_ON_CLOSE`，关掉窗口进程就正常结束。

### 一键导出 EXE

工具栏「导出 EXE」→ 选择一个输出文件夹 → 自动完成：生成源码 → 编译 → 打 `AppMain.jar` → 用 JDK 自带的 `jpackage` 生成 Windows 可执行程序。

- 产物是 **app-image 目录**：`<输出目录>/<应用标题>/`，里面有 `<应用标题>.exe` 和内嵌的 Java 运行时，**整个文件夹一起分发**即可，目标机器无需安装 Java。
- 因为带运行时，体积约 40–70 MB，首次导出需 10–40 秒。
- 需要完整 JDK（含 `jpackage`，JDK 14 以上）；只有 JRE 时会提示缺少工具。
- 若输出目录已存在同名文件夹，会自动追加 `_2`、`_3`。

### 运行预览是怎么实现的

点「运行预览」时，创作器会把当前设计**生成一份 `AppMain.java`**，用 JDK 内置的 `JavaCompiler` 在内存/Temp 目录中即时编译，再加载并执行 —— 于是你会看到一个真实弹出的窗口，行为和最终导出的程序完全一致。逻辑写错时，会弹出编译错误（含行号），还能一键查看生成的完整源码。

### 快捷键

`Ctrl+N` 新建 · `Ctrl+O` 打开 · `Ctrl+S` 保存 · `Ctrl+R` 运行预览 · `Delete` 删除选中组件 · 点右侧「逻辑」打开深色编辑器

## 项目文件

- 作品保存为 `.apcj`（自定义轻量二进制格式，纯 Java 实现，无第三方依赖），包含窗口信息、自定义图标、全部组件和逻辑代码。图标按 PNG 字节内嵌，换电脑打开也不丢。
- 「导出 Java」会输出可直接 `javac AppMain.java && java AppMain` 运行的独立源码。
- `控件逻辑代码.md` 是各控件的 Java 逻辑速查：点击、取值、监听、定时器、弹窗、窗口关闭等，代码可直接粘到「逻辑」面板。
- `Java基础语法.md` 是基础语法速查：创建变量、修改变量、类型转换、字符串、条件/循环、数组与集合、作用域，同样只含可运行代码块。
- `Java延迟与内置库.md` 是延时相关内置类速查：`Thread.sleep`、`TimeUnit`、`javax.swing.Timer`、`SwingWorker`、`ScheduledExecutorService`、`java.util.Timer`、`CountDownLatch`、`LockSupport`，含防抖、倒计时、加载进度、动画等组合示例。

## 目录结构

```
Java应用创作器/
├─ run.bat                  编译并启动
├─ 应用创作器.jar            已打包的可执行 jar
├─ src/appcreator/
│  ├─ AppCreator.java       启动类、全局外观
│  ├─ model/                数据模型：Project / Widget / ComponentFactory
│  ├─ ui/                   界面：MainFrame / DesignCanvas / PalettePanel /
│  │                        PropertiesPane / WindowMockPanel / Theme /
│  │                        RoundButton / RoundPanel / RoundBorder /
│  │                        LogicEditor（深色编辑器）/ LogicWindow（独立窗口）
│  ├─ gen/                  代码生成 CodeGenerator、即时编译运行 Runner、导出 EXE 的 Exporter、图标 IconUtil
│  └─ io/                   项目文件读写 ProjectIO
├─ preview_ui.png           界面截图（编辑画布）
├─ preview_mock.png         界面截图（模拟窗口）
├─ preview_logic_dark.png   界面截图（深色逻辑编辑器，含错误波浪线）
```

## 生成的代码长什么样

```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AppMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new AppMain().buildUI(); }
        });
    }

    private void buildUI() {
        JFrame frame = new JFrame("我的应用");
        if (Boolean.getBoolean("appcreator.preview")) {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        } else {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        frame.setSize(900, 620);
        JPanel root = new JPanel(null);
        root.setBackground(new Color(0xFFFFFF));
        frame.setContentPane(root);

        JButton button1 = new JButton();
        button1.setText("点我试试");
        button1.setFont(new Font("微软雅黑", Font.BOLD, 14));
        button1.setBounds(60, 100, 120, 38);
        root.add(button1);

        // ----- 逻辑 -----
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "你好，世界！");
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
```

## 说明

- 纯 JDK 实现，没有引入任何 jar 依赖。
- 画布上的组件就是真实 Swing 控件，所见即所得；设计态由一层透明玻璃面板接管鼠标，避免误触按钮/输入框。
- 需要 JDK（含 `javac`）才能「运行预览」；只有 JRE 时会尝试调用系统 `javac`。
