// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.scanstats

import com.ef_softworks.pathfinder_server.api.BaseApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** The statistics of the console's dashboard; every role reads it (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/stats")
class ScanStatisticsController(private val scanStatistics: ScanStatistics) {

    /** [from] and [to] are calendar days (YYYY-MM-DD, both included; default - the last 30 days), [slug] narrows to one code. */
    @GetMapping("/scans")
    fun scans(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) slug: String?,
    ): ResponseEntity<BaseApiResponse<ScanReport>> =
        ResponseEntity.ok(BaseApiResponse.ok(scanStatistics.report(from, to, slug)))
}
