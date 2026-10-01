package com.outsera.challenge_backend.movie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "producer")
public class Producer {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producer_seq")
	@SequenceGenerator(name = "producer_seq", sequenceName = "producer_seq", allocationSize = 1)
	private Long id;

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	protected Producer() {
	}

	public Producer(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
