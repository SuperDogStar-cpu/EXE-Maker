package appcreator.ui;

import appcreator.io.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 工具栏右上角的账号头像（在「设置」右边）：圆形头像，鼠标移上去有提示，点一下弹出账号菜单。
 */
public class AvatarButton extends JComponent {

    private static final int SIZE = 30;

    private final Runnable onSwitch;
    private final Runnable onLogout;

    public AvatarButton(Runnable onSwitch, Runnable onLogout) {
        this.onSwitch = onSwitch;
        this.onLogout = onLogout;
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setToolTipText(Account.platformName() + " · " + Account.display());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMenu(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Avatars.paint(g2, Math.min(getWidth(), getHeight()), Account.avatar(), Account.name(), true);
        g2.dispose();
    }

    private void showMenu(Component c, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new EmptyBorder(6, 6, 6, 6));
        JMenuItem header = new JMenuItem(Account.platformName() + " · " + Account.display());
        header.setEnabled(false);
        header.setFont(Theme.ui(12, Font.BOLD));
        menu.add(header);
        menu.addSeparator();

        JMenuItem change = new JMenuItem("切换账号");
        change.setFont(Theme.ui(12));
        change.addActionListener(e -> {
            if (onSwitch != null) {
                onSwitch.run();
            }
        });
        menu.add(change);

        JMenuItem logout = new JMenuItem("退出登录");
        logout.setFont(Theme.ui(12));
        logout.setForeground(Theme.DANGER);
        logout.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        menu.add(logout);

        menu.show(c, x - 120, y + 8);
    }
}
