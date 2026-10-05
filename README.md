# Antigravity Remote for Android

适用于 [Google Antigravity Remote](https://antigravity.google.com/) 的轻量级 Android 客户端，基于系统 WebView 构建，原生适配 **Android 16 (API 36)** 与 **Material Design 3**。

---

## ✨ 特性

- **Android 16 原生适配**：针对 API 36 全面启用 Edge-to-Edge 全屏沉浸显示，动态适配系统状态栏与手势导航栏。
- **Material Design 3 (Material You)**：
  - 动态壁纸取色（Dynamic Colors / Monet）；
  - 规范级 M3 模态底栏菜单（Bottom Sheet）与线性进度条；
  - 深色模式（深邃灰黑 `#131314`）与桌面单色主题图标。
- **固定位置悬浮菜单按钮**：
  - **竖屏模式**：固定停靠于右侧输入框上方（marginEnd 12dp，bottomMargin 94dp），避免遮挡输入与交互；
  - **横屏模式**：固定停靠于右下角安全区域（marginEnd 16dp，bottomMargin 20dp），无多余动态 DOM 检测与位移，体验更纯粹稳定。
- **流畅的原生手势体验**：无额外下拉拦截，顺畅翻阅长对话记录与代码块。
- **系统通知桥接**：网页端任务与 Agent 提醒自动桥接为 Android 原生系统通知。
- **多语言支持**：根据系统语言自动适配 English、简体中文及繁體中文。
- **本机 WebView 诊断与信息查看**：一键查看本机 WebView 提供程序（如 Android System WebView / Chrome）、版本号、构建代码、Chromium 内核版本、多进程沙箱模式、安全浏览与注入特性支持，以及完整 User-Agent 与系统环境，并支持一键复制诊断信息。
- **登录态与安全管理**：支持 Google 账号登录态持久化，提供一键清除所有缓存数据的安全注销功能。

---

## 🛠️ 构建与安装

### 使用 Android Studio
1. 使用 **Android Studio**（推荐 2024.2+）打开本项目。
2. 连接 Android 16 设备或模拟器，点击 **Run** 即可安装。

### 使用命令行
```bash
# 构建 Debug APK
./gradlew assembleDebug
```
产物路径：`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 规范与依赖

- **Min SDK**: 36 (Android 16)
- **Target / Compile SDK**: 36
- **Gradle**: 9.8.0
- **AGP**: 9.4.1 (Built-in Kotlin)
- **JDK**: 17+
