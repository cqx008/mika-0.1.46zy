package com.wangzan101.mika.feedback

import android.content.Context
import com.wangzan101.mika.BuildConfig
import com.wangzan101.mika.account.MikaAccount
import com.wangzan101.mika.core.net.NetProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 「反馈与建议」:纯文字反馈(意见建议/咨询/其他)发给可配置后端生成一张反馈记录。
 * 不带机型/任务日志/堆栈那套——那是 [FeedbackSender](报错工单)的事。
 * 只带:类型(建议/咨询/其他)+ 正文 + 邮箱(登录了才带,用于后台回复)。
 */
object SuggestionSender {
    private val URL = BuildConfig.MIKA_BACKEND_BASE + "/api/feedback"
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 2000L
    private val JSON = "application/json; charset=utf-8".toMediaType()
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

    class Result(
        /** 成功:后台生成的记录号。 */
        val ticketId: String? = null,
        /** 失败:给用户看的原因。 */
        val error: String? = null,
    ) {
        val ok: Boolean get() = ticketId != null
    }

    /**
     * 打包并发送。[type] 是反馈类型(suggestion/question/other),[text] 是用户写的纯文字,
     * 脱敏后最多 4000 字。
     */
    suspend fun send(
        context: Context,
        type: String,
        text: String,
    ): Result = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val session = runCatching { MikaAccount.load(app) }.getOrNull()
        val t = if (type in setOf("suggestion", "question", "other")) type else "suggestion"
        val body = com.wangzan101.mika.core.record.Redactor.text(text).take(4000)

        val payload = buildJsonObject {
            put("source", "mika-app")
            put("category", "suggestion") // 与报错工单(category 缺省=报错)分开
            put("type", t)
            put("description", body)
            session?.let { put("email", it.email) }
            put("appVersion", com.wangzan101.mika.BuildConfig.VERSION_NAME)
            put("build", com.wangzan101.mika.BuildConfig.GIT_HASH)
        }

        val request = Request.Builder().url(URL)
            .header("User-Agent", "Mika/0.1")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        var last: Result = Result(error = "request failed")
        var useDirect = true
        repeat(MAX_ATTEMPTS) { i ->
            val client = if (useDirect) direct else proxied
            val attempt = runCatching {
                client.newCall(request).execute().use { resp ->
                    val text2 = resp.body?.string().orEmpty()
                    val obj = runCatching { json.parseToJsonElement(text2).let { it as? JsonObject } }.getOrNull()
                    if (resp.isSuccessful && obj?.get("ok")?.let { (it as? JsonPrimitive)?.content == "true" } == true) {
                        Result(ticketId = (obj.get("ticketId") as? JsonPrimitive)?.content?.ifBlank { null })
                    } else {
                        val err = (obj?.get("error") as? JsonPrimitive)?.content?.ifBlank { null } ?: text2.take(200)
                        Result(error = "HTTP ${resp.code} $err")
                    }
                }
            }
            last = attempt.fold(
                onSuccess = { it },
                onFailure = { Result(error = "request failed: ${it.message ?: it.javaClass.simpleName}") },
            )
            if (last.ok) return@withContext last
            val code = last.error.orEmpty().removePrefix("HTTP ").take(4).toIntOrNull()
            if (code == null || (code in 500..599)) useDirect = !useDirect
            if (i < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        last
    }
}
