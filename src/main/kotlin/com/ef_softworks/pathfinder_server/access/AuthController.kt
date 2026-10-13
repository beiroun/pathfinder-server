// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.access

import com.ef_softworks.pathfinder_server.api.BaseApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Console sessions: login, refresh-and-retry of the access token, logout. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val consoleSessions: ConsoleSessions) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<BaseApiResponse<TokenPairResponse>> =
        ResponseEntity.ok(BaseApiResponse.ok(consoleSessions.logIn(request.username.trim(), request.password)))

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<BaseApiResponse<TokenPairResponse>> =
        ResponseEntity.ok(BaseApiResponse.ok(consoleSessions.refresh(request.refreshToken)))

    /** Revokes whatever token the request carries; a request without one is a no-op, not an error. */
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<BaseApiResponse<String>> {
        BearerTokenAuthenticationFilter.bearerToken(request)?.let(consoleSessions::logOut)
        return ResponseEntity.ok(BaseApiResponse.ok("Logged out"))
    }
}
