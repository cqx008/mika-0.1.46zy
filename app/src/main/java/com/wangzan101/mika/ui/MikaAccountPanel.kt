package com.wangzan101.mika.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wangzan101.mika.R
import com.wangzan101.mika.account.MikaAccount
import com.wangzan101.mika.i18n.tr
import com.wangzan101.mika.ui.components.ButtonKind
import com.wangzan101.mika.ui.components.ButtonSize
import com.wangzan101.mika.ui.components.IconTile
import com.wangzan101.mika.ui.components.MikaButton
import com.wangzan101.mika.ui.components.TileTone
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType

/**
 * mika 账号面板(0.1.45 起官方模型入口已撤):未登录时给「登录 / 注册」两个标签页,注册页多一个可选的昵称 +
 * 头像选择(预设头像挑一个,上传走后端 /api/avatar/upload,这里先给预设);
 * 复用 [ModelSetupViewModel] 现有的 mikaRegister / mikaLogin(登录/注册成功后自动 connectMikaProvider 建商汤预设,
 * 官方 agnes 模型不再由后端下发);登录中显示进度,失败留在原地说原因、可以重试。
 * 注册成功后引导注册商汤:给个「去注册商汤」按钮 → 跳浏览器 www.sensenova.cn 注册 → 回来填 key。
 * 引导入口在 WelcomeScreen([WelcomeMikaGuide])和 ModelSetupContent([ModelSetupMikaGuide])顶部,
 * 点引导按钮弹 [MikaAccountPanelSheet];已登录时换成「已登录 xxx@xx」只读卡。
 */
object MikaAccountPanel {
    val TITLE: String get() = tr(R.string.mika_account_title)
    /** 引导卡(未登录):一句说明注册 mika 账号 + 填商汤 key 解锁商汤免费模型。 */
    val GUIDE: String get() = tr(R.string.mika_guide_title)
    val GUIDE_NOTE: String get() = tr(R.string.mika_guide_note)
    /** 引导卡(未登录)的按钮:点进注册页。 */
    val GUIDE_BUTTON: String get() = tr(R.string.mika_guide_button)
    /** 已登录:「已登录 xxx@xx」。 */
    fun signedIn(email: String): String = tr(R.string.mika_guide_signed_in, email)

    val EMAIL: String get() = tr(R.string.mika_account_email)
    val PASSWORD: String get() = tr(R.string.mika_account_password)
    val EMAIL_PH: String get() = tr(R.string.mika_account_email_ph)
    val PASSWORD_PH: String get() = tr(R.string.mika_account_password_ph)
    val LOGIN_TAB: String get() = tr(R.string.mika_account_login_tab)
    val REGISTER_TAB: String get() = tr(R.string.mika_account_register_tab)
    val LOGIN_BUTTON: String get() = tr(R.string.mika_account_login_button)
    val REGISTER_BUTTON: String get() = tr(R.string.mika_account_register_button)
    val RUNNING: String get() = tr(R.string.mika_account_running)
    val TO_LOGIN: String get() = tr(R.string.mika_account_to_login)
    val TO_REGISTER: String get() = tr(R.string.mika_account_to_register)
    val PASSWORD_HINT: String get() = tr(R.string.mika_account_password_hint)

    /** 昵称(可选)。 */
    val NICK: String get() = tr(R.string.mika_account_nick)
    val NICK_PH: String get() = tr(R.string.mika_account_nick_ph)
    /** 头像选择(预设)。 */
    val AVATAR: String get() = tr(R.string.mika_account_avatar)
    val AVATAR_HINT: String get() = tr(R.string.mika_account_avatar_hint)
    /** 注册成功后引导注册商汤。 */
    val SENSENOVA_GUIDE: String get() = tr(R.string.mika_sensenova_guide)
    val SENSENOVA_BUTTON: String get() = tr(R.string.mika_sensenova_button)
}

/**
 * 预设头像:8 个,本地画(圆底 + 首字母/符号),和后端 /api/avatar/preset/<id> 的 a1–a8 对应。
 * 颜色 + 符号和后端一致,选了直接存 id,后端再下发同样的 SVG。
 */
data class AvatarPreset(val id: String, val color: Color, val glyph: String)

val MIKA_AVATAR_PRESETS: List<AvatarPreset> = listOf(
    AvatarPreset("a1", Color(0xFF4361EE), "M"),
    AvatarPreset("a2", Color(0xFF16A34A), "m"),
    AvatarPreset("a3", Color(0xFFD97706), "Mika"),
    AvatarPreset("a4", Color(0xFFDC2626), "📱"),
    AvatarPreset("a5", Color(0xFF0EA5E9), "🤖"),
    AvatarPreset("a6", Color(0xFF9333EA), "⚡"),
    AvatarPreset("a7", Color(0xFFDB2777), "🌙"),
    AvatarPreset("a8", Color(0xFF6B7280), "★"),
)

/** 面板里的表单状态。 */
data class MikaAccountForm(
    val email: String = "",
    val password: String = "",
    /** 昵称(可选,最多 32 字)。 */
    val nick: String = "",
    /** 头像预设 id(a1–a8);空=不选。 */
    val avatar: String = "",
)

/** 面板动作:复用现有 VM 的登录/注册;失败原因由面板自己显示(ui.accountSignIn.failure)。 */
class MikaAccountPanelActions(
    val login: (email: String, password: String) -> Unit = { _, _ -> },
    val register: (email: String, password: String, nick: String, avatar: String) -> Unit = { _, _, _, _ -> },
    /** 关掉面板。 */
    val dismiss: () -> Unit = {},
    /** 跳浏览器注册商汤(回 App 填 key)。 */
    val openSensenova: () -> Unit = {},
)

/**
 * 面板里的内容(不带面板本身,便于测试)。
 * 登录中(running)时输入区收起、显示进度;失败(failure)时留在原地、说原因,点「登录/注册」重发。
 * 注册页多昵称 + 头像;注册成功后给「去注册商汤」按钮(跳浏览器 www.sensenova.cn)。
 */
@Composable
fun MikaAccountPanelContent(
    running: Boolean,
    failure: String?,
    /** false=登录页, true=注册页。 */
    tab: Boolean,
    onTab: (Boolean) -> Unit,
    form: MikaAccountForm,
    onForm: (MikaAccountForm) -> Unit,
    actions: MikaAccountPanelActions,
    /** 注册成功后的引导(跳浏览器注册商汤);没注册成功不给。 */
    justRegistered: Boolean = false,
) {
    val c = MikaTheme.colors
    val isRegister = tab
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp).testTag("mika-account-panel"),
    ) {
        Text(MikaAccountPanel.TITLE, Modifier.fillMaxWidth(), style = MikaType.Title, color = c.ink, textAlign = TextAlign.Center)
        if (!running) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip(MikaAccountPanel.LOGIN_TAB, !isRegister, onClick = { onTab(false) })
                TabChip(MikaAccountPanel.REGISTER_TAB, isRegister, onClick = { onTab(true) })
            }
            LabeledField(MikaAccountPanel.EMAIL, form.email, { onForm(form.copy(email = it)) },
                MikaAccountPanel.EMAIL_PH, tag = "mika-email")
            LabeledField(MikaAccountPanel.PASSWORD, form.password, { onForm(form.copy(password = it)) },
                MikaAccountPanel.PASSWORD_PH, secret = true, tag = "mika-password")
            if (isRegister) {
                Text(MikaAccountPanel.PASSWORD_HINT, Modifier.fillMaxWidth().padding(top = 2.dp),
                    style = MikaType.Caption, color = c.ink3)
                // 昵称(可选) + 头像选择(预设 8 个)
                LabeledField(MikaAccountPanel.NICK, form.nick, { onForm(form.copy(nick = it)) },
                    MikaAccountPanel.NICK_PH, tag = "mika-nick", optional = true)
                AvatarPicker(form.avatar, { onForm(form.copy(avatar = it)) })
                // 注册成功后引导注册商汤:跳浏览器 www.sensenova.cn 注册 → 回来填 key
                if (justRegistered) {
                    Spacer(Modifier.width(0.dp))
                    Text(MikaAccountPanel.SENSENOVA_GUIDE, Modifier.fillMaxWidth().padding(top = 10.dp),
                        style = MikaType.Caption, color = c.ink2)
                    MikaButton(
                        MikaAccountPanel.SENSENOVA_BUTTON, actions.openSensenova,
                        Modifier.fillMaxWidth().padding(top = 6.dp).testTag("mika-sensenova-open"),
                        kind = ButtonKind.Tonal, size = ButtonSize.Medium,
                    )
                }
            }
            val canAct = form.email.isNotBlank() && form.password.length >= 8
            if (isRegister) {
                MikaButton(
                    MikaAccountPanel.REGISTER_BUTTON,
                    { actions.register(form.email.trim(), form.password, form.nick.trim(), form.avatar) },
                    Modifier.fillMaxWidth().padding(top = 14.dp).testTag("mika-register"),
                    enabled = canAct,
                )
            } else {
                MikaButton(
                    MikaAccountPanel.LOGIN_BUTTON, { actions.login(form.email.trim(), form.password) },
                    Modifier.fillMaxWidth().padding(top = 14.dp).testTag("mika-login"),
                    enabled = canAct,
                )
            }
            val switch = if (isRegister) MikaAccountPanel.TO_LOGIN else MikaAccountPanel.TO_REGISTER
            Text(
                switch,
                Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = { onTab(!isRegister) })
                    .testTag(if (isRegister) "mika-to-login" else "mika-to-register")
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                fontSize = 13.sp, color = c.accent, textAlign = TextAlign.Center,
            )
        } else {
            Text(MikaAccountPanel.RUNNING, Modifier.fillMaxWidth().padding(top = 16.dp).testTag("mika-running"),
                fontSize = 13.sp, lineHeight = 18.sp, color = c.ink2, textAlign = TextAlign.Center)
        }
        failure?.let {
            Text(it, Modifier.fillMaxWidth().padding(top = 10.dp).testTag("mika-failed"),
                fontSize = 13.sp, lineHeight = 18.sp, color = c.bad, textAlign = TextAlign.Center)
        }
    }
}

/** 头像选择:预设 8 个,点一个高亮,点已选中的取消。 */
@Composable
private fun AvatarPicker(selected: String, onSelect: (String) -> Unit) {
    val c = MikaTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(MikaAccountPanel.AVATAR, style = MikaType.Caption, color = c.ink2)
            Text(MikaAccountPanel.AVATAR_HINT, style = MikaType.Hint, color = c.ink3)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MIKA_AVATAR_PRESETS.forEach { p ->
                val on = selected == p.id
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (on) p.color else c.fill)
                        .clickable(onClick = { onSelect(if (on) "" else p.id) })
                        .testTag("mika-avatar-${p.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.glyph,
                        style = MikaType.Body.copy(color = if (on) Color.White else c.ink2),
                        fontSize = if (p.glyph.length > 1) 11.sp else 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = MikaTheme.colors
    Text(
        label,
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
        color = if (selected) c.accent else c.ink2,
    )
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit, placeholder: String,
    secret: Boolean = false, tag: String, optional: Boolean = false,
) {
    val c = MikaTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label + if (optional) " *" else "", Modifier.width(84.dp), style = MikaType.Body, color = c.ink)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MikaType.Body, color = c.ink3, maxLines = 1)
            BasicTextField(
                value, onChange,
                Modifier.fillMaxWidth().testTag(tag), singleLine = true,
                textStyle = MikaType.Body.copy(color = c.ink),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
                ),
                cursorBrush = SolidColor(c.accent),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            )
        }
    }
}

/**
 * 引导卡:未登录([email] 为 null)时一个醒目的「注册 mika 账号 + 填商汤 key,解锁商汤免费模型」+「注册/登录」按钮
 * (点进 [MikaAccountPanelSheet] 的注册页);已登录时换成「已登录 xxx@xx」的只读卡。
 * 不阻断进入聊天:卡本身只读,只有按钮才跳走(用户可跳过,用其他服务商)。
 */
@Composable
fun MikaAccountGuideCard(
    email: String?, // 非空=已登录
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MikaTheme.colors
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentSoft).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(MikaIcons.Spark, TileTone.Accent)
            Column(Modifier.weight(1f)) {
                Text(
                    if (email == null) MikaAccountPanel.GUIDE else MikaAccountPanel.signedIn(email),
                    fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = c.ink, maxLines = 2,
                )
                if (email == null) {
                    Text(MikaAccountPanel.GUIDE_NOTE, Modifier.padding(top = 2.dp),
                        style = MikaType.Caption, color = c.ink2, maxLines = 2)
                }
            }
        }
        if (email == null) {
            MikaButton(
                MikaAccountPanel.GUIDE_BUTTON, onOpenAccount,
                Modifier.fillMaxWidth().padding(top = 10.dp).testTag("mika-guide-open"),
                kind = ButtonKind.Secondary, size = ButtonSize.Medium,
            )
        }
    }
}

/**
 * 注册/登录的底部弹层:复用 [MikaAccountPanelContent];失败留在面板里说原因,点「注册/登录」重发。
 * 登录成功由 [ModelSetupScreen] 里 `signedInProvider` 的 LaunchedEffect 收起面板、打开服务商详情(已实现)。
 * 注册成功后给「去注册商汤」按钮:跳浏览器 www.sensenova.cn 注册 → 回来填 key。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikaAccountPanelSheet(
    ui: ModelSetupUi,
    /** false=登录页, true=注册页(引导卡默认进注册页)。 */
    initialTab: Boolean,
    actions: MikaAccountPanelActions,
    /** 注册成功后跳浏览器注册商汤。 */
    onOpenSensenova: () -> Unit = {},
    /** 注册成功后的引导标记(由调用方在 mikaRegister Ok 时置位)。 */
    justRegistered: Boolean = false,
) {
    val c = MikaTheme.colors
    var tab by remember { mutableStateOf(initialTab) }
    var form by remember { mutableStateOf(MikaAccountForm()) }
    ModalBottomSheet(
        onDismissRequest = actions.dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        MikaAccountPanelContent(
            running = ui.accountSignIn.running,
            failure = ui.accountSignIn.failure,
            tab = tab, onTab = { tab = it },
            form = form, onForm = { form = it },
            actions = actions, 
            justRegistered = justRegistered,
        )
    }
}

/**
 * 给 [WelcomeScreen] 用的轻封装:未登录时显示引导卡、点按钮开面板;已登录时换成「已登录」只读卡。
 * 每次进来读一次本地会话(登录/退出后回到欢迎页时也能反映最新状态)。
 */
@Composable
fun WelcomeMikaGuide(onOpenAccount: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val email = remember { runCatching { MikaAccount.load(context) }.getOrNull()?.email }
    if (email == null) {
        MikaAccountGuideCard(null, onOpenAccount = onOpenAccount, modifier = modifier.testTag("mika-guide-welcome"))
    } else {
        MikaAccountGuideCard(email, onOpenAccount = {}, modifier = modifier.testTag("mika-guide-welcome-signedin"))
    }
}

/** 给 ModelSetupContent(设置 › 模型 / 上手配置模型页)顶部用的轻封装:同 [WelcomeMikaGuide],但 tag 区分。 */
@Composable
fun ModelSetupMikaGuide(onOpenAccount: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val email = remember { runCatching { MikaAccount.load(context) }.getOrNull()?.email }
    if (email == null) {
        MikaAccountGuideCard(null, onOpenAccount = onOpenAccount, modifier = modifier.testTag("mika-guide-model"))
    } else {
        MikaAccountGuideCard(email, onOpenAccount = {}, modifier = modifier.testTag("mika-guide-model-signedin"))
    }
}

/** 跳浏览器注册商汤(www.sensenova.cn);回 App 后用户去填 key。 */
fun openSensenovaRegister(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sensenova.cn"))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
