package com.outsera.challenge_backend.movie.csv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MovieDataLoaderIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void loadsEveryMovieFromCsv() {
		assertThat(count("SELECT COUNT(*) FROM movie")).isEqualTo(206);
		assertThat(count("SELECT COUNT(*) FROM movie WHERE winner")).isEqualTo(42);
	}

	@Test
	void storesEachProducerOnce() {
		assertThat(count("SELECT COUNT(*) FROM producer")).isEqualTo(359);
	}

	@Test
	void mapsMovieColumns() {
		var movie = jdbcTemplate.queryForMap(
				"SELECT year, winner FROM movie WHERE title = ?", "Can't Stop the Music");

		assertThat(movie).containsEntry("YEAR", 1980).containsEntry("WINNER", true);
	}

	@Test
	void splitsProducersSeparatedByCommasAndAnd() {
		assertThat(producersOf("Cats"))
				.containsExactlyInAnyOrder("Debra Hayward", "Tim Bevan", "Eric Fellner", "Tom Hooper");
		assertThat(producersOf("Under the Cherry Moon"))
				.containsExactlyInAnyOrder("Bob Cavallo", "Joe Ruffalo", "Steve Fargnoli");
	}

	private List<String> producersOf(String title) {
		return jdbcTemplate.queryForList("""
				SELECT p.name
				FROM producer p
				JOIN movie_producer mp ON mp.producer_id = p.id
				JOIN movie m ON m.id = mp.movie_id
				WHERE m.title = ?
				""", String.class, title);
	}

	private Integer count(String sql) {
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}

}
