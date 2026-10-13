// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.api

/**
 * Envelope of every API answer.
 * On success [data] is populated, [message] is "OK" and [error] is null.
 * On failure [data] is null, [message] is "FAIL" and [error] carries the two-part message.
 */
data class BaseApiResponse<T>(
    val data: T? = null,
    val message: String = "OK",
    val error: FieldErrorDto? = null,
) {
    companion object {
        fun <T> ok(data: T): BaseApiResponse<T> = BaseApiResponse(data = data)

        fun fail(error: FieldErrorDto): BaseApiResponse<Nothing?> =
            BaseApiResponse(data = null, message = "FAIL", error = error)
    }
}

/** Structured error payload: technical [message] for the log and the developer, [localizedMessage] for the operator. */
data class FieldErrorDto(
    val message: String,
    val localizedMessage: String,
)
