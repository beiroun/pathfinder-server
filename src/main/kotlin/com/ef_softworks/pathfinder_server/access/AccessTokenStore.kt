// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.access

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * The live access tokens, one per user: a new login or refresh replaces the previous one, so a user has exactly
 * one active session, and a logout revokes the token before it expires.
 *
 * Kept in memory on purpose. The server runs as a single instance, and a restart costs nothing visible: the
 * console's next request gets a 401, refreshes with the refresh token (which lives in the database) and carries on.
 */
@Component
class AccessTokenStore {

    private class ActiveToken(val token: String, val expiresAt: Instant)

    private val activeTokensByLogin = ConcurrentHashMap<String, ActiveToken>()

    fun put(login: String, token: String, expiresAt: Instant) {
        activeTokensByLogin[login] = ActiveToken(token, expiresAt)
    }

    fun isActive(login: String, token: String): Boolean {
        val active = activeTokensByLogin[login] ?: return false
        if (active.expiresAt.isBefore(Instant.now())) {
            activeTokensByLogin.remove(login, active)
            return false
        }
        return active.token == token
    }

    fun revoke(login: String) {
        activeTokensByLogin.remove(login)
    }

    // Expired entries of users who never came back would otherwise stay until the next restart
    @Scheduled(fixedDelay = 600_000)
    fun dropExpired() {
        val now = Instant.now()
        activeTokensByLogin.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }
}
