package com.outsera.challenge_backend.movie.csv;

import java.util.List;

public record MovieCsvRecord(int year, String title, boolean winner, List<String> producers) {
}
