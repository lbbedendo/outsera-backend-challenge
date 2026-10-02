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
 * Runs against a fixture where every producer has at most one win. Alpha, Beta and Epsilon also have
 * non-winning movies one year apart from their win, so counting nominations would produce intervals.
 * Uses its own database so it does not share data with the default context.
 */
@SpringBootTest(properties = {
		"app.movies.csv-location=classpath:data/award-intervals-single-wins.csv",
		"spring.datasource.url=jdbc:h2:mem:award-intervals-single-wins;NON_KEYWORDS=YEAR"
})
@AutoConfigureMockMvc
class AwardIntervalSingleWinsIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void returnsEmptyMinAndMaxWhenNoProducerWonTwice() throws Exception {
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movie WHERE winner", Integer.class))
				.as("fixture must be loaded, otherwise empty lists prove nothing")
				.isEqualTo(3);

		mockMvc.perform(get("/producers/award-intervals"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{
						  "min": [],
						  "max": []
						}
						""", JsonCompareMode.STRICT));
	}

}
