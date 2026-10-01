package com.outsera.challenge_backend.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	long countByWinnerTrue();

	@Query("""
			select new com.outsera.challenge_backend.movie.ProducerWin(p.name, m.year)
			from Movie m
			join m.producers p
			where m.winner = true
			order by p.name, m.year
			""")
	List<ProducerWin> findProducerWinsOrderByProducerAndYear();

}
