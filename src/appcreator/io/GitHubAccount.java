package appcreator.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 账号：本机保存令牌，登录时用 api.github.com/user 校验一次。
 *
 * <pre>
 * 令牌文件：%USERPROFILE%/.appcreator/github.txt    （Linux / macOS 是 ~/.appcreator/github.txt）
 * 内容：第一行账号名，第二行令牌
 * </pre>
 *
 * 只用 JDK 自带 HttpURLConnection，没有第三方依赖。令牌只存在本机，不上传任何地方。
 */
public final class GitHubAccount {

    private static final String API = "https://api.github.com/user";
    private static final Pattern LOGIN = Pattern.compile("\"login\"\\s*:\\s*\"([^\"]+)\"");

    static {
        // Windows 上用系统证书库：JDK 自带 cacerts 遇到企业代理的证书会握手失败
        if (System.getProperty("javax.net.ssl.trustStoreType") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }

    /** 校验结果 */
    public static final class Result {
        public final boolean ok;
        public final String login;
        public final String message;

        Result(boolean ok, String login, String message) {
            this.ok = ok;
            this.login = login;
            this.message = message;
        }
    }

    private GitHubAccount() {
    }

    public static File dir() {
        return new File(System.getProperty("user.home"), ".appcreator");
    }

    public static File file() {
        return new File(dir(), "github.txt");
    }

    /** 上次保存的账号名；没有就返回 null */
    public static String savedLogin() {
        String[] pair = load();
        return pair == null ? null : pair[0];
    }

    /** 上次保存的令牌；没有就返回 null */
    public static String savedToken() {
        String[] pair = load();
        return pair == null ? null : pair[1];
    }

    private static String[] load() {
        File f = file();
        if (!f.isFile()) {
            return null;
        }
        try {
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[512];
            int n;
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
            }
            in.close();
            String text = new String(buf.toByteArray(), "UTF-8").trim();
            int p = text.indexOf('\n');
            if (p <= 0) {
                return null;
            }
            String login = text.substring(0, p).trim();
            String token = text.substring(p + 1).trim();
            if (login.length() == 0 || token.length() == 0) {
                return null;
            }
            return new String[]{login, token};
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 记住账号（token 明文存在用户目录下，仅本机可见） */
    public static boolean save(String login, String token) {
        if (login == null || token == null) {
            return false;
        }
        try {
            File d = dir();
            if (!d.exists()) {
                d.mkdirs();
            }
            File tmp = new File(d, "github.txt.part");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write((login + "\n" + token + "\n").getBytes("UTF-8"));
            out.close();
            File f = file();
            if (f.exists() && !f.delete()) {
                return false;
            }
            return tmp.renameTo(f);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void clear() {
        File f = file();
        if (f.exists()) {
            f.delete();
        }
    }

    /** 用令牌查一次 api.github.com/user：成功返回账号名 */
    public static Result verify(String token) {
        if (token == null || token.trim().length() == 0) {
            return new Result(false, null, "请输入令牌");
        }
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(API).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(6000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Authorization", "Bearer " + token.trim());
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            c.setRequestProperty("User-Agent", "appcreator-login");
            int code = c.getResponseCode();
            if (code == 401 || code == 403) {
                return new Result(false, null, "令牌无效或已过期，请重新生成");
            }
            if (code != 200) {
                return new Result(false, null, "GitHub 返回 " + code + "，稍后再试");
            }
            InputStream in = new BufferedInputStream(c.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[2048];
            int n;
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
            }
            in.close();
            String body = new String(buf.toByteArray(), "UTF-8");
            Matcher m = LOGIN.matcher(body);
            if (!m.find()) {
                return new Result(false, null, "没读到账号名，请重试");
            }
            return new Result(true, m.group(1), "登录成功");
        } catch (Exception e) {
            return new Result(false, null, "连不上 GitHub，检查网络后重试");
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }
}
