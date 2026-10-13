// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.exception.base

/**
 * Base of the business exceptions the global handler turns into an API answer. Carries a technical [message] for
 * the log, a [localMessage] for the operator and the HTTP [statusCode] of the category.
 */
open class ApiException(
    override val message: String,
    val localMessage: String,
    val statusCode: Int,
) : RuntimeException(message)

/** The requested entity does not exist. */
class NotFoundException(message: String, localMessage: String) : ApiException(message, localMessage, 404)

/** The input failed validation: wrong type, format or value. */
class WrongTypeException(message: String, localMessage: String) : ApiException(message, localMessage, 400)

/** The input collides with an existing record (a slug already taken). */
class ConflictException(message: String, localMessage: String) : ApiException(message, localMessage, 409)

/** Credentials or a token the console presented are not valid. */
class UnauthorizedException(message: String, localMessage: String) : ApiException(message, localMessage, 401)
