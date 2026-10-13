// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The public address of this server, printed into every QR code as <base-url>/r/<slug>. It cannot be derived
 * from a request: the console calls through its own nginx, so the forwarded host would be the console's name.
 * A printed code outlives any deployment, so the value is checked at start-up instead of failing on the first scan.
 */
@ConfigurationProperties(prefix = "pathfinder.public")
data class PublicUrlProperties(
    val baseUrl: String,
) {
    /** Without a trailing slash, so the redirect path can be appended. */
    val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')

    init {
        require(normalizedBaseUrl.startsWith("https://") || normalizedBaseUrl.startsWith("http://")) {
            "PF_PUBLIC_BASE_URL must be an absolute http(s) address, e.g. https://pathfinder.example.com"
        }
    }

    /** The address a scanner lands on: the one thing a printed code carries. */
    fun redirectUrl(slug: String): String = "$normalizedBaseUrl/$REDIRECT_PATH/$slug"

    companion object {
        const val REDIRECT_PATH = "r"
    }
}
