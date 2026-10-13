// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Builds the User-Agent analyzer once (it is expensive to construct) and limits it to the fields the platform
 * resolution needs, with an LRU cache for the hot redirect path.
 */
@Configuration
class UserAgentAnalyzerConfig {

    @Bean
    fun userAgentAnalyzer(): UserAgentAnalyzer =
        UserAgentAnalyzer.newBuilder()
            .hideMatcherLoadStats()
            .withField(UserAgent.OPERATING_SYSTEM_NAME)
            .withField(UserAgent.DEVICE_BRAND)
            .withField(UserAgent.DEVICE_CLASS)
            .withCache(10_000)
            .build()
}
