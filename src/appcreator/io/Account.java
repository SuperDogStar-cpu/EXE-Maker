package appcreator.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录账号：GitHub（个人访问令牌）或微软账号（设备码授权）。
 *
 * <pre>
 * 账号文件：%USERPROFILE%/.appcreator/account.txt    （Linux / macOS 是 ~/.appcreator/account.txt）
 * 头像文件：%USERPROFILE%/.appcreator/avatar.png
 * </pre>
 *
 * 全部用 JDK 自带 HttpURLConnection，没有第三方依赖；密码之类的东西一律不存，
 * 令牌 / 账号名 / 头像只留在本机。
 */
public final class Account {

    public static final String GITHUB = "github";
    public static final String MICROSOFT = "ms";

    /** 微软设备码授权用的公共客户端（Azure CLI 的公开 client id，支持个人与企业账号） */
    private static final String MS_CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";
    private static final String MS_DEVICE_CODE = "https://login.microsoftonline.com/common/oauth2/v2.0/devicecode";
    private static final String MS_TOKEN = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String MS_GRAPH_ME = "https://graph.microsoft.com/v1.0/me";
    private static final String MS_GRAPH_PHOTO = "https://graph.microsoft.com/v1.0/me/photo/$value";

    private static final Pattern GH_LOGIN = Pattern.compile("\"login\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GH_AVATAR = Pattern.compile("\"avatar_url\"\\s*:\\s*\"([^\"]+)\"");

    static {
        // Windows 上用系统证书库：JDK 自带 cacerts 遇到企业代理的证书会握手失败
        if (System.getProperty("javax.net.ssl.trustStoreType") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }

    /** 登录结果 */
    public static final class Result {
        public final boolean ok;
        public final String name;
        public final String message;

        Result(boolean ok, String name, String message) {
            this.ok = ok;
            this.name = name;
            this.message = message;
        }
    }

    /** 微软登录过程中把验证码推给界面 */
    public interface Prompt {
        void show(String code, String url, String message);
    }

    private static boolean loaded;
    private static String kind;
    private static String name;
    private static String token;
    private static String mail;

    private Account() {
    }

    // ------------------------------------------------------------ 存取

    public static File dir() {
        return new File(System.getProperty("user.home"), ".appcreator");
    }

    public static File file() {
        return new File(dir(), "account.txt");
    }

    public static File avatarFile() {
        return new File(dir(), "avatar.png");
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        File f = file();
        if (!f.isFile()) {
            return;
        }
        try {
            String[] lines = readText(f).split("\n");
            for (String line : lines) {
                int p = line.indexOf('=');
                if (p <= 0) {
                    continue;
                }
                String k = line.substring(0, p).trim();
                String v = line.substring(p + 1).trim();
                if ("kind".equals(k)) {
                    kind = v;
                } else if ("name".equals(k)) {
                    name = v;
                } else if ("token".equals(k)) {
                    token = v;
                } else if ("mail".equals(k)) {
                    mail = v;
                }
            }
        } catch (Exception ignored) {
            // 文件损坏就当没登录
        }
        if (kind == null || name == null) {
            kind = null;
            name = null;
        }
    }

    public static String kind() {
        load();
        return kind;
    }

    public static String name() {
        load();
        return name;
    }

    public static String token() {
        load();
        return token;
    }

    public static String mail() {
        load();
        return mail;
    }

    public static boolean isLoggedIn() {
        load();
        return kind != null && name != null;
    }

    /** 已登录账号的显示名：GitHub 直接用账号名，微软优先显示邮箱 */
    public static String display() {
        load();
        if (name == null) {
            return "";
        }
        if (MICROSOFT.equals(kind) && mail != null && mail.length() > 0) {
            return name + "（" + mail + "）";
        }
        return name;
    }

    public static String platformName() {
        load();
        return MICROSOFT.equals(kind) ? "微软账号" : "GitHub";
    }

    public static void save(String kind, String name, String token, String mail) {
        loaded = true;
        Account.kind = kind;
        Account.name = name;
        Account.token = token;
        Account.mail = mail;
        try {
            File d = dir();
            if (!d.exists()) {
                d.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("kind=").append(nullToEmpty(kind)).append('\n');
            sb.append("name=").append(nullToEmpty(name)).append('\n');
            if (token != null) {
                sb.append("token=").append(token).append('\n');
            }
            if (mail != null) {
                sb.append("mail=").append(mail).append('\n');
            }
            File tmp = new File(d, "account.txt.part");
            OutputStream out = new FileOutputStream(tmp);
            out.write(sb.toString().getBytes("UTF-8"));
            out.close();
            File f = file();
            if (f.exists() && !f.delete()) {
                return;
            }
            tmp.renameTo(f);
        } catch (Exception ignored) {
            // 存不进去只影响下次自动登录
        }
    }

    public static void clear() {
        loaded = true;
        kind = null;
        name = null;
        token = null;
        mail = null;
        File f = file();
        if (f.exists()) {
            f.delete();
        }
        File a = avatarFile();
        if (a.exists()) {
            a.delete();
        }
    }

    // ------------------------------------------------------------ 头像

    public static boolean hasAvatar() {
        return avatarFile().isFile();
    }

    /** 头像图片；没有就返回 null（界面用首字母兜底） */
    public static BufferedImage avatar() {
        File f = avatarFile();
        if (!f.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void saveAvatar(byte[] png) {
        if (png == null || png.length == 0) {
            return;
        }
        try {
            File d = dir();
            if (!d.exists()) {
                d.mkdirs();
            }
            File tmp = new File(d, "avatar.part");
            OutputStream out = new FileOutputStream(tmp);
            out.write(png);
            out.close();
            File f = avatarFile();
            if (f.exists() && !f.delete()) {
                return;
            }
            tmp.renameTo(f);
        } catch (Exception ignored) {
            // 头像存不下来就显示首字母
        }
    }

    // ------------------------------------------------------------ GitHub

    /** 用 GitHub 令牌校验一次，并把头像抓下来存本地 */
    public static Result loginGitHub(String token) {
        if (token == null || token.trim().length() == 0) {
            return new Result(false, null, "请输入令牌");
        }
        String body;
        try {
            int[] code = new int[1];
            body = request("https://api.github.com/user", token.trim(), null, code);
            if (code[0] == 401 || code[0] == 403) {
                return new Result(false, null, "令牌无效或已过期，请重新生成");
            }
            if (body == null) {
                return new Result(false, null, "GitHub 返回异常，稍后再试");
            }
        } catch (Exception e) {
            return new Result(false, null, "连不上 GitHub，检查网络后重试");
        }
        Matcher m = GH_LOGIN.matcher(body);
        if (!m.find()) {
            return new Result(false, null, "没读到账号名，请重试");
        }
        String login = m.group(1);
        save(GITHUB, login, token.trim(), null);
        Matcher a = GH_AVATAR.matcher(body);
        if (a.find()) {
            saveAvatar(fetchBytes(a.group(1).replace("\\/", "/"), null, null, 20000));
        }
        return new Result(true, login, "登录成功");
    }

    /** 上次 GitHub 令牌是否还有效：通过就返回账号名，失败返回 null */
    public static String autoGitHub() {
        String t = token();
        if (kind() == null || !GITHUB.equals(kind()) || t == null) {
            return null;
        }
        Result r = loginGitHub(t);
        return r.ok ? r.name : null;
    }

    // ------------------------------------------------------------ 微软账号

    /** 设备码授权登录微软账号（阻塞，直到登录成功或超时；验证码通过 prompt 推给界面） */
    public static Result loginMicrosoft(Prompt prompt) {
        try {
            String devBody = post(MS_DEVICE_CODE, null,
                    "client_id=" + MS_CLIENT_ID
                            + "&scope=" + URLEncoder.encode("openid profile User.Read", "UTF-8"));
            if (devBody == null) {
                return new Result(false, null, "微软登录服务暂时连不上，稍后再试");
            }
            String deviceCode = pick(devBody, "device_code", false);
            String userCode = pick(devBody, "user_code", false);
            String uri = pick(devBody, "verification_uri", false);
            String intervalText = pick(devBody, "interval", true);
            String expiresText = pick(devBody, "expires_in", true);
            if (deviceCode == null || userCode == null) {
                return new Result(false, null, "没拿到验证码，稍后再试");
            }
            int interval = 5;
            try {
                interval = Math.max(3, Integer.parseInt(intervalText == null ? "5" : intervalText));
            } catch (Exception ignored) {
            }
            long deadline;
            try {
                deadline = System.currentTimeMillis() + (Long.parseLong(expiresText) * 1000L);
            } catch (Exception e) {
                deadline = System.currentTimeMillis() + 600000L;
            }
            String openUri = uri == null || uri.length() == 0 ? "https://microsoft.com/devicelogin" : uri;
            if (prompt != null) {
                prompt.show(userCode, openUri, "在浏览器打开 " + openUri + " 并输入上面的验证码");
            }
            String accessToken = null;
            while (System.currentTimeMillis() < deadline) {
                sleep(interval * 1000L);
                String tokBody = post(MS_TOKEN, null,
                        "client_id=" + MS_CLIENT_ID
                                + "&grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code"
                                + "&device_code=" + URLEncoder.encode(deviceCode, "UTF-8"));
                if (tokBody == null) {
                    return new Result(false, null, "等待登录时网络中断，请重试");
                }
                accessToken = pick(tokBody, "access_token", false);
                if (accessToken != null) {
                    break;
                }
                String err = pick(tokBody, "error", false);
                if ("authorization_pending".equals(err)) {
                    continue;
                }
                if ("slow_down".equals(err)) {
                    interval += 5;
                    continue;
                }
                if ("authorization_declined".equals(err)) {
                    return new Result(false, null, "你在浏览器里拒绝了授权");
                }
                if ("expired_token".equals(err)) {
                    return new Result(false, null, "验证码超时了，请重新点登录");
                }
                if ("bad_verification_code".equals(err)) {
                    return new Result(false, null, "验证码不对，请重新点登录");
                }
                return new Result(false, null, "登录失败：" + (err == null ? "未知原因" : err));
            }
            if (accessToken == null) {
                return new Result(false, null, "等待超时了，请重新点登录");
            }
            String meBody = request(MS_GRAPH_ME, accessToken, null, null);
            if (meBody == null) {
                return new Result(false, null, "登录成功但读不到账号信息，请重试");
            }
            String displayName = pick(meBody, "displayName", false);
            String mailBox = pick(meBody, "mail", false);
            if (mailBox == null) {
                mailBox = pick(meBody, "userPrincipalName", false);
            }
            if (displayName == null) {
                displayName = mailBox == null ? "微软账号" : mailBox;
            }
            save(MICROSOFT, displayName, null, mailBox);
            byte[] photo = fetchBytes(MS_GRAPH_PHOTO, "Bearer " + accessToken, null, 20000);
            saveAvatar(photo);
            return new Result(true, displayName, "登录成功");
        } catch (Exception e) {
            return new Result(false, null, "连不上微软登录服务，检查网络后重试");
        }
    }

    /** 上次微软登录还在不在：返回显示名，失败 null（不联网，只看本地记录） */
    public static String autoMicrosoft() {
        if (!MICROSOFT.equals(kind())) {
            return null;
        }
        return name;
    }

    // ------------------------------------------------------------ HTTP 小工具

    private static String post(String url, String token, String form) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(6000);
        c.setReadTimeout(8000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.setRequestProperty("User-Agent", "appcreator-login");
        if (token != null) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        try {
            OutputStream out = c.getOutputStream();
            out.write(form.getBytes("UTF-8"));
            out.close();
            int code = c.getResponseCode();
            if (code != 200 && code != 400) {
                return null;
            }
            // 400 也要读：设备码流程用 400 + error 字段表示「还在等授权」
            InputStream in = code == 200 ? c.getInputStream() : c.getErrorStream();
            return readAll(in);
        } finally {
            c.disconnect();
        }
    }

    /** GET 一个地址：token 非空时带 Bearer；status 非 null 时把响应码写回 status[0] */
    private static String request(String url, String token, String arg, int[] status) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(8000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "appcreator-login");
        if (token != null) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        try {
            int code = c.getResponseCode();
            if (status != null) {
                status[0] = code;
            }
            if (code != 200) {
                return null;
            }
            return readAll(c.getInputStream());
        } finally {
            c.disconnect();
        }
    }

    /** 下载二进制（头像），失败或无内容返回 null */
    private static byte[] fetchBytes(String url, String auth, String arg, int max) {
        if (url == null) {
            return null;
        }
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url.replace("\\/", "/")).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "appcreator-login");
            if (auth != null) {
                c.setRequestProperty("Authorization", auth);
            }
            if (c.getResponseCode() != 200) {
                return null;
            }
            InputStream in = new BufferedInputStream(c.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(b)) > 0) {
                total += n;
                if (total > max) {
                    break;
                }
                buf.write(b, 0, n);
            }
            in.close();
            byte[] bytes = buf.toByteArray();
            return bytes.length > 0 ? bytes : null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return null;
        }
        BufferedInputStream bin = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[2048];
        int n;
        while ((n = bin.read(b)) > 0) {
            buf.write(b, 0, n);
        }
        bin.close();
        return new String(buf.toByteArray(), "UTF-8");
    }

    private static String readText(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        String s = readAll(in);
        in.close();
        return s == null ? "" : s;
    }

    /** 从 JSON 里抠一个字段；numeric=true 时按数字匹配 */
    private static String pick(String json, String key, boolean numeric) {
        if (json == null) {
            return null;
        }
        Pattern p = Pattern.compile(String.format(numeric ? "\"%s\"\\s*:\\s*(\\d+)"
                : "\"%s\"\\s*:\\s*\"([^\"]*)\"", Pattern.quote(key)));
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
