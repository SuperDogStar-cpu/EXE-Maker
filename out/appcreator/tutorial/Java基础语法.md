```java
// 每条代码块都是独立的，整段粘到「逻辑」面板即可运行。
// 变量名可自由修改；控件变量（如 label1、button1）来自画布。
// 命名规则：字母 / 下划线 / $ 开头，不能以数字开头，不能是关键字；区分大小写。
```

## 创建变量

```java
int count = 0;
int age = 18;
double price = 9.9;
boolean ok = true;
char grade = 'A';
String name = "豆包";
String empty = "";

int a = 1, b = 2, c = 3;          // 一行声明多个
String userCount2 = "两个";        // 不能写成 2user
final int MAX_COUNT = 5;           // 常量：全大写，之后不能再改
```

## 修改变量

```java
int score = 10;
score = 20;        // 重新赋值
score += 5;        // 加
score -= 2;        // 减
score *= 2;        // 乘
score /= 3;        // 除
score %= 7;        // 取余
score++;           // 自增 1
score--;           // 自减 1

String s = "你好";
s = s + "，世界";
s += "！";
```

```java
// 交换两个变量的值
int x = 3;
int y = 8;
int temp = x;
x = y;
y = temp;
```

## 类型与转换

```java
int i = 10;
double d = i;                 // 小转大：自动
double pi = 3.14;
int cut = (int) pi;           // 大转小：强制，直接截断小数
long big = 1000L;
float f = 1.5f;

String numText = "123";
int parsed = Integer.parseInt(numText);
double parsedD = Double.parseDouble("3.5");
boolean parsedB = Boolean.parseBoolean("true");

String back = String.valueOf(cut);
String backD = String.valueOf(pi);
```

## 字符串

```java
String s = "  Hello Java  ";
String t = s.trim();                      // 去首尾空格
boolean has = t.contains("Java");
boolean starts = t.startsWith("He");
boolean ends = t.endsWith("va");
int len = t.length();
char first = t.charAt(0);
String sub = t.substring(0, 5);
String up = t.toUpperCase();
String low = t.toLowerCase();
String replaced = t.replace("Java", "Swing");
String joined = String.join("-", "a", "b", "c");
String[] parts = "a,b,c".split(",");
boolean same = t.equals("Hello Java");    // 比较内容必须用 equals
boolean ignoreCase = t.equalsIgnoreCase("hello java");
int compare = "a".compareTo("b");         // 负数表示 a 在 b 前
```

## 条件判断

```java
int score = 85;
if (score >= 90) {
    label1.setText("优秀");
} else if (score >= 60) {
    label1.setText("及格");
} else {
    label1.setText("不及格");
}

String level = score >= 90 ? "A" : (score >= 60 ? "B" : "C");
boolean pass = score >= 60 && score <= 100;
boolean fail = score < 60 || score > 100;
boolean notAdmin = !"admin".equals("guest");
```

## switch 分支

```java
int day = 3;
switch (day) {
    case 1:
        label1.setText("周一");
        break;
    case 2:
        label1.setText("周二");
        break;
    case 3:
        label1.setText("周三");
        break;
    default:
        label1.setText("其他");
        break;
}
```

```java
String cmd = "start";
switch (cmd) {
    case "start":
        label1.setText("开始");
        break;
    case "stop":
        label1.setText("停止");
        break;
    default:
        break;
}
```

## for 循环

```java
for (int i = 0; i < 3; i++) {
    textArea1.append("第 " + (i + 1) + " 行\n");
}

int sum = 0;
for (int i = 1; i <= 100; i++) {
    sum += i;
}
label1.setText("1~100 求和 = " + sum);

for (int i = 10; i > 0; i -= 2) {
    textArea1.append("倒序 " + i + "\n");
}
```

## while / do-while

```java
int n = 100;
while (n > 90) {
    n -= 5;
}
label1.setText("n = " + n);

int k = 0;
do {
    k++;
} while (k < 3);
label1.setText("k = " + k);
```

## break 与 continue

```java
for (int i = 1; i <= 10; i++) {
    if (i % 2 == 0) {
        continue;        // 跳过偶数
    }
    if (i > 7) {
        break;           // 超过 7 就结束循环
    }
    textArea1.append(i + "\n");
}
```

## 数组

```java
int[] nums = {3, 8, 1, 6};
int first = nums[0];
nums[1] = 20;

int total = 0;
for (int v : nums) {
    total += v;
}
label1.setText("总和 = " + total + "，长度 = " + nums.length);

String[] names = new String[3];
names[0] = "甲";
names[1] = "乙";
names[2] = "丙";
label1.setText(names[0] + "、" + names[1] + "、" + names[2]);
```

## 集合 ArrayList / HashMap

```java
java.util.ArrayList<String> list = new java.util.ArrayList<String>();
list.add("苹果");
list.add("香蕉");
list.add("橙子");
list.remove("苹果");

int size = list.size();
String item = list.get(0);
boolean empty = list.isEmpty();

for (String s : list) {
    textArea1.append(s + "\n");
}
label1.setText("还剩 " + size + " 项，第一个是 " + item);
```

```java
java.util.HashMap<String, Integer> scores = new java.util.HashMap<String, Integer>();
scores.put("语文", 90);
scores.put("数学", 85);
scores.put("英语", 78);

int chinese = scores.get("语文");
boolean hasMath = scores.containsKey("数学");
scores.remove("英语");
int total = chinese + scores.get("数学");
label1.setText("总分 = " + total + "，科目数 = " + scores.size());
```

## 变量的作用域

```java
int total = 0;
{
    int temp = 5;
    total += temp;
    // temp 只在这个大括号里有效
}
// 这里写 temp 会编译报错
for (int i = 0; i < 3; i++) {
    int square = i * i;      // i、square 只在循环体内有效
    total += square;
}
label1.setText("total = " + total);
```

## 局部方法与局部类

```java
class Helper {
    int twice(int x) {
        return x * 2;
    }

    String greet(String who) {
        return "你好，" + who;
    }
}

Helper helper = new Helper();
label1.setText(helper.greet("小明") + "，" + helper.twice(21));
```

## 修改控件变量

```java
label1.setText("改文本");
label1.setVisible(false);
button1.setEnabled(false);
button1.setText("不可点");
textField1.setText("默认值");
textArea1.setText("");
progressBar1.setValue(80);
slider1.setValue(20);
comboBox1.addItem("新选项");
checkBox1.setSelected(true);
```

## 累加器示例：统计输入

```java
button1.addActionListener(e -> {
    String text = textField1.getText().trim();
    if (text.isEmpty()) {
        label1.setText("请输入内容");
        return;
    }
    int lines = textArea1.getLineCount();
    textArea1.append(text + "\n");
    textArea1.setCaretPosition(textArea1.getDocument().getLength());
    textField1.setText("");
    label1.setText("已录入 " + lines + " 条");
});
```
