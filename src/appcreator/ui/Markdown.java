package appcreator.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极简 Markdown 转 HTML：给教程窗口用（JDK 自带 HTML 渲染，不引第三方库）。
 *
 * 支持：标题（# ~ ######、以及 === / --- 下划线式）、段落、**粗体**、*斜体*、~~删除线~~、
 * `行内代码`、围栏代码块（```java）、链接 [文字](地址)、自动链接 &lt;https://…&gt;、图片（降级成链接文字）、
 * 无序 / 有序列表（含两级以上嵌套）、任务列表 - [ ] / - [x]、引用 &gt;、分隔线 --- *** ___、表格 | a | b |。
 */
public final class Markdown {

    private static final Pattern H = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*+]\\s+(.+)$");
    private static final Pattern ORDERED = Pattern.compile("^(\\s*)(\\d{1,3})[.)]\\s+(.+)$");
    private static final Pattern HR = Pattern.compile("^\\s*((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})\\s*$");
    private static final Pattern SETEXT = Pattern.compile("^(=+|-+)$");

    private Markdown() {
    }

    /** 转成完整 HTML 文档 */
    public static String toHtml(String md, int fontSize, String family) {
        String body = blocks(normalize(md), fontSize);
        return "<html><head><style>"
                + "body{font-family:\"" + family + "\";font-size:" + fontSize + "pt;color:#22312E;margin:6 12 10 12;}"
                + "a{color:#1E7A63;text-decoration:underline;}"
                + "</style></head><body>" + body
                + "</body></html>";
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replace("\r\n", "\n").replace('\r', '\n');
    }

    // ---------------- 块级 ----------------

    private static String blocks(String text, int fs) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = lines.length;
        while (i < n) {
            String line = lines[i];
            String t = line.trim();
            if (t.length() == 0) {
                i++;
                continue;
            }
            // 围栏代码块
            if (t.startsWith("```") || t.startsWith("~~~")) {
                String fence = t.substring(0, 3);
                String lang = t.substring(3).trim();
                StringBuilder code = new StringBuilder();
                i++;
                while (i < n && !lines[i].trim().startsWith(fence)) {
                    code.append(lines[i]).append('\n');
                    i++;
                }
                i++;
                out.append(codeBlock(code.toString(), lang, fs));
                continue;
            }
            // 标题
            Matcher h = H.matcher(t);
            if (h.matches()) {
                int level = h.group(1).length();
                String head = h.group(2).trim().replaceAll("\\s+#+\\s*$", "");
                out.append(heading(level, inline(head, fs), fs));
                i++;
                continue;
            }
            // 分隔线
            if (HR.matcher(t).matches()) {
                out.append("<hr>");
                i++;
                continue;
            }
            // 表格：本行有 |，下一行是分隔行
            if (t.indexOf('|') >= 0 && i + 1 < n && isTableSeparator(lines[i + 1])) {
                int[] end = new int[]{i};
                out.append(table(lines, i, end, fs));
                i = end[0];
                continue;
            }
            // 引用
            if (t.startsWith(">")) {
                StringBuilder inner = new StringBuilder();
                while (i < n && lines[i].trim().startsWith(">")) {
                    String s = lines[i].trim();
                    s = s.substring(1);
                    if (s.startsWith(" ")) {
                        s = s.substring(1);
                    }
                    inner.append(s).append('\n');
                    i++;
                }
                out.append(quote(blocks(inner.toString(), fs), fs));
                continue;
            }
            // 列表
            if (BULLET.matcher(line).matches() || ORDERED.matcher(line).matches()) {
                int[] end = new int[]{i};
                out.append(list(lines, i, end, fs));
                i = end[0];
                continue;
            }
            // 下划线式标题（当前一行 + 下一行 === 或 ---）
            if (i + 1 < n && SETEXT.matcher(lines[i + 1].trim()).matches()) {
                int level = lines[i + 1].trim().startsWith("=") ? 1 : 2;
                out.append(heading(level, inline(t, fs), fs));
                i += 2;
                continue;
            }
            // 普通段落：吃掉连续的非空行，中文之间不额外加空格
            StringBuilder para = new StringBuilder(t);
            boolean hardBreak = line.endsWith("  ") || line.endsWith("\\");
            i++;
            while (i < n) {
                String nx = lines[i];
                String nt = nx.trim();
                if (nt.length() == 0 || isBlockStart(nt)) {
                    break;
                }
                boolean cjk = joinCjk(para, nt);
                if (!cjk) {
                    para.append(' ');
                }
                para.append(nt);
                hardBreak = nx.endsWith("  ") || nx.endsWith("\\");
                i++;
            }
            out.append("<p>").append(inline(para.toString(), fs));
            if (hardBreak) {
                out.append("<br>");
            }
            out.append("</p>");
        }
        return out.toString();
    }

    private static boolean joinCjk(StringBuilder sb, String next) {
        if (sb.length() == 0 || next.length() == 0) {
            return false;
        }
        char a = sb.charAt(sb.length() - 1);
        char b = next.charAt(0);
        return isCjk(a) && isCjk(b);
    }

    private static boolean isCjk(char c) {
        return (c >= 0x2E80 && c <= 0x9FFF) || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFFEF) || (c >= 0x3000 && c <= 0x303F);
    }

    private static boolean isBlockStart(String t) {
        return t.startsWith("```") || t.startsWith("~~~") || H.matcher(t).matches()
                || HR.matcher(t).matches() || t.startsWith(">")
                || BULLET.matcher(t).matches() || ORDERED.matcher(t).matches()
                || SETEXT.matcher(t).matches();
    }

    private static String heading(int level, String inner, int fs) {
        // Swing 对 CSS font-size 支持不稳，用 <font size> 相对字号更可靠
        int size = level == 1 ? 6 : level == 2 ? 5 : level == 3 ? 4 : 3;
        String color = level <= 2 ? "#18876F" : "#3A4A46";
        return "<div style=\"margin-top:" + (level <= 2 ? 14 : 8) + ";\"><font size='" + size
                + "' color='" + color + "'><b>" + inner + "</b></font></div>";
    }

    private static String codeBlock(String code, String lang, int fs) {
        int mono = Math.max(8, fs - 1);
        StringBuilder sb = new StringBuilder();
        sb.append("<table width='100%' cellspacing='0' cellpadding='0' bgcolor='#F1F7F5'>")
                .append("<tr><td width='4' bgcolor='#8FCFBF'></td><td>")
                .append("<table width='100%' cellspacing='0' cellpadding='8' bgcolor='#F1F7F5'><tr><td>");
        if (lang != null && lang.length() > 0 && !"java".equalsIgnoreCase(lang)) {
            sb.append("<div style=\"font-size:").append(Math.max(8, fs - 2))
                    .append("pt;color:#7C9A93;margin-bottom:2;\">").append(escape(lang)).append("</div>");
        }
        sb.append("<pre style=\"font-family:Consolas;font-size:").append(mono)
                .append("pt;color:#1F3D3A;margin:0;\">").append(escape(wrapCode(code))).append("</pre>");
        sb.append("</td></tr></table></td></tr></table>");
        return sb.toString();
    }

    /** 代码块长行软换行：Swing 的 pre 不折行，太长会出现横向滚动条，这里按 96 字符断开 */
    private static String wrapCode(String code) {
        StringBuilder sb = new StringBuilder(code.length() + 64);
        for (String line : code.split("\n", -1)) {
            String rest = line;
            String indent = "    ";
            while (rest.length() > 96) {
                int cut = -1;
                for (int i = Math.min(95, rest.length() - 1); i >= 48; i--) {
                    char c = rest.charAt(i);
                    if (c == ' ' || c == ',' || c == ';' || c == '+' || c == '&') {
                        cut = i + 1;
                        break;
                    }
                }
                if (cut <= 0) {
                    cut = 96;
                }
                sb.append(rest, 0, cut).append('\n').append(indent);
                rest = rest.substring(cut);
            }
            sb.append(rest).append('\n');
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String quote(String inner, int fs) {
        return "<table width='100%' cellspacing='0' cellpadding='0' bgcolor='#F6FBFA'>"
                + "<tr><td width='4' bgcolor='#9BCBBD'></td><td>"
                + "<table cellspacing='0' cellpadding='6' bgcolor='#F6FBFA'><tr><td>" + inner
                + "</td></tr></table></td></tr></table>";
    }

    private static boolean isTableSeparator(String line) {
        String t = line.trim();
        if (t.indexOf('-') < 0 || t.indexOf('|') < 0) {
            return false;
        }
        return t.replace('|', ' ').replace(':', ' ').replace('-', ' ').trim().length() == 0;
    }

    private static String table(String[] lines, int start, int[] end, int fs) {
        List<String> head = cells(lines[start]);
        String sep = lines[start + 1].trim();
        List<String> aligns = new ArrayList<String>();
        for (String c : cells(lines[start + 1])) {
            String cell = c.trim();
            boolean left = cell.startsWith(":");
            boolean right = cell.endsWith(":");
            aligns.add(left && right ? "center" : right ? "right" : left ? "left" : "left");
        }
        while (aligns.size() < head.size()) {
            aligns.add("left");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1' cellspacing='0' cellpadding='6' bgcolor='#FFFFFF'>");
        sb.append("<tr bgcolor='#E8F4F1'>");
        for (int i = 0; i < head.size(); i++) {
            sb.append("<th align='").append(aligns.get(i)).append("'>")
                    .append(inline(head.get(i).trim(), fs)).append("</th>");
        }
        sb.append("</tr>");
        int i = start + 2;
        while (i < lines.length) {
            String t = lines[i].trim();
            if (t.length() == 0 || t.indexOf('|') < 0 || isTableSeparator(lines[i])) {
                break;
            }
            List<String> row = cells(lines[i]);
            sb.append("<tr>");
            for (int c = 0; c < head.size(); c++) {
                String v = c < row.size() ? row.get(c).trim() : "";
                sb.append("<td align='").append(aligns.get(c)).append("'>")
                        .append(inline(v, fs)).append("</td>");
            }
            sb.append("</tr>");
            i++;
        }
        sb.append("</table>");
        end[0] = i;
        return sb.toString();
    }

    private static List<String> cells(String line) {
        String t = line.trim();
        if (t.startsWith("|")) {
            t = t.substring(1);
        }
        if (t.endsWith("|")) {
            t = t.substring(0, t.length() - 1);
        }
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\\' && i + 1 < t.length() && t.charAt(i + 1) == '|') {
                cur.append('|');
                i++;
            } else if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // ---------------- 列表 ----------------

    private static class Level {
        int indent;
        boolean ordered;
        int start;
        int attach;
        Level parent;
        List<String> items = new ArrayList<String>();
        List<Level> children = new ArrayList<Level>();
    }

    /** 新建一层列表：parent 为 null 时放到根上 */
    private static Level create(List<Level> roots, int indent, boolean ordered, int number, Level parent) {
        Level l = new Level();
        l.indent = indent;
        l.ordered = ordered;
        l.start = ordered && number > 0 ? number : 1;
        l.parent = parent;
        if (parent == null) {
            roots.add(l);
        } else {
            l.attach = parent.items.size() - 1;
            parent.children.add(l);
        }
        return l;
    }

    private static String list(String[] lines, int start, int[] end, int fs) {
        List<Level> roots = new ArrayList<Level>();
        List<Level> stack = new ArrayList<Level>();
        int i = start;
        while (i < lines.length) {
            String line = lines[i];
            Matcher b = BULLET.matcher(line);
            Matcher o = ORDERED.matcher(line);
            if (!b.matches() && !o.matches()) {
                break;
            }
            int indent = (b.matches() ? b.group(1) : o.group(1)).length();
            boolean ordered = o.matches();
            String content = b.matches() ? b.group(2) : o.group(3);
            int number = o.matches() ? Integer.parseInt(o.group(2)) : 0;

            while (stack.size() > 1 && indent < stack.get(stack.size() - 1).indent) {
                stack.remove(stack.size() - 1);
            }
            Level top = stack.isEmpty() ? null : stack.get(stack.size() - 1);
            Level level;
            if (top == null) {
                level = create(roots, indent, ordered, number, null);
                stack.add(level);
            } else if (indent > top.indent) {
                level = create(roots, indent, ordered, number, top);
                stack.add(level);
            } else if (ordered == top.ordered) {
                level = top;
            } else {
                // 同一层级换了列表类型：开一个平级的兄弟列表
                Level parent = top.parent;
                stack.remove(stack.size() - 1);
                level = create(roots, indent, ordered, number, parent);
                stack.add(level);
            }
            level.items.add(inline(task(content), fs));
            i++;
        }
        end[0] = i;
        StringBuilder sb = new StringBuilder();
        sb.append("<table cellspacing='0' cellpadding='3'>");
        for (Level r : roots) {
            appendLevel(sb, r);
        }
        sb.append("</table>");
        return sb.toString();
    }

    /** 任务列表前缀：- [ ] / - [x] → ☐ / ☑ */
    private static String task(String content) {
        String t = content.trim();
        String lower = t.toLowerCase();
        if (lower.startsWith("[ ]")) {
            return "\u2610 " + content.substring(content.indexOf("[ ]") + 3).trim();
        }
        if (lower.startsWith("[x]")) {
            return "\u2611 " + content.substring(content.indexOf("[x]") + 3).trim();
        }
        return content;
    }

    private static void appendLevel(StringBuilder sb, Level level) {
        appendLevel(sb, level, 0);
    }

    /** 列表用窄表格画：行距紧凑、缩进用不换行空格，Swing 的 ul/li 默认间距太大 */
    private static void appendLevel(StringBuilder sb, Level level, int depth) {
        int counter = level.start;
        for (int i = 0; i < level.items.size(); i++) {
            sb.append("<tr>");
            String mark = level.ordered ? (counter + ".") : "\u00B7";
            StringBuilder pad = new StringBuilder();
            for (int d = 0; d < depth; d++) {
                pad.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            }
            sb.append("<td width='18' valign='top' nowrap><font color='#3E7A6C'>").append(pad).append(mark)
                    .append("</font></td>");
            sb.append("<td valign='top'>").append(level.items.get(i)).append("</td></tr>");
            if (level.ordered) {
                counter++;
            }
            for (Level child : level.children) {
                if (child.attach == i) {
                    appendLevel(sb, child, depth + 1);
                }
            }
        }
    }

    // ---------------- 行内 ----------------

    private static final Pattern CODE_SPAN = Pattern.compile("(`+)(.+?)\\1", Pattern.DOTALL);
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern AUTO = Pattern.compile("&lt;(https?://[^&\\s]+)&gt;");
    private static final Pattern STRIKE = Pattern.compile("~~(.+?)~~");
    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern ITALIC = Pattern.compile("(?<![*\\w])(\\*|_)([^*_<>]+?)\\1(?![*\\w])");

    private static String inline(String text, int fs) {
        if (text == null || text.length() == 0) {
            return "";
        }
        String s = escape(text);
        // 行内代码先抽出来，避免里面的星号被当成格式
        List<String> codes = new ArrayList<String>();
        Matcher cm = CODE_SPAN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (cm.find()) {
            codes.add(cm.group(2));
            cm.appendReplacement(sb, Matcher.quoteReplacement("\u0001C" + (codes.size() - 1) + "\u0001"));
        }
        cm.appendTail(sb);
        s = sb.toString();

        s = IMAGE.matcher(s).replaceAll("<a href='$2'>[图片] $1</a>");
        s = LINK.matcher(s).replaceAll("<a href='$2'>$1</a>");
        s = AUTO.matcher(s).replaceAll("<a href='$1'>$1</a>");
        s = STRIKE.matcher(s).replaceAll("<strike>$1</strike>");
        s = BOLD.matcher(s).replaceAll("<b>$2</b>");
        s = ITALIC.matcher(s).replaceAll("<i>$2</i>");
        s = s.replace("&lt;br&gt;", "<br>");

        // 还原行内代码
        Pattern ph = Pattern.compile("\u0001C(\\d+)\u0001");
        Matcher pm = ph.matcher(s);
        StringBuffer sb2 = new StringBuffer();
        while (pm.find()) {
            int idx = Integer.parseInt(pm.group(1));
            pm.appendReplacement(sb2, Matcher.quoteReplacement(
                    "<code style=\"font-family:Consolas;font-size:" + Math.max(8, fs - 1)
                            + "pt;color:#176B57;\">" + codes.get(idx) + "</code>"));
        }
        pm.appendTail(sb2);
        return sb2.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
