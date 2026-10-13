// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QrCodeRepository : JpaRepository<QrCode, Long> {

    fun findBySlug(slug: String): QrCode?
    fun existsBySlug(slug: String): Boolean
    fun findAllByOrderByCreatedAtDesc(): List<QrCode>

    // Atomic per-platform scan increments: no read-modify-write race on the hot redirect path

    @Modifying
    @Query("update QrCode q set q.scanCountAndroid = q.scanCountAndroid + 1 where q.slug = :slug")
    fun incrementAndroidScan(@Param("slug") slug: String)

    @Modifying
    @Query("update QrCode q set q.scanCountIos = q.scanCountIos + 1 where q.slug = :slug")
    fun incrementIosScan(@Param("slug") slug: String)

    @Modifying
    @Query("update QrCode q set q.scanCountHuawei = q.scanCountHuawei + 1 where q.slug = :slug")
    fun incrementHuaweiScan(@Param("slug") slug: String)

    @Modifying
    @Query("update QrCode q set q.scanCountDesktop = q.scanCountDesktop + 1 where q.slug = :slug")
    fun incrementDesktopScan(@Param("slug") slug: String)
}
