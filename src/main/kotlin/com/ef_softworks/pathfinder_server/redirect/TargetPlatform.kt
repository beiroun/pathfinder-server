// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.redirect

/**
 * Device platform a scan resolves to. DESKTOP is also the fallback for unknown or non-mobile devices and bots.
 * Resolution priority is IOS -> HUAWEI -> ANDROID -> DESKTOP.
 */
enum class TargetPlatform {
    IOS,
    HUAWEI,
    ANDROID,
    DESKTOP,
}
