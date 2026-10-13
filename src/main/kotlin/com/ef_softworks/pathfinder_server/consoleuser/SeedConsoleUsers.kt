// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.consoleuser

import com.ef_softworks.pathfinder_server.config.ConsoleAccessProperties
import com.ef_softworks.pathfinder_server.config.SeededConsoleUser
import com.ef_softworks.pathfinder_server.util.logInfo
import com.ef_softworks.pathfinder_server.util.logWarn
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Keeps the console_user table in step with the environment on every start.
 *
 * Upsert semantics: a login new to the table is created; an existing one gets its password hash, role and enabled
 * flag refreshed from the environment. A login present in the table but absent from the environment is disabled
 * (it cannot log in until it is listed again) - the environment is the only source of truth, and it always holds
 * at least one administrator, so nobody can lock themselves out this way.
 */
@Component
class SeedConsoleUsers(
    private val consoleUserRepository: ConsoleUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val consoleAccessProperties: ConsoleAccessProperties,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        val seeded = consoleAccessProperties.seededUsers
        seeded.forEach(::upsert)
        disableAbsent(seeded.map { it.login }.toSet())
    }

    private fun upsert(seed: SeededConsoleUser) {
        val existing = consoleUserRepository.findByLogin(seed.login)
        if (existing == null) {
            consoleUserRepository.save(
                ConsoleUser(
                    login = seed.login,
                    passwordHash = hash(seed.password),
                    displayName = seed.login,
                    role = seed.role,
                )
            )
            logInfo("Console user created from the environment: login=${seed.login}, role=${seed.role}")
            return
        }

        var changed = false
        if (!passwordEncoder.matches(seed.password, existing.passwordHash)) {
            existing.passwordHash = hash(seed.password)
            changed = true
        }
        if (existing.role != seed.role) {
            existing.role = seed.role
            changed = true
        }
        if (!existing.enabled) {
            existing.enabled = true
            changed = true
        }
        if (changed) {
            consoleUserRepository.save(existing)
            logInfo("Console user updated from the environment: login=${seed.login}, role=${seed.role}")
        }
    }

    // The encoder's contract allows a null answer; for BCrypt it never happens, and a null hash must not reach the table
    private fun hash(rawPassword: String): String =
        passwordEncoder.encode(rawPassword) ?: error("Password encoder returned no hash")

    private fun disableAbsent(seededLogins: Set<String>) {
        consoleUserRepository.findAll()
            .filter { it.enabled && it.login !in seededLogins }
            .forEach { user ->
                user.enabled = false
                consoleUserRepository.save(user)
                logWarn("Console user disabled: login=${user.login} is no longer listed in PF_ADMIN_USERS / PF_VIEWER_USERS")
            }
    }
}
