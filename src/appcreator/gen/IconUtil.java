package appcreator.gen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 自定义图标相关的纯 JDK 工具：读文件 → 缩放 → 统一存成 PNG 字节；导出 EXE 时再转成 .ico。
 */
public final class IconUtil {

    /** 图标最长边上限，既清晰又不会把项目文件撑大 */
    public static final int MAX_SIDE = 256;

    private IconUtil() {
    }

    /** 支持 png / jpg / gif / bmp；统一缩放到最长边 256 并返回 PNG 字节 */
    public static byte[] fromFile(File file) throws IOException {
        BufferedImage src = ImageIO.read(file);
        if (src == null) {
            throw new IOException("这个文件不是可识别的图片格式");
        }
        return toPng(src);
    }

    public static byte[] toPng(BufferedImage src) throws IOException {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            throw new IOException("图片尺寸无效");
        }
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bos);
        return bos.toByteArray();
    }

    /** 把 PNG 字节解成当前需要的尺寸，保持比例并居中（透明底） */
    public static Image image(byte[] png, int size) {
        if (png == null || png.length == 0) {
            return null;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
            if (src == null) {
                return null;
            }
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = src.getWidth();
            int h = src.getHeight();
            double s = Math.min((double) size / w, (double) size / h);
            int nw = Math.max(1, (int) Math.round(w * s));
            int nh = Math.max(1, (int) Math.round(h * s));
            g.drawImage(src, (size - nw) / 2, (size - nh) / 2, nw, nh, null);
            g.dispose();
            return out;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 生成 Windows .ico 文件字节（单个 PNG 压缩条目，Vista 及以上都支持）。
     * 供 jpackage --icon 使用，从而让导出的 exe 在资源管理器里也显示自定义图标。
     */
    public static byte[] toIco(byte[] png) throws IOException {
        if (png == null || png.length == 0) {
            throw new IOException("图标数据为空");
        }
        int w;
        int h;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null) {
                throw new IOException("图标数据不是有效 PNG");
            }
            w = img.getWidth();
            h = img.getHeight();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("解析图标失败：" + ex);
        }
        // ICO 目录项里 0 表示 256
        int bw = w >= 256 ? 0 : w;
        int bh = h >= 256 ? 0 : h;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // ICONDIR
        writeLE(bos, 0, 2);          // reserved
        writeLE(bos, 1, 2);          // type = icon
        writeLE(bos, 1, 2);          // 图像数量
        // ICONDIRENTRY（22 字节）
        bos.write(bw & 0xFF);
        bos.write(bh & 0xFF);
        bos.write(0);                // 调色板颜色数
        bos.write(0);                // reserved
        writeLE(bos, 1, 2);          // 色彩平面
        writeLE(bos, 32, 2);         // 位深
        writeLE(bos, png.length, 4); // 数据长度
        writeLE(bos, 22, 4);         // 数据偏移
        bos.write(png);
        return bos.toByteArray();
    }

    private static void writeLE(ByteArrayOutputStream out, int value, int bytes) {
        for (int i = 0; i < bytes; i++) {
            out.write((value >> (8 * i)) & 0xFF);
        }
    }
}
