package com.outsera.challenge_backend.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	long countByWinnerTrue();

	@Query(nativeQuery = true, value = """
			select p.name as producer, m.year as year
			from movie m
			join movie_producer mp on mp.movie_id = m.id
			join producer p on p.id = mp.producer_id
			where m.winner = true
			order by p.name, m.year
			""")
	List<ProducerWin> findProducerWinsOrderByProducerAndYear();

}
