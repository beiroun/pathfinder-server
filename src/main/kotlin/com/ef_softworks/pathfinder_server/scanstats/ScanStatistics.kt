// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import com.ef_softworks.pathfinder_server.exception.base.NotFoundException
import com.ef_softworks.pathfinder_server.exception.base.WrongTypeException
import com.ef_softworks.pathfinder_server.qrcode.QrCodeRepository
import com.ef_softworks.pathfinder_server.redirect.TargetPlatform
import com.ef_softworks.pathfinder_server.util.logDebug
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** The dashboard's default period: the last 30 days, today included. */
private const val DEFAULT_PERIOD_DAYS = 30L

/** A year and a day: a whole calendar year with a leap day still fits, a multi-year range does not. */
private const val MAX_PERIOD_DAYS = 366L

/**
 * The statistics of a period for the dashboard: totals with the previous period of the same length, scans by day,
 * by route (platform -> whose link), by code and by weekday and hour. Days are the server's calendar days.
 */
@Component
class ScanStatistics(
    private val queries: ScanStatisticsQueries,
    private val qrCodeRepository: QrCodeRepository,
) {

    @Transactional(readOnly = true)
    fun report(fromParam: String?, toParam: String?, slug: String?): ScanReport {
        val to = parseDay(toParam, "to") ?: LocalDate.now()
        val from = parseDay(fromParam, "from") ?: to.minusDays(DEFAULT_PERIOD_DAYS - 1)
        if (from.isAfter(to)) {
            throw WrongTypeException("Statistics period from=$from is after to=$to", "Начало периода позже конца")
        }
        val days = ChronoUnit.DAYS.between(from, to) + 1
        if (days > MAX_PERIOD_DAYS) {
            throw WrongTypeException("Statistics period of $days days", "Период длиннее $MAX_PERIOD_DAYS дней")
        }

        val code = slug?.takeIf { it.isNotBlank() }?.let {
            qrCodeRepository.findBySlug(it) ?: throw NotFoundException("QR code not found: slug=$it", "QR-код не найден")
        }
        val slice = ScanSlice(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), code?.id)
        val previous = ScanSlice(slice.start.minusDays(days), slice.start, code?.id)
        logDebug("Scan report: from=$from, to=$to, slug=${code?.slug}")

        val codes = if (code != null) listOf(code) else qrCodeRepository.findAllByOrderByCreatedAtDesc()
        val byCode = queries.byCodeAndPlatform(slice).groupBy { it.qrCodeId }

        return ScanReport(
            from = from,
            to = to,
            slug = code?.slug,
            trackingSince = queries.firstScanAt(),
            total = queries.total(slice),
            previousTotal = queries.total(previous),
            byDay = byDay(from, to, queries.byDayAndPlatform(slice)),
            byRoute = queries.byRoute(slice)
                .map { RouteScans(it.platform.lowercase(), it.destination.lowercase(), it.scans) }
                .sortedByDescending { it.scans },
            byCode = codes
                .map { qrCode ->
                    val counts = byCode[qrCode.id].orEmpty().associate { it.platform to it.scans }
                    CodeScans(
                        slug = qrCode.slug,
                        name = qrCode.name,
                        scans = counts.values.sum(),
                        ios = counts[TargetPlatform.IOS.name] ?: 0,
                        huawei = counts[TargetPlatform.HUAWEI.name] ?: 0,
                        android = counts[TargetPlatform.ANDROID.name] ?: 0,
                        desktop = counts[TargetPlatform.DESKTOP.name] ?: 0,
                    )
                }
                .sortedWith(compareByDescending<CodeScans> { it.scans }.thenBy { it.name }),
            byWeekdayHour = queries.byWeekdayAndHour(slice).map { WeekdayHourScans(it.weekday, it.hour, it.scans) },
        )
    }

    // Every day of the period, zeros included: a chart with gaps in the axis would hide the quiet days
    private fun byDay(from: LocalDate, to: LocalDate, counts: List<DayPlatformCount>): List<DayScans> {
        val byDay = counts.groupBy { it.day }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { day ->
                val perPlatform = byDay[day].orEmpty().associate { it.platform to it.scans }
                DayScans(
                    day = day,
                    ios = perPlatform[TargetPlatform.IOS.name] ?: 0,
                    huawei = perPlatform[TargetPlatform.HUAWEI.name] ?: 0,
                    android = perPlatform[TargetPlatform.ANDROID.name] ?: 0,
                    desktop = perPlatform[TargetPlatform.DESKTOP.name] ?: 0,
                    total = perPlatform.values.sum(),
                )
            }
            .toList()
    }

    private fun parseDay(value: String?, name: String): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw WrongTypeException("Statistics $name=$value is not YYYY-MM-DD", "Дата периода в формате ГГГГ-ММ-ДД")
        }
    }
}
