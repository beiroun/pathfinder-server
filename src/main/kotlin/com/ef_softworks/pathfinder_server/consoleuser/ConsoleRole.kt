// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.consoleuser

/** What a console user may do. ADMIN manages codes; VIEWER sees the list, the statistics and the images. */
enum class ConsoleRole {
    ADMIN,
    VIEWER;

    /** Spring Security authority: the hasRole() rules in SecurityConfig add the prefix themselves. */
    val authority: String get() = "ROLE_$name"
}
