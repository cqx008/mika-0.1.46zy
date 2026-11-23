package com.wangzan101.mika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.wangzan101.mika.R
import com.wangzan101.mika.feedback.SuggestionSender
import com.wangzan101.mika.ui.components.ButtonKind
import com.wangzan101.mika.ui.components.MikaButton
import com.wangzan101.mika.ui.components.PageBackground
import com.wangzan101.mika.ui.components.RadioRow
import com.wangzan101.mika.ui.components.TopBar
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页「反馈与建议」:发一条**纯文字**的意见建议/咨询/其他给后台——
 * 不带机型/任务日志/堆栈(那是「反馈工单」[OfficialFeedbackScreen] 的事),只带:
 * 类型(建议默认/咨询/其他)+ 正文 + 邮箱(登录了才带,后台用来回复)。
 * 发送成功显示记录号,失败显示原因。
 */
@Composable
internal fun FeedbackSuggestionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = MikaTheme.colors
    val scope = rememberCoroutineScope()
    // 在 Composable 顶层取好字符串(不在 onClick 等 lambda 里调 stringResource, 否则报 @Composable 上下文错)
    val emptyMsg = stringResource(R.string.feedback_suggestion_empty)
    val sendMsg = stringResource(R.string.feedback_send)
    val sendingMsg = stringResource(R.string.feedback_sending)
    // 类型:建议(默认)/咨询/其他
    var type by remember { mutableStateOf("suggestion") }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sentId by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    PageBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.settings_feedback), MikaIcons.Back, onBack, stringResource(R.string.back))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_feedback), style = MikaType.Title, color = c.ink)
                    Text(stringResource(R.string.feedback_suggestion_sub), style = MikaType.Hint, color = c.ink2)
                }

                // 类型:建议/咨询/其他(默认建议)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface)
                    .padding(horizontal = 4.dp, vertical = 8.dp).testTag("feedback-suggestion-type")) {
                    Text(stringResource(R.string.feedback_type_label), style = MikaType.BodyStrong, color = c.ink)
                    RadioRow(
                        title = stringResource(R.string.feedback_type_suggestion),
                        selected = type == "suggestion",
                        first = true,
                        onClick = { type = "suggestion" },
                    )
                    RadioRow(
                        title = stringResource(R.string.feedback_type_question),
                        selected = type == "question",
                        onClick = { type = "question" },
                    )
                    RadioRow(
                        title = stringResource(R.string.feedback_type_other),
                        selected = type == "other",
                        onClick = { type = "other" },
                    )
                }

                // 正文(纯文字,不带机型/日志)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feedback_suggestion_text_title), style = MikaType.BodyStrong, color = c.ink)
                    Text(stringResource(R.string.feedback_suggestion_text_hint), style = MikaType.Hint, color = c.ink3)
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("feedback-suggestion-text"),
                        enabled = !sending,
                    )
                }

                val id = sentId
                when {
                    id != null ->
                        Text(
                            stringResource(R.string.feedback_sent, id),
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.okSoft)
                                .padding(horizontal = 14.dp, vertical = 12.dp).testTag("feedback-suggestion-success"),
                            style = MikaType.Body, color = c.ok,
                        )
                    else ->
                        MikaButton(
                            if (sending) sendingMsg else sendMsg,
                            {
                                if (sending) return@MikaButton
                                val trimmed = text.trim()
                                if (trimmed.isEmpty()) { errorText = emptyMsg; return@MikaButton }
                                scope.launch {
                                    sending = true
                                    errorText = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { SuggestionSender.send(context, type, trimmed) }.getOrNull()
                                    }
                                    result?.let { r ->
                                        if (r.ok) sentId = r.ticketId
                                        else errorText = r.error
                                    }
                                    if (result == null) errorText = "unknown error"
                                    sending = false
                                }
                            },
                            Modifier.fillMaxWidth().testTag("feedback-suggestion-send"),
                            enabled = !sending,
                        )
                }
                errorText?.let {
                    Text(
                        it,
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.badSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("feedback-suggestion-error"),
                        style = MikaType.Body, color = c.bad,
                    )
                }
            }
        }
    }
}
