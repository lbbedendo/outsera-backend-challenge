package com.outsera.challenge_backend.award;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against the movielist.csv shipped with the application. */
@SpringBootTest
@AutoConfigureMockMvc
class AwardIntervalControllerIT {

	private static final String URL = "/producers/award-intervals";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsMinAndMaxIntervalsFromProvidedCsv() throws Exception {
		mockMvc.perform(get(URL).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "min": [
						    {"producer": "Joel Silver", "interval": 1, "previousWin": 1990, "followingWin": 1991}
						  ],
						  "max": [
						    {"producer": "Matthew Vaughn", "interval": 13, "previousWin": 2002, "followingWin": 2015}
						  ]
						}
						""", JsonCompareMode.STRICT));
	}

	@Test
	void rejectsUnsupportedMethod() throws Exception {
		mockMvc.perform(post(URL))
				.andExpect(status().isMethodNotAllowed());
	}

}
