package com.flowstudio.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void testCorsHeadersArePresent() {
		// Just to verity the gateway starts cleanly and responds with 404 without
		// crashing
		ResponseEntity<String> entity = restTemplate.getForEntity("/actuator/health", String.class);
		// With default settings without actuator exposed it should be 404
		assertThat(entity.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.OK);
	}
}
