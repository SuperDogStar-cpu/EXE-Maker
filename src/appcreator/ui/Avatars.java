package appcreator.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * 圆形头像绘制：有图片就按「填满裁圆」画，没有就用账号名首字母画在薄荷色圆底上。
 */
public final class Avatars {

    private Avatars() {
    }

    /** 取账号首字母（中文取第一个字，英文取第一个字母并大写） */
    public static String initial(String name) {
        if (name == null) {
            return "?";
        }
        String s = name.trim();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                String one = String.valueOf(c);
                return one.toUpperCase();
            }
        }
        return "?";
    }

    public static void paint(Graphics2D g, int size, BufferedImage img, String name, boolean ring) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (img != null) {
            Shape clip = new Ellipse2D.Float(0, 0, size, size);
            Graphics2D c = (Graphics2D) g.create();
            c.setClip(clip);
            double scale = Math.max(size / (double) img.getWidth(), size / (double) img.getHeight());
            int w = (int) Math.ceil(img.getWidth() * scale);
            int h = (int) Math.ceil(img.getHeight() * scale);
            c.drawImage(img, (size - w) / 2, (size - h) / 2, w, h, null);
            c.dispose();
        } else {
            g.setColor(Theme.ACCENT);
            g.fillOval(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Theme.uiFamily(), Font.BOLD, Math.max(10, (int) (size * 0.42))));
            String t = initial(name);
            int tw = g.getFontMetrics().stringWidth(t);
            int th = g.getFontMetrics().getAscent();
            g.drawString(t, (size - tw) / 2, (size + th) / 2 - 2);
        }
        if (ring) {
            g.setColor(new Color(0xD7E6E1));
            g.setStroke(new BasicStroke(1f));
            g.drawOval(0, 0, size - 1, size - 1);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** 生成一张 size×size 的圆头像图（给登录窗口用） */
    public static BufferedImage render(int size, String name) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        paint(g, size, appcreator.io.Account.avatar(), name, false);
        g.dispose();
        return out;
    }
}
