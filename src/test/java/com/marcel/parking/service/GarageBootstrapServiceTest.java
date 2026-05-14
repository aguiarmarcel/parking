package com.marcel.parking.service;

import com.marcel.parking.client.GarageSimulatorClient;
import com.marcel.parking.dto.GarageConfigurationResponse;
import com.marcel.parking.entity.GarageSector;
import com.marcel.parking.repository.GarageSectorRepository;
import com.marcel.parking.repository.ParkingSpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GarageBootstrapServiceTest {

    @Mock
    private GarageSimulatorClient garageSimulatorClient;

    @Mock
    private GarageSectorRepository garageSectorRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @InjectMocks
    private GarageBootstrapService garageBootstrapService;

    @Test
    @DisplayName("Deve carregar configuração da garagem criando setor e vaga inexistentes")
    void shouldLoadGarageConfigurationCreatingMissingSectorAndSpot() throws Exception {
        GarageConfigurationResponse response = new GarageConfigurationResponse(
                List.of(new GarageConfigurationResponse.GarageSectorItem(
                        "A",
                        new BigDecimal("10.00"),
                        10
                )),
                List.of(new GarageConfigurationResponse.GarageSpotItem(
                        1L,
                        "A",
                        new BigDecimal("-23.0000000"),
                        new BigDecimal("-46.0000000")
                ))
        );

        when(garageSimulatorClient.getGarageConfiguration())
                .thenReturn(CompletableFuture.completedFuture(response));

        when(garageSectorRepository.findBySector("A"))
                .thenReturn(Optional.empty());

        when(parkingSpotRepository.existsById(1L))
                .thenReturn(false);

        garageBootstrapService.run(null);

        verify(garageSectorRepository).save(argThat(sector ->
                sector.getSector().equals("A")
                        && sector.getBasePrice().compareTo(new BigDecimal("10.00")) == 0
                        && sector.getMaxCapacity().equals(10)
        ));

        verify(parkingSpotRepository).save(argThat(spot ->
                spot.getId().equals(1L)
                        && spot.getSector().equals("A")
                        && spot.getLatitude().compareTo(new BigDecimal("-23.0000000")) == 0
                        && spot.getLongitude().compareTo(new BigDecimal("-46.0000000")) == 0
        ));
    }

    @Test
    @DisplayName("Deve atualizar setor existente e não recriar vaga existente")
    void shouldUpdateExistingSectorAndNotCreateExistingSpot() throws Exception {
        GarageSector existingSector = new GarageSector(
                "A",
                new BigDecimal("5.00"),
                5
        );

        GarageConfigurationResponse response = new GarageConfigurationResponse(
                List.of(new GarageConfigurationResponse.GarageSectorItem(
                        "A",
                        new BigDecimal("15.00"),
                        20
                )),
                List.of(new GarageConfigurationResponse.GarageSpotItem(
                        1L,
                        "A",
                        new BigDecimal("-23.0000000"),
                        new BigDecimal("-46.0000000")
                ))
        );

        when(garageSimulatorClient.getGarageConfiguration())
                .thenReturn(CompletableFuture.completedFuture(response));

        when(garageSectorRepository.findBySector("A"))
                .thenReturn(Optional.of(existingSector));

        when(parkingSpotRepository.existsById(1L))
                .thenReturn(true);

        garageBootstrapService.run(null);

        assertThat(existingSector.getBasePrice()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(existingSector.getMaxCapacity()).isEqualTo(20);

        verify(garageSectorRepository, never()).save(any());
        verify(parkingSpotRepository, never()).save(any());
    }
}
