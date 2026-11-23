package com.wangzan101.mika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.account.MikaAccount
import com.wangzan101.mika.about.Diagnostics
import com.wangzan101.mika.about.DiagnosticsCollector
import com.wangzan101.mika.feedback.FeedbackSender
import com.wangzan101.mika.record.RunStore
import com.wangzan101.mika.settings.SystemStatus
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
 * 从关于页「反馈给官方」(或任务报告/回放的「反馈」按钮)进来的发送页:
 * 显示这次要发的那份运行记录,填一句描述(可选),点「发送」打包设备诊断 + 这份任务的完整日志(每步动作 + 报错结果)
 * 发给官方后台生成工单。发送成功显示工单号,失败显示原因;截图、Key、聊天内容都不带。
 * [systemStatus] 是此刻的系统状态(由调用方传,避免在这里重建监视器);
 * 没登录时只带诊断和任务日志,不带邮箱。
 */
@Composable
internal fun OfficialFeedbackScreen(
    runId: String,
    systemStatus: SystemStatus,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val c = MikaTheme.colors
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var summaryText by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    // 工单类型(报错/建议/咨询):默认「报错」,发给后台后 AI 按类型审核
    var ticketType by remember { mutableStateOf("error") }
    var sending by remember { mutableStateOf(false) }
    var ticketId by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(runId, systemStatus) {
        summaryText = withContext(Dispatchers.IO) {
            runCatching {
                val store = RunStore(context)
                val s = store.summary(runId)
                val metrics = store.metrics.byIds(setOf(runId))[runId]
                buildString {
                    s?.let {
                        append("Task: ").append(it.goal).append('\n')
                        append("Started: ").append(it.startedAtMs).append('\n')
                        append("Outcome: ").append(s.outcome ?: (metrics?.outcome ?: "unknown")).append('\n')
                        metrics?.failKind?.let { fk -> append("FailKind: ").append(fk).append('\n') }
                        metrics?.judged?.let { j -> append("Judged: ").append(j).append('\n') }
                    }
                    append('\n')
                    append(Diagnostics.render(DiagnosticsCollector.collect(context, systemStatus)))
                }
            }.getOrNull()
        }
        loading = false
    }

    val loggedIn = remember { runCatching { MikaAccount.load(context) }.getOrNull() }

    PageBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.feedback_official), MikaIcons.Back, onBack, stringResource(R.string.back))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 运行概要
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.feedback_official), style = MikaType.Title, color = c.ink)
                    Text(stringResource(R.string.feedback_official_sub), style = MikaType.Hint, color = c.ink2)
                    loggedIn?.let { Text("Account: ${it.email}", style = MikaType.Caption, color = c.ink3) }
                    Text("Run: $runId", style = MikaType.Caption, color = c.ink3)
                }

                // 描述输入
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feedback_desc_title), style = MikaType.BodyStrong, color = c.ink)
                    Text(stringResource(R.string.feedback_desc_hint), style = MikaType.Hint, color = c.ink3)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        enabled = !sending,
                    )
                }

                // 工单类型:报错(默认)/建议/咨询(后台按类型分流 AI 审核)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feedback_type_label), style = MikaType.BodyStrong, color = c.ink)
                    RadioRow(
                        title = stringResource(R.string.feedback_type_error),
                        selected = ticketType == "error",
                        first = true,
                        onClick = { ticketType = "error" },
                    )
                    RadioRow(
                        title = stringResource(R.string.feedback_type_suggestion),
                        selected = ticketType == "suggestion",
                        onClick = { ticketType = "suggestion" },
                    )
                    RadioRow(
                        title = stringResource(R.string.feedback_type_question),
                        selected = ticketType == "question",
                        onClick = { ticketType = "question" },
                    )
                }

                // 诊断与运行概要预览(可滚动)
                if (!loading) {
                    summaryText?.let { txt ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.diagnostics_copy_sub), style = MikaType.BodyStrong, color = c.ink)
                            SelectionContainer {
                                Text(
                                    txt,
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.fill).padding(10.dp)
                                        .testTag("feedback-preview"),
                                    style = MikaType.Caption.copy(fontFamily = FontFamily.Monospace),
                                    color = c.ink2,
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.diagnostics_loading), style = MikaType.Hint, color = c.ink3)
                }

                // 发送按钮
                val ticket = ticketId
                when {
                    ticket != null ->
                        Text(
                            stringResource(R.string.feedback_sent, ticket),
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.okSoft)
                                .padding(horizontal = 14.dp, vertical = 12.dp).testTag("feedback-success"),
                            style = MikaType.Body, color = c.ok,
                        )
                    else ->
                        MikaButton(
                            if (sending) stringResource(R.string.feedback_sending) else stringResource(R.string.feedback_send),
                            {
                                if (sending) return@MikaButton
                                scope.launch {
                                    sending = true
                                    errorText = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val store = RunStore(context)
                                            val metrics = store.metrics.byIds(setOf(runId))[runId]
                                            FeedbackSender.send(context, runId, store, metrics, description.trim(), summaryText.orEmpty(), ticketType)
                                        }.getOrNull()
                                    }
                                    result?.let { r ->
                                        if (r.ok) ticketId = r.ticketId
                                        else errorText = r.error
                                    }
                                    if (result == null) errorText = "unknown error"
                                    sending = false
                                }
                            },
                            Modifier.fillMaxWidth().testTag("feedback-send"),
                            enabled = !sending && !loading,
                        )
                }
                errorText?.let {
                    Text(
                        stringResource(R.string.feedback_failed, it),
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.badSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("feedback-error"),
                        style = MikaType.Body, color = c.bad,
                    )
                }
            }
        }
    }
}
