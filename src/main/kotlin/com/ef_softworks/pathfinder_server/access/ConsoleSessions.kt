// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.access

import com.ef_softworks.pathfinder_server.config.ConsoleAccessProperties
import com.ef_softworks.pathfinder_server.consoleuser.ConsoleUser
import com.ef_softworks.pathfinder_server.consoleuser.ConsoleUserRepository
import com.ef_softworks.pathfinder_server.exception.base.UnauthorizedException
import com.ef_softworks.pathfinder_server.util.logInfo
import com.ef_softworks.pathfinder_server.util.logWarn
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

/**
 * Login, refresh and logout of the console.
 *
 * Two tokens: a short-lived access token (kept in memory, see [AccessTokenStore]) and a long-lived refresh token
 * (one row per user in the database). A refresh re-issues both, so a stolen refresh token stops working the
 * moment its owner refreshes; a logout revokes both.
 */
@Component
class ConsoleSessions(
    private val consoleUserRepository: ConsoleUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokens: JwtTokens,
    private val accessTokenStore: AccessTokenStore,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val consoleAccessProperties: ConsoleAccessProperties,
) {

    @Transactional
    fun logIn(login: String, rawPassword: String): TokenPairResponse {
        val user = consoleUserRepository.findByLogin(login)
        // One message for every failure: the login form must not reveal which logins exist
        if (user == null || !user.enabled || !passwordEncoder.matches(rawPassword, user.passwordHash)) {
            logWarn("Login refused: login=$login")
            throw UnauthorizedException("Invalid credentials", "Неверный логин или пароль")
        }
        logInfo("Console login: login=$login, role=${user.role}")
        return issueTokens(user)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPairResponse {
        val claims = jwtTokens.parse(refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token", "Сессия истекла, войдите снова")

        val stored = refreshTokenRepository.findByLogin(claims.login)
        if (stored == null || stored.token != refreshToken) {
            throw UnauthorizedException("Refresh token is not the current one", "Сессия истекла, войдите снова")
        }
        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored)
            throw UnauthorizedException("Refresh token expired", "Сессия истекла, войдите снова")
        }

        val user = consoleUserRepository.findByLogin(claims.login)
        if (user == null || !user.enabled) {
            refreshTokenRepository.delete(stored)
            throw UnauthorizedException("User is disabled", "Доступ отключён")
        }
        return issueTokens(user)
    }

    @Transactional
    fun logOut(accessToken: String) {
        val claims = jwtTokens.parse(accessToken) ?: return
        accessTokenStore.revoke(claims.login)
        refreshTokenRepository.deleteByLogin(claims.login)
        logInfo("Console logout: login=${claims.login}")
    }

    private fun issueTokens(user: ConsoleUser): TokenPairResponse {
        val claims = TokenClaims(user.login, user.role, user.displayName)
        val accessTtl = consoleAccessProperties.accessTokenTtl
        val refreshTtl = consoleAccessProperties.refreshTokenTtl

        val accessToken = jwtTokens.issue(claims, accessTtl)
        val refreshToken = jwtTokens.issue(claims, refreshTtl)

        accessTokenStore.put(user.login, accessToken, Instant.now().plus(accessTtl))

        val expiresAt = LocalDateTime.now().plus(refreshTtl)
        val stored = refreshTokenRepository.findByLogin(user.login)
        if (stored == null) {
            refreshTokenRepository.save(RefreshToken(login = user.login, token = refreshToken, expiresAt = expiresAt))
        } else {
            stored.token = refreshToken
            stored.expiresAt = expiresAt
            refreshTokenRepository.save(stored)
        }
        return TokenPairResponse(accessToken, refreshToken)
    }
}
