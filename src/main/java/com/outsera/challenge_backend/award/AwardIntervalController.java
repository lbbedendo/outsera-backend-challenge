package com.outsera.challenge_backend.award;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/producers")
public class AwardIntervalController {

	private final AwardIntervalService awardIntervalService;

	public AwardIntervalController(AwardIntervalService awardIntervalService) {
		this.awardIntervalService = awardIntervalService;
	}

	@GetMapping("/award-intervals")
	public AwardIntervalsResponse getAwardIntervals() {
		return awardIntervalService.findMinAndMaxIntervals();
	}

}
