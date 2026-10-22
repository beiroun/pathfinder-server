-- SPDX-License-Identifier: BUSL-1.1
-- Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
--
-- The scan journal behind the statistics dashboard: one row per redirect - when, from which code, the scanner's
-- platform and the platform whose link it was sent to (a platform without a link of its own goes to the desktop
-- one, so the two differ). No User-Agent, no address: nothing personal is kept. The counters on qr_code stay the
-- lifetime totals; this journal starts with the release that adds it.
CREATE SEQUENCE qr_scan_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE qr_scan (
    id          BIGINT NOT NULL DEFAULT nextval('qr_scan_seq'),
    qr_code_id  BIGINT NOT NULL,
    scanned_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    platform    VARCHAR(16) NOT NULL,
    destination VARCHAR(16) NOT NULL,
    CONSTRAINT pk_qr_scan PRIMARY KEY (id),
    -- A deleted code takes its scans with it: the dashboard only knows codes that exist
    CONSTRAINT fk_qr_scan_qr_code FOREIGN KEY (qr_code_id) REFERENCES qr_code (id) ON DELETE CASCADE,
    CONSTRAINT ck_qr_scan_platform CHECK (platform IN ('IOS', 'HUAWEI', 'ANDROID', 'DESKTOP')),
    CONSTRAINT ck_qr_scan_destination CHECK (destination IN ('IOS', 'HUAWEI', 'ANDROID', 'DESKTOP'))
);
-- Every statistics query is a time range, optionally of one code
CREATE INDEX ix_qr_scan_scanned_at ON qr_scan (scanned_at);
CREATE INDEX ix_qr_scan_qr_code_scanned_at ON qr_scan (qr_code_id, scanned_at);
