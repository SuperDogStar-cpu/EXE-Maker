package appcreator.io;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 韩文字体：本机没有能显示谚文的字体时，自动下载一份 NanumGothic 并注册给 JVM。
 *
 * <pre>
 * 存放位置：%USERPROFILE%/.appcreator/fonts/NanumGothic-Regular.ttf
 *          （Linux / macOS 是 ~/.appcreator/fonts/）
 * </pre>
 *
 * NanumGothic 是 OFL 开源字体（Naver 出品，Google Fonts 分发），只用 JDK 自带能力下载和注册，
 * 没有第三方依赖；下载失败就继续用系统已有字体，不会卡住界面。
 */
public final class KoreanFont {

    /** 注册成功后 JVM 里可用的字体家族名 */
    public static final String FAMILY = "NanumGothic";

    private static final String[] MIRRORS = {
            "https://raw.githubusercontent.com/google/fonts/main/ofl/nanumgothic/NanumGothic-Regular.ttf",
            "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/nanumgothic/NanumGothic-Regular.ttf",
            "https://gitee.com/mirrors_google-fonts/fonts/raw/main/ofl/nanumgothic/NanumGothic-Regular.ttf"
    };

    private static final String FILE_NAME = "NanumGothic-Regular.ttf";

    private static volatile String family;
    private static volatile boolean tried;
    private static volatile boolean downloading;
    private static final Object LOCK = new Object();

    static {
        // Windows 上用系统证书库，避免 JDK 自带 cacerts 对代理证书握手失败
        if (System.getProperty("javax.net.ssl.trustStoreType") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }

    private KoreanFont() {
    }

    /** 字体文件放在用户目录下的 .appcreator/fonts */
    public static File dir() {
        return new File(new File(System.getProperty("user.home"), ".appcreator"), "fonts");
    }

    public static File file() {
        return new File(dir(), FILE_NAME);
    }

    /** 已经注册好的字体家族名，没有就返回 null */
    public static String family() {
        return family;
    }

    /** 正在后台下载 */
    public static boolean downloading() {
        return downloading;
    }

    /**
     * 确保韩文字体可用：本地有文件就直接注册，没有就下载。
     * 会阻塞调用线程，所以启动画面里用；界面里请用 {@link #ensureAsync(Runnable)}。
     */
    public static boolean ensure() {
        synchronized (LOCK) {
            if (family != null) {
                return true;
            }
            File f = file();
            if (f.isFile() && register(f)) {
                tried = true;
                return true;
            }
            tried = true;
            for (String url : MIRRORS) {
                if (download(url, f) && register(f)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 后台下载 + 注册，完成后在 EDT 上回调 */
    public static void ensureAsync(final Runnable after) {
        synchronized (LOCK) {
            if (family != null) {
                runLater(after);
                return;
            }
            if (downloading) {
                return;
            }
            downloading = true;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ensure();
                } finally {
                    synchronized (LOCK) {
                        downloading = false;
                    }
                }
                runLater(after);
            }
        }, "korean-font");
        t.setDaemon(true);
        t.start();
    }

    private static void runLater(Runnable r) {
        if (r == null) {
            return;
        }
        javax.swing.SwingUtilities.invokeLater(r);
    }

    /** 把下载到（或已存在的）字体文件注册进 JVM，之后 new Font("NanumGothic", …) 就能用 */
    private static boolean register(File f) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            String name = font.getFamily();
            if (name == null || name.length() == 0) {
                name = font.getName();
            }
            family = name;
            if (System.getProperty("appcreator.debug") != null) {
                System.out.println("DBG korean font registered: " + name + " <- " + f);
            }
            return true;
        } catch (Exception e) {
            if (System.getProperty("appcreator.debug") != null) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private static boolean download(String url, File target) {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tmp = new File(parent, target.getName() + ".part");
        InputStream in = null;
        OutputStream out = null;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(4000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "appcreator-font-sync");
            try {
                if (c.getResponseCode() != 200) {
                    return false;
                }
                in = new BufferedInputStream(c.getInputStream());
                out = new FileOutputStream(tmp);
                byte[] b = new byte[8192];
                int n;
                long total = 0;
                long deadline = System.currentTimeMillis() + 180000L;
                while ((n = in.read(b)) > 0) {
                    out.write(b, 0, n);
                    total += n;
                    if (System.currentTimeMillis() > deadline) {
                        // 网速太慢就先放弃，下次启动接着试
                        out.close();
                        out = null;
                        tmp.delete();
                        return false;
                    }
                }
                out.close();
                out = null;
                in.close();
                in = null;
                if (total < 200000) {
                    // 太小肯定不是字体文件
                    tmp.delete();
                    return false;
                }
            } finally {
                c.disconnect();
            }
            if (target.exists() && !target.delete()) {
                tmp.delete();
                return false;
            }
            return tmp.renameTo(target);
        } catch (Exception e) {
            if (System.getProperty("appcreator.debug") != null) {
                e.printStackTrace();
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

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
