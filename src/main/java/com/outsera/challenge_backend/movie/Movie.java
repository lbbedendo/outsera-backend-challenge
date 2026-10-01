package com.outsera.challenge_backend.movie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movie")
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movie_seq")
	@SequenceGenerator(name = "movie_seq", sequenceName = "movie_seq", allocationSize = 1)
	private Long id;

	@Column(name = "year", nullable = false)
	private Integer year;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "winner", nullable = false)
	private boolean winner;

	@ManyToMany
	@JoinTable(
			name = "movie_producer",
			joinColumns = @JoinColumn(name = "movie_id"),
			inverseJoinColumns = @JoinColumn(name = "producer_id"))
	private Set<Producer> producers = new HashSet<>();

	protected Movie() {
	}

	public Movie(Integer year, String title, boolean winner, Set<Producer> producers) {
		this.year = year;
		this.title = title;
		this.winner = winner;
		this.producers = new HashSet<>(producers);
	}

	public Long getId() {
		return id;
	}

	public Integer getYear() {
		return year;
	}

	public String getTitle() {
		return title;
	}

	public boolean isWinner() {
		return winner;
	}

	public Set<Producer> getProducers() {
		return Set.copyOf(producers);
	}

}
