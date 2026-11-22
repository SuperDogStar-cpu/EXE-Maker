package appcreator.ui;

import appcreator.io.ComponentData;
import appcreator.io.Lang;
import appcreator.model.ComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 左侧组件库：可拖拽，也可双击快速添加到画布中央。
 */
public class PalettePanel extends JPanel {

    private static final Color[] TINTS = {
            new Color(0x2FC2A6), new Color(0x58B7E8), new Color(0x7C9EF2),
            new Color(0xF2A65A), new Color(0x9BD26B), new Color(0xE98FA5),
            new Color(0x63C7C2), new Color(0xB39DEE), new Color(0xEFC25A)
    };

    /** 折叠起来的分类（点分类标题展开 / 收起，同一次运行里记住状态） */
    private static final java.util.Set<String> COLLAPSED = new java.util.HashSet<String>();

    public PalettePanel(Consumer<String> onAddCenter) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(2, 0, 2, 0));

        for (String[] group : ComponentFactory.GROUPS) {
            java.util.List<Component> items = new java.util.ArrayList<Component>();
            for (int i = 1; i < group.length; i++) {
                String type = group[i];
                int idx = typeIndex(type);
                items.add(new Item(type, ComponentFactory.GLYPHS[idx],
                        TINTS[idx % TINTS.length], onAddCenter));
                items.add(Box.createVerticalStrut(5));
            }
            addSection(group[0], items);
        }
        // 启动时自动下载的 .NET 工具箱组件（容器 / 菜单 / 数据 / 输入 / 对话框 / 组件）
        if (ComponentData.count() == 0) {
            ComponentData.load();
        }
        for (String category : ComponentData.categories()) {
            java.util.List<Component> items = new java.util.ArrayList<Component>();
            for (ComponentData.Entry e : ComponentData.byCategory(category)) {
                int tint = Math.abs((category + e.name).hashCode()) % TINTS.length;
                items.add(new Item(e.name, e.glyph, TINTS[tint], onAddCenter));
                items.add(Box.createVerticalStrut(5));
            }
            addSection(category, items);
        }
        add(Box.createVerticalGlue());
    }

    private void addSection(String title, java.util.List<Component> items) {
        add(sectionHead(title, items));
        for (Component c : items) {
            c.setVisible(!COLLAPSED.contains(title));
            add(c);
        }
        add(Box.createVerticalStrut(4));
    }

    private static int typeIndex(String type) {
        for (int i = 0; i < ComponentFactory.TYPES.length; i++) {
            if (ComponentFactory.TYPES[i].equals(type)) {
                return i;
            }
        }
        return 0;
    }

    /** 分类标题：三角箭头 + 分类名，点一下收起 / 展开该分类 */
    private JComponent sectionHead(final String name, final java.util.List<Component> items) {
        final Head head = new Head(name);
        head.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean collapsed = !COLLAPSED.contains(name);
                if (collapsed) {
                    COLLAPSED.add(name);
                } else {
                    COLLAPSED.remove(name);
                }
                head.collapsed = collapsed;
                head.setToolTipText(collapsed ? "点一下展开该分类" : "点一下收起该分类");
                for (Component c : items) {
                    c.setVisible(!collapsed);
                }
                revalidate();
                repaint();
            }
        });
        return head;
    }

    private static class Head extends JComponent {

        private final String name;
        private boolean collapsed;

        Head(String name) {
            this.name = name;
            this.collapsed = COLLAPSED.contains(name);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(170, 24));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            setMinimumSize(new Dimension(100, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(collapsed ? "点一下展开该分类" : "点一下收起该分类");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 左侧小三角：收起时朝右，展开时朝下
            g2.setColor(Theme.MUTED);
            int cx = 7;
            int cy = getHeight() / 2 + 1;
            int s = 4;
            if (collapsed) {
                g2.fillPolygon(new int[]{cx, cx, cx + 2 * s}, new int[]{cy - s, cy + s, cy}, 3);
            } else {
                g2.fillPolygon(new int[]{cx, cx + 2 * s, cx + s}, new int[]{cy - s, cy - s, cy + s}, 3);
            }

            String label = Lang.t(name);
            g2.setFont(new Font(Fonts.familyForText(label), Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.MUTED);
            Fonts.draw(g2, label, 16, (getHeight() - fm.getHeight()) / 2 + fm.getAscent(), Font.BOLD, 11);
            g2.dispose();
        }
    }

    private static class Item extends JComponent {

        private final String type;
        private final char glyph;
        private final Color tint;
        private final Consumer<String> onAddCenter;
        private boolean hover;
        private Point pressedAt;

        Item(String type, char glyph, Color tint, Consumer<String> onAddCenter) {
            this.type = type;
            this.glyph = glyph;
            this.tint = tint;
            this.onAddCenter = onAddCenter;
            setPreferredSize(new Dimension(170, 36));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setMinimumSize(new Dimension(120, 36));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(Lang.t(type) + " · 拖到画布中放置，或双击直接添加");

            setTransferHandler(new TransferHandler() {
                @Override
                protected Transferable createTransferable(JComponent c) {
                    return new DesignCanvas.WidgetTransferable(type);
                }

                @Override
                public int getSourceActions(JComponent c) {
                    return TransferHandler.COPY;
                }
            });

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressedAt = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (pressedAt == null) {
                        return;
                    }
                    if (Math.abs(e.getX() - pressedAt.x) + Math.abs(e.getY() - pressedAt.y) < 6) {
                        return;
                    }
                    pressedAt = null;
                    TransferHandler th = getTransferHandler();
                    if (th != null) {
                        th.exportAsDrag(Item.this, e, TransferHandler.COPY);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && onAddCenter != null) {
                        onAddCenter.accept(type);
                    }
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(hover ? Theme.SOFT : new Color(0xFBFDFC));
            g2.fillRoundRect(0, 1, w - 1, h - 3, 12, 12);
            g2.setColor(hover ? Theme.ACCENT : Theme.BORDER);
            g2.drawRoundRect(0, 1, w - 1, h - 3, 12, 12);

            int s = 24;
            int ix = 10;
            int iy = (h - s) / 2;
            g2.setColor(tint);
            g2.fillRoundRect(ix, iy, s, s, 8, 8);
            g2.setColor(Color.WHITE);
            String ch = String.valueOf(glyph);
            g2.setFont(new Font(Fonts.familyForText(ch), Font.BOLD, 12));
            int cw = Fonts.width(g2, ch);
            FontMetrics fm = g2.getFontMetrics();
            Fonts.draw(g2, ch, ix + (s - cw) / 2,
                    iy + (s - fm.getHeight()) / 2 + fm.getAscent(), Font.BOLD, 12);

            String label = Lang.t(type);
            g2.setFont(new Font(Fonts.familyForText(label), Font.PLAIN, 13));
            FontMetrics tm = g2.getFontMetrics();
            g2.setColor(Theme.TEXT);
            int maxW = Math.max(10, w - (ix + s + 10) - 8);
            String drawn = label;
            if (Fonts.width(g2, drawn) > maxW) {
                // 名称太长就截断加省略号，完整名称在鼠标悬停提示里看
                while (drawn.length() > 1 && Fonts.width(g2, drawn + "…") > maxW) {
                    drawn = drawn.substring(0, drawn.length() - 1);
                }
                drawn = drawn + "…";
            }
            Fonts.draw(g2, drawn, ix + s + 10, (h - tm.getHeight()) / 2 + tm.getAscent(), Font.PLAIN, 13);
            g2.dispose();
        }
    }
}
