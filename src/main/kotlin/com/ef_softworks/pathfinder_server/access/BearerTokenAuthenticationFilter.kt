// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.access

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Turns a valid "Authorization: Bearer" access token into the security context. A token has to pass two checks:
 * its signature and expiry, and being the user's current one (a logout or a newer login revokes the previous).
 * Anything else leaves the request anonymous, and the authorization rules decide what an anonymous caller may do.
 */
class BearerTokenAuthenticationFilter(
    private val jwtTokens: JwtTokens,
    private val accessTokenStore: AccessTokenStore,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = bearerToken(request)
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            val claims = jwtTokens.parse(token)
            if (claims != null && accessTokenStore.isActive(claims.login, token)) {
                val authentication = UsernamePasswordAuthenticationToken(
                    claims.login,
                    null,
                    listOf(SimpleGrantedAuthority(claims.role.authority)),
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "

        fun bearerToken(request: HttpServletRequest): String? {
            val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
            if (!header.startsWith(BEARER_PREFIX)) return null
            return header.substring(BEARER_PREFIX.length).trim().ifEmpty { null }
        }
    }
}
