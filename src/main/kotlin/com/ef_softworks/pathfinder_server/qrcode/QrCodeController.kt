// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import com.ef_softworks.pathfinder_server.api.BaseApiResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Management API of the console. Access is role-gated in SecurityConfig: GET for every role, changes for ADMIN. */
@RestController
@RequestMapping("/api/v1/qr")
class QrCodeController(private val qrCodeManagement: QrCodeManagement) {

    @GetMapping
    fun list(): ResponseEntity<BaseApiResponse<List<QrCodeView>>> =
        ResponseEntity.ok(BaseApiResponse.ok(qrCodeManagement.list()))

    @GetMapping("/{slug}")
    fun get(@PathVariable slug: String): ResponseEntity<BaseApiResponse<QrCodeView>> =
        ResponseEntity.ok(BaseApiResponse.ok(qrCodeManagement.get(slug)))

    @PostMapping
    fun create(@RequestBody request: CreateQrCodeRequest): ResponseEntity<BaseApiResponse<QrCodeView>> =
        ResponseEntity.ok(BaseApiResponse.ok(qrCodeManagement.create(request)))

    @PutMapping("/{slug}")
    fun update(
        @PathVariable slug: String,
        @RequestBody request: UpdateQrCodeRequest,
    ): ResponseEntity<BaseApiResponse<QrCodeView>> =
        ResponseEntity.ok(BaseApiResponse.ok(qrCodeManagement.update(slug, request)))

    @DeleteMapping("/{slug}")
    fun delete(@PathVariable slug: String): ResponseEntity<BaseApiResponse<String>> {
        qrCodeManagement.delete(slug)
        return ResponseEntity.ok(BaseApiResponse.ok("Deleted"))
    }

    /** The image to print or embed; the console fetches it with its token and shows it as a blob. */
    @GetMapping("/{slug}/image")
    fun image(
        @PathVariable slug: String,
        @RequestParam(name = "format", defaultValue = "png") format: String,
        @RequestParam(name = "size", defaultValue = "512") size: Int,
    ): ResponseEntity<ByteArray> {
        val imageFormat = QrImageFormat.from(format)
        val bytes = qrCodeManagement.renderImage(slug, imageFormat, size)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageFormat.mediaType))
            .body(bytes)
    }
}
