# Golden Raspberry Awards API

RESTful API over the nominees and winners of the Golden Raspberry Awards "Worst Picture" category. It returns the producers with the longest and the shortest interval between two consecutive wins.

On startup the application reads [`movielist.csv`](src/main/resources/data/movielist.csv) and loads it into an in-memory H2 database, after the Flyway migrations have created the schema. Nothing needs to be installed besides the JDK.

## Requirements

- JDK 25 (`java -version` must report 25). Gradle is downloaded by the wrapper.

## Running the application

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8080`. On Windows use `gradlew.bat` instead of `./gradlew`.

## API

### `GET /producers/award-intervals`

Producers with the shortest (`min`) and longest (`max`) interval, in years, between two consecutive wins. When several producers tie, all of them are returned. Producers with a single win are not considered.

```bash
curl http://localhost:8080/producers/award-intervals
```

Response `200 OK` for the provided CSV:

```json
{
  "min": [
    { "producer": "Joel Silver", "interval": 1, "previousWin": 1990, "followingWin": 1991 }
  ],
  "max": [
    { "producer": "Matthew Vaughn", "interval": 13, "previousWin": 2002, "followingWin": 2015 }
  ]
}
```

Other HTTP methods on this resource return `405 Method Not Allowed`.

## H2 Console

While the application is running, the in-memory database can be browsed at `http://localhost:8080/h2-console`:

| Field | Value |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:challenge;NON_KEYWORDS=YEAR` |
| User Name | `sa` |
| Password | *(empty)* |

Keep `NON_KEYWORDS=YEAR` in the URL: `YEAR` is a reserved word in H2 and is used as a column name in the `movie` table.

## Running the tests

The project has integration tests only. They start the full application and assert against the data in the provided CSV.

```bash
./gradlew test
```

Run a single test class:

```bash
./gradlew test --tests 'com.outsera.challenge_backend.award.AwardIntervalControllerIT'
```

The HTML test report is written to `build/reports/tests/test/index.html`.

| Test | What it checks |
| --- | --- |
| `AwardIntervalControllerIT` | Full JSON response of the endpoint for `movielist.csv`, and `405` on `POST` |
| `AwardIntervalTiesIT` | Ties on min and max, non-winners ignored and single-win producers excluded, using the fixture `src/test/resources/data/award-intervals-ties.csv` |
| `MovieDataLoaderIT` | CSV import: 206 movies, 42 winners, 359 distinct producers, and producer names split on `,` / `and` |
| `MovieRepositoryIT` | Repository queries on the Flyway schema: winning producer years ordered by producer then year, non-winners ignored, and the winner count |

### Test coverage

Coverage is measured with [JaCoCo](https://www.jacoco.org/jacoco/). The report is generated automatically after `./gradlew test`:

- HTML: `build/reports/jacoco/test/html/index.html`
- XML (for CI tools such as Sonar or Codecov): `build/reports/jacoco/test/jacocoTestReport.xml`

To regenerate it without re-running unchanged tests:

```bash
./gradlew jacocoTestReport
```

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `app.movies.csv-location` | `classpath:data/movielist.csv` | CSV loaded on startup. Accepts any Spring resource, e.g. `file:/path/movies.csv` |

To start with another file:

```bash
./gradlew bootRun --args='--app.movies.csv-location=file:/path/to/movies.csv'
```

The file must use the same format: `year;title;studios;producers;winner`, with a header line and `winner` set to `yes` or left empty.

## AI usage

This project was built with Claude Code. The prompts and the steps executed for each one are recorded in [CLAUDE.md](CLAUDE.md#agent-interaction-log), and the JPA conventions given to the agent are in [skills/spring-data-jpa/SKILL.md](skills/spring-data-jpa/SKILL.md).
