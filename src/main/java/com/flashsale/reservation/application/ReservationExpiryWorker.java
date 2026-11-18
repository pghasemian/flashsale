package com.flashsale.reservation.application;

import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.domain.ReservationStatus;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryWorker {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationExpiryWorker(
            ReservationRepository reservationRepository,
            ReservationService reservationService) {

        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelay = 1000)
    public void expireOverdueReservations() {

        List<Reservation> expiredReservations =
                reservationRepository.findByStatusAndExpiresAtBefore(
                        ReservationStatus.ACTIVE,
                        Instant.now()
                );

        for (Reservation reservation : expiredReservations) {
            try {
                reservationService.expireReservation(reservation.getId());
            } catch (RuntimeException exception) {
                System.err.println(
                        "Failed to expire reservation: "
                                + reservation.getId()
                );
            }
        }
    }
}