# 《仕途》音频资源

本目录存放游戏的全部本地音频。全部为自制的占位音频（柔和钢琴 / 弦乐铺底 / 极轻低频，
66~76 BPM，无鼓点、无刺耳高频），风格为「现代影视配乐 + 轻东方氛围」，
不使用任何版权音乐。可用 `tools/audio/GenerateAudio.java` 重新生成。

## 目录

```
audio/bgm/   背景音乐（循环）
              menu home governance event major politics crisis promotion ending
audio/ui/    界面音效（按钮、翻页、返回、关闭、确认）
audio/sfx/   事件与里程碑音效（晋升、成就、结局、警告等）
```

## 替换成正式音乐

* 保持**文件名不变**，直接替换同名文件即可，代码无需修改；
* 查找优先级：`ogg` → `mp3` → `m4a` → `aac` → `wav`；
  例如放入 `bgm/home.ogg` 后，可以删掉 `bgm/home.wav`；
* BGM 建议 60~90 秒、编曲首尾可无缝衔接（循环点无爆音、无停顿）；
* 音量已在设置中默认压到 30%（音乐）/ 45%（音效），资源本身不需要额外衰减。

## 文件名清单

| 场景 | 文件 |
| --- | --- |
| 主菜单 | `bgm/menu` |
| 首页 | `bgm/home` |
| 日常政务 | `bgm/governance` |
| 普通事件 | `bgm/event` |
| 重大事件 / 重大决策 | `bgm/major` |
| 政治与人事 | `bgm/politics` |
| 危机 | `bgm/crisis` |
| 晋升（一次性） | `bgm/promotion` |
| 结局 | `bgm/ending` |

| 音效 | 文件 |
| --- | --- |
| 点击 | `ui/click` |
| 翻页 | `ui/page` |
| 返回 | `ui/back` |
| 关闭 | `ui/close` |
| 确认 | `ui/confirm` |
| 开关 | `ui/toggle` |
| 提醒 | `sfx/warning` |
| 成功 | `sfx/success` |
| 失败 | `sfx/failure` |
| 重大事件 | `sfx/event_major` |
| 晋升 | `sfx/promotion` |
| 成就 | `sfx/achievement` |
| 结局 | `sfx/ending` |