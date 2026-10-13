// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import com.ef_softworks.pathfinder_server.access.AccessTokenStore
import com.ef_softworks.pathfinder_server.access.BearerTokenAuthenticationFilter
import com.ef_softworks.pathfinder_server.access.JwtTokens
import com.ef_softworks.pathfinder_server.consoleuser.ConsoleRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Who may call what.
 *
 * Stateless bearer tokens (no cookies on the server side, hence no CSRF). Public: the scan redirect itself,
 * login and token refresh, the server identity and health. The management API is role-gated: any console role
 * may read codes and download images, changing them takes an administrator. Everything not listed is denied,
 * so a new endpoint is closed until a rule names it.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtTokens: JwtTokens,
        accessTokenStore: AccessTokenStore,
        apiSecurityErrorResponses: ApiSecurityErrorResponses,
    ): SecurityFilterChain {
        val bearerTokenFilter = BearerTokenAuthenticationFilter(jwtTokens, accessTokenStore)
        http {
            // Reads the corsConfigurationSource bean (CorsConfig), so preflights and refused requests alike carry the headers
            cors { }
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                // CORS preflight carries no token
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                // The whole point of a QR code: anyone who scans it gets redirected
                authorize(HttpMethod.GET, "/${PublicUrlProperties.REDIRECT_PATH}/*", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/info", permitAll)
                authorize("/system/**", permitAll)
                authorize("/error", permitAll)

                authorize(HttpMethod.GET, "/api/v1/qr/**", hasAnyRole(ConsoleRole.ADMIN.name, ConsoleRole.VIEWER.name))
                authorize("/api/v1/qr/**", hasRole(ConsoleRole.ADMIN.name))
                authorize(anyRequest, denyAll)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(bearerTokenFilter)
            exceptionHandling {
                authenticationEntryPoint = apiSecurityErrorResponses
                accessDeniedHandler = apiSecurityErrorResponses
            }
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
