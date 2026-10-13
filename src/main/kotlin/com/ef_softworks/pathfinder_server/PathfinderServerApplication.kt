// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/** Pathfinder: one QR code, the right destination for every device, plus the management API of the console. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class PathfinderServerApplication

fun main(args: Array<String>) {
    runApplication<PathfinderServerApplication>(*args)
}
