# Jigsaw - 九宫格拼图游戏

涉及知识：自定义ViewGroup、ViewDragHelper、Drawable图形分割

已给出大致开发流程，读者可根据自己的想法实现（做法不唯一）

## 开发流程

### 1、将Drawable分割为九张图片，并生成对应的ImageView

### 2、将ImageView添加到`Jigsaw`布局中，并重写`Jigsaw`的`onLayout`方法

### 3、通过`ViewDragHelper`处理滑动事件

### 4、添加获胜回调接口

### 5、扩展其他功能

## 应用截图

<img src="https://github.com/woolsen/Jigsaw/blob/master/screenshot/1.jpg" width="240" align="middle" />

## 参考教程：

[神奇的 ViewDragHelper，让你轻松定制拥有拖拽能力的 ViewGroup](https://juejin.im/entry/6844903482516832269)
