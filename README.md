# FlashSale

A production-oriented Flash Sale backend application built with Java 21, Spring Boot, PostgreSQL, Docker, and Nginx.

## Technology Stack

- Java 21
- Spring Boot 3.5.16
- Maven
- PostgreSQL 17
- Docker and Docker Compose
- Nginx
- JWT Authentication
- Flyway Database Migration

## Architecture

The application uses Nginx as a reverse proxy in front of the Spring Boot application.

```text
                 Client
                   |
                   | HTTPS :443
                   v
              Nginx Proxy
                   |
                   | HTTP :8080
                   v
           Spring Boot Application
                   |
                   v
               PostgreSQL
```

HTTP traffic is redirected to HTTPS.

The Spring Boot application is accessible to Nginx through the internal Docker network. Its port 8080 is not published directly to the host.

## HTTPS Configuration

Local HTTPS is configured using a self-signed RSA certificate.

Certificate files are generated locally and stored in:

`nginx/certs/`

These files are excluded from Git using `.gitignore`.

Enabled TLS protocols:

- TLS 1.2
- TLS 1.3

HTTP requests are redirected to HTTPS using an HTTP 301 response.

**Note:** The self-signed certificate is intended only for local development and testing. Browsers may display a certificate security warning.

## Docker Configuration

The application runs using Docker Compose.

Services:

| Service | Container | Port |
|---|---|---|
| PostgreSQL | flashsale-postgres | 5432 |
| Spring Boot | flashsale-app | 8080 (internal) |
| Nginx | flashsale-nginx | 80, 443 |

The Spring Boot application exposes port 8080 internally without publishing it to the host.

PostgreSQL port 5432 is published for local development access.

## Health Check

The application health endpoint is available through HTTPS:

