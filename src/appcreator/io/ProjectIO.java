package appcreator.io;

import appcreator.model.Project;
import appcreator.model.Widget;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 项目文件（*.apcj）读写：轻量二进制格式，不依赖任何外部库。
 */
public final class ProjectIO {

    private static final String MAGIC = "APCJ1";

    private ProjectIO() {
    }

    public static void save(Project p, File file) throws IOException {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            writeText(out, MAGIC);
            writeText(out, p.title);
            out.writeInt(p.width);
            out.writeInt(p.height);
            out.writeInt(p.bg == null ? 0xFFFFFF : p.bg.getRGB());
            out.writeInt(p.widgets.size());
            for (Widget w : p.widgets) {
                writeText(out, w.type);
                writeText(out, w.name);
                out.writeInt(w.x);
                out.writeInt(w.y);
                out.writeInt(w.w);
                out.writeInt(w.h);
                writeText(out, w.text);
                out.writeInt(w.fontSize);
                out.writeBoolean(w.bold);
                out.writeInt(w.fg == null ? 0x22312E : w.fg.getRGB());
                out.writeInt(w.bg == null ? -1 : w.bg.getRGB());
                writeText(out, w.items);
            }
            writeText(out, p.logic);
            // 自定义窗口图标（PNG 字节），可能为空
            if (p.iconPng != null && p.iconPng.length > 0) {
                out.writeBoolean(true);
                out.writeInt(p.iconPng.length);
                out.write(p.iconPng);
            } else {
                out.writeBoolean(false);
            }
            writeText(out, p.iconName == null ? "" : p.iconName);
        } finally {
            out.close();
        }
    }

    public static Project load(File file) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            String magic = readText(in);
            if (!MAGIC.equals(magic)) {
                throw new IOException("不是有效的创作器项目文件");
            }
            Project p = new Project();
            p.widgets.clear();
            p.title = readText(in);
            p.width = in.readInt();
            p.height = in.readInt();
            p.bg = new Color(in.readInt(), true);
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                Widget w = new Widget();
                w.type = readText(in);
                w.name = readText(in);
                w.x = in.readInt();
                w.y = in.readInt();
                w.w = in.readInt();
                w.h = in.readInt();
                w.text = readText(in);
                w.fontSize = in.readInt();
                w.bold = in.readBoolean();
                w.fg = new Color(in.readInt(), true);
                int bg = in.readInt();
                w.bg = bg == -1 ? null : new Color(bg, true);
                w.items = readText(in);
                p.widgets.add(w);
            }
            p.logic = readText(in);
            // 图标段是新版本才有的：老项目文件读到这里就结束了
            try {
                if (in.readBoolean()) {
                    int len = in.readInt();
                    if (len > 0 && len <= 4000000) {
                        byte[] data = new byte[len];
                        int read = 0;
                        while (read < len) {
                            int r = in.read(data, read, len - read);
                            if (r < 0) {
                                throw new IOException("项目文件已损坏");
                            }
                            read += r;
                        }
                        p.iconPng = data;
                    }
                }
                p.iconName = readText(in);
            } catch (java.io.EOFException ignored) {
                p.iconPng = null;
                p.iconName = "";
            }
            return p;
        } finally {
            in.close();
        }
    }

    private static void writeText(DataOutputStream out, String s) throws IOException {
        byte[] bytes = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readText(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 20000000) {
            throw new IOException("项目文件已损坏");
        }
        byte[] bytes = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(bytes, read, len - read);
            if (r < 0) {
                throw new IOException("项目文件已损坏");
            }
            read += r;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
