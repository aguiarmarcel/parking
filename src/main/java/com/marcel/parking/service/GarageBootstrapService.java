package com.marcel.parking.service;

import com.marcel.parking.client.GarageSimulatorClient;
import com.marcel.parking.dto.GarageConfigurationResponse;
import com.marcel.parking.entity.GarageSector;
import com.marcel.parking.entity.ParkingSpot;
import com.marcel.parking.repository.GarageSectorRepository;
import com.marcel.parking.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarageBootstrapService implements ApplicationRunner {

    private final GarageSimulatorClient garageSimulatorClient;
    private final GarageSectorRepository garageSectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Loading garage configuration from simulator");

        GarageConfigurationResponse response = garageSimulatorClient.getGarageConfiguration().join();

        response.garage().forEach(item -> {
            garageSectorRepository.findBySector(item.sector())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setBasePrice(item.basePrice());
                                existing.setMaxCapacity(item.maxCapacity());
                            },
                            () -> garageSectorRepository.save(
                                    new GarageSector(item.sector(), item.basePrice(), item.maxCapacity())
                            )
                    );
        });

        response.spots().forEach(item -> {
            if (!parkingSpotRepository.existsById(item.id())) {
                parkingSpotRepository.save(
                        new ParkingSpot(item.id(), item.sector(), item.lat(), item.lng())
                );
            }
        });

        log.info("Garage configuration loaded. sectors={}, spots={}",
                response.garage().size(),
                response.spots().size()
        );
    }
}
