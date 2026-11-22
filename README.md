# 应用创作器（Java 版）

一个用 Java/Swing 写的 Windows 桌面「应用创作器」：**拖拽搭界面，Java 写代码，一键预览真实窗口、导出 EXE**。界面走清爽风格（薄荷绿 + 白 + 淡灰绿背景、圆角卡片、扁平按钮），不依赖任何第三方库。

## 运行

双击 `run.bat` 即可（自动编译 + 启动）。

也可以直接双击 `应用创作器.jar`（已打包好，需 JDK/JRE 8 以上）。

若系统 PATH 没有 JDK，请打开 `run.bat`，把 `set "JDK=..."` 改成你自己的 JDK 目录（例如 `C:\Program Files\Java\jdk-21`）。

## 功能

| 区域      | 能做什么                                                               |
| ------- | ------------------------------------------------------------------ |
| 左侧「组件库」 | 12 类组件，分三组：**基础**（标签、按钮、文本框、文本域、复选框、单选框、下拉框、进度条、滑块）、**图文**（图片、视频）、**链接**（超链接）；启动时还会自动下载 Visual Studio .NET（WinForms）工具箱清单，追加**容器 / 菜单 / 数据 / 输入 / 对话框 / 组件** 6 组共 44 个组件（Panel、GroupBox、选项卡、表格 DataGrid、树形 TreeView、菜单栏、状态栏、月历、计时器等）。**拖到画布放置**，或双击直接添加到画布中央；每个分类标题左边的小三角可以点一下**收起 / 展开**该分类 |
| 中间画布    | 点击选中、拖动移动、8 个手柄拖拽缩放（最小不限，最大不超过窗口尺寸）；顶部显示组件变量名；Delete 删除；虚线网格 + 白纸页效果               |
| 右侧「属性」  | 窗口标题 / 尺寸 / 背景色 / **窗口图标**；组件变量名、文本、位置、尺寸、字号、加粗、前景色、背景色、下拉项；图片 / 视频的**文件路径**、超链接的**网址**，全部实时生效 |
| 右侧「代码」  | 点一下即弹出**独立的深色代码编辑器**，写 Java 代码可引用画布里的组件变量名（如 `button1`），也能直接改整套生成源码 |
| 顶部工具栏   | 新建、打开、保存、导出 Java、导出 EXE、删除组件、模拟窗口、运行预览、**教程**（软件内读教程）、**设置**（界面语言、Java 选择） |
| 底部状态栏   | 操作提示 + 当前画布尺寸                                                      |

### 深色代码编辑器（VS Code 风格）

点右侧「代码」标签，会弹出独立窗口 `CodeWindow`，里面是跟着 VS Code Dark+ 配色走的代码编辑器，分两个标签页：

- **「逻辑」**：只写逻辑片段，创作器自动把它套进生成的骨架里（引用 `button1`、`label1` 这些画布变量名即可）。
- **「完整源码（可改）」**：展示生成的完整 `AppMain.java`，可以直接手改——**添加 / 删除 / 移动组件、改属性、改布局**都行。改过之后「运行预览」和「导出 EXE」都按这份代码跑（顶部提示会变成「已手改…」），点右上「重新生成」即可丢弃手改、回到由设计器生成。

两个页面共用的编辑能力：

- **语法高亮**：关键字蓝 `#569CD6`、`if/for/while` 等控制流紫 `#C586C0`、字符串橙 `#CE9178`、数字浅绿 `#B5CEA8`、类型青 `#4EC9B0`、注释绿斜体 `#6A9955`、注解黄 `#DCDCAA`。
- **智能行号**：当前行整行铺灰底 + 行号加粗高亮；出错的行左侧一个红点、行号变红；行号宽度随位数自动加宽，不会挤在一起。
- **自动查错**：停止输入约 0.6 秒后用 JDK 内置 `JavaCompiler` 在后台编译，错误处画红色波浪线，底部列出「第 N 行 · 原因」，点一下就跳到那一行；行号会自动换算回你在编辑器里看到的行号。
- **自动补全**：打字到第二个字符就弹出候选框——Java 关键字、常用 Swing/Java 方法（`setText()`、`System.out.println()`、`Thread.sleep()` 等）、以及画布里已有的组件变量名（如 `btnSave`），输入 `btn` 直接候选 `btnSave`；`Ctrl+空格` 手动唤出，`↑↓` 选择、`Enter / Tab` 插入、`Esc` 关闭；插入 `xxx()` 时光标自动停在括号里；在注释和字符串里不会自动打扰。
- **括号自动配对**：输入 `(` `[` `{` `<` 自动补全右括号、光标停在中间；右括号后面已经是同一个字符时直接跳过；比较运算的 `<`（如 `a < b`）不误补，注释和字符串内不打扰。
- **自动保存**：编辑过程中每 15 秒自动保存（有项目文件就写项目文件，没有就存到 `用户目录\应用创作器-自动保存.apcj`），窗口底部显示最后一次保存时间；`Ctrl+S` 立即保存，关闭窗口时也会再保存一次。
- **编辑体验**：等宽字体、Tab 插入 4 空格、回车自动缩进、`Ctrl+/` 注释/取消注释、`Ctrl+Z / Ctrl+Y` 撤销重做、`Ctrl+A` 全选，垂直滚动条 + 当前行列坐标。
- **窗口操作**：Esc 关闭、`Ctrl+S` 保存、`Ctrl+Enter` 运行预览；「立即检查 / 运行预览 / 保存并关闭」三个按钮。

由于一个 Swing 组件只能有一个父容器，编辑器整体搬进了独立窗口，主界面「代码」页只留一张功能介绍卡（含「打开编辑器」按钮）。

### 图片 / 视频 / 超链接

- **图片**：属性面板「文件」行点「选择」挑一张图，画布上按控件框大小**等比缩放并居中**显示（不变形，比例不符时留白）；拖动缩放手柄、改属性里的宽高时，图片会立刻跟着按新尺寸重新缩放。导出代码里的窗口也是同一算法；「清除」恢复「图片（未选择）」。图片会一起存进 `.apcj` 项目文件（存路径），预览和导出的窗口按同一路径加载。
- **视频**：选好视频后画布显示「视频 · 文件名」占位，左侧薄荷绿播放徽标，徽标直径按控件短边的三分之一缩放——控件放大，徽标同步变大。纯 JDK 无法内嵌播放，所以设计态和真实窗口里都是**双击**用系统默认播放器打开视频。
- **超链接**：属性面板「网址」行填地址（`www.example.com` 这种不写协议的会自动补 `https://`）。画布/窗口里是蓝色下划线文字，**双击**用系统浏览器打开网址，单击只是选中组件，不冲突。

### 内置教程（工具栏「教程」）

工具栏「设置」左边有个 **「教程」** 按钮，点开就是一个四标签的教程窗口，内容全部**打进 jar 里**（资源路径 `appcreator/tutorial/*.md`），不联网也能看：

| 标签        | 内容                                                        |
| --------- | --------------------------------------------------------- |
| 快速上手      | 界面一览、组件库 9 分类与收起展开、44 个 .NET 组件清单、摆放与改尺寸（无最小限制）、写逻辑、运行导出、设置、`.apcj` 项目文件 |
| 逻辑代码      | 每个组件可直接粘贴的逻辑片段（按钮、标签、文本框、下拉框、图片、视频、表格、树、列表、选项卡、面板、菜单栏、状态栏、月历、计时器、滚动条联动等） |
| Java 基础    | 语法、变量、判断循环、方法、类与对象                                       |
| 延迟与内置库    | `Thread.sleep`、`javax.swing.Timer`、`SwingUtilities.invokeLater` 等延迟与常用内置库用法 |

教程窗口是可关的独立窗口（不挡主界面），白底、等宽字体显示代码块，`**粗体**` 与 `` `行内代码` `` 会被渲染成样式。程序目录里放了同名的 `.md` 时会优先用你改过的那份，方便自己补内容。

### 设置与语言包（.japa）

工具栏「设置」可以改界面语言，默认是简体中文。想换成别的语言，就放一个 **`.japa` 语言包** 进去：

- **格式**：UTF-8 纯文本，一行一条「原文=译文」，`#` 开头是注释，`#name=English` 用来指定语言在设置里显示的名字；文件名随便取（程序认的是 `.japa` 后缀，不会改你的文件名）。
- **放哪里**：程序目录、用户目录、当前项目目录，以及它们下面的 `Language` 文件夹里的语言包都会被自动发现，列在设置的下拉框里；也可以在设置里点「导入 .japa」直接挑文件。
- **自带 6 国语言**：项目里的 `Language` 文件夹已经放好 6 个语言包（English、繁體中文、日本語、한국어、Deutsch、Français），简体中文是内置默认，不需要语言包。每个包都覆盖工具栏、组件库、属性面板、代码编辑器标签、设置按钮和状态栏提示。
- **字形自动匹配**：汉字走黑体，韩文走 Malgun Gothic，日文走游明朝体，其他语言依次回退 Microsoft YaHei / Segoe UI，切语言后不会再出现整屏方框；德语、法语这种长文字会把工具栏按钮自动缩小，属性面板标签超长显示省略号、悬停看全文。
- **怎么生效**：点确定后，界面上所有能对上的内置文字（按钮、标签、属性名、组件库、代码编辑器里的标签等）立刻换成译文，字体也会跟着重新挑（英文 Consolas、汉字黑体）；没写到的文字保持原样。
- 切回「简体中文（内置）」即可恢复原样；语言选择会记在用户目录的设置文件里，下次启动自动加载。

### 设置 → Java 选择

工具栏「设置」的第二行就是 **Java**：

- **自动识别**：启动时会扫描本机的 Java —— 当前运行环境、`JAVA_HOME`、`PATH`，以及 `Program Files` / Eclipse Adoptium / AdoptOpenJDK / Zulu / Microsoft / BellSoft / Corretto / Semeru / ojdkbuild、`%LOCALAPPDATA%\Programs`、`%LOCALAPPDATA%\Java`、`~/tools`、`~/.jdks`、`~/scoop/apps`（Linux / macOS 还有 `/usr/lib/jvm`、`/opt` 等）。每个候选都会**真的跑一次 `java -version`** 验证，跑不通的不进列表。
- **下拉框**：默认「自动（跟随当前运行环境）」；其它项显示成 `Java 21.0.12（JDK · 当前运行）` 这种「版本（JDK / JRE · 是否当前运行）」，鼠标悬停能看到完整安装路径。点「重新检测」会重新扫描（检测在后台线程跑，界面不卡）。
- **影响范围**：「导出 EXE」用的 `jpackage`、「运行预览」的外部编译回退用的 `javac` / `java`，都优先用你选中的那个 JDK；选的是纯 JRE（没有 `javac` / `jpackage`）时会自动退回当前运行的 JDK，缺工具时弹提示引导你去设置里改。
- **启动顺序**：打开创作器会先出现一个小窗口，依次显示「正在适配本机字体… → 正在下载语言包…（x/6） → 正在下载组件数据… → 正在检测本机 Java 版本并验证是否可用…」，四步都跑完才进主界面，所以进来之后语言、组件库和「设置 → Java」里都是现成的，不用等。
- **自动下载组件数据**：启动时从 GitHub 仓库的 `Data/components.jcmp` 拉取 Visual Studio .NET（WinForms）工具箱组件清单，存到 `%USERPROFILE%\.appcreator\data\components.jcmp`（macOS / Linux 是 `~/.appcreator/data/`），下载失败或没网就用程序内置那份。清单每行一个组件：`分类 ⇥ 名称 ⇥ 类型 ⇥ 默认宽 ⇥ 默认高 ⇥ 默认文字 ⇥ 图标字`，改完重新上传即可扩充组件库。
- **自动下载语言包**：启动时会从 GitHub 仓库的 `Language/` 目录把 6 个 `.japa` 拉到用户目录下的 `Language`（Windows `%USERPROFILE%\Language`，macOS / Linux `~/Language`），下次离线也能直接用；网络不通或某个包失败只会跳过，不影响进主界面。
- 选择记在用户目录的 `应用创作器.java.properties`（键 `home`），下次启动直接复用。

### 字体

界面统一：**英文和字母一律 Consolas，汉字一律黑体**。代码编辑器里逐字符混排——代码用 Consolas，中文注释、中文字符串自动用黑体，字号、颜色、加粗斜体都保持原样。

**缺字形自动换字体**：每段文字先找一款能显示全部字形的字体（黑体 → 微软雅黑 → 游黑体 → Meiryo UI → MS Gothic → Malgun Gothic → 宋体 → Nirmala UI → Leelawadee UI → Segoe UI Emoji / Symbol → Segoe UI → Arial Unicode MS → Dialog）；万一某款字体显示不了其中某个字（罕见汉字、日文假名、谚文、阿拉伯文、泰文、天城文、希腊文、表情符号等），就按单个字符再换一次字体来画，不会留下空白方框。组件库、画布标签、模拟窗口标题、属性面板色值这些自绘文字都走同一套逐字兜底。

**韩文字体自动下载**：切到韩文界面时，如果本机没有一款能显示谚文的字体（Windows 一般自带 Malgun Gothic，有这个就直接用），程序会在后台下载一份开源字体 NanumGothic（OFL 许可，Google Fonts 分发），存到 `%USERPROFILE%\.appcreator\fonts\NanumGothic-Regular.ttf`（macOS / Linux 是 `~/.appcreator/fonts/`），注册给 JVM 后自动用于韩文，已有窗口会立刻重刷，不用重启。下载在后台线程跑，下载慢或失败都不会拖住启动和界面；下次启动直接读本地文件。

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

`Ctrl+N` 新建 · `Ctrl+O` 打开 · `Ctrl+S` 保存 · `Ctrl+R` 运行预览 · `Delete` 删除选中组件 · 点右侧「代码」打开深色编辑器

## 项目文件

- 作品保存为 `.apcj`（自定义轻量二进制格式，纯 Java 实现，无第三方依赖），包含窗口信息、自定义图标、全部组件（含图片 / 视频路径、超链接网址）、逻辑代码和手改过的完整源码；图标按 PNG 字节内嵌，换电脑打开也不丢。旧版项目文件仍可直接打开。
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
│  │                        PropertiesPane / WindowMockPanel / Theme / Fonts /
│  │                        RoundButton / RoundPanel / RoundBorder / SettingsDialog /
│  │                        CodeEditor（深色编辑器）/ CodeWindow（独立窗口，两个标签页）
│  ├─ gen/                  代码生成 CodeGenerator、即时编译运行 Runner、导出 EXE 的 Exporter、图标 IconUtil
│  └─ io/                   项目文件读写 ProjectIO、界面语言 Lang（.japa 语言包）
├─ preview_ui.png           界面截图（编辑画布）
├─ preview_mock.png         界面截图（模拟窗口）
├─ preview_v12.png          界面截图（组件库三组分类 + 超链接属性）
├─ preview_code.png         界面截图（代码编辑器「完整源码（可改）」页）
├─ preview_settings.png     界面截图（设置 · 语言选择）
├─ preview_settings_ui.png  界面截图（English.japa 生效后的主界面）
├─ Language/                6 国语言包（English.japa、日本語.japa、한국어.japa …）
├─ Data/components.jcmp     .NET（WinForms）工具箱组件清单（启动时自动下载最新一份）
├─ preview_extras_ui.png    界面截图（组件库新增的 6 个分类）
├─ preview_extras.png       界面截图（表格、树、选项卡、月历等新组件在画布上的样子）
├─ preview_logic_dark.png   界面截图（深色代码编辑器，含错误波浪线）
├─ preview_complete.png     界面截图（代码补全候选框）
├─ preview_splash.png       界面截图（启动时的 Java 检测窗口）
├─ preview_splash_sync.png 界面截图（启动时自动下载语言包）
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
