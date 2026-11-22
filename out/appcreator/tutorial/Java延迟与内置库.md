```java
// 本文所有代码块都只用 JDK 内置类，粘到「逻辑」面板即可运行。
// 关键前提：逻辑代码运行在 Swing 的界面线程（EDT）上。
// 在界面线程里 Thread.sleep 会把整个窗口卡住（不刷新、点不动），所以界面里的延时优先用 javax.swing.Timer。
```

## 选哪种延时

| 方式                                      | 所在线程        | 会不会卡界面   | 适合场景                       |
| --------------------------------------- | ----------- | -------- | -------------------------- |
| `javax.swing.Timer`                     | 界面线程        | 不会       | 界面延时、提示自动消失、动画、防抖          |
| `Thread.sleep`                          | 当前线程        | 在界面线程里会卡 | 后台任务里的等待                   |
| `SwingWorker`                           | 后台线程 + 回到界面 | 不会       | 耗时操作，完成后更新界面               |
| `ScheduledExecutorService`              | 线程池         | 不会       | 后台定时任务，更新界面要 `invokeLater` |
| `java.util.Timer`                       | 后台线程        | 不会       | 老式后台定时任务                   |
| `System.currentTimeMillis` / `nanoTime` | 当前线程        | 不会       | 只测时间差，不阻塞                  |

## Thread.sleep：最基础的阻塞延时

```java
// 单位是毫秒；1000 = 1 秒
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // 被别的线程打断时恢复中断标记，是个好习惯
    Thread.currentThread().interrupt();
}
label1.setText("睡了 1 秒");
```

```java
// 只在后台线程里这样用（例如 SwingWorker 里），界面才不会卡
new SwingWorker<String, Void>() {
    protected String doInBackground() throws Exception {
        Thread.sleep(2000);
        return "后台睡了 2 秒";
    }

    protected void done() {
        try {
            label1.setText(get());
        } catch (Exception ex) {
            label1.setText("出错了");
        }
    }
}.execute();
```

## TimeUnit：可读性更好的 sleep

```java
try {
    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(500);   // 0.5 秒
    java.util.concurrent.TimeUnit.SECONDS.sleep(2);          // 2 秒
    java.util.concurrent.TimeUnit.MINUTES.sleep(1);          // 1 分钟
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
label1.setText("等待结束");
```

## 测量耗时：currentTimeMillis / nanoTime

```java
long start = System.currentTimeMillis();
int sum = 0;
for (int i = 0; i < 1000000; i++) {
    sum += i;
}
long cost = System.currentTimeMillis() - start;
label1.setText("耗时 " + cost + " 毫秒，sum = " + sum);
```

```java
long t0 = System.nanoTime();
try {
    Thread.sleep(200);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
double ms = (System.nanoTime() - t0) / 1_000_000.0;
label1.setText("实际睡了 " + String.format("%.1f", ms) + " 毫秒");
```

## javax.swing.Timer：界面延时的首选

```java
Timer once = new Timer(3000, e -> {
    ((Timer) e.getSource()).stop();       // 只执行一次就停
    label1.setText("3 秒到了");
});
once.setRepeats(false);
once.start();
```

```java
Timer tick = new Timer(1000, e -> label1.setText("每秒刷新：" + System.currentTimeMillis()));
tick.start();
// 不用时停掉，避免一直在跑
// tick.stop();
```

```java
// 首次延时 2 秒，之后每 1 秒一次
Timer delayed = new Timer(1000, e -> label1.setText("每秒一次"));
delayed.setInitialDelay(2000);
delayed.start();
```

```java
// 秒表：用数组保存可变的计数（lambda 里只能读外部局部变量）
int[] sec = {0};
Timer clock = new Timer(1000, e -> {
    sec[0]++;
    label1.setText("已运行 " + sec[0] + " 秒");
});
clock.start();

button1.addActionListener(e -> clock.stop());
```

## Timer 做提示自动消失

```java
label1.setText("保存成功");
Timer hide = new Timer(2000, e -> {
    ((Timer) e.getSource()).stop();
    label1.setText("");
});
hide.setRepeats(false);
hide.start();
```

## Timer 依次执行多个步骤

```java
String[] steps = {"连接服务器…", "读取数据…", "处理中…", "完成"};
int[] index = {0};
Timer seq = new Timer(800, e -> {
    label1.setText(steps[index[0]]);
    index[0]++;
    if (index[0] >= steps.length) {
        ((Timer) e.getSource()).stop();
        progressBar1.setValue(100);
    }
});
seq.start();
```

## 输入防抖：停止输入 0.5 秒后才处理

```java
Timer debounce = new Timer(500, e -> {
    ((Timer) e.getSource()).stop();
    label1.setText("最终输入：" + textField1.getText());
});
debounce.setRepeats(false);

textField1.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
    public void insertUpdate(javax.swing.event.DocumentEvent e) {
        debounce.restart();      // 每敲一次就重新计时
    }

    public void removeUpdate(javax.swing.event.DocumentEvent e) {
        debounce.restart();
    }

    public void changedUpdate(javax.swing.event.DocumentEvent e) {
        debounce.restart();
    }
});
```

## Timer 做动画

```java
Timer anim = new Timer(16, e -> {
    int v = progressBar1.getValue();
    if (v >= 100) {
        ((Timer) e.getSource()).stop();
        return;
    }
    progressBar1.setValue(v + 2);
});
anim.start();
```

```java
// 左右来回移动按钮（用数组保存可变坐标）
int[] pos = {button1.getX()};
int[] dir = {1};
Timer move = new Timer(20, e -> {
    int x = pos[0] + dir[0] * 3;
    if (x < 0 || x > 900 - button1.getWidth()) {
        dir[0] = -dir[0];
        x = pos[0] + dir[0] * 3;
    }
    pos[0] = x;
    button1.setLocation(x, button1.getY());
});
move.start();
```

## 取消已安排的延时

```java
Timer cancelable = new Timer(5000, e -> label1.setText("5 秒后才会出现"));
cancelable.setRepeats(false);
cancelable.start();

// 用户反悔了，提前取消
button1.addActionListener(e -> {
    cancelable.stop();
    label1.setText("已取消");
});
```

## SwingUtilities.invokeLater：不是延时，是把任务排到界面线程

```java
SwingUtilities.invokeLater(() -> label1.setText("界面线程执行"));
```

```java
// 常见组合：后台算完，回界面线程更新
new Thread(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    SwingUtilities.invokeLater(() -> label1.setText("后台 1 秒后回来更新界面"));
}).start();
```

## ScheduledExecutorService：后台定时 / 延时

```java
java.util.concurrent.ScheduledExecutorService pool =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

// 延时 2 秒执行一次（更新界面要用 invokeLater）
pool.schedule(() -> SwingUtilities.invokeLater(() -> label1.setText("2 秒后执行")),
        2, java.util.concurrent.TimeUnit.SECONDS);

// 首次 1 秒后开始，之后每 1 秒一次
pool.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(
        () -> label1.setText("定时刷新 " + System.currentTimeMillis())),
        1, 1, java.util.concurrent.TimeUnit.SECONDS);
```

```java
// 用完就关，避免线程一直挂着
java.util.concurrent.ScheduledExecutorService pool =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
java.util.concurrent.ScheduledFuture<?> future = pool.schedule(
        () -> SwingUtilities.invokeLater(() -> label1.setText("5 秒任务")),
        5, java.util.concurrent.TimeUnit.SECONDS);

button1.addActionListener(e -> {
    future.cancel(false);     // 取消还没执行的任务
    pool.shutdown();          // 关闭线程池
    label1.setText("已取消并关闭");
});
```

## java.util.Timer：老式后台定时器

```java
// 注意：它在后台线程执行，改界面必须 invokeLater
java.util.Timer utilTimer = new java.util.Timer();
utilTimer.schedule(new java.util.TimerTask() {
    public void run() {
        SwingUtilities.invokeLater(() -> label1.setText("后台延时任务执行了"));
    }
}, 2000);

// 取消全部任务并结束线程
// utilTimer.cancel();
```

```java
// 周期任务：首次 1 秒后，每 2 秒一次
java.util.Timer repeat = new java.util.Timer();
final int[] times = {0};
repeat.schedule(new java.util.TimerTask() {
    public void run() {
        times[0]++;
        SwingUtilities.invokeLater(() -> label1.setText("已执行 " + times[0] + " 次"));
    }
}, 1000, 2000);
```

## CountDownLatch：等待某个条件，带超时

```java
java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
try {
    boolean arrived = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    label1.setText(arrived ? "等到了" : "等了 5 秒超时");
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

## Object.wait：最底层的等待 / 超时

```java
Object lock = new Object();
synchronized (lock) {
    try {
        lock.wait(1000);     // 最多等 1 秒，可被 notify 提前唤醒
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
label1.setText("等待结束");
```

## LockSupport.parkNanos：不抛异常的等待

```java
java.util.concurrent.locks.LockSupport.parkNanos(1_000_000_000L);   // 约 1 秒
java.util.concurrent.locks.LockSupport.parkUntil(System.currentTimeMillis() + 2000);
label1.setText("等待结束");
```

## 反例：这样写界面会假死

```java
// 错误：在界面线程直接睡 3 秒，窗口会卡住不动，按钮也点不了
// try { Thread.sleep(3000); } catch (InterruptedException e) { }
// label1.setText("3 秒后");

// 正确：交给 Timer，界面照常响应
Timer ok = new Timer(3000, e -> {
    ((Timer) e.getSource()).stop();
    label1.setText("3 秒后");
});
ok.setRepeats(false);
ok.start();
```

## 组合示例：带倒计时的按钮解锁

```java
button1.setEnabled(false);
button1.setText("请等待 5 秒");
int[] left = {5};
Timer countdown = new Timer(1000, e -> {
    left[0]--;
    if (left[0] <= 0) {
        ((Timer) e.getSource()).stop();
        button1.setEnabled(true);
        button1.setText("可以点击了");
        label1.setText("倒计时结束");
    } else {
        button1.setText("请等待 " + left[0] + " 秒");
    }
});
countdown.start();
```

## 组合示例：模拟加载进度（不卡界面）

```java
progressBar1.setStringPainted(true);
progressBar1.setValue(0);
progressBar1.setString("加载中…");

Timer loading = new Timer(50, e -> {
    int v = progressBar1.getValue();
    v += 2;
    progressBar1.setValue(v);
    progressBar1.setString("加载中 " + v + "%");
    if (v >= 100) {
        ((Timer) e.getSource()).stop();
        progressBar1.setString("完成");
        label1.setText("数据加载完成");
    }
});
loading.start();
```

## 界面关闭时收尾

```java
Timer cleanup = new Timer(1000, e -> label1.setText("运行中"));
cleanup.start();

frame.addWindowListener(new WindowAdapter() {
    public void windowClosing(WindowEvent e) {
        cleanup.stop();        // 停掉定时器，避免后台线程/事件继续跑
    }
});
```
