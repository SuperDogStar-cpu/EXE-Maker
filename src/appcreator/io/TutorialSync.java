package appcreator.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * 启动时自动下载教程：从仓库根目录把 4 份 .md 拉到用户目录的 .appcreator/tutorial。
 *
 * <pre>
 * 下载位置：%USERPROFILE%/.appcreator/tutorial    （Linux / macOS 是 ~/.appcreator/tutorial）
 * </pre>
 *
 * 只用 JDK 自带 HttpURLConnection，没有第三方依赖；网络不通就继续用 jar 里内置的那份，
 * 不会卡住启动（整体限定在 {@link #TOTAL_BUDGET_MS} 内）。
 */
public final class TutorialSync {

    private static final String REPO = "SuperDogStar-cpu/EXE-Maker";
    private static final String BRANCH = "main";
    private static final String RAW = "https://raw.githubusercontent.com/" + REPO + "/" + BRANCH + "/";
    /** 兜底源：jsDelivr 的 GitHub 加速地址，raw 握手失败时用 */
    private static final String CDN = "https://cdn.jsdelivr.net/gh/" + REPO + "@" + BRANCH + "/";

    /** 教程文件名（和仓库根目录、jar 里的资源同名） */
    public static final String[] FILES = {
            "快速上手.md", "控件逻辑代码.md", "对话框使用.md", "Java基础语法.md", "Java延迟与内置库.md"
    };

    private static final long TOTAL_BUDGET_MS = 12000;

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

    private TutorialSync() {
    }

    /** 教程缓存目录 */
    public static File dir() {
        return new File(new File(System.getProperty("user.home"), ".appcreator"), "tutorial");
    }

    /** 拉取全部教程，返回本地可用的 .md 数量 */
    public static int sync(Listener listener) {
        File dir = dir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long deadline = System.currentTimeMillis() + TOTAL_BUDGET_MS;
        // 缺的先补：每个文件 raw 试两次、jsDelivr 试一次，握手偶发失败也能补上
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < FILES.length; i++) {
                if (System.currentTimeMillis() > deadline) {
                    break;
                }
                String name = FILES[i];
                File target = new File(dir, name);
                if (target.isFile() && pass == 0) {
                    continue;
                }
                if (pass == 0 && listener != null) {
                    listener.step("正在下载教程…（" + (i + 1) + "/" + FILES.length + "）" + name);
                }
                fetch(name, target, deadline, pass == 0);
            }
        }
        // 已有的再刷新一遍内容（远程改过教程就能拿到新版；失败就保留旧文件）
        for (int i = 0; i < FILES.length; i++) {
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            fetch(FILES[i], new File(dir, FILES[i]), deadline, false);
        }
        return Math.max(localCount(dir), 0);
    }

    /** 下载单个教程：raw 为主，jsDelivr 兜底；verbose 时打调试日志 */
    private static boolean fetch(String name, File target, long deadline, boolean verbose) {
        String url1 = RAW + encode(name);
        String url2 = CDN + encode(name);
        String body = tryRead(url1, Math.min(4000L, deadline - System.currentTimeMillis()));
        if (body == null) {
            body = tryRead(url1, Math.min(4000L, deadline - System.currentTimeMillis()));
        }
        if (body == null) {
            body = tryRead(url2, Math.min(4000L, deadline - System.currentTimeMillis()));
        }
        if (body == null) {
            if (verbose && System.getProperty("appcreator.debug") != null) {
                System.out.println("DBG tutorial fetch fail " + url1);
            }
            return false;
        }
        File tmp = new File(target.getParentFile(), target.getName() + ".part");
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            out.write(body.getBytes("UTF-8"));
            out.close();
            if (target.exists() && !target.delete()) {
                tmp.delete();
                return false;
            }
            if (!tmp.renameTo(target)) {
                tmp.delete();
                return false;
            }
            return true;
        } catch (Exception e) {
            if (System.getProperty("appcreator.debug") != null) {
                e.printStackTrace();
            }
            tmp.delete();
            return false;
        }
    }

    private static String tryRead(String url, long budget) {
        if (budget <= 300) {
            return null;
        }
        try {
            String body = read(url, budget);
            return body != null && body.trim().length() > 10 ? body : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 本地缓存的教程文件；没有就返回 null */
    public static File local(String name) {
        File f = new File(dir(), name);
        return f.isFile() ? f : null;
    }

    private static int localCount(File dir) {
        String[] names = dir.list();
        int n = 0;
        if (names != null) {
            for (String s : names) {
                if (s.toLowerCase().endsWith(".md")) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean download(String url, File target, long deadline) {
        File tmp = new File(target.getParentFile(), target.getName() + ".part");
        OutputStream out = null;
        try {
            long budget = Math.min(4000L, deadline - System.currentTimeMillis());
            if (budget <= 0) {
                return false;
            }
            String body = read(url, budget);
            if (body == null || body.trim().length() < 10) {
                if (System.getProperty("appcreator.debug") != null) {
                    System.out.println("DBG tutorial fetch fail " + url);
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
                System.out.println("DBG tutorial rename fail " + tmp + " -> " + target);
            }
            return renamed;
        } catch (Exception ignored) {
            if (System.getProperty("appcreator.debug") != null) {
                ignored.printStackTrace();
            }
            return false;
        } finally {
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
        c.setRequestProperty("User-Agent", "appcreator-tutorial-sync");
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
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
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
}
