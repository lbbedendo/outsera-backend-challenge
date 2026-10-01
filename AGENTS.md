# AGENTS.md

Instructions for AI coding agents working in this repo. This file is about the things
that aren't obvious from reading the code, and that have already caused real build/test failures.

## Spring Boot API

**Stack:** Java 25, Spring Boot 4.1, h2 database + Flyway, Spring MVC

**Build & test:**
```bash
./gradlew build          # compile + test
./gradlew test           # tests only
./gradlew bootRun        # run locally
```