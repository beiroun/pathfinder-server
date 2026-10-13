// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.consoleuser

import org.springframework.data.jpa.repository.JpaRepository

interface ConsoleUserRepository : JpaRepository<ConsoleUser, Long> {
    fun findByLogin(login: String): ConsoleUser?
}
