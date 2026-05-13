package com.marcel.parking.exception;

public class SectorFullException extends BusinessException {
    public SectorFullException(String sector) {
        super("Sector is full" + sector);
    }
}
