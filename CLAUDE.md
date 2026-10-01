# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Commands

```bash
./gradlew build                                   # compile + all tests
./gradlew test                                    # tests only
./gradlew test --tests 'com.outsera.challenge_backend.movie.csv.MovieDataLoaderIT'                       # one class
./gradlew test --tests 'com.outsera.challenge_backend.movie.csv.MovieDataLoaderIT.loadsEveryMovieFromCsv'  # one method
./gradlew bootRun                                 # run locally
```

There is no linter or formatter configured. `README.md` holds the user-facing run/test/API docs; keep it in sync when the endpoint, tests or configuration change.

## Goal

REST API (Richardson maturity level 2) over the Golden Raspberry Awards "Worst Picture" list. It must return the producers with the shortest and longest interval between two consecutive wins, as `{"min": [...], "max": [...]}` where each item has `producer`, `interval`, `previousWin`, `followingWin`. Only integration tests are allowed, and they must assert against the data in the provided CSV.

## Architecture

- Base package is `com.outsera.challenge_backend` (underscore — the hyphenated name is not a valid Java package).
- Java 25 toolchain; Spring Boot 4.1 modular starters (`-webmvc`, `-data-jpa`, `-flyway`, `spring-boot-h2console`) plus their `*-test` starters.
- Config is `src/main/resources/application.yml` (YAML, not `.properties`).
- **Schema is owned by Flyway** (`src/main/resources/db/migration`), `ddl-auto: none`. Entity IDs use the DB sequences `movie_seq` / `producer_seq` with `allocationSize = 1` to match `INCREMENT BY 1`.
- **H2 in-memory with `NON_KEYWORDS=YEAR`** in the datasource URL: `YEAR` is reserved in H2 2.x and `movie.year` is an unquoted column. Removing it breaks the V1 migration.
- **Startup data load:** `movie.csv.MovieDataLoader` is an `ApplicationRunner`, so it runs after context refresh, i.e. after Flyway has migrated. It parses the CSV set by `app.movies.csv-location` (default `classpath:data/movielist.csv`) and inserts producers then movies in one transaction; it skips if `movie` already has rows.
- **CSV format:** `year;title;studios;producers;winner`, `winner` is `yes` or empty, studios are not persisted. Producers are separated by `,`, ` and ` or `, and ` (`MovieCsvParser.PRODUCER_SEPARATOR`); the same name in different movies maps to one `producer` row.
- **Award intervals endpoint:** `GET /producers/award-intervals` (`award` package). `MovieRepository.findProducerWinsOrderByProducerAndYear` fetches (producer, year) of winning movies in one JPQL projection query; `AwardIntervalService` walks it once to build consecutive-win intervals and returns every producer tied on min and on max. Producers with a single win are excluded; no intervals → empty lists.
- Integration tests are named `*IT` and use `@SpringBootTest` (+ `@AutoConfigureMockMvc` from `org.springframework.boot.webmvc.test.autoconfigure` for HTTP). Baseline for the real CSV: 206 movies, 42 winners, 359 distinct producers; min = Joel Silver 1 (1990→1991), max = Matthew Vaughn 13 (2002→2015).
- A test that loads a different CSV (`app.movies.csv-location`) must also set its own `spring.datasource.url` (see `AwardIntervalTiesIT`): the in-memory DB name is fixed, so contexts would otherwise share it and the loader would skip the import.
- JPA conventions for this repo are in `skills/spring-data-jpa/SKILL.md` (Jakarta imports, no entity records, DTO records out of controllers, LAZY relations, no `findAll()` in endpoints). It is not under `.claude/skills`, so read it explicitly before persistence work.

## Agent interaction log

Kept here because the challenge requires a record of AI-agent interactions in the repo.

1. `/init` — created this file.
2. "disable JPA auto-DDL in application.yml" — set `spring.jpa.hibernate.ddl-auto: none`.
3. Commit requests — committed on `main` (project rule: no feature branches).

### 4. Movie schema and CSV load

Prompt (verbatim, abridged only where marked):

> Analise a seguinte especificação abaixo:
>
> 1.Especificação do teste
> 1. Desenvolva uma API RESTful para possibilitar a leitura da lista de indicados e vencedores da categoria Pior Filme do Golden Raspberry Awards.
>
> 2.Requisitos do sistema
> 1. Ler o arquivo CSV dos filmes e inserir os dados em uma base de dados ao iniciar a aplicação.
>
> 3.Requisitos da API
> 1. Obter o produtor com maior intervalo entre dois prêmios consecutivos, e o que obteve dois prêmios mais rápido, seguindo a especificação de formato definida na página 2.
>
> 4.Requisitos não funcionais do sistema
> 1. O web service RESTful deve ser implementado com base no nível 2 de maturidade de Richardson;
> 2. Devem ser implementados somente testes de integração. Eles devem garantir que os dados obtidos estão de acordo com os dados fornecidos na proposta;
> 3. O banco de dados deve estar em memória utilizando um SGBD embarcado (por exemplo: H2, SQLite). Nenhuma instalação externa deve ser necessária;
> 4. A aplicação deve conter um readme com instruções para rodar o projeto e os testes de integração.
> 5. O código-fonte deve ser disponibilizado em um repositório git (Github, Gitlab, Bitbucket, etc).
>
> 5.LLMs, agentes e skills (inteligência artificial)
> 1. Para esta vaga, o uso de ferramentas de “inteligência artificial” é obrigatório e será avaliado;
> 2. O projeto deve conter no repositório um registro de interações com o agente. Isto pode ser disponibilizado no formato de logs, lista de prompts/skills utilizadas, ou histórico de interações;
> 3. A qualidade e desempenho do código serão avaliados nos mesmos critérios de uma revisão de código tradicional.
>
> 6.Formato da API — Descrição: Intervalo de prêmios — Formato: `{"min": [{"producer", "interval", "previousWin", "followingWin"}, ...], "max": [...]}` *(example JSON abridged)*
>
> Agora utilize o meu direcionamento:
>
> 1) O arquivo com os dados de filmes estão contidos no arquivo: /home/lbarbieri/Downloads/challenge-backend/src/main/resources/data/movielist.csv
>
> 2) Utilize a seguinte modelagem de domínios e crie a migration V1 no flyway: *(DDL for `movie_seq`, `producer_seq`, `movie`, `producer`, `movie_producer` and indexes — stored verbatim in `V1__create_movie_and_producer_tables.sql`)*
>
> 3) Faça o parse do CSV informado no item 1 na inicialização da aplicação e faça o insert nas tabelas. A execução deve ocorrer após o Flyway executar a migration com sucesso.

Steps executed:

1. Inspected `movielist.csv`: `;`-separated, 206 data rows, no quoting, `winner` = `yes`/empty, producer lists in the forms `A and B`, `A, B and C`, `A, B, and C`. Computed the expected distinct producer count (359) with a shell pipeline to use as test ground truth.
2. Created `db/migration/V1__create_movie_and_producer_tables.sql` with the given DDL unchanged.
3. Mapped `Movie` / `Producer` entities (`@ManyToMany` through `movie_producer`, sequence IDs with `allocationSize = 1`) and their repositories.
4. Wrote `MovieCsvParser` (fails fast with line number on malformed rows), `MovieCsvRecord`, `MovieCsvProperties` (`app.movies.csv-location`) and `MovieDataLoader` (`ApplicationRunner`, single transaction, deduplicates producers).
5. Added `MovieDataLoaderIT`: totals, winner count, producer count, column mapping, and producer splitting for `Cats` and `Under the Cherry Moon`.
6. First `./gradlew build` failed: H2 rejected `year` in `CREATE TABLE movie` (reserved keyword). Fixed with `NON_KEYWORDS=YEAR` in the datasource URL to keep the DDL as given; also set `open-in-view: false`.
7. `./gradlew build` green (5 tests). Logs confirm order: Flyway applies V1 → application started → "Loaded 206 movies and 359 producers".
8. Committed as `1f12790`.

### 5. Update CLAUDE.md

Prompt: "Atualize o CLAUDE.md com o prompt e os passos executados" — added the architecture notes and this log; read `skills/spring-data-jpa/SKILL.md` and linked it here. Committed as `dfa3b06`.

### 6. Award intervals endpoint

Prompt: "agora implemente o endpoint de intervalo de prêmios"

Steps executed:

1. Computed the expected result straight from the CSV with an awk pipeline (independent of the Java code): min Joel Silver 1 (1990→1991), max Matthew Vaughn 13 (2002→2015).
2. Added `ProducerWin` record and a JPQL constructor-projection query in `MovieRepository` (winners only, ordered by producer and year) to avoid loading entities / N+1.
3. Added `AwardIntervalService` (single pass over sorted wins, keeps all ties), response records `AwardIntervalsResponse` / `ProducerInterval`, and `AwardIntervalController` (`GET /producers/award-intervals`).
4. Added `AwardIntervalControllerIT` (strict JSON match against the real CSV, `POST` → 405) and `AwardIntervalTiesIT` with fixture `src/test/resources/data/award-intervals-ties.csv` (ties on min and max, ignored non-winner, single-win producer), on its own H2 database.
5. `./gradlew build` green (8 tests).
6. Prompt: "atualize o CLAUDE.md e faça commit" — updated this file and committed as `2d5ffab`.

### 7. README

Prompt: "crie o README com instruções para rodar o projeto e os testes"

Steps executed:

1. Ran `./gradlew bootRun` and called the endpoint with `curl` to confirm the documented response and the `405` on `POST`.
2. Wrote `README.md`: requirements (JDK 25 only), how to run, endpoint docs with the real response, how to run all/one test class and where the report is, what each IT covers, the `app.movies.csv-location` property, and a pointer to this log.
3. Verified the single-class test command and `bootRun --args='--app.movies.csv-location=file:...'` with the ties fixture.
4. Prompt: "atualize o CLAUDE.md e faça commit" — updated this file and committed on `main`.

All spec items are now covered; pushing to a remote git host is left to the user.
