package com.marcel.parking.client;

import com.marcel.parking.dto.GarageConfigurationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

@Component
public class GarageSimulatorClient {

    private final RestClient restClient;

    public GarageSimulatorClient(
            RestClient.Builder restClientBuilder,
            @Value("${garage.simulator.base-url}") String simulatorBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(simulatorBaseUrl)
                .build();
    }

    @Retry(name = "garageSimulator")
    @CircuitBreaker(name = "garageSimulator", fallbackMethod = "fallbackGarageConfiguration")
    @TimeLimiter(name = "garageSimulator")
    public CompletableFuture<GarageConfigurationResponse> getGarageConfiguration() {
        return CompletableFuture.supplyAsync(() ->
                restClient.get()
                        .uri("/garage")
                        .retrieve()
                        .body(GarageConfigurationResponse.class)
        );
    }

    private CompletableFuture<GarageConfigurationResponse> fallbackGarageConfiguration(Throwable throwable) {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Garage simulator unavailable. Startup degraded.", throwable)
        );
    }
}
