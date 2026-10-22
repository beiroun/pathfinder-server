// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import com.ef_softworks.pathfinder_server.redirect.TargetPlatform
import com.ef_softworks.pathfinder_server.util.logError
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Writes the scan journal in a transaction of its own: a failed write loses one row of the statistics, never the
 * redirect of a printed code. In the redirect's transaction a failed insert would abort it (PostgreSQL refuses every
 * statement after an error), taking the counter and the scanner's redirect with it.
 */
@Component
class ScanJournal(private val qrScanRepository: QrScanRepository) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(qrCodeId: Long, platform: TargetPlatform, destination: TargetPlatform) {
        qrScanRepository.save(QrScan(qrCodeId = qrCodeId, platform = platform, destination = destination))
    }
}

/** Records a scan, or logs the lost row: the statistics are best effort, the redirect is not. */
fun ScanJournal.recordOrLog(qrCodeId: Long, slug: String, platform: TargetPlatform, destination: TargetPlatform) {
    try {
        record(qrCodeId, platform, destination)
    } catch (exception: RuntimeException) {
        logError("Scan journal row lost: slug=$slug, platform=$platform, destination=$destination", exception)
    }
}
