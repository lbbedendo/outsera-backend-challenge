package com.outsera.challenge_backend.movie.csv;

import com.outsera.challenge_backend.movie.Movie;
import com.outsera.challenge_backend.movie.MovieRepository;
import com.outsera.challenge_backend.movie.Producer;
import com.outsera.challenge_backend.movie.ProducerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads the movie CSV into the database on startup. Runs as an {@link ApplicationRunner},
 * i.e. after the context is refreshed, so Flyway migrations have already been applied.
 */
@Component
@EnableConfigurationProperties(MovieCsvProperties.class)
public class MovieDataLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MovieDataLoader.class);

	private final MovieCsvProperties properties;
	private final MovieCsvParser parser;
	private final MovieRepository movieRepository;
	private final ProducerRepository producerRepository;

	public MovieDataLoader(MovieCsvProperties properties, MovieCsvParser parser,
			MovieRepository movieRepository, ProducerRepository producerRepository) {
		this.properties = properties;
		this.parser = parser;
		this.movieRepository = movieRepository;
		this.producerRepository = producerRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (movieRepository.count() > 0) {
			log.info("Movies already loaded, skipping CSV import");
			return;
		}

		List<MovieCsvRecord> records = parser.parse(properties.csvLocation());

		Map<String, Producer> producersByName = new HashMap<>();
		records.stream()
				.flatMap(movieRecord -> movieRecord.producers().stream())
				.distinct()
				.forEach(name -> producersByName.put(name, new Producer(name)));
		producerRepository.saveAll(producersByName.values());

		List<Movie> movies = records.stream()
				.map(movieRecord -> new Movie(
						movieRecord.year(),
						movieRecord.title(),
						movieRecord.winner(),
						movieRecord.producers().stream().map(producersByName::get).collect(Collectors.toSet())))
				.toList();
		movieRepository.saveAll(movies);

		log.info("Loaded {} movies and {} producers from {}", movies.size(), producersByName.size(),
				properties.csvLocation().getDescription());
	}

}
