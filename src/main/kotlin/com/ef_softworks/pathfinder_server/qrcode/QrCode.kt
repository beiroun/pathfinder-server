// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * A QR code: the printed image encodes <public base url>/r/<slug>, and the redirect resolves the scanning device
 * to one of the four platform targets. The slug never changes (it is on paper); everything else may.
 * Per-platform scan counters are incremented in place on every redirect.
 */
@Entity
@Table(name = "qr_code")
class QrCode(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qr_code_seq")
    @SequenceGenerator(name = "qr_code_seq", sequenceName = "qr_code_seq", allocationSize = 50)
    var id: Long? = null,

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    var slug: String,

    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "target_android_url", nullable = false, length = 2048)
    var targetAndroidUrl: String,

    @Column(name = "target_ios_url", nullable = false, length = 2048)
    var targetIosUrl: String,

    @Column(name = "target_huawei_url", nullable = false, length = 2048)
    var targetHuaweiUrl: String,

    @Column(name = "target_desktop_url", nullable = false, length = 2048)
    var targetDesktopUrl: String,

    @Column(name = "scan_count_android", nullable = false)
    var scanCountAndroid: Long = 0,

    @Column(name = "scan_count_ios", nullable = false)
    var scanCountIos: Long = 0,

    @Column(name = "scan_count_huawei", nullable = false)
    var scanCountHuawei: Long = 0,

    @Column(name = "scan_count_desktop", nullable = false)
    var scanCountDesktop: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
