// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Renders a QR image for arbitrary text content. PNG comes straight from the encoder; SVG is built from the bit
 * matrix so it prints at any scale.
 */
@Component
class QrImageRenderer {

    fun render(content: String, format: QrImageFormat, size: Int): ByteArray =
        when (format) {
            QrImageFormat.PNG -> renderPng(content, size)
            QrImageFormat.SVG -> renderSvg(content, size).toByteArray(StandardCharsets.UTF_8)
        }

    private fun encode(content: String, size: Int): BitMatrix {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        )
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    }

    private fun renderPng(content: String, size: Int): ByteArray {
        val matrix = encode(content, size)
        val output = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", output)
        return output.toByteArray()
    }

    private fun renderSvg(content: String, size: Int): String {
        val matrix = encode(content, size)
        val width = matrix.width
        val height = matrix.height

        val path = StringBuilder()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix.get(x, y)) {
                    path.append("M").append(x).append(" ").append(y).append("h1v1h-1z")
                }
            }
        }

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""")
            append(
                """<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" """ +
                    """viewBox="0 0 $width $height" shape-rendering="crispEdges">"""
            )
            append("""<rect width="$width" height="$height" fill="#ffffff"/>""")
            append("""<path fill="#000000" d="$path"/>""")
            append("</svg>")
        }
    }
}
