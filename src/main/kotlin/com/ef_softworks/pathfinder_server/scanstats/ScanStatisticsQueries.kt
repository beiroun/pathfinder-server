// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

/** A half-open time range of the journal, optionally of one code. */
data class ScanSlice(val start: LocalDateTime, val end: LocalDateTime, val qrCodeId: Long?)

data class DayPlatformCount(val day: LocalDate, val platform: String, val scans: Long)
data class RouteCount(val platform: String, val destination: String, val scans: Long)
data class CodePlatformCount(val qrCodeId: Long, val platform: String, val scans: Long)
data class WeekdayHourCount(val weekday: Int, val hour: Int, val scans: Long)

/**
 * The aggregations of the scan journal, counted by PostgreSQL: the dashboard never pulls single scans. Every query
 * is a range of scanned_at (indexed), narrowed to one code when the slice says so.
 */
@Component
class ScanStatisticsQueries(private val jdbc: NamedParameterJdbcTemplate) {

    fun total(slice: ScanSlice): Long =
        jdbc.queryForObject("select count(*) from qr_scan where ${filter(slice)}", params(slice), Long::class.java) ?: 0

    fun byDayAndPlatform(slice: ScanSlice): List<DayPlatformCount> = jdbc.query(
        """
        select cast(scanned_at as date) as day, platform, count(*) as scans
        from qr_scan where ${filter(slice)}
        group by 1, 2
        """.trimIndent(),
        params(slice),
    ) { row, _ -> DayPlatformCount(row.getObject("day", LocalDate::class.java), row.getString("platform"), row.getLong("scans")) }

    fun byRoute(slice: ScanSlice): List<RouteCount> = jdbc.query(
        "select platform, destination, count(*) as scans from qr_scan where ${filter(slice)} group by 1, 2",
        params(slice),
    ) { row, _ -> RouteCount(row.getString("platform"), row.getString("destination"), row.getLong("scans")) }

    fun byCodeAndPlatform(slice: ScanSlice): List<CodePlatformCount> = jdbc.query(
        "select qr_code_id, platform, count(*) as scans from qr_scan where ${filter(slice)} group by 1, 2",
        params(slice),
    ) { row, _ -> CodePlatformCount(row.getLong("qr_code_id"), row.getString("platform"), row.getLong("scans")) }

    fun byWeekdayAndHour(slice: ScanSlice): List<WeekdayHourCount> = jdbc.query(
        """
        select cast(extract(isodow from scanned_at) as int) as weekday,
               cast(extract(hour from scanned_at) as int) as hour,
               count(*) as scans
        from qr_scan where ${filter(slice)}
        group by 1, 2
        """.trimIndent(),
        params(slice),
    ) { row, _ -> WeekdayHourCount(row.getInt("weekday"), row.getInt("hour"), row.getLong("scans")) }

    /** The oldest row of the journal, whatever the slice: where the detailed statistics begin. */
    fun firstScanAt(): LocalDateTime? =
        jdbc.queryForObject("select min(scanned_at) from qr_scan", MapSqlParameterSource(), Timestamp::class.java)
            ?.toLocalDateTime()

    // The code condition is added only when the slice has one: a "(:id is null or ...)" form leaves PostgreSQL
    // without a type for a null parameter
    private fun filter(slice: ScanSlice): String =
        "scanned_at >= :start and scanned_at < :end" + if (slice.qrCodeId != null) " and qr_code_id = :qrCodeId" else ""

    private fun params(slice: ScanSlice) = MapSqlParameterSource()
        .addValue("start", Timestamp.valueOf(slice.start))
        .addValue("end", Timestamp.valueOf(slice.end))
        .addValue("qrCodeId", slice.qrCodeId)
}
