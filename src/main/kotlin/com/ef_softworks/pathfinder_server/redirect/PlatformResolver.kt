// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.redirect

import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Component

/**
 * Resolves a device platform from the raw User-Agent.
 *
 * Priority is critical and must stay in this order: iOS -> Huawei -> Android -> desktop. Huawei / Honor / HarmonyOS
 * is checked BEFORE generic Android because those devices also carry "Android" in the UA - reversing the order
 * would route every Huawei to the Android target. iOS is matched by OS name only (not device brand "Apple"), so a
 * Mac never lands in the iOS bucket. Anything unrecognised (desktop, bots, blank UA) falls back to DESKTOP.
 */
@Component
class PlatformResolver(private val userAgentAnalyzer: UserAgentAnalyzer) {

    fun resolve(userAgent: String?): TargetPlatform {
        if (userAgent.isNullOrBlank()) return TargetPlatform.DESKTOP

        val parsed = userAgentAnalyzer.parse(userAgent)
        val osName = parsed.getValue(UserAgent.OPERATING_SYSTEM_NAME).orEmpty()
        val deviceBrand = parsed.getValue(UserAgent.DEVICE_BRAND).orEmpty()

        return when {
            isIos(osName) -> TargetPlatform.IOS
            isHuawei(osName, deviceBrand) -> TargetPlatform.HUAWEI
            isAndroid(osName) -> TargetPlatform.ANDROID
            else -> TargetPlatform.DESKTOP
        }
    }

    private fun isIos(osName: String): Boolean =
        osName.startsWith("iOS", ignoreCase = true) || osName.equals("iPadOS", ignoreCase = true)

    private fun isHuawei(osName: String, deviceBrand: String): Boolean =
        deviceBrand.equals("Huawei", ignoreCase = true) ||
            deviceBrand.equals("Honor", ignoreCase = true) ||
            osName.contains("HarmonyOS", ignoreCase = true)

    private fun isAndroid(osName: String): Boolean =
        osName.contains("Android", ignoreCase = true)
}
