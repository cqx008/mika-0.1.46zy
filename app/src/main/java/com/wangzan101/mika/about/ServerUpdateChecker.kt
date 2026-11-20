package com.wangzan101.mika.about

import com.wangzan101.mika.BuildConfig

/** 检查更新的源:可配置后端(BuildConfig.MIKA_BACKEND_BASE,账号/配额/更新/公告一体)或 GitHub 发布页。 */
object ServerUpdateChecker {
    val ENDPOINT = BuildConfig.MIKA_BACKEND_BASE + "/api/update/check"
}
