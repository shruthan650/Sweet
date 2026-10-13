package com.shruthan.sweet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.profiles.active=production",
		"sweet.api.access-token=backend-test-token-0123456789-abcdef"
})
@AutoConfigureMockMvc
class SweetApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthEndpointIsAvailableWithoutComponentDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void productionApiRejectsRequestsWithoutTheBearerToken() throws Exception {
		mockMvc.perform(get("/api/cities"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	@Test
	void productionApiAcceptsTheConfiguredTokenBeforeCallingStorage() throws Exception {
		mockMvc.perform(get("/api/cities")
					.header("Authorization", "Bearer backend-test-token-0123456789-abcdef"))
				.andExpect(status().isServiceUnavailable());
	}

}
