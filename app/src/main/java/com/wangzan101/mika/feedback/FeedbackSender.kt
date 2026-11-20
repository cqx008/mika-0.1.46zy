package com.wangzan101.mika.feedback

import android.content.Context
import com.wangzan101.mika.BuildConfig
import com.wangzan101.mika.account.MikaAccount
import com.wangzan101.mika.core.net.NetProxy
import com.wangzan101.mika.core.record.RunEvent
import com.wangzan101.mika.core.record.RunMetrics
import com.wangzan101.mika.record.RunStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 「反馈给官方」:把这次任务运行的报错打包发给官方后台(生成一张工单)。
 * 只传诊断(机型/厂商/系统版本/SDK/ABI + 各用途模型 + 最近任务结局)和这次任务自己的日志(每步动作 + 报错结果),
 * 外加用户自己填的描述、结局/失败类别/判定,以及登录后的邮箱;
 * 不传回放帧(截图)、不传 Key、token、聊天内容和屏幕原文(事件里的参数与结果落盘时已脱敏,这里只做整段脱敏兜底)。
 */
object FeedbackSender {
    private val FEEDBACK_URL = BuildConfig.MIKA_BACKEND_BASE + "/api/feedback"
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 2000L
    private val JSON = "application/json; charset=utf-8".toMediaType()
    // 目标(自有后台)不走国内代理:代理卡住/不通时会把请求挂在代理上直到 callTimeout。
    // 直连为主客户端;另保留一个代理客户端兜底(直连失败时回退试一次代理)。
    private val direct: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }
    private val proxied: OkHttpClient by lazy {
        NetProxy.shared.client(OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS))
    }
    private val json = Json { ignoreUnknownKeys = true }

    /** 一条工单里「这次任务」的完整日志:概要 + 每个事件(已脱敏)。 */
    class Result(
        /** 成功:工单号。 */
        val ticketId: String? = null,
        /** 失败:给用户看的原因。 */
        val error: String? = null,
    ) {
        val ok: Boolean get() = ticketId != null
    }

    /**
     * 打包并发送。[runId] 是任务报告页当前那份记录;[store] 读它的概要、小结和事件;
     * [metrics] 带结局/失败类别/判定(读不出时留空);[description] 是用户填的问题描述(可空);[appDiagnostics] 是诊断文本。
     */
    suspend fun send(
        context: Context,
        runId: String,
        store: RunStore,
        metrics: RunMetrics?,
        description: String,
        appDiagnostics: String,
        ticketType: String = "error",
    ): Result = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val summary = runCatching { store.summary(runId) }.getOrNull()
        val events = runCatching { store.events(runId) }.getOrNull().orEmpty()
        val session = runCatching { MikaAccount.load(app) }.getOrNull()

        val payload = buildJsonObject {
            put("source", "mika-app")
            put("runId", runId)
            put("createdAtMs", System.currentTimeMillis())
            // 工单类型(报错/建议/咨询):后台按类型分流 AI 审核
            put("type", ticketType.ifBlank { "error" })
            // 设备
            put("device", buildJsonObject {
                put("manufacturer", android.os.Build.MANUFACTURER)
                put("model", android.os.Build.MODEL)
                put("androidRelease", android.os.Build.VERSION.RELEASE)
                put("sdk", android.os.Build.VERSION.SDK_INT)
                put("abis", buildJsonArray { android.os.Build.SUPPORTED_ABIS.forEach { add(JsonPrimitive(it)) } })
            })
            put("appVersion", com.wangzan101.mika.BuildConfig.VERSION_NAME)
            put("build", com.wangzan101.mika.BuildConfig.GIT_HASH)
            // 结局/失败类别/判定(读不到小结时留空)
            put("outcome", metrics?.outcome.orEmpty())
            put("failKind", metrics?.failKind.orEmpty())
            put("judged", metrics?.judged.orEmpty())
            // 用户描述(脱敏)
            put("description", com.wangzan101.mika.core.record.Redactor.text(description).take(4000))
            // 登录了才带邮箱(脱敏:邮箱本身可带,后端按它回显「已修复」)
            session?.let { put("email", it.email) }
            // 诊断文本(各用途模型 + 最近任务 Run 等,整段再脱敏兜底)
            put("diagnostics", com.wangzan101.mika.about.Diagnostics.sanitize(appDiagnostics))
            // 任务概要 + 完整日志(每步事件;报错堆栈在事件结果里,已脱敏;不传回放帧)
            put("run", buildJsonObject {
                put("runId", runId)
                put("goal", summary?.goal.orEmpty())
                put("startedAtMs", summary?.startedAtMs ?: 0L)
                put("endedAtMs", summary?.endedAtMs?.toString().orEmpty())
                put("outcome", summary?.outcome ?: metrics?.outcome.orEmpty())
                put("failKind", metrics?.failKind.orEmpty())
                put("judged", summary?.judged ?: metrics?.judged.orEmpty())
                put("model", summary?.model.orEmpty())
                put("steps", summary?.steps?.toString() ?: (metrics?.steps?.toString()).orEmpty())
                put("modelCalls", metrics?.modelCalls?.toString().orEmpty())
                put("events", buildJsonArray {
                    events.forEach { e -> add(JsonPrimitive(RunEvent.encode(e))) }
                })
            })
        }

        val body = payload.toString().toRequestBody(JSON)
        val request = Request.Builder().url(FEEDBACK_URL)
            .header("User-Agent", "Mika/0.1")
            .post(body)
            .build()

        // 自动重试:发送失败(含超时、代理不通、5xx)最多 3 次,间隔 2s;
        // 网络/超时类失败时回退客户端(直连失败试代理,代理失败试直连),5xx 不换客户端原样重试;
        // 拿到 Result(成功或全部失败)才返回,不卡协程。
        var last: Result = Result(error = "request failed")
        var useDirect = true
        repeat(MAX_ATTEMPTS) { i ->
            val client = if (useDirect) direct else proxied
            val attempt = runCatching {
                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val obj = runCatching { json.parseToJsonElement(text).let { it as? JsonObject } }.getOrNull()
                    if (resp.isSuccessful && obj?.get("ok")?.let { (it as? JsonPrimitive)?.content == "true" } == true) {
                        Result(ticketId = (obj.get("ticketId") as? JsonPrimitive)?.content?.ifBlank { null })
                    } else {
                        val err = (obj?.get("error") as? JsonPrimitive)?.content?.ifBlank { null } ?: text.take(200)
                        Result(error = "HTTP ${resp.code} ${err}")
                    }
                }
            }
            last = attempt.fold(
                onSuccess = { it },
                onFailure = { Result(error = "request failed: ${it.message ?: it.javaClass.simpleName}") },
            )
            if (last.ok) return@withContext last
            // 只有网络/超时类失败才在下一轮回退客户端;5xx 与 4xx 不换路,原样重试
            val code = last.error.orEmpty().removePrefix("HTTP ").take(4).toIntOrNull()
            if (code == null || (code in 500..599)) useDirect = !useDirect
            if (i < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        last
    }
}
