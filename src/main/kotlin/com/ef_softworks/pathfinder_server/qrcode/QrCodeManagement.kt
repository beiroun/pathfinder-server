// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import com.ef_softworks.pathfinder_server.config.PublicUrlProperties
import com.ef_softworks.pathfinder_server.exception.base.ConflictException
import com.ef_softworks.pathfinder_server.exception.base.NotFoundException
import com.ef_softworks.pathfinder_server.util.logInfo
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

private const val SLUG_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private const val GENERATED_SLUG_LENGTH = 10
private const val MIN_IMAGE_SIZE = 64
private const val MAX_IMAGE_SIZE = 2048

/**
 * Management of QR codes: list, create, update, delete and the image. The slug is generated when not supplied
 * and is the stable public identity; the encoded content is always <public base url>/r/<slug>.
 */
@Component
class QrCodeManagement(
    private val qrCodeRepository: QrCodeRepository,
    private val qrImageRenderer: QrImageRenderer,
    private val publicUrlProperties: PublicUrlProperties,
) {
    private val random = SecureRandom()

    fun list(): List<QrCodeView> = qrCodeRepository.findAllByOrderByCreatedAtDesc().map { it.toView() }

    fun get(slug: String): QrCodeView = load(slug).toView()

    @Transactional
    fun create(request: CreateQrCodeRequest): QrCodeView {
        val slug = QrCodeInputRules.optionalSlug(request.slug)
            ?.also { requireAvailableSlug(it) }
            ?: generateUniqueSlug()

        val saved = qrCodeRepository.save(
            QrCode(
                slug = slug,
                name = QrCodeInputRules.name(request.name),
                targetAndroidUrl = QrCodeInputRules.optionalTarget(request.targetAndroidUrl, "Android"),
                targetIosUrl = QrCodeInputRules.optionalTarget(request.targetIosUrl, "iOS"),
                targetHuaweiUrl = QrCodeInputRules.optionalTarget(request.targetHuaweiUrl, "Huawei"),
                targetDesktopUrl = QrCodeInputRules.desktopTarget(request.targetDesktopUrl),
            )
        )
        logInfo("QR code created: slug=${saved.slug}, name=${saved.name}")
        return saved.toView()
    }

    @Transactional
    fun update(slug: String, request: UpdateQrCodeRequest): QrCodeView {
        val qrCode = load(slug)
        qrCode.name = QrCodeInputRules.name(request.name)
        qrCode.targetAndroidUrl = QrCodeInputRules.optionalTarget(request.targetAndroidUrl, "Android")
        qrCode.targetIosUrl = QrCodeInputRules.optionalTarget(request.targetIosUrl, "iOS")
        qrCode.targetHuaweiUrl = QrCodeInputRules.optionalTarget(request.targetHuaweiUrl, "Huawei")
        qrCode.targetDesktopUrl = QrCodeInputRules.desktopTarget(request.targetDesktopUrl)
        qrCode.updatedAt = LocalDateTime.now()
        val saved = qrCodeRepository.save(qrCode)
        logInfo("QR code updated: slug=$slug")
        return saved.toView()
    }

    @Transactional
    fun delete(slug: String) {
        val qrCode = load(slug)
        qrCodeRepository.delete(qrCode)
        // Printed copies of this code now lead to a 404 - worth a line in the log when somebody asks why
        logInfo("QR code deleted: slug=$slug, name=${qrCode.name}")
    }

    fun renderImage(slug: String, format: QrImageFormat, size: Int): ByteArray {
        load(slug) // 404 for an unknown slug before anything is rendered
        val boundedSize = size.coerceIn(MIN_IMAGE_SIZE, MAX_IMAGE_SIZE)
        return qrImageRenderer.render(publicUrlProperties.redirectUrl(slug), format, boundedSize)
    }

    private fun load(slug: String): QrCode =
        qrCodeRepository.findBySlug(slug)
            ?: throw NotFoundException("QR code not found: slug=$slug", "QR-код не найден")

    private fun requireAvailableSlug(slug: String) {
        if (qrCodeRepository.existsBySlug(slug)) {
            throw ConflictException("Slug already taken: $slug", "Слаг уже занят")
        }
    }

    private fun generateUniqueSlug(): String {
        var candidate = randomSlug()
        while (qrCodeRepository.existsBySlug(candidate)) {
            candidate = randomSlug()
        }
        return candidate
    }

    private fun randomSlug(): String =
        buildString { repeat(GENERATED_SLUG_LENGTH) { append(SLUG_ALPHABET[random.nextInt(SLUG_ALPHABET.length)]) } }

    private fun QrCode.toView(): QrCodeView = QrCodeView(
        slug = slug,
        name = name,
        redirectUrl = publicUrlProperties.redirectUrl(slug),
        targetAndroidUrl = targetAndroidUrl,
        targetIosUrl = targetIosUrl,
        targetHuaweiUrl = targetHuaweiUrl,
        targetDesktopUrl = targetDesktopUrl,
        scanCountAndroid = scanCountAndroid,
        scanCountIos = scanCountIos,
        scanCountHuawei = scanCountHuawei,
        scanCountDesktop = scanCountDesktop,
        scanCountTotal = scanCountAndroid + scanCountIos + scanCountHuawei + scanCountDesktop,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
