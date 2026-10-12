# Project: eCommerce Bookstore Backend

## Stack
- Java 17, Spring Boot 3.x
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL (via Docker Compose locally)
- Maven build
- Lombok
- JUnit 5 + Mockito for tests
- Flyway for DB migrations

## Conventions
- Package structure: com.bookstore.{config, controller, service, repository, model, dto, exception, security}
- Controllers only call services; business logic lives in services, not controllers
- All DTOs validated with jakarta.validation annotations
- All API responses use a consistent envelope: { success, data, error }
- Global exception handling via @ControllerAdvice
- Every service method gets a corresponding JUnit test

## Commands
- Run app: mvn spring-boot:run
- Run tests: mvn test
- Start local DB: docker-compose up -d

- Entity <-> DTO mapping uses MapStruct (@Mapper(componentModel = "spring")) in
  com.bookstore.dto.mapper. Lombok + MapStruct annotation processor ordering is
  configured in pom.xml (lombok, mapstruct-processor, lombok-mapstruct-binding).