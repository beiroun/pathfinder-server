// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.qrcode

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/** Create request. [slug] is optional - a unique one is generated when omitted. */
data class CreateQrCodeRequest(
    @JsonProperty("name") val name: String,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("target_android_url") val targetAndroidUrl: String = "",
    @JsonProperty("target_ios_url") val targetIosUrl: String = "",
    @JsonProperty("target_huawei_url") val targetHuaweiUrl: String = "",
    @JsonProperty("target_desktop_url") val targetDesktopUrl: String,
)

/** Update request. The slug is immutable (it is printed), so only the name and the four targets can change. */
data class UpdateQrCodeRequest(
    @JsonProperty("name") val name: String,
    @JsonProperty("target_android_url") val targetAndroidUrl: String = "",
    @JsonProperty("target_ios_url") val targetIosUrl: String = "",
    @JsonProperty("target_huawei_url") val targetHuaweiUrl: String = "",
    @JsonProperty("target_desktop_url") val targetDesktopUrl: String,
)

/** What the console sees: the code, its public redirect address and the scan statistics. */
data class QrCodeView(
    @JsonProperty("slug") val slug: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("redirect_url") val redirectUrl: String,
    @JsonProperty("target_android_url") val targetAndroidUrl: String,
    @JsonProperty("target_ios_url") val targetIosUrl: String,
    @JsonProperty("target_huawei_url") val targetHuaweiUrl: String,
    @JsonProperty("target_desktop_url") val targetDesktopUrl: String,
    @JsonProperty("scan_count_android") val scanCountAndroid: Long,
    @JsonProperty("scan_count_ios") val scanCountIos: Long,
    @JsonProperty("scan_count_huawei") val scanCountHuawei: Long,
    @JsonProperty("scan_count_desktop") val scanCountDesktop: Long,
    @JsonProperty("scan_count_total") val scanCountTotal: Long,
    @JsonProperty("created_at") val createdAt: LocalDateTime,
    @JsonProperty("updated_at") val updatedAt: LocalDateTime,
)
