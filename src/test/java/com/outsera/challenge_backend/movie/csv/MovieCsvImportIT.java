package com.outsera.challenge_backend.movie.csv;

import com.outsera.challenge_backend.ChallengeBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Starts the whole application with a given CSV, as {@code bootRun} would, and checks that a malformed
 * file stops startup with a message pointing at the offending line. Each run gets its own H2 database
 * and no web server.
 */
class MovieCsvImportIT {

	@Test
	void failsStartupWhenLineHasWrongColumnCount() {
		assertThatThrownBy(() -> startWith("classpath:data/invalid/wrong-column-count.csv"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 3: expected 5 columns but found 4");
	}

	@Test
	void failsStartupWhenYearIsNotANumber() {
		assertThatThrownBy(() -> startWith("classpath:data/invalid/invalid-year.csv"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 3: invalid year '198O'")
				.hasCauseInstanceOf(NumberFormatException.class);
	}

	@Test
	void failsStartupWhenFileDoesNotExist() {
		assertThatThrownBy(() -> startWith("classpath:data/invalid/missing.csv"))
				.isInstanceOf(UncheckedIOException.class)
				.hasMessage("Failed to read movie CSV: class path resource [data/invalid/missing.csv]")
				.hasCauseInstanceOf(FileNotFoundException.class);
	}

	@Test
	void skipsBlankLines() {
		try (ConfigurableApplicationContext context = startWith("classpath:data/with-blank-lines.csv")) {
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

			assertThat(jdbcTemplate.queryForList("SELECT title FROM movie ORDER BY year", String.class))
					.containsExactly("First", "Second");
		}
	}

	/** Passed as command-line args: builder {@code properties(...)} are defaults and lose to application.yml. */
	private static ConfigurableApplicationContext startWith(String csvLocation) {
		return new SpringApplicationBuilder(ChallengeBackendApplication.class)
				.web(WebApplicationType.NONE)
				.run(
						"--app.movies.csv-location=" + csvLocation,
						"--spring.datasource.url=jdbc:h2:mem:csv-import-" + UUID.randomUUID() + ";NON_KEYWORDS=YEAR");
	}

}
