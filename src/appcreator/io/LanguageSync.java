package appcreator.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启动时自动下载语言包：从项目的 Language 文件夹（GitHub）拉到用户目录的 Language 里。
 *
 * <pre>
 * 下载位置：%USERPROFILE%/Language    （Linux / macOS 是 ~/Language）
 * </pre>
 *
 * 只用 JDK 自带的 HttpURLConnection，没有第三方依赖；网络不通就保留本地已有的包，
 * 不会卡住启动（整体限定在 {@link #TOTAL_BUDGET_MS} 内）。
 */
public final class LanguageSync {

    private static final String REPO = "SuperDogStar-cpu/EXE-Maker";
    private static final String BRANCH = "main";
    private static final String API = "https://api.github.com/repos/" + REPO + "/contents/Language?ref=" + BRANCH;
    private static final String RAW = "https://raw.githubusercontent.com/" + REPO + "/" + BRANCH + "/Language/";

    /** 接口拿不到清单时的兜底文件名 */
    private static final String[] FALLBACK = {
            "English.japa", "繁體中文.japa", "日本語.japa", "한국어.japa", "Deutsch.japa", "Français.japa"
    };

    private static final long TOTAL_BUDGET_MS = 8000;

    static {
        // Windows 上用系统证书库：JDK 自带 cacerts 遇到企业代理的证书会握手失败
        if (System.getProperty("javax.net.ssl.trustStoreType") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }

    public interface Listener {
        void step(String text);
    }

    private LanguageSync() {
    }

    /** 语言包目录（用户目录下的 Language，Lang.discover 会扫到） */
    public static File dir() {
        return new File(System.getProperty("user.home"), "Language");
    }

    /**
     * 拉取全部语言包，返回本地可用的 .japa 数量。
     */
    public static int sync(Listener listener) {
        File dir = dir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long deadline = System.currentTimeMillis() + TOTAL_BUDGET_MS;
        List<String[]> packs = remoteList(deadline);
        if (packs.isEmpty()) {
            packs = fallbackList();
        }
        int done = 0;
        int total = packs.size();
        for (String[] pack : packs) {
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            String name = pack[0];
            String url = pack[1];
            if (listener != null) {
                listener.step("正在下载语言包…（" + (done + 1) + "/" + total + "）" + name);
            }
            boolean ok = download(url, new File(dir, name), deadline);
            if (ok) {
                done++;
            }
        }
        return localCount(dir);
    }

    /** 轻量 JSON 解析：从 GitHub contents 接口取 name + download_url */
    private static List<String[]> remoteList(long deadline) {
        List<String[]> out = new ArrayList<String[]>();
        try {
            String json = read(API, Math.min(3000L, Math.max(800L, deadline - System.currentTimeMillis())));
            if (json == null) {
                return out;
            }
            Pattern item = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+\\.japa)\"");
            Matcher m = item.matcher(json);
            while (m.find()) {
                // 只用文件名自己拼 raw 地址：接口给的 download_url 有时是未编码的原名，中文文件名会打不开
                String name = m.group(1);
                out.add(new String[]{name, RAW + encode(name)});
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static List<String[]> fallbackList() {
        List<String[]> out = new ArrayList<String[]>();
        for (String name : FALLBACK) {
            out.add(new String[]{name, RAW + encode(name)});
        }
        return out;
    }

    private static int localCount(File dir) {
        String[] names = dir.list();
        int n = 0;
        if (names != null) {
            for (String s : names) {
                if (s.toLowerCase().endsWith(".japa")) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean download(String url, File target, long deadline) {
        File tmp = new File(target.getParentFile(), target.getName() + ".part");
        InputStream in = null;
        OutputStream out = null;
        try {
            long budget = Math.min(4000L, deadline - System.currentTimeMillis());
            if (budget <= 0) {
                return false;
            }
            String body = read(url, budget);
            if (body == null || body.indexOf('=') < 0) {
                if (System.getProperty("appcreator.debug") != null) {
                    System.out.println("DBG read fail " + url + " body=" + (body == null ? "null" : body.length()));
                }
                return false;
            }
            out = new FileOutputStream(tmp);
            out.write(body.getBytes("UTF-8"));
            out.close();
            out = null;
            if (target.exists() && !target.delete()) {
                tmp.delete();
                return false;
            }
            boolean renamed = tmp.renameTo(target);
            if (!renamed && System.getProperty("appcreator.debug") != null) {
                System.out.println("DBG rename fail " + tmp + " -> " + target);
            }
            return renamed;
        } catch (Exception ignored) {
            if (System.getProperty("appcreator.debug") != null) {
                ignored.printStackTrace();
            }
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            if (tmp.exists() && !target.exists()) {
                tmp.delete();
            }
        }
    }

    private static String read(String url, long timeoutMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout((int) Math.min(2500L, Math.max(600L, timeoutMs)));
        c.setReadTimeout((int) Math.min(4000L, Math.max(800L, timeoutMs)));
        c.setRequestProperty("User-Agent", "appcreator-language-sync");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (c.getResponseCode() != 200) {
                return null;
            }
            InputStream in = new BufferedInputStream(c.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
            }
            in.close();
            return new String(buf.toByteArray(), "UTF-8");
        } finally {
            c.disconnect();
        }
    }

    private static String encode(String name) {
        try {
            String e = java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20");
            // URLEncoder 会把路径分隔符之外的东西都转好，这里不需要整段解码
            return e;
        } catch (Exception ex) {
            return name;
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** 本地已有的语言包（用于界面提示） */
    public static Map<String, File> local() {
        Map<String, File> out = new LinkedHashMap<String, File>();
        File dir = dir();
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".japa")) {
                out.put(Lang.displayName(f), f);
            }
        }
        return out;
    }
}
