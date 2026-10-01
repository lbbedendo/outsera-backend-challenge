package com.outsera.challenge_backend.movie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway schema. The CSV loader is not part of the JPA slice, so each
 * test starts from empty tables and builds its own data; changes are rolled back after each test.
 * Keeps the configured datasource (NON_KEYWORDS=YEAR is required by the schema) under its own name.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:movie-repository;NON_KEYWORDS=YEAR")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MovieRepositoryIT {

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findsWinningProducerYearsOrderedByProducerThenYear() {
		Producer beta = entityManager.persist(new Producer("Beta"));
		Producer alpha = entityManager.persist(new Producer("Alpha"));
		entityManager.persist(new Movie(2005, "Shared Win", true, Set.of(alpha, beta)));
		entityManager.persist(new Movie(1999, "Beta Win", true, Set.of(beta)));
		entityManager.persist(new Movie(2001, "Alpha Win", true, Set.of(alpha)));
		entityManager.flush();

		assertThat(movieRepository.findProducerWinsOrderByProducerAndYear())
				.containsExactly(
						new ProducerWin("Alpha", 2001),
						new ProducerWin("Alpha", 2005),
						new ProducerWin("Beta", 1999),
						new ProducerWin("Beta", 2005));
	}

	@Test
	void ignoresNonWinningMovies() {
		Producer alpha = entityManager.persist(new Producer("Alpha"));
		entityManager.persist(new Movie(2001, "Alpha Win", true, Set.of(alpha)));
		entityManager.persist(new Movie(2002, "Alpha Nomination", false, Set.of(alpha)));
		entityManager.flush();

		assertThat(movieRepository.findProducerWinsOrderByProducerAndYear())
				.containsExactly(new ProducerWin("Alpha", 2001));
	}

	@Test
	void returnsNoProducerWinsWhenThereAreNoWinners() {
		Producer alpha = entityManager.persist(new Producer("Alpha"));
		entityManager.persist(new Movie(2001, "Alpha Nomination", false, Set.of(alpha)));
		entityManager.flush();

		assertThat(movieRepository.findProducerWinsOrderByProducerAndYear()).isEmpty();
	}

	@Test
	void countsOnlyWinningMovies() {
		Producer alpha = entityManager.persist(new Producer("Alpha"));
		entityManager.persist(new Movie(2001, "Win 1", true, Set.of(alpha)));
		entityManager.persist(new Movie(2002, "Win 2", true, Set.of(alpha)));
		entityManager.persist(new Movie(2003, "Nomination", false, Set.of(alpha)));
		entityManager.flush();

		assertThat(movieRepository.countByWinnerTrue()).isEqualTo(2);
	}

}
