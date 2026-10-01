# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Commands

```bash
./gradlew build                                   # compile + all tests
./gradlew test                                    # tests only
./gradlew test --tests 'com.outsera.challenge_backend.ChallengeBackendApplicationTests'           # one class
./gradlew test --tests 'com.outsera.challenge_backend.ChallengeBackendApplicationTests.contextLoads'  # one method
./gradlew bootRun                                 # run locally
```

There is no linter or formatter configured.

## Project state and structure

This is a freshly generated Spring Boot skeleton (Spring Initializr): only the `@SpringBootApplication` main class and a `contextLoads` test exist. Most of the architecture is still to be built.

- Base package is `com.outsera.challenge_backend` (underscore — the hyphenated name is not a valid Java package). New code goes under it so component scanning picks it up.
- Gradle uses a Java 25 toolchain; Spring Boot 4.1 with the modular starters (`spring-boot-starter-webmvc`, `-data-jpa`, `-flyway`, `spring-boot-h2console`) and their matching `*-test` starters for slice tests.
- Config is in `src/main/resources/application.yml` (YAML, not `.properties`).
- Persistence: H2 (runtime-only) with Flyway. Schema changes belong in Flyway migrations under `src/main/resources/db/migration` (`V<n>__<description>.sql`), not in JPA auto-DDL.
