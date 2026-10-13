// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Browser origins allowed to call this API cross-site.
 *
 * The console reaches the server through its own nginx (same-origin /api/v1), so cross-site access is NOT the
 * normal path - it is for a console served from another name (built with VITE_API_BASE_URL) or a developer's
 * vite server. Empty list = no cross-site browser access at all, which is the safe default and breaks nothing in
 * the same-origin deployment.
 */
@ConfigurationProperties(prefix = "pathfinder.cors")
data class CorsProperties(
    /** Exact origins, scheme included (https://console.example.com); never a wildcard, see [CorsConfig]. */
    val allowedOrigins: List<String> = emptyList(),
)

/**
 * Global CORS policy, registered as the source Spring Security's CORS filter reads. It runs before authentication,
 * so even a 401 carries the CORS headers and a cross-site console can tell "log in again" from a blocked request.
 *
 * Credentials are allowed, and that rules out the "*" origin: the browser rejects a wildcard together with
 * credentials. So the origins stay an explicit allowlist from the environment - a deployment that forgets to set
 * it loses cross-site access, not its isolation.
 */
@Configuration
class CorsConfig(private val corsProperties: CorsProperties) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        if (corsProperties.allowedOrigins.isEmpty()) {
            return source
        }
        val policy = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With")
            allowCredentials = true
            // Preflight answers are cached for an hour; shorter means a preflight per unique request shape
            maxAge = 3600
        }
        source.registerCorsConfiguration("/**", policy)
        return source
    }
}
