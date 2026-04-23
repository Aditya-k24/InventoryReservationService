package com.example.inventoryreservation.scheduler;

import com.example.inventoryreservation.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${app.expiry.interval-ms:60000}")
    public void expireStaleReservations() {
        log.debug("Running reservation expiry job");
        reservationService.expireReservations();
    }
}
