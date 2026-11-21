package appcreator.gen;

import appcreator.model.Project;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * 把当前项目导出为 Windows EXE：生成源码 → 编译 → 打 jar → 调用 JDK 自带 jpackage 生成 app-image。
 * 生成的文件夹里带一份 Java 运行时，可脱离开发环境直接运行。
 */
public final class Exporter {

    public static final class Result {
        public final String error;
        public final File exe;
        public final File folder;

        Result(String error, File exe, File folder) {
            this.error = error;
            this.exe = exe;
            this.folder = folder;
        }

        public static Result fail(String message) {
            return new Result(message, null, null);
        }
    }

    private Exporter() {
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static Result exportExe(Project project, File destDir) {
        Path jdkBin = Paths.get(System.getProperty("java.home"), "bin");
        Path jpackage = jdkBin.resolve(isWindows() ? "jpackage.exe" : "jpackage");
        if (!Files.exists(jpackage)) {
            return Result.fail("当前运行环境不是完整 JDK（缺少 jpackage）。\n"
                    + "请用 JDK 14 及以上版本启动创作器后再导出 EXE。\n当前 java.home = "
                    + System.getProperty("java.home"));
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return Result.fail("当前运行环境没有 javac，无法编译。请用完整 JDK 启动创作器。");
        }

        Path work = null;
        try {
            work = Files.createTempDirectory("appcreator-exe-");
            Path classes = work.resolve("classes");
            Files.createDirectories(classes);

            Path srcFile = work.resolve("AppMain.java");
            Files.write(srcFile, CodeGenerator.generate(project).getBytes(StandardCharsets.UTF_8));

            // 1. 编译
            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8);
            Iterable<? extends JavaFileObject> units =
                    fm.getJavaFileObjectsFromFiles(Collections.singletonList(srcFile.toFile()));
            StringWriter log = new StringWriter();
            boolean ok = compiler.getTask(log, fm, diags,
                    Arrays.asList("-encoding", "UTF-8", "-g:none", "-d", classes.toString()),
                    null, units).call();
            fm.close();
            if (!ok) {
                return Result.fail(formatDiagnostics(diags));
            }

            // 2. 打 jar（自定义图标作为资源进 jar，运行时窗口用得上）
            if (project.iconPng != null && project.iconPng.length > 0) {
                Files.write(classes.resolve("appicon.png"), project.iconPng);
            }
            Path input = work.resolve("input");
            Files.createDirectories(input);
            Path jarFile = input.resolve("AppMain.jar");
            writeJar(classes, jarFile);

            // 3. 自定义图标：生成 .ico 交给 jpackage，让 exe 也带图标
            Path ico = null;
            if (project.iconPng != null && project.iconPng.length > 0) {
                try {
                    ico = work.resolve("app.ico");
                    Files.write(ico, IconUtil.toIco(project.iconPng));
                } catch (Exception ex) {
                    ico = null; // 图标失败不影响导出
                }
            }

            // 4. 决定输出名字（避免覆盖已有目录）
            String baseName = safeName(project.title);
            String name = baseName;
            File target = new File(destDir, name);
            int n = 2;
            while (target.exists()) {
                name = baseName + "_" + n;
                target = new File(destDir, name);
                n++;
            }
            if (!destDir.exists() && !destDir.mkdirs()) {
                return Result.fail("无法创建输出目录：" + destDir.getAbsolutePath());
            }

            // 5. jpackage 生成 app-image
            List<String> cmd = new ArrayList<String>();
            cmd.add(jpackage.toString());
            cmd.add("--type");
            cmd.add("app-image");
            cmd.add("--name");
            cmd.add(name);
            cmd.add("--input");
            cmd.add(input.toString());
            cmd.add("--main-jar");
            cmd.add("AppMain.jar");
            cmd.add("--main-class");
            cmd.add("AppMain");
            cmd.add("--dest");
            cmd.add(destDir.getAbsolutePath());
            cmd.add("--app-version");
            cmd.add("1.0");
            cmd.add("--java-options");
            cmd.add("-Dfile.encoding=UTF-8");
            cmd.add("--add-modules");
            cmd.add("java.desktop");
            if (ico != null && isWindows()) {
                cmd.add("--icon");
                cmd.add(ico.toAbsolutePath().toString());
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = readAll(proc);
            int code = proc.waitFor();
            if (code != 0) {
                return Result.fail("jpackage 执行失败（退出码 " + code + "）：\n" + output);
            }

            File exe = new File(target, name + (isWindows() ? ".exe" : ""));
            if (!exe.exists()) {
                return Result.fail("jpackage 没有生成可执行文件：\n" + output);
            }
            return new Result(null, exe, target);

        } catch (Exception ex) {
            return Result.fail("导出失败：\n" + ex);
        } finally {
            deleteRecursively(work);
        }
    }

    private static void writeJar(Path classesDir, Path jarFile) throws Exception {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "AppMain");

        List<Path> files = new ArrayList<Path>();
        Stream<Path> walk = Files.walk(classesDir);
        try {
            walk.forEach(f -> {
                if (Files.isRegularFile(f)) {
                    files.add(f);
                }
            });
        } finally {
            walk.close();
        }

        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()), mf);
        try {
            for (Path f : files) {
                String entry = classesDir.relativize(f).toString().replace('\\', '/');
                jos.putNextEntry(new JarEntry(entry));
                jos.write(Files.readAllBytes(f));
                jos.closeEntry();
            }
        } finally {
            jos.close();
        }
    }

    private static String formatDiagnostics(DiagnosticCollector<JavaFileObject> diags) {
        StringBuilder sb = new StringBuilder("编译错误，无法导出：\n");
        int count = 0;
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                count++;
                sb.append("第 ").append(d.getLineNumber()).append(" 行：")
                        .append(d.getMessage(Locale.CHINA)).append("\n");
                if (count >= 12) {
                    sb.append("（仅显示前 12 条）\n");
                    break;
                }
            }
        }
        if (count == 0) {
            sb.append("未知编译错误\n");
        }
        return sb.toString();
    }

    private static String safeName(String title) {
        String s = title == null ? "" : title.trim();
        s = s.replaceAll("[\\\\/:*?\"<>|]", "");
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() == 0) {
            s = "我的应用";
        }
        if (s.length() > 40) {
            s = s.substring(0, 40);
        }
        return s;
    }

    private static String readAll(Process p) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void deleteRecursively(Path dir) {
        if (dir == null) {
            return;
        }
        try {
            List<Path> all = new ArrayList<Path>();
            Stream<Path> walk = Files.walk(dir);
            try {
                walk.forEach(all::add);
            } finally {
                walk.close();
            }
            for (int i = all.size() - 1; i >= 0; i--) {
                Files.deleteIfExists(all.get(i));
            }
        } catch (Exception ignored) {
            // 清理失败不影响导出结果
        }
    }
}
