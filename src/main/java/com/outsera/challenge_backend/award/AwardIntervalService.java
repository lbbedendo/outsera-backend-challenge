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
	 * Producers with the shortest and longest gap between two consecutive wins.
	 * Ties are all returned; a producer with a single win has no interval.
	 */
	@Transactional(readOnly = true)
	public AwardIntervalsResponse findMinAndMaxIntervals() {
		List<ProducerInterval> intervals = consecutiveWinIntervals(
				movieRepository.findProducerWinsOrderByProducerAndYear());

		int min = intervals.stream().mapToInt(ProducerInterval::interval).min().orElse(0);
		int max = intervals.stream().mapToInt(ProducerInterval::interval).max().orElse(0);

		return new AwardIntervalsResponse(
				intervals.stream().filter(interval -> interval.interval() == min).toList(),
				intervals.stream().filter(interval -> interval.interval() == max).toList());
	}

	/** Expects wins sorted by producer, then year. */
	private static List<ProducerInterval> consecutiveWinIntervals(List<ProducerWin> wins) {
		List<ProducerInterval> intervals = new ArrayList<>();
		for (int i = 1; i < wins.size(); i++) {
			ProducerWin previous = wins.get(i - 1);
			ProducerWin following = wins.get(i);
			if (previous.producer().equals(following.producer())) {
				intervals.add(new ProducerInterval(following.producer(), following.year() - previous.year(),
						previous.year(), following.year()));
			}
		}
		return intervals;
	}

}
