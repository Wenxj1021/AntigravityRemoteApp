# Antigravity Remote Android App (System WebView)

基于 Android 系统 WebView 的 Google Antigravity Remote 移动端客户端应用，专为 **Android 16 (API 36)** 打造。

---

## 🌟 核心特性与技术亮点

### 1. Android 16 (API 36) 原生适配
* **SDK 配置**：
  * `compileSdk = 36` (Android 16)
  * `targetSdk = 36` (Android 16)
  * `minSdk = 36` (Android 16 专享环境，全面启用最新系统级特性与安全沙箱)
* **Edge-to-Edge 全屏沉浸适配**：
  * 完美适配 Android 15/16 默认强制开启的全局边到边显示，采用 `enableEdgeToEdge()` 与 `WindowInsetsCompat` 动态计算安全边距，智能避让系统状态栏、手势导航栏与摄像头挖孔。

### 2. Google Material Design 3 (Material You) 全面美学重构
* **动态壁纸取色（Dynamic Colors）**：
  * 启用 `DynamicColors.applyToActivitiesIfAvailable(application)`，在 Android 12 ~ 16 上自动吸取用户系统壁纸的 Monet 动态色板，所有组件、按钮、指示器和菜单自适应变色。
* **Material 3 模态底栏（Modal Bottom Sheet）**：
  * 彻底废弃旧式居中弹窗，采用 Google 规范的 M3 `BottomSheetDialog`；
  * 配备 28dp 大圆角、M3 拖拽手柄（Drag Handle）、分层色调容器（Tonal Containers）与波纹反馈（Ripple Effect）。
* **Material 3 线性进度条（LinearProgressIndicator）**：
  * 采用 `LinearProgressIndicator` 替代传统横条，具备平滑动画与主题色轨。
* **M3 响应式定向悬浮按钮（横竖屏专属智能定位，禁用拖拽）**：
  * 采用 14dp 平滑方圆角（Squircle）设计与 `colorSurfaceContainerHigh` 质感表面；
  * **竖屏模式（精准定位于输入框上方）**：吸附于屏幕右侧输入栏上方（距底部 94dp），按钮可见圆形右边缘与发送框右边缘完全精准对齐；
  * **横屏模式（右侧空白区等距居中与高度对齐）**：按钮动态坐落于发送框右侧空白区域的正中心（左距发送框右边缘与右距屏幕边缘完全等距），垂直中心高度与发送框中心线精确对齐；
  * **位置稳定与后台保活**：支持 `singleTask` 与 `alwaysRetainTaskState`，禁用拖拽，切后台或桌面重新进入无缝保持页面状态。
* **全球化多语言自动适配（English / 简体中文 / 繁體中文）**：
  * 根据 Android 系统当前语言自动无缝切换，覆盖所有菜单、对话框、提示信息与通知渠道：
    * **English** (`values/`, `values-en/`) - 默认全局兜底语言；
    * **简体中文** (`values-zh/`, `values-zh-rCN/`)；
    * **繁體中文** (`values-zh-rTW/`, `values-zh-rHK/`)。
* **深浅色模式与 Material You 主题图标适配**：
  * **自适应背景（Day / Night Adaptive）**：浅色模式纯白、深色模式 Google 深邃黑（`#131314`）。
  * **Material You 单色主题图标（Monochrome Themed Icon）**：桌面开启主题图标时自动同色化。

### 3. 下一代纯净构建架构（AGP Built-in Kotlin）
* **原生内置 Kotlin**：采用 Google 最新的 **AGP 9.4.1+ Built-in Kotlin** 机制，告别独立的外部 `org.jetbrains.kotlin.android` 插件，根除扩展名抢占冲突。
* **现代 Gradle 规范**：使用 **Gradle 9.8.0** 与 **Version Catalog (`libs.versions.toml`)** 统一编排依赖。
* **0 警告极速构建**：移除了所有过时的兼容补丁，编译无任何废弃语法或 D8 编译器警告，秒级极速构建。

### 4. 深度调优的系统级 WebView
* **官方访问入口**：锁定访问官方控制台 `https://antigravity.google.com/`，免除手动配置繁琐。
* **原汁原味原生手势体验（0 下拉手势冲突）**：
  * 彻底移除了拦截手势的外部 `SwipeRefreshLayout`，将完整的垂直滑动、长列表惯性滚动与内部 `<div>` 容器手势 100% 交还给 WebView；
  * **顺畅翻阅聊天历史**：手指向下滑动查看上方历史记录、代码块与长对话时，绝不再触发任何误刷新，体验如丝般顺滑。
  * **需要刷新控制台时**：点击悬浮菜单中的「🔄 刷新控制台」即可平滑重载。
* **登录态持久化**：启用 JavaScript、DOM Storage (localStorage/sessionStorage) 与全局 Cookie 管理，完美支持 Google 账号登录鉴权与单页应用。
* **一键退出登录（安全注销）**：
  * 菜单底部提供醒目红色的安全退出入口，联动 `MaterialAlertDialogBuilder` 弹出 M3 风格确认对话框。
  * 触发后系统级全方位清除所有 Cookie、WebStorage (localStorage / sessionStorage / IndexedDB)、WebView 缓存、历史记录与表单数据，安全注销并立即重置回官方纯净登录页面。
* **文件与图片上传**：实现了 `WebChromeClient.onShowFileChooser`，支持在手机上调用系统文件/相册选择器，向 Agent 随时发送附件和截图。
* **离线容错**：网络异常或加载失败时提供 M3 风格的居中卡片重试面板。
* **原生系统通知桥接（Notification Bridge）**：
  * 支持 Android 13 ~ 16 现代 `POST_NOTIFICATIONS` 运行时权限与独立高优先级通知渠道（`Antigravity Remote`）。
  * 注入 HTML5 Notification API Polyfill，网页端触发的任何任务完成、Agent 审批提示或消息推送均自动桥接为手机原生横幅通知。
* **现代手势返回处理**：集成 `OnBackPressedDispatcher`，优先回退网页历史记录，无历史记录时优雅退出应用。

---

## 📁 目录结构

```
AntigravityRemoteApp/
├── app/
│   ├── build.gradle.kts                        # 采用 AGP 内置 Kotlin，纯净单插件声明
│   ├── proguard-rules.pro                      # WebView 与 JSInterface 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml                 # 权限、硬件加速与深色图标声明
│       ├── java/com/antigravity/remote/
│       │   └── MainActivity.kt                 # Edge-to-Edge, M3 Bottom Sheet, 通知桥接与登出控制
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml           # WebView + LinearProgressIndicator + M3 错误卡片 + 智能 FAB
│           │   └── bottom_sheet_menu.xml       # Material 3 模态底栏菜单布局
│           ├── values/                         # M3 Light 完整色板、样式与白天背景色
│           ├── values-night/                   # M3 Dark 完整色板与夜间深色背景色
│           ├── drawable/                       # 矢量图形 (ic_refresh, ic_notifications, ic_logout, ic_wifi_off)
│           ├── mipmap-anydpi-v26/              # 自适应图标与主题单色图标定义
│           └── mipmap-[m|h|xh|xxh|xxxh]dpi/    # 全分辨率图标前景与位图资源
├── gradle/
│   ├── libs.versions.toml                      # 依赖版本编排 (AGP 9.4.1, Gradle 9.8.0)
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties                           # 纯净 AndroidX 与 JVM 优化配置
└── README.md
```

---

## 🚀 构建与运行指南

### 方法 1：使用 Android Studio 打开（推荐）
1. 启动 **Android Studio**（推荐 2024.2+ Ladybug 或更高版本）。
2. 点击 **File -> Open...**，选择本项目目录：
   `C:\Users\wenxj\.gemini\antigravity\scratch\AntigravityRemoteApp`
3. 等待 Gradle 同步完成，连接 Android 16 设备（或模拟器），点击顶部的绿色小三角 ▶ **Run (Shift + F10)** 即可安装。

### 方法 2：使用命令行构建 APK
在配置有 JDK 17+ 和 Android SDK 的终端中运行：
```powershell
.\gradlew assembleDebug
```
生成的安装包（APK）位于：
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ 常见调试技巧
* **手机 USB 连接不弹授权框**：若 Windows 自动加载了陈旧的 Google USB 驱动，可在 Windows 设备管理器中找到手机设备，右键选择“卸载设备”并勾选“删除驱动程序”，拔插数据线即可恢复系统正常 ADB 授权弹窗。
* **无线调试掉线**：Android 系统的无线调试在息屏或重连 Wi-Fi 后端口号会随机变动，若无法连接，可在 Android Studio 顶部设备栏中点击 **Pair Devices Using Wi-Fi** 重新扫码秒连。
