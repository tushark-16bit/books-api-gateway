package com.tk16.microservices.apigateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Function;

@Configuration
public class ApiGatewayConfiguration {

    Function<GatewayFilterSpec, UriSpec> retryFilter =
            f -> f
                    .retry(retryConfig ->
                            RetryConfig.custom()
                                    .failAfterMaxAttempts(true)
                                    .maxAttempts(3)
                                    .waitDuration(Duration.ofMillis(50)))
                    .setResponseHeader("X-Tenant", "true");

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory
                .configureDefault(id -> new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .slidingWindowSize(10)
                                .permittedNumberOfCallsInHalfOpenState(5)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofSeconds(5))
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(1))
                                .build())
                        .build());
    }

    @Bean
    public RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/book/**")
                        .filters(retryFilter)
                        .uri("lb://book-service"))
                .route(p -> p.path("/cart/**")
                        .filters(retryFilter)
                        .uri("lb://cart-service"))
                .route(p -> p.path("/purchase/**")
                        .filters(retryFilter)
                        .uri("lb://books-purchase-order-service"))
                .build();
    }
}
