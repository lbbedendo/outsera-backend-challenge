package com.outsera.challenge_backend.movie;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	long countByWinnerTrue();

}
