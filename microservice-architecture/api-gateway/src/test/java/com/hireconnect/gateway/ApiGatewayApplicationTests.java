package com.hireconnect.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {

	@Autowired
	private RouteDefinitionLocator routeDefinitionLocator;

	@Test
	void contextLoads() {
	}

	@Test
	void routeDefinitionsMatchServiceControllerBasePaths() {
		Map<String, RouteDefinition> routesById = routeDefinitionLocator.getRouteDefinitions()
				.collectList()
				.block(Duration.ofSeconds(5))
				.stream()
				.collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));

		assertRoute(routesById, "auth-admin-service-v1", "lb://AUTH-SERVICE",
				"/api/v1/auth/admin/?(?<segment>.*)", "/auth/admin/$\\{segment}");
		assertRoute(routesById, "auth-service-v1", "lb://AUTH-SERVICE",
				"/api/v1/auth/?(?<segment>.*)", "/auth/$\\{segment}");
		assertRoute(routesById, "job-service-v1", "lb://JOB-SERVICE",
				"/api/v1/jobs/?(?<segment>.*)", "/api/jobs/$\\{segment}");
		assertRoute(routesById, "application-service-v1", "lb://APPLICATION-SERVICE",
				"/api/v1/applications/?(?<segment>.*)", "/api/applications/$\\{segment}");
		assertRoute(routesById, "interview-service-v1", "lb://INTERVIEW-SERVICE",
				"/api/v1/interviews/?(?<segment>.*)", "/api/interviews/$\\{segment}");
		assertRoute(routesById, "notification-service-v1", "lb://NOTIFICATION-SERVICE",
				"/api/v1/notifications/?(?<segment>.*)", "/api/notifications/$\\{segment}");
		assertRoute(routesById, "subscription-service-v1", "lb://SUBSCRIPTION-SERVICE",
				"/api/v1/subscriptions/?(?<segment>.*)", "/api/subscriptions/$\\{segment}");
		assertRoute(routesById, "analytics-service-v1", "lb://ANALYTICS-SERVICE",
				"/api/v1/analytics/?(?<segment>.*)", "/api/analytics/$\\{segment}");
	}

	private void assertRoute(Map<String, RouteDefinition> routesById, String routeId, String uri,
			String rewriteRegex, String rewriteReplacement) {
		RouteDefinition route = routesById.get(routeId);

		assertThat(route).as(routeId).isNotNull();
		assertThat(route.getUri()).isEqualTo(URI.create(uri));
		assertThat(route.getFilters())
				.extracting(FilterDefinition::getName)
				.contains("RewritePath");

		List<String> rewriteValues = route.getFilters().stream()
				.filter(filter -> "RewritePath".equals(filter.getName()))
				.flatMap(filter -> filter.getArgs().values().stream())
				.toList();

		assertThat(rewriteValues).contains(rewriteRegex, rewriteReplacement);
	}

}
