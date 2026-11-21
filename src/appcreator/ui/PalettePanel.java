package appcreator.ui;

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

    public PalettePanel(Consumer<String> onAddCenter) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(2, 0, 2, 0));

        for (String[] group : ComponentFactory.GROUPS) {
            add(sectionHead(group[0]));
            for (int i = 1; i < group.length; i++) {
                String type = group[i];
                int idx = typeIndex(type);
                Item item = new Item(type, ComponentFactory.GLYPHS[idx],
                        TINTS[idx % TINTS.length], onAddCenter);
                add(item);
                add(Box.createVerticalStrut(5));
            }
            add(Box.createVerticalStrut(4));
        }
        add(Box.createVerticalGlue());
    }

    private static int typeIndex(String type) {
        for (int i = 0; i < ComponentFactory.TYPES.length; i++) {
            if (ComponentFactory.TYPES[i].equals(type)) {
                return i;
            }
        }
        return 0;
    }

    /** 分类标题：左侧竖条 + 分类名 */
    private JComponent sectionHead(String name) {
        JPanel head = new JPanel(new BorderLayout(6, 0));
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(new EmptyBorder(2, 4, 4, 4));
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        head.setPreferredSize(new Dimension(170, 24));

        JLabel text = Theme.label(Lang.t(name), 11, Theme.MUTED);
        text.setFont(new Font(Fonts.familyForText(Lang.t(name)), Font.BOLD, 11));
        head.add(text, BorderLayout.WEST);
        return head;
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
            setToolTipText("拖到画布中放置，或双击直接添加");

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
            Fonts.draw(g2, label, ix + s + 10, (h - tm.getHeight()) / 2 + tm.getAscent(), Font.PLAIN, 13);
            g2.dispose();
        }
    }
}
