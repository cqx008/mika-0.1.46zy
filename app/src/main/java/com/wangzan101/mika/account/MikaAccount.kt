package com.wangzan101.mika.account

import android.content.Context
import com.wangzan101.mika.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Mika 账号:注册 / 登录 / 心跳 / 配额,走可配置后端(BuildConfig.MIKA_BACKEND_BASE,
 * 在 gradle.properties 注入 MIKA_BACKEND_BASE)。
 * 设备指纹 = ANDROID_ID + 机型 + 系统版本,同一设备只能绑一个账号(服务端 403)。
 * 官方(agnes)模型入口已撤(0.1.45):后端不再下发官方 key/模型,账号只存昵称/头像 + 商汤引导信息,
 * 模型走用户自己的 key(商汤免费预设或自定义 key,存 App 本地)。
 */
object MikaAccount {
    private val BASE = BuildConfig.MIKA_BACKEND_BASE
    private val json = Json { ignoreUnknownKeys = true }
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** 登录凭据 + 账号资料(昵称/头像)+ 商汤引导,整份存本机。 */
    data class Session(
        val email: String,
        val token: String,
        val dailyQuota: Int,
        val todayUsed: Int,
        val announcement: String,
        val perTaskModelCallLimit: Int = 50,
        /** 昵称(注册/资料页填的,可选);没填时后端回落邮箱前缀。 */
        val nick: String = "",
        /** 头像:预设 id(a1–a8)或上传后的 /avatars/xxx 相对路径;空=没有。 */
        val avatar: String = "",
        /** 头像预设 id(a1–a8);avatar 是上传路径时为空。 */
        val avatarPreset: String = "",
        /** 商汤注册/领 Key 的网页(跳浏览器注册)。 */
        val sensenovaRegisterUrl: String = "https://www.sensenova.cn",
        /** 商汤免费预设里存的商汤 key(填了才能连上,没填时只建服务商提示填 key);登录/注册/心跳时同步。 */
        val sensenovaKey: String = "",
        /** 商汤免费模型 OpenAI 兼容接口。 */
        val sensenovaApiBase: String = "https://token.sensenova.cn/v1",
        /** 商汤免费档模型名(写死,预设里也用这个)。 */
        val sensenovaModel: String = "sensenova-6.8-flash-lite",
        val sensenovaEnabled: Boolean = true,
        /** 该账号已填过商汤 key(本地标记,上报用)。 */
        val sensenovaUsed: Boolean = false,
        /** 「你有 N 条反馈已修复 ✓」:该用户已报工单中状态为「已修复」的条数,静默显示,不弹窗。 */
        val fixedFeedbackCount: Int = 0,
    )

    sealed interface Result {
        data class Ok(val session: Session, val isRegister: Boolean) : Result
        data object Canceled : Result
        data class Error(val message: String) : Result
    }

    private fun defaultClient(context: Context): OkHttpClient =
        com.wangzan101.mika.core.net.NetProxy.shared.client(OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS))

    private fun contextOf(c: Context): Context = c.applicationContext

    // ---- 持久化 ----
    private fun file(context: Context) = java.io.File(contextOf(context).filesDir, "mika-account-session.json")

    fun load(context: Context): Session? {
        val f = file(context)
        if (!f.exists()) return null
        return runCatching {
            val o = Json.parseToJsonElement(f.readText()).jsonObject
            session(o)
        }.getOrNull()
    }

    private fun save(context: Context, s: Session) {
        val txt = buildJsonObject {
            put("email", s.email); put("token", s.token)
            put("dailyQuota", s.dailyQuota); put("todayUsed", s.todayUsed)
            put("announcement", s.announcement)
            put("perTaskModelCallLimit", s.perTaskModelCallLimit)
            put("nick", s.nick); put("avatar", s.avatar); put("avatarPreset", s.avatarPreset)
            put("sensenovaRegisterUrl", s.sensenovaRegisterUrl)
            put("sensenovaKey", s.sensenovaKey)
            put("sensenovaApiBase", s.sensenovaApiBase); put("sensenovaModel", s.sensenovaModel)
            put("sensenovaEnabled", s.sensenovaEnabled); put("sensenovaUsed", s.sensenovaUsed)
            put("fixedFeedbackCount", s.fixedFeedbackCount)
        }.toString()
        runCatching {
            val f = file(context)
            val tmp = java.io.File(f.path + ".tmp")
            tmp.writeText(txt)
            if (!tmp.renameTo(f)) { f.writeText(txt); tmp.delete() }
        }
    }

    fun clear(context: Context) { runCatching { file(context).delete() } }

    /** 昵称/头像/商汤 key 更新后存回本机(填 key、改资料时调用)。 */
    fun saveProfile(context: Context, nick: String, avatar: String, avatarPreset: String, sensenovaKey: String) {
        val s = load(context) ?: return
        save(context, s.copy(nick = nick, avatar = avatar, avatarPreset = avatarPreset, sensenovaKey = sensenovaKey))
    }

    // ---- 接口 ----

    /** 注册:邮箱+密码+设备指纹(+昵称/头像)。成功直接登录。 */
    suspend fun register(context: Context, email: String, password: String, nick: String = "", avatar: String = ""): Result = withContext(Dispatchers.IO) {
        runCatching {
            val o = postJson(context, "/api/register", buildJsonObject {
                put("email", email); put("password", password)
                put("nick", nick); put("avatar", avatar)
                put("androidId", androidId(context)); put("model", deviceModel(context)); put("osVer", osVersion(context))
            }.toString(), null) ?: error("register failed")
            val s = session(o)
            save(context, s)
            Result.Ok(s, true)
        }.getOrElse { Result.Error(it.message ?: "register failed") }
    }

    /** 登录。 */
    suspend fun login(context: Context, email: String, password: String): Result = withContext(Dispatchers.IO) {
        runCatching {
            val o = postJson(context, "/api/login", buildJsonObject {
                put("email", email); put("password", password)
            }.toString(), null) ?: error("login failed")
            val s = session(o)
            save(context, s)
            Result.Ok(s, false)
        }.getOrElse { Result.Error(it.message ?: "login failed") }
    }

    /**
     * 商汤 key 保存后上报一次(账号记「已填商汤 key」);失败不阻塞,静默吞掉。
     * 返回更新后的会话(含 sensenovaUsed)。
     */
    suspend fun markSensenovaUsed(context: Context, session: Session): Session? = withContext(Dispatchers.IO) {
        runCatching {
            val o = postJson(context, "/api/sensenova/mark", buildJsonObject {
                put("token", session.token); put("keySet", true)
            }.toString(), null) ?: return@withContext null
            val s = session.copy(
                sensenovaUsed = o["sensenovaUsed"]?.jsonPrimitive?.booleanOrNull ?: true,
            )
            save(context, s)
            s
        }.getOrNull()
    }

    private fun session(o: JsonObject): Session = Session(
        email = o.str("email"), token = o.str("token"),
        dailyQuota = o.int("dailyQuota"), todayUsed = o.int("todayUsed"),
        announcement = o.str("announcement"),
        perTaskModelCallLimit = o.int("perTaskModelCallLimit").takeIf { it > 0 } ?: 50,
        nick = o.str("nick"),
        avatar = o.str("avatar"),
        avatarPreset = o.str("avatarPreset"),
        sensenovaRegisterUrl = o.str("sensenovaRegisterUrl").ifEmpty { "https://www.sensenova.cn" },
        sensenovaKey = o.str("sensenovaKey"),
        sensenovaApiBase = o.str("sensenovaApiBase").ifEmpty { "https://token.sensenova.cn/v1" },
        sensenovaModel = o.str("sensenovaModel").ifEmpty { "sensenova-6.8-flash-lite" },
        sensenovaEnabled = o.bool("sensenovaEnabled", true),
        sensenovaUsed = o.bool("sensenovaUsed", false),
        fixedFeedbackCount = o.int("fixedFeedbackCount"),
    )

    /** 刷新会话(心跳): 更新配额、公告、账号资料和商汤引导信息。 */
    suspend fun refresh(context: Context, session: Session): Session? = withContext(Dispatchers.IO) {
        runCatching {
            val o = postJson(context, "/api/status", buildJsonObject {
                put("token", session.token)
            }.toString(), null) ?: return@withContext null
            val s = session(o)
            // /api/status 响应不带 token/sensenovaKey 字段，保留原值防止被清空
            val s2 = s.copy(
                token = if (s.token.isBlank()) session.token else s.token,
                sensenovaKey = if (s.sensenovaKey.isBlank()) session.sensenovaKey else s.sensenovaKey,
            )
            save(context, s2)
            s2
        }.getOrNull()
    }

    /** 配额:一次 AI 任务扣 1。返回剩余;429 时返回 null。 */
    suspend fun consumeQuota(context: Context, session: Session, sensenovaUsed: Boolean = false): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val o = postJson(context, "/api/quota/consume", buildJsonObject {
                put("token", session.token); put("taskCount", 1)
                if (sensenovaUsed) put("sensenovaUsed", true)
            }.toString(), null, allow429 = true) ?: return@withContext null
            o.int("remaining")
        }.getOrNull()
    }

    /** 退出登录。 */
    fun signOut(context: Context) { clear(context) }

    // ---- 设备指纹 ----
    private fun androidId(context: Context): String =
        runCatching { android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) }.getOrNull() ?: "unknown"
    private fun deviceModel(context: Context): String = android.os.Build.MODEL
    private fun osVersion(context: Context): String = android.os.Build.VERSION.RELEASE

    // ---- HTTP ----
    private fun postJson(context: Context, path: String, body: String, auth: String?, allow429: Boolean = false): JsonObject? {
        val client = defaultClient(context)
        val req = Request.Builder().url("$BASE$path")
            .header("User-Agent", "Mika/0.1")
            .apply { if (auth != null) header("Authorization", "Bearer $auth") }
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (resp.code == 429 && allow429) return null
            if (!resp.isSuccessful) {
                val err = runCatching { json.parseToJsonElement(text).jsonObject.str("error") }.getOrNull()
                error("HTTP ${resp.code} ${err ?: text}".trim())
            }
            return runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        }
    }

    private fun JsonObject.str(k: String): String = (this[k] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: ""
    private fun JsonObject.int(k: String): Int = (this[k] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
    private fun JsonObject.bool(k: String, default: Boolean): Boolean {
        val el = this[k] as? JsonPrimitive ?: return default
        return if (el.isString) el.content.toBooleanStrictOrNull() ?: default else el.booleanOrNull ?: default
    }
}
