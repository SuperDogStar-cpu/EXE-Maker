package appcreator.gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 本机 Java 检测：扫出所有 JDK，逐个跑 {@code java -version} 验证能不能用，
 * 记住用户选中的那一个；编译 / 预览 / 导出 EXE 时优先用它。
 */
public final class JavaEnv {

    /** 一个可用的 Java 环境 */
    public static final class Jdk {

        public final String version;
        public final File home;
        public final File javaExe;
        public final boolean hasJavac;
        public final boolean hasJpackage;
        public final boolean current;

        Jdk(String version, File home, File javaExe, boolean hasJavac, boolean hasJpackage, boolean current) {
            this.version = version;
            this.home = home;
            this.javaExe = javaExe;
            this.hasJavac = hasJavac;
            this.hasJpackage = hasJpackage;
            this.current = current;
        }

        public String title() {
            String tag = hasJavac ? (hasJpackage ? "JDK" : "JDK") : "JRE";
            return "Java " + version + "（" + tag + (current ? " · 当前运行" : "") + "）";
        }

        public String detail() {
            return home.getAbsolutePath();
        }

        @Override
        public String toString() {
            return title() + "  ·  " + home.getAbsolutePath();
        }
    }

    private static final String CONFIG_NAME = "应用创作器.java.properties";
    private static List<Jdk> list = new ArrayList<Jdk>();
    private static Jdk selected; // null = 跟随当前运行环境

    private JavaEnv() {
    }

    public static List<Jdk> list() {
        return list;
    }

    public static Jdk selected() {
        return selected;
    }

    public static void select(Jdk jdk) {
        selected = jdk;
        save();
    }

    /** 当前运行环境算不算“用户没选” */
    public static boolean usesCurrent() {
        return selected == null;
    }

    /**
     * 扫描并验证本机所有 Java。较慢（每个候选都要跑一次 java -version），
     * 所以启动时放在后台线程里跑。
     */
    public static List<Jdk> detect() {
        List<Jdk> found = new ArrayList<Jdk>();
        List<File> homes = candidates();
        File running = normalizeHome(new File(System.getProperty("java.home")));
        String runningHome = running == null ? null : running.getAbsolutePath();
        for (File home : homes) {
            File javaExe = bin(home, "java");
            if (javaExe == null) {
                continue;
            }
            if (contains(found, home)) {
                continue;
            }
            String ver = version(javaExe);
            if (ver == null) {
                continue;
            }
            boolean isCurrent = runningHome != null && runningHome.equalsIgnoreCase(home.getAbsolutePath());
            found.add(new Jdk(ver, home, javaExe, bin(home, "javac") != null, bin(home, "jpackage") != null, isCurrent));
        }
        list = found;

        String saved = loadSavedPath();
        selected = null;
        if (saved != null) {
            for (Jdk j : found) {
                if (j.home.getAbsolutePath().equals(saved)) {
                    selected = j;
                    break;
                }
            }
        }
        return found;
    }

    private static boolean contains(List<Jdk> l, File home) {
        for (Jdk j : l) {
            if (j.home.getAbsolutePath().equalsIgnoreCase(home.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /** 选中的（或当前运行的）JDK 里某个可执行文件（javac / jpackage / java） */
    public static File tool(String name) {
        String exe = isWindows() ? name + ".exe" : name;
        if (selected != null) {
            File f = new File(new File(selected.home, "bin"), exe);
            if (f.isFile()) {
                return f;
            }
        }
        File f = new File(new File(normalizeHome(new File(System.getProperty("java.home")), true), "bin"), exe);
        return f.isFile() ? f : null;
    }

    // ---------------- 候选目录 ----------------

    private static List<File> candidates() {
        List<File> homes = new ArrayList<File>();
        addHome(homes, new File(System.getProperty("java.home")));
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            addHome(homes, new File(javaHome.trim()));
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String p : path.split(File.pathSeparator)) {
                if (p == null || p.trim().isEmpty()) {
                    continue;
                }
                addHome(homes, new File(p.trim()).getParentFile());
            }
        }
        if (isWindows()) {
            String pf = System.getenv("ProgramFiles");
            String pf86 = System.getenv("ProgramFiles(x86)");
            String local = System.getenv("LOCALAPPDATA");
            if (pf != null) {
                scanVendors(homes, new File(pf));
            }
            if (pf86 != null) {
                scanVendors(homes, new File(pf86));
            }
            if (local != null) {
                scanVendors(homes, new File(local + "\\Programs"));
                scanVendors(homes, new File(local + "\\Java"));
            }
            String userHome = System.getProperty("user.home");
            scanTwoLevels(homes, new File(userHome, "tools"));
            scanTwoLevels(homes, new File(userHome, ".jdks"));
            scanTwoLevels(homes, new File(System.getProperty("user.home"), "scoop\\apps"));
        } else {
            scanVendors(homes, new File("/usr/lib/jvm"));
            scanVendors(homes, new File("/opt"));
            scanTwoLevels(homes, new File("/usr/local"));
        }
        return homes;
    }

    /** Program Files 下 Java / Adoptium / Zulu / Microsoft / BellSoft 这些厂商目录 */
    private static void scanVendors(List<File> homes, File base) {
        if (base == null || !base.isDirectory()) {
            return;
        }
        String[] known = {"Java", "Eclipse Adoptium", "Eclipse Foundation", "AdoptOpenJDK", "Zulu",
                "Microsoft", "BellSoft", "Amazon Corretto", "Semeru", "RedHat", "ojdkbuild"};
        for (String k : known) {
            scanTwoLevels(homes, new File(base, k));
        }
    }

    /** dir 下一层、两层目录里如果能找到 bin/java 就当候选（jdk-21.0.1 / zulu21 / 21.0.12 等） */
    private static void scanTwoLevels(List<File> homes, File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        addHome(homes, dir);
        File[] one = dir.listFiles();
        if (one == null) {
            return;
        }
        for (File f : one) {
            if (!f.isDirectory()) {
                continue;
            }
            addHome(homes, f);
            File[] two = f.listFiles();
            if (two == null) {
                continue;
            }
            for (File g : two) {
                if (g.isDirectory() && (bin(g, "java") != null || bin(g, "javac") != null)) {
                    addHome(homes, g);
                }
            }
        }
    }

    private static void addHome(List<File> homes, File f) {
        if (f == null || !f.isDirectory()) {
            return;
        }
        File home = normalizeHome(f, true);
        if (home == null) {
            return;
        }
        if (bin(home, "java") == null) {
            return;
        }
        for (File h : homes) {
            if (h.getAbsolutePath().equalsIgnoreCase(home.getAbsolutePath())) {
                return;
            }
        }
        homes.add(home);
    }

    /** java.home 可能指向 .../jdk/jre，要回到 JDK 根目录 */
    private static File normalizeHome(File f) {
        return normalizeHome(f, true);
    }

    private static File normalizeHome(File f, boolean upIfJre) {
        if (f == null) {
            return null;
        }
        File home = f;
        if (home.getName().equalsIgnoreCase("jre") && home.getParentFile() != null) {
            home = home.getParentFile();
        }
        if (bin(home, "java") == null && !home.getName().equalsIgnoreCase("jre")) {
            // 有些运行时 java.home 指向 bin 之上的 release 目录，向上找一层
            File parent = home.getParentFile();
            if (parent != null && bin(parent, "java") != null) {
                home = parent;
            }
        }
        return home;
    }

    private static File bin(File home, String name) {
        if (home == null) {
            return null;
        }
        String exe = isWindows() ? name + ".exe" : name;
        File f = new File(new File(home, "bin"), exe);
        return f.isFile() ? f : null;
    }

    // ---------------- 验证 ----------------

    /** 跑 java -version，能正常退出就返回版本号，否则 null */
    private static String version(File javaExe) {
        try {
            ProcessBuilder pb = new ProcessBuilder(javaExe.getAbsolutePath(), "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            final StringBuilder sb = new StringBuilder();
            final Process proc = p;
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader r = null;
                    try {
                        r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                    } catch (IOException ignored) {
                        // 读不到就算了，按失败处理
                    } finally {
                        if (r != null) {
                            try {
                                r.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            });
            reader.setDaemon(true);
            reader.start();
            boolean done = p.waitFor() == 0;
            reader.join(3000);
            if (!done) {
                return null;
            }
            return parseVersion(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String parseVersion(String out) {
        int i = out.indexOf("version");
        if (i < 0) {
            return null;
        }
        int q1 = out.indexOf('"', i);
        if (q1 < 0) {
            return null;
        }
        int q2 = out.indexOf('"', q1 + 1);
        if (q2 < 0) {
            return null;
        }
        return out.substring(q1 + 1, q2);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    // ---------------- 配置 ----------------

    private static File configFile() {
        return new File(System.getProperty("user.home"), CONFIG_NAME);
    }

    private static String loadSavedPath() {
        try {
            File cfg = configFile();
            if (!cfg.isFile()) {
                return null;
            }
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(cfg);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            in.close();
            String v = p.getProperty("home");
            return v == null || v.trim().isEmpty() ? null : v.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("home", selected == null ? "" : selected.home.getAbsolutePath());
            Writer w = new OutputStreamWriter(new FileOutputStream(configFile()), StandardCharsets.UTF_8);
            p.store(w, "应用创作器 Java 选择");
            w.close();
        } catch (Exception ignored) {
            // 存不下就算了，不影响使用
        }
    }
}
