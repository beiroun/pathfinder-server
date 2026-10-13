-- SPDX-License-Identifier: BUSL-1.1
-- Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
--
-- Pathfinder schema: QR codes with their four platform targets and scan counters, console users and their
-- refresh tokens. Conventions: BIGINT ids from a sequence with INCREMENT BY 50 (Hibernate pooled allocation),
-- snake_case, created_at on every table.

-- Console users. The environment is the source of truth (PF_ADMIN_USERS / PF_VIEWER_USERS): the server upserts
-- these rows on every start, so the table only adds the BCrypt hash and the audit timestamps to what .env says.
CREATE SEQUENCE console_user_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE console_user (
    id            BIGINT NOT NULL DEFAULT nextval('console_user_seq'),
    login         VARCHAR(64) NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    role          VARCHAR(16) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_console_user PRIMARY KEY (id),
    CONSTRAINT uq_console_user_login UNIQUE (login),
    CONSTRAINT ck_console_user_role CHECK (role IN ('ADMIN', 'VIEWER'))
);

-- One refresh token per user: a new login or refresh replaces the row, a logout deletes it.
CREATE SEQUENCE refresh_token_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE refresh_token (
    id         BIGINT NOT NULL DEFAULT nextval('refresh_token_seq'),
    login      VARCHAR(64) NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_login UNIQUE (login)
);

-- A QR code: the slug is the public identity printed into the code (<base-url>/r/<slug>) and never changes;
-- the four targets and the name do. Scan counters are incremented in place on the redirect path.
CREATE SEQUENCE qr_code_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE qr_code (
    id                 BIGINT NOT NULL DEFAULT nextval('qr_code_seq'),
    slug               VARCHAR(64) NOT NULL,
    name               VARCHAR(128) NOT NULL,
    target_android_url VARCHAR(2048) NOT NULL,
    target_ios_url     VARCHAR(2048) NOT NULL,
    target_huawei_url  VARCHAR(2048) NOT NULL,
    target_desktop_url VARCHAR(2048) NOT NULL,
    scan_count_android BIGINT NOT NULL DEFAULT 0,
    scan_count_ios     BIGINT NOT NULL DEFAULT 0,
    scan_count_huawei  BIGINT NOT NULL DEFAULT 0,
    scan_count_desktop BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_qr_code PRIMARY KEY (id),
    CONSTRAINT uq_qr_code_slug UNIQUE (slug)
);
