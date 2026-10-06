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
./gradlew jacocoTestReport                        # coverage report (also runs automatically after test)
```

There is no linter or formatter configured. JaCoCo (plugin, `toolVersion = '0.8.14'`, the first release with official Java 25 support) writes HTML/XML coverage to `build/reports/jacoco/test/`; `test` is `finalizedBy` `jacocoTestReport`. `README.md` holds the user-facing run/test/API docs; keep it in sync when the endpoint, tests or configuration change.

## Goal

REST API (Richardson maturity level 2) over the Golden Raspberry Awards "Worst Picture" list. It must return the producers with the shortest and longest interval between two consecutive wins, as `{"min": [...], "max": [...]}` where each item has `producer`, `interval`, `previousWin`, `followingWin`. Only integration tests are allowed, and they must assert against the data in the provided CSV.

## Architecture

- Base package is `com.outsera.challenge_backend` (underscore — the hyphenated name is not a valid Java package).
- Java 25 toolchain; Spring Boot 4.1 modular starters (`-webmvc`, `-data-jpa`, `-flyway`, `spring-boot-h2console`) plus their `*-test` starters.
- Config is `src/main/resources/application.yml` (YAML, not `.properties`).
- **Schema is owned by Flyway** (`src/main/resources/db/migration`), `ddl-auto: none`. Entity IDs use the DB sequences `movie_seq` / `producer_seq` with `allocationSize = 1` to match `INCREMENT BY 1`.
- **H2 in-memory with `NON_KEYWORDS=YEAR`** in the datasource URL: `YEAR` is reserved in H2 2.x and `movie.year` is an unquoted column. Removing it breaks the V1 migration.
- **H2 Console** is enabled at `/h2-console` (JDBC URL `jdbc:h2:mem:challenge;NON_KEYWORDS=YEAR`, user `sa`, empty password).
- **Startup data load:** `movie.csv.MovieDataLoader` is an `ApplicationRunner`, so it runs after context refresh, i.e. after Flyway has migrated. It parses the CSV set by `app.movies.csv-location` (default `classpath:data/movielist.csv`) and inserts producers then movies in one transaction; it skips if `movie` already has rows.
- **CSV format:** `year;title;studios;producers;winner`, `winner` is `yes` or empty, studios are not persisted. Producers are separated by `,`, ` and ` or `, and ` (`MovieCsvParser.PRODUCER_SEPARATOR`); the same name in different movies maps to one `producer` row.
- **Award intervals endpoint:** `GET /producers/award-intervals` (`award` package). `MovieRepository.findProducerWinsOrderByProducerAndYear` fetches (producer, year) of winning movies in one native SQL query mapped straight to the `ProducerWin` record — the column aliases `producer` / `year` must match the record component names; `AwardIntervalService` computes everything in a **single loop**: since rows are sorted by producer then year, consecutive wins of a producer are adjacent; each interval updates the `min`/`max` lists on the fly (smaller → clear and add, equal → add; mirrored for max). The checks are separate `if`s, not `else if`, so the first interval enters both lists. Keep it single-pass — no intermediate list of all intervals, no extra stream passes. Producers with a single win are excluded; no intervals → empty lists.
- Integration tests are named `*IT` and use `@SpringBootTest` (+ `@AutoConfigureMockMvc` from `org.springframework.boot.webmvc.test.autoconfigure` for HTTP). Baseline for the real CSV: 206 movies, 42 winners, 359 distinct producers; min = Joel Silver 1 (1990→1991), max = Matthew Vaughn 13 (2002→2015).
- Repository tests (`MovieRepositoryIT`) use `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`: the default replacement datasource lacks `NON_KEYWORDS=YEAR`, so V1 would fail. The CSV loader is not in the JPA slice, so these tests build their own rows (rolled back per test). Fixtures must make wrong orderings observable — e.g. a producer sorted later by name must have an earlier year.
- AssertJ (3.27.7) comes managed through the Boot test starters; do not add it or other test dependencies to `build.gradle`.
- Startup-failure tests (`MovieCsvImportIT`) start the app with `SpringApplicationBuilder` and pass overrides as `run("--key=value")` args: `SpringApplicationBuilder.properties(...)` are *default* properties and lose to `application.yml`. Exceptions thrown by an `ApplicationRunner` propagate unwrapped from `run()` in Boot 4 (no `IllegalStateException` wrapper).
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
4. Prompt: "atualize o CLAUDE.md e faça commit" — updated this file and committed as `8e48903`.

### 8. H2 Console

Prompt: "Habilite o H2 Console no application.yml do projeto"

Steps executed:

1. Set `spring.h2.console.enabled: true` and `path: /h2-console` in `application.yml` (the `spring-boot-h2console` dependency was already present).
2. Ran `./gradlew bootRun`: `/h2-console/` returned `200` and the log showed "H2 console available at '/h2-console'"; `./gradlew build` still green.
3. Prompt: "atualize o README e o CLAUDE.md e faça commit" — documented console access in `README.md` and here, committed as `062d597`.

### 9. Native SQL for producer wins

Prompt: "Converta a Query JPA no arquivo .../MovieRepository.java para uma consulta SQL nativa." with the user-provided SQL:

```sql
select p.name, m.year
from movie m
join movie_producer mp on mp.movie_id = m.id
join producer p on p.id = mp.producer_id
where m.winner = true
order by p.name, m.year
```

Steps executed:

1. Replaced the JPQL constructor expression in `MovieRepository.findProducerWinsOrderByProducerAndYear` with `@Query(nativeQuery = true)` using the user's SQL, only adding the aliases `as producer` / `as year` so Spring Data maps the columns to the `ProducerWin` record by name.
2. `./gradlew build` green (8 tests): the strict JSON assertions confirm the result is unchanged.
3. Prompt: "Rode todos os testes e atualize o CLAUDE.md" — `./gradlew test --rerun-tasks` green (8 tests: `MovieDataLoaderIT` 4, `AwardIntervalControllerIT` 2, `AwardIntervalTiesIT` 1, `ChallengeBackendApplicationTests` 1); updated the architecture note and this log.
4. Committed as `0046ab0`.

### 10. MovieRepository integration test

Prompt: "Agora escreva um teste de integração para o MovieRepository.java. Utilize a biblioteca assertj-core na versão 3.27.7. Não inclua nenhuma outra dependência no build.gradle"

Steps executed:

1. Ran `dependencyInsight`: `org.assertj:assertj-core:3.27.7` is already on the test classpath via `spring-boot-starter-test` (Boot-managed), so `build.gradle` was left unchanged.
2. Located the Boot 4 packages for `@DataJpaTest`, `@AutoConfigureTestDatabase` and `TestEntityManager`.
3. Wrote `MovieRepositoryIT` (`@DataJpaTest`, real Flyway schema, own H2 database `movie-repository`): ordering by producer then year with a multi-producer movie, non-winners ignored, empty result without winners, and `countByWinnerTrue`.
4. Mutation check: changed the query to `order by m.year, p.name` — the test still passed because the fixture produced the same order either way. Moved Beta's solo win to 1999 so year-first ordering differs; the broken query then failed the test. Restored the original query.
5. `./gradlew test --rerun-tasks` green (12 tests).
6. Prompt: "atualize o CLAUDE.md e faça commit" — updated this file and committed as `b1fdb9b`.

### 11. JaCoCo coverage

Prompt: "agora adicione a biblioteca JaCoCo para cobertura de testes. Depois rode todos os testes novamente"

Steps executed:

1. Added Gradle's built-in `jacoco` plugin (no new entries in `dependencies`). Checked the `jacocoAgent` configuration: Gradle 9.7.1 defaults to JaCoCo 0.8.14, which supports Java 25; pinned `toolVersion = '0.8.14'` so a Gradle upgrade cannot change it silently.
2. Made `test` `finalizedBy` `jacocoTestReport`, with HTML and XML reports enabled.
3. `./gradlew test --rerun-tasks` green (12 tests). Coverage: 86% lines (93/108), 83% branches (15/18). Gaps: `MovieCsvParser` error paths (malformed line, invalid year, I/O failure), unused entity getters, and `main()`.
4. Prompt: "atualize o README e o CLAUDE.md e faça commit" — documented coverage reports (and the missing `MovieRepositoryIT` row) in `README.md`, updated this file and committed as `8583705` (amended to fix the JaCoCo/Java 25 wording).

### 12. CSV parser error tests

Prompt: "escreva o teste de integração para os erros do parser"

Steps executed:

1. Added fixtures: `data/invalid/wrong-column-count.csv`, `data/invalid/invalid-year.csv` (year `198O`) and `data/with-blank-lines.csv`.
2. Wrote `MovieCsvImportIT`: starts the full application (no web server, unique H2 database per run) with each CSV and asserts the exact startup failure, or that blank lines are skipped.
3. First run: all 4 failed because the app loaded the default CSV — builder `properties(...)` are defaults overridden by `application.yml`. Switched to command-line args.
4. Second run: 3 failed because the assertions expected the runner exception to be wrapped; Boot 4 rethrows it as is. Asserted on the thrown exception directly.
5. `./gradlew test --rerun-tasks` green (16 tests). Coverage: `MovieCsvParser` 100% lines / 88% branches (was 79% / 62%); total 92% lines / 94% branches. Only uncovered branch: the filter dropping empty producer names.
6. Prompt: "atualize o README e o CLAUDE.md e faça commit" — added the test to `README.md`, updated this file and committed as `48a60cc`.

### 13. No producer with more than one win

Prompt: "Faça um novo teste de integração para cobrir o cenário onde não há nenhum produtor com mais de uma nova vitória. Gere um arquivo csv específico para esse teste e crie um novo arquivo no diretório ./src/test/java/com/outsera/challenge_backend/award/"

Steps executed:

1. Added fixture `src/test/resources/data/award-intervals-single-wins.csv`: every producer has at most one win; Alpha, Beta and Epsilon also have non-winning movies one year before/after their win (would create intervals if nominations were counted), Beta and Gamma share a winning movie, Delta is only nominated.
2. Wrote `award/AwardIntervalSingleWinsIT` (own H2 database, same pattern as `AwardIntervalTiesIT`): asserts 3 winning movies were loaded, so empty lists are meaningful, then a strict JSON match of `{"min": [], "max": []}`.
3. `./gradlew test --rerun-tasks` green (17 tests).
4. Mutation check: replaced `where m.winner = true` with `where 1 = 1` in `MovieRepository` — the test failed; restored the original query.
5. Prompt: "atualize o CLAUDE.md e faça commit" — updated this file and committed as `943d7bf` (`README.md` not changed).

### 14. Single producer with two wins (user-written test, reviewed)

Prompt: "Adicionei mais um cenário de teste: data/award-intervals-one-consecutive-win.csv e AwardIntervalOneConsecutiveWinIT. Revise, rode todos os testes novamente"

Steps executed:

1. Reviewed the user's test (copy of `AwardIntervalSingleWinsIT` with Movie B turned into a win, so Alpha won 2000 and 2001) and ran `./gradlew test --rerun-tasks`: green (18 tests).
2. Review findings: the `.as(...)` message was copied ("empty lists prove nothing") and wrong here; a 1-year interval cannot catch an interval computed as a constant or as "adjacent years"; "consecutive win" in the name reads like consecutive years (the spec means consecutive wins of the producer); the expected JSON mixed tabs and spaces. Also, the staged copy of both files was the original single-wins copy — committing without re-adding would have recorded the wrong content.

Prompt: "aplique os ajustes, atualize o CLAUDE.md e faça commit"

1. Renamed to `award/AwardIntervalSingleProducerIT` and `data/award-intervals-single-producer.csv` (removed the old staged files with `git rm -f`, since the index held stale content).
2. Fixture: Alpha's second win moved to 2004 (new row `Movie H`), keeping Alpha's 2001/2002 and Beta's 2002 nominations, so counting nominations would add intervals. Expected `min` = `max` = Alpha, interval 4, 2000→2004.
3. Fixed the `.as(...)` message and Javadoc, and indented the expected JSON with spaces only.
4. `./gradlew test --rerun-tasks` green (18 tests). Mutation check (`where 1 = 1` instead of `where m.winner = true`) made the test fail; query restored.
5. Updated this file and committed as `97cc456` (`README.md` not changed).

### 15. README for the new award tests

Prompt: "atualize o README e faça commit" — added `AwardIntervalSingleWinsIT` and `AwardIntervalSingleProducerIT` to the README test table and stated that `min`/`max` are empty when no producer has two wins. Committed as `4cddae1`.

### 16. Fewer loops in `AwardIntervalService`: manual 3-loop attempt, then a single loop

Prompt: "O método "consecutiveWinIntervals" na classe "AwardIntervalService" está fazendo 5 loops para calcular o menor e o maior intervalo. Analise a minha solução implementada na classe AwardIntervalService. O objetivo é diminuir a quantidade de loops"

Starting point — the original implementation made **5 passes**:

1. loop building the list of all consecutive-win intervals;
2. stream for `min`;
3. stream for `max`;
4. stream filtering the intervals equal to `min`;
5. stream filtering the intervals equal to `max`.

**First attempt (written manually by the user): 3 passes.** The building loop also tracked `min` (start `Integer.MAX_VALUE`) and `max` (start `-1`) and returned them with the full interval list in a new public record `ConsecutiveWinIntervals(int min, int max, List<ProducerInterval> intervals)`; `findMinAndMaxIntervals` still ran two filter streams over that list. Steps executed by the agent:

1. Ran `./gradlew test --rerun-tasks` on the user's version: green (18 tests), so the attempt was correct, including empty results and an interval of 0.
2. Review findings:
   - still 3 passes plus an intermediate list holding every interval; `min`/`max` lists can be maintained inside the loop itself;
   - `ConsecutiveWinIntervals` was a public type for an internal detail;
   - `max = -1` relied on intervals never being negative; `Integer.MIN_VALUE` is explicit and symmetric;
   - the whole file had been re-indented from tabs to 4 spaces (project uses tabs), so the diff covered the entire file;
   - `ConsecutiveWinIntervals.java` was staged as an empty record (`AM`): committing without re-adding would not compile.
3. Proposed a single-loop version.

Prompt: "Aplique a solução de um único loop e rode os testes"

**Optimization: 1 pass.** Steps executed:

1. Rewrote `findMinAndMaxIntervals` as one loop over the sorted wins that keeps `min`/`max` lists: smaller interval → update bound, clear list; equal → add (mirrored for max). Separate `if`s so the first interval enters both lists; bounds start at `Integer.MAX_VALUE` / `Integer.MIN_VALUE`. Output order is unchanged (producer, then year), so the strict JSON tests still apply.
2. Removed `ConsecutiveWinIntervals.java` (never committed; no remaining references) and restored tab indentation.
3. `./gradlew test --rerun-tasks` green (18 tests). JaCoCo: `AwardIntervalService` 100% lines (27/27) and 100% branches (12/12) — every path (different producer, new min, min tie, new max, max tie) is exercised.

| Version | Passes over the data | Intermediate structures |
| --- | --- | --- |
| Original | 5 (1 loop + 4 streams) | list of all intervals |
| User's manual attempt | 3 (1 loop + 2 streams) | list of all intervals + `ConsecutiveWinIntervals` record |
| Final | 1 loop | none besides the `min` / `max` result lists |

All versions are O(n) in the number of wins (42 winning movies in the CSV), so the gain is mostly clarity and memory rather than runtime.

Prompt: "atualize o CLAUDE.md. Deixe explícito a primeira tentativa manual com 3 loops e a posterior otimização para um único loop" — updated the architecture note and this log (not committed).

All spec items are now covered; pushing to a remote git host is left to the user.
