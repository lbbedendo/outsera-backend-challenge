package com.outsera.challenge_backend.award;

import com.outsera.challenge_backend.movie.MovieRepository;
import com.outsera.challenge_backend.movie.ProducerWin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AwardIntervalService {

	private final MovieRepository movieRepository;

	public AwardIntervalService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	/**
	 * Producers with the shortest and longest gap between two consecutive wins, in a single pass.
	 * Ties are all returned; a producer with a single win has no interval.
	 */
	@Transactional(readOnly = true)
	public AwardIntervalsResponse findMinAndMaxIntervals() {
		// Sorted by producer, then year: consecutive wins of a producer are adjacent.
		List<ProducerWin> wins = movieRepository.findProducerWinsOrderByProducerAndYear();

		List<ProducerInterval> min = new ArrayList<>();
		List<ProducerInterval> max = new ArrayList<>();
		int minInterval = Integer.MAX_VALUE;
		int maxInterval = Integer.MIN_VALUE;

		for (int i = 1; i < wins.size(); i++) {
			ProducerWin previous = wins.get(i - 1);
			ProducerWin following = wins.get(i);
			if (!previous.producer().equals(following.producer())) {
				continue;
			}
			int interval = following.year() - previous.year();
			var producerInterval = new ProducerInterval(following.producer(), interval, previous.year(),
					following.year());

			// Separate ifs, not else-if: the first interval found must enter both lists.
			if (interval < minInterval) {
				minInterval = interval;
				min.clear();
			}
			if (interval == minInterval) {
				min.add(producerInterval);
			}
			if (interval > maxInterval) {
				maxInterval = interval;
				max.clear();
			}
			if (interval == maxInterval) {
				max.add(producerInterval);
			}
		}
		return new AwardIntervalsResponse(min, max);
	}

}
