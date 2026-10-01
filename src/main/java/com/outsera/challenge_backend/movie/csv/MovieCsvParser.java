package com.outsera.challenge_backend.movie.csv;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the Golden Raspberry Awards CSV: {@code year;title;studios;producers;winner}.
 * Producers are separated by "," and/or "and" (e.g. "A, B, and C").
 */
@Component
public class MovieCsvParser {

	private static final String DELIMITER = ";";
	private static final int COLUMN_COUNT = 5;
	private static final int YEAR = 0;
	private static final int TITLE = 1;
	private static final int PRODUCERS = 3;
	private static final int WINNER = 4;
	private static final Pattern PRODUCER_SEPARATOR = Pattern.compile("\\s*,\\s*(?:and\\s+)?|\\s+and\\s+");

	public List<MovieCsvRecord> parse(Resource resource) {
		try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			List<MovieCsvRecord> records = new ArrayList<>();
			reader.readLine(); // header
			int lineNumber = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (!line.isBlank()) {
					records.add(parseLine(line, lineNumber));
				}
			}
			return records;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read movie CSV: " + resource.getDescription(), ex);
		}
	}

	private MovieCsvRecord parseLine(String line, int lineNumber) {
		String[] columns = line.split(DELIMITER, -1);
		if (columns.length != COLUMN_COUNT) {
			throw new IllegalArgumentException(
					"Line %d: expected %d columns but found %d".formatted(lineNumber, COLUMN_COUNT, columns.length));
		}
		try {
			return new MovieCsvRecord(
					Integer.parseInt(columns[YEAR].strip()),
					columns[TITLE].strip(),
					"yes".equalsIgnoreCase(columns[WINNER].strip()),
					parseProducers(columns[PRODUCERS]));
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Line %d: invalid year '%s'".formatted(lineNumber, columns[YEAR]), ex);
		}
	}

	private List<String> parseProducers(String producers) {
		return Arrays.stream(PRODUCER_SEPARATOR.split(producers.strip()))
				.map(String::strip)
				.filter(name -> !name.isEmpty())
				.distinct()
				.toList();
	}

}
