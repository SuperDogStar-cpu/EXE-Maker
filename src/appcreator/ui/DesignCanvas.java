package appcreator.ui;

import appcreator.model.ComponentFactory;
import appcreator.model.Project;
import appcreator.model.Widget;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 设计画布：底层放真实 Swing 组件，上层是透明玻璃层，负责选中、拖动、缩放和接收拖放。
 */
public class DesignCanvas extends JLayeredPane {

    /** 自定义拖拽数据格式（同一 JVM 内部使用） */
    public static final DataFlavor WIDGET_FLAVOR = createFlavor();

    private static DataFlavor createFlavor() {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=java.lang.String;humanPresentableName=AppCreatorWidget");
        } catch (Exception e) {
            return DataFlavor.stringFlavor;
        }
    }

    private static final int NONE = 0;
    private static final int MOVE = 1;
    private static final int RESIZE = 2;
    private static final int HANDLE = 7;

    private Project project = new Project();
    private final Map<Widget, JComponent> comps = new LinkedHashMap<Widget, JComponent>();
    private Widget selected;

    private int mode = NONE;
    private int handle = -1;
    private final Point press = new Point();
    private Rectangle startRect = new Rectangle();

    private Consumer<Widget> onSelect;
    private Runnable onChange;
    private BiConsumer<String, Point> onDrop;

    private final JPanel base = new JPanel(null) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.GRID);
            for (int x = 20; x < w; x += 20) {
                for (int y = 20; y < h; y += 20) {
                    g2.fillRect(x, y, 1, 1);
                }
            }
            g2.setColor(Theme.BORDER);
            g2.drawRect(0, 0, w - 1, h - 1);
            g2.dispose();
        }
    };

    private final JPanel glass = new JPanel(null) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selected == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.ACCENT);
            g2.fillRect(selected.x, selected.y, selected.w, 1);
            g2.fillRect(selected.x, selected.y + selected.h - 1, selected.w, 1);
            g2.fillRect(selected.x, selected.y, 1, selected.h);
            g2.fillRect(selected.x + selected.w - 1, selected.y, 1, selected.h);

            // 组件名字标签
            String tag = selected.name;
            g2.setFont(new Font(Fonts.familyForText(tag), Font.PLAIN, 11));
            int tw = Fonts.width(g2, tag) + 10;
            int ty = selected.y - 20;
            if (ty < 0) {
                ty = selected.y + 3;
            }
            g2.setColor(new Color(0x22, 0x31, 0x2E, 200));
            g2.fillRoundRect(selected.x, ty, tw, 17, 8, 8);
            g2.setColor(Color.WHITE);
            Fonts.draw(g2, tag, selected.x + 5, ty + 12);

            // 八个缩放手柄
            for (Rectangle r : handles(selected)) {
                g2.setColor(Color.WHITE);
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.setColor(Theme.ACCENT);
                g2.drawRect(r.x, r.y, r.width, r.height);
            }
            g2.dispose();
        }
    };

    public DesignCanvas() {
        setLayout(null);
        setOpaque(false);
        base.setOpaque(true);
        base.setBackground(Color.WHITE);
        glass.setOpaque(false);
        glass.setLayout(null);
        glass.addMouseListener(glassMouse);
        glass.addMouseMotionListener(glassMouse);
        glass.setTransferHandler(new DropHandler());
        glass.setFocusable(true);

        add(base, Integer.valueOf(0));
        add(glass, Integer.valueOf(100));
        setCanvasSize(project.width, project.height);
    }

    // ---------------- 对外接口 ----------------

    public void setProject(Project p) {
        this.project = p;
        base.removeAll();
        comps.clear();
        selected = null;
        for (Widget w : p.widgets) {
            JComponent c = ComponentFactory.create(w);
            base.add(c);
            comps.put(w, c);
        }
        base.setBackground(p.bg);
        setCanvasSize(p.width, p.height);
        base.revalidate();
        base.repaint();
        repaint();
        if (onSelect != null) {
            onSelect.accept(null);
        }
    }

    public void setCanvasSize(int w, int h) {
        setPreferredSize(new Dimension(Math.max(80, w), Math.max(60, h)));
        base.setBounds(0, 0, Math.max(80, w), Math.max(60, h));
        glass.setBounds(0, 0, Math.max(80, w), Math.max(60, h));
        revalidate();
        repaint();
    }

    public void refreshBackground() {
        base.setBackground(project.bg);
        base.repaint();
    }

    public void addWidget(Widget w) {
        JComponent c = ComponentFactory.create(w);
        base.add(c);
        comps.put(w, c);
        base.revalidate();
        base.repaint();
        selected = w;
        if (onSelect != null) {
            onSelect.accept(w);
        }
        repaint();
    }

    public void removeWidget(Widget w) {
        JComponent c = comps.remove(w);
        if (c != null) {
            base.remove(c);
        }
        if (selected == w) {
            selected = null;
            if (onSelect != null) {
                onSelect.accept(null);
            }
        }
        base.revalidate();
        base.repaint();
        repaint();
    }

    public void refresh(Widget w) {
        JComponent c = comps.get(w);
        if (c != null) {
            ComponentFactory.apply(w, c);
            c.revalidate();
        }
        base.repaint();
        repaint();
    }

    public Widget getSelected() {
        return selected;
    }

    public void select(Widget w) {
        selected = w;
        if (onSelect != null) {
            onSelect.accept(w);
        }
        repaint();
    }

    public void setOnSelect(Consumer<Widget> l) {
        this.onSelect = l;
    }

    public void setOnChange(Runnable l) {
        this.onChange = l;
    }

    public void setOnDrop(BiConsumer<String, Point> l) {
        this.onDrop = l;
    }

    // ---------------- 内部 ----------------

    private Rectangle boundsOf(Widget w) {
        return new Rectangle(w.x, w.y, w.w, w.h);
    }

    private Rectangle[] handles(Widget w) {
        int s = HANDLE;
        int x = w.x;
        int y = w.y;
        int ww = w.w;
        int hh = w.h;
        Rectangle[] r = new Rectangle[8];
        r[0] = new Rectangle(x - s / 2, y - s / 2, s, s);                            // 左上
        r[1] = new Rectangle(x + ww / 2 - s / 2, y - s / 2, s, s);                   // 上
        r[2] = new Rectangle(x + ww - s / 2, y - s / 2, s, s);                       // 右上
        r[3] = new Rectangle(x + ww - s / 2, y + hh / 2 - s / 2, s, s);              // 右
        r[4] = new Rectangle(x + ww - s / 2, y + hh - s / 2, s, s);                  // 右下
        r[5] = new Rectangle(x + ww / 2 - s / 2, y + hh - s / 2, s, s);              // 下
        r[6] = new Rectangle(x - s / 2, y + hh - s / 2, s, s);                       // 左下
        r[7] = new Rectangle(x - s / 2, y + hh / 2 - s / 2, s, s);                   // 左
        return r;
    }

    private int handleAt(Point p, Widget w) {
        Rectangle[] hs = handles(w);
        for (int i = 0; i < hs.length; i++) {
            Rectangle r = new Rectangle(hs[i].x - 2, hs[i].y - 2, hs[i].width + 4, hs[i].height + 4);
            if (r.contains(p)) {
                return i;
            }
        }
        return -1;
    }

    private Widget findWidget(Point p) {
        for (int i = project.widgets.size() - 1; i >= 0; i--) {
            Widget w = project.widgets.get(i);
            if (new Rectangle(w.x, w.y, w.w, w.h).contains(p)) {
                return w;
            }
        }
        return null;
    }

    /** 缩放上限：不超过窗口（项目窗口）尺寸；最小尺寸不再限制 */
    private void resizeSelected(Point p) {
        int maxW = Math.max(1, project.width);
        int maxH = Math.max(1, project.height);

        int x1 = startRect.x;
        int y1 = startRect.y;
        int x2 = startRect.x + startRect.width;
        int y2 = startRect.y + startRect.height;

        // 拖动点先夹在窗口范围内，组件就不会被拖出窗口
        int px = Math.max(0, Math.min(p.x, maxW));
        int py = Math.max(0, Math.min(p.y, maxH));

        if (handle == 0 || handle == 1 || handle == 2) {
            y1 = py;
        }
        if (handle == 4 || handle == 5 || handle == 6) {
            y2 = py;
        }
        if (handle == 0 || handle == 6 || handle == 7) {
            x1 = px;
        }
        if (handle == 2 || handle == 3 || handle == 4) {
            x2 = px;
        }

        // 只保证宽高不小于 1 像素，不再设下限
        if (x2 - x1 < 1) {
            if (handle == 0 || handle == 6 || handle == 7) {
                x1 = x2 - 1;
            } else {
                x2 = x1 + 1;
            }
        }
        if (y2 - y1 < 1) {
            if (handle == 0 || handle == 1 || handle == 2) {
                y1 = y2 - 1;
            } else {
                y2 = y1 + 1;
            }
        }

        // 上限：整块不能超出窗口
        x1 = Math.max(0, Math.min(x1, maxW - 1));
        x2 = Math.min(maxW, Math.max(x2, x1 + 1));
        y1 = Math.max(0, Math.min(y1, maxH - 1));
        y2 = Math.min(maxH, Math.max(y2, y1 + 1));

        selected.x = x1;
        selected.y = y1;
        selected.w = Math.max(1, Math.min(maxW - x1, x2 - x1));
        selected.h = Math.max(1, Math.min(maxH - y1, y2 - y1));
        applyBounds(selected);
    }

    /** 位置和尺寸夹在窗口范围内（不再限制最小尺寸） */
    private void applyBounds(Widget w) {
        // 上限：组件不能超出窗口范围，超出就把它挤回窗口里
        int maxW = Math.max(1, project.width);
        int maxH = Math.max(1, project.height);
        w.w = Math.min(maxW, Math.max(1, w.w));
        w.h = Math.min(maxH, Math.max(1, w.h));
        if (w.x < 0) {
            w.x = 0;
        }
        if (w.y < 0) {
            w.y = 0;
        }
        if (w.x + w.w > maxW) {
            w.x = Math.max(0, maxW - w.w);
        }
        if (w.y + w.h > maxH) {
            w.y = Math.max(0, maxH - w.h);
        }
        JComponent c = comps.get(w);
        if (c != null) {
            c.setBounds(w.x, w.y, w.w, w.h);
            // 图片 / 视频的资源和控件一起缩放（放大控件，图片按同比例放大，不变形）
            if ("图片".equals(w.type) || "视频".equals(w.type)) {
                ComponentFactory.apply(w, c);
            }
        }
        base.repaint();
    }

    private final MouseAdapter glassMouse = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            glass.requestFocusInWindow();
            Point p = e.getPoint();
            if (selected != null) {
                int idx = handleAt(p, selected);
                if (idx >= 0) {
                    mode = RESIZE;
                    handle = idx;
                    press.setLocation(p);
                    startRect = boundsOf(selected);
                    return;
                }
            }
            Widget hit = findWidget(p);
            if (hit != null) {
                mode = MOVE;
                press.setLocation(p);
                startRect = boundsOf(hit);
                if (hit != selected) {
                    selected = hit;
                    if (onSelect != null) {
                        onSelect.accept(hit);
                    }
                }
                repaint();
            } else {
                mode = NONE;
                selected = null;
                if (onSelect != null) {
                    onSelect.accept(null);
                }
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selected == null || mode == NONE) {
                return;
            }
            Point p = e.getPoint();
            if (mode == MOVE) {
                selected.x = Math.max(0, startRect.x + p.x - press.x);
                selected.y = Math.max(0, startRect.y + p.y - press.y);
                applyBounds(selected);
            } else if (mode == RESIZE) {
                resizeSelected(p);
            }
            if (onChange != null) {
                onChange.run();
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (mode != NONE && onChange != null) {
                onChange.run();
            }
            mode = NONE;
            handle = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point p = e.getPoint();
            if (selected != null) {
                int idx = handleAt(p, selected);
                if (idx >= 0) {
                    glass.setCursor(Cursor.getPredefinedCursor(cursorOf(idx)));
                    return;
                }
            }
            Widget hit = findWidget(p);
            glass.setCursor(hit != null
                    ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    : Cursor.getDefaultCursor());
        }
    };

    private static int cursorOf(int handle) {
        if (handle == 0 || handle == 4) {
            return Cursor.NW_RESIZE_CURSOR;
        }
        if (handle == 2 || handle == 6) {
            return Cursor.NE_RESIZE_CURSOR;
        }
        if (handle == 1 || handle == 5) {
            return Cursor.N_RESIZE_CURSOR;
        }
        return Cursor.E_RESIZE_CURSOR;
    }

    // ---------------- 拖放 ----------------

    /** 调色板拖动时携带的数据 */
    public static class WidgetTransferable implements Transferable, ClipboardOwner {

        private final String type;

        public WidgetTransferable(String type) {
            this.type = type;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{WIDGET_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return WIDGET_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!WIDGET_FLAVOR.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return type;
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // 无需处理
        }
    }

    private class DropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(WIDGET_FLAVOR);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                String type = (String) support.getTransferable().getTransferData(WIDGET_FLAVOR);
                Point p = support.getDropLocation().getDropPoint();
                if (onDrop != null) {
                    onDrop.accept(type, new Point(p.x, p.y));
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /** 供键盘删除使用 */
    public void deleteSelected() {
        if (selected == null) {
            return;
        }
        Widget w = selected;
        project.widgets.remove(w);
        removeWidget(w);
    }

    public List<Widget> widgets() {
        return new ArrayList<Widget>(project.widgets);
    }
}
