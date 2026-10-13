// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import com.ef_softworks.pathfinder_server.exception.base.WrongTypeException
import java.net.URI

/**
 * What a code may be called and where it may point. Targets are validated here and not on the redirect: a bad
 * address must be refused in the console, never discovered by the first person who scans the printed code.
 */
object QrCodeInputRules {

    private const val MAX_NAME_LENGTH = 128
    private const val MAX_URL_LENGTH = 2048
    private val SLUG_PATTERN = Regex("^[A-Za-z0-9_-]{3,64}$")

    fun name(raw: String): String {
        val name = raw.trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) {
            throw WrongTypeException("Name must be 1-$MAX_NAME_LENGTH characters", "Название: от 1 до $MAX_NAME_LENGTH символов")
        }
        return name
    }

    /** A slug typed by the operator; null when left blank so one is generated. */
    fun optionalSlug(raw: String?): String? {
        val slug = raw?.trim().orEmpty()
        if (slug.isEmpty()) return null
        if (!SLUG_PATTERN.matches(slug)) {
            throw WrongTypeException(
                "Slug must be 3-64 characters of latin letters, digits, '_' or '-'",
                "Слаг: от 3 до 64 символов – латиница, цифры, «_» или «-»",
            )
        }
        return slug
    }

    /** A platform target: may be blank (falls back to the desktop target on a scan), otherwise an absolute http(s) address. */
    fun optionalTarget(raw: String, platformLabel: String): String {
        val url = raw.trim()
        if (url.isEmpty()) return ""
        return requireHttpUrl(url, platformLabel)
    }

    /** The desktop target is the fallback for every other platform, so it can never be blank. */
    fun desktopTarget(raw: String): String {
        val url = raw.trim()
        if (url.isEmpty()) {
            throw WrongTypeException("Desktop target URL is required", "Ссылка для Desktop обязательна – это запасной вариант для всех платформ")
        }
        return requireHttpUrl(url, "Desktop")
    }

    private fun requireHttpUrl(url: String, platformLabel: String): String {
        val valid = url.length <= MAX_URL_LENGTH &&
            (url.startsWith("https://") || url.startsWith("http://")) &&
            runCatching { URI(url) }.isSuccess
        if (!valid) {
            throw WrongTypeException(
                "$platformLabel target must be an absolute http(s) URL of at most $MAX_URL_LENGTH characters",
                "Ссылка $platformLabel должна быть полным адресом (https://…)",
            )
        }
        return url
    }
}
