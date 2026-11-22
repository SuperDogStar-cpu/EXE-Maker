package appcreator.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visual Studio .NET（WinForms）工具箱组件数据。
 *
 * <p>数据文件是纯文本，一行一个组件，字段用制表符分开：
 * {@code 分类 名称 类型(内部映射) 默认宽 默认高 默认文字 图标字}。
 * 启动时自动从仓库下载最新一份到 {@code %USERPROFILE%\.appcreator\data\}，
 * 下载不到就用程序内置的那份，界面照常能显示全部组件。</p>
 */
public final class ComponentData {

    /** 一个工具箱组件 */
    public static final class Entry {
        public String category;
        public String name;
        /** 内部映射类型：label / button / textfield / textarea / checkbox / radio / combo /
         *  list / checkedlist / spinner / progress / slider / scrollbar-h / scrollbar-v /
         *  table / tree / tabs / split / panel / group / toolbar / statusbar / menubar /
         *  calendar / datetime / color / linklabel / nonvisual */
        public String kind;
        public int w;
        public int h;
        public String text;
        public char glyph;

        public String toString() {
            return name;
        }
    }

    /** 下载地址（第一个可用即止） */
    private static final String[] SOURCES = {
            "https://raw.githubusercontent.com/SuperDogStar-cpu/EXE-Maker/main/Data/components.jcmp",
            "https://cdn.jsdelivr.net/gh/SuperDogStar-cpu/EXE-Maker@main/Data/components.jcmp",
            "https://raw.githubusercontent.com/SuperDogStar-cpu/EXE-Maker/master/Data/components.jcmp"
    };

    private static final List<Entry> ENTRIES = new ArrayList<Entry>();
    private static final Map<String, Entry> BY_NAME = new HashMap<String, Entry>();
    private static final List<String> CATEGORIES = new ArrayList<String>();

    static {
        // Windows 上用系统证书库：JDK 自带 cacerts 遇到企业代理的证书会握手失败
        if (System.getProperty("javax.net.ssl.trustStoreType") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }

    private static String loadedFrom = "内置";

    private ComponentData() {
    }

    /** 用户目录下的缓存文件 */
    public static File cacheFile() {
        File dir = new File(System.getProperty("user.home"), ".appcreator" + File.separator + "data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "components.jcmp");
    }

    public static String loadedFrom() {
        return loadedFrom;
    }

    public static synchronized int count() {
        return ENTRIES.size();
    }

    public static synchronized List<String> categories() {
        return new ArrayList<String>(CATEGORIES);
    }

    public static synchronized List<Entry> byCategory(String category) {
        List<Entry> list = new ArrayList<Entry>();
        for (Entry e : ENTRIES) {
            if (e.category.equals(category)) {
                list.add(e);
            }
        }
        return list;
    }

    public static synchronized Entry get(String name) {
        return BY_NAME.get(name);
    }

    /**
     * 启动流程用：先读内置数据保证组件库完整，再尝试下载最新数据（失败静默跳过）。
     */
    public static void sync() {
        load();
        if (download()) {
            load();
        }
    }

    /** 读数据：优先用户缓存，其次程序内置 */
    public static synchronized void load() {
        ENTRIES.clear();
        BY_NAME.clear();
        CATEGORIES.clear();
        File cache = cacheFile();
        if (cache.isFile() && cache.length() > 0) {
            try {
                InputStream in = new java.io.FileInputStream(cache);
                try {
                    if (parse(in) > 0) {
                        loadedFrom = "已下载";
                        return;
                    }
                } finally {
                    in.close();
                }
            } catch (Exception ignored) {
                // 缓存坏了就退回内置数据
            }
        }
        parseBuiltin();
        loadedFrom = "内置";
    }

    private static void parseBuiltin() {
        InputStream in = null;
        try {
            in = ComponentData.class.getResourceAsStream("/appcreator/data/components.jcmp");
            if (in != null) {
                parse(in);
            }
        } catch (Exception ignored) {
            // 没有内置数据也不崩，只是没有额外组件
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
        if (ENTRIES.isEmpty()) {
            // 兜底：内置资源丢失时给最小的一份，至少面板不会空
            add("容器", "Panel 面板", "panel", 220, 140, "", '板');
            add("菜单", "MenuStrip 菜单栏", "menubar", 300, 28, "文件  编辑  视图", '菜');
            add("数据", "DataGridView 表格", "table", 300, 160, "", '表');
        }
    }

    private static int parse(InputStream in) throws java.io.IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        int n = 0;
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            if (f.length < 6) {
                continue;
            }
            String category = f[0].trim();
            String name = f[1].trim();
            String kind = f[2].trim();
            if (category.length() == 0 || name.length() == 0 || kind.length() == 0) {
                continue;
            }
            int w;
            int h;
            try {
                w = Integer.parseInt(f[3].trim());
                h = Integer.parseInt(f[4].trim());
            } catch (Exception e) {
                w = 180;
                h = 100;
            }
            String text = f[5];
            char glyph = f.length > 6 && f[6].trim().length() > 0 ? f[6].trim().charAt(0)
                    : name.charAt(0);
            add(category, name, kind, w, h, text, glyph);
            n++;
        }
        return n;
    }

    private static void add(String category, String name, String kind, int w, int h, String text, char glyph) {
        if (BY_NAME.containsKey(name)) {
            return;
        }
        Entry e = new Entry();
        e.category = category;
        e.name = name;
        e.kind = kind;
        e.w = w;
        e.h = h;
        e.text = text;
        e.glyph = glyph;
        ENTRIES.add(e);
        BY_NAME.put(name, e);
        if (!CATEGORIES.contains(category)) {
            CATEGORIES.add(category);
        }
    }

    /** 下载最新数据到用户目录，成功返回 true（失败不影响启动） */
    public static boolean download() {
        File target = cacheFile();
        for (String src : SOURCES) {
            HttpURLConnection conn = null;
            try {
                String encoded = src;
                URL url = new URL(encoded);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "AppCreator");
                if (conn.getResponseCode() != 200) {
                    continue;
                }
                File part = new File(target.getParentFile(), "components.jcmp.part");
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(part);
                byte[] buf = new byte[8192];
                int len;
                long total = 0;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    total += len;
                    if (total > 512 * 1024) {
                        break;
                    }
                }
                out.flush();
                out.close();
                in.close();
                if (part.length() < 512) {
                    part.delete();
                    continue;
                }
                // 先自检：能解析出组件才算有效，避免把半截文件当数据用
                java.io.FileInputStream check = new java.io.FileInputStream(part);
                int parsed;
                try {
                    parsed = parse(check);
                } finally {
                    check.close();
                }
                if (parsed <= 0) {
                    part.delete();
                    load();
                    continue;
                }
                if (target.exists()) {
                    target.delete();
                }
                if (!part.renameTo(target)) {
                    copy(part, target);
                    part.delete();
                }
                return true;
            } catch (Exception ignored) {
                // 换下一个镜像
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return false;
    }

    private static void copy(File from, File to) {
        try {
            InputStream in = new java.io.FileInputStream(from);
            FileOutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception ignored) {
        }
    }

    /** 供外部（导出/上传）拼地址用，保留百分号编码能力 */
    public static String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return name;
        }
    }
}
