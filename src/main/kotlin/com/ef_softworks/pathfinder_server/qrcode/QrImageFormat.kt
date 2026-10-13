// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

/** Output format of a rendered QR image. PNG is the default; SVG scales losslessly for print. */
enum class QrImageFormat(val mediaType: String, val fileExtension: String) {
    PNG("image/png", "png"),
    SVG("image/svg+xml", "svg");

    companion object {
        fun from(value: String?): QrImageFormat =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PNG
    }
}
