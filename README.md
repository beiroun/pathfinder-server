# Pathfinder Server

The server of [Pathfinder](https://github.com/beiroun/pathfinder) by
[84softworks](https://84softworks.com): one printed QR code, the right
destination for every device. It answers the public redirect every code points
at, tells iOS from Huawei from Android by the User-Agent, counts the scans,
renders the QR images and exposes the management API the
[console](https://github.com/beiroun/pathfinder-console) uses.

---

## What it does

- **Redirect.** `GET /r/<slug>` resolves the scanning device and answers a
  `302` to one of the code's four targets. Resolution order is iOS → Huawei /
  Honor / HarmonyOS → Android → everything else (desktop, bots, unknown). A
  blank platform target falls back to the desktop one. The answer is
  `Cache-Control: no-store`, so every scan is counted and a changed target takes
  effect at once.
- **Codes.** Create, edit, delete; the slug is generated (10 characters) or
  typed once and never changes - it is on paper. Targets are validated when
  saved (absolute `http(s)` addresses), never discovered broken by a scan.
- **Images.** PNG at any size from 64 to 2048 px, or SVG for print. The image
  always encodes `<PF_PUBLIC_BASE_URL>/r/<slug>`.
- **Statistics.** Per-platform scan counters, incremented atomically on the
  redirect path.
- **Console access.** Logins come from the environment (`PF_ADMIN_USERS`,
  `PF_VIEWER_USERS`); the server keeps the accounts in step on every start.
  Sessions are a short-lived access token (in memory, one per user) and a
  refresh token (in the database): a logout or a new login revokes the previous
  session.

## Stack

| | |
|---|---|
| Language / runtime | Kotlin 2.3, Java 25 (LTS) |
| Framework | Spring Boot 4.1 — Spring MVC, Spring Data JPA, Spring Security (BCrypt, bearer filter) |
| Tokens | JJWT 0.13, HS256 |
| Device detection | Yauaa 8 |
| QR | ZXing 3.5 |
| Database | PostgreSQL 18, Flyway migrations |
| Build | Gradle 9.8 (Kotlin DSL), `group = "com.ef_softworks"` |
| Package | `com.ef_softworks.pathfinder_server` |
| Image | multi-stage Gradle → Eclipse Temurin 25 JRE, published as `ghcr.io/beiroun/pathfinder-server:<version>` on a tagged release |

Code conventions: self-describing packages by domain (`redirect`, `qrcode`,
`access`, `consoleuser`), typed error taxonomy with a global handler, every API
error carries a body, logs on every point where data can be lost. Comments and
log messages in English; the operator-facing error texts are Russian, like the
console.

## API outline

| Area | Endpoints |
|---|---|
| Public | `GET /r/{slug}` — the redirect; `GET /api/v1/info` — server name, version, license; `GET /system/health` (actuator) |
| Auth | `POST /api/v1/auth/login` `{username, password}` → `{access_token, refresh_token}`; `POST /api/v1/auth/refresh` `{refresh_token}` → a new pair; `POST /api/v1/auth/logout` (bearer) |
| Codes (bearer, any role) | `GET /api/v1/qr`, `GET /api/v1/qr/{slug}`, `GET /api/v1/qr/{slug}/image?format=png\|svg&size=512` |
| Codes (bearer, `ADMIN`) | `POST /api/v1/qr`, `PUT /api/v1/qr/{slug}`, `DELETE /api/v1/qr/{slug}` |

Every JSON answer is the envelope `{data, message, error}`; `message` is `OK`
or `FAIL`, and a failure carries `error.message` (technical) and
`error.localizedMessage` (for the operator). Status codes are plain HTTP:
`400` invalid input, `401` no valid session, `403` role may not, `404` unknown
slug, `409` slug taken.

Request and response fields of a code: `slug`, `name`, `redirect_url`,
`target_ios_url`, `target_huawei_url`, `target_android_url`,
`target_desktop_url` (required), `scan_count_*`, `created_at`, `updated_at`.

## Run

The server is deployed as an image from the umbrella repository's
`docker-compose.yml`, bound to `127.0.0.1:8080`. Two things reach it there: the
console's nginx (same-origin `/api/v1` for the browser) and the edge proxy,
which terminates TLS for the server's public name and forwards the scheme in
`X-Forwarded-Proto`. It never listens on a public interface itself.
Configuration comes from environment variables (`.env` on the host):

| Variable | Default | Meaning |
|---|---|---|
| `PF_DB_URL` | `jdbc:postgresql://127.0.0.1:5432/pathfinder` | PostgreSQL the schema lives in |
| `PF_DB_USER` / `PF_DB_PASSWORD` | `pathfinder` / — | schema owner; Flyway runs as it |
| `PF_SERVER_BIND` / `PF_SERVER_PORT` | `127.0.0.1` / `8080` | listen address and port |
| `PF_PUBLIC_BASE_URL` | — | the server's public address, printed into every QR code as `<base>/r/<slug>` (`https://pathfinder.example.com`) |
| `PF_CORS_ORIGINS` | — (none) | comma-separated browser origins allowed to call the API cross-site. Empty is correct while the console is same-origin through its own nginx; needed only for a console built with `API_BASE_URL` or a local vite server on another origin. Wildcards are rejected on purpose - the policy allows credentials |
| `PF_JWT_SECRET` | — | signing key of the console tokens, at least 32 characters |
| `PF_ADMIN_USERS` | — | administrators as `login:password` pairs, comma-separated; at least one |
| `PF_VIEWER_USERS` | — (none) | read-only users, same format |
| `PF_ACCESS_TOKEN_TTL` / `PF_REFRESH_TOKEN_TTL` | `PT15M` / `P7D` | session lifetimes (ISO-8601 durations) |
| `PF_LOG_LEVEL` | `INFO` | log level of the server's own packages |

Logins are 3-64 latin letters, digits, `.`, `_` or `-`; passwords at least 8
characters and, because the list is comma-separated, without commas. A pair is
split at its first colon.

Schema: Flyway owns it. `V1` creates `console_user`, `refresh_token` and
`qr_code`; Hibernate only validates that the entities match.

Local build and run against a PostgreSQL of your own:

```bash
./gradlew bootJar                                   # JDK 25 is provisioned by the toolchain resolver if missing
PF_DB_PASSWORD=… PF_JWT_SECRET=… PF_PUBLIC_BASE_URL=http://127.0.0.1:8080 PF_ADMIN_USERS=admin:admin-admin \
  java -jar build/libs/pathfinder-server.jar
curl -s http://127.0.0.1:8080/api/v1/info           # {"data":{"name":"pathfinder-server",…},"message":"OK"}
```

Local build with Docker Compose - the image from this working tree plus its own
PostgreSQL, nothing else on the laptop:

```bash
cp .env.example .env                    # then fill the secrets - compose has no defaults for them
docker compose up --build               # builds the jar in Docker, starts postgres + server on 127.0.0.1:8080
docker compose logs -f server           # migrations, console users, then "Started PathfinderServerApplication"
docker compose down                     # stop; the database stays in ./.local (gitignored)
rm -rf .local && docker compose up      # start from an empty database
```

`npm run dev` in `pathfinder-console` proxies `/api` to this server.

Image: `docker build -t pathfinder-server:dev .` (the Dockerfile runs the same
Gradle build; CI publishes `ghcr.io/beiroun/pathfinder-server:<version>` on a
`vX.Y.Z` tag, the version comes from the tag).

## License

**Business Source License 1.1** — see [`LICENSE`](LICENSE). Non-production use
is free; production use beyond the Additional Use Grant requires a commercial
license from the Licensor. Third-party components keep their own licenses
(`NOTICE` in the umbrella repository).
