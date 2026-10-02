package com.outsera.challenge_backend.award;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against a fixture where only Alpha has two wins (2000 and 2004), so its single interval is both
 * min and max. Alpha's and Beta's nominations between those years would add intervals if counted.
 * Uses its own database so it does not share data with the default context.
 */
@SpringBootTest(properties = {
		"app.movies.csv-location=classpath:data/award-intervals-single-producer.csv",
		"spring.datasource.url=jdbc:h2:mem:award-intervals-single-producer;NON_KEYWORDS=YEAR"
})
@AutoConfigureMockMvc
class AwardIntervalSingleProducerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void returnsTheSameProducerInMinAndMaxWhenOnlyOneProducerWonTwice() throws Exception {
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movie WHERE winner", Integer.class))
				.as("fixture must be loaded with Alpha's two wins")
				.isEqualTo(4);

		mockMvc.perform(get("/producers/award-intervals"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{
						  "min": [
						    {"producer": "Alpha", "interval": 4, "previousWin": 2000, "followingWin": 2004}
						  ],
						  "max": [
						    {"producer": "Alpha", "interval": 4, "previousWin": 2000, "followingWin": 2004}
						  ]
						}
						""", JsonCompareMode.STRICT));
	}

}
