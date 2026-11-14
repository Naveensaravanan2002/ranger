# Ranger backend

Java 21 / Spring Boot 4.1.1 Maven API. From this directory on Windows: `.\mvnw.cmd spring-boot:run` for a local, disposable H2 demo; `.\mvnw.cmd test` for the H2 test suite. Health: `http://localhost:8080/api/public/health`. Do not run tests against a real database.

The complete Ranger source ZIP includes `../docs/` with the required Windows, Neon, Firebase, free deployment, security and test-only Gmail guides. Those guides are essential before enabling accounts or handling personal data. No Flyway: review/apply `src/main/resources/db/schema/001_initial.sql` manually on the intended PostgreSQL database and use `SPRING_PROFILES_ACTIVE=neon`. Never commit or send passwords/URLs/tokens. The Gmail connector is disabled by default and has not been verified against real Google accounts.
