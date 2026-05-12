CREATE TABLE garage_sector (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               sector VARCHAR(50) NOT NULL,
                               base_price DECIMAL(10, 2) NOT NULL,
                               max_capacity INT NOT NULL,
                               occupied_count INT NOT NULL DEFAULT 0,
                               closed BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_garage_sector_sector (sector)
);

CREATE TABLE parking_spot (
                              id BIGINT NOT NULL,
                              sector VARCHAR(50) NOT NULL,
                              latitude DECIMAL(10, 7) NOT NULL,
                              longitude DECIMAL(10, 7) NOT NULL,
                              status VARCHAR(30) NOT NULL,
                              occupied_by_license_plate VARCHAR(20) NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (id),
                              INDEX idx_parking_spot_sector_status (sector, status),
                              INDEX idx_parking_spot_license_plate (occupied_by_license_plate)
);

CREATE TABLE parking_ticket (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                license_plate VARCHAR(20) NOT NULL,
                                sector VARCHAR(50) NOT NULL,
                                spot_id BIGINT NULL,
                                entry_time TIMESTAMP NOT NULL,
                                parked_time TIMESTAMP NULL,
                                exit_time TIMESTAMP NULL,
                                price_multiplier DECIMAL(5, 2) NOT NULL,
                                amount DECIMAL(10, 2) NULL,
                                status VARCHAR(30) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (id),
                                INDEX idx_ticket_license_status (license_plate, status),
                                INDEX idx_ticket_sector_exit_time (sector, exit_time),
                                INDEX idx_ticket_exit_time (exit_time)
);

CREATE TABLE webhook_event (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               idempotency_key VARCHAR(120) NOT NULL,
                               license_plate VARCHAR(20) NOT NULL,
                               event_type VARCHAR(30) NOT NULL,
                               payload JSON NOT NULL,
                               processed BOOLEAN NOT NULL DEFAULT FALSE,
                               received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               processed_at TIMESTAMP NULL,
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_webhook_event_idempotency_key (idempotency_key),
                               INDEX idx_webhook_event_processed (processed),
                               INDEX idx_webhook_event_license_type (license_plate, event_type)
);