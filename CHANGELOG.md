# 更新日志

## v1.1 — 2026-11-21

在 v1.0 的深色逻辑编辑器上加代码补全，并把语法检查的行号收拢到真实出错的那一行。

### 新增功能

- **代码自动补全**：输入到第二个字符自动弹出候选框，支持 Java 关键字（`private`、`return`…）、常用 Swing / Java API（`setText()`、`System.out.println()`、`JOptionPane.showMessageDialog()`、`Thread.sleep()`…）以及画布里组件的变量名（组件名优先排在最前）。
- **键盘操作**：`↑↓` 切换候选、`Enter / Tab` 插入、`Esc` 关闭；`Ctrl+空格` 在任意位置手动唤出。
- **插入细节**：插入带括号的条目时光标自动落在括号内（如 `setText()` → 光标停在括号中间），不带括号的条目光标停在末尾。
- **不打扰**：注释（`//`、`/* */`）和字符串内部不自动弹框，手动 `Ctrl+空格` 仍可用。

### 改进

- 语法检查的错误行号会收拢到逻辑代码的最后一行：像 `btnSave.set` 这种写到一半的语句，编译器原本会报到生成的收尾代码上（显示「第 6 行」「第 9 行」），现在统一指向真正出错的那一行（第 3 行），左侧红点和波浪线也跟着对齐。
- 补全候选框配色跟随 VS Code Dark+：深底、选中项 `#094771`、等宽字体，和编辑器融为一体。

### 截图

- `preview_complete.png` — 输入 `set` 时的补全候选框
- `preview_logic_dark.png` — 含补全提示和错误波浪线的编辑器

## v1.0 — 2026-11-21

首个正式版本。用 Java/Swing 手写、零第三方依赖的 Windows 桌面「应用创作器」：左侧组件库拖拽到画布上搭界面，右侧改属性，中间实时预览真实窗口，一键导出 EXE。

### 新增功能

**搭建界面**



* 设计画布（DesignCanvas）：从组件库拖拽组件到窗口模拟区，支持选中、拖动移动、八向拖拽调整大小、Delete 删除、方向键微调。

* 9 类控件：标签、按钮、文本框、文本域、复选框、单选框、下拉框、进度条、滑块。

* 模拟窗口（WindowMockPanel）：标题栏、圆角窗体、窗口尺寸预览，标题栏同步显示自定义图标。

**属性与项目**



* 属性面板（PropertiesPane）：改文本、变量名、坐标尺寸、字体、前景 / 背景色（带色块与 `#RRGGBB` 色值行）、是否启用、进度条 / 滑块取值区间。

* 项目文件 `.apcj`：自定义二进制格式，保存窗口标题、尺寸、颜色、组件列表、逻辑代码与图标，可反复打开继续改。

* 自定义窗口图标：属性面板选图（PNG/JPG/ICO 任意尺寸，自动按最长边缩放到 256px），模拟窗口、预览窗口、导出的 EXE 三处生效；清除按钮可撤销。

**写逻辑**



* 深色逻辑编辑器（LogicEditor + LogicWindow）：VS Code Dark+ 配色、语法高亮、智能行号、错误波浪线、自动保存、按钮 / 事件代码片段插入。

* 内置三份中文教程：`控件逻辑代码.md`、`Java基础语法.md`、`Java延迟与内置库.md`。

**预览与生成**



* 一键预览：内存编译 + 启动真实窗口，不落地 class 文件；预览窗口关闭不影响主程序。

* 代码生成（CodeGenerator）：把画布还原成一份可直接阅读、可继续手改的 `AppMain.java`（纯 JDK，带布局、事件与图标读取）。

* 导出 EXE（Exporter）：调用 JDK 自带 `jpackage` 打出免安装 Windows 程序，图标打进 jar 并转成 `.ico` 传给打包器。

* 程序本身只有约 130 KB 的 `应用创作器.jar`，`run.bat` 双击即可运行，不需要安装任何库。

**界面**



* 薄荷绿主题（Theme）：白底卡片、淡灰绿背景、扁平圆角按钮（RoundButton / RoundPanel / RoundBorder），全中文界面。

### 已修复



* **自定义图标导致「常量字符串过长」**：早期把图标 PNG 转 Base64 写成源码常量，图标稍大就超出 Java 单个字符串 65535 字节上限，编译直接失败。改为资源文件方案 —— 生成的源码只写一句 `getResourceAsStream("appicon.png")`，预览时把图标写进临时编译目录，导出时打进 jar，源码里不再有图标数据。

* **关闭预览窗口时主程序跟着退出**：预览进程用系统属性 `appcreator.preview` 区分关闭行为，预览窗口 `DISPOSE_ON_CLOSE`，主程序 `EXIT_ON_CLOSE`。

* **按钮、单选、复选框文字外出现虚线焦点框**：设计态组件统一 `setFocusable(false)`、`setRequestFocusEnabled(false)`，`AbstractButton` 关闭 `setFocusPainted`；生成的代码对 `JButton`/`JCheckBox`/`JRadioButton` 同样追加 `.setFocusPainted(false)`。

* **色值控件像被划掉**：深色色块上直接写白色等宽 `#22312E`，`#`、数字与字母的中横笔画连成一条线，看着像删除线。改为白底卡片 + 左侧小色块 + 右侧深色色值文字，透明 / 默认时色块画斜线。

### 已知限制



* 导出 EXE 依赖 JDK 自带的 `jpackage`，只在 Windows 上验证过；macOS/Linux 会走同一套逻辑但未实测。

* 只生成 Java 单文件源码时（不走导出），源码本身不含图标数据，图标仅在预览与导出的 EXE 中生效。

* `.apcj` 为 v1.0 自有格式，向后兼容通过文件末尾追加字段实现，暂不支持回退到更早版本打开。

### 文件说明



| 文件                | 说明                       |
| ----------------- | ------------------------ |
| `应用创作器.jar`       | 程序本体，双击或 `run.bat` 启动    |
| `run.bat`         | 用当前 JDK 的 `javaw` 启动 jar |
| `src/appcreator/` | 全部源码，21 个 `.java`，无第三方依赖 |
| `out/`            | 编译产物（`.class`）           |
| `README.md`       | 功能与用法说明                  |
| `CHANGELOG.md`    | 本文件                      |
| 三份 `*.md`         | 逻辑编辑器内置的中文教程             |
| `preview_*.png`   | 界面截图                     |

### 环境



* 编译目标：Java 8（`--release 8`），运行需要 JDK 8 及以上。

* 导出 EXE 需要 JDK 14+（含 `jpackage`），已在 JDK 21（Zulu 21.0.12）验证。

* 仓库地址：[https://github.com/SuperDogStar-cpu/EXE-Maker](https://github.com/SuperDogStar-cpu/EXE-Maker)