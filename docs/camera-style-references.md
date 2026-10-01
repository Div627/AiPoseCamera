# 相机 / 胶片风格研究与实现

2026-10-01，0.1.0-beta.4。以下为品牌官方公开特征的自编近似，不包含厂商私有 LUT、商标图形、样片或固件代码。未取得同场景原厂 JPEG 对照，不能宣称一比一复刻；传感器、曝光、白平衡和厂商 ISP 仍会影响差异。

| 系列 | 本次风格 | 公开描述与实现方向 |
| --- | --- | --- |
| 富士 | PROVIA、Velvia、ASTIA、Classic Chrome、Classic Negative、ACROS | 均衡色彩 / 风景高饱和 / 柔和肤色 / 低饱和纪实 / 复古层次 / 丰富黑白灰阶 |
| 柯达 | Portra 400、Gold 200、Ektar 100 | 自然肤色柔和高光 / 暖调鲜明 / 高饱和清晰色彩 |
| 理光 | Positive Film、Negative Film、GR 高反差 | 浓郁街拍 / 柔和怀旧 / 强反差黑白 |
| 徕卡 | Natural、Vivid | 自然层次 / 鲜活色彩 |
| Agfa | APX 100 | 细腻层次黑白 |

## 官方资料

- [Fujifilm Film Simulation](https://www.fujifilm-x.com/en-gb/learning-centre/get-to-grips-with-film-simulation-modes/)
- [Fujifilm Classic Chrome](https://www.fujifilm-x.com/en-ie/stories/film-simulation-classic-chrome/)
- [Kodak Portra 400 技术资料](https://kodakprofessional.com/sites/default/files/2025-07/e4050.pdf)
- [Kodak Gold 200](https://www.kodak.com/en/still-film/product/consumer/gold-200-film/)
- [Kodak Professional 胶片资料](https://www.kodakprofessional.com/sites/default/files/wysiwyg/film/KODAKPROFESSIONAL_Film_Brochure2018.pdf)
- [Ricoh GR III Image Control](https://www.ricoh-imaging.co.jp/english/products/gr-3/feature/03.html)
- [Ricoh Negative Film](https://www.grblog.jp/en/article/7151/)
- [Leica Looks](https://leica-camera.com/en-US/photography/leica-looks)
- [AgfaPhoto 模拟摄影](https://agfaphoto.com/en/analog-photography-single-use-cameras/)

## 实现与边界

`FilmProfiles` 定义分段色调曲线、饱和度、绿/蓝选择性色彩、阴影/高光偏色与肤色保护。`StyleLut` 生成 17³ 色彩立方体，CPU 成片/缩略图与 Android 13+ AGSL 取景共用同一数据和三线性插值。取景流和 JPEG 的相机处理可能不同，算法一致不意味着设备输出逐像素一致。老系统不承诺该实时效果；当前只针对小米 17。AGSL 使用方式参考 [Android 官方说明](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl)。

缩略图使用实时画面，不使用其他相机品牌样片。风格仅改变颜色，不缩小照片、不做插值放大、不加模糊或颗粒；先保留原片，再保存同尺寸 JPEG 98 副本。默认自然自动推荐仍保留；手选风格关闭自动推荐，避免自动覆盖。

本轮验证涵盖原色及透明度保持、强度归零、黑白去色/灰阶单调、风格区分。未连接真机，尚未验证 GPU 实际预览、肤色表现、拍照耗时及同场景原厂对比。后续用同一场景固定白平衡和曝光的原片/原厂样片校准，才能评价还原程度。

## 人物与诊断

姿态阈值调整为 0.5，关节点可见性 0.45；越界点仍拒绝。检测到人体但关节点不全，或仅检测到近景人脸，均计入人物提示；自动构图/自动快门仍要求有效身体关节点，缺失时只允许手动拍摄。Pose 未检出时追加本地 BlazeFace short-range 检测，模型来自 [Google 官方模型](https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite)，不增加云端识别。

日志入口：相机顶部辅助面板 → 导出诊断日志；扫一扫右上角更多 → 导出诊断日志。保留两份各约 256 KiB 的轮换日志，包含版本、机型、模型状态、帧尺寸、识别人数/耗时、错误类与栈。排除图片、二维码链接、模型回复、异常正文和 Key；不会自动发送。复现后手动导出 ZIP 即可排查。

Release 的混淆映射和对应 APK SHA256 保存在本地忽略目录 `.local-diagnostics/0.1.0-beta.4/`，可还原导出日志中的栈；该目录不在下载服务白名单内。
