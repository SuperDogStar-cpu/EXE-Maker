package appcreator.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 订阅（授权）状态：首次启动自动送 30 天试用，试用到期后必须输入订阅码续订一个月。
 *
 * 记录写在 %USERPROFILE%\.appcreator\license.txt，只在本机，不上传：
 * <pre>
 * start=2026-11-28 22:40
 * end=2026-12-28 22:40
 * paid=0          用过订阅码就记 1
 * code=23K1-KWSL-SPIO
 * </pre>
 * 时间用毫秒时间戳存，格式化只是为了好看。
 */
public final class License {

    /** 有效订阅码：输入时不区分大小写，连不连横线都行 */
    private static final String CODE = "23K1KWSLSPIO";

    /** 试用天数 */
    private static final int TRIAL_DAYS = 30;

    /** 续订一次给的天数 */
    private static final int RENEW_DAYS = 30;

    private static final long DAY = 24L * 60 * 60 * 1000;

    private static final Map<String, String> DATA = new LinkedHashMap<String, String>();
    private static boolean loaded;

    static {
        // 走 Windows 证书库，绕开企业代理的根证书
        System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
    }

    private License() {
    }

    public static File dir() {
        String home = System.getProperty("user.home");
        if (home == null || home.length() == 0) {
            home = ".";
        }
        return new File(home, ".appcreator");
    }

    private static File file() {
        return new File(dir(), "license.txt");
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        DATA.clear();
        File f = file();
        if (!f.isFile()) {
            return;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                    Charset.forName("UTF-8")));
            String line;
            while ((line = in.readLine()) != null) {
                int p = line.indexOf('=');
                if (p > 0) {
                    DATA.put(line.substring(0, p).trim(), line.substring(p + 1).trim());
                }
            }
        } catch (Exception ignored) {
        } finally {
            close(in);
        }
    }

    private static void save() {
        dir().mkdirs();
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(file()), Charset.forName("UTF-8"));
            for (Map.Entry<String, String> e : DATA.entrySet()) {
                out.write(e.getKey() + "=" + e.getValue() + "\n");
            }
            out.flush();
        } catch (Exception ignored) {
        } finally {
            close(out);
        }
    }

    private static void close(java.io.Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception ignored) {
        }
    }

    /** 丢掉内存里的缓存，下次读文件（改过 license.txt 后让它生效） */
    public static synchronized void reload() {
        loaded = false;
        DATA.clear();
    }

    /**
     * 开机调用：没有记录就开一个 30 天试用；记录坏了（结束时间缺失）也重开一次。
     */
    public static synchronized void ensure() {
        load();
        long end = number("end");
        if (number("start") == 0 || end == 0) {
            long now = System.currentTimeMillis();
            DATA.put("start", Long.toString(now));
            DATA.put("end", Long.toString(now + TRIAL_DAYS * DAY));
            DATA.putIfAbsent("paid", "0");
            save();
        }
    }

    private static long number(String key) {
        try {
            String v = DATA.get(key);
            return v == null ? 0L : Long.parseLong(v.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 现在还能不能用 */
    public static synchronized boolean active() {
        ensure();
        return number("end") > System.currentTimeMillis();
    }

    /** 剩下的完整天数（不足一天按一天算，过期返回 0） */
    public static synchronized int daysLeft() {
        ensure();
        long left = number("end") - System.currentTimeMillis();
        if (left <= 0) {
            return 0;
        }
        return (int) Math.ceil(left / (double) DAY);
    }

    /** 用没用过订阅码（用来区分「试用」和「已订阅」两种说法） */
    public static synchronized boolean paid() {
        load();
        return "1".equals(DATA.get("paid"));
    }

    /** 结束日期，形如 2026-12-28 */
    public static synchronized String endDate() {
        ensure();
        long end = number("end");
        if (end <= 0) {
            return "";
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return f.format(new Date(end));
    }

    /** 一行状态文案，给界面直接显示 */
    public static synchronized String statusText() {
        ensure();
        int left = daysLeft();
        if (left <= 0) {
            return "试用 / 订阅已到期（" + endDate() + "）";
        }
        if (paid()) {
            return "已订阅，剩余 " + left + " 天（到 " + endDate() + "）";
        }
        return "免费试用剩余 " + left + " 天（到 " + endDate() + "）";
    }

    /** 界面判断：到期后按钮该叫「输入订阅码」 */
    public static synchronized boolean trialOnly() {
        load();
        return !"1".equals(DATA.get("paid"));
    }

    /**
     * 输入订阅码续订一个月（从当前到期日往后加，已经过期就从现在算）。
     *
     * @return 成功返回 "订阅已续期，剩余 X 天（到 yyyy-MM-dd）"，失败返回原因，界面直接显示
     */
    public static synchronized String redeem(String input) {
        if (input == null || input.trim().length() == 0) {
            return "请输入订阅码";
        }
        String norm = input.replace("-", "").replace(" ", "").replace("　", "").trim().toUpperCase(Locale.ROOT);
        if (!CODE.equals(norm)) {
            return "订阅码不正确，请检查后重试";
        }
        ensure();
        long now = System.currentTimeMillis();
        long end = Math.max(number("end"), now) + RENEW_DAYS * DAY;
        if (number("start") == 0) {
            DATA.put("start", Long.toString(now));
        }
        DATA.put("end", Long.toString(end));
        DATA.put("paid", "1");
        DATA.put("code", pretty(norm));
        DATA.put("redeemed", Long.toString(now));
        save();
        return "订阅已续期，剩余 " + daysLeft() + " 天（到 " + endDate() + "）";
    }

    /** 存成 23K1-KWSL-SPIO 的样子，方便回看 */
    private static String pretty(String norm) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < norm.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(norm.charAt(i));
        }
        return sb.toString();
    }

    /** 订阅码的正确写法，给界面做提示 / 测试用 */
    public static String sample() {
        return "23K1-KWSL-SPIO";
    }
}
