// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import com.ef_softworks.pathfinder_server.redirect.TargetPlatform
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One redirect of a printed code: when, which code, the scanner's [platform] and the platform whose link it was sent
 * to ([destination] - the desktop one when the scanner's platform has no link). Nothing personal is kept.
 */
@Entity
@Table(name = "qr_scan")
class QrScan(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qr_scan_seq")
    @SequenceGenerator(name = "qr_scan_seq", sequenceName = "qr_scan_seq", allocationSize = 50)
    var id: Long? = null,

    @Column(name = "qr_code_id", nullable = false)
    var qrCodeId: Long,

    @Column(name = "scanned_at", nullable = false)
    var scannedAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    var platform: TargetPlatform,

    @Enumerated(EnumType.STRING)
    @Column(name = "destination", nullable = false, length = 16)
    var destination: TargetPlatform,
)
