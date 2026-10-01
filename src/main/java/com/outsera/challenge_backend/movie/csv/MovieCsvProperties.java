package com.outsera.challenge_backend.movie.csv;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("app.movies")
public record MovieCsvProperties(Resource csvLocation) {
}
