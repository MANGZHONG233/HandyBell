# HandyBell · 给事钟

Minecraft模组，把命名过的钟变成一块指令面板。纯客户端 Fabric 模组。
A Minecraft mod that turns a named bell into a command palette. Client-side Fabric mod.

---

## 这是什么 / What it is

这是一个建筑向的小构想，没有特殊核心功能。原版的钟除了吓跑村民、村袭时报警，几乎没有用处。
我想给它一个"值得被摆进建筑里"的理由：玩家可围绕一口钟布置一小块**有实际功能**的区域——
为建筑内和户外小角落的装饰提供可供发挥的方向。
纯客户端，基于原版的小功能添加：为钟添加右键面板，加入自定义指令界面。
模组基于记录钟的坐标实现功能，对方块数据世界存档没有修改。**卸载本模组不会导致存档、方块的损坏**。
有和Command Keys的联动，可以关联在那个模组中设定好的指令集。

A small builder-oriented idea, not a feature-heavy tool. The vanilla bell does almost nothing except
scare villagers and warn of raids. This mod gives it a reason to be *placed on purpose*: a bell you
build a small functional corner around — a direction to take for decorating inside a building, or for
a small nook outside your base.
It is a client-side addition on top of vanilla: a right-click panel on a bell, plus a custom command
interface. The mod works purely by remembering coordinates; it never touches block data or the world
save. **Uninstalling it will not damage your world or your blocks.**
It also integrates with Command Keys, so you can bind the macros you have already set up there.

---

## 灵感 / Inspiration

两件事撞在一起。用 Carpet 的假人时，我想到若要构建一个更加完善的幻想世界，最好为这些NPC的突然出现寻找有代入感的理由；
由此我想起《最终幻想 14》的**雇员传唤铃**（Retainer Bell）——一口挂着的铃，点它，你的雇员就来了。
这个模组就是把那个动作搬进 Minecraft。

Two things collided. While using Carpet's fake players I thought that a more convincing fantasy
world needs a plausible reason for these NPCs to appear out of nowhere. That brought back Final
Fantasy XIV's **Retainer Bell** — a bell on a post; you click it, your retainer arrives. This mod
moves that gesture into Minecraft.

---

## 用法速览 / At a glance

把钟改名为 `给事钟`、`給事鐘` 或 `HandyBell`（不分大小写），**手持右键放下**，这口钟就会被模组接管。
Rename a bell to `给事钟`, `給事鐘` or `HandyBell` (case-insensitive), then **right-click it onto a
block while holding it** — from then on the mod owns that bell.

| 操作 / Interaction | 结果 / Result |
|---|---|
| 空手右键 / Right-click empty-handed | 本地响一声并弹出 3×3 面板 / Rings locally and opens the 3×3 panel |
| 潜行右键 / Sneak + right-click | 打开配置页 / Opens the configuration screen |
| 潜行拿染料右键 / Sneak + right-click with a dye | 切换到该颜色组，**不消耗染料** / Switches colour group, dye **not** consumed |

被本模组登记过的钟，不再受「只能从某一面敲」的限制，任何方向都可以互动。
For bells registered by this mod, the side you approach from no longer matters — interaction works
from any direction.

十六种颜色是十六套独立的配置方案。当且仅当某个颜色组只填了一格时，可以勾选「敲响时直接执行」，敲一下就直接执行输入的内容，不弹面板。
Sixteen colours, sixteen independent layouts. If a group has exactly one filled slot you can tick
*execute directly* — ringing the bell simply runs it, no panel.

### 它为什么是"静音"的 / Why it is silent

右键在**客户端就被拦下**，交互包根本不发出去。服务端不知道有人敲过钟，所以村民不会跑回家、
袭击者不会发光、也不冒粒子。声音和摇晃是本地补的，你自己看着一切正常。

The right-click is intercepted **client-side and the interaction packet is never sent**. The server
never learns a bell was rung, so villagers do not run home, raiders do not glow, and no particles
spawn. The sound and swing are played locally so it looks normal to you.

这是有意为之，不是 bug——理由见下文「已知限制」。
This is intentional, not a bug — see *Known limitations* below.

---

## 需求 / Requirements

| | |
|---|---|
| Minecraft | **26.3**（更早的版本渲染 API 不同，不支持 / earlier versions use a different rendering API and are not supported） |
| Fabric Loader | 0.16 或更高 / 0.16 or newer |
| 端 / Side | **纯客户端**，服务端什么都不用装 / **Client only** — nothing to install on a server |
| 可选 / Optional | [Command Keys](https://modrinth.com/mod/commandkeys)：装了可以直接挑现成的指令集 / pick one of your existing macros instead of typing |

## 安装 / Installation

1. 把 `handybell-<version>.jar` 丢进 `.minecraft/mods/` / Drop the jar into `.minecraft/mods/`
2. 启动游戏 / Launch the game

---

## 名字 / The name

**HandyBell** 是把 `handyman` 的 `man` 换成 `bell`——杂工要干的活，交给一口钟。
希望它能成为为玩家包揽杂事的万事钟。

**HandyBell** is `handyman` with `man` swapped for `bell` — the odd jobs a handyman would do, handed
to a bell. The hope is that it becomes the bell that takes care of all your errands.

**给事钟**（jǐ shì zhōng）谐音 **给事中**，中国古代官职名。例如在明代此官职是六科的言官，正七品。
一个不太好笑的谐音+给字双关梗，字面意思也解释的通——杂事都给它。

**给事钟** (*jǐ shì zhōng*) is a homophone of **给事中**, an old Chinese official title — in the Ming
dynasty, a seventh-rank censorial post under the Six Offices. A not-very-funny pun plus a double
reading of 给 ("to give" / "to hand over"); the literal reading works too — hand all the odd jobs
to it.

（`handbell` 手摇铃是顺带的巧合，不是名字的来源。）
(`handbell`, the hand-rung instrument, is a happy accident rather than the source.)

---

## 配置一个槽位 / Configuring a slot

九行，每行两个字段：左边是Command Keys配置文件中你设置好的宏（用「选」按钮挑），也可手动输入一条要发送的文字或指令，右边是自定义按钮上显示的内容。
Nine rows, two fields each: **left** is a macro you have already set up in Command Keys (pick it with
the 选 button), or a line of text or a command typed by hand; **right** is the label shown on the
button.

模组会根据左边头两个字符识别你要发送什么类型的内容 / What gets sent depends on the first characters:

| 写法 / Written | 发送方式 / Sent as |
|---|---|
| `/home` | 命令，去掉前导斜杠 / a command (leading slash stripped) |
| `//wand`、`!!help` | 命令，原样发（WorldEdit 与插件命令）/ a command, verbatim |
| `hello everyone` | 聊天消息 / a chat message |

留空则该按钮不可点。 / Leave a row blank and the button stays disabled.

---

## 颜色分组 / Colour groups

金色钟体 + 一条彩色绑带：钟还是那口铜钟，你在上面绑了条带子，十六色各一条。
A gold bell with one coloured strap: still a brass bell, you just tied a band around it — sixteen
colours, sixteen bands.

颜色有三种写法，指令与命名里都认 / A colour can be written three ways:

| 写法 / Written as | 例子 / Example | 说明 / Notes |
|---|---|---|
| 内部名 / internal name | `pink` | 跨语言稳定，写文档和配置用它 / stable across languages |
| 原版染料序号 / vanilla dye id | `6` | 0–15，顺序同 `DyeColor`，铁砧里打一个字符就够 / short enough for an anvil |
| 当前语言译名 / your language's name | `粉红色` / `Pink` | 游戏里显示什么就认什么 / whatever the game shows you |

序号顺序 / The ids follow vanilla: `0` 白 white, `1` 橙 orange, `2` 品红 magenta, `3` 淡蓝 light blue,
`4` 黄 yellow, `5` 黄绿 lime, `6` 粉红 pink, `7` 灰 gray, `8` 淡灰 light gray, `9` 青 cyan,
`10` 紫 purple, `11` 蓝 blue, `12` 棕 brown, `13` 绿 green, `14` 红 red, `15` 黑 black.

**颜色也可以写进物品名。** 命名成 `HandyBell 6`、`给事钟 粉红色`、`HandyBell pink`，
放下就直接进那个颜色组，不用再拿染料染。空格后面认不出的当普通后缀处理，钟照样登记，
所以 `给事钟 一号` 不受影响。

**You can also put the colour in the item name.** `HandyBell 6`, `给事钟 粉红色`, `HandyBell pink` —
the bell lands directly in that group when placed, no dye needed. Anything after the space that is
not a colour is treated as an ordinary suffix and the bell still registers, so `给事钟 一号` keeps working.

---

## 模组指令：注册钟的另一种方式 / Commands: registering a bell another way

区域保护的服务器常常禁止放置、拆除方块，而原来的登记只发生在"放钟"那一刻，于是模组在那类服务器上没法用。
下面几条指令都是纯客户端的，一个字都不发给服务端：

Region-protected servers often refuse block placement, which used to make the mod unusable there,
since registration only happened when you placed a bell. These commands are client-side and send
nothing to the server:

```
/handybell add                  登记你正看着的那口钟 / register the bell you are looking at
/handybell add 249 104 23       或者直接给坐标 / or give the coordinates directly
/handybell add 249 104 23 pink  坐标 + 颜色组 / coordinates plus a colour group
/handybell add nether 120 64 -30 给别的维度登记 / coordinates in another dimension
/handybell list                 打开名单管理页 / open the list screen
/handybell config pink          直接打开那个颜色组的配置 / edit that group directly
```

维度写在**最前面**、颜色写在**最后面**，位置固定，所以不用靠猜。原版三个维度可以用短名
`ow` / `nether` / `end`；**模组或服务器自定义的维度写完整 id**，这是唯一能到达它们的写法：

The dimension goes **first** and the colour **last**, so no token ever has to be guessed. Vanilla
dimensions have short aliases (`ow`, `nether`, `end`); **any other dimension can be given as a full
id**, which is the only way to reach dimensions added by a mod or a server:

```
/handybell add mypack:mining 100 64 200
```

`~` 用法同原版指令，但**只在你自己所在的维度里有效**——你的位置说明不了别处。
`~` works as in vanilla, but only in the dimension you are standing in — your own position says
nothing about coordinates elsewhere.

坐标在反馈里带括号（`mypack:mining（100, 64, 200）`），带命名空间的 id 就不会和第一个数字粘成一片。
Coordinates are shown in parentheses (`mypack:mining（100, 64, 200）`) so a namespaced id never runs
into the first number.

名单页列出所有登记过的钟，标出那儿还是不是钟（`✔` 是、`✘` 不是、`?` 区块没加载或在别的维度），
可以改色、删除、新增。加钟时有维度选择，坐标默认填你正看着的位置。

The list screen shows every registered bell, marks whether the block there is still a bell
(`✔` yes, `✘` no, `?` chunk not loaded or another dimension), and lets you recolour, delete or add.
Adding has a dimension picker and pre-fills whatever you are looking at.

---

## 已知限制 / Known limitations

**只认登记过的钟。** 方块本身不存物品名，客户端模组读不回来。模组只在你**亲手放下**、
或者用 `/handybell add` 登记时记住坐标。别人放的、或者装模组之前放的钟，都需要一次性登记。

**Only registered bells are recognised.** A bell block does not store its item name, so a
client-side mod cannot read it back. Coordinates are remembered either when *you* place a bell or
when you add it with `/handybell add`. Bells placed by someone else, or before the mod was
installed, need that one-time registration.

**染料不消耗。** 客户端没法可靠地从背包里扣掉一件物品——服务端会立刻纠正回来。
所以染色是一次配置动作，不是代价。

**Dyes are not consumed.** The client cannot reliably remove an item from the inventory; the server
corrects it immediately. Colouring is a configuration action, not a cost.

**使用 Better Block Entities 等方块实体优化模组时需调整配置。** BBE 等方块实体优化模组会把静止的钟烘焙进区块几何体，完全绕过原版的方块实体渲染器，
于是自定义贴图被忽略——症状是"只有敲响时才显示颜色"。需做取舍：要么关闭钟的优化选项，要么无法在外观上显示颜色分组。
关闭优化用BBE举例：在 `config/BBEConfig.json` 里关掉两项：

**Conflicts with block-entity optimisation mods such as Better Block Entities.** BBE and similar mods
bake idle bells into chunk geometry, bypassing the vanilla block-entity renderer entirely, so the
custom texture is ignored — the symptom is a bell that shows its colour *only while ringing*. You
have to choose: turn the bell optimisation off, or accept that the colour group will not show
visually. For BBE, disable two options in `config/BBEConfig.json`:

```json
{ "option": "optimize.bell",  "value": false }
{ "option": "animation.bell", "value": false }
```

**木支架不能染色。** 方块实体只渲染铜钟本体，木架是普通的方块模型。
**The wooden bracket cannot be recoloured.** Only the bronze body goes through the block entity.

**配置跟着客户端走**，存在 `config/handybell.json`，不随存档走。
**Configs are per-client** and live in `config/handybell.json`; they do not travel with a world.

---

## 进阶：让红石信号执行命令 / Advanced: redstone

默认情况下，给事钟**只在你亲手敲它时**执行命令。红石、投掷物、别人路过敲一下，都不会有反应。

By default a HandyBell runs its command **only when you ring it yourself**. A redstone signal, a
projectile, or someone else ringing it does nothing.

原因是钟的"响"是**公共事件**——服务端会广播给附近所有人。如果默认放开，公共据点里一个拉杆就会
把**所有安装本模组且设置了直接执行的人**的命令一起跑掉。假设这样一个场景：恶作剧者几小时前启动的计时器会把剩余 tick 存进区块，
任何人重新加载那块地，它就接着走完。

The reason is that a bell's ring is a *public* event — the server broadcasts it to everyone nearby.
Allowed by default, one lever in a shared base would fire the commands of **everyone who has this
mod installed and has direct-execute ticked**. Picture this: a joker starts a timer hours earlier;
the remaining ticks are stored in the chunk, and the moment anyone loads that chunk it runs to
completion.

如果完全清楚这个代价，可以按颜色组单独打开：
If you understand that trade-off, you can opt in per colour group:

```
/handybell allowRedstone <color> true
```

- **仅限单人存档、且未开放局域网。** 服务器上、或者开了局域网的世界里，一律忽略钟的非玩家触发事件，没有例外。
  **Only works in singleplayer with LAN closed.** On a server, or in a world where LAN has been
  opened, non-player triggers are ignored outright — no exceptions.
- 纯客户端设置：指令在发包前被拦下，所以在任何服务器上都能用，服务端日志不会出现"未知指令"。
  A pure client setting: the command is intercepted before sending, so it works anywhere and leaves
  no "unknown command" in the server log.
- 打开需要过一道警告页，我们希望不会有不清楚自己在做什么的玩家打开此设置。
  Turning it on means passing a warning screen; the hope is that nobody who is unsure what it does
  will turn it on.
- 同样的指令加 `false` 关掉。 / Turn it off again with `false`.

打开之后，当且仅当该颜色分组的钟只有一条命令且勾选了直接执行复选框时，
任何让这口钟响起来的东西都能执行你的命令：红石装置、箭、雪球、附魔风爆的重锤，
或是不小心踩上触发装置压力板的村民，或是别有用心的存档访客留下的延时触发装置。

Once enabled — and only when that colour group has exactly one command *and* direct-execute is
ticked — anything that rings the bell can run your command: redstone contraptions, arrows, snowballs,
a mace with Wind Burst, a villager that happens to step on a pressure plate, or a delayed trigger
left behind by a visitor to your world with other ideas.

---

## 免责与说明 / Disclaimer

**命令以模组使用者的身份发出，照常受服务端权限约束。** 本模组不授予、不绕过、不提升任何权限——
玩家手打 `/gamemode creative` 做不到的事，钟也没办法绕过权限做到。
但为了安全起见，即使在单人游戏中也请不要把危险指令输入钟中并设置为直接触发+红石触发。

**Commands are sent as the player using the mod, and are subject to normal server permissions.**
This mod does not grant, bypass, or elevate any permission — if you cannot run `/gamemode creative`
by hand, the bell cannot get around permissions either. For safety's sake, though, even in
singleplayer please do not put dangerous commands into a bell and set it to direct-execute plus
redstone trigger.

**不为刷屏消息导致的服务器封禁或利用本模组在聊天框中输入传播的任何内容负责**
本模组并不能为这类行为提供无可替代不可或缺的便利，我们同该玩家的键盘制造商一样，并不为该玩家的个人行为分担任何责任。

**I am not responsible for server bans caused by spam, nor for anything a player spreads by typing it
into chat through this mod.** This mod offers no irreplaceable or indispensable convenience for that
kind of behaviour; like the manufacturer of that player's keyboard, I do not share any responsibility
for that player's individual actions.

**这个模组的代码由 AI 按我的需求生成；设计、取舍、贴图、游戏内测试与全部产品决策由我完成。**
但这并不意味着这是一个随随便便的仓库。至少26.3以上的Fabric版本的更新、仓库issue我都会尽力处理。bug我会（和我的ai智能体一起）修。
如果你想判断的是"这东西会不会有人维护"，我想是的，只要我还在游玩minecraft，至少这个模组我自己会一直在用。
本人虽非专业人士，也完全没有代码编程方面的基础，但我会尽力去做的，这是我人生中又一个在完全陌生的领域的尝试，维护它也是一种很有趣的人生体验。
因避免二次分发原版钟的材质而一个像素一个像素的手工重绘了新的钟的材质+16色的绑带，这是这个mod中除了创意本身最人类的部分。
不过AI助手还是建议我把初版测试材质的生成脚本放进仓库，虽然最终版材质已经过完全的重新设计和重绘（初版材质是直接让整个钟被染为16色的废案。那个脚本生成的钟底面甚至没有钟舌）。Okay, whatever—let’s go with it.

**The code in this repository was generated by an AI working from my requirements; the design, the
trade-offs, the textures, the in-game testing and every product decision are mine.**
That does not mean this is a throwaway repository. I will do my best with Fabric updates from 26.3
onward and with issues on the tracker. Bugs I will fix — together with my AI agent.
If what you are really asking is whether this will be maintained: I believe so. As long as I am
still playing Minecraft, I will at the very least keep using this mod myself.
I am not a professional and I have no background in programming at all, but I will do my best. This
is another attempt at a field entirely new to me, and maintaining it has been an interesting
experience in itself.
To avoid redistributing the vanilla bell texture, I redrew the new bell texture and its sixteen
colour bands pixel by pixel. That is the most human part of this mod, apart from the idea itself.
My AI assistant did suggest putting the generator script for the first test textures into the
repository, even though the final textures were completely redesigned and redrawn (that first version
was a scrapped idea where the whole bell was simply dyed one of sixteen colours — the bell that
script produces does not even have a clapper). Okay, whatever — let's go with it.

**本文中文原文除本小节外的部分尤其是技术部分由AI生成，人类润色改动；全文英语翻译完全由AI根据润色后的中文原文生成。**
**模组内除简体中文以外的本地化由AI根据简体中文版本生成，并不一定100%准确。**

**Except for this section, the Chinese text of this README — especially the technical parts — was
generated by AI and then edited by me; the entire English translation was generated by AI from the
edited Chinese.**
**In-game localisations other than Simplified Chinese were generated by AI from the Simplified Chinese
version and are not guaranteed to be 100% accurate.**

---

## 授权 / License

MIT — 见仓库里的 [LICENSE](https://github.com/MANGZHONG233/HandyBell/blob/main/LICENSE) / see LICENSE in the repository.

钟体贴图是**为本模组手绘**的，起点是
[`tools/make_original_textures.py`](https://github.com/MANGZHONG233/HandyBell/blob/main/tools/make_original_textures.py) 生成的底图。
该脚本只读取原版贴图的 **alpha 通道**（轮廓由方块模型的 UV 决定，改不了），
RGB 全部现算——所以脚本产物和最终贴图都不含任何取自 Mojang 资源的像素。
脚本留在仓库里，来源可以自己验证，不必只凭这句话。

The bell textures were **hand-drawn for this mod**, starting from a base produced by
[`tools/make_original_textures.py`](https://github.com/MANGZHONG233/HandyBell/blob/main/tools/make_original_textures.py). That script takes nothing but
the **alpha channel** of the vanilla texture (the silhouette is fixed by the block model's UV
layout) and paints the RGB values procedurally — so neither the script's output nor the final
textures contain any pixel taken from Mojang's assets. The script is in the repository so the
provenance can be checked rather than taken on trust.

Command Keys 使用 Apache-2.0 授权。本模组通过反射调用它的公开 API，不含任何抄自它的代码。
Command Keys is licensed Apache-2.0. This mod calls its public API via reflection and contains no
code copied from it.
