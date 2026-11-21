package appcreator.gen;

import appcreator.model.Project;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 把生成的源码在内存中编译，然后在同一个 JVM 里启动真实窗口做预览。
 * 返回值：null 表示成功，否则是错误信息。
 */
public final class Runner {

    private Runner() {
    }

    public static String run(Project project) {
        String source = CodeGenerator.generate(project);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return runExternal(source, project);
        }

        Path dir = null;
        try {
            dir = Files.createTempDirectory("appcreator-run-");
            Path srcFile = dir.resolve("AppMain.java");
            Files.write(srcFile, source.getBytes(StandardCharsets.UTF_8));
            // 自定义图标作为资源文件放在编译输出目录，生成的代码用 getResourceAsStream 读取
            if (project.iconPng != null && project.iconPng.length > 0) {
                Files.write(dir.resolve("appicon.png"), project.iconPng);
            }

            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8);
            Iterable<? extends JavaFileObject> units =
                    fm.getJavaFileObjectsFromFiles(Collections.singletonList(srcFile.toFile()));
            StringWriter log = new StringWriter();
            boolean ok = compiler.getTask(log, fm, diags,
                    Arrays.asList("-encoding", "UTF-8", "-g:none"), null, units).call();
            fm.close();
            if (!ok) {
                return formatDiagnostics(diags);
            }

            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            if (parent == null) {
                parent = Runner.class.getClassLoader();
            }
            URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, parent);
            Class<?> clazz = loader.loadClass("AppMain");
            // 预览模式：生成的窗口只释放自己，不调用 System.exit
            System.setProperty("appcreator.preview", "true");
            clazz.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            return null;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            return "运行出错：\n" + cause;
        } catch (Exception ex) {
            return "运行失败：\n" + ex;
        } finally {
            // 编译产物需要保留到程序结束（预览窗口可能仍会延迟加载匿名内部类）
            if (dir != null) {
                dir.toFile().deleteOnExit();
            }
        }
    }

    private static String formatDiagnostics(DiagnosticCollector<JavaFileObject> diags) {
        StringBuilder sb = new StringBuilder("编译错误：\n");
        int count = 0;
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                count++;
                String msg = d.getMessage(Locale.CHINA);
                sb.append("第 ").append(d.getLineNumber()).append(" 行：").append(msg).append("\n");
                if (count >= 12) {
                    sb.append("（仅显示前 12 条）\n");
                    break;
                }
            }
        }
        if (count == 0) {
            sb.append("未知错误\n");
        }
        return sb.toString();
    }

    /** 当前环境只有 JRE 时，退化为调用外部 javac / java */
    private static String runExternal(String source, Project project) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("appcreator-run-");
            Path srcFile = dir.resolve("AppMain.java");
            Files.write(srcFile, source.getBytes(StandardCharsets.UTF_8));
            if (project.iconPng != null && project.iconPng.length > 0) {
                Files.write(dir.resolve("appicon.png"), project.iconPng);
            }

            ProcessBuilder javac = new ProcessBuilder("javac", "-encoding", "UTF-8",
                    "-d", dir.toString(), srcFile.toString());
            javac.redirectErrorStream(true);
            Process p1 = javac.start();
            String out1 = readAll(p1);
            if (p1.waitFor() != 0) {
                return "编译失败（未找到内置编译器，请确认已安装 JDK）：\n" + out1;
            }

            ProcessBuilder java = new ProcessBuilder("java", "-Dappcreator.preview=true",
                    "-cp", dir.toString(), "AppMain");
            java.redirectErrorStream(true);
            Process p2 = java.start();
            p2.getOutputStream().close();
            return null;
        } catch (IOException ex) {
            return "无法启动外部编译器，请安装 JDK（而非仅 JRE）。\n" + ex;
        } catch (InterruptedException ex) {
            return "运行被中断：\n" + ex;
        } finally {
            // 外部进程仍在使用这些 class 文件，交给系统临时目录回收
        }
    }

    private static String readAll(Process p) throws IOException {
        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
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
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteRecursively(f.toPath());
                    } else {
                        f.delete();
                    }
                }
            }
            Files.deleteIfExists(dir);
        } catch (Exception ignored) {
            // 临时文件清理失败不影响使用
        }
    }

    public static List<String> warnings() {
        return Collections.emptyList();
    }
}
