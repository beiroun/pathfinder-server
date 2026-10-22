// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.redirect

import com.ef_softworks.pathfinder_server.exception.base.NotFoundException
import com.ef_softworks.pathfinder_server.qrcode.QrCode
import com.ef_softworks.pathfinder_server.qrcode.QrCodeRepository
import com.ef_softworks.pathfinder_server.scanstats.ScanJournal
import com.ef_softworks.pathfinder_server.scanstats.recordOrLog
import com.ef_softworks.pathfinder_server.util.logInfo
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Turns a scanned slug and the scanner's User-Agent into the target address, and records the scan: the counter of
 * the scanner's platform and a row of the scan journal (the statistics dashboard). A platform whose target is blank
 * falls back to the desktop target, so a redirect never resolves to an empty location.
 */
@Component
class ResolveScan(
    private val qrCodeRepository: QrCodeRepository,
    private val platformResolver: PlatformResolver,
    private val scanJournal: ScanJournal,
) {

    @Transactional
    fun targetUrl(slug: String, userAgent: String?): String {
        val qrCode = qrCodeRepository.findBySlug(slug)
            ?: throw NotFoundException("QR code not found: slug=$slug", "QR-код не найден")

        val platform = platformResolver.resolve(userAgent)
        // The platform whose link the scanner is sent to: its own, or the desktop one when it has none
        val destination = if (targetOf(qrCode, platform).isBlank()) TargetPlatform.DESKTOP else platform
        val targetUrl = targetOf(qrCode, destination)
        countScan(slug, platform)
        scanJournal.recordOrLog(qrCode.id!!, slug, platform, destination)

        logInfo("Scan redirected: slug=$slug, platform=$platform, destination=$destination, target=$targetUrl")
        return targetUrl
    }

    private fun targetOf(qrCode: QrCode, platform: TargetPlatform): String = when (platform) {
        TargetPlatform.IOS -> qrCode.targetIosUrl
        TargetPlatform.HUAWEI -> qrCode.targetHuaweiUrl
        TargetPlatform.ANDROID -> qrCode.targetAndroidUrl
        TargetPlatform.DESKTOP -> qrCode.targetDesktopUrl
    }

    private fun countScan(slug: String, platform: TargetPlatform) {
        when (platform) {
            TargetPlatform.IOS -> qrCodeRepository.incrementIosScan(slug)
            TargetPlatform.HUAWEI -> qrCodeRepository.incrementHuaweiScan(slug)
            TargetPlatform.ANDROID -> qrCodeRepository.incrementAndroidScan(slug)
            TargetPlatform.DESKTOP -> qrCodeRepository.incrementDesktopScan(slug)
        }
    }
}
