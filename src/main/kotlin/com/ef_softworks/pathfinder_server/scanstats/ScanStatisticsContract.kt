// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The statistics of one period, everything the dashboard draws in one answer. Platforms are lower case keys
 * (ios, huawei, android, desktop); days and hours are the server's local time, like every timestamp of the API.
 */
data class ScanReport(
    @JsonProperty("from") val from: LocalDate,
    @JsonProperty("to") val to: LocalDate,
    /** The code the report is narrowed to; null - every code. */
    @JsonProperty("slug") val slug: String?,
    /** The first scan the journal holds: the period before it has only the lifetime counters, no detail. */
    @JsonProperty("tracking_since") val trackingSince: LocalDateTime?,
    @JsonProperty("total") val total: Long,
    /** The same number of days right before [from], for the change of the period. */
    @JsonProperty("previous_total") val previousTotal: Long,
    @JsonProperty("by_day") val byDay: List<DayScans>,
    @JsonProperty("by_route") val byRoute: List<RouteScans>,
    @JsonProperty("by_code") val byCode: List<CodeScans>,
    @JsonProperty("by_weekday_hour") val byWeekdayHour: List<WeekdayHourScans>,
)

/** One calendar day of the period, every day present (zeros included), split by the scanner's platform. */
data class DayScans(
    @JsonProperty("day") val day: LocalDate,
    @JsonProperty("ios") val ios: Long,
    @JsonProperty("huawei") val huawei: Long,
    @JsonProperty("android") val android: Long,
    @JsonProperty("desktop") val desktop: Long,
    @JsonProperty("total") val total: Long,
)

/** Where scanners of one platform were sent: [destination] differs from [platform] when its own link is missing. */
data class RouteScans(
    @JsonProperty("platform") val platform: String,
    @JsonProperty("destination") val destination: String,
    @JsonProperty("scans") val scans: Long,
)

/** One code over the period, split by the scanner's platform; codes without scans are listed too. */
data class CodeScans(
    @JsonProperty("slug") val slug: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("scans") val scans: Long,
    @JsonProperty("ios") val ios: Long,
    @JsonProperty("huawei") val huawei: Long,
    @JsonProperty("android") val android: Long,
    @JsonProperty("desktop") val desktop: Long,
)

/** When people scan: ISO weekday (1 = Monday) and hour of the day; only cells with scans. */
data class WeekdayHourScans(
    @JsonProperty("weekday") val weekday: Int,
    @JsonProperty("hour") val hour: Int,
    @JsonProperty("scans") val scans: Long,
)
