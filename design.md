> 2.0.0 实现更新：名称“映刻相机”；启动进入人像模式，默认不自动拍照。快门左风格、右 AI 构图，顶部品牌按钮打开辅助设置。人像默认小照片参考卡加动作提示，轮廓改为可选。风景圈选后本地平移跟踪，彩环对齐中央准星后安全放大，支持撤销；失去可信匹配需重新圈选。加载用稀疏光点与柔和彩环，随真实分析反馈退出并设超时。以下 1.9 规范与 HTML 参考保留为历史，不是 2.0 原生截图。

# 姿映相机 · 界面设计规范 v2 / 1.9.0

本规范先于本轮界面实现制定。目标：取景优先、手动模式选择、少量清晰动作、统一底部面板。保留姿映相机名称、粉色相机图标及 Android 平台行为。借鉴 Apple HIG 的信息层级与克制，不复制 iOS 专有字体、SF Symbols、Liquid Glass 实现或系统相机外观。

## 依据

2026-10-01 实际读取 Apple 官方 HIG 网页对应的公开文档 JSON（网页正文需 JavaScript）：
- [Layout](https://developer.apple.com/design/human-interface-guidelines/layout)：对齐和分组表达关系，逐步显示次要选项。
- [Typography](https://developer.apple.com/design/human-interface-guidelines/typography)：减少字体种类、保持可读字号与常规/中等字重。
- [Color](https://developer.apple.com/design/human-interface-guidelines/color)：颜色语义一致，不能仅靠颜色传达状态。
- [Materials](https://developer.apple.com/design/human-interface-guidelines/materials)：控件与内容分层，避免过度材料效果。本项目使用深色实底面板与渐变遮罩，不假称系统玻璃。
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)：明确标签、按压反馈、单一主动作；本项目采用 Android 48dp 最小目标。
- [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)：支持文字缩放、对比度与非颜色反馈。

## 可执行 tokens

统一实现入口：`ui/theme/CameraDesign.kt`、`Color.kt`、`Type.kt`、`camera/CameraControls.kt`。

|Token|值|用途|
|---|---|---|
|background|#000000|相机与页面背景|
|surface|#1C1C1E|底部面板、设置分组|
|surfaceRaised|#2C2C2E|选项/次级背景|
|textPrimary|#F5F5F7|正文和图标|
|textSecondary|#B8B8BD|说明文字|
|accent|#FFD60A|选中模式/选项、可执行重点|
|success|#75E6B4|匹配完成，必须伴随文字提示|
|error|#FF8A80|错误，同时说明解决动作|
|scrim|黑色0.65到透明渐变|只用于上下控制区域，禁止整块遮住取景|
|border|白色0.12，1dp|普通边界；选中用accent 2dp|
|spacing|4 / 8 / 12 / 16 / 24dp|同层间距8或12，页面边距20，分组24|
|radius|12dp选项；20dp卡片；28dp面板；圆形图标|不混用任意圆角|
|icon / target|24dp图形 / 48dp触控|使用现有Material图标，单色，含contentDescription|
|shutter|80dp外圈，3dp边框，7dp内距|唯一主要动作，保存时禁用并显示进度|
|type|系统sans；标题22/28sp semibold，分组17/24sp semibold，正文16/24sp regular，标签14/20sp medium，说明13/19sp regular|sp遵循用户字体缩放；不使用细字|
|motion|160ms颜色/选中反馈，底部面板用Material默认过渡|不对取景做动画，不添加持续装饰动画|
|elevation|面板0dp；页面不使用装饰投影|取景线框仅轻暗色偏移以保证亮背景可读|

安全区使用systemBarsPadding/navigationBarsPadding；关键按钮不能进入系统手势区。窄屏用横向滚动选项、纵向滚动面板，不压缩触控区。正文允许换行，不能固定一行裁切关键信息。

## 三模式信息架构

底部固定「照片 / AI人像 / AI风景」，文字加选中状态；冷启动默认照片。点击模式销毁旧相机会话并新建，以key(mode)隔离。切换取消倒计时、待执行自动拍摄、焦距/对焦回调、ROI、打开面板；已开始的原片保存允许完成。人数只能产生提示，不修改模式。

### 照片

仅Preview和ImageCapture，不创建分析器或姿态模型。顶部闪光灯/辅助/设置。底部焦距、模式条、快门与换镜头。辅助底部面板包含延时、网格、水平仪、实际曝光补偿及原生相机入口。无自动姿势、无人场景分析和ROI。

### AI人像

顶部闪光灯/简洁人数状态/设置。取景只展示可隐藏姿势细轮廓、一句可执行提示及必要自动拍进度。没有人时保持人像模式，提示“请让人物进入画面”，允许手动拍。1–4人使用对应姿势；5人以上提示手动。底部工具为姿势/滤镜，辅助归入统一面板，保留环境人像/合照意图。姿势选择为底部两列卡片网格，分类横向选择，照片/轮廓真实对应，选中后关闭。废除右侧高黑色浮层。自动拍只在人像条件满足时执行，不运行无人风景自动拍。

### AI风景

顶部闪光灯/风景模式标识/设置。只保留构图网格、圈选路径及一句提示；不显示人数卡、pose面板或人像轮廓。出现路人也保留风景模式。底部工具为圈选/滤镜，更多面板为场景意图、网格、水平仪、曝光/延时/自动拍等。圈选针对当前画面任意区域，不要求无人或居中；稳定的ROI统计引导全局调色，不声称局部涂色或物体跟踪。放大拍摄是独立明确动作，继续使用安全居中/边界校验。

## 面板与状态

一次只打开一个：TOOLS / POSES / FILTERS。统一底部sheet、28dp上角、20dp内边距、22sp标题、48dp关闭按钮；返回首先关闭当前面板或取消圈选，再退出AI到照片。所有面板打开均暂停自动快门。

滤镜：当前画面预览条、明确选中边界、强度、原色比较；精细调色折叠。手动调色暂停自动推荐。保留原片，另存副本。高光/阴影近似预览与保存曲线差异放在精细调整说明。

状态优先级：相机错误与重试 > 保存/倒计时 > 圈选操作 > 缺少人物/数据未就绪 > 自动拍稳定过程 > 已拍与再拍 > 单条摄影提示。每次只显示一条主提示。匹配绿线必须有“已对齐，保持稳定”文字，不能仅以颜色判断。设置、扫描更新、权限错误使用同一主题与文本层级，说明简洁且包含下一步动作。

## 姿势美学与来源策略

旧版缺陷：整页同质浅色衣服像商品展示、站姿默认偏僵硬、多人等距重复、外轮廓无法说明叠合手臂/坐姿双腿。新默认推荐强调真实日常姿势、重心偏移、手有自然落点、手臂与身体留空、头身比例正常、站坐倚蹲均保留。缩略采用更自然的摄影表现，不把人物整张叠加到取景。轮廓仅主体外缘与必要肢体分离，不能描满服装纹理和家具。

参考动作和构图：[FUJIFILM自然人像](https://www.fujifilm-x.com/en-gb/learning-centre/five-tips-for-better-portraits/)、[FUJIFILM家庭人像](https://www.fujifilm-x.com/en-gb/learning-centre/create-pro-style-family-portraits/)、[Nikon自然人物引导](https://www.nikonusa.com/learn-and-explore/c/tips-and-techniques/the-anatomy-of-a-powerful-image)。教程照片有摄影师版权标识，没有确认可再分发授权，仅学习原则。新打包图为内置imagegen原创，完整提示词/来源/锚点/视觉审查保存于design/pose-guides。不得把网络照片直接当成自有素材。

## 验收

模式路由纯逻辑测试与key生命周期代码审查；照片零分析、AI人像不转风景、AI风景不弹pose/不受路人控制；面板互斥；ROI仅风景；手动滤镜优先；延时取消和原片保存回归；锚点与轮廓同步。构建/单元测试/lint通过。真实App截图须来自设备/模拟器；资源渲染和布局示意必须标注，不能冒称真机验证。
