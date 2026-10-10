# eCommerce Bookstore Backend

A Spring Boot REST API for a bookstore: catalog browsing, cart, checkout, and order management, with JWT-based auth and role-based access control.

## Stack

- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL (via Docker Compose locally)
- Flyway for DB migrations
- Maven, Lombok
- springdoc-openapi (Swagger UI)

## Prerequisites

- Java 17
- Maven
- Docker + Docker Compose (for PostgreSQL)

## Setup

1. Start PostgreSQL:
   ```
   docker-compose up -d
   ```
2. Run the app (Flyway applies all migrations, including seed data, on startup):
   ```
   mvn spring-boot:run
   ```
3. The API is available at `http://localhost:8080`.

## Environment variables

All have defaults suitable for local development (see `src/main/resources/application.yml` and `docker-compose.yml`); override as needed.

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `bookstore` | Database name |
| `DB_USERNAME` | `bookstore_user` | Database user |
| `DB_PASSWORD` | `bookstore_pass` | Database password |
| `SERVER_PORT` | `8080` | HTTP port the app listens on |
| `JWT_SECRET` | `dev-fixed-secret-key-change-me-0123456789abcdef` | HMAC signing key for JWTs — **change this in any non-local environment** |
| `JWT_EXPIRATION_MS` | `86400000` | JWT lifetime in milliseconds (default: 24h) |

## Seed data

`V6__seed_data.sql` seeds the database on first startup with:
- 3 categories: Fiction, Non-Fiction, Science
- 10 sample books spread across those categories
- 1 admin user:
  - email: `admin@bookstore.com`
  - password: `Admin@12345`

These credentials are for local development only — don't reuse them anywhere real.

## API docs (Swagger UI)

Once the app is running:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

Both are publicly accessible (no JWT required).

## Running tests

```
mvn test
```

## Endpoints

All responses use the envelope `{ success, data, error }`. Endpoints marked **Admin** require a JWT for a user with role `ADMIN`; **Auth** means any authenticated user; **Public** means no token required.

### Health
| Method | Path | Access |
|---|---|---|
| GET | `/health` | Public |

### Auth
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Books
| Method | Path | Access |
|---|---|---|
| GET | `/api/books` | Public (search/filter/paginate via `search`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size`) |
| GET | `/api/books/{id}` | Public |
| POST | `/api/books` | Admin |
| PUT | `/api/books/{id}` | Admin |
| DELETE | `/api/books/{id}` | Admin |

### Categories
| Method | Path | Access |
|---|---|---|
| GET | `/api/categories` | Public |
| GET | `/api/categories/{id}` | Public |
| POST | `/api/categories` | Admin |
| PUT | `/api/categories/{id}` | Admin |
| DELETE | `/api/categories/{id}` | Admin |

### Cart
| Method | Path | Access |
|---|---|---|
| GET | `/api/cart` | Auth |
| POST | `/api/cart/items` | Auth |
| PATCH | `/api/cart/items/{id}` | Auth |
| DELETE | `/api/cart/items/{id}` | Auth |

### Checkout
| Method | Path | Access |
|---|---|---|
| POST | `/api/checkout` | Auth |

### Orders
| Method | Path | Access |
|---|---|---|
| GET | `/api/orders` | Auth (own orders only) |
| GET | `/api/orders/{id}` | Auth (own order only) |

### Admin Orders
| Method | Path | Access |
|---|---|---|
| GET | `/api/admin/orders` | Admin |
| PATCH | `/api/admin/orders/{id}/status` | Admin |
