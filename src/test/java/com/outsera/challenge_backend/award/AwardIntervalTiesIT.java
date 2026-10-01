package com.outsera.challenge_backend.award;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against a small fixture with ties on both min and max, a non-winning movie that would
 * shorten Beta's interval if counted, and a producer (Epsilon) with a single win.
 * Uses its own database so it does not share data with the default context.
 */
@SpringBootTest(properties = {
		"app.movies.csv-location=classpath:data/award-intervals-ties.csv",
		"spring.datasource.url=jdbc:h2:mem:award-intervals-ties;NON_KEYWORDS=YEAR"
})
@AutoConfigureMockMvc
class AwardIntervalTiesIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsEveryProducerTiedOnMinAndMax() throws Exception {
		mockMvc.perform(get("/producers/award-intervals"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{
						  "min": [
						    {"producer": "Alpha", "interval": 1, "previousWin": 2000, "followingWin": 2001},
						    {"producer": "Gamma", "interval": 1, "previousWin": 2015, "followingWin": 2016}
						  ],
						  "max": [
						    {"producer": "Alpha", "interval": 9, "previousWin": 2001, "followingWin": 2010},
						    {"producer": "Beta", "interval": 9, "previousWin": 2010, "followingWin": 2019}
						  ]
						}
						""", JsonCompareMode.STRICT));
	}

}
