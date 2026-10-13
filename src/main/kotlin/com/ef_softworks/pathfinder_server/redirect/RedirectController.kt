// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.redirect

import com.ef_softworks.pathfinder_server.config.PublicUrlProperties
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * The public entry point of every printed code. Detects the device and answers a 302 to the matching target.
 * No authentication (permitAll in SecurityConfig); no caching, so every scan is counted and a changed target
 * takes effect at once.
 */
@RestController
class RedirectController(private val resolveScan: ResolveScan) {

    @GetMapping("/${PublicUrlProperties.REDIRECT_PATH}/{slug}")
    fun redirect(
        @PathVariable slug: String,
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) userAgent: String?,
    ): ResponseEntity<Void> {
        val targetUrl = resolveScan.targetUrl(slug, userAgent)
        return ResponseEntity.status(HttpStatus.FOUND)
            .cacheControl(CacheControl.noStore())
            .location(URI.create(targetUrl))
            .build()
    }
}
