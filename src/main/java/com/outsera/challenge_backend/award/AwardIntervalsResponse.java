package com.outsera.challenge_backend.award;

import java.util.List;

public record AwardIntervalsResponse(List<ProducerInterval> min, List<ProducerInterval> max) {
}
