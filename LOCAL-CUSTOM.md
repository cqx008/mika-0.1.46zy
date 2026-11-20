# CUSTOM-FEATURES.md — 本仓库特性清单（自有实现说明）

> 位置：仓库根。
> 用途：记录本仓库在官方上游（wangzan101/Mika）基础上的自有实现清单，供维护者核对。
> 维护人：仓库维护者。最近更新：2026-11-18。

## 改动 0：商汤（免费）预设 + 官方模型入口撤 + 注册昵称/头像 + 引导注册商汤（自有实现，2026-11-18，版本 0.1.45）

商汤 SenseNova 免费档（sensenova-6.8-flash-lite）默认不发思考字段（免费档限流）：Mika 的 OpenAI 兼容适配器只要带了思考档位就拼 thinking/reasoning_effort 字段，
带思考参数会撞 429（参考之前智谱 GLM 免费档那套 noThinkingByDefault）。
用户要「选一个预设只填 Key 就能用」，所以本仓库加了预设 + 写死模型 + 三用途默认全填 + 默认关思考。
官方（agnes）模型入口（apihub.agnes-ai.com / agnes-3.0-flash）全撤：后端不再下发官方 key/模型/三用途/备援/思考档位，
App 不再连官方服务商，账号只存昵称/头像 + 商汤引导；注册成功后引导注册商汤（跳浏览器 www.sensenova.cn）。

| # | 文件 | 指纹（grep 用） |
|---|---|---|
| 1 | app/src/main/java/com/wangzan101/mika/settings/ProviderPresets.kt | `ProviderPreset("商汤（免费）"`、`defaultModel = "sensenova-6.8-flash-lite"`、`noThinkingByDefault = true`、`familyTokens = listOf("sensenova")` |
| 2 | app/src/main/java/com/wangzan101/mika/settings/AllowanceRoute.kt | `"商汤（免费）" -> AllowanceRoute` |
| 3 | app/src/main/java/com/wangzan101/mika/ui/ModelSetupViewModel.kt | `f.defaultModel`（testAndSave 走预设默认模型分支：不列模型不试调用，三用途全填）、`noThinkingByDefault`、`connectMikaProvider`（建商汤预设、不再拉官方配置）、`mikaRegister(email, password, nick, avatar)` |
| 4 | app/src/main/java/com/wangzan101/mika/settings/ProviderStore.kt | `fun setReasoningEffortAfterSave`、`noThinkingByDefault = p.baseUrl.contains("token.sensenova.cn")`、`fun connectMika(apiKey, baseUrl, model, models)` |
| 5 | app/src/test/java/com/wangzan101/mika/settings/ProviderPresetsTest.kt | `preset("商汤（免费）")`、`sensenova-6.8-flash-lite` |
| 6 | app/src/main/java/com/wangzan101/mika/account/MikaAccount.kt | `val nick`、`val avatar`、`val sensenovaRegisterUrl`、`val sensenovaKey`（Session 字段）、`fun saveProfile`、`fun markSensenovaUsed` |
| 7 | app/src/main/java/com/wangzan101/mika/ui/MikaAccountPanel.kt | `MIKA_AVATAR_PRESETS`（8 个预设头像）、`AvatarPicker`、`MikaAccountForm(nick, avatar)`、`openSensenovaRegister` |
| 8 | app/src/main/java/com/wangzan101/mika/ui/ModelsScreen.kt | `MikaAccountConnection`（商汤 key 行 + 「去注册商汤」引导）、`ModelsCopy.SENSENOVA_KEY`、`ModelsCopy.ACCOUNT_NICK` |

行为约定（与上游的 OpenAiCompatModel 不冲突，不改 core）：
- 选「商汤（免费）」预设：界面只露出 Key 一栏（apiBase 预配 token.sensenova.cn/v1、模型写死 sensenova-6.8-flash-lite、思考默认不发），填 Key 存即主模型，看图/轻量也默认填同一个模型。
- 思考档位：预设没列 reasoningLevels（不猜）；主/看图/轻量三个用途默认设 none（请求不拼思考字段 → 不 429）；用户在界面上显式选档仍会照发（能力保留，不硬删）。
- 官方（agnes）模型入口全撤：App 不再拉后端官方 key/模型/三用途/备援/思考档位，不再连官方服务商；账号只存昵称/头像 + 商汤引导；注册成功后引导注册商汤（跳浏览器 www.sensenova.cn）。
- 注册页多昵称（可选）+ 头像选择（预设 8 个，和后端 /api/avatar/preset/<id> 对应）；后端 /api/register 收 nick/avatar，/api/profile 改，/api/avatar/upload 传。
- 官方上游更新若把预设/默认模型字段/商汤引导刷掉，照本表指纹逐项恢复；OpenAiCompatModel 的 reasoningEffort 逻辑保持上游原样（null 不发、显式选了才发）。
- 智谱 GLM（免费）预设已删（不要了）；「商汤（免费）」顶替它的位置。


## 恢复流程（官方更新后 30 秒自检）

```bash
# 在仓库根执行（国内源 + 文档基础包全量自检）
sh tools/sandbox/check-local-cn-mirror.sh
```

## 改动 1：tools/sandbox/build-runtime.py（核心，唯一被改动的源码文件）

指纹 + 行号（以 2026-11-10 的 729 行版本为准，行号漂移时用 grep 定位）：

| # | 改动 | 指纹（grep 用） | 行号 |
|---|---|---|---|
| 1 | 三个源环境变量 | `ALPINE_CDN`（默认 dl-cdn.alpinelinux.org）、`ALPINE_MIRROR_CN`（阿里 mirrors.aliyun.com/alpine）、`ALPINE_MIRROR_BACKUP`（清华 mirrors.tuna.tsinghua.edu.cn/alpine） | 44-46 |
| 2 | 代理环境变量 | `PROXY = os.environ.get("HTTP_PROXY", ...)`；`_opener` 走 ProxyHandler | 47, 55-57 |
| 3 | 浏览器 UA（躲国内镜像 403） | `USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"` | 48, 54 |
| 4 | 官方 404 自动 fallback | `fetch()` 内 `attempts = [url, MIRROR_CN, MIRROR_BACKUP]`，`if "404" in str(e): continue` | 184-223 |
| 5 | /etc/apk/repositories 三层 | 官方 main/community → MIRROR_CN → MIRROR_BACKUP（apk 按顺序取第一个成功的源） | 620-627 |
| 6 | pip.conf → 清华 pypi | `index-url = https://pypi.tuna.tsinghua.edu.cn/simple` + trusted-host | 629-634 |
| 7 | npmrc → npmmirror | `registry=https://registry.npmmirror.com`（/etc/npmrc + /root/.npmrc） | 636-637 |
| 8 | 99-proxy.sh 烘焙代理 + NO_PROXY | `export HTTP_PROXY="{PROXY}"`；NO_PROXY 白名单 `mirrors.aliyun.com,registry.npmmirror.com,registry.npm.taobao.org,pypi.tuna.tsinghua.edu.cn,pypi.mirrors.ustc.edu.cn,*.cn,localhost,127.0.0.1`；`gen("etc/profile.d/99-proxy.sh", ..., 0o755)` | 641-650 |
| 9 | openssl.exe 候选 | `C:\Program Files\Git\usr\bin\openssl.exe` 等 4 条 Windows 路径（备注③） | 258-273 |

## 改动 1.5：内置文档功能基础包（本仓库扩加，烘焙进 rootfs，勿被收窄）

`WANT`（apk 层）与 `PIP_PACKAGES`（pip 层）里除运行时基础集外，还预烘焙了「文档 + 日常」基础集
——装 APK 后**不联网即可用**（对应 core 的 `skills/documents` PDF 技能，读 PDF 取字/拆合/看元数据）。

官方基准对照（`core/.../sandbox/SandboxPackages.kt` L9）：官方只认
`["python3", "nodejs", "py3-pillow", "py3-pypdf", "tzdata"]` 五个包；
下列包都是**本仓库扩加的**，若官方更新把 WANT/PIP_PACKAGES 收窄回基准集，这些能力就丢了。

| 层 | 包 | 用途 | 指纹（build-runtime.py） | 行号 |
|---|---|---|---|---|
| apk | poppler-utils | PDF→图片/文字（pdftoppm 等） | `"poppler-utils"` | 73 |
| apk | font-wqy-zenhei | CJK 字体（reportlab 中文输出需 TTF） | `"font-wqy-zenhei"` | 73 |
| apk | fontconfig | 字体配置 | `"fontconfig"` | 73 |
| apk | curl/unzip/zip | 日常抓取/解压 | `"curl", "unzip", "zip"` | 74 |
| pip | reportlab | PDF 生成 | `"reportlab"` | 79 |
| pip | fpdf2 | PDF 生成（轻量） | `"fpdf2"` | 79 |
| pip | python-docx | docx 读写 | `"python-docx"` | 79 |
| pip | openpyxl | xlsx 读写 | `"openpyxl"` | 79 |
| pip | xlsxwriter | xlsx 写 | `"xlsxwriter"` | 79 |
| pip | requests / httpx | 日常抓取 | L80 | 80 |
| pip | beautifulsoup4 / lxml | HTML 解析 | L80 | 80 |
| pip | cryptography | 加密/TLS | L80 | 80 |

配套资产（本仓库维护，官方更新也可能被覆盖）：
- `core/src/main/resources/skills/documents/`（SKILL.md + scripts/extract_pdf.py + manifest.json）——PDF 读字技能
- `core/src/main/resources/prompts/capabilities/sandbox.md`——提示词里提到的文档能力
- `app/src/main/java/.../ui/viewer/FileViewer*`（PDF/Office 预览 UI）

**恢复方法**：官方更新后 `grep -n "poppler-utils\|reportlab" tools/sandbox/build-runtime.py`，
缺失就照上表加回 WANT（L70-74）与 PIP_PACKAGES（L78-81），再重跑 `build-runtime.py` 重新烘焙
rootfs + packages.json（`BundledRuntimeTest` 会核对 WANT 与 core 的 SANDBOX_PACKAGES 一致性）。

## 改动 2：tools/sandbox/runtime/sources.lock（版本重解析，非手工编辑）

官方 CDN 把 python3 3.12.15 轮下了（404），`--resolve` 重解析到 3.12.16-r0，并手工补了 3 个 python 系包的 APKINDEX checksum + SHA-256：

```
apk main python3 3.12.16-r0 Q1hMNCLt9bBmM3XedZcyiZLtHF9JQ= d21dd05b578d14c84a735d66d8623e9454bdc18a1b3841815b37948ce3e2aa92
apk main python3-pyc 3.12.16-r0 Q1ELX1tcK17irbEcKpzSOJXFSlSa8= cdd845907cd99484cc392d44277dc3b08b477406b90036d44b8729f3dca7dc98
apk main python3-pycache-pyc0 3.12.16-r0 Q1oOpNb2ZOInuGksJRbzfWrQT/X2o= 024147092032dcbbbefd75d22d11a2b8b230627ee34952185fcd113b4a20d42a
```

> 官方更新若带着旧 lock（或它自己重新解析的版本），用这三行覆盖回去；跑 `build-runtime.py --check` 验证与产物一致。

## 改动 3：生成产物（烘焙物，不是源码，官方更新不会带，但必须重生成）

| 产物 | 生成命令 | 仓库值 |
|---|---|---|
| app/src/main/assets/runtime/rootfs.tar.xz | `python3 tools/sandbox/build-runtime.py`（带 ALPINE_MIRROR_CN/BACKUP + PROXY=http://<你的代理地址>:10000） | 当前 50.9M（⚠️ 与说明的 31.46M 不符，未定哪份为准） |
| app/src/main/assets/runtime/packages.json | 同上 | 开源许可页数据源 |
| app/src/main/java/.../cap/sandbox/BundledRuntime.kt | 同上（版本/SHA-256/大小指纹） | `BundledRuntimeTest` 会核对 WANT 与 core 的 SANDBOX_PACKAGES |

**官方更新后若 rootfs.tar.xz 被官方新产物覆盖**：按上表命令在本仓库重跑一次即可（源路由策略会保留：国内镜像直连，官方 CDN 兜底走自建代理，GitHub 永远走 <你的代理地址>:10000）。

## 改动 4：app/build.gradle.kts（服务器侧一次性修改，若同步回 Windows 需注意）

```
val keystore = File("<你的 keystore 路径>/mika-release-key.jks")
```

> 这是构建环境适配，不是功能改动；keystore 文件本身不入仓库，构建时由构建方在本地指定。

## 备注

① 源路由策略（最终版，改动 1 的行为契约）：
- alpine 包 / minirootfs：阿里直连 → 清华备 → 官方 dl-cdn 兜底（经自建代理）
- pip：清华 pypi；npm：npmmirror —— 均直连（NO_PROXY 白名单保证不绕自建代理）
- GitHub（更新 / APK / GPL 源码包）：无国内镜像，永远走自建代理 <你的代理地址>:10000

② 官方更新后的完整自检顺序：`git diff` 看 build-runtime.py 是否被改 → 跑 `tools/sandbox/check-local-cn-mirror.sh`
（含国内源 + 文档基础包全量指纹）→ `build-runtime.py --check` → `:app:assembleRelease` → apksigner 验证指纹。

③ 改动 1 第 9 项（openssl.exe）里含两条**本机专属** Windows 路径（`E:\开发软件\kaifa\Git\...`），只在本机构建环境有效；若要提交进仓库，删掉这两条，保留 `C:\Program Files\Git\...` 两条通用路径 + PATH 探测。

④ 签名：keystore 路径 `mika-release-key.jks`（别名 mika，密码见本仓库构建文档；构建侧自行保管，**不入仓库**）。本仓库证书指纹与官方作者签名不同，不能覆盖官方版，须先卸载再装。构建方自行记录本证书指纹；交付前确认。
