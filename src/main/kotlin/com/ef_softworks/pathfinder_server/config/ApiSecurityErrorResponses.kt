// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import com.ef_softworks.pathfinder_server.api.BaseApiResponse
import com.ef_softworks.pathfinder_server.api.FieldErrorDto
import com.ef_softworks.pathfinder_server.util.logWarn
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * Bodies of the refusals the security filters produce before any controller runs - without this they would be
 * empty, and the console could not tell "log in again" from "not allowed".
 *
 * 401 = no valid session (missing, expired or revoked access token): the console refreshes and retries, or shows
 * the login. 403 = logged in, but the role may not do this.
 */
@Component
class ApiSecurityErrorResponses(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint, AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        logWarn("Unauthenticated ${request.method} ${request.requestURI}: ${authException.message}")
        write(response, HttpServletResponse.SC_UNAUTHORIZED, authException.message ?: "Authentication required", "Войдите в консоль")
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        logWarn("Access denied: ${request.method} ${request.requestURI} for ${request.userPrincipal?.name}")
        write(response, HttpServletResponse.SC_FORBIDDEN, accessDeniedException.message ?: "Access denied", "Недостаточно прав для этого действия")
    }

    private fun write(response: HttpServletResponse, status: Int, message: String, localizedMessage: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, BaseApiResponse.fail(FieldErrorDto(message, localizedMessage)))
    }
}
